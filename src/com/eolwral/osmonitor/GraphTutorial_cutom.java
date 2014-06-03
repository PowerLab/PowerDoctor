package com.eolwral.osmonitor;

import java.util.Arrays;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.sunb.lib.SunGraph.GraphView;
import com.sunb.lib.SunGraph.LineInfo;
import com.sunb.lib.SunGraph.XYAxisInfo;

public class GraphTutorial_cutom extends Activity {
	/**
	 * GraphView : 그래프 뷰 (현재까지 Line 그래프만 구현됨)
	 *  / 모든 기능구현 이전에 성능을 가장 고려하여 설계
	 *  / 다수의 line data 를 Color 로 구분하여 표시
	 *  / 각 line 의 의미 String 표시
	 *  / 각 Axis 의 의미 String 표시
	 *  / View Size 에 따라 flexible 한 좌표계산 및 그래핑
	 *  
	 * ※ 트위로 업뎃 등 관련정보 공유드리며, 버그 및 요청사항도 알려주시면 감사하겠습니다! ^-^ (twitter id : startactivity)

	 * @author 선비(http://likeiron.blog.me/)
	 * @version : 0.1
	 * @author sunb(heavwind@gmail.com)
	 */
	
	//그래프를 그리는 객체 이다.
	GraphView mLcdWidget = null;
	GraphView mCpuWidget = null;
	GraphView mWifiWidget = null;
	GraphView m3gWidget = null;
	GraphView mGpsWidget = null;
	GraphView mAudioWidget = null;
	
	//라인 데이터를 가지고 있다.
	LineInfo mLcdLine = null;
	LineInfo mCpuLine = null;
	LineInfo mWifiLine = null;
	LineInfo m3gLine = null;
	LineInfo mGpsLine = null;
	LineInfo mAudioLine = null;
	
	//Thread의 동작 여부를 결정 한다.
	boolean mStateThread = false;
	NewsThread thread;

	ContentResolver cr;
	//JNI 객체
	private static JNIInterface JNILibrary = JNIInterface.getInstance();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph);

        //그래프 불러옵니다. XML에서 크기 등 속성을 변경하실 수 있습니다.
        mLcdWidget = (GraphView)findViewById(R.id.lcd_graph);
        mCpuWidget = (GraphView)findViewById(R.id.cpu_graph);
        mWifiWidget = (GraphView)findViewById(R.id.wifi_graph);
        m3gWidget = (GraphView)findViewById(R.id.cellular_graph);
        mGpsWidget = (GraphView)findViewById(R.id.gps_graph);
        mAudioWidget = (GraphView)findViewById(R.id.audio_graph);
        
        //그려질 데이터 생성
        float[] initialData = new float[60];
       
        Arrays.fill(initialData, 0, 60, 0);       
        //그려질 Line 정보 구체화. 여기선 Line 2개를 만들어봅니다.
        /*
         *	@par1 = 라인의 이름으로 Graph 상단에 표시되며 라인간 구별의 역할
         *	@par2 = 라인의 색깔로 Graph 상단에 라인명과 같이 표시된다.
         *	@par3 = 라인의 두께
         *	@par4 = 그려질 데이터 
         */
        mLcdLine = new LineInfo("LCD", Color.CYAN, 0, initialData);
        mCpuLine = new LineInfo("CPU", Color.GREEN, 0, initialData);
        mWifiLine = new LineInfo("WIFI", Color.BLUE, 0, initialData);
        m3gLine = new LineInfo("3G", Color.YELLOW, 0, initialData);
        mGpsLine = new LineInfo("GPS", Color.RED, 0, initialData);
        mAudioLine = new LineInfo("AUDIO", Color.MAGENTA, 0, initialData);

        // X/Y 축에대한 정보를 구체화. 맨마지막은 구분선의 색이다.
        XYAxisInfo LcdAxis = new XYAxisInfo("elapsed time", "LCD(mW)", 0, 2000, 60, new Point(4,4), Color.GRAY);
        XYAxisInfo CpuAxis = new XYAxisInfo("elapsed time", "CPU(mW)", 0, 1000, 60, new Point(4,4), Color.GRAY);
        XYAxisInfo WifiAxis = new XYAxisInfo("elapsed time", "WIFI(mW)", 0, 100, 60, new Point(4,4), Color.GRAY);
        XYAxisInfo CellularAxis = new XYAxisInfo("elapsed time", "3G(mW)", 0, 100, 60, new Point(4,4), Color.GRAY);
        XYAxisInfo GpsAxis = new XYAxisInfo("elapsed time", "GPSU(mW)", 0, 100, 60, new Point(4,4), Color.GRAY);
        XYAxisInfo AudioAxis = new XYAxisInfo("elapsed time", "AUDIO(mW)", 0, 100, 60, new Point(4,4), Color.GRAY);
        
        //구체화된 객체들로 그래프 초기화
        mLcdWidget.CreateXYAxis(LcdAxis);
        mLcdWidget.AddLine(mLcdLine);
        
        //구체화된 객체들로 그래프 초기화
        mCpuWidget.CreateXYAxis(CpuAxis);
        mCpuWidget.AddLine(mCpuLine);
        
        //구체화된 객체들로 그래프 초기화
        mWifiWidget.CreateXYAxis(WifiAxis);
        mWifiWidget.AddLine(mWifiLine);
        
        //구체화된 객체들로 그래프 초기화
        m3gWidget.CreateXYAxis(CellularAxis);
        m3gWidget.AddLine(m3gLine);
        
        //구체화된 객체들로 그래프 초기화
        mGpsWidget.CreateXYAxis(GpsAxis);
        mGpsWidget.AddLine(mGpsLine);

      //구체화된 객체들로 그래프 초기화
        mAudioWidget.CreateXYAxis(AudioAxis);
        mAudioWidget.AddLine(mAudioLine);
        
        //시스템 정보를 얻어오기 위한 리졸버 얻어온다.
		cr = getContentResolver();
		thread = new NewsThread();
		thread.start();
      //  Button btn =(Button)findViewById(R.id.BUTTON);
     //   btn.setOnClickListener(mBtnClickListener);
    }

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mStateThread = true;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	OnClickListener mBtnClickListener = new OnClickListener()
	{
		public void onClick(View arg0)
		{
			thread = new NewsThread();
			thread.start();
		}
	};
	
	//핸들러에서 Adapter 데이터가 갱신되었으니 새로 그리라는 명령을 받아 주어야 한다.
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			//기존의 라인객체에서 Array 얻어옵니다. 
			float lcdTmp[] = mLcdLine.getLineInnerData();
			float cpuTmp[] = mCpuLine.getLineInnerData();
			float wifiTmp[] = mWifiLine.getLineInnerData();
			float cellTmp[] = m3gLine.getLineInnerData();
			float gpsTmp[] = mGpsLine.getLineInnerData();
			float audioTmp[] = mAudioLine.getLineInnerData();
			
			// 값을 한단계씩 땡긴다.
			for(int i=(cpuTmp.length-1); i >0; i--){
				cpuTmp[i] = cpuTmp[i-1];
				lcdTmp[i] = lcdTmp[i-1];
				wifiTmp[i] = wifiTmp[i-1];
				cellTmp[i] = cellTmp[i-1];
				gpsTmp[i] = gpsTmp[i-1];
			}
			//cpuTmp[0] = JNILibrary.GetCPUUsageValue();
			cpuTmp[0] = (float)OSMonitorService.getInstance().mOPowerModelValue.mCPUPower;
			lcdTmp[0] = (float)OSMonitorService.getInstance().mOPowerModelValue.mLEDPower;
			if(Settings.System.getInt(cr, Settings.Secure.WIFI_ON, -1)>0){
				wifiTmp[0] = 70;
			}
			else{
				wifiTmp[0] = 0;
			}
			if(Settings.System.getInt(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED, -1)>0){
				gpsTmp[0] = 70;
			}
			else{
				gpsTmp[0] = 0;
			}


			//CPU LOAD 관련이다.
			//line 객체에 Array 셋팅
			mLcdLine.setLineInnerData(lcdTmp);
			mCpuLine.setLineInnerData(cpuTmp);
			mWifiLine.setLineInnerData(wifiTmp);
			m3gLine.setLineInnerData(cellTmp);
			mGpsLine.setLineInnerData(gpsTmp);
			mAudioLine.setLineInnerData(audioTmp);
			
			// 그래프에 값 반영
			mLcdWidget.UpdateLine(mLcdLine);	//LCD
			mCpuWidget.UpdateLine(mCpuLine);//CPU LOAD
			mWifiWidget.UpdateLine(mWifiLine);//WIFI signal
			m3gWidget.UpdateLine(m3gLine);
			mGpsWidget.UpdateLine(mGpsLine);
			mAudioWidget.UpdateLine(mAudioLine);
			
			// screen update
			mLcdWidget.UpdateAll();
			mCpuWidget.UpdateAll();
			mWifiWidget.UpdateAll();
			m3gWidget.UpdateAll();
			mGpsWidget.UpdateAll();
			mAudioWidget.UpdateAll();
			
			Log.d("Graph","Handler Response");
		}
	};
	//메시지를 주기적으로 발생시키는 Thread 이다.
	class NewsThread extends Thread {	
		public void run() {
			while(mStateThread == false){
				
				//핸들러 메시지를 보내서 어뎁터를 MAIN UI Thread에서 갱신하도록 한다. 
				mHandler.sendEmptyMessage(0);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}