/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eolwral.osmonitor;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.TabHost;

import com.eolwral.osmonitor.messages.DebugBox;
import com.eolwral.osmonitor.misc.MiscBox;
import com.eolwral.osmonitor.preferences.Preferences;
import com.eolwral.osmonitor.processes.ProcessList;
import com.eslab.osmonitor.traffic.ApplicationTrafficList;

public class OSMonitor extends TabActivity
{
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
 
    	// load settings
   		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
   		
   		if(settings.getBoolean(Preferences.PREF_HIDEAPPBAR, true))
   	        requestWindowFeature(Window.FEATURE_NO_TITLE);
   			
        if(settings.getBoolean(Preferences.PREF_STATUSBAR, false))
        	if(OSMonitorService.getInstance() == null)
        		startService(new Intent(this, OSMonitorService.class));
        
        // detect screen size
        CompareFunc.detectScreen(this);

        // create view
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);

        // set title
        this.setTitle(R.string.app_title);
        
        
        // create tab
        TabHost mTabHost = getTabHost();
        
        mTabHost.addTab(mTabHost.newTabSpec("MiscTab")
                .setIndicator(getResources().getText(R.string.misc_tab), getResources().getDrawable(R.drawable.misc))
                .setContent(new Intent(this, MiscBox.class)));         
        
        mTabHost.addTab(mTabHost.newTabSpec("NetworkTab")
                .setIndicator(getResources().getText(R.string.network_tab), getResources().getDrawable(R.drawable.network))
                .setContent(new Intent(this, ApplicationTrafficList.class)));
/*
        mTabHost.addTab(mTabHost.newTabSpec("ConnectionTab")
                .setIndicator(getResources().getText(R.string.connection_tab), getResources().getDrawable(R.drawable.connection))
                .setContent(new Intent(this, NetworkList.class)));
*/
        mTabHost.addTab(mTabHost.newTabSpec("ProcessTab")
                .setIndicator(getResources().getText(R.string.process_tab), getResources().getDrawable(R.drawable.process))
                .setContent(new Intent(this, ProcessList.class)));
       
        mTabHost.addTab(mTabHost.newTabSpec("DebugMsgTab")
                .setIndicator(getResources().getText(R.string.debug_tab), getResources().getDrawable(R.drawable.debug))
                .setContent(new Intent(this, DebugBox.class)));
               
        mTabHost.addTab(mTabHost.newTabSpec("GraphTab")
                .setIndicator(getResources().getText(R.string.graph_tab), getResources().getDrawable(R.drawable.graph_icon))
                .setContent(new Intent(this, GraphTutorial_cutom.class)));
                
    }
}
