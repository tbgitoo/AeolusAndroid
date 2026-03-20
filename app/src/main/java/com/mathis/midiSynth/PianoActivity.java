package com.mathis.midiSynth;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.ActionBar;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.baseMidiActivity.baseAeolusMidiActivity;
import com.mathis.midiSynth.databinding.ActivityPianoBinding;
import com.mathis.midiSynth.musipedia.MidiEvent;
import com.mathis.midiSynth.musipedia.MusipediaTriplet;
import com.mathis.midiSynth.userInterface.PianoView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An activity that displays two on-screen piano keyboards.
 * It includes functionality to record a melody and search for it on Musipedia.
 */
public class PianoActivity extends baseAeolusMidiActivity {

    /** Logging tag for this activity. */
    private static final String TAG = "PianoActivity";

    /** View binding instance for the activity_piano layout. */
    private ActivityPianoBinding binding;

    /** Singleton manager for persistent MIDI channel settings. */
    private MidiChannelManager midiChannelManager;

    // --- Musipedia Recording Fields ---
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private final List<MidiEvent> recordedEvents = new ArrayList<>();






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPianoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        midiChannelManager = MidiChannelManager.getInstance(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Dual Piano Keyboard");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setupSpinners();
        setupKeyboards();
        setupMidiRecorder();

        initMidiAeolus(this, this);
    }

    @Override
    protected void updateUIFromNative() {
        updateDivisionLabels();
    }

    private void updateDivisionLabels() {
        updateDivisionLabelForSpinner(midiChannelManager.getUpperKeyboardChannel(), binding.textUpperDivisions);
        updateDivisionLabelForSpinner(midiChannelManager.getLowerKeyboardChannel(), binding.textLowerDivisions);
    }

    private void updateDivisionLabelForSpinner(int channel, android.widget.TextView textView) {
        byte midiMap = AeolussynthManager.queryMidiMap(channel);
        StringBuilder divisions = new StringBuilder();

        for (int i = 0; i < AeolussynthManager.getNumberDivisions(); i++) {
            if ((midiMap & (1 << i)) != 0) {
                if (divisions.length() > 0) {
                    divisions.append(", ");
                }
                divisions.append(AeolussynthManager.getLabelForDivision(i));
            }
        }

        if (divisions.length() == 0) {
            textView.setText("(None)");
        } else {
            textView.setText(divisions.toString());
        }
    }

    private void setupSpinners() {
        List<Integer> channels = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            channels.add(i);
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, channels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spinnerUpper.setAdapter(adapter);
        binding.spinnerLower.setAdapter(adapter);
        binding.spinnerUpper.setSelection(midiChannelManager.getUpperKeyboardChannel());
        binding.spinnerLower.setSelection(midiChannelManager.getLowerKeyboardChannel());

        binding.spinnerUpper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                midiChannelManager.setUpperKeyboardChannel(position);
                updateDivisionLabels();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerLower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                midiChannelManager.setLowerKeyboardChannel(position);
                updateDivisionLabels();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupKeyboards() {
        binding.pianoViewUpper.setOnPianoKeyListener(new PianoView.OnPianoKeyListener() {
            @Override
            public void onNoteOn(int note) {
                sendMidiNoteOn(midiChannelManager.getUpperKeyboardChannel(), note, 127);
            }

            @Override
            public void onNoteOff(int note) {
                sendMidiNoteOff(midiChannelManager.getUpperKeyboardChannel(), note, 0);
            }
        });

        binding.pianoViewLower.setOnPianoKeyListener(new PianoView.OnPianoKeyListener() {
            @Override
            public void onNoteOn(int note) {
                sendMidiNoteOn(midiChannelManager.getLowerKeyboardChannel(), note, 127);
            }

            @Override
            public void onNoteOff(int note) {
                sendMidiNoteOff(midiChannelManager.getLowerKeyboardChannel(), note, 0);
            }
        });
    }

    private void setupMidiRecorder() {
        binding.recordButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRecording = isChecked;
            if (isRecording) {
                // Start recording
                recordedEvents.clear();
                recordingStartTime = 0;

                binding.contourTextView.setText("Contour: Recording...");
                binding.searchMusipediaButton.setVisibility(View.GONE);
            } else {
                // Stop recording and process the data

                if (!recordedEvents.isEmpty()) {
                    binding.searchMusipediaButton.setVisibility(View.VISIBLE);

                }
            }
        });

        binding.searchMusipediaButton.setOnClickListener(v -> {
            if ( !recordedEvents.isEmpty()) {
                // to do: search for the lastRecorded Contour
            }
        });
    }



    private void sendMidiNoteOn(int channel, int note, int velocity) {
        if (isRecording) {
            long now = SystemClock.uptimeMillis();
            if (recordingStartTime == 0) {
                recordingStartTime = now;
            }
            recordedEvents.add(new MidiEvent(note, now, true));
        }
        byte[] msg = new byte[3];
        msg[0] = (byte) (0x90 | channel);
        msg[1] = (byte) note;
        msg[2] = (byte) velocity;
        super.onSoftwareMidiMessageReceive(msg);
    }

    private void sendMidiNoteOff(int channel, int note, int velocity) {
        if (isRecording) {
            recordedEvents.add(new MidiEvent(note, SystemClock.uptimeMillis(), false));
        }
        byte[] msg = new byte[3];
        msg[0] = (byte) (0x80 | channel);
        msg[1] = (byte) note;
        msg[2] = (byte) velocity;
        super.onSoftwareMidiMessageReceive(msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, AeolusActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDeviceListChange() {}

}
