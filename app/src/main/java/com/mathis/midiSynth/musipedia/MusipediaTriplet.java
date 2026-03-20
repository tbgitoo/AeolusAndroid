package com.mathis.midiSynth.musipedia;

import java.util.Locale;

/**
 * A simple data class for a Musipedia triplet (pitch, duration, onset).
 * It now formats the output to the required "onset,pitch,duration" format with values in seconds.
 */
public class MusipediaTriplet {
    public final int pitch;
    public final long duration; // in milliseconds
    public final long onset;    // in milliseconds

    public MusipediaTriplet(int pitch, long duration, long onset) {
        this.pitch = pitch;
        this.duration = duration;
        this.onset = onset;
    }

    /**
     * Returns the triplet formatted as a string required by the Musipedia API.
     * Format: "onset,pitch,duration" with time values in seconds.
     * @return The formatted string.
     */
    @Override
    public String toString() {
        double onsetInSeconds = onset / 1000.0;
        double durationInSeconds = duration / 1000.0;
        return String.format(Locale.US, "%.3f,%d,%.3f", onsetInSeconds, pitch, durationInSeconds);
    }
}
