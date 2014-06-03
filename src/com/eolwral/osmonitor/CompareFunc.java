package com.eolwral.osmonitor;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;

public class CompareFunc
{
	public static boolean checkExtraStore(PreferenceActivity activity)
	{
		boolean flag = false;
    	if(Integer.parseInt(Build.VERSION.SDK) >= 8)
    	{
    		// use Reflection to avoid errors (for cupcake 1.5)
    		Method MethodList[] = activity.getClass().getMethods();
    		for(int checkMethod = 0; checkMethod < MethodList.length; checkMethod++)
    		{
    			if(MethodList[checkMethod].getName().indexOf("ApplicationInfo") != -1)
    			{
    				try{
    					if((((ApplicationInfo) MethodList[checkMethod].invoke(activity , new Object[]{})).flags & 0x40000 /* ApplicationInfo.FLAG_EXTERNAL_STORAGE*/ ) != 0 )
    						flag = true;
    				}
    				catch(Exception e) {}
    			}
    		}
    	}
    	return flag;
	}
	
	public static int getSDKVersion()
	{
		return Integer.parseInt(Build.VERSION.SDK);
	}
	
	
	// Screen Size
	private static int ScreenSize = 1; /* 0 == Small, 1 == Normal, 2 == Large */

	public static void detectScreen(Activity activity)
	{
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        int lanscapeHight = 0 ; 
        if( activity.getResources().getConfiguration().orientation ==
        								Configuration.ORIENTATION_PORTRAIT)
        	lanscapeHight = metrics.heightPixels;
        else
        	lanscapeHight = metrics.widthPixels;
        	        
        if(lanscapeHight >= 800)
        	ScreenSize = 2;
        else if(lanscapeHight <= 320)
        	ScreenSize = 0;
        else 
        	ScreenSize = 1;
	}
	
	public static int getScreenSize()
	{
		return ScreenSize;
	}
	
	// Gesture Threshold
	public static final int SWIPE_MIN_DISTANCE = 120;
	public static final int SWIPE_MAX_OFF_PATH = 250;
	public static final int SWIPE_THRESHOLD_VELOCITY = 200;
}