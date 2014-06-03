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

package com.eolwral.osmonitor.misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.eolwral.osmonitor.*;
import com.eolwral.osmonitor.preferences.Preferences;
import com.eslab.osmonitor.providerDB.SaveInformationActivity;
import com.eslab.osmonitor.utility.SDcardWrite;

public class MiscBox extends Activity implements OnGestureListener, OnTouchListener
{
	private static JNIInterface JNILibrary = JNIInterface.getInstance();;
	
	private static long PreCPUFreq = 0;
	private static String SensorName = "";
	private static float SensorTemp = 0;
	
	private static TextView ProcessorBox = null;
	private static TextView ProcessorFreqBox = null;
	private static TextView ProcessorTempBox = null;
	private static TextView MinCPUBox = null;
	private static TextView MaxCPUBox = null;
	private static TextView GovCPUBox = null;
	private static TextView PowerBox = null;
	private static TextView DiskBox = null;
	private static TextView SettingBox = null;
	private static Resources ResourceManager = null;
	private static Context MiscContext = null;
	private static boolean Rooted = false;
	
	private boolean useJNI = false;
	
	//실시간으로 Power Model대로 반영하여 전력값을 계산해서 파일에 기록해주는 class
	private SDcardWrite mOSdDcardWrite;
	// Gesture
	private GestureDetector gestureScanner = new GestureDetector(this);;
	
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
		try {
			if (Math.abs(e1.getY() - e2.getY()) > CompareFunc.SWIPE_MAX_OFF_PATH)
				return false;
			else if (e1.getX() - e2.getX() > CompareFunc.SWIPE_MIN_DISTANCE &&
							Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) 
				((TabActivity) this.getParent()).getTabHost().setCurrentTab(4);
			else if (e2.getX() - e1.getX() > CompareFunc.SWIPE_MIN_DISTANCE &&
							Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) 
				((TabActivity) this.getParent()).getTabHost().setCurrentTab(2);
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
	
	public boolean onTrackballMotion(MotionEvent e)
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
		{
			v.onTouchEvent(event);
			return true;
		}
		else
		{
			if(v.onTouchEvent(event))
				return true;
			return false;
		}
	}
	
	private SensorEventListener SensorListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			SensorName = event.sensor.getName().replace("sensor", "");
			SensorTemp = event.values[0];
		}
	};

	
	private Runnable MiscRunnable = new Runnable() {
		public void run() 
		{
		
			if(JNILibrary.doDataLoad() == 1)
			{ 
	        
		        if(useJNI == true)
		        {

					java.text.DecimalFormat TempFormat = new java.text.DecimalFormat("#.##");
					StringBuilder m_PowerStr = new StringBuilder();
	    		    m_PowerStr.append(ResourceManager.getText(R.string.status_text))
	    		    		  .append(": <b>"+JNILibrary.GetPowerStatus()+"</b>")
	        				  .append("<br />"+ResourceManager.getText(R.string.health_text))
	        				  .append(": "+JNILibrary.GetPowerHealth())
	        				  .append("<br />"+ResourceManager.getText(R.string.technology_text))
	        				  .append(": <i>"+JNILibrary.GetPowerTechnology()+"</i>")
	        				  .append("<br />"+ResourceManager.getText(R.string.capacity_text))
	        				  .append(": "+JNILibrary.GetPowerCapacity()+"%")
		        			  .append("<br />"+ResourceManager.getText(R.string.voltage_text))
		        			  .append(": <b>"+JNILibrary.GetPowerVoltage()+"mV</b>")        		  
		    	    		  .append("<br />"+ResourceManager.getText(R.string.temperature_text))
		    	    		  .append(": "+((double)(JNILibrary.GetPowerTemperature()/10))+"°C")
		    	    		  .append(" ("+TempFormat.format(((double)JNILibrary.GetPowerTemperature()/10*9/5+32))+"°F)");

					if(JNILibrary.GetACOnline() == 1)
						m_PowerStr.append("<br />"+ResourceManager.getText(R.string.acpower_text))
								  .append(": <font color=\"green\">")
								  .append(ResourceManager.getText(R.string.online_text)+"</font>");
		       		else
						m_PowerStr.append("<br />"+ResourceManager.getText(R.string.acpower_text))
						  		  .append(": <font color=\"red\">")
						  		  .append(ResourceManager.getText(R.string.offline_text)+"</font>");
	        
					if(JNILibrary.GetUSBOnline() == 1)
						m_PowerStr.append("<br />"+ResourceManager.getText(R.string.usbpower_text))
								  .append(": <font color=\"green\">")
								  .append(ResourceManager.getText(R.string.online_text)+"</font>");
		    	   	else
						m_PowerStr.append("<br />"+ResourceManager.getText(R.string.usbpower_text))
								  .append(": <font color=\"red\">")
								  .append(ResourceManager.getText(R.string.offline_text)+"</font>");
	        
			        PowerBox.setText(Html.fromHtml(m_PowerStr.toString()));
		        }

		        StringBuilder m_ProcessorStr = new StringBuilder();

       		   	m_ProcessorStr.append(ResourceManager.getText(R.string.processorscal_text));
    			
    			ProcessorFreqBox.setText(Html.fromHtml(m_ProcessorStr.toString()));
    			
    			m_ProcessorStr = new StringBuilder();
    			m_ProcessorStr.append("&nbsp;&nbsp;&nbsp;&nbsp;<b>"+JNILibrary.GetProcessorScalMin()+"</b> ");
    			MinCPUBox.setText(Html.fromHtml(m_ProcessorStr.toString()));
    			
    			m_ProcessorStr = new StringBuilder();
    			m_ProcessorStr.append(" <b>"+JNILibrary.GetProcessorScalMax()+"</b> ");
    			MaxCPUBox.setText(Html.fromHtml(m_ProcessorStr.toString()));
    
    			GovCPUBox.setText(Html.fromHtml(ResourceManager.getText(R.string.processorgov_text)
    		   				  	  +": <i>"+JNILibrary.GetProcessorScalGov()+"</i>"));
    			
		        m_ProcessorStr = new StringBuilder();
    		   	
    		   	m_ProcessorStr.append(ResourceManager.getText(R.string.processorfreq_text)+"<br />")
    		   				  .append("&nbsp;&nbsp;&nbsp;&nbsp;<b>"+JNILibrary.GetProcessorMin()+"</b> ~ ")
    		   				  .append("<b>"+JNILibrary.GetProcessorMax()+"</b><br />");

//    		   	m_ProcessorStr.append(ResourceManager.getText(R.string.processorgov_text))
  //  		   				  .append(": <i>"+JNILibrary.GetProcessorScalGov()+"</i><br />");
    		   	
    		   	if(JNILibrary.GetProcessorScalCur() > PreCPUFreq)
    		   	{
        		   	m_ProcessorStr.append(ResourceManager.getText(R.string.processorcur_text))
	   				  			  .append(": <font color=red>"+JNILibrary.GetProcessorScalCur()+"</font>");
    		   	}
    		   	else if (JNILibrary.GetProcessorScalCur() < PreCPUFreq)
    		   	{
    		   		m_ProcessorStr.append(ResourceManager.getText(R.string.processorcur_text))
    		   					  .append(": <font color=green>"+JNILibrary.GetProcessorScalCur()+"</font>");
    		   	}
    		   	else
    		   	{
        		   	m_ProcessorStr.append(ResourceManager.getText(R.string.processorcur_text))
			  			  .append(": "+JNILibrary.GetProcessorScalCur());
    		   	}
    		   	PreCPUFreq = JNILibrary.GetProcessorScalCur();
    		   	
    		   	//Current 숫자를 변경하면서 출력하는 코드 이다.
    		   	ProcessorBox.setText(Html.fromHtml(m_ProcessorStr.toString()));
    		   	
    		   	StringBuilder m_ProcessorTempStr = new StringBuilder();
    		   	java.text.DecimalFormat TempFormat = new java.text.DecimalFormat("#.##");

    			if(JNILibrary.GetProcessorOMAPTemp() != 0)
    				m_ProcessorTempStr.append("OMAP3403 "+ResourceManager.getText(R.string.processortmp_text)+"<br />")
    		   					      .append("&nbsp;&nbsp;&nbsp;&nbsp<i>"+JNILibrary.GetProcessorOMAPTemp()+"°C")
		    		  		   	      .append(" ("+TempFormat.format(((double)JNILibrary.GetProcessorOMAPTemp()*9/5+32))+"°F)</i>");

    		    if(SensorTemp != 0)
    		    	m_ProcessorTempStr.append("<br />"+SensorName+"<br />")
    		    	 			      .append("&nbsp;&nbsp;&nbsp;&nbsp<b>"+((double)SensorTemp)+"°C")
		    		  			      .append(" ("+TempFormat.format(((double)SensorTemp*9/5+32))+"°F)</b>");
    		   		
    		    ProcessorTempBox.setText(Html.fromHtml(m_ProcessorTempStr.toString()));

    		   	StringBuilder m_DiskStr = new StringBuilder();
	
				java.text.DecimalFormat DiskFormat = new java.text.DecimalFormat(",###");
				java.text.DecimalFormat UsageFormat = new java.text.DecimalFormat("#.#");
				
				String DiskTotal = ResourceManager.getText(R.string.disktotal_text).toString();
				String DiskUsed = ResourceManager.getText(R.string.diskused_text).toString();
				String DiskAvail = ResourceManager.getText(R.string.diskavailable_text).toString();
				
				if(JNILibrary.GetSystemMemAvail() == 0)
					m_DiskStr.append("<b>/system</b>&nbsp;&nbsp;&nbsp;0% Used");
				else
					m_DiskStr.append("<b>/system</b>&nbsp;&nbsp;&nbsp;"+UsageFormat.format(JNILibrary.GetSystemMemUsed()/JNILibrary.GetSystemMemTotal()*100)+"% Used");
					
				m_DiskStr.append("<br /> "+DiskTotal+": "+DiskFormat.format(JNILibrary.GetSystemMemTotal())+"K ")
        				 .append("<br /> "+DiskUsed+": "+DiskFormat.format(JNILibrary.GetSystemMemUsed())+"K ")
        				 .append("<br /> "+DiskAvail+": "+DiskFormat.format(JNILibrary.GetSystemMemAvail())+"K ");

				if(JNILibrary.GetDataMemAvail() == 0)
					m_DiskStr.append("<br /><br /><b>/data</b>&nbsp;&nbsp;&nbsp;0% Used");
				else
					m_DiskStr.append("<br /><br /><b>/data</b>&nbsp;&nbsp;&nbsp;"+UsageFormat.format(JNILibrary.GetDataMemUsed()/JNILibrary.GetDataMemTotal()*100)+"% Used");
        		
				m_DiskStr.append("<br /> "+DiskTotal+": "+DiskFormat.format(JNILibrary.GetDataMemTotal())+"K ")
        				 .append("<br /> "+DiskUsed+": "+DiskFormat.format(JNILibrary.GetDataMemUsed())+"K ")
        				 .append("<br /> "+DiskAvail+": "+DiskFormat.format(JNILibrary.GetDataMemAvail())+"K ");
				
				if(JNILibrary.GetSDCardMemAvail() == 0)
					m_DiskStr.append("<br /><br /><b>/sdcard</b>&nbsp;&nbsp;&nbsp;0% Used");
				else
					m_DiskStr.append("<br /><br /><b>/sdcard</b>&nbsp;&nbsp;&nbsp;"+UsageFormat.format(JNILibrary.GetSDCardMemUsed()/JNILibrary.GetSDCardMemTotal()*100)+ "% Used");
        		
				m_DiskStr.append("<br /> "+DiskTotal+": "+DiskFormat.format(JNILibrary.GetSDCardMemTotal())+"K ")
	        			 .append("<br /> "+DiskUsed+": "+DiskFormat.format(JNILibrary.GetSDCardMemUsed())+"K ")
        				 .append("<br /> "+DiskAvail+": "+DiskFormat.format(JNILibrary.GetSDCardMemAvail())+"K ");        		         		 

				if(JNILibrary.GetCacheMemAvail() == 0)
					m_DiskStr.append("<br /><br /><b>/cache</b>&nbsp;&nbsp;&nbsp;0% Used");
				else
					m_DiskStr.append("<br /><br /><b>/cache</b>&nbsp;&nbsp;&nbsp;"+UsageFormat.format(JNILibrary.GetCacheMemUsed()/JNILibrary.GetCacheMemTotal()*100)+"% Used");
        		
				m_DiskStr.append("<br /> "+DiskTotal+": "+DiskFormat.format(JNILibrary.GetCacheMemTotal())+"K ")
						 .append("<br /> "+DiskUsed+": "+DiskFormat.format(JNILibrary.GetCacheMemUsed())+"K ")
						 .append("<br /> "+DiskAvail+": "+DiskFormat.format(JNILibrary.GetCacheMemAvail())+"K ");
        		
    	    	DiskBox.setText(Html.fromHtml(m_DiskStr.toString()));		

        	}
           	MiscHandler.postDelayed(this, 1000);
        }
	};   
	
	Handler MiscHandler = new Handler();


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mOSdDcardWrite = new SDcardWrite();	//파일에 기록하는 class이다. 
        setContentView(R.layout.misclayout);
        
        ResourceManager = getResources();
        
        ((ScrollView) findViewById(R.id.miscview)).setOnTouchListener(this);
        MiscContext = this;
        ProcessorFreqBox = (TextView) findViewById(R.id.processorFreqText);
        ProcessorTempBox = (TextView) findViewById(R.id.processorTempText);
        ProcessorBox = (TextView) findViewById(R.id.processorText);
        MinCPUBox = (TextView) findViewById(R.id.setCpuMin);
        MaxCPUBox = (TextView) findViewById(R.id.setCpuMax);
        GovCPUBox = (TextView) findViewById(R.id.setCpuGov);
        PowerBox = (TextView) findViewById(R.id.powerText);
        DiskBox = (TextView) findViewById(R.id.diskText);
        SettingBox = (TextView) findViewById(R.id.settingText);
        

        ImageButton MinCpu = (ImageButton) findViewById(R.id.btnCpuMin);

        MinCpu.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	AlertDialog.Builder SetCPUMinBox = new AlertDialog.Builder(MiscContext);
            	
            	String [] CPUFreqList = GetCPUFreqList();
            	
            	if(CPUFreqList == null)
            		return;
            	
            	int CurFreq = 0;
            	for(CurFreq = 0; CurFreq < CPUFreqList.length; CurFreq++)
            	{
            		if(Integer.parseInt(CPUFreqList[CurFreq]) == JNILibrary.GetProcessorScalMin())
            			break;
            	}
            	
                SetCPUMinBox.setSingleChoiceItems(GetCPUFreqList(), CurFreq, new DialogInterface.OnClickListener() 
                	{
                    	public void onClick(DialogInterface dialog, int item) 
                    	{
                        	if(!Rooted)
                        	{
                				dialog.dismiss();
                        		return;
                        	}

                    		String SetCPUCmd = "";
                    		String [] CPUFreqList = GetCPUFreqList();

                    		
                    		if(CPUFreqList == null)
                    		{
                    			dialog.dismiss();
                        		return;
                    		}
                			
                    		if(Integer.parseInt(CPUFreqList[item]) <= JNILibrary.GetProcessorScalMax() 
                    				&& Rooted)
                			{
                				SetCPUCmd = "echo "+CPUFreqList[item]+
                		        			" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"+"\n";
                				JNILibrary.execCommand(SetCPUCmd);
                			}
            				dialog.dismiss();
                    	}
                	}
                );
                
                AlertDialog SetCPUMin = SetCPUMinBox.create();
                SetCPUMin.show();
            }
        });

        ImageButton MaxCpu = (ImageButton) findViewById(R.id.btnCpuMax);

        MaxCpu.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	AlertDialog.Builder SetCPUMaxBox = new AlertDialog.Builder(MiscContext);
            	
            	String [] CPUFreqList = GetCPUFreqList();
            	
            	if(CPUFreqList == null)
            		return;
            	
            	int CurFreq = 0;
            	for(CurFreq = 0; CurFreq < CPUFreqList.length; CurFreq++)
            	{
            		if(Integer.parseInt(CPUFreqList[CurFreq]) == JNILibrary.GetProcessorScalMax())
            			break;
            	}
            	
                SetCPUMaxBox.setSingleChoiceItems(GetCPUFreqList(), CurFreq, new DialogInterface.OnClickListener() 
                	{
                    	public void onClick(DialogInterface dialog, int item) 
                    	{
                        	if(!Rooted)
                        	{
                				dialog.dismiss();
                        		return;
                        	}

                    		String SetCPUCmd = "";
                    		String [] CPUFreqList = GetCPUFreqList();

                    		if(CPUFreqList == null)
                    		{
                    			dialog.dismiss();
                        		return;
                    		}
                			

                    		if(Integer.parseInt(CPUFreqList[item]) >= JNILibrary.GetProcessorScalMin()
                				&& Rooted)
                			{
                				SetCPUCmd = "echo "+CPUFreqList[item]+
                		        			" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"+"\n";
                				JNILibrary.execCommand(SetCPUCmd);
                			}
            				dialog.dismiss();
                    	}
                	}
                );
                
                AlertDialog SetCPUMax = SetCPUMaxBox.create();
                SetCPUMax.show();
            }
        });

        ImageButton GovCpu = (ImageButton) findViewById(R.id.btnCpuGov);

        GovCpu.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	AlertDialog.Builder SetCPUMaxBox = new AlertDialog.Builder(MiscContext);
            	
            	String [] CPUGovList = GetCPUGovList();
            	
            	if(CPUGovList == null)
            		return;
            	
            	int CurGov = 0;
            	for(CurGov = 0; CurGov < CPUGovList.length; CurGov++)
            	{
            		if(CPUGovList[CurGov].equals(JNILibrary.GetProcessorScalGov()))
            			break;
            	}
            	
                SetCPUMaxBox.setSingleChoiceItems(GetCPUGovList(), CurGov, new DialogInterface.OnClickListener() 
                	{
                    	public void onClick(DialogInterface dialog, int item) 
                    	{
                        	if(!Rooted)
                        	{
                				dialog.dismiss();
                        		return;
                        	}

                    		String SetCPUCmd = "";
                    		String [] CPUGovList = GetCPUGovList();

                    		if(CPUGovList == null)
                    		{
                    			dialog.dismiss();
                        		return;
                    		}
                			

                    		if(Rooted)
                			{
                				SetCPUCmd = "echo "+CPUGovList[item]+
                		        			" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"+"\n";
                				JNILibrary.execCommand(SetCPUCmd);
                			}
            				dialog.dismiss();
                    	}
                	}
                );
                
                AlertDialog SetCPUMax = SetCPUMaxBox.create();
                SetCPUMax.show();
            }
        });
    }

	private void restorePrefs()
    {
		// load settings
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
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

        useJNI = settings.getBoolean(Preferences.PREF_BATTERYJNI, false);

        // Root
		Rooted = settings.getBoolean(Preferences.PREF_ROOTED, false);
    }
	
    public boolean onCreateOptionsMenu(Menu optionMenu) 
    {
     	super.onCreateOptionsMenu(optionMenu);
     	optionMenu.add(0, 1, 0, getResources().getString(R.string.options_text));
       	optionMenu.add(0, 4, 0, getResources().getString(R.string.aboutoption_text));
       	optionMenu.add(0, 5, 0, getResources().getString(R.string.forceexit_text));
       	optionMenu.add(0, 6, 0, getResources().getString(R.string.open_db_information));
       	optionMenu.add(0, 7, 0, getResources().getString(R.string.open_power_value));
        
    	return true;
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
    	}
    	
    	return null;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	restorePrefs();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	
        super.onOptionsItemSelected(item);
        switch(item.getItemId())
        {
        case 1:
       		Intent launchPreferencesIntent = new Intent().setClass( this, Preferences.class);
       		startActivityForResult(launchPreferencesIntent, 0);
        	break;
        case 4:
        	this.showDialog(0);
        	break;

        case 5:
        	if(OSMonitorService.getInstance() != null)
        		OSMonitorService.getInstance().stopSelf();

        	JNILibrary.killSelf(this);

        	break;
        	//청크 DB를 open하는 코드이다.
        case 6:
            Intent intentDB = new Intent().setClass( this, SaveInformationActivity.class);
            startActivityForResult(intentDB, 0);
        	break;
        case 7:
        	mOSdDcardWrite.writePoweResult(OSMonitorService.getInstance().mPowerValue);
        	break;
        	
        }
        
        return true;
    }
    
    @Override
    public void onPause() 
    {
    	if(!useJNI)
    		stopBatteryMonitor();

    	SensorManager SMer = (SensorManager) getSystemService(SENSOR_SERVICE);

    	SMer.unregisterListener(SensorListener, SMer
				.getDefaultSensor(Sensor.TYPE_TEMPERATURE));

    	MiscHandler.removeCallbacks(MiscRunnable);
    	//debug
    	//JNILibrary.doTaskStop();
    	super.onPause();
    }

    @Override
    protected void onResume() 
    {    
    	restorePrefs();

    	SensorManager SMer = (SensorManager) getSystemService(SENSOR_SERVICE);

    	SMer.registerListener(SensorListener, SMer
				.getDefaultSensor(Sensor.TYPE_TEMPERATURE),
				SensorManager.SENSOR_DELAY_UI);
    	
    	if(!useJNI)
    		startBatteryMonitor();
/*    	else
    	{
    		try {
    			stopBatteryMonitor();
    		}
    		catch(IllegalArgumentException e) {}
    	}*/
    	
    	JNILibrary.doTaskStart(JNILibrary.doTaskMisc);
    	MiscHandler.post(MiscRunnable);
    	super.onResume();
    }
    
    private String [] GetCPUFreqList() 
    {
    	try {
    		byte[] RawData = new byte[256];

    		File CPUFreq = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
    		BufferedInputStream bInputStream = 
    							new BufferedInputStream(new FileInputStream(CPUFreq));

    		bInputStream.read(RawData);
    		String CPUFreqList = (new String(RawData)).trim();
    		bInputStream.close();
    		
    		String [] FreqList = CPUFreqList.split(" ");
    		
    		return FreqList;
    		
    	} catch (Exception e) {}
    	
    	return null;
    }
    
    private String [] GetCPUGovList() 
    {
    	try {
    		byte[] RawData = new byte[256];

    		File CPUGov = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors");
    		BufferedInputStream bInputStream = 
    							new BufferedInputStream(new FileInputStream(CPUGov));

    		bInputStream.read(RawData);
    		String CPUGovList = (new String(RawData)).trim();
    		bInputStream.close();
    		
    		String [] GovList = CPUGovList.split(" ");
    		
    		return GovList;
    		
    	} catch (Exception e) {}
    	
    	return null;
    }
    
    private void startBatteryMonitor()
    {
    	IntentFilter battFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    	registerReceiver(battReceiver, battFilter);		        		
    }
    
    private void stopBatteryMonitor()
    {
    	unregisterReceiver(battReceiver);
    }

	private static BroadcastReceiver battReceiver = new BroadcastReceiver() 
	{
		public void onReceive(Context context, Intent intent) {
			
			if(ResourceManager == null || PowerBox == null)
				return;
			
			int rawlevel = intent.getIntExtra("level", -1);
			int scale = intent.getIntExtra("scale", -1);
			int status = intent.getIntExtra("status", -1);
			int health = intent.getIntExtra("health", -1);
			int plugged = intent.getIntExtra("plugged", -1);
			int temperature = intent.getIntExtra("temperature", -1);
			int voltage = intent.getIntExtra("voltage", -1);
			String technology = intent.getStringExtra("technology");
				
			int level = -1;  // percentage, or -1 for unknown
			if (rawlevel >= 0 && scale > 0) {
				level = (rawlevel * 100) / scale;
			}

	        StringBuilder m_PowerStr = new StringBuilder();
			m_PowerStr.append(ResourceManager.getText(R.string.status_text));
			switch(status) 
			{
			case BatteryManager.BATTERY_STATUS_UNKNOWN:
				m_PowerStr.append(": <b>Unknown</b>");
				break;
			case BatteryManager.BATTERY_STATUS_CHARGING:
				m_PowerStr.append(": <b>Charging</b>");
				break;
			case BatteryManager.BATTERY_STATUS_DISCHARGING:
				m_PowerStr.append(": <b>DisCharging</b>");
				break;
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
				m_PowerStr.append(": <b>Not Charging</b>");
				break;
			case BatteryManager.BATTERY_STATUS_FULL:
				m_PowerStr.append(": <b>Full</b>");
				break;
			}
				
			m_PowerStr.append("<br />"+ResourceManager.getText(R.string.health_text));
			switch(health)
			{
			case BatteryManager.BATTERY_HEALTH_DEAD:
				m_PowerStr.append(": Dead");
				break;
				case BatteryManager.BATTERY_HEALTH_GOOD:
				m_PowerStr.append(": Good");
				break;
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				m_PowerStr.append(": Over Heat");
				break;
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				m_PowerStr.append(": Over Voltage");
				break;
			case BatteryManager.BATTERY_HEALTH_UNKNOWN:
				m_PowerStr.append(": Unknown");
				break;
			case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
				m_PowerStr.append(": Unspecified Failure");
				break;
				
			}
			
			java.text.DecimalFormat TempFormat = new java.text.DecimalFormat("#.##");
			m_PowerStr.append("<br />"+ResourceManager.getText(R.string.technology_text))
					  .append(": <i>"+technology+"</i>")
					  .append("<br />"+ResourceManager.getText(R.string.capacity_text))
   		    		  .append(": "+level+"%")
   		    		  .append("<br />"+ResourceManager.getText(R.string.voltage_text))
   		    		  .append(": <b>"+voltage+"mV</b>")        		  
   		    		  .append("<br />"+ResourceManager.getText(R.string.temperature_text))
   		    		  .append(": "+((double)temperature/10)+"°C")
   		    		  .append(" ("+TempFormat.format(((double)temperature/10*9/5+32))+"°F)");
				
			if(plugged == BatteryManager.BATTERY_PLUGGED_AC)
				m_PowerStr.append("<br />"+ResourceManager.getText(R.string.acpower_text))
						  .append(": <font color=\"green\">")
						  .append(ResourceManager.getText(R.string.online_text)+"</font>");
       		else
				m_PowerStr.append("<br />"+ResourceManager.getText(R.string.acpower_text))
				  		  .append(": <font color=\"red\">")
				  		  .append(ResourceManager.getText(R.string.offline_text)+"</font>");
       
			if(plugged == BatteryManager.BATTERY_PLUGGED_USB)
				m_PowerStr.append("<br />"+ResourceManager.getText(R.string.usbpower_text))
						  .append(": <font color=\"green\">")
						  .append(ResourceManager.getText(R.string.online_text)+"</font>");
    	   	else
				m_PowerStr.append("<br />"+ResourceManager.getText(R.string.usbpower_text))
						  .append(": <font color=\"red\">")
						  .append(ResourceManager.getText(R.string.offline_text)+"</font>");
       
			PowerBox.setText(Html.fromHtml(m_PowerStr.toString()));
		}
	};
    
}
