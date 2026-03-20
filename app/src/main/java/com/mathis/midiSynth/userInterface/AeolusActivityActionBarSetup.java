package com.mathis.midiSynth.userInterface;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import com.mathis.midiSynth.R;

public class AeolusActivityActionBarSetup {

    private ActionBar theActionBar;
    private AeolusActivityActionBarResponder theResponder;
    private Context theContext;

    public AeolusActivityActionBarSetup(String heading, ActionBar actionBar, AeolusActivityActionBarResponder responder, Context context)
    {
        theActionBar= actionBar;
        theResponder=responder;
        theContext = context;

        theActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        theActionBar.setDisplayShowCustomEnabled(true);
        theActionBar.setCustomView(R.layout.action_bar_aeolus_activity);
        theActionBar.setElevation(10);

        // Restore the custom gray background color for the action bar
        theActionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.actionBar)));

        ImageView settingsButton = theActionBar.getCustomView().findViewById(R.id.gotoSettings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.launchAudioSettingsActivity();
            }
        });

        ImageView pianoButton = theActionBar.getCustomView().findViewById(R.id.gotoPiano);
        pianoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.launchPianoActivity();
            }
        });

        Button panicOffButton= theActionBar.getCustomView().findViewById(R.id.alertpanicbuttonoff);
        panicOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.panicoff();
            }
        });

        Button panicOnButton= theActionBar.getCustomView().findViewById(R.id.alertpanicbuttonon);
        panicOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.panicon();
            }
        });

        Button allOnButton= theActionBar.getCustomView().findViewById(R.id.toggleAllOnButton);
        allOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.activateAllStops();
            }
        });

        Button allOffButton= theActionBar.getCustomView().findViewById(R.id.toggleAllOffButton);
        allOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.deactivateAllStops();
            }
        });

        Button recallStopsButton = theActionBar.getCustomView().findViewById(R.id.recallStopsButton);
        recallStopsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                theResponder.recallStops();
            }
        });
    }

    public void show()
    {
        theActionBar.show();
    }
}
