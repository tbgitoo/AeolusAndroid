package com.mathis.midiSynth.similaritySearch;





import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FeatureExtractor
 * ----------------
 * Input:  one symbolic motif string (see format in the header).
 * Output: float[256] feature vector (L2-normalized).
 *
 * Block layout (in order; total 256):
 *   A  Δ-root histogram in [-6..+6]                       13
 *   B  chord-class histogram (16 classes)                 16
 *   C  rhythm unigrams (3) + bigrams (9)                  12
 *   D  interval n-grams on clamp([-2..+2]): bi(25)+tri(125)=150
 *   E  stats (31)                                         31
 *   F  |Δ| histogram bins 0..5                             6
 *   G  direction bigrams (3x3 without (0,0))               8
 *   P  PAD zeros                                           20
 *  -----------------------------------------------------------
 *   TOTAL                                                256
 *
 * Notes:
 * - Histograms use sqrt-count + L1 normalization per block.
 * - Final 256-D vector is L2-normalized.
 * - PAD(20) are zeros reserved for future tweaks; harmless for the student.
 */
public final class FeatureExtractor {

    private FeatureExtractor(){}

    // ---- sizes & offsets ----
    private static final int A = 13;                         // Δ-root hist [-6..+6]
    private static final int B = 16;                         // chord class (16)
    private static final int C = 12;                         // rhythm uni(3)+bi(9)
    private static final int D = 150;                        // interval n-grams
    private static final int E = 31;                         // stats
    private static final int F = 6;                          // |Δ| 0..5
    private static final int G = 8;                          // dir-bigram without (0,0)
    private static final int P = 20;                         // pad

    private static final int OFF_A = 0;
    private static final int OFF_B = OFF_A + A;
    private static final int OFF_C = OFF_B + B;
    private static final int OFF_D = OFF_C + C;
    private static final int OFF_E = OFF_D + D;
    private static final int OFF_F = OFF_E + E;
    private static final int OFF_G = OFF_F + F;
    private static final int OFF_P = OFF_G + G;
    private static final int FEAT = OFF_P + P;               // 256

    // ---- public API ----
    public static float[] from(String symbolic) {
        List<Onset> onsets = Parser.parse(symbolic);
        float[] x = new float[FEAT];

        if (onsets.isEmpty()) return x;

        // Build roots, rhythms, chord pcs
        List<Integer> roots = new ArrayList<>();
        List<Character> rhythms = new ArrayList<>();
        List<int[]> chordPcs = new ArrayList<>();

        int currentRoot = 0; // start at 0
        for (Onset o : onsets){
            if (o.type == Onset.Type.NOTE){
                currentRoot += o.noteDelta; // absolute (running) root
            } // chord keeps the currentRoot as-is
            roots.add(currentRoot);
            rhythms.add(o.rhythm);
            chordPcs.add(o.chordPcs);
        }

        // Δ-root stream (nearest octave wrap, then clip to [-6..+6] only for A; D uses clamp[-2..+2])
        List<Integer> dRoot = new ArrayList<>();
        for (int i=1;i<roots.size();i++){
            int d = wrapNearest12(roots.get(i) - roots.get(i-1));
            dRoot.add(d);
        }

        // ---- A: Δ-root hist [-6..+6] (13) ----
        int[] a = new int[A];
        for (int d : dRoot) a[d + 6]++; // bins 0..12 map to -6..+6
        writeHistSqrtL1(a, x, OFF_A);

        // ---- B: chord class hist (16) ----
        int[] b = new int[B];
        for (int i=0;i<onsets.size();i++){
            int[] pcs = chordPcs.get(i);
            if (pcs != null){
                b[ChordClass.map(pcs)]++;
            }
        }
        writeHistSqrtL1(b, x, OFF_B);

        // ---- C: rhythm unigrams+ bigrams (3 + 9) ----
        int[] c = new int[C];
        int s=0,m=0,l=0;
        for (char r : rhythms){
            if (r=='S') s++; else if (r=='M') m++; else l++;
        }
        c[0]=s; c[1]=m; c[2]=l;
        for (int i=1;i<rhythms.size();i++){
            int p = rIndex(rhythms.get(i-1));
            int q = rIndex(rhythms.get(i));
            c[3 + p*3 + q]++;
        }
        writeHistSqrtL1(c, x, OFF_C);

        // ---- D: interval n-grams (bi 25 + tri 125) on clamp[-2..+2] ----
        int[] d = new int[D];
        List<Integer> d2 = new ArrayList<>(dRoot.size());
        for (int v : dRoot) d2.add(clamp(v, -2, 2));
        // bigrams
        for (int i=1;i<d2.size();i++){
            int a2 = d2.get(i-1)+2, b2 = d2.get(i)+2; // 0..4
            d[a2*5 + b2]++;
        }
        // trigrams
        int offTri = 25;
        for (int i=2;i<d2.size();i++){
            int a2 = d2.get(i-2)+2, b2 = d2.get(i-1)+2, c2 = d2.get(i)+2; // 0..4
            d[offTri + a2*25 + b2*5 + c2]++;
        }
        writeHistSqrtL1(d, x, OFF_D);

        // ---- E: stats (31) ----
        float[] e = StatsBlock.compute(onsets, dRoot, rhythms);
        System.arraycopy(e, 0, x, OFF_E, E);

        // ---- F: |Δ| bins 0..5 (6) ----
        int[] f = new int[F];
        for (int v : dRoot){
            int bin = Math.min(5, Math.abs(v));
            f[bin]++;
        }
        writeHistSqrtL1(f, x, OFF_F);

        // ---- G: direction bigrams over {-,0,+} without (0,0) (8) ----
        int[] g = new int[G];
        for (int i=1;i<dRoot.size();i++){
            int di = dir(dRoot.get(i-1))+1; // -1,0,+1 -> 0,1,2
            int dj = dir(dRoot.get(i))+1;
            int full = di*3 + dj;           // 0..8
            int idx = RemapDirBigram.compactIndex(full); // maps all except (1,1)
            if (idx >= 0) g[idx]++;
        }
        writeHistSqrtL1(g, x, OFF_G);

        // ---- P: PAD zeros (20) ----
        // already zeros

        // ---- final L2 normalize 256-D vector ----
        l2Normalize(x);
        return x;
    }

    // ---------- helpers ----------
    private static int clamp(int v, int lo, int hi){
        return (v<lo)? lo : (v>hi)? hi : v;
    }
    private static int rIndex(char r){ return (r=='S')?0 : (r=='M')?1 : 2; }
    private static int dir(int d){ return (d<0)? -1 : (d>0)? +1 : 0; }

    /** Wrap to nearest octave then return as-is (may exceed [-6..+6]; A uses clip). */
    private static int wrapNearest12(int x){
        // reduce to the closest representative modulo 12
        int k = Math.round(x / 12f);
        return x - 12*k;
    }

    private static void writeHistSqrtL1(int[] counts, float[] dest, int off){
        double s = 0.0;
        for (int c : counts) s += Math.sqrt(c);
        if (s <= 1e-12){
            Arrays.fill(dest, off, off+counts.length, 0f);
            return;
        }
        float inv = (float)(1.0 / s);
        for (int i=0;i<counts.length;i++){
            dest[off+i] = (float)(Math.sqrt(counts[i]) * inv);
        }
    }

    private static void l2Normalize(float[] v){
        double n=0; for (float t : v) n += (double)t*t;
        if (n <= 1e-12) return;
        float s = (float)(1.0/Math.sqrt(n));
        for (int i=0;i<v.length;i++) v[i] *= s;
    }

    // ---------- nested: domain objects & parser ----------
    static final class Onset {
        enum Type { NOTE, CHORD }
        final Type type;
        final int noteDelta;   // for NOTE: ±k; for CHORD: 0
        final int[] chordPcs;  // for CHORD: pitch-class offsets; for NOTE: null
        final char rhythm;     // 'S' | 'M' | 'L'
        Onset(Type t, int noteDelta, int[] chordPcs, char rhythm){
            this.type=t; this.noteDelta=noteDelta; this.chordPcs=chordPcs; this.rhythm=rhythm;
        }
    }

    static final class Parser {
        private static final Pattern NOTE = Pattern.compile("^N([+-]?\\d+):([SML])$");
        private static final Pattern CHORD = Pattern.compile("^CH\\[([0-9,\\- ]+)\\]:([SML])$");

        static List<Onset> parse(String s){
            List<Onset> out = new ArrayList<>();
            if (s == null) return out;
            String[] toks = s.trim().split("\\s+");
            for (String t : toks){
                if (t.isEmpty()) continue;

                Matcher mn = NOTE.matcher(t);
                if (mn.matches()){
                    int delta = Integer.parseInt(mn.group(1));
                    char r = mn.group(2).charAt(0);
                    out.add(new Onset(Onset.Type.NOTE, delta, null, r));
                    continue;
                }
                Matcher mc = CHORD.matcher(t);
                if (mc.matches()){
                    String[] parts = mc.group(1).split(",");
                    List<Integer> pcs = new ArrayList<>(parts.length);
                    for (String p : parts){
                        String z = p.trim();
                        if (z.isEmpty()) continue;
                        int v = ((Integer.parseInt(z) % 12) + 12) % 12;
                        pcs.add(v);
                    }
                    char r = mc.group(2).charAt(0);
                    int[] arr = new int[pcs.size()];
                    for (int i=0;i<pcs.size();i++) arr[i]=pcs.get(i);
                    Arrays.sort(arr);
                    out.add(new Onset(Onset.Type.CHORD, 0, arr, r));
                    continue;
                }
                // ignore unknown tokens silently
            }
            return out;
        }
    }

    // ---------- chord class mapping (16 classes) ----------
    static final class ChordClass {
        // 16 canonical classes defined as sorted pcs relative to root
        private static final int[][] CLASSES = {
                {0,4,7},      // 0 maj
                {0,3,7},      // 1 min
                {0,3,6},      // 2 dim
                {0,4,8},      // 3 aug
                {0,2,7},      // 4 sus2
                {0,5,7},      // 5 sus4
                {0,7},        // 6 pow5
                {0,4,7,11},   // 7 maj7
                {0,3,7,10},   // 8 min7
                {0,4,7,10},   // 9 dom7
                {0,3,6,9},    //10 dim7
                {0,3,7,11},   //11 minMaj7
                {0,3,6,10},   //12 halfDim7
                {0,2,4,7},    //13 add2/9
                {0,4,5,7},    //14 add4
                {0,4,7,9}     //15 add6
        };

        static int map(int[] pcsSorted){
            for (int i=0;i<CLASSES.length;i++){
                if (Arrays.equals(pcsSorted, CLASSES[i])) return i;
            }
            return 15; // map "other" to the last class if you prefer; or create explicit OTHER
        }
    }

    // ---------- stats block (31 dims in [0,1]) ----------
    static final class StatsBlock {
        static float[] compute(List<Onset> onsets, List<Integer> dRoot, List<Character> rhythms){
            float[] e = new float[E];
            int idx = 0;

            int total = onsets.size();
            int chords = 0; int repeated=0; int leaps=0;
            int neg=0, zero=0, pos=0;

            // Δ stats
            List<Integer> absList = new ArrayList<>(dRoot.size());
            for (int v : dRoot){
                int a = Math.abs(v); absList.add(a);
                if (a==0) repeated++;
                if (a>=3) leaps++;
                int d = dir(v);
                if (d<0) neg++; else if (d>0) pos++; else zero++;
            }

            // 1) total_onsets / 64 (cap)
            e[idx++] = Math.min(1f, total/64f);

            // 2) fraction chords
            for (Onset o : onsets) if (o.type==Onset.Type.CHORD) chords++;
            e[idx++] = (total>0)? (chords/(float)total) : 0f;

            // 3) share_repeated_notes
            e[idx++] = (dRoot.size()>0)? (repeated/(float)dRoot.size()) : 0f;

            // 4) share_leaps (|Δ|>=3)
            e[idx++] = (dRoot.size()>0)? (leaps/(float)dRoot.size()) : 0f;

            // 5) entropy of A (normalized by ln(13))
            e[idx++] = normalizedEntropy(histDelta(dRoot, -6, 6), 13);

            // 6) entropy of rhythm unigrams (normalized by ln(3))
            int[] rUni = new int[3];
            for (char r : rhythms) rUni[rIndex(r)]++;
            e[idx++] = normalizedEntropy(rUni, 3);

            // 7) max run same direction / total
            e[idx++] = (total>0)? (maxRunDirection(dRoot)/ (float)Math.max(1, total)) : 0f;

            // 8) max run same rhythm / total
            e[idx++] = (total>0)? (maxRunRhythm(rhythms)/ (float)Math.max(1, total)) : 0f;

            // 9..13) chord size histogram (1,2,3,4,>=5) normalized by chord count
            int[] sizeBins = new int[]{0,0,0,0,0};
            int chordOnsets=0;
            for (Onset o : onsets){
                if (o.type==Onset.Type.CHORD){
                    chordOnsets++;
                    int n = o.chordPcs.length;
                    int bi = (n<=4)? (n-1) : 4;
                    sizeBins[bi]++;
                }
            }
            for (int k=0;k<5;k++){
                e[idx++] = (chordOnsets>0)? (sizeBins[k]/(float)chordOnsets) : 0f;
            }

            // 14..22) rhythm bigram distribution (9) normalized
            int[] rBi = new int[9];
            for (int i=1;i<rhythms.size();i++){
                int p = rIndex(rhythms.get(i-1)), q = rIndex(rhythms.get(i));
                rBi[p*3 + q]++;
            }
            float sumBi=0f; for (int v : rBi) sumBi += v;
            for (int i2=0;i2<9;i2++){
                e[idx++] = (sumBi>0f)? (rBi[i2]/sumBi) : 0f;
            }

            // 23) mean(|Δ|)/6, 24) std(|Δ|)/6
            float meanAbs=0f; for (int a : absList) meanAbs+=a;
            meanAbs = (dRoot.size()>0)? (meanAbs/dRoot.size()) : 0f;
            float var=0f; for (int a: absList){ float d=(a-meanAbs); var+=d*d; }
            float std = (absList.size()>0)? (float)Math.sqrt(var/absList.size()) : 0f;
            e[idx++] = meanAbs/6f;
            e[idx++] = std/6f;

            // 25..27) direction shares (neg, zero, pos)
            int denom = Math.max(1, dRoot.size());
            e[idx++] = neg/(float)denom;
            e[idx++] = zero/(float)denom;
            e[idx++] = pos/(float)denom;

            // 28) fraction of S, 29) fraction of M (L is implied)
            e[idx++] = (total>0)? (rUni[0]/(float)total) : 0f;  // S
            e[idx++] = (total>0)? (rUni[1]/(float)total) : 0f;  // M

            // 30) fraction of triads among chords (pcs==3)
            int tri=0; for (Onset o : onsets) if (o.type==Onset.Type.CHORD && o.chordPcs.length==3) tri++;
            e[idx++] = (chordOnsets>0)? (tri/(float)chordOnsets) : 0f;

            // 31) average chord size / 6
            float avgSize=0f; for (Onset o : onsets) if (o.type==Onset.Type.CHORD) avgSize += o.chordPcs.length;
            e[idx++] = (chordOnsets>0)? ((avgSize/chordOnsets)/6f) : 0f;

            // safety
            for (int i=0;i<E;i++){
                if (Float.isNaN(e[i]) || Float.isInfinite(e[i])) e[i]=0f;
                if (e[i] < 0f) e[i]=0f;
                if (e[i] > 1f) e[i]=1f;
            }
            return e;
        }

        private static int[] histDelta(List<Integer> dRoot, int lo, int hi){
            int n = hi - lo + 1;
            int[] h = new int[n];
            for (int v : dRoot){
                int k = v;
                // clip to [-6..+6] for entropy purpose
                if (k < lo) k = lo; else if (k > hi) k = hi;
                h[k - lo]++;
            }
            return h;
        }

        private static float normalizedEntropy(int[] h, int bins){
            float sum=0f; for (int v : h) sum += v;
            if (sum <= 0f) return 0f;
            double H=0.0;
            for (int v : h){
                if (v==0) continue;
                double p = v / (double)sum;
                H += -p * Math.log(p);
            }
            double Hmax = Math.log(bins);
            return (float)((Hmax>0)? (H/Hmax) : 0.0);
        }

        private static int maxRunRhythm(List<Character> r){
            if (r.isEmpty()) return 0;
            int best=1, cur=1;
            for (int i=1;i<r.size();i++){
                if (r.get(i)==r.get(i-1)) cur++;
                else { best = Math.max(best, cur); cur=1; }
            }
            return Math.max(best, cur);
        }
        private static int maxRunDirection(List<Integer> d){
            if (d.isEmpty()) return 0;
            int best=1, cur=1, pd = dir(d.get(0));
            for (int i=1;i<d.size();i++){
                int cd = dir(d.get(i));
                if (cd==pd) cur++; else { best = Math.max(best, cur); cur=1; pd=cd; }
            }
            return Math.max(best, cur);
        }
    }

    // ---------- map 3x3 direction bigrams to 8 bins (drop (0,0)) ----------
    static final class RemapDirBigram {
        // full index = (di+1)*3 + (dj+1) where di,dj in {-1,0,+1} -> 0..8
        // We return compact 0..7 or -1 for (0,0)
        private static final int[] FULL2COMPACT = {
                0, 1, 2,   // (-1,-1),( -1, 0),( -1,+1)
                3, -1, 4,  // ( 0,-1), ( 0, 0),( 0,+1)
                5, 6, 7    // (+1,-1),( +1, 0),( +1,+1)
        };
        static int compactIndex(int fullIndex){
            if (fullIndex<0 || fullIndex>8) return -1;
            return FULL2COMPACT[fullIndex];
        }
    }
}


