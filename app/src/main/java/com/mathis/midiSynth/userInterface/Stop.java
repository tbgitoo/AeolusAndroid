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

import static java.lang.Integer.max;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.R;

/**
 * Stop in the main Aeolus user interface, represents a register (in Aeolus terminology, a rank)
 * or sometimes a mixture of registers (ranks). Can be set on or off by the user. xml layout used:
 * stop.xml
 */
public class Stop extends ConstraintLayout {

    /**
     * The division to which this stop belongs
     */
    public int my_division_index=0;
    /**
     * The index of this stop within the division
     */
    public int my_stop_index=0;

    public int n_stops=0; // For layouting, the total number of stops

    /**
     * Is this stop currently active (playing notes, provided notes
     * are actually received)?
     */
    public boolean isActive=false;

    /**
     * The label of this stop in the user interface
     */
    public TextView labelText;
    /**
     * The image representing this stop
     */
    public ImageView imageButton;

    /**
     * Layout parameters to format the image button
     */
    protected ViewGroup.LayoutParams imageButtonLayout;

    /**
     * Will be calculated when more data is avaiable (before drawing, in the
     * onMeasure method of the division holding this stop)
     */
    public int avalaible_width=600;

    /**
     * My current width
     */
    public int my_width=0;

    /**
     * Constructor, including layout inflation from stop.xml
     * @param context app activitity context
     */

    public Stop(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        View inflatedLayout = inflater.inflate(R.layout.stop,this);

        labelText = inflatedLayout.findViewById(R.id.stopLabelText);




        imageButton = inflatedLayout.findViewById(R.id.stopImageView);


        setClickable(true);
         isActive=false;
        setOnClickListener(v -> {

            if(!isActive) {
                AeolussynthManager.activateStop(my_division_index, my_stop_index);
            }
            if(isActive){
                AeolussynthManager.deactivateStop(my_division_index,my_stop_index);
            }

        });


    }

    /**
     * Set the text of this stop
     * @param theLabel The text to be displayed for this stop
     */

    public void setStopText(String theLabel)
    {
        labelText.setText(theLabel);
    }


    /**
     * Set whether this stop is activated (without triggering the event
     * chain). The stop layout status is set asynchronously
     * @param stopActivated
     */
    public void setLayoutActivated(boolean stopActivated) {
        isActive=stopActivated;
       if(stopActivated)
       {
           imageButton.setImageDrawable(getResources().getDrawable(R.drawable.aeolus_stop_active,getContext().getTheme()));

       } else {
           imageButton.setImageDrawable(getResources().getDrawable(R.drawable.aeolus_stop_inactive,getContext().getTheme()));

       }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        //
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);



        my_width=(int)(avalaible_width/max(n_stops,1));



        this.setMeasuredDimension(my_width, parentHeight);


        imageButtonLayout=imageButton.getLayoutParams();
        imageButtonLayout.width=my_width;
        imageButton.setLayoutParams(imageButtonLayout);


    }
}
