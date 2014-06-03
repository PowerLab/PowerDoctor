package com.eolwral.osmonitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;

/*
 * CPU Usage을 조회하는 class이다. 
 */
public class CpuUsage {
	
	final private String STAT_FILE = "/proc/stat";
	private long mUser;
	private long mSystem;
	private long mTotal;
	private long mIdle;

	private	int mUserCpuUsage;
	private int mOtherCpuUsage;
	private int mTotalCPUInt;
	
	public CpuUsage(){
		mTotalCPUInt = 0;
		mUserCpuUsage = 0;
		mOtherCpuUsage = 0;
		
		mUser = 0;
		mSystem = 0;
		mIdle = 0;
		mTotal = 0 ;
	}
	//user mode 이용률만 계산한다.
	public int getUserCpuUsage(){
		return mUserCpuUsage;
	}
	//user idle 2개의 mode를 제외한 이용률이다. 
	public int getOtherCpuUsage(){
		return mOtherCpuUsage;
	}
	public int getTotalCPUInt(){
		return mTotalCPUInt;
	}

	public boolean readStats()
	{
		FileReader fstream;
		try
		{
			fstream = new FileReader(STAT_FILE);
		}
		catch (FileNotFoundException e)
		{
			Log.e("CPUStatusLED", "Could not read " + STAT_FILE);
			return false;
		}
		BufferedReader in = new BufferedReader(fstream, 500);
		String line;
		try
		{
			while ((line = in.readLine()) != null)
			{
				if (line.startsWith("cpu"))
				{
					updateStats(line.trim().split("[ ]+"));//or expr "[ ]+"
					return true;//one line only
				}
			}
		}
		catch (IOException e)
		{
			Log.e("CPUStatusLED", e.toString());
		}
		return false;
	}
	
	private void updateStats(String [] segs)
	{
		/*
		 * 2.6 커널에서는 /proc/stat 파일에 총 7개의 정보가 존재한다.
		 * cpu user nice system idle iowait irq soft irq zero1 zero2
		 */
		// user = user(user mode) + nice(user mode)
		long user = Long.parseLong(segs[1]) + Long.parseLong(segs[2]);
		// system = system(kernel mode) + irq + soft_irq
		long system = Long.parseLong(segs[3]) + Long.parseLong(segs[6]) + Long.parseLong(segs[7]);
		// total = user + system + idle + io_wait
		long total = user + system + Long.parseLong(segs[4]) + Long.parseLong(segs[5]);
		// idle만 계산한다.
		long idle =  Long.parseLong(segs[4]);
		if (mTotal != 0 || total >= mTotal)
		{
			long duser = user - mUser;
			long dsystem = system - mSystem;
			long dtotal = total - mTotal;
			long didle = idle - mIdle;
			
			/*
			 *  ioWait가 빠진 usage이다.
			 */
			//mTotalCPUInt = new Double((duser + dsystem) * 100.0 / dtotal).intValue();	
			mUserCpuUsage = new Double(duser * 100.0 / dtotal).intValue();		//user mode 이용률만 계산한다.
			mOtherCpuUsage = (int)(100 - ( (didle+duser) * 100.0 / dtotal));	//user idle 2개의 mode를 제외한 이용률이다. 															//user mode를 제외한 이용률
			mTotalCPUInt = (int)(100 - ((didle * 100.0 / dtotal)));	// 전체 CPU Usage를 계산한다.(IoWait가 포함된)
			//use totalCPUInt  to set LED color.
		}
		mUser = user;
		mSystem = system;
		mIdle = idle;
		mTotal = total;
		}
}
