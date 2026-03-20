package com.mathis.midiSynth;

import android.content.Context;
import android.content.SharedPreferences;
import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;

/**
 * Manages the persistent storage of the synthesizer stop states using SharedPreferences.
 * This is a singleton class to ensure a single source of truth for the stop state.
 */
public class StopStateManager {

    private static StopStateManager instance;
    private static final String PREFS_NAME = "StopStatePrefs";
    private static final String KEY_STOP_STATE = "stop_state";

    private final SharedPreferences sharedPreferences;

    /**
     * Private constructor for the singleton pattern.
     * @param context The application context, used to get SharedPreferences.
     */
    private StopStateManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets the singleton instance of the StopStateManager.
     * @param context The application context.
     * @return The singleton instance.
     */
    public static synchronized StopStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new StopStateManager(context);
        }
        return instance;
    }

    /**
     * Saves the current state of all stops by fetching it from the native synthesizer.
     * The state is saved as a semicolon-separated string of long values, where each long
     * represents a bitmask of the active stops for a division.
     */
    public void saveCurrentStopState() {
        int numDivisions = AeolussynthManager.getNumberDivisions();
        if (numDivisions == 0) return;

        StringBuilder stateString = new StringBuilder();
        for (int i = 0; i < numDivisions; i++) {
            long stopStatesForDivision = AeolussynthManager.getActiveStopsForDivision(i);
            stateString.append(stopStatesForDivision).append(";");
        }

        sharedPreferences.edit().putString(KEY_STOP_STATE, stateString.toString()).apply();
    }

    /**
     * Loads the saved stop state from SharedPreferences and applies it to the native synthesizer.
     */
    public void recallStopState() {
        String savedState = sharedPreferences.getString(KEY_STOP_STATE, null);
        if (savedState == null || savedState.isEmpty()) {
            return; // No saved state
        }

        String[] divisionStates = savedState.split(";");
        int numDivisions = AeolussynthManager.getNumberDivisions();

        for (int i = 0; i < divisionStates.length && i < numDivisions; i++) {
            try {
                long stopStatesForDivision = Long.parseLong(divisionStates[i]);
                AeolussynthManager.setActiveStopsForDivision(i, stopStatesForDivision);
            } catch (NumberFormatException e) {
                // Ignore malformed data for any specific division
            }
        }
    }
}
