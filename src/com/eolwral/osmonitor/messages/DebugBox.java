/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.eolwral.osmonitor.messages;

import java.io.File;
import java.io.FileWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.eolwral.osmonitor.*;
import com.eolwral.osmonitor.preferences.Preferences;

public class DebugBox extends Activity implements OnGestureListener, OnTouchListener
{
	private JNIInterface JNILibrary = JNIInterface.getInstance();
	
	private DebugBox Self = null;
	private BaseAdapter UpdateInterface = null;	
	private ListView InternalList = null;
	private TextView EmptyMsg = null;
	private TextView MsgCountText = null;
	private String Mode = "dmesg";
	
	private boolean FirstView = false;
	private boolean FreezeIt = false;
	
	// watch log
	private int targetPID = 0; 
    
	// Gesture
	private GestureDetector gestureScanner = new GestureDetector(this);
	
	@Override
	public boolean onTouchEvent(MotionEvent me)
	{
		return gestureScanner.onTouchEvent(me);
	}
	
	@Override
	public boolean onDown(MotionEvent e)
	{
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		if(targetPID != 0)
			return false;
		
		try {
			if (Math.abs(e1.getY() - e2.getY()) > CompareFunc.SWIPE_MAX_OFF_PATH)
				return false;
			else if (e1.getX() - e2.getX() > CompareFunc.SWIPE_MIN_DISTANCE &&
						Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) 
				((TabActivity) this.getParent()).getTabHost().setCurrentTab(0);
			else if (e2.getX() - e1.getX() > CompareFunc.SWIPE_MIN_DISTANCE && 
						Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) 
				((TabActivity) this.getParent()).getTabHost().setCurrentTab(3);
			else
				return false;
		} catch (Exception e) {
			// nothing
		}

		return true;
	}
	
	@Override
	public void onLongPress(MotionEvent e)
	{
		return;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		return false;
	}
	
	@Override
	public void onShowPress(MotionEvent e)
	{
		return;
	} 
	
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if(gestureScanner.onTouchEvent(event))
			return true;
		else
		{
			if(v.onTouchEvent(event))
				return true;
			return false;
		}
	}
	
	
	// Refresh
    private Runnable runnable = new Runnable() {
		public void run() {

 			if(!FreezeIt){
 				if(JNILibrary.doDataLoad() == 1)
 				{
 			    	if(Mode.equals("dmesg"))
 				    	MsgCountText.setText(""+JNILibrary.GetDebugMessageCounts());
 				    else
 				    	MsgCountText.setText(""+JNILibrary.GetLogcatCounts());
 					
 					if(EmptyMsg != null)
 						EmptyMsg.setText("");

 					Self.onRefresh();
 				}
 			}
	        handler.postDelayed(this, 1000);
		}
	};   
	Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.debuglayout);
        
		Self = this;
		((ListView) findViewById(R.id.debuglist)).setOnTouchListener(this);
        EmptyMsg = (TextView) findViewById(R.id.debugempty);
        MsgCountText = (TextView) findViewById(R.id.debugmsgcounts);
        
        // Freeze
        CheckBox Freeze = (CheckBox) findViewById(R.id.debugmsgfreeze);
        Freeze.setOnClickListener(
        	new OnClickListener(){
        		public void onClick(View v) {
        			if(FreezeIt)
        			{
        				FreezeIt = false;
        			}
        			else
        			{
        				FreezeIt = true;
        			}
				}
        	}
        );
    }
    
    public void onRefresh()
    {
		JNILibrary.doDataSwap();
		UpdateInterface.notifyDataSetChanged();

		if(FirstView)
    	{
    		InternalList.setSelection(UpdateInterface.getCount());
			FirstView = false;
    	}

    }
    
    public boolean onCreateOptionsMenu(Menu optionMenu) 
    {
     	super.onCreateOptionsMenu(optionMenu);
     	
     	if(targetPID == 0)
         	optionMenu.add(0, 1, 0, getResources().getString(R.string.options_text));

     	optionMenu.add(0, 2, 0, getResources().getString(R.string.logexport_title));

     	if(targetPID == 0)
           	optionMenu.add(0, 4, 0, getResources().getString(R.string.aboutoption_text));
     	else
     		optionMenu.add(0, 3, 0, getResources().getString(R.string.filterlevel_text));

       	optionMenu.add(0, 5, 0, getResources().getString(R.string.forceexit_text));
    	return true;
    }



	private void restorePrefs()
    {
		// load settings
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
		try {
			JNILibrary.doDataTime(Integer.parseInt(settings.getString(Preferences.PREF_UPDATE, "2")));
		} catch(Exception e) {}

        Mode = settings.getString(Preferences.PREF_LOGTYPE, "dmesg");
        if(Mode.equals("dmesg"))
        {
            InternalList = (ListView) findViewById(R.id.debuglist);
            InternalList.setEmptyView(findViewById(R.id.debugempty));
            InternalList.setAdapter(new DMesgListAdapter(this));
            InternalList.setOnItemClickListener(NullListener);
            UpdateInterface = (DMesgListAdapter) InternalList.getAdapter();
        }
        else
        {
        	InternalList = (ListView) findViewById(R.id.debuglist);
            InternalList.setEmptyView(findViewById(R.id.debugempty));
            InternalList.setAdapter(new LogcatListAdapter(this));
            InternalList.setOnItemClickListener(InternalListListener);
            UpdateInterface = (LogcatListAdapter) InternalList.getAdapter();
        }
        
		if(settings.getBoolean(Preferences.PREF_DMESGUSEFILTER, false))
		{
			JNILibrary.SetDebugMessageFilter(1);
			JNILibrary.SetDebugMessage(settings.getString(Preferences.PREF_DMESGFILTERSTR, ""));
			JNILibrary.SetDebugMessageLevel(Integer.parseInt(settings.getString(Preferences.PREF_DMESGFILTERLV, "8")));
		}
		else
			JNILibrary.SetDebugMessageFilter(0);
		
		try {
			JNILibrary.SetLogcatSource(Integer.parseInt(settings.getString(Preferences.PREF_LOGCATSOURCE, "0")));
		} 
		catch(Exception e)
		{
			JNILibrary.SetLogcatSource(0);
		}
		
		if(settings.getBoolean(Preferences.PREF_LOGCATUSEFILTER, false))
		{
			JNILibrary.SetLogcatFilter(1);
			JNILibrary.SetLogcatMessage(settings.getString(Preferences.PREF_LOGCATFILTERSTR, ""));

			try {
				JNILibrary.SetLogcatLevel(Integer.parseInt(settings.getString(Preferences.PREF_LOGCATFILTERLV, "0")));
			} 
			catch(Exception e)
			{
				JNILibrary.SetLogcatLevel(0);
			}
				
			try {
				JNILibrary.SetLogcatPID(Integer.parseInt(settings.getString(Preferences.PREF_LOGCATFILTERPID, "0")));
			} 
			catch(Exception e)
			{
				JNILibrary.SetLogcatPID(0);
			}
		}
		else
			JNILibrary.SetLogcatFilter(0);
		
        if(settings.getBoolean(Preferences.PREF_STATUSBAR, false))
        {
        	if(OSMonitorService.getInstance() == null)
        		startService(new Intent(this, OSMonitorService.class));
        	else
        		OSMonitorService.getInstance().Notify();
        }
        else
        	if(OSMonitorService.getInstance() != null)
        		OSMonitorService.getInstance().stopSelf();
		
    }
	
	private void setTarget()
	{
		// load settings
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
		try {
			JNILibrary.doDataTime(Integer.parseInt(settings.getString(Preferences.PREF_UPDATE, "2")));
		} catch(Exception e) {}
		
		Mode = "logcat";
    	InternalList = (ListView) findViewById(R.id.debuglist);
        InternalList.setEmptyView(findViewById(R.id.debugempty));
        InternalList.setAdapter(new LogcatListAdapter(this));
        InternalList.setOnItemClickListener(InternalListListener);
        UpdateInterface = (LogcatListAdapter) InternalList.getAdapter();
        
		JNILibrary.SetLogcatFilter(1);
		JNILibrary.SetLogcatLevel(JNILibrary.doLogcatNONE);
		JNILibrary.SetLogcatMessage("");
		JNILibrary.SetLogcatSource(0);
		JNILibrary.SetLogcatPID(targetPID);
		
	}
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	restorePrefs();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) 
    {
    	switch (id)
    	{
    	case 0:
        	return new AlertDialog.Builder(this)
			   .setIcon(R.drawable.monitor)
			   .setTitle(R.string.app_name)
			   .setMessage(R.string.about_text)
			   .setPositiveButton(R.string.aboutbtn_text,
			   new DialogInterface.OnClickListener() {
				   public void onClick(DialogInterface dialog, int whichButton) { } })
			   .create();
        	
    	case 1:
    		LinearLayout Layout = new LinearLayout(this);
    		Layout.setOrientation(LinearLayout.VERTICAL);
    		Layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
    									LayoutParams.FILL_PARENT));
    		
    		EditText FileName = new EditText(this);
    		FileName.setId(100);

    		CheckBox UseHTML = new CheckBox(this);
    		UseHTML.setId(200);
    		UseHTML.setText("HTML Format");
    		
    		Layout.addView(FileName, 0);
    		Layout.addView(UseHTML, 1);
    		
    		return new AlertDialog.Builder(this)
            .setTitle(R.string.exportfile_title)
            .setView(Layout)
            .setPositiveButton(R.string.btnok_title, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	String FileName = ((EditText)((AlertDialog)dialog).findViewById(100)).getText().toString();
                	Boolean useHTML = ((CheckBox)((AlertDialog)dialog).findViewById(200)).isChecked();
                	SaveLog(FileName, useHTML);
                }
            })
            .setNegativeButton(R.string.btncancel_title, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    /* User clicked cancel so do some stuff */
                }
            })
            .create();
    		
    	case 2:
    		LinearLayout FilterLayout = new LinearLayout(this);
    		FilterLayout.setOrientation(LinearLayout.VERTICAL);
    		FilterLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
    									LayoutParams.FILL_PARENT));
    		
    		Spinner LogLevel = new Spinner(this);
    		LogLevel.setId(100);
    		ArrayAdapter<?> LevelAdapter = ArrayAdapter.createFromResource(
    		            this, R.array.logcat_level_list, android.R.layout.simple_spinner_item);
    		LevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		LogLevel.setAdapter(LevelAdapter);
    		
    		FilterLayout.addView(LogLevel, 0);
    		
    		return new AlertDialog.Builder(this)
            .setTitle(R.string.filterlevel_text)
            .setView(FilterLayout)
            .setPositiveButton(R.string.btnok_title, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	long Level = ((Spinner)((AlertDialog)dialog).findViewById(100)).getSelectedItemId()+1;
                	if(Level == 1) Level = 0;
    				JNILibrary.SetLogcatLevel((int) Level);
                }
            })
            .setNegativeButton(R.string.btncancel_title, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();    		
    	}
    	
    	return null;
    }
    
    private void SaveLog(String FileName, Boolean useHTML)
    {
    	if(FileName.trim().equals(""))
    		return;
    	
    	try {
        	File LogFile = new File("/sdcard/" + FileName);
    		
        	if (LogFile.exists())
        	{
        		new AlertDialog.Builder(this)
    		   		.setIcon(R.drawable.monitor)
    		   		.setTitle(R.string.app_name)
    		   		.setMessage(R.string.exportexist_title)
    		   		.setPositiveButton(R.string.btnok_title,
    		   				new DialogInterface.OnClickListener() {
    		   			public void onClick(DialogInterface dialog, int whichButton) { } })
    		   		.create()
    		   		.show();
        		return;
        	}

        	LogFile.createNewFile();
        	
        	int LogCount = 0;
        	if(Mode.equals("dmesg"))
        		LogCount = JNILibrary.GetDebugMessageCounts();
        	else
        		LogCount = JNILibrary.GetLogcatCounts();
        	
        	FileWriter TextFile = new FileWriter(LogFile);
        	
        	if(useHTML)
        		TextFile.write("<html>\n<body>\n<table>\n");
        	
        	for(int i = 0; i < LogCount;i++)
        	{
        		String TextLine = "";
        		if(Mode.equals("dmesg"))
        		{
        			if(useHTML)
       					TextLine = "<tr><td>"+JNILibrary.GetDebugMessageTime(i)+ "</td><td> ["
   								+ JNILibrary.GetDebugMessageLevel(i) + "] </td><td>"
   								+ JNILibrary.GetDebugMessage(i) + "</td></tr>\n";
       				else
       					TextLine = JNILibrary.GetDebugMessageTime(i)+ " ["
       						+ JNILibrary.GetDebugMessageLevel(i) + "] "
       						+ JNILibrary.GetDebugMessage(i) + "\n";
        		}
        		else
        		{
        			if(useHTML)
        				TextLine = "<tr><td>"+JNILibrary.GetLogcatTime(i) + "</td><td> ["
        						+ JNILibrary.GetLogcatLevel(i)+ "] </td><td>"
        						+ JNILibrary.GetLogcatTag(i) + "("
								+ JNILibrary.GetLogcatPID(i) + ") </td><td>"
								+ JNILibrary.GetLogcatMessage(i) + "</td></tr>\n";
        			else
        				TextLine = JNILibrary.GetLogcatTime(i) + " ["
        						+ JNILibrary.GetLogcatLevel(i)+ "] "
        						+ JNILibrary.GetLogcatTag(i) + "("
        						+ JNILibrary.GetLogcatPID(i) + ") "
        						+ JNILibrary.GetLogcatMessage(i) + "\n";
        		}
        		TextFile.write(TextLine);
        	}

        	if(useHTML)
        		TextFile.write("</table>\n</body>\n</html>\n");

        	TextFile.close();

    	} catch (Exception e) {
    		new AlertDialog.Builder(this)
	   		.setIcon(R.drawable.monitor)
	   		.setTitle(R.string.app_name)
	   		.setMessage(e.getMessage())
	   		.setPositiveButton(R.string.btnok_title,
	   				new DialogInterface.OnClickListener() {
	   			public void onClick(DialogInterface dialog, int whichButton) { } })
	   		.create()
	   		.show();

    		return;
    	}
    	
  		new AlertDialog.Builder(this)
   		.setIcon(R.drawable.monitor)
   		.setTitle(R.string.app_name)
   		.setMessage(R.string.exportdone_title)
   		.setPositiveButton(R.string.btnok_title,
   				new DialogInterface.OnClickListener() {
   			public void onClick(DialogInterface dialog, int whichButton) { } })
   		.create()
   		.show();

  		return;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	
        super.onOptionsItemSelected(item);
        switch(item.getItemId())
        {
        case 1:
        	if(targetPID == 0)
        	{
        		Intent launchPreferencesIntent = new Intent().setClass( this, Preferences.class);
        		startActivityForResult(launchPreferencesIntent, 0);
        	}
        	break;
        	
        case 2:
        	this.showDialog(1);
        	break;
        
        case 3:
        	this.showDialog(2);
        	break;
        	
        case 4:
        	this.showDialog(0);
        	break;
        
    	case 5:
        	if(OSMonitorService.getInstance() != null)
        		OSMonitorService.getInstance().stopSelf();

        	JNILibrary.killSelf(this);
        	
    		break;
        }

        
        return true;
    }
    

    @Override
    public void onPause() 
    {
    	handler.removeCallbacks(runnable);
    	JNILibrary.doTaskStop();

    	super.onPause();
    }

    @Override
    protected void onResume() 
    {    
    	if (getIntent().getExtras() != null)
    		targetPID = this.getIntent().getExtras().getInt("targetPID", 0);
    	else
    		targetPID = 0;

		if(targetPID == 0)
    	{
    		restorePrefs();
        
    		if(Mode.equals("dmesg"))
    			JNILibrary.doTaskStart(JNILibrary.doTaskDMesg);
    		else
    			JNILibrary.doTaskStart(JNILibrary.doTaskLogcat);
    	}
    	else
    	{
    		setTarget();
			JNILibrary.doTaskStart(JNILibrary.doTaskLogcat);
    	}
    	
		FirstView = true;
    	
    	handler.post(runnable);
    	super.onResume();
    }
    
    private OnItemClickListener InternalListListener = new OnItemClickListener()
    {
    	@Override
    	public void onItemClick(AdapterView<?> l, View v, int position, long id) 
    	{  
    		if(position > JNILibrary.GetLogcatCounts())
    			position = JNILibrary.GetLogcatCounts();
    		
    		AlertDialog.Builder LogcatInfo = new AlertDialog.Builder(l.getContext());
    		LogcatInfo.setTitle("Message");
    		LogcatInfo.setMessage(JNILibrary.GetLogcatMessage(position));
    		LogcatInfo.show();    		
    	}
    };
    
    private OnItemClickListener NullListener = new OnItemClickListener()
    {
    	@Override
    	public void onItemClick(AdapterView<?> l, View v, int position, long id) 
    	{  
    	}
    };
    
    private class DMesgListAdapter extends BaseAdapter {
        public DMesgListAdapter(Context context)
        {
            mContext = context;
        }

        public int getCount() {
           return JNILibrary.GetDebugMessageCounts();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            DMesgDetailView sv;
            
            if (convertView == null) {
                sv = new DMesgDetailView(mContext, JNILibrary.GetDebugMessageTime(position),
                								   JNILibrary.GetDebugMessageLevel(position),
                								   JNILibrary.GetDebugMessage(position),
                								   position);
            } else {
                sv = (DMesgDetailView)convertView;
                sv.setContext( JNILibrary.GetDebugMessageTime(position),
                			   JNILibrary.GetDebugMessageLevel(position),
						   	   JNILibrary.GetDebugMessage(position),
						 	  position);
            }
            
            return sv;
        }

        private Context mContext;
    }
    
    private class DMesgDetailView extends TableLayout {

    	private TextView DMesgLevel;
    	private TextView DMesgMsg;
    	
        public DMesgDetailView(Context context,String Time, String Level, String Msg, int position) 
        {
            super(context);
            
            DMesgMsg = new TextView(context);
            DMesgLevel = new TextView(context);

            DMesgLevel.setGravity(Gravity.LEFT);
            DMesgLevel.setPadding(3, 3, 3, 3);
            DMesgLevel.setWidth(60);

            DMesgMsg.setGravity(Gravity.LEFT);
            DMesgMsg.setPadding(3, 3, 3, 3);
            DMesgMsg.setWidth(getWidth()-60);
            
            DMesgLevel.setText(Time+" ["+Level+"]");
            
            if(Level.endsWith("EMERGENCY"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("ALERT"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("CRITICAL"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("ERROR"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("WARNING"))
                DMesgLevel.setTextColor(Color.YELLOW);
            else if (Level.endsWith("NOTICE"))
                DMesgLevel.setTextColor(Color.MAGENTA);
            else if (Level.endsWith("INFORMATION"))
                DMesgLevel.setTextColor(Color.GREEN);
            else if (Level.endsWith("DEBUG"))
                DMesgLevel.setTextColor(Color.BLUE);

            DMesgMsg.setText(Msg);
            
            addView(DMesgLevel);
            addView(DMesgMsg);
            
	     	if(position % 2 == 0)
	     		setBackgroundColor(0x80444444);
	     	else
	     		setBackgroundColor(0x80000000);

        }
        
		public void setContext(String Time, String Level, String Msg, int position) 
		{
            DMesgLevel.setText(Time+" ["+Level+"]");

            if(Level.endsWith("EMERGENCY"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("ALERT"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("CRITICAL"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("ERROR"))
                DMesgLevel.setTextColor(Color.RED);
            else if (Level.endsWith("WARNING"))
                DMesgLevel.setTextColor(Color.YELLOW);
            else if (Level.endsWith("NOTICE"))
                DMesgLevel.setTextColor(Color.MAGENTA);
            else if (Level.endsWith("INFORMATION"))
                DMesgLevel.setTextColor(Color.GREEN);
            else if (Level.endsWith("DEBUG"))
                DMesgLevel.setTextColor(Color.BLUE);
            
            DMesgMsg.setText(Msg);

	     	if(position % 2 == 0)
	     		setBackgroundColor(0x80444444);
	     	else
	     		setBackgroundColor(0x80000000);
	     	
		}
		
		
    }
    
    private class LogcatListAdapter extends BaseAdapter {
        public LogcatListAdapter(Context context)
        {
            mContext = context;
        }

        public int getCount() {
           return JNILibrary.GetLogcatCounts();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
        	LogcatDetailView sv;
            
            if (convertView == null) {
                sv = new LogcatDetailView(mContext, 
                		JNILibrary.GetLogcatTime(position),
                		JNILibrary.GetLogcatLevel(position),
                		JNILibrary.GetLogcatPID(position),
                		JNILibrary.GetLogcatTag(position),
                		/*JNILibrary.GetLogcatMessage(position)*/ "",
                								   position);
            } else {
                sv = (LogcatDetailView)convertView;
                sv.setContext( JNILibrary.GetLogcatTime(position),
                			   JNILibrary.GetLogcatLevel(position),
               				   JNILibrary.GetLogcatPID(position),
               				   JNILibrary.GetLogcatTag(position),
               				/*JNILibrary.GetLogcatMessage(position)*/ "",
               				   position);
            }
            
            return sv;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        private Context mContext;
    }
    
    /**
     * We will use a SpeechView to display each speech. It's just a LinearLayout
     * with two text fields.
     *
     */
    private class LogcatDetailView extends TableLayout {

    	private TextView LogcatTitle;
    	private TextView LogcatMsg;
    	
        public LogcatDetailView(Context context,String Time, String Level, int PID,
        						String Tag, String Msg, int position) 
        {
            super(context);
            
            LogcatMsg = new TextView(context);
            LogcatTitle = new TextView(context);

            LogcatTitle.setGravity(Gravity.LEFT);

            LogcatMsg.setGravity(Gravity.LEFT);
            
            LogcatTitle.setText(Time+"  ["+Level+"]\n"+Tag+"("+PID+")");
            
            if(Level.endsWith("ERROR"))
                LogcatTitle.setTextColor(Color.RED);
            else if (Level.endsWith("DEBUG"))
            	LogcatTitle.setTextColor(Color.BLUE);
            else if (Level.endsWith("INFORMATION"))
            	LogcatTitle.setTextColor(Color.GREEN);
            else if (Level.endsWith("WARNING"))
            	LogcatTitle.setTextColor(Color.YELLOW);
            else if (Level.endsWith("VERBOSE"))
            	LogcatTitle.setTextColor(Color.WHITE);

            String temp = JNILibrary.GetLogcatMessage(position);
            if(temp.length() > 200)
            	LogcatMsg.setText("Message too long!! \nPlease click it to show..");
            else
            	LogcatMsg.setText(temp.trim());
            
            addView(LogcatTitle);
            addView(LogcatMsg);
            
	     	if(position % 2 == 0)
	     		setBackgroundColor(0x80444444);
	     	else
	     		setBackgroundColor(0x80000000);

        }
        
		public void setContext(String Time, String Level, int PID,
									String Tag, String Msg, int position) 
		{
            
            LogcatTitle.setText(Time+"  ["+Level+"]\n"+Tag+"("+PID+")");
            
            if(Level.endsWith("ERROR"))
                LogcatTitle.setTextColor(Color.RED);
            else if (Level.endsWith("DEBUG"))
            	LogcatTitle.setTextColor(Color.BLUE);
            else if (Level.endsWith("INFORMATION"))
            	LogcatTitle.setTextColor(Color.GREEN);
            else if (Level.endsWith("WARNING"))
            	LogcatTitle.setTextColor(Color.YELLOW);
            else if (Level.endsWith("VERBOSE"))
            	LogcatTitle.setTextColor(Color.WHITE);

            String temp = JNILibrary.GetLogcatMessage(position);
            if(temp.length() > 150)
            	LogcatMsg.setText("Message too long!! \nPlease click it to show..");
            else
            	LogcatMsg.setText(temp.trim());
            
            
	     	if(position % 2 == 0)
	     		setBackgroundColor(0x80444444);
	     	else
	     		setBackgroundColor(0x80000000);
	     	
		}
		
		
    }
}
