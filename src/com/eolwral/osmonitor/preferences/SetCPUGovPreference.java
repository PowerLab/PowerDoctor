package com.eolwral.osmonitor.preferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import com.eolwral.osmonitor.JNIInterface;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SetCPUGovPreference extends DialogPreference {
	
	JNIInterface JNILibrary = JNIInterface.getInstance();
	Spinner GovSpinner = null;
	
	@Override
	protected View onCreateDialogView() {

		if(GetCPUGovList() == null)
			return new View(this.getContext());

		this.GovSpinner = new Spinner(this.getContext());
		ArrayAdapter<String> GovAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item,
													GetCPUGovList());
	    GovAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		GovSpinner.setAdapter(GovAdapter);
		
	    if(getPersistedString("None").equals("None"))
	    {
		    for (int CurGov = 0; CurGov < GovAdapter.getCount(); CurGov++)
		    {
		    	if(GovAdapter.getItem(CurGov).trim().equals(JNILibrary.GetProcessorScalGov().trim()))
		    			GovSpinner.setSelection(CurGov);		
		    }
	    }
	    else
	    {
		    for (int CurGov = 0; CurGov < GovAdapter.getCount(); CurGov++)
		    {
		    	if(GovAdapter.getItem(CurGov).trim().equals(getPersistedString("")))
		    			GovSpinner.setSelection(CurGov);		
		    }
	    }
		
		return this.GovSpinner;
	}
	
	private String [] GetCPUGovList() 
	{
		String [] GovList = null;
		
		try {
			byte[] RawData = new byte[256];
    		
			File CPUGov = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors");
    		BufferedInputStream bInputStream = 
	    						new BufferedInputStream(new FileInputStream(CPUGov));

    		bInputStream.read(RawData);
    		String CPUGovList = (new String(RawData)).trim();
    		bInputStream.close();
	    		
    		GovList = CPUGovList.split(" ");
    		
    		if(GovList.length == 0)
    			GovList = null;
    		
    	} catch (Exception e) {
    		GovList = null;
    	}
    	
		return GovList;
    }

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if(positiveResult){
			persistString(GovSpinner.getSelectedItem().toString());
		}
		super.onDialogClosed(positiveResult);
	}

	
	public SetCPUGovPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public SetCPUGovPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
	
}