
#!/usr/bin/env python3
"""
Sanity check: run the first 5 motifs through the ONNX student and print
the first few numbers of each embedding. This verifies end-to-end:
  motifs.txt -> handcrafted features (256-d) -> ONNX student -> embeddings
"""

import os
import sys
import numpy as np


from features import vectorize_lines

def main():
    data_path = "motifs.txt"
    onnx_path = "aeolus_embedder.onnx"
    n_take = 5           # how many lines to test
    n_preview = 6        # how many numbers to print from each embedding row

    # ---------- Preconditions ----------
    if not os.path.isfile(data_path):
        print(f"ERROR: '{data_path}' not found. Create it first (JSB -> motifs).", file=sys.stderr)
        sys.exit(1)

    if not os.path.isfile(onnx_path):
        print(f"ERROR: '{onnx_path}' not found. Train & export the student first.", file=sys.stderr)
        print("       Expected to be created by train_embedding.py", file=sys.stderr)
        sys.exit(1)

    # ---------- Load first N lines ----------
    with open(data_path, "r", encoding="utf-8") as f:
        lines = [l.strip() for l in f if l.strip()]

    if len(lines) == 0:
        print("ERROR: motifs.txt is empty.", file=sys.stderr)
        sys.exit(1)

    lines = lines[:min(n_take, len(lines))]

    # ---------- Handcrafted 256-d features ----------
    X = vectorize_lines(lines)  # shape [N, 256]
    if X.shape[0] == 0:
        print("ERROR: No valid lines parsed into features.", file=sys.stderr)
        sys.exit(1)

    # ---------- ONNX student inference ----------
    try:
        import onnxruntime as ort
    except Exception as e:
        print("ERROR: onnxruntime not available. Install with:", file=sys.stderr)
        print("       pip install onnxruntime==1.18.0", file=sys.stderr)
        sys.exit(1)

    sess = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    outs = sess.run(["embedding"], {"input": X.astype(np.float32)})
    emb = outs[0]  # shape [N, D], typically D=128 from Student

    # ---------- Pretty print ----------
    np.set_printoptions(suppress=True, precision=6)

    print(f"\nSanity check on first {emb.shape[0]} motifs "
          f"(embedding dim = {emb.shape[1]}):\n")

    for i, (raw, v) in enumerate(zip(lines, emb)):
        preview = ", ".join(f"{z:.6f}" for z in v[:n_preview])
        # Keep the original line short in logs
        short = raw if len(raw) <= 80 else (raw[:77] + "...")
        print(f"[{i}] {short}")
        print(f"     embedding[0:{n_preview}] = [{preview}]")
        print()

    print("✅ End-to-end embedding succeeded.")

if __name__ == "__main__":
    main()
