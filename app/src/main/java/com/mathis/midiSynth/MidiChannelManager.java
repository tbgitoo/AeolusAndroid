package com.mathis.midiSynth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A Singleton class to manage MIDI channel settings across the application.
 * This ensures that channel selections persist between different activities and
 * across application restarts by using SharedPreferences.
 */
public class MidiChannelManager {

    /** The singleton instance of the manager. */
    private static MidiChannelManager instance;

    /** The name of the SharedPreferences file. */
    private static final String PREFS_NAME = "MidiChannelPrefs";

    /** The key for storing the upper keyboard channel. */
    private static final String KEY_UPPER_CHANNEL = "upper_channel";

    /** The key for storing the lower keyboard channel. */
    private static final String KEY_LOWER_CHANNEL = "lower_channel";

    /** The SharedPreferences instance for storing data. */
    private SharedPreferences sharedPreferences;

    /** The currently selected MIDI channel for the upper keyboard (0-15). */
    private int upperKeyboardChannel;

    /** The currently selected MIDI channel for the lower keyboard (0-15). */
    private int lowerKeyboardChannel;

    /**
     * Private constructor to prevent direct instantiation. It loads the
     * previously saved channel settings from SharedPreferences.
     * @param context The application context, used for accessing SharedPreferences.
     */
    private MidiChannelManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Load the stored values, with defaults. I've set the lower keyboard to default to channel 2 (index 1)
        // to avoid both keyboards being on the same channel on first launch.
        upperKeyboardChannel = sharedPreferences.getInt(KEY_UPPER_CHANNEL, 0);
        lowerKeyboardChannel = sharedPreferences.getInt(KEY_LOWER_CHANNEL, 1);
    }

    /**
     * Returns the single, shared instance of the MidiChannelManager.
     * The context is only used on the very first call to create the instance.
     * @param context The application context.
     * @return The singleton instance.
     */
    public static synchronized MidiChannelManager getInstance(Context context) {
        if (instance == null) {
            instance = new MidiChannelManager(context);
        }
        return instance;
    }

    /**
     * Gets the MIDI channel for the upper keyboard.
     * @return The channel number (0-15).
     */
    public int getUpperKeyboardChannel() {
        return upperKeyboardChannel;
    }

    /**
     * Sets and persists the MIDI channel for the upper keyboard.
     * @param channel The channel number to set (0-15).
     */
    public void setUpperKeyboardChannel(int channel) {
        this.upperKeyboardChannel = channel;
        sharedPreferences.edit().putInt(KEY_UPPER_CHANNEL, channel).apply();
    }

    /**
     * Gets the MIDI channel for the lower keyboard.
     * @return The channel number (0-15).
     */
    public int getLowerKeyboardChannel() {
        return lowerKeyboardChannel;
    }

    /**
     * Sets and persists the MIDI channel for the lower keyboard.
     * @param channel The channel number to set (0-15).
     */
    public void setLowerKeyboardChannel(int channel) {
        this.lowerKeyboardChannel = channel;
        sharedPreferences.edit().putInt(KEY_LOWER_CHANNEL, channel).apply();
    }
}
