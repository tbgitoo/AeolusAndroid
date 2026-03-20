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
package com.mathis.midiSynth.userInterface;

import static android.widget.LinearLayout.HORIZONTAL;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;

import com.mathis.midiSynth.R;

/**
 * Specific class to help set up the action bar at the top of the Audio Settings Activity window (that is,
 * the action at the top of the window for detailed audio/midi settings). This class is for setting up action bar, it
 * is not the action bar itself; the action bar itself is the actionBar field of this class.<br /><br />
 * This class also needs a responder conforming to the AudioSettingsActionBarResponder for executing
 * the actions triggered by the user pushing the action bar buttons. This responder is typically (but
 * not necessarily) the activity.<br /><br />
 * The graphical layout for the action bar is loaded from the layout file action_bar_audio_settings_activity.xml
 */
public class AudioSettingsActivityActionBarSetup extends ActionBarSetup {



    /** Object that will respond to the requests
     * triggered when the user clicks on the buttons of the action bar
     */

   AudioSettingsActivityActionBarResponder responder=null;



    /**
     * Constructor of the action bar setup for the AeolusActivity window
     * @param heading Header string to be displayed
     * @param appActionBar Reference to the action bar. This needs to be obtained by the App Activity class, via getSupportActionBar()
     * @param theResponder Responder object for executing the tasks triggered by the action Bar, conforms to the
     *                     AeolusActivityActionBarResponder interface
     * @param theContext Get this context from the App Activity (that is, specifically
     *                   , via getApplicationContext(). This serves to find ressources like
     *                   drawables and colors
     */

    public AudioSettingsActivityActionBarSetup(String heading, ActionBar appActionBar,
                                        AudioSettingsActivityActionBarResponder theResponder,
                                        Context theContext) {
        super(heading,appActionBar,theContext);
        responder=theResponder;

        if(actionBar != null) {

            actionBar.setDisplayOptions(actionBar.getDisplayOptions() |
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            LinearLayout theLayout = new LinearLayout(actionBar.getThemedContext());

            theLayout.setOrientation(HORIZONTAL);
            View inflatedLayout = inflater.inflate(R.layout.action_bar_audio_settings_activity,theLayout);


            ImageView imageView =inflatedLayout.findViewById(R.id.gotoAeolus);

            imageView.setClickable(true);

            imageView.setOnClickListener(v -> responder.launchAeolusActivity());

            imageView.setImageResource(R.drawable.aeolus_button);

            actionBar.setBackgroundDrawable(
                    new ColorDrawable(context.getColor(R.color.purple_700)));


            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.END |
                            Gravity.CENTER_VERTICAL);
            layoutParams.rightMargin = 40;
            theLayout.setLayoutParams(layoutParams);

            actionBar.setCustomView(theLayout);


            actionBar.show();



        }

    }



}

