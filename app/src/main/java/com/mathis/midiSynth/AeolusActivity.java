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

import android.content.Intent;
import android.os.Bundle;

import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.baseMidiActivity.baseAeolusMidiActivity;
import com.mathis.midiSynth.userInterface.AeolusActivityActionBarSetup;
import com.mathis.midiSynth.userInterface.AeolusActivityActionBarResponder;
import com.mathis.midiSynth.userInterface.DivisionsMainScreen;



public class AeolusActivity extends baseAeolusMidiActivity implements AeolusActivityActionBarResponder {

    public AeolusActivityActionBarSetup theActionBar=null;

    public DivisionsMainScreen theDivisionMainScreen = null;

    private StopStateManager stopStateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aeolus);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        theDivisionMainScreen = new DivisionsMainScreen(getApplicationContext(),findViewById(R.id.baseLinearLayout));

        stopStateManager = StopStateManager.getInstance(getApplicationContext());

        setActionBar("Aeolus Android > Synthesizer");

        initMidiAeolus(this,this);




    }

    public void setActionBar(String heading) {

        if(theActionBar==null)
        {
            theActionBar=new AeolusActivityActionBarSetup(heading, getSupportActionBar(),this,getApplicationContext());
        }
        theActionBar.show();


    }

    public void launchAudioSettingsActivity()
    {
        Intent myIntent = new Intent(this, AudioSettingsActivity.class);
        startActivity(myIntent);
    }

    @Override
    public void panicoff() {
        AeolussynthManager.panicoff();
    }

    @Override
    public void panicon() {
        AeolussynthManager.panicon();
    }

    @Override
    public void launchPianoActivity() {
        Intent myIntent = new Intent(this, PianoActivity.class);
        startActivity(myIntent);
    }


    @Override
    public void onDeviceStatusChanged(boolean active) {
        // For now, do nothing, there is no active selection of software input at this point
    }



    @Override
    protected void updateUIFromNative() {
        // implement this in derived classes
        theDivisionMainScreen.updateUIFromNative(); // Get values from native



        runOnUiThread(() -> {
            theDivisionMainScreen.addDivisionsToBaseLayout(); // update stops, layout

        });




    }
    @Override

    /**
     * This update only changes values, not the layout of the stops and controls
     */
    protected void updateStopsFromNative() {

        runOnUiThread(() -> {

            theDivisionMainScreen.updateStopActiveSettingFromNative();
            // Automatically save the new stop state whenever it changes.
            if (stopStateManager != null) {
                stopStateManager.saveCurrentStopState();
            }
                }
        );

    }


    @Override
    public void activateAllStops() {
        theDivisionMainScreen.activateAll();
    }

    @Override
    public void deactivateAllStops() {
        theDivisionMainScreen.deactivateAll();
    }

    /**
     * Loads the last saved stop state from SharedPreferences and applies it to the synthesizer.
     */
    @Override
    public void recallStops() {
        if (stopStateManager != null) {
            stopStateManager.recallStopState();
            // The native layer will automatically send an update which triggers updateStopsFromNative().
            // We show a Toast for immediate user feedback.
            Toast.makeText(this, "Recalled stops from last session", Toast.LENGTH_SHORT).show();
        }
    }
}