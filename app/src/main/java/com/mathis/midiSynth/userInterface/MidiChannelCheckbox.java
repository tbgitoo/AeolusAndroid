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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.R;

/**
 * Checkbox + label, for displaying and updating a midi-channel / division combination
 * This class uses the layout file midi_channel_checkbox.xml, and also ensures combination
 * with the C Aeolus implementation through AeolussynthManager
 */
public class MidiChannelCheckbox extends LinearLayout {

    /**
     * The division to which this labelled checkbox belongs
     */
    public int my_division_index=0;

    /**
     * The midi channel index handled by this checkbox/label combo
     */
    public int my_midi_channel_index =0;

    /**
     * Total number of midi channels, for layouting
     */
    public int n_midi_channels =0;

    /**
     * Reference to the label
     */
    public TextView labelText;

    /**
     * Reference to the checkbox
     */

    public CheckBox checkbox;

    /**
     * Listener for the user clicking on the checkboxes
     */

    protected View.OnClickListener theListener;


    /**
     * Constructor; this inflates the layout and populates the values from the Aeolus C part
     * @param context The application activity context
     */
    public MidiChannelCheckbox(@NonNull Context context) {
        super(context);
        setOrientation(VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedLayout = inflater.inflate(R.layout.midi_channel_checkbox, this);

        labelText=inflatedLayout.findViewById(R.id.midiChannelLabel);

        checkbox=inflatedLayout.findViewById(R.id.midiChannelCheckBox);

        theListener=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked=checkbox.isChecked();
                AeolussynthManager.setMidiMapping(my_division_index, my_midi_channel_index, isChecked);

            }


        };

        checkbox.setOnClickListener(theListener);

    }


    /**
     * Set the present label
     * @param theLabel to be shown for the present checkbox (the midi channel)
     */
    public void setChannelText(String theLabel)
    {
        labelText.setText(theLabel);
    }

    /**
     * Set the checked state of the checkbox
     * @param b True if checked, false otherwise
     */

    public void setChecked(boolean b) {
        checkbox.setChecked(b);

    }

    /**
     * Set the checkbox value without causing the change event to be triggered
     * This is important when programmatically setting the value, sometimes it is not
     * whished to cause another round of onChange
     * @param b Check the box if true, don't otherwise
     */


    public void setCheckedWithoutEvent(boolean b) {
        checkbox.setOnClickListener(null);
        setChecked(b);
        checkbox.setOnClickListener(theListener);
    }
}
