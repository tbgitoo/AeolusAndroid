package com.mathis.midiSynth.userInterface;

public interface AeolusActivityActionBarResponder {

    public void launchAudioSettingsActivity();

    public void panicoff();

    public void panicon();

    public void launchPianoActivity();

    public void activateAllStops();

    public void deactivateAllStops();

    void recallStops();
}
