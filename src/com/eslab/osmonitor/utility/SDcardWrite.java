package com.eslab.osmonitor.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.os.Environment;

/*
 * ArrayList의 내용을 SD card에 기록하는 class이다.
 */
public class SDcardWrite {

	String mSdpath; //sdcard의 경로를 저장한다.
	File mDir;
	File mFile;
	PrintWriter out = null;
	static int FILEVERSIOn = 1;
	public SDcardWrite() {
		//먼저 해당 phone에 SD card가 있는지 유무를 판별 한다.
		String ext = Environment.getExternalStorageState();
		if(ext.equals(Environment.MEDIA_MOUNTED)){
			mSdpath = Environment.getExternalStorageDirectory().getAbsolutePath();//절대 결로를 얻어온다.
		}
		else{
			mSdpath = Environment.MEDIA_UNMOUNTED; // SD카드가 없음을 저장한다.
		}
		
		//경로를 가지고 디렉토리를 생성한다.
		mDir = new File(mSdpath + "/PowerDoctor");
		mDir.mkdir(); //폴더를 생성한다.
		
		mFile = new File(mSdpath + "/PowerDoctor/file"+FILEVERSIOn+".txt");	//파일을 생성한다. 
		FILEVERSIOn++; //version을 하나 증가시킨다. 
		
		try {
		
			out = new PrintWriter(new PrintWriter(mFile.getAbsoluteFile()));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // 기록하기 위해 스트림 객체를 생성 한다. 
	}
	//결과를 파일에 저장하는 것이다.
    public boolean writePoweResult(ArrayList<Double> arPowerList){
    	for(int i =0; i < arPowerList.size(); i++){
    		out.printf("%f\n",arPowerList.get(i));
    	}
    	out.close();
    	return true;
    }
}
