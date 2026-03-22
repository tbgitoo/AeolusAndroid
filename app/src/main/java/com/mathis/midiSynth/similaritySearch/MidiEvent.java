package com.mathis.midiSynth.similaritySearch;

/**
 * A simple data class to hold the details of a single MIDI event,
 * including the note, the time it occurred, and whether it was a Note On event.
 */
public class MidiEvent {
    public final int note;
    public final long timestamp;
    public final boolean isNoteOn;

    public MidiEvent(int note, long timestamp, boolean isNoteOn) {
        this.note = note;
        this.timestamp = timestamp;
        this.isNoteOn = isNoteOn;
    }
}
