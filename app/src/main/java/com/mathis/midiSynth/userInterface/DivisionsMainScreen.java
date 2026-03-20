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
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;
import com.mathis.midiSynth.R;

import java.util.ArrayList;

/**
 * Class representing the graphical interface with ensemble of the divisions of Aeolus
 */
public class DivisionsMainScreen {

// Number of divisions read from Aeolus. Before completion of Aeolus initialization, this is set to 0
    public int n_divisions=0;

// Array holding the divisions. Before completion of Aeolus initialization, this is set to an
    // empty array
    public ArrayList<Division> divisions = new ArrayList<Division>();

    ///  Context from the Android app activity
    Context theContext=null;


    ConstraintLayout baseLayout;

    public DivisionsMainScreen(Context c, ConstraintLayout b)
    {
        theContext=c;
        baseLayout=b;

    }

    public void updateUIFromNative() {
        // implement this in derived classes


        n_divisions = AeolussynthManager.getNumberDivisions();


        divisions.clear();
        for (int index = 0; index < n_divisions; index++) {
            divisions.add(new Division(theContext));
            divisions.get(index).n_divisions = n_divisions;
            divisions.get(index).myIndex = index;
            divisions.get(index).setDivisionLabel(AeolussynthManager.getLabelForDivision(index));
            divisions.get(index).set_n_stops(AeolussynthManager.get_n_StopsForDivision(index));
            divisions.get(index).setVolume(AeolussynthManager.getDivisionVolume(index));
            for (int index_stop = 0; index_stop < divisions.get(index).n_stops; index_stop++) {
                divisions.get(index).stops.get(index_stop).setStopText(
                        AeolussynthManager.getStopLabel(index, index_stop).replace("$", "\n"));
                divisions.get(index).stops.get(index_stop).setLayoutActivated(
                        AeolussynthManager.getStopActivated(index, index_stop));
            }
            divisions.get(index).setHasTremulantButton(AeolussynthManager.hasTremulant(index));


        }
    }

    /** Run this on the uithread */
    public void addDivisionsToBaseLayout() {
        for(int index=0; index<n_divisions; index++) {


            if( divisions.get(index).getParent() != null) {
                ((ViewGroup) divisions.get(index).getParent()).removeView( divisions.get(index)); // <- fix
            }
            divisions.get(index).setId(View.generateViewId());

            baseLayout.addView(divisions.get(index));




            onDivisionLayout(divisions.get(index));

        }
        ConstraintSet theConstraints=new ConstraintSet();
        theConstraints.clone(baseLayout);

        int viewIds[]=new int[n_divisions];
        for(int i=0; i<n_divisions; i++) {
            viewIds[i]=divisions.get(i).getId();
            if(i==0) {
                theConstraints.connect(divisions.get(i).getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            } else {
                theConstraints.connect(divisions.get(i-1).getId(), ConstraintSet.TOP, divisions.get(i).getId(), ConstraintSet.TOP);
            }
            if(i==n_divisions-1)
            {
                theConstraints.connect(divisions.get(i).getId(), ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            }

            theConstraints.connect(divisions.get(i).getId(), ConstraintSet.LEFT,ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            theConstraints.connect(divisions.get(i).getId(), ConstraintSet.RIGHT,ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);


            //apply the default width and height constraints in code






        }




        theConstraints.createVerticalChain(ConstraintSet.PARENT_ID, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, viewIds, null, ConstraintSet.CHAIN_SPREAD);
        theConstraints.applyTo(baseLayout);
    }

    public void divisionsUpdateCommonButtons()
    {
        for(int index=0; index<n_divisions; index++) {
            divisions.get(index).updateCommonButtons();

        }
    }

    protected void onDivisionLayout(Division division) {


        division.setBackground(AppCompatResources.getDrawable(theContext,R.drawable.color_gradient_manual_iii));

        //division.setBackground(new ColorDrawable(getResources().getColor(R.color.grey_task_bar)));

    }

    public void updateStopActiveSettingFromNative() {
        for(int index=0; index<n_divisions; index++) {
            divisions.get(index).updateStopActiveSettingFromNative();
            divisions.get(index).updateCommonButtons();
        }
    }

    public void activateAll() {
        for(int i = 0; i<n_divisions; i++){
           divisions.get(i).activateAll();
        }
    }

    public void deactivateAll() {
        for(int i = 0; i<n_divisions; i++){
            divisions.get(i).deactivateAll();
        }
    }
}
