
# aeolus_embed/model.py
import torch, torch.nn as nn, torch.nn.functional as F
from torch.utils.data import DataLoader

class Student(nn.Module):
    def __init__(self, in_dim=256, hid=128, out_dim=128):
        super().__init__()
        self.fc1 = nn.Linear(in_dim, hid)
        self.fc2 = nn.Linear(hid, out_dim)
    def forward(self, x):
        z = F.relu(self.fc1(x))
        z = self.fc2(z)
        return F.normalize(z, dim=-1, eps=1e-12)

def train_contrastive(dataset, epochs=10, lr=2e-3, temp=0.07, batch_size=128):
    dl = DataLoader(dataset, batch_size=batch_size, shuffle=True)
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = Student().to(device)
    opt = torch.optim.AdamW(model.parameters(), lr=lr)

    for ep in range(1, epochs+1):
        model.train()
        total = 0.0
        for x1, x2 in dl:
            x1 = x1.to(device)
            x2 = x2.to(device)
            z1 = model(x1)
            z2 = model(x2)
            logits = (z1 @ z2.t()) / temp
            labels = torch.arange(z1.size(0), device=device)
            loss = (F.cross_entropy(logits, labels) +
                    F.cross_entropy(logits.t(), labels)) * 0.5
            opt.zero_grad()
            loss.backward()
            opt.step()
            total += loss.item() * x1.size(0)
        print(f"ep{ep}: train {total/len(dataset):.4f}")
    model.eval()
    return model.cpu()

def export_onnx(model, out_path="aeolus_embedder.onnx", in_dim=256):
    dummy = torch.zeros(1, in_dim, dtype=torch.float32)
    torch.onnx.export(
        model,
        dummy,
        out_path,
        input_names=["input"],
        output_names=["embedding"],
        opset_version=13,
        dynamic_axes={"input": {0: "N"}, "embedding": {0: "N"}}
    )
    return out_path
