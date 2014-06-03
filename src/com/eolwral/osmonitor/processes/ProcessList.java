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
 
package com.eolwral.osmonitor.processes;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;

import com.eolwral.osmonitor.*;
import com.eolwral.osmonitor.messages.DebugBox;
import com.eolwral.osmonitor.preferences.Preferences;
import com.eslab.osmonitor.providerDB.SaveInformationActivity;

public class ProcessList extends ListActivity implements OnGestureListener, OnTouchListener,  ListView.OnScrollListener
{
	private boolean mBusy = false;
	private static ProcessListAdapter UpdateInterface = null;
	private static ProcessList Self = null;
	private static JNIInterface JNILibrary = JNIInterface.getInstance();
	private static int OrderBy = JNILibrary.doSortPID; //기본적인 정렬방법은 PID로 정렬 하는 것이다.
	
	private ProcessInfoQuery ProcessInfo = null;
	 
	// TextView
	private static TextView CPUUsage = null;
	private static TextView RunProcess = null;
	private static TextView MemTotal = null;
	private static TextView MemFree = null;

	private static DecimalFormat MemoryFormat = new DecimalFormat(",000");

	// Short & Click
	private static int longClick = 2;
	private static int shortClick = 3;
	private static boolean shortTOlong = false;
	private static boolean longTOshort = false;
	
	// Selected item
	private static int selectedPosition = 0;
	private static String selectedPackageName = null;
	private static int selectedPackagePID = 0;
	
	// MultiSelect
	private static CheckBox MultiSelect = null;
	private static Button MultiKill = null;

	// Freeze
	private static CheckBox Freeze = null;
	private static boolean FreezeIt =  false;
	private static boolean FreezeTask = false;
	
	// Root
	private static boolean Rooted = false;
	
	// Slow Adapter
	private static boolean SlowAdapter = false;
	
	// Gesture
	private GestureDetector gestureScanner = new GestureDetector(this);;
	
	private static boolean GestureLong = false;
	private static boolean GestureSingleTap = false;
	
	@Override
	public boolean onTouchEvent(MotionEvent me)
	{
		return gestureScanner.onTouchEvent(me);
	}
	
	@Override
	public boolean onDown(MotionEvent e)
	{
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		try {
			if (Math.abs(e1.getY() - e2.getY()) > CompareFunc.SWIPE_MAX_OFF_PATH)
				return false;
			else if (e1.getX() - e2.getX() > CompareFunc.SWIPE_MIN_DISTANCE && 
						Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY){
				//옆으로 밀어서 넘기는 제스처를 제거 한다.
				//((TabActivity) this.getParent()).getTabHost().setCurrentTab(1);
			}
			else if (e2.getX() - e1.getX() > CompareFunc.SWIPE_MIN_DISTANCE &&
						Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY){
				//옆으로 밀어서 넘기는 제스처를 제거 한다.
				//((TabActivity) this.getParent()).getTabHost().setCurrentTab(4);
			}
			else
				return false;
		} catch (Exception e) {
			// nothing
		}

		GestureLong = false;

		return true;
	}
	
	@Override
	public void onLongPress(MotionEvent e)
	{
		//performLongClick();
		GestureLong = true;
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
		
		GestureSingleTap = true;
		
		return true;
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {

		GestureSingleTap = false;
		
		
		if(gestureScanner.onTouchEvent(event))
		{
			
			GestureLong = false;
			
			if(GestureSingleTap == true)
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
	
	//핸들러에 의해서 동작하는 Thread이다.
	private Runnable uiRunnable = new Runnable() {
		public void run() {

			//전체적인 cpu 사용률과 실핼중인 프로세스의 수와 총 사용 메모리, Free 메모리를 표시한다. 
     		if(JNILibrary.doDataLoad() == 1) {
     			CPUUsage.setText(JNILibrary.GetCPUUsage());
    	     	RunProcess.setText(JNILibrary.GetProcessCounts()+"");
    	     	MemTotal.setText(MemoryFormat.format(JNILibrary.GetMemTotal())+ "K");
    	     	MemFree.setText(MemoryFormat.format(JNILibrary.GetMemBuffer()
    	     					+JNILibrary.GetMemCached()+JNILibrary.GetMemFree())+ "K");
    	    
    	     	Self.onRefresh();
   	     	
     		}
     		else
     		{
     			//정지를 체크 했다면 Thread 동작을 멈춰 준다. 멈춘 상태에서도 지속적으로 이 핸들러는 동작하기 때문에
     			//다시 멈춤을 풀어 줄 수가 있는 것이다.
     			if(FreezeIt)
    			{
    				if(!FreezeTask)
    				{
    					JNILibrary.doTaskStop();
    					FreezeTask = true;
    				}
    				else
    	     			CPUUsage.setText(JNILibrary.GetCPUUsage());
    			}
    			else
    			{
    				if(FreezeTask)
    				{
    					JNILibrary.doTaskStart(JNILibrary.doTaskProcess);
    					FreezeTask = false;
    				}
    			}

     		}
     		//0.05초 마다 핸들러를 발생 시키기 때문에 응답성을 매우 높일 수가 있다.
	        uiHandler.postDelayed(this, 50);
		}
	};   
	
	private Handler uiHandler = new Handler();
	private ActivityManager ActivityMan = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
         gestureScanner = new GestureDetector(this);
        
        // Use a custom layout file
        setContentView(R.layout.processlayout);

        //4가지 정보를 출력하기 위함 이다.
        CPUUsage = (TextView) findViewById(R.id.CPUUsage);
        RunProcess = (TextView) findViewById(R.id.RunProcessText);
        MemTotal = (TextView) findViewById(R.id.MemTotalText);
        MemFree = (TextView) findViewById(R.id.MemFreeText);
        
        // Tell the list view which view to display when the list is empty
        // empty일때 empty라는 글자를 표시해 주기 위함 이다.
        getListView().setEmptyView(findViewById(R.id.empty));

        // Use our own list adapter
        // ListActivity를 상속 받았기 떄문에 이런식으로 처리 한다.
        // Self는 자기 자신에 대한 객체 이다. 
        Self = this;
        Self.getListView().setOnTouchListener(this); //리스너를 등록해 준다.
        setListAdapter(new ProcessListAdapter(this)); //커스터마이즈한 어뎁터를 달아준다.
        UpdateInterface = (ProcessListAdapter) getListAdapter(); //할당한 어뎁터를 다시 읽어 들인다.
        ProcessInfo = ProcessInfoQuery.getInstance(this); //Thread를 생성해서 시작 시킨다.
        ActivityMan = (ActivityManager) getSystemService(ACTIVITY_SERVICE); // 엑티비티 관리자를 얻어온다.
        getListView().setOnScrollListener(this); // 스크롤 리스너를 설정한다.
        
        //MultiKill 멀티 선택을 했을때 멀티 킬을 할 수 있도록 도와주는 것이다.
        MultiKill = (Button) findViewById(R.id.MultiKill);
        MultiKill.setOnClickListener(
          	new OnClickListener(){
           		public void onClick(View v) {
           			String KillCmd = ""; 
           			//현재 화며에 표시되는 프로세스 리스트중에서 선택 된 것을 추출 한다.
           			ArrayList<String> KillList = ProcessInfo.getSelected();
           			for(String pid:KillList)
           			{
           				int tPID = Integer.parseInt(pid);
           				
           				//root 권할일 때와 root 권한이 아닐 때를 구분하여 킬 한다.
           	   	        if(Rooted)
           	   	        {
           	   	        	if(KillCmd.length() == 0)
           	   	        		KillCmd += "kill -9 "+tPID;
           	   	        	else
           	   	        		KillCmd += ";kill -9 "+tPID;
           	   	        }
           	   	        else
           	   	        {
           	   	        	for(int i =0; i < JNILibrary.GetProcessCounts(); i++)
           	   	        	{
           	   	        		if(JNILibrary.GetProcessPID(i) == tPID)
           	   	        		{
           	   	        			//PID 값을 가지고 프로세스를 종료 시킨다.
           	   	        			android.os.Process.killProcess(tPID);
           	   	        			
           	   	        			
           	   	        			//2.1에서는 사용가능하지만 2.2 이상 부터는 동작하지 않는다.
           	   	        			//ActivityMan.restartPackage(JNILibrary.GetProcessName(i));
           	   	        			
           	   	        			//2.2에서 허용하는 코드이다. 하지만 깔끔하게 종료 되지는 않는다.
           	   	        			ActivityMan.killBackgroundProcesses(JNILibrary.GetProcessName(i));
           	   	        			break;
           	   	        		}
           	   	        	}
           	   	        }
           			}
           			
           			//루투 권한을 선택 하였을 때 그냥 커멘드창에다가 명령어를 입력해서 종료하는 방식이다.
           			if(Rooted)
           				JNILibrary.execCommand(KillCmd+"\n");

           			//선택된 정보들을 삭제한다.
         	        ProcessInfo.clearSelected();
         	        
         	        //데이터를 리플래쉬 한다.
         	        JNILibrary.doDataRefresh();
         	        
         	        //어뎁터에게 데이터가 변화됬음을 알려서 다시 ListView를 채우도록 한다.
         	        UpdateInterface.notifyDataSetChanged();
         	        
           			Toast.makeText(Self, "Kill "+KillList.size()+" Process..",
           													Toast.LENGTH_SHORT).show();
    			}
           	}
        );
        
        // Freeze 체크 박스 리스너이다.
        Freeze = (CheckBox) findViewById(R.id.Freeze);
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
        
        // Multi-Select
        MultiSelect = (CheckBox) findViewById(R.id.MultiSelect);
        MultiSelect.setOnCheckedChangeListener(
        	new CompoundButton.OnCheckedChangeListener() {
        		@Override
        		public void onCheckedChanged( CompoundButton buttonView, boolean isChecked) 
        		{
        			if(isChecked)
        			{
        				MultiKill.setEnabled(true);
        			}
        			else
        			{
        				MultiKill.setEnabled(false);
        				ProcessInfo.clearSelected();
        				UpdateInterface.notifyDataSetChanged();
        			}
        		}
        	}
        );
    
        // restore 컨텍스트 메뉴를 ListView에 설정 한다.
        registerForContextMenu(getListView());
    }
    
    //어뎁터를 갱신해주는 기능을 한다.
	public void onRefresh()
	{
		JNILibrary.doDataSwap(); // 단순히  data_refresh값을 리턴해주는 기능을 가진다. 큰의미는 없는것 같다.
		UpdateInterface.notifyDataSetChanged();
	}
	
	//사용자의 설정값을 읽어와서 그에 맞는 동작을 하도록 처리한다.
	private void restorePrefs()
    {
		//system 관련 Application을 제외할지에 대한 것을 판단한다.
		boolean ExcludeSystem = false;
		boolean SortIn = false;
		int Algorithm = 1;

		// load settings 디펄트 셋팅 객체를 로드 한다. 
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		try {
			/*
			 * 사용자 엑션에 대한 설정 값들이다. 1: show Detial 2: Actio Menu 3: Right Detail/Left Action
			 */
			longClick = Integer.parseInt(settings.getString(Preferences.PREF_LONGBEHAVIOR, "2")); 
			shortClick = Integer.parseInt(settings.getString(Preferences.PREF_SHORTBEHAVIOR, "3"));
			
			//프로세스 업데이트 시간을 설정 한다. (default는 2초 이다.)
			JNILibrary.doDataTime(Integer.parseInt(settings.getString(Preferences.PREF_UPDATE, "2")));

			//소팅 종류와 소팅 알고리즘을 선택 한다.
			OrderBy =  Integer.parseInt(settings.getString(Preferences.PREF_ORDER, "1"));
			Algorithm = Integer.parseInt(settings.getString(Preferences.PREF_ALGORITHM, "1"));
		
		} catch(Exception e) {}

	    SortIn = settings.getBoolean(Preferences.PREF_SORT, false);
	    ExcludeSystem = settings.getBoolean(Preferences.PREF_EXCLUDE, false);
	    
	    // change options
   		JNILibrary.SetProcessSort(OrderBy);
   		
   		JNILibrary.SetProcessAlgorithm(Algorithm);
   		
        if(ExcludeSystem)
    		JNILibrary.SetProcessFilter(1);
        else
        	JNILibrary.SetProcessFilter(0);
        
        if(SortIn)
        	JNILibrary.SetProcessOrder(0);
        else 
        	JNILibrary.SetProcessOrder(1);
        
        // change display
        TextView OrderType = (TextView) findViewById(R.id.OrderType);
        
        switch(OrderBy)
        {
        case 1:
        case 2:
        case 5:
        	OrderType.setText(getResources().getString(R.string.load_text));
        	break;
        case 3:
        	OrderType.setText(getResources().getString(R.string.mem_text));
        	break;
        case 4:
        	OrderType.setText(getResources().getString(R.string.thread_text));
        	break;
        }
        UpdateInterface.OrderBy = OrderBy; // 선택된 mode로 정렬하기 위해서 사용한다.
        
    	TableLayout Msv = (TableLayout) findViewById(R.id.MultiSelectView);
        if(settings.getBoolean(Preferences.PREF_HIDEMULTISELECT, false))
        	Msv.setVisibility(View.GONE);
        else
        	Msv.setVisibility(View.VISIBLE);
                
        // Status Bar
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

        // Root
		Rooted = settings.getBoolean(Preferences.PREF_ROOTED, false);
		
		// Slow Adapter
		SlowAdapter = settings.getBoolean(Preferences.PREF_SLOWADAPTER, false);
		

    }

    public boolean onCreateOptionsMenu(Menu optionMenu) 
    {
     	super.onCreateOptionsMenu(optionMenu);
     	optionMenu.add(0, 1, 0, getResources().getString(R.string.options_text));
       	optionMenu.add(0, 4, 0, getResources().getString(R.string.aboutoption_text));
       	optionMenu.add(0, 5, 0, getResources().getString(R.string.forceexit_text));
       	optionMenu.add(0, 6, 0, getResources().getString(R.string.open_db_information));
        
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
        	
    	case 1:
    		return null;
    	}
    	
    	return null;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	restorePrefs();
    }
    
    //opTion 메뉴를 눌렀을 때의 동작에 대해서 기술 되어 있다.
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
        }
        
        return true;
    }

    @Override
    public void onPause() 
    {
    	uiHandler.removeCallbacks(uiRunnable);
    	JNILibrary.doTaskStop();
    	
    	if(Freeze.isChecked())
    	{
    		Freeze.setChecked(false);
    		FreezeIt = false;
    	}
    	
    	if(MultiSelect.isChecked())
    	{
			MultiSelect.setChecked(false);
			MultiKill.setEnabled(false);
			ProcessInfo.clearSelected();
    	}
    	
     	super.onPause();
    }

    //create에서 ProcessInfoQuery 클래스의 Thread를 수행 한다면 이건에서는 JIN로 작성된 Native Thread를 수행한다.
    @Override
    protected void onResume() 
    {   
    	//프리퍼런스 설정값대로 설정을 읽어 드려서 그대로 시스템이 동작하도록 만든다.
        restorePrefs();
        
        //native Thread를 수행 시켜 준다. Thread의 기능이 틀린데 그 기능을 Type으로 구분하여 하나의 Thread로 여러 동작을 하도록 설정 했다.
        JNILibrary.doTaskStart(JNILibrary.doTaskProcess);
    	//정치를 체크 했을 때 반응하는 것과 4개의 메이저 정보를 출력하는 것을 담당하는 핸들러이다. 0.05초 마다 한번씩 동작하게 되어 진다.
        uiHandler.post(uiRunnable);
    	super.onResume();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

    	
    	boolean useMenu = false; 
    	ProcessDetailView selectedItemView =  (ProcessDetailView) 
    							((AdapterContextMenuInfo)menuInfo).targetView;

    	if(!GestureLong)
    	{
    		selectedItemView.setSelected(true);
    		return;
    	}

    	selectedPosition = (int) ((AdapterContextMenuInfo)menuInfo).position;
    	selectedPackageName = ProcessInfo.getPacakge(selectedPosition);
    	selectedPackagePID = JNILibrary.GetProcessPID(selectedPosition);
 
    	if(shortTOlong)
    	{
    		useMenu = true;
    		shortTOlong = false;
    	}
    	else
    	{
    		if(longClick == 1)
    			((ProcessListAdapter)getListAdapter()).toggle(selectedItemView,
    														  selectedPosition,
    														  false,
    														  false);
    		else if(longClick == 2)
        		useMenu = true;
    		else if(longClick == 3)
        		if(!((ProcessListAdapter)getListAdapter()).toggle(selectedItemView,
        														 selectedPosition,
        														 true,
        														 false))
        			useMenu = true;

    	}


    	if(useMenu)
      	{
       		menu.setHeaderTitle(ProcessInfo.getPackageName(selectedPosition));
       		menu.add(0, 1, 0, getResources().getString(R.string.killdialog_text));
       		menu.add(0, 2, 0, getResources().getString(R.string.switchdialog_text));
       		menu.add(0, 3, 0, getResources().getString(R.string.watchlog_text));
       		menu.add(0, 4, 0, getResources().getString(R.string.btncancel_title));
    	}
    	else
    	{
    		menu.clear();
    		longTOshort = true;
    	}
    	

    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) 
    {
        switch(item.getItemId()) 
   	    {
   	    case 1:

   	    	if(Rooted)
   	        {
   	    		JNILibrary.execCommand("kill -9 "+JNILibrary.GetProcessPID(selectedPosition)+"\n");
   	        }
   	        else
   	        {
   	        	android.os.Process.killProcess(JNILibrary.GetProcessPID(selectedPosition));
   	        	ActivityMan.restartPackage(selectedPackageName);
   	        }
   	        
   	        if(FreezeIt && FreezeTask)
   	        {
   	        	JNILibrary.doTaskStart(JNILibrary.doTaskProcess);
   	        	JNILibrary.doDataRefresh();
   	        	JNILibrary.doTaskStop();
   	        }
   	        else 
   	        {
   	        	JNILibrary.doDataRefresh();
   	        }
   	        
   	        UpdateInterface.notifyDataSetChanged();
   	        
   	        return true;
   	        
   	    case 2:

   	    	String ClassName = null;
   	        
   	    	// find ClassName
   	    	PackageManager QueryPackage = this.getPackageManager();
   	        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
   	        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER); 
   	        List<ResolveInfo> appList = QueryPackage.queryIntentActivities(mainIntent, 0);
   	        for(int i=0; i<appList.size(); i++)
   	        {
   	        	if(appList.get(i).activityInfo.applicationInfo.packageName.equals(selectedPackageName))
   	        		ClassName = appList.get(i).activityInfo.name;
   	        }
   	        
   	        if(ClassName != null)
   	        {
   	   	        Intent switchIntent = new Intent();
   	   	        switchIntent.setAction(Intent.ACTION_MAIN);
   	   	        switchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
   	   	        switchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
   	   	        		   			  Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
   	   	        		   			  Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
   	   	        switchIntent.setComponent(new ComponentName(selectedPackageName, ClassName));
   	   	        startActivity(switchIntent);
   	   	        finish();
   	        }
   	        return true;
   	        
   	    case 3:
   	    	Intent WatchLog =  new Intent(this, DebugBox.class);
   	    	WatchLog.putExtra("targetPID", selectedPackagePID);
   	    	startActivity(WatchLog);
   	    	return true;
   	    }
   	    return super.onContextItemSelected(item);
 	}    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
    	if(!GestureSingleTap && !GestureLong)
    	{
    		((ProcessDetailView) v).setSelected(false);
    		return;
    	}
    	
    	if(GestureLong)
    	{
    		v.performLongClick();
    		return;
    	}
    	
    	if(longTOshort)
    	{
    		longTOshort = false;
    		return;
    	}
    	
    	if(MultiSelect.isChecked())
    	{
    		((ProcessListAdapter)getListAdapter()).toggle((ProcessDetailView) v,
					position, false, MultiSelect.isChecked());
    		return;
    	}
    	
    		
    	if(shortClick == 1)
    		((ProcessListAdapter)getListAdapter()).toggle((ProcessDetailView) v,
											position, false, false);
    	else if(shortClick == 2)
		{
			shortTOlong = true;
			GestureLong = true;
			v.performLongClick();
		}
    	else if(shortClick == 3)
    		if(!((ProcessListAdapter)getListAdapter()).toggle((ProcessDetailView) v,
    		   								position, true, false))
    		{
    			shortTOlong = true;
    			GestureLong = true;
    			v.performLongClick();
    		}
    }
    
    //커스터 마이즈한 리스트 어뎁터 이다. 결국 모든것은 이 어뎁터에 의해서 수행되어 질 것이다.
    private class ProcessListAdapter extends BaseAdapter {
    	// onCreate에서 한번 해줫기 때문에 여기서 또 해줄 필요가 없어서 주석 처리 했다. 
    //	private ProcessInfoQuery ProcessInfo = null;
    	public int OrderBy = JNILibrary.doSortPID;
    	
        public ProcessListAdapter(Context context)
        {
    //      ProcessInfo = ProcessInfoQuery.getInstance(context);
            mContext = context;
        }

        public int getCount() {
            return JNILibrary.GetProcessCounts(); //전체 길이에 대한 정보이다.
        }

        public Object getItem(int position) {
            return position; // 현재 위치에대한 정보
        }
     
        public long getItemId(int position) {
            return position; // 현재 아이디에 대한 정보
        }
 
        //실제적으로 ListView를 채우는 코드이다.
        public View getView(int position, View convertView, ViewGroup parent) {
        	
            ProcessDetailView sv = null;

            ProcessInfo.doCacheInfo(position);

        	String OrderValue = "";
        	 
        	switch(OrderBy)
        	{
        	case 1:
        	case 2:
        	case 5:
        		OrderValue = ProcessInfo.getProcessLoad(position);
        		break;
        	case 3:
        		OrderValue = ProcessInfo.getProcessMem(position);
        		break;
        	case 4:
        		OrderValue = ProcessInfo.getProcessThreads(position);
        		break;
        	}
        	
        	//맨 오르쪽에 있는 세모 화살표를 제어 한다.
    		Drawable DetailIcon = null;
    		if(!ProcessInfo.getExpaned(position))
        		DetailIcon = mContext.getResources().getDrawable(R.drawable.dshow);
    		else
    			DetailIcon = mContext.getResources().getDrawable(R.drawable.dclose);

    		
    		if (convertView == null && mBusy == true)
    		{
    			sv = new ProcessDetailView(mContext, ProcessInfo.getProcessPID(position),
    										ProcessInfo.getExpaned(position), position);
    		}
    		else if (convertView == null && mBusy == false) {
                sv = new ProcessDetailView(mContext, ProcessInfo.getAppIcon(position),
                							ProcessInfo.getProcessPID(position),
                							ProcessInfo.getPackageName(position),
                							OrderValue,
        	        						ProcessInfo.getAppInfo(position), 
        	        						ProcessInfo.getExpaned(position),
	               							position,
	               							DetailIcon);
            } 
            else if (mBusy == true)
            {
                sv = (ProcessDetailView)convertView;
            	sv.setView(ProcessInfo.getProcessPID(position), position);
                
                sv.setContext("");
                sv.setExpanded(ProcessInfo.getExpaned(position));
                sv.setMultiSelected(ProcessInfo.getSelected(position));
            }
            else
            {
                sv = (ProcessDetailView)convertView;
               	sv.setView( ProcessInfo.getAppIcon(position), 
               				 ProcessInfo.getProcessPID(position),
               				 ProcessInfo.getPackageName(position),
               				 OrderValue,
               				 position,
               				 DetailIcon);
                
                sv.setContext(ProcessInfo.getAppInfo(position));
                sv.setExpanded(ProcessInfo.getExpaned(position));
                sv.setMultiSelected(ProcessInfo.getSelected(position));
        	}
            
           	return sv;
        }

        public boolean toggle(ProcessDetailView v, int position, boolean split, boolean multi) {

    		if(multi)
    		{
    			if(ProcessInfo.getSelected(position))
    				ProcessInfo.setSelected(position, false);
    			else
    				ProcessInfo.setSelected(position, true);
    			
            	notifyDataSetChanged();
            	
            	return false;
    		}

        	if(v.checkClick() != 1 && split == true) 
        	{
        		return false;
        	}
        	else
        	{
            	if(ProcessInfo.getExpaned(position))
            		ProcessInfo.setExpaned(position, false);
            	else
            		ProcessInfo.setExpaned(position, true);
        	}

        	notifyDataSetChanged();
        	
        	return true;
        }
        
        private Context mContext;
    }
    
    /*
     * ListView의 한셀 한셀을 나타낸다. Expanded의 값에 따라서 자세한 정보가 보여질지 안보여질지를 결정 한다.
     * TableLayout을 상속 받아서 프로세스 정보를 표시해 주는 용도로 완성 시킨다.  
     */
    private class ProcessDetailView extends TableLayout {
    	
    	private TableRow TitleRow;
    	private TextView PIDField;
    	private ImageView IconField;
    	private TextView NameField;
    	private ImageView DetailField;
    	private TextView ValueField;
    	private TextView AppInfoField;
    	
    	private boolean Expanded = false;

        public ProcessDetailView(Context context, Drawable Icon, int PID, String Name,
        						 String Value, String AppInfo, boolean expanded, int position,
        						 Drawable DetailIcon) {
            super(context);
            this.setColumnStretchable(2, true);
            
            //this.setOrientation(VERTICAL);
            
            PIDField = new TextView(context); // 프로세스 ID를 표시한다.
            IconField = new ImageView(context); // 어플리케이션 아이콘을 표시 한다.
            NameField = new TextView(context); // 어플리케이션의 이름을 표시 한다.

            ValueField = new TextView(context); 
            AppInfoField = new TextView(context);
            DetailField = new ImageView(context);
            
            DetailField.setImageDrawable(DetailIcon); // 화살표 아이콘을 의미 한다.
            DetailField.setPadding(3, 3, 3, 3); 
            
            PIDField.setText(""+PID); // PID 정보를 삽입한다.

           	IconField.setImageDrawable(Icon);
           	IconField.setPadding(8, 3, 3, 3);
            
            NameField.setText(Name);
	     	ValueField.setText(Value); //Odervalue가 된다.

            PIDField.setGravity(Gravity.LEFT);
            PIDField.setPadding(3, 3, 3, 3);
            
            //스크린 사이즈에 따른 처리를 해주는 것이다.
            if(CompareFunc.getScreenSize() == 2)
            	PIDField.setWidth(90);
            else if(CompareFunc.getScreenSize() == 0)
            	PIDField.setWidth(35);
            else
            	PIDField.setWidth(55);

            NameField.setPadding(3, 3, 3, 3);
            NameField.setGravity(Gravity.LEFT);
            NameField.setWidth(getWidth()- IconField.getWidth() - DetailField.getWidth() - 115);

            ValueField.setPadding(3, 3, 8, 3);

            if(CompareFunc.getScreenSize() == 2)
            	ValueField.setWidth(80);
            else if (CompareFunc.getScreenSize() == 0)
            	ValueField.setWidth(35);
            else
            	ValueField.setWidth(50);
            
            /*
             * 최종적으로 TableRow를 셍상히야 값을 달아준다.
             * PID Icon(application) Name Value(load값) DetailField(Icon)
             */
            TitleRow = new TableRow(context);
            TitleRow.addView(PIDField);
            TitleRow.addView(IconField);
            TitleRow.addView(NameField);
            TitleRow.addView(ValueField);
            TitleRow.addView(DetailField);
            addView(TitleRow);

            //DetailField 버튼을 눌렀을때 나오는정보를 할당한다. (ProcessInfoQuery에 의해서 값이 만들어 진다.)
	     	AppInfoField.setText(AppInfo);
            addView(AppInfoField);
            //혹장 상태 여부에 따라서 VISIBLE 인지 GONE인지를 결정한다. GONE은 자리까지 제거해서 사라지게 하는 것이다.
	     	AppInfoField.setVisibility(expanded ? VISIBLE : GONE);
	     	
	     	//포지션마다 백그라운드 색을 다르게 한다.
	     	if(position % 2 == 0)
	     		setBackgroundColor(0x80444444);
	     	else
	     		setBackgroundColor(0x80000000);

        }
        
        public ProcessDetailView(Context context, int PID, boolean expanded,int position) {
        	
        	super(context);
        	this.setColumnStretchable(2, true);

        	PIDField = new TextView(context);
        	IconField = new ImageView(context);  
        	NameField = new TextView(context);

        	ValueField = new TextView(context);
        	AppInfoField = new TextView(context);
        	DetailField = new ImageView(context);

        	DetailField.setImageDrawable(null);
            DetailField.setPadding(3, 3, 3, 3);
            
           	IconField.setImageDrawable(null);
        	IconField.setPadding(8, 3, 3, 3);

			PIDField.setText(""+PID);
        	PIDField.setGravity(Gravity.LEFT);
        	PIDField.setPadding(3, 3, 3, 3);

        	if(CompareFunc.getScreenSize() == 2)
        		PIDField.setWidth(90);
        	else if(CompareFunc.getScreenSize() == 0)
        		PIDField.setWidth(35);
        	else
        		PIDField.setWidth(55);

        	NameField.setPadding(3, 3, 3, 3);
        	NameField.setGravity(Gravity.LEFT);
        	NameField.setWidth(getWidth()- IconField.getWidth() - DetailField.getWidth() - 115);

        	ValueField.setPadding(3, 3, 8, 3);

        	if(CompareFunc.getScreenSize() == 2)
        		ValueField.setWidth(80);
        	else if (CompareFunc.getScreenSize() == 0)
        		ValueField.setWidth(35);
        	else
        		ValueField.setWidth(50);

        	NameField.setText("Loading");
        	
        	TitleRow = new TableRow(context);
        	TitleRow.addView(PIDField);
        	TitleRow.addView(IconField);
        	TitleRow.addView(NameField);
        	TitleRow.addView(ValueField);
        	TitleRow.addView(DetailField);
        	addView(TitleRow);

        	addView(AppInfoField);
        	AppInfoField.setVisibility(expanded ? VISIBLE : GONE);

        	if(position % 2 == 0)
        		setBackgroundColor(0x80444444);
        	else
        		setBackgroundColor(0x80000000);

        }


        public void setContext(String AppInfo) {
       		AppInfoField.setText(AppInfo);
		}

		public void setView( Drawable Icon, int PID, String Name, String Value, int position,
							  Drawable DetailIcon) {

			IconField.setImageDrawable(Icon);
			DetailField.setImageDrawable(DetailIcon);
			PIDField.setText(""+PID);
			NameField.setText(Name);
			ValueField.setText(Value);

			if(position % 2 == 0)
				setBackgroundColor(0x80444444);
			else
				setBackgroundColor(0x80000000);
    	}
		
		public void setView(int PID, int position) {

			IconField.setImageDrawable(null);
			DetailField.setImageDrawable(null);
//			IconField.setVisibility(View.GONE);
//			DetailField.setVisibility(View.INVISIBLE);
			PIDField.setText(""+PID);
			NameField.setText("Loading");
			ValueField.setText("");
			
			if(position % 2 == 0)
				setBackgroundColor(0x80444444);
			else
				setBackgroundColor(0x80000000);
		}

        /**
         * Convenience method to expand or hide the dialogue
         */
        public void setExpanded(boolean expanded) {
        	AppInfoField.setVisibility(expanded ? VISIBLE : GONE);
        }
        
        public void setMultiSelected(boolean selected) {
        	if(selected)
        		setBackgroundColor(0x803CC8FF);
        }
        
		public boolean onTouchEvent(MotionEvent event)
		{
			if(event.getX() > getWidth()/3*2 )
				Expanded = true;
			else if (event.getX() <= getWidth()/3*2 )
				Expanded = false;

			return super.onTouchEvent(event);
		}
        
        public int checkClick()
        {
        	if(Expanded == true)
        	{ 
        		Expanded = false;
        		return 1;
        	} 
        	return 0;
        }   
    }
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		
	}
	
	// 스크롤 리스너이다. 화면이 맨 아래로 이동 했을때 다음 내용이 자동적으로 나오도록 처리 하기 위해서
	// 리스너를 등록해 주었다.
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_IDLE:
	            mBusy = false;

	            if(SlowAdapter)
				{
		            int count = view.getChildCount();
		            for (int i=0; i<count; i++) {
		            	view.getChildAt(i).refreshDrawableState();
		            }
				}
	            break;
	        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
	        case OnScrollListener.SCROLL_STATE_FLING:
	        	if(SlowAdapter)
	        		mBusy = true;
	        	else
	        		mBusy = false;
	            break;
        }
	}
}
