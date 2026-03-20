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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.R;

/** Graphical use interface for division-level controls (volume, all on/off, ...)
 *
 */

public class DivisionCommonControls extends LinearLayout {

    // Slidebar for showing and choosing division-specific volume
    public SeekBar division_volume=null;

    // Clicking this button de-activates all the stops of the associated division
    Button toggleoff= null;

    // Clicking this button activates all the stops of the associated divison

    Button toggleon= null;

    // Button for tremulant. Not all divisions have a tremulant button, but if there is one,
    // it will be held by this field
    Button tremulant=null;

    // Icon for showing that the tremulant has been activated (only relevant if there
    ImageView tremulantActivated =null;

    // Reference to the parent division
    Division parentDivision=null;




    LinearLayout tremulantLayout=null;


    boolean hasTremulant=false;



    public void setParentDivision(Division p)
    {
        parentDivision=p;
    }

    /**
     * Constructor reads the layout from xml and inflates
     * @param context Context needed for view construction
     */

    public DivisionCommonControls(Context context)
    {
        super(context);
        setOrientation(VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedLayout = inflater.inflate(R.layout.division_common_controls_layout, this);
        division_volume = inflatedLayout.findViewById(R.id.divisionVolume);
        division_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) // Avoid update loop when the update comes from the UI
                {
                    AeolussynthManager.setDivisionVolume(parentDivision.myIndex,getVolume());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });





        tremulantLayout = inflatedLayout.findViewById(R.id.tremulant_layout);





        toggleoff=inflatedLayout.findViewById(R.id.all_division_stops_off);

        toggleoff.setOnClickListener(v -> parentDivision.deactivateAll());

        toggleon= inflatedLayout.findViewById(R.id.all_division_stops_on);

        toggleon.setOnClickListener(v -> parentDivision.activateAll());

        setId(View.generateViewId());


    }

    /**
     * Default constructors. It is safest to directly provide the parent division
     * in the constructor. If you cannot, for example because you use this class
     * in an xml layout, do not forget to provide after object construction
     * via a call to setParentDivision
     * @param context Context needed for view construction
     * @param theParent Division for which this common division control UI element will be
     *                  working
     */
    public DivisionCommonControls(Context context, Division theParent) {
        this(context);
        setParentDivision(theParent);

    }

    /** Set the volume for this division to be shown in the user interface
     * The volume should be provided on a linear scale, with 1=do not change it, >1 = amplify,
     * <1=attenuate. For graphical display, the volume will be shown on a linear scale and limited
     * to minimal and maximal values as compatible with the seekbar
     * @param vol Volume on linear scale, with 1=unchanged
     */

    public void setVolume(float vol)
    {
        int p=(int)Math.round(20.0f*Math.log10((double)vol));
        if(p<division_volume.getMin()){ p=division_volume.getMin(); }
        if(p>division_volume.getMax()) {p=division_volume.getMax(); }
        division_volume.setProgress(p);
    }

    /** Get the volume is a linear gain factor
     * @return Volume as linear gain factor (i.e. 10^(dB/20))
     */
    public float getVolume()
    {
        return (float)Math.pow(10.0f,((double)division_volume.getProgress())/20.0f);

    }

    /**
     * Set whether or not a tremulant button should be shown
     * @param hasTremulantNewValue True if a tremulant button should be shown, false otherwise
     */
    public void setHasTremulant(boolean hasTremulantNewValue) {
        if(hasTremulantNewValue != hasTremulant) // we need to change
        {
            hasTremulant=hasTremulantNewValue;
            if(hasTremulant)
            {
                tremulant = new Button(getContext());
                tremulant.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AeolussynthManager.toggleTremulant(parentDivision.myIndex);

                    }
                });

                tremulant.setText("Tremulant");
                tremulantLayout.addView(tremulant);

                if(tremulantActivated==null)
                {
                    tremulantActivated=new ImageView(getContext());
                    tremulantActivated.setImageDrawable(getResources().getDrawable(R.drawable.tremulant_activated,getContext().getTheme()));

                    tremulantLayout.addView(tremulantActivated);

                }

               updateTremulantVisibility();


            } else {
                tremulantLayout.removeView(tremulant);
                tremulant=null;
                tremulantLayout.removeView(tremulantActivated);
                tremulantActivated=null;

            }
        }
    }

    /** Update the visibility of the tremulant indicator
     * The tremulant indicator (by default, a green circle) should be visible
     * only when the division has a tremulant and if that tremulant is currently activated.
     * Calling this function causes synchronization with the Aeolus C implementation to correctly indicate
     * whether the tremulant is at present active.
     */
    protected void updateTremulantVisibility()
    {

        if(tremulantActivated!=null)
        {
            if(AeolussynthManager.tremulantIsActive(parentDivision.myIndex))
            {
                tremulantActivated.setVisibility(VISIBLE);

            } else {
                tremulantActivated.setVisibility(INVISIBLE);
            }

        }

    }


}
