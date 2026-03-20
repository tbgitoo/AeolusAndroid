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

package com.mathis.midiSynth.baseMidiActivity;

import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.mathis.aeolusnative.AeolusSynth.AeolusFileInstallation;
import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.inputMidi.MidiSpec;
import com.mathis.aeolusnative.userInterface.AeolusUIManager;

import com.mathis.aeolusnative.userInterface.InterfaceUpdater;
import com.mathis.aeolusnative.hardwareMidi.AppMidiManager;
import com.mathis.inputMidi.hardwareMidi.hardwareMidiManager;
import com.mathis.midiBase.hardwareMidiNativeReceiver;
import com.mathis.inputMidi.softwareMidi.MidiSynthDeviceService;
import com.mathis.inputMidi.softwareMidi.softwareMidiReceiver;

/**
 * Base class for Aeolus-Midi based application activities. This implements a series of common
 * functionalities that permit the different activities to all fulfill common midi functionality as
 * well as response to update requests from the Aeolus C implmentation. In many cases, the basic
 * implementations provided here will be overriden in daughter classes
 */
public class baseAeolusMidiActivity extends AppCompatActivity
        implements softwareMidiReceiver, hardwareMidiNativeReceiver, InterfaceUpdater {

    // Load the native implementation of the Aeolus synthesizer
    static {
        System.loadLibrary("midi_synth");
    }

    /**
     * Scans and gathers the list of connected physical devices,
     * then calls onDeviceListChange() to update the UI. In derived classes,
     * this typically has the
     * side-effect of causing a list item to be selected, which then
     * invokes the listener logic which connects the device(s).
     */
    protected void ScanMidiDevices() {
        hardwareMidiManager.scanhardwareMidiDevices();
        onDeviceListChange();
    }

    /** Evant caled when the available hardware device list has changed. As a default root class behaviour,
     * silently select the first midi hardware port available, or null if no hardware
     * ports are actually available
     */

    protected void onDeviceListChange() {
        if(!hardwareMidiManager.availableReceiveDevices().isEmpty())
            hardwareMidiManager.hardwareMidiOpenReceiveDevice(
                    hardwareMidiManager.availableReceiveDevices().get(0));




    }

    /**
     * Initialize the midi communication for the Aeolus synthesizer. This involves: <br />
     * - Initialization of the software midi service (MidiSynthDeviceService) <br />
     * - Installing the Aeolus stops, and instrument definition and preset files into the private
     * storage area of the application<br />
     *  - Setting up the hardware midi receiving architecture (namely, hardwareMidiManager) <br />
     *  - Initialization of Aeolus native application (via AeolusSynthManager) <br />
     *  - Initialization of the response pathway to notifications generated in the native implementation
     *  of Aeolus (via AeolusUIManager) <br /><br />
     *  Note: For genericity, the procedure explicityl requires the callback receivers for software and hardware midi. In
     *  a typical application, the two callback receivers are identical to the Activity object, that is
     *  this function is typically invoked via initMidiAeolus(this, this);
     * @param softwareReceiver Callback receiver for software midi
     * @param hardwareReceiver Callback receiver for hardware midi
     */

    public void initMidiAeolus(softwareMidiReceiver softwareReceiver, hardwareMidiNativeReceiver hardwareReceiver)
    {
        MidiSynthDeviceService.setSoftwareMidiMessageReceiver(softwareReceiver);

        MidiSynthDeviceService.checkDeviceStatus();

        AeolusFileInstallation.setContext(getApplicationContext());
        AeolusFileInstallation.initStorageLocationsDefault();
        Log.i("baseAeolusMidiActivity::initMidiAeolus","Installing stops and settings to "+AeolusFileInstallation.getPrivateStorageRoot());

        AeolusFileInstallation.install_files();

        hardwareMidiManager.setHardwareMidiManager((MidiManager) getSystemService(Context.MIDI_SERVICE));
        if(!hardwareMidiManager.isMidiNativeSetupHandlerSet()) {
            AppMidiManager mAppMidiManager=new AppMidiManager();
            mAppMidiManager.setMessageReceiver(hardwareMidiManager.getDefaultMidiMessageReceiver());
            hardwareMidiManager.setMidiNativeSetupHandler(mAppMidiManager);

        }
        hardwareMidiManager.initNative();
        hardwareMidiManager.setHardwareMidiDeviceCallback(new MidiDeviceCallback());
        hardwareMidiManager.setMessageReceiver(hardwareReceiver);

        AeolusUIManager.setAeolusUserInterfaceUpdater(this);
        AeolusUIManager.initNative();

        AeolussynthManager.initAeolussynth();

        ScanMidiDevices();
        Log.i("baseAeolusMidiActivity","anyways");
        if(!AeolussynthManager.isInitializing())
        {
            Log.i("baseAeolusMidiActivity","already initialized");
            updateUIFromNative();
        }
    }


    /**
     * This function is called once the initialization of the Aeolus C implementation is complete
     * Typically, you would query the necessary properties of the Aeolus native implementation
     * here in order to present a suitable user interface. This function needs to be implemented
     * for the particular use case in daughter classes
     */
    protected void updateUIFromNative() {
        // implement this in derived classes
    }

    /**
     * This method is involved when the Aeolus C implementation receives a hardware midi message.
     * This is a particularity of using Amidi with C implementation, the hardware midi messages
     * reach the application through the C part first, and the C part notifies the Java layer.
     * The midi message here, in raw, binary form,
     * is relayed via this function to the Java layer. In this base class,
     * the message is simply forwared to showReceivedMessage, on the UI Thread, for display
     * @param message Raw (byte-wise) midi message
     */

    public void onNativeMessageReceive(final byte[] message) {
        // Messages are received on some other thread, so switch to the UI thread
        // before attempting to access the UI
        runOnUiThread(new Runnable() {
            public void run() {
                showReceivedMessage(message);
            }
        });
    }

    /**
     * Stub for handling incoming hardware midi message. In this base class, this function
     * simply does nothing. Needs to be overriden in daughter classes
     * @param message Raw hardware midi message
     */
    protected void showReceivedMessage(byte[] message){}

    /**
     * Reception of a software midi message. Contrary to hardware midi messages, the primary
     * receiving point for software midi messages is the Java layer. It is therefore among
     * others necessary to pass on the information to the C Aeolus implementation layer. This is done
     * here for the case of note-on and note-off events (i.e 0x9... and 0x8... messages).
     * @param message Incoming software midi message, raw bytes
     */

    @Override
    public void onSoftwareMidiMessageReceive(byte[] message) {
        Log.i("midiSynth.baseAeolusMidiActivity","onSoftwareMidiMessageReceive "+message.length+" bytes");
        switch ((message[0] & 0xF0) >> 4) {
            case MidiSpec.MIDICODE_NOTEON:



                AeolussynthManager.AeolusSynthNoteOn((byte)(message[0] & 0x0F),message[1],message[2]);


                break;

            case MidiSpec.MIDICODE_NOTEOFF:

                AeolussynthManager.AeolusSynthNoteOff((byte)(message[0] & 0x0F),message[1],message[2]);

                break;
        }
    }

    /**
     * The status of the software midi port created by this application has changed, that is,
     * another app has connected to it (it becomes active) or has disconnected from it (it becomes
     * inactive)
     * @param active Current status of the software midi port managed by this application
     */
    @Override
    public void onDeviceStatusChanged(boolean active) {

    }

    /**
     * Notification by the native Aeolus implementation to indicate the set of active and inactive
     * stops have changed. For application activities handling the stops, this means that
     * the UI should be updated correspondingly. In this base implementation, relay to updateStopsFromNative()
     */

    @Override
    public void onActiveStopsChanged() {
        Log.i("baseAeolusMidiActivity::onActiveStopsChanged","called");
        updateStopsFromNative();
    }

    /**
     * Function called when the stops should updated integrally from the native part (identity and
     * whether or not they are active)
     */

    protected void updateStopsFromNative() {
       // implement this in derived classes
    }

    /** Notification from the native part indicating that loading is complete and the UI can
     * now be populated
     */

    @Override
    public void onLoadComplete() {
        updateUIFromNative();
    }

    /** Notification from the native part indicating that retuning is now complete
     *
     */

    @Override
    public void onRetuned()
    {

    }

    /**
     * Local implementation of the MidiManager.DeviceCallback interface.
     * This is needed for hardware midi device management: This implementation is
     * minimal: If a hardware change in terms
     * if midi devices is signalled, the device list is rescanned (which is also includes
     * UI update as needed)
     */

    public class MidiDeviceCallback extends MidiManager.DeviceCallback {
        @Override
        public void onDeviceAdded(MidiDeviceInfo device) {
            ScanMidiDevices();
        }

        @Override
        public void onDeviceRemoved(MidiDeviceInfo device) {
            ScanMidiDevices();
        }
    }
}
