
# aeolus_embed/datasets.py
import random
import torch
from torch.utils.data import Dataset
from .features import feature_vector_from_symbolic

class SymDataset(Dataset):
    def __init__(self, lines):
        self.syms = [l.strip() for l in lines if l.strip()]

    def __len__(self):
        return len(self.syms)

    def _augment(self, s):
        # very mild augmentation: local token swap
        toks = s.split()
        if len(toks) >= 3 and random.random() < 0.5:
            i = random.randint(0, len(toks)-2)
            toks[i], toks[i+1] = toks[i+1], toks[i]
        return " ".join(toks)

    def __getitem__(self, idx):
        s = self.syms[idx]
        s2 = self._augment(s)
        x1 = feature_vector_from_symbolic(s)
        x2 = feature_vector_from_symbolic(s2)
        return torch.from_numpy(x1).float(), torch.from_numpy(x2).float()
