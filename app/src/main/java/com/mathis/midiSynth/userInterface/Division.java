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

import static android.widget.LinearLayout.VERTICAL;
import static java.lang.Integer.max;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.mathis.aeolusnative.AeolusSynth.AeolussynthManager;

import java.util.ArrayList;

/** Class for showing the user interface for division of the Aeolus synthesizer, implemented as
 * extension of the Android ConstraintLayout. There are n divisions, contained by the main
 * user interface element DivisionsMainScreen java. The division contains m stops.<br />
 * For the
 * purpose of the user interface coding, the division
 * consists in a set of stops that can be switched on or off and common division-level controls
 * such as volume or tremulus<br /><br />
 * Technically, in the interface layout of the "division" is described jointly by the
 * Aeolus "division" and the Aeolus "group" concept. The reason for the existence of the two distinct
 * levels is that the divisions contains the technical implementation of the ranks, which
 * are the actual sets of 1/2-tone-spaced pipes. Some of the stops shown in the user interface
 * are however mixtures of ranks, and
 * so a level of grouping is necessary, containing the mapping of which rank is associated with
 * which stop(s). This is the primary function of the "groups" in the Aeolus native implementation.<br />
 * However, the group
 * descriptors in the Aeolus implementation are associated 1:1 with the divisions.
 * Since in addition there seem to be some oddities of exactly how the
 * information is distributed between groups and divisions in the Aeolus native implementation, in the user
 * interface, only the term of division is used, the jni functions will look for the appropriate information
 * source in either the divisions, groups or both, depending on the user interface element.
 */
public class Division extends ConstraintLayout {

    /** Total number of divisions to show<br />
     * The number of divisions is required to calculate some of the
     * layouting coordinates as a function of the total number of divisions
     * to be shown. This variable is 0, and needs to be updated once Aeolus C initialization is
     * complete. This is done in DivisionsMainScreen, which handles the entire set of divisions.
     */
    public int n_divisions=0;

    /**
     * index of the current division among the divisions
     */
    protected int myIndex=0;

    /** The number of stops (aka on/off buttons for ranks
    * or sometimes mixtures of ranks) in the current division. The correct number of stops
     * can only be obtained once the Aeolus C-implementation is complete. The update is handled
     * by DivisionsMainScreen holding the divisions
     */
    public int n_stops =0;

    /**
     * List of the stops in this division
     */
    public ArrayList<Stop> stops = new ArrayList<Stop>();

    /** Common control user interface associated with this division */
    public DivisionCommonControls commonControls;

    /**
     * Horizontal split fraction between the part allocated to the stops (=split_fraction) and the
     * common division controls (1-split_fraction)
     */
    public float split_fraction=0.8f;

    /**
     * Set the number. Setting the number of stops also initializes the corresponding number of Stop elements
     * @param n The desired number of stops in this division
     */
    public void set_n_stops(int n)
    {

        n_stops=n;

        stops.clear();
        for(int i=0; i<n_stops; i++)
        {
            stops.add(new Stop(getContext()));
            stops.get(i).my_division_index = myIndex;
            stops.get(i).my_stop_index=i;
            stops.get(i).n_stops=n_stops;

            stops.get(i).setId(View.generateViewId());
            registerRow.addView(stops.get(i));
        }

    }

    /**
     * Update the active / inactive status of each stop in this division by querying the Aeolus
     * C implementation and correspondingly update graphical display
     */
    public void updateStopActiveSettingFromNative()
    {
        for(int i=0; i<n_stops; i++)
        {
            stops.get(i).setLayoutActivated(AeolussynthManager.getStopActivated(myIndex,i));

        }

    }

    /**
     * The label (name) of this division
     */

    public TextView labelText;

    /**
     * Horizontal layout containing the aligned stops of this division
     */
    public LinearLayout registerRow;

    /**
     * Vertical layout containing the division label on top and the registerRow on the bottom
     */
    public LinearLayout registerRowAndLabelText;


    /** Constructur with the application context. The contstructor initializes the constant elements of the division,
     * which are the different layout elements and the common divison level controls object. The stops and variable
     * elements of the common division level controls are initialized after loading of the Aeolus C implementation (via
     * the DivisionsMainScreen.updateUIFromNative method, which is invoked at completion of initialization of the
     * Aeolus native implementation)
     * @param context Apllication context from the Android Activity
     */
    public Division(Context context) {
        super(context);

        registerRowAndLabelText=new LinearLayout(context){

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

                int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
                //
                int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

                // This is subtle: the MeasureSpec with is the remaining available with
                // and
                int theWidth=(int)(parentWidth*split_fraction);
                this.setMeasuredDimension(theWidth, parentHeight);

                for(int i=0; i<n_stops; i++)
                {
                    stops.get(i).avalaible_width=theWidth;
                }
            }

        };

        registerRowAndLabelText.setOrientation(VERTICAL);
        registerRowAndLabelText.setGravity(Gravity.CENTER_HORIZONTAL);



        labelText = new TextView(context);

        registerRow = new LinearLayout(context);

        registerRowAndLabelText.addView(labelText);
        registerRowAndLabelText.addView(registerRow);
        registerRowAndLabelText.setId(View.generateViewId());

        addView(registerRowAndLabelText);

        commonControls = new DivisionCommonControls(context,this);


        addView(commonControls);

        ConstraintSet theConstraints=new ConstraintSet();
        theConstraints.clone(this);

        theConstraints.constrainPercentWidth(registerRowAndLabelText.getId(),0.8f);
        theConstraints.constrainPercentWidth(commonControls.getId(),0.2f);


        theConstraints.connect(registerRowAndLabelText.getId(),ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT);
        theConstraints.connect(registerRowAndLabelText.getId(),ConstraintSet.RIGHT, commonControls.getId(),ConstraintSet.LEFT);

        theConstraints.connect(commonControls.getId(),ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT);

        int viewIds[]=new int[2];
        viewIds[0]=registerRowAndLabelText.getId();
        viewIds[1]= commonControls.getId();

        theConstraints.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, viewIds, null, ConstraintSet.CHAIN_SPREAD);


        setConstraintSet(theConstraints);


    }


    /**
     * Set the label for this division
     * @param theLabel Label for this division
     */
    public void setDivisionLabel(String theLabel) {
        labelText.setText(theLabel);
    }

    /** Activate all the stops in this division.<br />
     * This is carried out in an asynchroneous matter. Here, the
     * activation of the stops is demanded from the C interface of Aeolus. Once the
     * C interface completes the activation, this is signalled back to the Java Android part via
     * AeolusUIManager.onStopsUpdated(). Through a cascade of notified objects consisting of the activity and
     * DivisionsMainScreen, the divisions are instructed to update the graphical layout of the
     * stops via updateStopActiveSettingFromNative(). The purpose of this approach is to
     * ensure that the stops' graphism reflects
     * the state of the Aeolus synthesizer, not just the demand.
     */

    public void activateAll() {
        for(int stop_to_activate=0; stop_to_activate<n_stops; stop_to_activate++)
        {
            AeolussynthManager.activateStop(myIndex,stop_to_activate);

        }
    }

    /** Deactivate all the stops in this division.<br />
     * This is carried out in an asynchroneous matter. Here, the
     * deactivation of the stops is demanded from the C interface of Aeolus. Once the
     * C interface completes the deactivation, this is signalled back to the Java Android part via
     * AeolusUIManager.onStopsUpdated(). Through a cascade of notified objects consisting of the activity and
     * DivisionsMainScreen, the divisions are instructed to update the graphical layout of the
     * stops via updateStopActiveSettingFromNative(). The purpose of this approach is to ensure that the stops' graphism reflects
     * the state of the Aeolus synthesizer, not just the demand.
     */

    public void deactivateAll() {
        for(int stop_to_deactivate=0; stop_to_deactivate<n_stops; stop_to_deactivate++)
        {
            AeolussynthManager.deactivateStop(myIndex,stop_to_deactivate);

        }
    }

    /**
     * Set the volume
     * @param vol Linear gain factor value.
     */

    public void setVolume(float vol)
    {
        commonControls.setVolume(vol);
    }

    /** Get the volume as a linear gain factor
     * @return Volume as linear gain factor (i.e. 10^(dB/20))
     */
    public float getVolume()
    {
        return commonControls.getVolume();

    }

    /**
     * Override of the parent onMeasure member function for the purpose of allocating equal height
     * to each division. The onMeasure function is called when the layout widht are actually calculated.
     * Here, the total number of divisions n_divisions is used to allocate equal vertical space
     * to each division, as parent_height/n_divisions
      */


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        //
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        // This is subtle: the MeasureSpec with is the remaining available with
        // and
        this.setMeasuredDimension(parentWidth, parentHeight/max(n_divisions,1));


    }

    /** Do we show a tremulant button or not?
     *
     * @param hasTremulant If true, show a tremulant button
     */
    public void setHasTremulantButton(boolean hasTremulant) {
        commonControls.setHasTremulant(hasTremulant);
    }

    /** Update the common buttons for the division due to changes that occurred externally (on the level of
     * the C Aeolus implementation)
     */
    public void updateCommonButtons() {
        commonControls.updateTremulantVisibility();
    }
}
