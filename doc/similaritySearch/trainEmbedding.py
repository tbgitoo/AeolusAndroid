
# %% [setup]
# pip install torch==2.2.2 numpy==1.26.4 onnx onnxruntime==1.18.0 sentence-transformers

import re, math, random, json
import numpy as np
import torch, torch.nn as nn, torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader

USE_TEACHER = True     # set False to use contrastive training
TEACHER_NAME = "sentence-transformers/all-MiniLM-L6-v2"  # only used offline during training

if USE_TEACHER:
    from sentence_transformers import SentenceTransformer

# %% [symbolic parser & extractor]
NOTE_RE  = re.compile(r"^N([+-]?\d+):([SML])$")
CHORD_RE = re.compile(r"^CH\[([0-9,\- ]+)\]:([SML])$")

def parse_symbolic(s):
    """Return list of dict onsets: {'type':'N'|'C','delta':int,'pcs':list[int] or None,'r':char}"""
    out = []
    if not s: return out
    for t in s.strip().split():
        m = NOTE_RE.match(t)
        if m:
            out.append({'type':'N','delta':int(m.group(1)),'pcs':None,'r':m.group(2)})
            continue
        m = CHORD_RE.match(t)
        if m:
            pcs = [((int(z.strip())%12)+12)%12 for z in m.group(1).split(',') if z.strip()!='']
            pcs.sort()
            out.append({'type':'C','delta':0,'pcs':pcs,'r':m.group(2)})
            continue
        # ignore unknown tokens
    return out

def wrap_nearest12(x):
    k = int(round(x/12.0))
    return x - 12*k

def r_index(r): return 0 if r=='S' else (1 if r=='M' else 2)
def clamp(v,lo,hi): return lo if v<lo else (hi if v>hi else v)
def direction(d): return -1 if d<0 else (1 if d>0 else 0)

# chord classes (same as Java)
CH_CLASSES = [
    [0,4,7], [0,3,7], [0,3,6], [0,4,8], [0,2,7], [0,5,7], [0,7],
    [0,4,7,11], [0,3,7,10], [0,4,7,10], [0,3,6,9], [0,3,7,11],
    [0,3,6,10], [0,2,4,7], [0,4,5,7], [0,4,7,9]
]
def chord_class_map(pcs):
    for i,cls in enumerate(CH_CLASSES):
        if pcs == cls: return i
    return 15  # "other" mapped to last

# Compact mapping of dir bigram (drop (0,0))
FULL2COMPACT = np.array([0,1,2, 3,-1,4, 5,6,7], dtype=np.int32)

def feature_vector_from_symbolic(s):
    # config
    A,B,C,D,E,F,G,P = 13,16,12,150,31,6,8,20
    OFF_A=0; OFF_B=OFF_A+A; OFF_C=OFF_B+B; OFF_D=OFF_C+C
    OFF_E=OFF_D+D; OFF_F=OFF_E+E; OFF_G=OFF_F+F; OFF_P=OFF_G+G
    FEAT = OFF_P + P  # 256
    x = np.zeros(FEAT, np.float32)

    onsets = parse_symbolic(s)
    if not onsets: return x

    # roots, rhythms, pcs
    roots=[]; rhythms=[]; pcs_list=[]
    current_root=0
    for o in onsets:
        if o['type']=='N':
            current_root += o['delta']
        roots.append(current_root)
        rhythms.append(o['r'])
        pcs_list.append(o['pcs'])

    # dRoot
    dRoot=[]
    for i in range(1,len(roots)):
        dRoot.append(wrap_nearest12(roots[i]-roots[i-1]))

    # A: Δ-root hist [-6..+6]
    a = np.zeros(A, np.float32)
    for d in dRoot:
        k = max(-6, min(6, d))
        a[k+6]+=1
    _write_hist_sqrt_l1(a, x, OFF_A)

    # B: chord class
    b = np.zeros(B, np.float32)
    for pcs in pcs_list:
        if pcs is not None:
            b[chord_class_map(pcs)] += 1
    _write_hist_sqrt_l1(b, x, OFF_B)

    # C: rhythm uni + bi
    c = np.zeros(C, np.float32)
    s = sum(1 for r in rhythms if r=='S')
    m = sum(1 for r in rhythms if r=='M')
    l = sum(1 for r in rhythms if r=='L')
    c[0],c[1],c[2] = s,m,l
    for i in range(1,len(rhythms)):
        p=r_index(rhythms[i-1]); q=r_index(rhythms[i])
        c[3 + p*3 + q]+=1
    _write_hist_sqrt_l1(c, x, OFF_C)

    # D: n-grams on clamp[-2..+2]
    d = np.zeros(D, np.float32)
    d2=[clamp(v,-2,2) for v in dRoot]
    # bi 25
    for i in range(1,len(d2)):
        a2=d2[i-1]+2; b2=d2[i]+2
        d[a2*5 + b2]+=1
    # tri 125
    offTri=25
    for i in range(2,len(d2)):
        a2=d2[i-2]+2; b2=d2[i-1]+2; c2=d2[i]+2
        d[offTri + a2*25 + b2*5 + c2]+=1
    _write_hist_sqrt_l1(d, x, OFF_D)

    # E: stats (31)
    e = _stats_block(onsets, dRoot, rhythms)  # returns 31 dims
    x[OFF_E:OFF_E+E] = e

    # F: |Δ| 0..5
    f = np.zeros(F, np.float32)
    for v in dRoot: f[min(5, abs(v))]+=1
    _write_hist_sqrt_l1(f, x, OFF_F)

    # G: dir bigram compact
    g = np.zeros(G, np.float32)
    for i in range(1,len(dRoot)):
        di = ( -1 if dRoot[i-1]<0 else (1 if dRoot[i-1]>0 else 0) ) + 1  # 0..2
        dj = ( -1 if dRoot[i]  <0 else (1 if dRoot[i]  >0 else 0) ) + 1
        full = di*3 + dj
        idx = FULL2COMPACT[full]
        if idx>=0: g[idx]+=1
    _write_hist_sqrt_l1(g, x, OFF_G)

    # PAD already zeros
    # L2
    n = np.linalg.norm(x)
    if n>1e-12: x/=n
    return x

def _write_hist_sqrt_l1(h, dest, off):
    s = np.sqrt(h).sum()
    dest[off:off+len(h)] = (np.sqrt(h)/s) if s>1e-12 else 0.0

def _stats_block(onsets, dRoot, rhythms):
    E=31; e=np.zeros(E, np.float32); idx=0
    total=len(onsets); chords=sum(1 for o in onsets if o['type']=='C')
    # 1 total/64
    e[idx]; idx+=1; e[idx-1] = min(1.0, total/64.0)
    # 2 fraction chords
    e[idx]; idx+=1; e[idx-1] = (chords/total) if total>0 else 0.0
    # 3 share repeated (|Δ|==0)
    rep=sum(1 for v in dRoot if abs(v)==0); denom=max(1,len(dRoot))
    e[idx]; idx+=1; e[idx-1] = rep/denom
    # 4 share leaps (|Δ|>=3)
    leap=sum(1 for v in dRoot if abs(v)>=3)
    e[idx]; idx+=1; e[idx-1] = leap/denom
    # 5 entropy Δ in [-6..+6]
    h=np.zeros(13, np.float32)
    for v in dRoot: h[max(-6,min(6,v))+6]+=1
    e[idx]; idx+=1; e[idx-1] = _entropy_norm(h, 13)
    # 6 rhythm unigram entropy
    rUni=np.zeros(3, np.float32)
    for r in rhythms: rUni[r_index(r)]+=1
    e[idx]; idx+=1; e[idx-1] = _entropy_norm(rUni, 3)
    # 7 max run same direction / total
    e[idx]; idx+=1; e[idx-1] = _max_run_direction(dRoot)/max(1,total)
    # 8 max run same rhythm / total
    e[idx]; idx+=1; e[idx-1] = _max_run_rhythm(rhythms)/max(1,total)
    # 9..13 chord size hist 1,2,3,4,>=5
    size=np.zeros(5, np.float32); chord_onsets=0
    for o in onsets:
        if o['type']=='C':
            chord_onsets+=1
            n=len(o['pcs']); bi = n-1 if n<=4 else 4
            size[bi]+=1
    size = (size/chord_onsets) if chord_onsets>0 else size
    e[idx:idx+5]=size; idx+=5
    # 14..22 rhythm bigrams 9
    rBi=np.zeros(9, np.float32)
    for i in range(1,len(rhythms)):
        p=r_index(rhythms[i-1]); q=r_index(rhythms[i]); rBi[p*3+q]+=1
    s=rBi.sum(); e[idx:idx+9] = (rBi/s) if s>0 else 0.0; idx+=9
    # 23 mean(|Δ|)/6, 24 std(|Δ|)/6
    abs_list=np.array([abs(v) for v in dRoot], np.float32)
    mean = abs_list.mean() if len(abs_list)>0 else 0.0
    std  = abs_list.std()  if len(abs_list)>0 else 0.0
    e[idx]; idx+=1; e[idx-1] = mean/6.0
    e[idx]; idx+=1; e[idx-1] = std/6.0
    # 25..27 direction shares
    neg=sum(1 for v in dRoot if v<0); zer=sum(1 for v in dRoot if v==0); pos=sum(1 for v in dRoot if v>0)
    e[idx:idx+3] = np.array([neg,zer,pos], np.float32)/denom; idx+=3
    # 28 fraction S, 29 fraction M
    e[idx]; idx+=1; e[idx-1] = (rUni[0]/total) if total>0 else 0.0
    e[idx]; idx+=1; e[idx-1] = (rUni[1]/total) if total>0 else 0.0
    # 30 fraction triads
    tri=sum(1 for o in onsets if o['type']=='C' and len(o['pcs'])==3)
    e[idx]; idx+=1; e[idx-1] = (tri/chord_onsets) if chord_onsets>0 else 0.0
    # 31 avg chord size / 6
    avg = (sum(len(o['pcs']) for o in onsets if o['type']=='C')/chord_onsets) if chord_onsets>0 else 0.0
    e[idx]; idx+=1; e[idx-1] = avg/6.0
    return e

def _entropy_norm(h, bins):
    s=h.sum()
    if s<=0: return 0.0
    p=h/s; p=p[p>0]
    H=-(p*np.log(p)).sum()
    return float(H / math.log(bins))

def _max_run_rhythm(r):
    if not r: return 0
    best=1; cur=1
    for i in range(1,len(r)):
        if r[i]==r[i-1]: cur+=1
        else: best=max(best,cur); cur=1
    return max(best,cur)

def _max_run_direction(d):
    if not d: return 0
    def sgn(x): return -1 if x<0 else (1 if x>0 else 0)
    prev=sgn(d[0]); best=1; cur=1
    for i in range(1,len(d)):
        k=sgn(d[i])
        if k==prev: cur+=1
        else: best=max(best,cur); cur=1; prev=k
    return max(best,cur)

# %% [dataset & student]
class SymDataset(Dataset):
    def __init__(self, lines, use_teacher=True):
        self.syms = [l.strip() for l in lines if l.strip()]
        self.use_teacher = use_teacher
        if use_teacher:
            self.teacher = SentenceTransformer(TEACHER_NAME)
            self.tvecs = self.teacher.encode(self.syms, normalize_embeddings=True)
        else:
            self.teacher = None
            self.tvecs = None
    def __len__(self): return len(self.syms)
    def __getitem__(self, idx):
        x = feature_vector_from_symbolic(self.syms[idx])  # (256,)
        if self.use_teacher:
            y = np.asarray(self.tvecs[idx], np.float32)   # (384,)
            return x, y
        else:
            # trivial "augmentation": shuffle two adjacent tokens (robustness);
            # customize with your own rhythmic jitter if you like
            toks = self.syms[idx].split()
            if len(toks)>=3:
                i = random.randint(0,len(toks)-2)
                toks[i], toks[i+1] = toks[i+1], toks[i]
            x2 = feature_vector_from_symbolic(" ".join(toks))
            return x, x2

class Student(nn.Module):
    def __init__(self, in_dim=256, hid=128, out_dim=128):
        super().__init__()
        self.fc1 = nn.Linear(in_dim, hid)
        self.fc2 = nn.Linear(hid, out_dim)
    def forward(self, x):
        z = F.relu(self.fc1(x))
        z = self.fc2(z)
        return F.normalize(z, dim=-1, eps=1e-12)

def cosine_loss(a,b):
    return 1.0 - F.cosine_similarity(a,b,dim=-1).mean()

def train_teacher(lines, epochs=8, lr=1e-3):
    ds = SymDataset(lines, use_teacher=True)
    n = len(ds); ntr = max(1,int(0.9*n))
    tr, va = torch.utils.data.random_split(ds, [ntr, n-ntr], generator=torch.Generator().manual_seed(42))
    dltr = DataLoader(tr, batch_size=128, shuffle=True)
    dlva = DataLoader(va, batch_size=128, shuffle=False)
    model = Student(); opt = torch.optim.AdamW(model.parameters(), lr=lr)
    best=1e9; best_state=None
    for ep in range(1,epochs+1):
        model.train(); tl=0.0
        for x,y in dltr:
            x=x.float(); y=F.normalize(y.float(),dim=-1)
            pred=model(x); loss=cosine_loss(pred,y)
            opt.zero_grad(); loss.backward(); opt.step(); tl += loss.item()*x.size(0)
        tl/=len(tr); model.eval(); vl=0.0
        with torch.no_grad():
            for x,y in dlva:
                x=x.float(); y=F.normalize(y.float(),dim=-1)
                pred=model(x); loss=cosine_loss(pred,y)
                vl += loss.item()*x.size(0)
        vl/=max(1,len(va)); print(f"ep{ep}: train {tl:.4f} val {vl:.4f}")
        if vl<best: best=vl; best_state={k:v.cpu() for k,v in model.state_dict().items()}
    model.load_state_dict(best_state); model.eval(); return model

def train_contrastive(lines, epochs=10, lr=2e-3, temp=0.07):
    ds=SymDataset(lines, use_teacher=False)
    dl=DataLoader(ds, batch_size=128, shuffle=True)
    model=Student(); opt=torch.optim.AdamW(model.parameters(), lr=lr)
    for ep in range(1,epochs+1):
        model.train(); tl=0.0
        for x1,x2 in dl:
            x1=x1.float(); x2=x2.float()
            z1=model(x1); z2=model(x2)
            # simple symmetric InfoNCE
            z1=F.normalize(z1,dim=-1); z2=F.normalize(z2,dim=-1)
            logits=(z1 @ z2.t())/temp
            labels=torch.arange(z1.size(0))
            loss=(F.cross_entropy(logits,labels)+F.cross_entropy(logits.t(),labels))/2
            opt.zero_grad(); loss.backward(); opt.step(); tl += loss.item()*x1.size(0)
        tl/=len(ds); print(f"ep{ep}: train {tl:.4f}")
    model.eval(); return model

# %% [run training & export]
DATA_PATH="motifs.txt"   # one symbolic motif per line
with open(DATA_PATH,"r",encoding="utf-8") as f:
    lines=[l.strip() for l in f if l.strip()]

model = train_teacher(lines, epochs=8, lr=1e-3) if USE_TEACHER else train_contrastive(lines, epochs=10)

dummy = torch.zeros(1,256, dtype=torch.float32)
torch.onnx.export(model, dummy, "aeolus_embedder.onnx",
                  input_names=["input"], output_names=["embedding"],
                  opset_version=13, dynamic_axes={"input":{0:"N"}, "embedding":{0:"N"}})
print("Exported aeolus_embedder.onnx")
