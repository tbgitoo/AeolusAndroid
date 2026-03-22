
package com.mathis.midiSynth.similaritySearch;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Static utility to convert raw MIDI NoteOn/NoteOff events into a compact,
 * tempo- and transposition-invariant symbolic string.
 *
 * Input: List<MidiEvent> as defined in com.mathis.midiSynth.similaritySearch.MidiEvent
 *   - note:       0..127
 *   - timestamp:  uptimeMs (or any monotonic ms)
 *   - isNoteOn:   true for NoteOn, false for NoteOff
 *
 * Output example:
 *   "E0: CH[0,4,7]-M | E1: N+2-S | E2: N-1-S | E3: CH[0,3,7]-L"
 *
 * Conventions:
 *   CH[...]      chord onset (intervals from lowest note, mod 12)
 *   N±k          single-note onset, interval vs previous onset anchor (nearest octave, [-6..+6])
 *   -S/-M/-L     rhythm bucket from normalized IOI (optional)
 */
public final class SymbolicEncoder {

    private SymbolicEncoder() {}

    // -------- Tunable knobs (can be made public or driven via a Config later) --------
    private static final long  TIME_BIN_MS        = 25;   // NOTE_ONs within this window = same onset
    private static final long  DUP_ON_DEBOUNCE_MS = 3;    // ignore re-triggers of same note within this window
    private static final double IOI_SHORT         = 0.75; // IOI < 0.75 * median  => S
    private static final double IOI_LONG          = 1.50; // IOI > 1.50 * median  => L
    private static final boolean ADD_RHYTHM_BUCKETS = true;
    private static final boolean ADD_EVENT_LABELS   = true; // "E0:", "E1:", ...

    // -------- Public API --------
    public static String encode(List<MidiEvent> rawEvents) {
        if (rawEvents == null || rawEvents.isEmpty()) return "";

        // Defensive copy & stable sort: time asc, On before Off at equal time
        List<MidiEvent> events = new ArrayList<>(rawEvents);
        events.sort((a, b) -> {
            int t = Long.compare(a.timestamp, b.timestamp);
            if (t != 0) return t;
            return Boolean.compare(!a.isNoteOn, !b.isNoteOn);
        });

        // Rebase time
        long t0 = events.get(0).timestamp;
        List<MidiEvent> rebased = new ArrayList<>(events.size());
        for (MidiEvent e : events) {
            rebased.add(new MidiEvent(e.note, e.timestamp - t0, e.isNoteOn));
        }

        // Build onsets & attach per-note durations
        List<Onset> onsets = buildOnsets(rebased);
        if (onsets.isEmpty()) return "";

        // Compute IOIs & median IOI for normalization
        for (int i = 1; i < onsets.size(); i++) {
            onsets.get(i).ioiMs = Math.max(0, onsets.get(i).t - onsets.get(i - 1).t);
        }
        long medianIoi = median(onsets.stream().skip(1).map(o -> o.ioiMs).collect(Collectors.toList()));
        if (medianIoi <= 0) medianIoi = 1;

        // Encode sequence
        StringBuilder out = new StringBuilder();
        Integer lastAnchor = null; // lowest note of previous onset

        for (int i = 0; i < onsets.size(); i++) {
            Onset o = onsets.get(i);
            Collections.sort(o.notes);                 // canonical order
            int root = o.notes.get(0);

            String token;
            if (o.notes.size() == 1) {
                // Single note onset: signed semitone interval vs previous anchor, wrapped to nearest octave
                int iv = (lastAnchor == null) ? 0 : wrapToNearestOctave(root - lastAnchor);
                token = (iv >= 0) ? ("N+" + iv) : ("N" + iv);
            } else {
                // Chord: pitch-class intervals from root (unique, sorted)
                List<Integer> pcs = new ArrayList<>(o.notes.size());
                for (int n : o.notes) {
                    int iv = (n - root) % 12; if (iv < 0) iv += 12;
                    if (!pcs.contains(iv)) pcs.add(iv);
                }
                Collections.sort(pcs);
                token = "CH[" + pcs.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
            }

            // Optional rhythm bucket
            String rhythm = "";
            if (ADD_RHYTHM_BUCKETS) {
                char r;
                if (i > 0) {
                    double x = (double) o.ioiMs / (double) medianIoi;
                    r = (x < IOI_SHORT) ? 'S' : (x > IOI_LONG) ? 'L' : 'M';
                } else if (!o.noteDurations.isEmpty()) {
                    long medDur = median(o.noteDurations);
                    double x = (double) medDur / (double) medianIoi;
                    r = (x < IOI_SHORT) ? 'S' : (x > IOI_LONG) ? 'L' : 'M';
                } else {
                    r = 'M';
                }
                rhythm = "-" + r;
            }

            if (out.length() > 0) out.append(" | ");
            if (ADD_EVENT_LABELS) out.append("E").append(i).append(": ");
            out.append(token).append(rhythm);

            lastAnchor = root;
        }

        return out.toString();
    }

    // -------- Internals --------

    private static class Onset {
        long t;                                // onset time (rebased)
        List<Integer> notes = new ArrayList<>();      // absolute MIDI note numbers
        List<Long> noteDurations = new ArrayList<>(); // durations per started note
        long ioiMs = 0;
    }

    private static List<Onset> buildOnsets(List<MidiEvent> events) {
        List<Onset> onsets = new ArrayList<>();
        Map<Integer, Long> activeStart = new HashMap<>();
        Map<Integer, Integer> activeOnsetIndex = new HashMap<>();
        long[] lastOnAt = new long[128];
        Arrays.fill(lastOnAt, Long.MIN_VALUE / 4);

        int i = 0;
        while (i < events.size()) {
            MidiEvent e = events.get(i);

            if (e.isNoteOn) {
                long binStart = e.timestamp;
                Onset onset = new Onset();
                onset.t = binStart;

                // collect NOTE_ON within the bin
                while (i < events.size()) {
                    MidiEvent x = events.get(i);
                    if (!x.isNoteOn) break;
                    if (x.timestamp - binStart > TIME_BIN_MS) break;
                    if (x.note < 0 || x.note > 127) { i++; continue; }
                    if (x.timestamp - lastOnAt[x.note] < DUP_ON_DEBOUNCE_MS) { i++; continue; }

                    onset.notes.add(x.note);
                    activeStart.put(x.note, x.timestamp);
                    activeOnsetIndex.put(x.note, onsets.size());
                    lastOnAt[x.note] = x.timestamp;
                    i++;
                }
                if (!onset.notes.isEmpty()) onsets.add(onset);

            } else {
                // NOTE_OFF: attach durations to the onset that started it
                Long start = activeStart.remove(e.note);
                Integer idx = activeOnsetIndex.remove(e.note);
                if (start != null && idx != null && idx >= 0 && idx < onsets.size()) {
                    long dur = Math.max(1, e.timestamp - start);
                    onsets.get(idx).noteDurations.add(dur);
                }
                i++;
            }
        }
        return onsets;
    }

    private static int wrapToNearestOctave(int semitones) {
        // Map to [-6 .. +6] by adding/subtracting 12 as needed
        while (semitones > 6)  semitones -= 12;
        while (semitones < -6) semitones += 12;
        return semitones;
    }

    private static long median(List<Long> xs) {
        if (xs == null || xs.isEmpty()) return 0;
        Collections.sort(xs);
        int n = xs.size();
        return (n % 2 == 1) ? xs.get(n / 2)
                : Math.round((xs.get(n/2 - 1) + xs.get(n/2)) / 2.0);
    }
}
