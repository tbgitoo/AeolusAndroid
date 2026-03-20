// ----------------------------------------------------------------------------
//
//  Copyright (C) 2025 Thomas and Mathis Braschler <thomas.braschler@gmail.com>
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// ----------------------------------------------------------------------------

package com.mathis.midiSynth;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.media.midi.MidiDeviceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.inputMidi.MidiSpec;
import com.mathis.midiSynth.baseMidiActivity.baseAeolusMidiActivity;
import com.mathis.midiSynth.databinding.ActivityAudioSettingsBinding;
import com.mathis.inputMidi.hardwareMidi.hardwareMidiManager;
import com.mathis.midiBase.hardwareMidiNativeReceiver;
import com.mathis.inputMidi.softwareMidi.MidiSynthDeviceService;
import com.mathis.inputMidi.softwareMidi.softwareMidiReceiver;
import com.mathis.midiSynth.userInterface.AudioSettingsActivityActionBarResponder;
import com.mathis.midiSynth.userInterface.AudioSettingsActivityActionBarSetup;
import com.mathis.midiSynth.userInterface.MidiMapDivision;

import java.util.ArrayList;

/**
 * Activity for setting up the midi and audio settings
 */
public class AudioSettingsActivity extends baseAeolusMidiActivity implements
        AdapterView.OnItemSelectedListener, softwareMidiReceiver,
        hardwareMidiNativeReceiver, AudioSettingsActivityActionBarResponder
{


    /** Dropdown list to select from which hardware port
     *  we want to receive midi messages (if there is several)
      */

    Spinner mOutputDevicesSpinner;


    /** Switch for switching on or off
     * the software midi message reception
     */

    Switch mSwitch_software_port;


    /**
     * For displaying the lastest incoming mesage
     */
    TextView mReceiveMessageTx;

    /**
     * Dropdown for selecting the temperaement
     */

    Spinner mTuningScaleSelection;


    private ActivityAudioSettingsBinding binding;

    /**
     * For each division, holds the choice of midi mappings routed to the present division
     */
    public ArrayList<MidiMapDivision> midiMapDivisions = new ArrayList<MidiMapDivision>();

    /**
     * Helper class to set up the action bar at the top of the window
     */
    AudioSettingsActivityActionBarSetup theActionBar=null;

    /**
     * Set up the midi and UI state
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        binding = ActivityAudioSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());





        mReceiveMessageTx = findViewById(R.id.receiveMessageTx);
    // Setup UI

        mOutputDevicesSpinner = findViewById(R.id.outputDevicesSpinner);
        mOutputDevicesSpinner.setOnItemSelectedListener(this);

        mTuningScaleSelection= findViewById(R.id.spinner_tuning);
        mTuningScaleSelection.setOnItemSelectedListener(this);


        mSwitch_software_port = (Switch)findViewById(R.id.switch_software_port);

        //
        // Setup the MIDI interface
        //

        initMidiAeolus(this,this);


        setActionBar("Aeolus Android > Audio/Midi settings");


        mSwitch_software_port.setChecked(MidiSynthDeviceService.isTransmittingSoftwareMidiMessagesToReceiver());

        mSwitch_software_port.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MidiSynthDeviceService.setTransmitSoftwareMidiMessagesToReceiver(isChecked);
            }
        });



    }

    /**
     * Event triggered whenever there is new data available from the Aeolus C inteface
     */
    @Override
    protected void updateUIFromNative() {

        int n_midi=AeolussynthManager.getNumberMidiChannels();

        int n_divisions = AeolussynthManager.getNumberDivisions();
        // The midi messages technically get routed to keyboards, but there is really juste
        // one keyboard (primary) keyboard per division. There is the couplers, but this is to be
        // seen
        runOnUiThread(new Runnable() {
            public void run() {

        midiMapDivisions.clear();
        for(int index=0; index<n_divisions; index++) {
            midiMapDivisions.add(new MidiMapDivision(getApplicationContext()));
            midiMapDivisions.get(index).n_divisions=n_divisions;
            midiMapDivisions.get(index).myIndex=index;
            midiMapDivisions.get(index).setDivisionLabel(AeolussynthManager.getLabelForDivision(index));
            midiMapDivisions.get(index).set_n_midi_channels(n_midi);
            midiMapDivisions.get(index).updateUIFromNative();

        }



                updateRetuningFieldActive();

                for(int index=0; index<n_divisions; index++) {

                    LinearLayout base_layout = findViewById(R.id.midiChannelsBaseLayout);
                    if( midiMapDivisions.get(index).getParent() != null) {
                        ((ViewGroup) midiMapDivisions.get(index).getParent()).removeView( midiMapDivisions.get(index)); // <- fix
                    }

                    base_layout.addView(midiMapDivisions.get(index));



                }

            }
        } );



        for(int midi_index=0; midi_index<n_midi; midi_index++)
        {
            byte midi_keyboard_distribution_mask = AeolussynthManager.queryMidiMap(midi_index);

        }

        fillTuningList(mTuningScaleSelection);

    }

    /**
     * Get the list of tunings from the underlying Aeolus C implementation
     * @param spinner Dropdown list to fill
     */
    private void fillTuningList(Spinner spinner) {
        int n_tunings = AeolussynthManager.get_n_tunings();
        ArrayList<String> listItems = new ArrayList<>();
        for(int i=0; i<n_tunings; i++)
        {
            listItems.add(AeolussynthManager.getTuningLabel(i));
        }

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter =
                new  ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item,
                        listItems);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        if(listItems.size()>0)
        {
            // We have different at least one (and generally several) items in the list,
            // synchronize the selection with the currently selected tuning (=temperament)
            // in the C implementation of Aeolus
            spinner.setSelection(AeolussynthManager.getCurrentTuning());


        }

    }

    /**
     * Iniate the action bar
     * @param heading Heading to show in the action bar
     */

    public void setActionBar(String heading) {

        if(theActionBar==null)
        {
            theActionBar=new AudioSettingsActivityActionBarSetup(heading, getSupportActionBar(),this,getApplicationContext());
        }
        theActionBar.show();


    }


    /**
     * Generic function called when on one of the dropdown, the user selected something
     * @param spinner The dropdown list where selection happene
     * @param view The view within the AdapterView that was clicked (the graphical item clicked)
     * @param position The position of the view in the adapter (the index of the item clicked, which is
     *                 what we need to know here
     * @param id The row id of the item that is selected (?)
     */
    @Override
    public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
        if(spinner.getId()==R.id.outputDevicesSpinner) { // Hardware midi device selected by the user,
            // do the actual selection

                MidiDeviceListItem listItem = (MidiDeviceListItem) spinner.getItemAtPosition(position);
                hardwareMidiManager.hardwareMidiOpenReceiveDevice(listItem.getDeviceInfo());
        }
        if(spinner.getId()==R.id.spinner_tuning)
        {
            // A tuning different from
            // the present one has been selected, we need to retune
            if(AeolussynthManager.getCurrentTuning()!=position)
            {
                updateRetuningFieldActive(false);
               AeolussynthManager.reTune(position);

            }
        }

    }

    // To indicate that we are doing re-tuning (or not)
    private void updateRetuningFieldActive() {

        updateRetuningFieldActive(!AeolussynthManager.isRetuning());



    }

    /**
     * Show or hide the retuning indicator
     * @param isActive If true show, hide otherwise
     */
    private void updateRetuningFieldActive(boolean isActive) {

        mTuningScaleSelection.setEnabled(isActive);
        if(isActive)
        {
            findViewById(R.id.status_tuning).setVisibility(INVISIBLE);
        } else {
            findViewById(R.id.status_tuning).setVisibility(VISIBLE);
        }



    }

    /**
     * Event triggered from the C implementation of Aeolus when the retuning is complete
     */

    @Override
    public void onRetuned()
    {
        Log.i("AudioSettingsActivity::onRetuned","retuning done");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateRetuningFieldActive();
            }
        });

    }

    /**
     * By default, indicate what to do when nothing is selected
     * @param adapterView The AdapterView that now contains no selected item.
     */

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}


    /**
     * Fills the specified list control with a set of MidiDevices
     * @param spinner   The list control.
     * @param devices   The set of MidiDevices.
     */
    private void fillDeviceList(Spinner spinner, ArrayList<MidiDeviceInfo> devices) {
        ArrayList<MidiDeviceListItem> listItems = new ArrayList<MidiDeviceListItem>();
        for(MidiDeviceInfo devInfo : devices) {
            listItems.add(new MidiDeviceListItem(devInfo));
        }


        // Creating adapter for spinner
        ArrayAdapter<MidiDeviceListItem> dataAdapter =
                new  ArrayAdapter<MidiDeviceListItem>(this,
                        android.R.layout.simple_spinner_item,
                        listItems);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        if(listItems.size()==1)
        {
            // If there is exactly 1 item, select it
            spinner.setSelection(0);


        }

    }

    /**
     * Event called when a software midi message is received on a software port (virtual midi port)
     * @param message Incoming software midi message, raw bytes
     */

    @Override
    public void onSoftwareMidiMessageReceive(byte[] message) {
        super.onSoftwareMidiMessageReceive(message);
        runOnUiThread(new Runnable() {
            public void run() {
                showReceivedMessage(message);
            }
        });
    }

    /**
     * Event called when software midi is activated or de-activated for this application
     * This happens when other applications select to connect or disconnect from the software midi
     * port offered by the Aeolus app
     * @param active Current status of the software midi port managed by this application
     */

    @Override
    public void onDeviceStatusChanged(boolean active) {
       if(active)
       {
           Log.i("midiSynth.AudioSettingsActivity","onDeviceStatusChanged: software source active");
           runOnUiThread(new Runnable() {
               public void run() {

                   findViewById(R.id.no_software_input).setVisibility(GONE);
               }
           });
       } else {
           Log.i("midiSynth.AudioSettingsActivity","onDeviceStatusChanged: no active software source");
           runOnUiThread(new Runnable() {
               public void run() {

                   findViewById(R.id.no_software_input).setVisibility(VISIBLE);
               }
           });

       }
    }

    /**
     * Event triggered when the user wants to emit a test soudn
     * @param view The button clicked
     */

    public void testAeolusSynth(View view) {

            AeolussynthManager.AeolusSynthTest();



    }




    /**
     * Fills the Input & Output UI device list with the current set of hardware midi devices
     */
    @Override
    protected void onDeviceListChange() {
        runOnUiThread(new Runnable() {
            public void run() {
                fillDeviceList(mOutputDevicesSpinner,hardwareMidiManager.availableReceiveDevices());
            }
        } );

    }





    //
    // UI Helpers
    //
    /**
     * Formats a set of MIDI message bytes into a user-readable form.
     * @param message   The bytes comprising a Midi message.
     */
    @Override
    protected void showReceivedMessage(byte[] message) {
        switch ((message[0] & 0xF0) >> 4) {
            case MidiSpec.MIDICODE_NOTEON:
                mReceiveMessageTx.setText(
                        "NOTE_ON [ch:" + (message[0] & 0x0F) +
                                " key:" + message[1] +
                                " vel:" + message[2] + "]");
                break;

            case MidiSpec.MIDICODE_NOTEOFF:
                 mReceiveMessageTx.setText(
                        "NOTE_OFF [ch:" + (message[0] & 0x0F) +
                                " key:" + message[1] +
                                " vel:" + message[2] + "]");
                break;

            // Potentially handle other messages here.
            default: mReceiveMessageTx.setText(" "+message[0]+" "+message[1]);
                break;
        }
    }

    /**
     * Quit this audio setting activity and go back to the main Aeolus activity
     */
    @Override
    public void launchAeolusActivity() {
        Intent myIntent = new Intent(this, AeolusActivity.class);
        startActivity(myIntent);
    }


    /**
     * A class to hold MidiDevices in the list controls.
     */
    private class MidiDeviceListItem {
        private final MidiDeviceInfo mDeviceInfo;

        /** Constructor with MidiDeviceInfo
         *
         * @param deviceInfo The midi device info object
         */
        public MidiDeviceListItem(MidiDeviceInfo deviceInfo) {
            mDeviceInfo = deviceInfo;
        }

        public MidiDeviceInfo getDeviceInfo() { return mDeviceInfo; }


        @Override
        public String toString() {
            return mDeviceInfo.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
        }
    }











}


// Pathway from incoming software midi event to activation of pipes ===============================================
// 1) Upon an incoming software midi event, the onSoftwareMidiMessageReceive method of baseAeolusMidiActivity is called
// 2) If the midi event corresponds to a key on / off event, the channel and midi information is
// transmitted to AeolussynthManager.AeolusSynthNoteOn()
// 3) AeolusSynthNoteOn() corresponds to the native method Java_com_mathis_midiSynth_AeolusSynth_AeolussynthManager_AeolusSynthNoteOn
// 4) The native method calls the noteon function of AeolusSynthesizer.cpp. This in turn triggers a midi event by calling
// 5) proc_midi_event defined in Imidi::proc_midi_event. Here, the channel is read out and
//    associated with the bit-mask stored in _midimap
// 6) This is information is stored in the _qnote queue, with the last 6 bits (&127 mask) of the _midimap value transmitted
// 7) In the next loop of the audio thread (AeolusAudio, in audio.cpp), e.g. in AeolusAudio::proc_queue this will be picked up
// 8) proc_queue isolates the last 7 bits as the varialbe "b"
// 9) With this information, the key_on method, defined directly in audio.h, is called. key_on sets the _keymap variable for
//    note n equal to the transmitted 7 bits, and also sets the 8-th bit to high
// 10) This 8-bit high state will be specifically picked up in the next round of proc_keys1 procedure of the audio thread,
// which is also called at every round of the thread. proc_keys1 is defined in audio.cpp
// 11) proc_keys1 sets the 8th bit to low, and updates the divisions with the 7 remaining bit. That is, for each division,
// 12) the Division::update function is called with the note n and the 7-bit information ultimately from _midimap.
// 13) The division then runs through its ranks. When there is an AND overlap between the _cmask of the rank
//     and the midimap 7-bit terminus, the rank is turned on for that particular note , otherwise it is turned off.
//     a note for a given rank means a pipe and so this terminates the communication chain from incoming midi message
//     to activation / disactivation of the pipe.


// Pathway of configuring the midimap
// 1) The _midimap is a 16-member array of type uint 16, which is a two-byte or 16-bit mask, one mask for each channel
// 2) It is initialized upon construction of the AeolusAudio object, which in the implementation here is the
//    central AeolusSynthesizer object (i.e. the global synth object initialized in the AeolusSynth_jni_functions.cpp main
//    bridge.). Technically, the initialization is through declaration in audio.h
//  3) During initialization of AeolusSynthesizer, in AeolusSynthesizer.cpp, the pointer to the _midimap of the AeolusSynthesizer
//    is transmitted to the _midiInterface member of the AeolusSynthesizer, which is of type MidiAndroidAeolus, and by
//   inheritance of type IMidi.
//  4) At the end of AeolusSynthesizer constructor, the midiInterface _midiInterface is launched via the command
//    openMidi
//  5) Inherent in the inner working of Aeolus, in Imidi::open_midi(), a notification message indicating that midi initialization is
//   complete is generated. This message is of type M_midi_info, and conveys the pointer to _midimap, as the field _chbits.
//  6) Also inherent in the inner workings of Aeolus, this message is received by the _model member of AeolusSynthesizer. The midi
//  information in the _model object (of type Model) is stored in the _midi field. The _model->_midi->_chbits pointer therefore points
//  to identically to the _midimap array after initialization
//  7) The initial setting of the midimap is overriden from preset 0. Preset 0 is read in Model::read_presets ,
//  8) and then set via set_mconf at the end of the Model::init_iface method
//  9) in the set_mconf method, things are as follows:
//  10) a 0x1000 mask is used to determine whether a the midi channel should be routed to a keyboard, this is the case
//      for the default preset 0 (which is 0x 50 02 (aka 0250 in the raw preset file in aeolus.zip)
//      the lower bits (mask 0x7) are used to select a keyboard, so this is keyboard 2 here
//   11) The flags for the keyboard are set by default, 1 for keyboard 0, 2 for keyboard 1, 4 for keyobard 2, 8 for keyboard 3

