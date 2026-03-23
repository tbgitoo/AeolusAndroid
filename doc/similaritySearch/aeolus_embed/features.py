
# aeolus_embed/features.py
import re, math
import numpy as np

# -------------------------------
# Parser & small utils (unchanged)
# -------------------------------
NOTE_RE  = re.compile(r"^N([+-]?\d+):([SML])$")
CHORD_RE = re.compile(r"^CH\[([0-9,\- ]+)\]:([SML])$")

def parse_symbolic(s: str):
    """Parse symbolic line into list of onsets: {type:'N'|'C', delta:int, pcs:list|None, r:'S'|'M'|'L'}"""
    out = []
    if not s:
        return out
    for t in s.strip().split():
        m = NOTE_RE.match(t)
        if m:
            out.append({'type':'N','delta':int(m.group(1)),'pcs':None,'r':m.group(2)})
            continue
        m = CHORD_RE.match(t)
        if m:
            pcs = [((int(z.strip()) % 12) + 12) % 12 for z in m.group(1).split(',') if z.strip()]
            pcs.sort()
            out.append({'type':'C','delta':0,'pcs':pcs,'r':m.group(2)})
    return out

def wrap_nearest12(x):
    return x - 12 * int(round(x / 12.0))

def r_index(r): return 0 if r=='S' else (1 if r=='M' else 2)
def clamp(v,lo,hi): return lo if v<lo else (hi if v>hi else v)

# Chord classes (16)
CH_CLASSES = [
    [0,4,7],[0,3,7],[0,3,6],[0,4,8],[0,2,7],[0,5,7],[0,7],
    [0,4,7,11],[0,3,7,10],[0,4,7,10],[0,3,6,9],[0,3,7,11],
    [0,3,6,10],[0,2,4,7],[0,4,5,7],[0,4,7,9]
]
def chord_class_map(pcs):
    for i,cls in enumerate(CH_CLASSES):
        if pcs == cls: return i
    return 15

FULL2COMPACT = np.array([0,1,2, 3,-1,4, 5,6,7], dtype=np.int32)

# -------------------------------
# Helpers
# -------------------------------
def _write_hist_sqrt_l1(h, dest, off):
    """L1-normalize then sqrt (Hellinger)."""
    s = float(np.sum(h))
    if s > 1e-12:
        dest[off:off+len(h)] = np.sqrt(h / s, dtype=np.float32)
    else:
        dest[off:off+len(h)] = 0.0

def _entropy_norm(h, bins):
    s = float(np.sum(h))
    if s <= 0.0:
        return 0.0
    p = h / s
    p = p[p > 0]
    H = float(-(p * np.log(p)).sum())
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

# -------------------------------
# Stats block
# -------------------------------
def _stats_block(onsets, dRoot, rhythms):
    E=31; e=np.zeros(E, np.float32); idx=0
    total=len(onsets); chords=sum(1 for o in onsets if o['type']=='C')
    # 1 total/64
    e[idx] = min(1.0, total/64.0); idx+=1
    # 2 fraction chords
    e[idx] = (chords/total) if total>0 else 0.0; idx+=1
    # 3 share repeated (|Δ|==0)
    rep=sum(1 for v in dRoot if abs(v)==0); denom=max(1,len(dRoot))
    e[idx] = rep/denom; idx+=1
    # 4 share leaps (|Δ|>=3)
    leap=sum(1 for v in dRoot if abs(v)>=3)
    e[idx] = leap/denom; idx+=1
    # 5 entropy Δ in [-6..+6]
    h=np.zeros(13, np.float32)
    for v in dRoot: h[max(-6,min(6,v))+6]+=1
    e[idx] = _entropy_norm(h, 13); idx+=1
    # 6 rhythm unigram entropy
    rUni=np.zeros(3, np.float32)
    for r in rhythms: rUni[r_index(r)]+=1
    e[idx] = _entropy_norm(rUni, 3); idx+=1
    # 7 max run same direction / total
    e[idx] = _max_run_direction(dRoot)/max(1,total); idx+=1
    # 8 max run same rhythm / total
    e[idx] = _max_run_rhythm(rhythms)/max(1,total); idx+=1
    # 9..13 chord size hist 1,2,3,4,>=5
    size=np.zeros(5, np.float32); chord_onsets=0
    for o in onsets:
        if o['type']=='C':
            chord_onsets+=1
            n=len(o['pcs']); bi = n-1 if n<=4 else 4
            size[bi]+=1
    size = (size/chord_onsets) if chord_onsets>0 else size
    e[idx:idx+5]=size; idx+=5
    # 14..22 rhythm bigrams 9 (L1)
    rBi=np.zeros(9, np.float32)
    for i in range(1,len(rhythms)):
        p=r_index(rhythms[i-1]); q=r_index(rhythms[i]); rBi[p*3+q]+=1
    s=rBi.sum(); e[idx:idx+9] = (rBi/s) if s>0 else 0.0; idx+=9
    # 23 mean(|Δ|)/6, 24 std(|Δ|)/6
    abs_list=np.array([abs(v) for v in dRoot], np.float32)
    mean = abs_list.mean() if len(abs_list)>0 else 0.0
    std  = abs_list.std()  if len(abs_list)>0 else 0.0
    e[idx] = mean/6.0; idx+=1
    e[idx] = std/6.0; idx+=1
    # 25..27 direction shares
    neg=sum(1 for v in dRoot if v<0); zer=sum(1 for v in dRoot if v==0); pos=sum(1 for v in dRoot if v>0)
    e[idx:idx+3] = np.array([neg,zer,pos], np.float32)/denom; idx+=3
    # 28 fraction S, 29 fraction M
    e[idx] = (rUni[0]/total) if total>0 else 0.0; idx+=1
    e[idx] = (rUni[1]/total) if total>0 else 0.0; idx+=1
    # 30 fraction triads
    tri=sum(1 for o in onsets if o['type']=='C' and len(o['pcs'])==3)
    e[idx] = (tri/chord_onsets) if chord_onsets>0 else 0.0; idx+=1
    # 31 avg chord size / 6
    avg = (sum(len(o['pcs']) for o in onsets if o['type']=='C')/chord_onsets) if chord_onsets>0 else 0.0
    e[idx] = avg/6.0; idx+=1
    return e

# -------------------------------
# Feature extractor (256-dim)
# -------------------------------
FEAT_DIM = 256

def feature_vector_from_symbolic(s: str) -> np.ndarray:
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
    s_cnt = sum(1 for r in rhythms if r=='S')
    m_cnt = sum(1 for r in rhythms if r=='M')
    l_cnt = sum(1 for r in rhythms if r=='L')
    c[0],c[1],c[2] = s_cnt,m_cnt,l_cnt
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
    e = _stats_block(onsets, dRoot, rhythms)
    x[OFF_E:OFF_E+31] = e

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

    # PAD already zeros (20 dims)
    # L2
    n = float(np.linalg.norm(x))
    if n>1e-12: x/=n
    return x

def vectorize_lines(lines):
    """Convenience: lines[str] -> np.ndarray [N, 256]"""
    xs = [feature_vector_from_symbolic(l.strip()) for l in lines if l.strip()]
    if not xs:
        return np.zeros((0, FEAT_DIM), np.float32)
    return np.stack(xs, axis=0).astype(np.float32)
