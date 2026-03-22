
# build_jsb_motifs_music21.py
# Creates motifs.txt (one symbolic motif per chorale) from the music21 Bach corpus.
#
# Usage:
#   python build_jsb_motifs_music21.py --out motifs.txt
#
# Dependencies:
#   pip install music21

import argparse, sys
from music21 import corpus, converter, note, chord, tempo

def nearest_octave_wrap(delta_semitones: int) -> int:
    # Map delta to nearest representative modulo 12 (e.g., +14 → +2, −13 → −1)
    k = round(delta_semitones / 12.0)
    return int(delta_semitones - 12 * k)

def rhythm_bucket(ioi_beats: float | None) -> str:
    # IOI in beats: <0.33 → S, <0.75 → M, else L
    if ioi_beats is None:
        return 'M'
    if ioi_beats < 0.33: return 'S'
    if ioi_beats < 0.75: return 'M'
    return 'L'

def get_qpm(stream):
    # Use first MetronomeMark if present; else default 120 QPM.
    # music21 exposes metronome marks via metronomeMarkBoundaries / stream.metronomeMarkBoundaries()
    try:
        for mm, start in stream.metronomeMarkBoundaries():
            if isinstance(mm, tempo.MetronomeMark) and mm.number:
                return float(mm.number)
    except Exception:
        pass
    return 120.0

def onset_groups(stream):
    """
    Returns [(offset_quarters, [midi pitches])]
    Chorales are quantized; grouping on quarterLength offsets is stable.
    """
    g = {}
    for n in stream.flat.notes:
        off = float(n.offset)
        if isinstance(n, note.Note):
            g.setdefault(off, set()).add(int(n.pitch.midi))
        elif isinstance(n, chord.Chord):
            for p in n.pitches:
                g.setdefault(off, set()).add(int(p.midi))
    groups = [(off, sorted(list(pitches))) for off, pitches in sorted(g.items(), key=lambda z: z[0])]
    return groups

def symbolic_from_groups(groups, qpm):
    if not groups:
        return ""
    sec_per_beat = 60.0 / qpm  # quarter = beat

    # Convert offsets (quarters) to seconds for IOI; then back to beats for bucketing
    groups_sec = [(off_q, off_q * sec_per_beat, pitches) for (off_q, pitches) in groups]

    tokens = []
    prev_root = None
    prev_sec = None

    for off_q, t_sec, pitches in groups_sec:
        ioi_beats = None if prev_sec is None else max(1e-9, (t_sec - prev_sec) / sec_per_beat)
        R = rhythm_bucket(ioi_beats)

        if len(pitches) == 1:
            this_root = pitches[0]
            delta = 0 if prev_root is None else nearest_octave_wrap(this_root - prev_root)
            tokens.append(f"N{delta:+d}:{R}")
            prev_root = this_root
        else:
            root = pitches[0]  # lowest note as chord "root"
            pcs = sorted({(p - root) % 12 for p in pitches})
            tokens.append(f"CH[{','.join(str(x) for x in pcs)}]:{R}")
            if prev_root is None:
                prev_root = root

        prev_sec = t_sec

    return " ".join(tokens)

def main():
    ap = argparse.ArgumentParser(description="Build motifs.txt from music21's Bach chorales corpus")
    ap.add_argument("--out", default="motifs.txt", help="Output text file (one motif per line)")
    args = ap.parse_args()

    out_lines = 0
    # Get all entries for 'bach' from the corpus and filter to common symbolic formats
    entries = corpus.getComposer('bach')  # list of corpus IDs or paths
    with open(args.out, "w", encoding="utf-8") as f:
        for entry in entries:
            name = str(entry)
            if not name.lower().endswith((".mxl", ".xml", ".musicxml", ".krn", ".mid", ".midi")):
                continue
            try:
                s = corpus.parse(entry)  # load via music21 corpus
                qpm = get_qpm(s)
                groups = onset_groups(s)
                line = symbolic_from_groups(groups, qpm)
                if line:
                    f.write(line + "\n")
                    out_lines += 1
            except Exception as e:
                # Not every "bach" entry is a chorale; skip others quietly
                sys.stderr.write(f"[skip] {name}: {e}\n")
    print(f"Wrote {out_lines} lines to {args.out}")

if __name__ == "__main__":
    main()
