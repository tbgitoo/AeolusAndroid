
# train_embedding.py
# pip install torch==2.2.2 numpy==1.26.4 onnx onnxruntime==1.18.0
from aeolus_embed.datasets import SymDataset
from aeolus_embed.model import train_contrastive, export_onnx

if __name__ == "__main__":
    DATA_PATH = "motifs.txt"
    with open(DATA_PATH, "r", encoding="utf-8") as f:
        lines = [l.strip() for l in f if l.strip()]

    ds = SymDataset(lines)
    model = train_contrastive(ds, epochs=10)
    out = export_onnx(model, "aeolus_embedder.onnx", in_dim=256)
    print(f"✅ Exported {out}")
