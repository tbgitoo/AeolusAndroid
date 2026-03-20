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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.R;

import java.util.ArrayList;

/**
 * This class represents the UI of the midimapping of a given division. This means, it represents
 * the graphical userface that permits to set, via checkboxes, which midi channel is routed
 * to the division (corresponding checkbox check) or not (corresponding checkbox not checked).
 * The graphical layout is loaded from midi_map_division.xml
 */

public class MidiMapDivision extends LinearLayout {

    /** The total number of divisions */
    public int n_divisions=0;

    /**
     * The division index of this object
     */
    public int myIndex=0;

    /** The number of midichannels to handle. 0 for as long as Aeolus C is not completely
     * initialized, should be 16 afterwards
     */
    public int n_midi_channels =0; // The number of midi channels, typically 16, but set to 0 prior to object construction

    /** The lable of this division
     *
     */
    public TextView labelText;

    /**
     * Linear layout for the checkboxes indicating the midi channels signalling to this division
     */
    public LinearLayout registerRow;

    /** The midi channel checkboxes of this division */
    public ArrayList<MidiChannelCheckbox> channelCheckboxes = new ArrayList<MidiChannelCheckbox>();

    /**
     * Constructor, including loading of midi_map_division.xml
     * @param context App activity context
     */
    public MidiMapDivision(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedLayout = inflater.inflate(R.layout.midi_map_division, this);

        labelText = inflatedLayout.findViewById(R.id.divisionMidimapLabelText);

        registerRow = inflatedLayout.findViewById(R.id.divisionMidimapRegisterRow);

    }

    /**
     * Set the label for this division
     * @param theLabel String to be displayed in the user interface
     */

    public void setDivisionLabel(String theLabel) {
        labelText.setText(theLabel);
    }

    /**
     * Set the number of midichannels (and initialize the midi channel
     * checkboxes)
     * @param n The number of midi channels that can be associated with
     *          this division
     */

    public void set_n_midi_channels(int n)
    {
        n_midi_channels=n;
        channelCheckboxes.clear();
        for(int i=0; i<n_midi_channels; i++)
        {
            channelCheckboxes.add(new MidiChannelCheckbox(getContext()));
            channelCheckboxes.get(i).my_division_index = myIndex;
            channelCheckboxes.get(i).my_midi_channel_index =i;
            channelCheckboxes.get(i).n_midi_channels =n_midi_channels;
            registerRow.addView(channelCheckboxes.get(i));
        }
        label_midi_boxes();
    }

    /**
     * Event triggered when the C implementation of Aeolus has changed, to
     * update the user interface
     */

    public void updateUIFromNative() {
        for(int midi_index=0; midi_index < n_midi_channels; midi_index++)
        {

            byte midi_keyboard_distribution_mask = AeolussynthManager.queryMidiMap(midi_index);

            if((midi_keyboard_distribution_mask & (1<<myIndex))>0)
            {
                channelCheckboxes.get(midi_index).setCheckedWithoutEvent(true);
            }
        }
    }

    /**
     * Label the midi checkboxes 1:n
     */

    public void label_midi_boxes()
    {
        for(int midi_index=0; midi_index < n_midi_channels; midi_index++)
        {
            channelCheckboxes.get(midi_index).setChannelText(""+(midi_index+1));
        }
    }
}

