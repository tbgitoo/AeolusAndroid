
## Training data (motifs.txt)

This project trains a contrastive embedding model on symbolic musical motifs.

`motifs.txt` is a **generated file** and is intentionally **not included** in the repository.

It can be reproduced by running the provided data-preparation script (see "run instructions for model training.txt").
This script uses the 
the music21 toolkit to extract abstract symbolic motifs (intervals, chord classes,
rhythm categories) from musical scores.

The reference data sources used during development were **public-domain scores**
(e.g., J. S. Bach chorales). No musical scores or melodies are distributed with
this repository.

music21 is BSD-licensed:
https://github.com/cuthbertLab/music21


## Pretrained embedding model (aeolus_embedder.onnx)

`aeolus_embedder.onnx` is a pretrained neural embedding model that maps
256‑dimensional symbolic feature vectors to 128‑dimensional unit‑normalized
embeddings.

The model was trained using contrastive learning on abstract symbolic motifs
(intervals, chord classes, and rhythm categories) derived from public‑domain
musical works (e.g., J. S. Bach chorales).

No musical scores, note sequences, or audio data are included in this repository.
The model cannot reproduce or reconstruct musical works.
