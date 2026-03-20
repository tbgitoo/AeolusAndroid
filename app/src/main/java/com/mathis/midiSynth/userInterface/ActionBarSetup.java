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

import androidx.appcompat.app.ActionBar;

/**
 * Base class for setting up action bars at the top of the Activity windows
 */

public class ActionBarSetup {

    /** The action bar to be handled. Get this with  getSupportActionBar() in an Android activity and
     * supply it in the constructor
     */
    ActionBar actionBar = null;
    /** Application context, get this this with getApplicationContext() from an Activity and supply it
     * with the constructor
     */
    Context context=null;
    /**
     * Constructor of the action bar setup for a basic title in the action bar
     * @param heading Header string to be displayed
     * @param appActionBar Reference to the action bar. This needs to be obtained by the App Activity class, via getSupportActionBar()
     * @param theContext Get this context from the App Activity (that is, specifically
     *                   , via getApplicationContext(). This serves to find ressources like
     *                   drawables and colors
     */
    public ActionBarSetup(String heading, ActionBar appActionBar,
                          Context theContext)
    {
        actionBar=appActionBar;
        context=theContext;

        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle(heading);


        }

    }

    /**
     * Show the action bar
     */
    public void show()
    {
        actionBar.show();
    }
}
