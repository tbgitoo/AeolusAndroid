
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
End-to-end sanity check with visible intermediate stages:
MIDI -> symbolic -> 256-d handcrafted -> 128-d ONNX student

Usage examples:
  python sanity_full_pipeline_ascii.py --midi_dir ./jsb_midi --take 5
  python sanity_full_pipeline_ascii.py --motifs motifs.txt --take 5
"""

import os
import sys
import glob
import argparse
import numpy as np

from features import feature_vector_from_symbolic, FEAT_DIM

# ------- MIDI -> symbolic helpers (music21) -------
def load_music21():
    try:
        import music21 as m21  # noqa: F401
        return True
    except Exception:
        return False

def midi_to_symbolic(midi_path, max_events=512):
    """
    Convert a MIDI file to the symbolic format:
      - melody as NOTE tokens with relative semitone steps (format: N{+/-d}:{R})
      - chord snapshots as CH[pc1,pc2,...]:{R}
      - rhythm label R in {S,M,L} derived by per-piece duration quantiles
    This is a pragmatic demo converter for sanity checking.
    """
    import music21 as m21

    s = m21.converter.parse(midi_path)
    fs = s.flat.notesAndRests.stream()
    notes = [n for n in fs if isinstance(n, m21.note.Note)]
    if len(notes) == 0:
        return ""

    events = []
    for n in notes:
        t = float(n.offset)
        pitch_midi = int(n.pitch.midi)
        dur = float(n.duration.quarterLength)
        events.append((t, pitch_midi, dur))
    events.sort(key=lambda x: (x[0], x[1]))

    durs = np.array([ev[2] for ev in events], dtype=np.float32)
    if len(durs) == 0:
        return ""
    if len(durs) >= 3:
        q1, q2 = np.quantile(durs, [0.33, 0.66])
    else:
        q1, q2 = float(np.min(durs)), float(np.max(durs))

    def dur_to_r(d):
        if d <= q1: return 'S'
        if d <= q2: return 'M'
        return 'L'

    tokens = []
    prev_pitch = events[0][1]

    by_onset = {}
    for t, p, d in events:
        by_onset.setdefault(float(t), []).append((p, d))

    for t in sorted(by_onset.keys()):
        bucket = by_onset[t]
        bucket.sort(key=lambda z: z[0])
        melody_p, melody_d = bucket[-1]

        delta = melody_p - prev_pitch
        r = dur_to_r(melody_d)
        tokens.append(f"N{delta:+d}:{r}")
        prev_pitch = melody_p

        pcs = sorted({(p % 12 + 12) % 12 for (p, d) in bucket})
        if len(pcs) >= 2:
            pcs_str = ",".join(str(pc) for pc in pcs[:5])
            tokens.append(f"CH[{pcs_str}]:{r}")

        if len(tokens) >= max_events:
            break

    return " ".join(tokens)

# ------- ONNX inference -------
def run_onnx_student(onnx_path, X):
    try:
        import onnxruntime as ort
    except Exception:
        print("ERROR: onnxruntime not installed. Install with:", file=sys.stderr)
        print("       pip install onnxruntime==1.18.0", file=sys.stderr)
        sys.exit(1)
    if not os.path.isfile(onnx_path):
        print(f"ERROR: ONNX model '{onnx_path}' not found. Train & export first.", file=sys.stderr)
        sys.exit(1)
    sess = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    out = sess.run(["embedding"], {"input": X.astype(np.float32)})[0]
    return out

# ------- Pretty printers -------
def preview_list(vals, n=6):
    return ", ".join(f"{float(v):.6f}" for v in vals[:n])

def preview_symbolic(sym, n_tokens=16, max_len=120):
    toks = sym.split()
    s = " ".join(toks[:n_tokens])
    if len(toks) > n_tokens:
        s += " ..."
    if len(s) > max_len:
        s = s[:max_len-3] + "..."
    return s

# ------- Main -------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--midi_dir", default=None, help="Directory with MIDI files to sample")
    ap.add_argument("--motifs", default=None, help="Fallback motifs.txt if no MIDI")
    ap.add_argument("--onnx", default="aeolus_embedder.onnx", help="Path to ONNX student")
    ap.add_argument("--take", type=int, default=5, help="How many items to show")
    ap.add_argument("--tokens_preview", type=int, default=16, help="How many symbolic tokens to show")
    ap.add_argument("--vec_preview", type=int, default=6, help="How many numbers to show from vectors")
    args = ap.parse_args()

    items = []  # list of dicts: {src, name, symbolic}

    # Prefer MIDI
    if args.midi_dir and os.path.isdir(args.midi_dir):
        if not load_music21():
            print("ERROR: music21 not installed. Install with:", file=sys.stderr)
            print("       pip install music21==8.1.0", file=sys.stderr)
            sys.exit(1)

        midi_paths = []
        for ext in ("*.mid", "*.midi", "*.MID", "*.MIDI"):
            midi_paths.extend(glob.glob(os.path.join(args.midi_dir, ext)))
        midi_paths = sorted(midi_paths)[:args.take]

        if len(midi_paths) == 0:
            print(f"WARNING: No MIDI files in '{args.midi_dir}'.", file=sys.stderr)

        for p in midi_paths:
            try:
                sym = midi_to_symbolic(p)
            except Exception as e:
                print(f"WARNING: Failed to parse MIDI '{p}': {e}", file=sys.stderr)
                sym = ""
            if sym.strip():
                items.append({"src":"midi", "name":os.path.basename(p), "symbolic":sym})
            if len(items) >= args.take:
                break

    # Fallback to motifs.txt
    if len(items) == 0:
        motifs_path = args.motifs or "motifs.txt"
        if not os.path.isfile(motifs_path):
            print("ERROR: No MIDI parsed and motifs.txt not found.", file=sys.stderr)
            print("       Provide --midi_dir or --motifs.", file=sys.stderr)
            sys.exit(1)
        with open(motifs_path, "r", encoding="utf-8") as f:
            lines = [l.strip() for l in f if l.strip()]
        for i, l in enumerate(lines[:args.take]):
            items.append({"src":"motif", "name":f"motif_{i}", "symbolic":l})

    if len(items) == 0:
        print("ERROR: No items to process.", file=sys.stderr)
        sys.exit(1)

    # 256-d handcrafted
    X = [feature_vector_from_symbolic(it["symbolic"]) for it in items]
    X = np.stack(X, axis=0).astype(np.float32)

    # 128-d student
    Z = run_onnx_student(args.onnx, X)

    # Print previews
    np.set_printoptions(suppress=True, precision=6)

    print(f"\nEnd-to-end sanity on {len(items)} items")
    print(f"Handcrafted features: {X.shape} (expected N x {FEAT_DIM})")
    print(f"Student embedding:    {Z.shape} (expected N x ~128)\n")

    for i, it in enumerate(items):
        sym_prev = preview_symbolic(it["symbolic"], n_tokens=args.tokens_preview)
        print(f"[{i}] source={it['src']}, name={it['name']}")
        print(f"    symbolic: {sym_prev}")
        print(f"    256-d[0:{args.vec_preview}]: [{preview_list(X[i], args.vec_preview)}]")
        print(f"    128-d[0:{args.vec_preview}]: [{preview_list(Z[i], args.vec_preview)}]")
        print()

    print("OK: MIDI -> symbolic -> 256 -> 128")

if __name__ == "__main__":
    main()
