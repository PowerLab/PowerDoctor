package com.eolwral.osmonitor;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import com.eolwral.osmonitor.preferences.Preferences;
import com.eslab.osmonitor.providerDB.DatabaseHelper;
import com.eslab.osmonitor.utility.StandardDeviation;

public class OSMonitorService extends Service
{
	private static final String TAG = "OSmonitor";
	private static final int NOTIFYID = 20091231; //통지의 아이디 값이다.
	private static final int POWERMODELID = 20110716; //power model 전용 통지 ID이다.
	private static final int DEBUGNOTIFYID = 20111226; //debug용 이다. 
	private static int battLevel = 0;  // percentage, or -1 for unknown
	private static int temperature = 0;
	private static int voltage = 0;
	private static int useColor = 0;
	private static boolean useCelsius = true;
	private static boolean plugState = false;	//플러긴 상태에 따라서 PowerModel chunk를 수집할지를 결정하기 위해서 상태를 저장한다.
	private static boolean chunkSkip = false;	//맨 처음 시작은 청크를 기록 하지않는다. 그 유무는 플러긴으로 구별된다.
	private static int chunkSkipCount = 0;
    private NotificationManager serviceNM = null;
	private Notification serviceNotify = null;
	private Notification powerModelNotify = null;
	private Notification debugNotify = null;
	private Context serviceContext = null;
	
	private double mBatteryCapacity = 0.015;	//베터리 용량이다.
	
	private boolean TimeUpdate = false;
	private int UpdateInterval = 1;
	
	private static JNIInterface JNILibrary = JNIInterface.getInstance();;

	private static OSMonitorService single = null;
	
	private CPowerModel mPowerModel;	//나의 Power Model 관련 객체 이다.
	
	private int mOldPowerCap;	//이전의 battery cap의 수치 값이다.
	private int mNewPowerCap;	//현재의 battery cap의 수치 값이다.
	
	private ContentResolver mContentResolver;
	private PowerManager mPowermanager;	//Power Manager의 객체이다.
	private WakeLock mWakeLock;	//Wake Lock 객체이다.
	private AudioManager mAudioManager; 
	
	//Power Model 값을 연산해서 DB에 삽입하기위한 것들이다.
	//private boolean OldCapCondition = false;
	private double avgVoltage = 0.0;
	private double avgPower = 0.0;
	private double avgCPULoad = 0.0;
	private double sdCPULoad = 0.0;
	private double avgCPUFrequency = 0.0;
	private double sdCPUFrequency = 0.0;
	private double avgLEDTime = 0.0;
	private double sdLEDTime = 0.0;
	private double avgLEDBright = 0.0;
	private double sdBright = 0.0;
	private double avgWifiOnTime = 0.0;
	private double avgWifiPacketRate = 0.0;
	private int debugCount = 0; // 디버깅을 위해서 만든 것이다.
	
	private double LedOn = 0.0;	//power 값을 계산할때 led값을 반영할지에 대한 여부이다. 
	
	//wifi interface의 state를 알아오기 위한 정보 이다.
	private WifiManager wifiManager;
	long lastTransmitPackets;
	long lastReceivePackets;
	
	//계산한 Power 값을 저장하고 있다. 
	public ArrayList<Double> mPowerValue;
	//Model값을 가지고있는 class이다.
	CPowerModelValue mOPowerModelValue;
	
	//java proc/stat를 읽기위해서
	private CpuUsage mOCpuUsage;
	
	private int AverageConditionBit = 0;
	
	//main activity에서 동작 시키기 위함 이다.
	public static OSMonitorService getInstance()
	{
		if(single != null)
			return single;
		return null;
	}
	
	public class OSMonitorBinder extends Binder 
	{
		OSMonitorService getService()
		{
			return OSMonitorService.this;
		}
	}
	
	private final IBinder mBinder = new OSMonitorBinder();

	private static DecimalFormat MemoryFormat = new DecimalFormat(",000");
	
	private static int cpuLoad = 0;
	
	
	//profile을 해서 power를 계산하는 handler이다.
	private Handler mHandler = new Handler();
	private Runnable mRefresh = new Runnable() 
	{
		@Override  
            public void run() {
				double mWifiPower = 0.0;
				int bright_level=0;
            	cpuLoad = JNILibrary.GetCPUUsageValue();
				if(cpuLoad < 20)
					serviceNotify.iconLevel = 1+useColor*100;
				else if(cpuLoad < 40)
					serviceNotify.iconLevel = 2+useColor*100;
				else if(cpuLoad < 60)
					serviceNotify.iconLevel = 3+useColor*100;
				else if(cpuLoad < 80)
					serviceNotify.iconLevel = 4+useColor*100;
				else if(cpuLoad < 100)
					serviceNotify.iconLevel = 5+useColor*100;
				else 
					serviceNotify.iconLevel = 6+useColor*100;
				
				//java proc/stat
				mOCpuUsage.readStats();
				//notification에 표시하는 정보들을 기록한다.
				String maininfo = "CPU: "+cpuLoad+"% , "+mOCpuUsage.getTotalCPUInt()+"%, "
									 +"MEM:"+MemoryFormat.format(JNILibrary.GetMemBuffer()+JNILibrary.GetMemCached()+JNILibrary.GetMemFree())+ "K"; 

				String extendinfo = "";
				
				if(useCelsius)
					extendinfo = " Battery: "+battLevel+"%"+" ("+temperature/10+"°C)";
				else
					extendinfo = " Battery: "+battLevel+"%"+" ("+((int)temperature/10*9/5+32)+"°F)";

				serviceNotify.setLatestEventInfo(serviceContext, maininfo,
												 extendinfo, serviceNotify.contentIntent);

				/* 기존 소스 주석임 제거시 에러 발생. 
				serviceNotify.contentView.setTextViewText(R.id.StatusBarCPU, "CPU: "+cpuLoad+"%");
				serviceNotify.contentView.setTextViewText(R.id.StatusBarMEM, "MEM: "+MemoryFormat.format(JNILibrary.GetMemBuffer()+JNILibrary.GetMemCached()+JNILibrary.GetMemFree())+ "K");

				if(useCelsius)
					serviceNotify.contentView.setTextViewText(R.id.StatusBarBAT, "BAT: "+battLevel+"%"+" ("+temperature/10+"°C)");
				else
					serviceNotify.contentView.setTextViewText(R.id.StatusBarBAT, "BAT: "+battLevel+"%"+" ("+((int)temperature/10*9/5+32)+"°F)");
				*/

				//App-Widget에 대한 동작 부분을 설정한다.
				//출력할 내뇽을 RemoteViews로 만들어서 관리자에게 전달해야 Widget이 갱신되어 진다.
				RemoteViews views = new RemoteViews(serviceContext.getPackageName(), R.layout.battery_widget);
				views.setTextViewText(R.id.gauge, "" + cpuLoad);
				AppWidgetManager wm = AppWidgetManager.getInstance(OSMonitorService.this);
				ComponentName widget = new ComponentName(serviceContext, BatteryWidget.class);
				wm.updateAppWidget(widget, views);	
				try
				{
					serviceNM.notify(NOTIFYID, serviceNotify);
				} catch(Exception e) {}
				
				//스크린 여부에따라 연산에 적용시킬 껀지를 판별 한다. 
				if(mPowermanager.isScreenOn()){
					LedOn = 1.0;
					bright_level =Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, -1);
				}
				else{
					LedOn = 0.0;
				}
				
				//WIFI chunk를 수집한다.
				//Wifi Interface가 활성화 상태인지 부터 체크 한다. 
				if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
					mPowerModel.mWIFITActivityTime++;
					long transmitPackets = TrafficStats.getTotalTxPackets();
					long receivePackets = TrafficStats.getTotalRxPackets();
					long lastPackets = receivePackets + transmitPackets -
		            lastReceivePackets - lastTransmitPackets;
					//chunk에 packet Rate를 누적 시킨다.
					mPowerModel.mWIFIPacketRate += lastPackets;	//총 패킷을 누적 시킨다.
					mPowerModel.mSDWiFiPacketRate.add((double)lastPackets); //표준편차를 계산하기 위함 이다. 
					
					//wifi Power 정보를 계산한다.
			    	if(  lastPackets >= mOPowerModelValue.mWifiMaxCondition){
			    		mWifiPower = mOPowerModelValue.mWifiMax;
			    	}
			    	else { 
			    		mWifiPower = mOPowerModelValue.mWiFiOn+
						lastPackets * mOPowerModelValue.mWiFiModel_B +
		    			Math.pow(lastPackets,2)*mOPowerModelValue.mWiFiModel_A;
			    	}
					//Packets 정보를 갱신 한다. 
					lastTransmitPackets = transmitPackets;
					lastReceivePackets = receivePackets;
				}
				
				//Power Model 연산을 한다.
/*				mOPowerModelValue.mCurrentPower_typeFour.add(
						LedOn * mOPowerModelValue.mBrightCoeff*bright_level + mOPowerModelValue.mBrightConst + 
						mOPowerModelValue.mCPUUageMulTypeFour*JNILibrary.GetCPUUsageValue() +
						mOPowerModelValue.mCPUUageSumTypeFour +
						mWifiPower);*/
				
				mPowerValue.add(
						LedOn * mOPowerModelValue.mBrightCoeff*bright_level + mOPowerModelValue.mBrightConst + 
						mOPowerModelValue.mCPUUageMulTypeFour*JNILibrary.GetCPUUsageValue() +
						mOPowerModelValue.mCPUUageSumTypeFour +
						mWifiPower);
						
				//current Sensor와 비교하기 위한 루틴 50초마다 평균을 내어주기 위함 이다. 
				/*AverageConditionBit++;
				if(AverageConditionBit >= 50){
					double sumFour = 0.0;
					double avgFour = 0.0;
					for(int i =0; i<mOPowerModelValue.mCurrentPower_typeFour.size(); i++){
						sumFour += mOPowerModelValue.mCurrentPower_typeFour.get(i);
					}
					avgFour = sumFour / 50.0;		
					mPowerValue.add(avgFour);
					
					//초기화 루틴
					AverageConditionBit = 0 ;
					mOPowerModelValue.CurrentPowerClear(); //다시 power값을 줙 시켜야 하므로
				}*/
				mHandler.postDelayed(mRefresh, UpdateInterval * 1000);
            }
    };
    
    //PowerModel을 생성하는 handler이다.
    private Handler mPMGeneratorHandler = new Handler();
	private Runnable mPowerModelGenerator = new Runnable() 
	{
		@Override  
        public void run() {
			
			//plug 상태가 충전중이 아닐 때만 동작 하도록 한다. 
			if(plugState){
						
				mNewPowerCap = battLevel;	//현재 Battery cap을 읽는다.(1초에 한번씩)
				//debug
				/*if(debugCount > 10){
					debugCount = 0;
					battLevel--;
				}
				debugCount ++;*/
				
				
				//Battery cap에 변화가 없다면 누적 연산을 지속 한다.
				if(mOldPowerCap == mNewPowerCap){
					
					//계산을 위해서 누적 하는 부분이다.
					mPowerModel.mCount++;
					mPowerModel.mVoltage += voltage;
					mPowerModel.mSDVoltage.add(voltage);	//표준편차 계산을 위해서 리스트 삽입
					//CPU
					mPowerModel.mCPULoad += JNILibrary.GetCPUUsageValue();
					mPowerModel.mSDCPULoad.add(JNILibrary.GetCPUUsageValue());//표준편차 계산을 위해서 리스트 삽입
					mPowerModel.mCPUFrequency +=JNILibrary.GetProcessorScalCur();
					mPowerModel.mSDCPUFrequency.add(JNILibrary.GetProcessorScalCur()); //표준편차 계산을 위해서 리스트 삽입
					
					//if(mPowerModel.mLEDState){
					if(mPowermanager.isScreenOn()){
						mPowerModel.mLEDActivityTime++;
						mPowerModel.mLEDBrightness += 
							Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, -1);
						mPowerModel.mSDLEDBright.add(
								Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, -1));
						//표준편차 계산을 위해서 리스트 삽입
					}
					//WIFI chunk를 수집한다.
					//Wifi Interface가 활성화 상태인지 부터 체크 한다. 
					if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
						mPowerModel.mWIFITActivityTime++;
						long transmitPackets = TrafficStats.getTotalTxPackets();
						long receivePackets = TrafficStats.getTotalRxPackets();
						long lastPackets = receivePackets + transmitPackets -
			            lastReceivePackets - lastTransmitPackets;
						//chunk에 packet Rate를 누적 시킨다.
						mPowerModel.mWIFIPacketRate += lastPackets;	//총 패킷을 누적 시킨다.
						mPowerModel.mSDWiFiPacketRate.add((double)lastPackets); //표준편차를 계산하기 위함 이다. 
						
						//Packets 정보를 갱신 한다. 
						lastTransmitPackets = transmitPackets;
						lastReceivePackets = receivePackets;
					}
					//3G
					//GPS
					
					//AUDIO 동작 중이라면 오디오 기록 시간을 증가 시킨다.
					if(mAudioManager.isMusicActive()){
						mPowerModel.mAUDIOActivityTime++;
					}
					
					//통지에 출력을 하는 부분이다.
					String maininfo = String.format("L:%.0f R:%.0f B:%.0f F:%.0f", 
							mPowerModel.mCPULoad,mPowerModel.mLEDActivityTime,mPowerModel.mLEDBrightness,mPowerModel.mCPUFrequency);
					String extendinfo = "";
					extendinfo = String.format("Time:%.0f AvgV:%.2f AvgP:%.2f", mPowerModel.mCount,avgVoltage,avgPower);
					serviceNotify.setLatestEventInfo(serviceContext, maininfo,
							 extendinfo, serviceNotify.contentIntent);
					try
					{
						serviceNM.notify(POWERMODELID, serviceNotify);
					} catch(Exception e) {}
				}
				else if(mOldPowerCap > mNewPowerCap){					
					//Power Model의 첫번째 청크는 Skip하기 위함이다. 왜냐하면  battery 1% charge가 올바르지 않기 때문이다.
					if(chunkSkip){
						//전력을 계산해서 청크로 DB에 기록한다.
						avgVoltage = ((double)mPowerModel.mVoltage/mPowerModel.mCount)/1000;
						avgPower = ((mBatteryCapacity*3600*avgVoltage)/mPowerModel.mCount)*1000;
						
						avgCPULoad = (double)mPowerModel.mCPULoad/mPowerModel.mCount;
						avgCPUFrequency = mPowerModel.mCPUFrequency / mPowerModel.mCount;
						
						//잘못된 계산 결과를 맊기 위함이다. 
						if(mPowerModel.mLEDActivityTime == 0){
							avgLEDTime = 0;
							avgLEDBright = 0;
						}
						else{
							avgLEDTime = (double)mPowerModel.mLEDActivityTime / mPowerModel.mCount;
							avgLEDBright = (double)mPowerModel.mLEDBrightness /mPowerModel.mLEDActivityTime; // led가 가동 했을 때만 밝기를 계산하기 때문에.	
						}
						
						//wifi 부분을 계산한다.
						avgWifiOnTime = (double)mPowerModel.mWIFITActivityTime / mPowerModel.mCount;
						avgWifiPacketRate = (double)mPowerModel.mWIFIPacketRate / mPowerModel.mCount;
						
						//표준편차를 계산 한다.
						double SDCPULoad = mPowerModel.mSDCPULoad.EvaluateSD(); 
						double SDCPUFreq = mPowerModel.mSDCPUFrequency.EvaluateSD();
						double SDLEDBright = mPowerModel.mSDLEDBright.EvaluateSD();
						double SDVoltage = mPowerModel.mSDVoltage.EvaluateSD();
						double SDPacketRate = mPowerModel.mSDWiFiPacketRate.EvaluateSD();
						
						/*
						//debug notification을 출력하는 부분이다. wifi packet rate를 조사사기 위함이다. 
						String maininfo = String.format("debug message");
						String extendinfo = "";
						extendinfo = mPowerModel.mSDWiFiPacketRate.printItemList();
						debugNotify.setLatestEventInfo(serviceContext, maininfo,
								 extendinfo, debugNotify.contentIntent);
						try
						{
							serviceNM.notify(DEBUGNOTIFYID, debugNotify);
						} catch(Exception e) {}
						*/
						
						//DB에 기록을 하는 코드 이다.
						ContentValues values = new ContentValues();
						values.put("power", avgPower); 
						values.put("cpu_load_avg", avgCPULoad);						
						values.put("cpu_load_sd",SDCPULoad);						
						values.put("cpu_freq_avg", avgCPUFrequency);		
						values.put("cpu_freq_sd", SDCPUFreq);						
						values.put("led_time_avg", avgLEDTime);
						values.put("led_bright_avg", avgLEDBright);
						values.put("led_bright_sd", SDLEDBright);
						values.put("count", mPowerModel.mCount);
						values.put("start_point",mOldPowerCap);
						values.put("end_point",mNewPowerCap);
						values.put("voltage_avg",avgVoltage); 
						values.put("voltage_sd",SDVoltage);	
						values.put("wifi_on_time", avgWifiOnTime);
						values.put("wifi_avg_packet_rate", avgWifiPacketRate);
						values.put("wifi_sd_packet_rate", SDPacketRate);
						insertPowerModelData(values);
						
						mOldPowerCap = mNewPowerCap; // 값을 다시 같게 해준다.(낮아진 값으로)
						mPowerModel.Clear();	
				
					}
					else{
						chunkSkip = true;	// 처음 시작할떄 1%의 한 청크만 스킵하기 위함이다.
						chunkSkipCount = 1;	// 리시버에 의해서 계속  chunkSkip이 false가 되지 않게 하기 위함이다.
						
						mOldPowerCap = mNewPowerCap; // 값을 다시 같게 해준다.(낮아진 값으로)
						mPowerModel.Clear();	// 클리어는 필수다. 
					}
				}
				else{
					//이경우는 NewPowerCap이 Old보다 높은 경우 이다.
					//충전에 의해서 높아진경우.
					mOldPowerCap = mNewPowerCap; //값을 맞춰주어서 정확히 동작하도록 만들어준다.
				}
			}
			Log.d(TAG, "Old " + mOldPowerCap + " New " + mNewPowerCap + "PlugState"+plugState);
			mPMGeneratorHandler.postDelayed(mPowerModelGenerator,1000);
		}
	};
	//DB에 데이터를 삽입하는 함수이다.
	public boolean insertPowerModelData(ContentValues values){
		Log.d(TAG, "insertBatteryData: " + values);
		
		DatabaseHelper mOpenHelper = null;
		SQLiteDatabase db = null;
		boolean rc = true;
		
		try {
			mOpenHelper = new DatabaseHelper(this);
			db = mOpenHelper.getReadableDatabase();
			Long rowid = db.insert(DatabaseHelper.POWER_TABLE, "PowerModel", values);
			if (rowid < 0) {
				Log.e(TAG, "database insert failed: " + rowid);
				rc = false;
			} else {
				Log.d(TAG, "sample collected, rowid=" + rowid);
			}
		} catch (Exception e) {
			Log.e(TAG, "database exception");
			rc = false;
		}
		if (db != null) db.close();
		if (mOpenHelper != null) mOpenHelper.close();
		return rc;
	}



	@Override
    public void onCreate() {
    	
    	serviceNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    	InitNotification();
    	Notify();
    	InitalPowerModel(); //Power Model 관련 초기화를 수행 한다
    	single = this;
    	mOCpuUsage = new CpuUsage();	//java proc/stat
    	mOPowerModelValue = new CPowerModelValue(); // Power Model 값이다.
    	mOPowerModelValue.intialModel();
    	mPowerValue = new ArrayList<Double>(); //Power 연산 값을 저장하기 위한 Array List이다.
    	wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE); // wifi Interface 객체를 얻어온다. 
    	initWifivariable(); // Wifi chunk관련 변수들의 초기화를 담당 한다.
    	
    }
	
	public void initWifivariable(){
		lastTransmitPackets = TrafficStats.getTotalTxPackets();
		lastReceivePackets = TrafficStats.getTotalRxPackets();
	}
    
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}


	@Override
    public void onDestroy() {
    	Disable();
    	//WakeLock을 제거한다.
    	mWakeLock.release();
    	//debug profile != PowerModel 핸들러를 제거 한다.
    	mPMGeneratorHandler.removeCallbacks(mPowerModelGenerator);
    	//통지를 제거한다.
    	serviceNM.cancel(POWERMODELID);
    }

    public void Notify()
    {
    	Enable();
    }
    
	//Power Model을 동작하게 하는데 들어가는 각종 초기화 작업이다.
    public void InitalPowerModel(){
    	mPowerModel = new CPowerModel();	//Power model 객체
    	//battery cap
    	mNewPowerCap = 0;
    	mOldPowerCap = 0;
    	mContentResolver = getContentResolver();	//시스템 설정값을 알아오기위해 컨텐트리졸버를 얻어와야한다.
    	
    	//Wake Lock를 얻어온다.
    	mPowermanager = (PowerManager) getSystemService(Context.POWER_SERVICE); //파워 매니저를 얻어온다. (스크린 상태를 조사한다.)
    	mWakeLock = mPowermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeAlways"); //WakeLock을 만들어 낸다.
    	mWakeLock.acquire(); // sleep mode에서도 CPU를 활성화 시키기위한 WakeLock이다.
    	
    	mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE); //오디오 매니저를 얻어온다. (오디오 상태를 조사하기 위해서)
    	//debug profile러의 속도를 향상 시키기위해서 CHunk수집 기능을 제거한다.
    	mPMGeneratorHandler.postDelayed(mPowerModelGenerator,1000); // PowerModel 핸드러를 시작 시킨다. 

    }
    private void Enable()
    {
    	//지속적으로 Enable이 호출 되기 때문에 필터의 등록 여부를 다시 확인한다.
    	//설정을 바궜다고해서 등록한 필터를 또 등록할 필요는 없기 때문이다.
    	if(!mRegistered)
    	{
    		// 동적으로 리시버와 필터를 등록해 준다. 
    		IntentFilter filterScreenON = new IntentFilter(Intent.ACTION_SCREEN_ON);
    		registerReceiver(mReceiver, filterScreenON);
    		IntentFilter filterScreenOFF = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    		registerReceiver(mReceiver, filterScreenOFF);
    		
    		//리시버가 등록 되었는지 여부를 나타내는 플래그 이다. 
    		mRegistered = true;
    	}
    	
		// load settings
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		try {
			UpdateInterval = Integer.parseInt(settings.getString(Preferences.PREF_UPDATE, "2"));
		} catch(Exception e) {}		
		
		//프리퍼런스 속성에 의존된다.
		if(settings.getBoolean(Preferences.PREF_CPUUSAGE, false))
		{
			//일단 처음엔 등록이 안되었기 때문에 핸들러 메시지를 한번 날려주는 형태이다.
			/*
			 *이걸 하는 이유는 Acitivyㅇ서 프리퍼런스나. 리쥼에 의해서 다시 재시작 됬을떄
			 *설정값이 바뀌었을수도있기 때문에 Enaable을 한번 더해준다. 근데 이미 서비스가 실행 중이라면
			 *서비스 핸들러가 중복해서 2개가 돌아가는것이기 때문에 따라서 TimeUpdate를 가지고 구분하게 되어진다.  
			 */
			if(TimeUpdate == false)
			{
				JNILibrary.doCPUUpdate(1);
				mHandler.postDelayed(mRefresh, UpdateInterval * 1000);
				TimeUpdate = true; // 핸들러 메시지가 한번 전송 되었기 때문에 true로 값이 바뀐다. 
			}
		}
		else
		{
			//동작하고 있으면 제거해 준다. 
			if(TimeUpdate == true)
			{
				// modi = 0 ->1
	    		JNILibrary.doCPUUpdate(1);
	    		mHandler.removeCallbacks(mRefresh);
	    		TimeUpdate = false;
			}
			serviceNotify.iconLevel = 0;
			serviceNM.notify(NOTIFYID, serviceNotify);
		}
		
		useCelsius = settings.getBoolean(Preferences.PREF_TEMPERATURE, true);
		useColor =  Integer.parseInt(settings.getString(Preferences.PREF_STATUSBARCOLOR, "0"));
		
		startBatteryMonitor();
    }
    
    private void Disable()
    {
    	serviceNM.cancel(NOTIFYID);
    	
    	if(TimeUpdate)
    	{	// 0 -> 1
    		JNILibrary.doCPUUpdate(1);
    		mHandler.removeCallbacks(mRefresh);
    		TimeUpdate = false;
    	}

    	if(mRegistered)
    	{
    		unregisterReceiver(mReceiver);
    		mRegistered = false;
    	}
    	
    	stopBatteryMonitor();
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
			
			int rawlevel = intent.getIntExtra("level", -1);
			int scale = intent.getIntExtra("scale", -1);
			int plug;
			voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);	//베터리의 전압을 조사한다.
			temperature = intent.getIntExtra("temperature", -1);
			
			plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);		//외부 전원이 연결 되어있는 지를 조사한다. (e.g : AC, USB)
			// 베터리 충전 상태를 구분 한다. 이 상태에 따라서 Chunk를 모을지 안모을지를 결정한다.
			switch (plug) {
			case BatteryManager.BATTERY_PLUGGED_AC:
				plugState = false;
				
				chunkSkip = false;
				chunkSkipCount = 0; //다시 신뢰할수 없는 상태로 돌아감.
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:
				plugState = false;
				
				chunkSkip = false;
				chunkSkipCount = 0; //다시 신뢰할수 없는 상태로 돌아감.
				break;
			default:
				//초기화시에 핸들러가 등록되어 지지않고 플러긴 상태에 따라서 핸들러를 다르게 등록 시킨다.
				plugState = true;
				
				//이 구문이 계속 수행되어지기 때문에 0이라면 false를 유지시켜줘야한다. 1이라면 더이상 false면 안된다.
				//왜냐하면 처음 딱 한번만 Skip해야 하기 때문이다. 
				if(chunkSkipCount == 0) chunkSkip = false;	// 첫번째를 무시하기 위함이다.
		    	break;
			}
			if (rawlevel >= 0 && scale > 0) {
				//debug;
				battLevel = (rawlevel * 100) / scale;
			}
		}
	};

    private boolean mRegistered = false;
    //sleep mode 일때 쓸때 없는 동작을 막아 주기위한 리시버이다.
    //즉 시스템에서 ACTION_SCREEN_OFF라고 메시지가 날라온다면 그때 모든 작업을 정지 시킨다.
    private BroadcastReceiver mReceiver = new BroadcastReceiver() 
    {
    	public void onReceive(Context context, Intent intent) {
    		if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
    		{
    			mPowerModel.mLEDState = false;	// screen off를 나타낸다.
    
    			if(TimeUpdate)												
    	    	{	//0->1
    				/*
    				 * doCPUUpdate는 native 함수에서 cpu_usage_bg를 결정한다
    				 * 즉 1이 설정되면 cpu_usage관련 함수들이 동작을 한다. 
    				 * e.g : cpu_refresh(), cpu_refresh_usage(), misc_dump_processor(), mem_dump()
    				 * cpu_usage_bg가 0 라면 update_process가 1일때 즉, Activity가 프로세스 Activity일때 만 위의 매서드들이
    				 * 동작하게 되어 진다. 
    				 */
    	    		JNILibrary.doCPUUpdate(1); 
    	    		//mHandler.removeCallbacks(mRefresh);
    	    		TimeUpdate = false;
    	    	}
    		}
    		else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
    		{
    			mPowerModel.mLEDState = true;	// screen on을 나타낸다.
    			// load settings
    			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    			if(settings.getBoolean(Preferences.PREF_CPUUSAGE, false))
    			{
    				if(TimeUpdate == false)
    				{
    					JNILibrary.doCPUUpdate(1);
    					//mHandler.postDelayed(mRefresh, UpdateInterval * 1000);
    					TimeUpdate = true;
    				}
    			}
    		}
    	}
    }; 
     
    @Override
    public IBinder onBind(Intent intent) {
            return mBinder;
    }
    
    
    //통지 초기화
    private void InitNotification() 
    {
	    int thisIcon = R.anim.statusicon;        		// icon from resources
	    long thisTime = System.currentTimeMillis();     // notification time
	    
	    serviceContext = this; 
	    CharSequence tickerText = getResources().getString(R.string.bar_title);
	    CharSequence contentText =  getResources().getString(R.string.notify_text);
	    CharSequence contentTextSecond = getResources().getString(R.string.service_text);
	    CharSequence contentTitle = getResources().getString(R.string.app_title);
	    CharSequence contentTitleSecond = getResources().getString(R.string.service_title);
	    serviceNotify = new Notification(thisIcon, tickerText, thisTime);
	    serviceNotify.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;

	    //RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notificationlayout);
    	//contentView.setImageViewResource(R.id.image, R.drawable.appicon);
    	//contentView.setTextViewText(R.id.StatusBarCPU, "Wait..");
    	//serviceNotify.contentView = contentView;

    	Intent notificationIntent = new Intent(this, OSMonitor.class);
    	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    	serviceNotify.contentIntent = contentIntent;

	    serviceNotify.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

    	serviceNM.notify(NOTIFYID, serviceNotify);
    	
    	//Power Model Notification이다.
    	powerModelNotify = new Notification(thisIcon, tickerText, thisTime);
    	powerModelNotify.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;
    	powerModelNotify.contentIntent = contentIntent;
    	powerModelNotify.setLatestEventInfo(this, contentTitleSecond, contentTextSecond, contentIntent);
    	serviceNM.notify(POWERMODELID, powerModelNotify);
    	
    	//debug Notify이다.
    	debugNotify = new Notification(thisIcon, tickerText, thisTime);
    	debugNotify.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;
    	debugNotify.contentIntent = contentIntent;
    	debugNotify.setLatestEventInfo(this, contentTitleSecond, contentTextSecond, contentIntent);
    	serviceNM.notify(DEBUGNOTIFYID, debugNotify);
    }

    //Power Model 값을 보유하고있으며, 계산 결과를 누적시키는 클래스이다.
    class CPowerModelValue{
    	public ArrayList<Double> mCurrentPower;
    	public ArrayList<Double> mCurrentPower_typeOne;
    	public ArrayList<Double> mCurrentPower_typeTwo;
    	public ArrayList<Double> mCurrentPower_typeThree;
    	public ArrayList<Double> mCurrentPower_typeFour;
    	public ArrayList<Double> mCPUUage;
    	public ArrayList<Double> mBright;
    	
    	public double mLEDPower = 0.0;
    	public double mCPUPower = 0.0;
    	
    	//regression value
    	public double mBrightTypeOne;
    	public double mBrightTypeTwo;
    	public double mBrightTypeThree;
    	public double mBrightConst;
    	public double mBrightCoeff;
    	
    	public double mCPUUageMulTypeOne;
    	public double mCPUUageSumTypeOne;
    	public double mCPUUageMulTypeTwo;
    	public double mCPUUageSumTypeTwo;
    	public double mCPUUageMulTypeThree;
    	public double mCPUUageSumTypeThree;
    	public double mCPUUageMulTypeFour;
    	public double mCPUUageSumTypeFour;
    	public double mWiFiOn;
    	public double mWiFiModel_A;
    	public double mWiFiModel_B;
    	public double mWifiMax;
    	public int mWifiMaxCondition;
    	
    	
    	public CPowerModelValue(){
    		mCPUUage = new ArrayList<Double>();
    		mBright = new ArrayList<Double>();
    		mCurrentPower = new ArrayList<Double>();
    		mCurrentPower_typeOne = new ArrayList<Double>();
        	mCurrentPower_typeTwo = new ArrayList<Double>();
        	mCurrentPower_typeThree = new ArrayList<Double>();
        	mCurrentPower_typeFour = new ArrayList<Double>();
    	}
    	public void intialModel(){
    		mBrightConst = 285.036; // chunk very high 압축 (45개)
    		mBrightCoeff = 2.281;
  		
    		mCPUUageMulTypeFour = 7.080; // chunk very high 압축 
    		mCPUUageSumTypeFour = 131.495;
    		
        	mWiFiModel_A = 0.0525;
        	mWiFiModel_B = 2.3181;
        	mWiFiOn = 23.764;
        	mWifiMax = 890.704;
        	mWifiMaxCondition = 109;
    	}
    	
		public void ModelClear(){
			mCPUUage.clear();
			mBright.clear();
		}
		public void CurrentPowerClear(){
        	mCurrentPower_typeFour.clear();
		}
	}
    
    //청크를 구성하기 전에 데이터를 누적하기위한 데이터 Class이다.
    class CPowerModel{
    	public boolean mLEDState;
    	public double mLEDActivityTime;
    	public double mLEDBrightness;
    	
    	public double mCPULoad;
    	public double mCPUFrequency;
    	
    	public boolean mGPSState;
    	public double mGPSActivityTime;
    	
    	public double mCellularRx;
    	public double mCellularTx;
    	public double mCellularActivityTime;
    	
    	public double mWIFIPacketRate;
    	public double mWIFIRx;
    	public double mWIFITx;
    	public double mWIFITActivityTime;
    	
    	public boolean mAUDIOState;
    	public double mAUDIOActivityTime;
    	public double mCount;
    	public double mVoltage;
    	
    	//표준편차를 구하기 위함 객체들
    	public StandardDeviation mSDLEDBright;
    	public StandardDeviation mSDCPULoad;
    	public StandardDeviation mSDCPUFrequency;
    	public StandardDeviation mSDCellularRx;
    	public StandardDeviation mSDCellularTx;
    	public StandardDeviation mSDWIFIRx;
    	public StandardDeviation mSDWIFITx;
    	public StandardDeviation mSDWiFiPacketRate;
    	public StandardDeviation mSDVoltage;
    	
    	

    	public CPowerModel(){
    		
        	mLEDState = true; //시작할때는 어차피 프로그램이 한번은 켜지기 때문에 화면이 켜진 상태가 맞다.
        	mLEDActivityTime = 0.0;
        	mLEDBrightness = 0.0;
        	
        	mCPULoad = 0.0;
        	mCPUFrequency = 0.0;
        	
        	mGPSState = false; //장치가 켜졌다고해서 활성화가 된것이 아니기 때문에 지금 상태가 맞을 것이다.  
        	mGPSActivityTime = 0.0;
        	
        	mCellularRx = 0.0;
        	mCellularTx = 0.0;
        	mCellularActivityTime = 0.0;
        	
        	mWIFIPacketRate = 0.0;
        	mWIFIRx = 0.0;
        	mWIFITx = 0.0;
        	mWIFITActivityTime = 0.0;
        	
        	mAUDIOState = false; //오디오의 경우 애매한 부분이다.
        	mAUDIOActivityTime = 0.0;
        	mCount = 0.0;
        	mVoltage = 0.0;
        	
        	//표준편차를 구하기 위함 객체들의 초기화 코드이다.
        	mSDLEDBright = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDCPULoad = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDCPUFrequency = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDCellularRx = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDCellularTx = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDWiFiPacketRate = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDWIFIRx = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDWIFITx = new StandardDeviation(StandardDeviation.DOUBLE);
        	mSDVoltage = new StandardDeviation(StandardDeviation.DOUBLE);
    	}
    	public void Clear(){
        	//mLEDState = true;	상태는 초기화 되면 안된다
        	mLEDActivityTime = 0.0;
        	mLEDBrightness = 0.0;
        	
        	mCPULoad = 0.0;
        	mCPUFrequency = 0.0;
        	
        	//mGPSState = false; 상태는 초기화 되면 안된다
        	mGPSActivityTime = 0.0;
        	
        	mCellularRx = 0.0;
        	mCellularTx = 0.0;
        	mCellularActivityTime = 0.0;
        	
        	mWIFIPacketRate = 0.0;
        	mWIFIRx = 0.0;
        	mWIFITx = 0.0;
        	mWIFITActivityTime = 0.0;
        	
        	//mAUDIOState = false; 상태는 초기화 되면 안된다
        	mAUDIOActivityTime = 0.0;
        	mCount = 0.0;
        	mVoltage = 0.0;
        	
        	//표준편차를 구하는 개체들의 내부를 초기화 해준다.
        	mSDLEDBright.clear();
        	mSDCPULoad.clear();
        	mSDCPUFrequency.clear();
        	mSDCellularRx.clear();
        	mSDCellularTx.clear();
        	mSDWiFiPacketRate.clear();
        	mSDWIFIRx.clear();
        	mSDWIFITx.clear();
        	mSDVoltage.clear();
    	}
    }
}
