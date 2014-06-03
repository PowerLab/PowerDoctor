package com.eolwral.osmonitor;

import java.io.DataOutputStream;
import java.io.IOException;

import android.app.ActivityManager;
import android.os.Handler;
import android.os.Message;
import android.content.Context;

public class JNIInterface
{ 
	private static JNIInterface singletone = null;
	
	// Design Pattern "Singletone"
	public static JNIInterface getInstance()
	{
		if(singletone == null) 
		{
	        System.loadLibrary("osmonitor");
			singletone = new JNIInterface();
		}
		 
		return singletone;
	}

    /* Load Module */
	public final int doTaskProcess = 1; 
	public final int doTaskInterface = 2; 
	public final int doTaskNetwork = 3; 
	public final int doTaskMisc = 4;
	public final int doTaskDMesg = 5;
	public final int doTaskLogcat = 6;

    public native int doTaskStop();
    public native int doTaskStart(int TaskType);
    public native int doDataLoad();
    public native int doDataSwap();
    public native int doDataRefresh();
    public native int doDataTime(int Time);
    public native int doCPUUpdate(int Update);
 
    /* Root */
    public native int GetRooted();
    
    /* CPU */
    public native String GetCPUUsage();
    public native int GetCPUUsageValue();
    
    /* Processor */
    public native int GetProcessorMax();
    public native int GetProcessorMin();
    public native int GetProcessorScalMax();
    public native int GetProcessorScalMin();
    public native int GetProcessorScalCur();
    public native int GetProcessorOMAPTemp();
    public native String GetProcessorScalGov();
      
    /* Memory */
    public native long GetMemTotal();
    public native long GetMemFree();
    public native long GetMemCached();
    public native long GetMemBuffer();

    /* Power */
    public native int GetPowerCapacity();
    public native int GetPowerVoltage();
    public native int GetPowerTemperature();
    public native int GetUSBOnline();
    public native int GetACOnline();
    public native String GetPowerHealth();
    public native String GetPowerStatus();
    public native String GetPowerTechnology();

	/* Disk */
    public native double GetSystemMemTotal();
    public native double GetDataMemTotal();
    public native double GetSDCardMemTotal();
    public native double GetCacheMemTotal();
    public native double GetSystemMemUsed();
    public native double GetDataMemUsed();
    public native double GetSDCardMemUsed();
    public native double GetCacheMemUsed();
    public native double GetSystemMemAvail();
    public native double GetDataMemAvail();
    public native double GetSDCardMemAvail();
    public native double GetCacheMemAvail();


	/* Process */
	public final int doSortPID = 1; 
	public final int doSortLoad = 2; 
	public final int doSortMem = 3; 
	public final int doSortThreads = 4; 
	public final int doSortName = 5;
	 
	public final int doOrderASC = 0; 
	public final int doOrderDESC = 1; 

    public native int SetProcessFilter(int Filter);
    public native int SetProcessAlgorithm(int Algorithm);
    public native int SetProcessSort(int Sort);
    public native int SetProcessOrder(int Order);
    public native int GetProcessCounts();
    public native int GetProcessPID(int position);
    public native int GetProcessUID(int position);
    public native int GetProcessLoad(int position);
    public native long GetProcessUTime(int position);
    public native long GetProcessSTime(int position);
    public native int GetProcessThreads(int position);
    public native long GetProcessRSS(int position);
    public native String GetProcessName(int position);
    public native String GetProcessOwner(int position);
    public native String GetProcessStatus(int position);
    public native String GetProcessNamebyUID(int uid);

    public native int doInterfaceNext();
    public native int doInterfaceReset();
    public native int GetInterfaceCounts();
    public native int GetInterfaceOutSize(int position);
    public native int GetInterfaceInSize(int position);
    public native String GetInterfaceName(int position);
    public native String GetInterfaceAddr(int position);
    public native String GetInterfaceAddr6(int position);
    public native String GetInterfaceNetMask(int position);
    public native String GetInterfaceNetMask6(int position);
    public native String GetInterfaceMac(int position);
    public native String GetInterfaceScope(int position);
    public native String GetInterfaceFlags(int position);

    public native int SetNetworkIP6To4(int value);
    public native int GetNetworkCounts();
    public native String GetNetworkProtocol(int position);
    public native String GetNetworkLocalIP(int position);
    public native int GetNetworkLocalPort(int position);
    public native String GetNetworkRemoteIP(int position);
    public native int GetNetworkRemotePort(int position);
    public native String GetNetworkStatus(int position);
    public native int GetNetworkUID(int position);

    
	/*
	0: KERN_EMERG
	1: KERN_ALERT
	2: KERN_CRIT
	3: KERN_ERR
	4: KERN_WARNING
	5: KERN_NOTICE
	6: KERN_INFO
	7: KERN_DEBUG
	*/
	public final int doDMesgEMERG = 1; 
	public final int doDMesgALERT = 2; 
	public final int doDMesgERR = 3; 
	public final int doDMesgWARNING = 4; 
	public final int doDMesgNOTICE = 5; 
	public final int doDMesgINFO = 6; 
	public final int doDMesgDEBUG = 7; 
	public final int doDMesgNONE = 8; 

	public native int GetDebugMessageCounts();
	public native String GetDebugMessageTime(int position);
	public native String GetDebugMessageLevel(int position);
	public native String GetDebugMessage(int position);
	public native int SetDebugMessageLevel(int value);
	public native int SetDebugMessage(String filter);
	public native int SetDebugMessageFilter(int value);
    public native String GetDebugMessage();

    public final int doLogcatNONE = 0;
    public final int doLogcatVERBOSE = 2;
    public final int doLogcatDEBUG = 3;
    public final int doLogcatINFO = 4;
    public final int doLogcatWARN = 5;
    public final int doLogcatERROR = 6;
    public final int doLogcatFATAL = 7;
    
	public native int GetLogcatCounts();
	public native int GetLogcatSize();
	public native int GetLogcatCurrentSize();
	public native String GetLogcatLevel(int position);
	public native int GetLogcatPID(int position);
	public native String GetLogcatTime(int position);
	public native String GetLogcatTag(int position);
	public native String GetLogcatMessage(int position);
	public native int SetLogcatSource(int value);
	public native int SetLogcatFilter(int value);
	public native int SetLogcatPID(int value);
	public native int SetLogcatLevel(int value);
	public native int SetLogcatMessage(String filter);
	
    public void execCommand(String command) {
    	try {
    		Process shProc = Runtime.getRuntime().exec("su");
    		DataOutputStream InputCmd = new DataOutputStream(shProc.getOutputStream());
    	
    		InputCmd.writeBytes(command);

    		// Close the terminal
    		InputCmd.writeBytes("exit\n");
    		InputCmd.flush();
    		InputCmd.close();
    	
    		try {
    			shProc.waitFor();
    		} catch (InterruptedException e) { };
    	} catch (IOException e) {}
    }    

    private Handler EndHelper = new Handler() 
    {
    	public void handleMessage(Message msg)
    	{
    		android.os.Process.killProcess(android.os.Process.myPid());
    	}
    	
    }; 
    
    public void killSelf(Context target)
    {
    	if(CompareFunc.getSDKVersion() <= 7)
    	{
           	((ActivityManager) target.getSystemService(Context.ACTIVITY_SERVICE))
           									.restartPackage("com.eolwral.osmonitor");
    	}
    	else
    	{
    		EndHelper.sendEmptyMessageDelayed(0, 500);

    	}
    }
}

