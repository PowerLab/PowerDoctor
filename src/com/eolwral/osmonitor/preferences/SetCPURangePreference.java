package com.eolwral.osmonitor.preferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class SetCPURangePreference extends DialogPreference {
	
	Spinner FreqMaxSpinner = null;
	Spinner FreqMinSpinner = null;
	
	@Override
	protected View onCreateDialogView() {

		if(GetCPUFreqList() == null)
			return new View(this.getContext());

		this.FreqMaxSpinner = new Spinner(this.getContext());
		this.FreqMinSpinner = new Spinner(this.getContext());

		ArrayAdapter<String> FreqAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item,
																GetCPUFreqList());
	    FreqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		FreqMaxSpinner.setAdapter(FreqAdapter);
		FreqMinSpinner.setAdapter(FreqAdapter);
		
	    if(getPersistedString("None").equals("None"))
	    {
	    	FreqMaxSpinner.setSelection(FreqAdapter.getCount()-1);
	    	FreqMinSpinner.setSelection(0);
	    }
	    else
	    {
	    	String Freqs[] = getPersistedString("").split(";");
	    	
	    	if(Freqs.length > 0 )
	    	{
			    for (int CurFreq = 0; CurFreq < FreqAdapter.getCount(); CurFreq++)
			    {
			    	if(FreqAdapter.getItem(CurFreq).trim().equals(Freqs[0]))
		    			FreqMinSpinner.setSelection(CurFreq);	
			    	
			    	if(FreqAdapter.getItem(CurFreq).trim().equals(Freqs[1]))
		    			FreqMaxSpinner.setSelection(CurFreq);		
			    }
	    	}
	    }
	    
	    LinearLayout FreqLayout = new LinearLayout(this.getContext());
		TextView FreqArrow = new TextView(this.getContext());
	    FreqArrow.setText(" <--> ");
	    
	    FreqLayout.addView(FreqMinSpinner);
	    FreqLayout.addView(FreqArrow);
	    FreqLayout.addView(FreqMaxSpinner);
	    
		return FreqLayout;
	}
	
    private String [] GetCPUFreqList() 
    {
    	String [] FreqList = null;
    	try {
    		byte[] RawData = new byte[256];

    		File CPUFreq = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
    		BufferedInputStream bInputStream = 
    							new BufferedInputStream(new FileInputStream(CPUFreq));

    		bInputStream.read(RawData);
    		String CPUFreqList = (new String(RawData)).trim();
    		bInputStream.close();
    		
    		FreqList = CPUFreqList.split(" ");
    		
    		if(FreqList.length == 0)
    			FreqList = null;
    		
    	} catch (Exception e) {
    		FreqList = null;
    	}
    	
		return FreqList;
	}


	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if(positiveResult){
			persistString(FreqMinSpinner.getSelectedItem().toString()+ ";"+
						  FreqMaxSpinner.getSelectedItem().toString());
		}
		super.onDialogClosed(positiveResult);
	}

	
	public SetCPURangePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public SetCPURangePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
	
}