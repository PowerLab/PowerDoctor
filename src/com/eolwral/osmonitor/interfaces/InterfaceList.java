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

package com.eolwral.osmonitor.interfaces;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.eolwral.osmonitor.*;
import com.eolwral.osmonitor.preferences.Preferences;

public class InterfaceList extends Activity implements OnGestureListener, OnTouchListener
{
	private static InterfaceList Self = null;
    private static ExpandableListView UpdateInterface = null;
	private static JNIInterface JNILibrary = JNIInterface.getInstance();;

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
				((TabActivity) this.getParent()).getTabHost().setCurrentTab(2);
			else if (e2.getX() - e1.getX() > CompareFunc.SWIPE_MIN_DISTANCE &&
										Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) 
				((TabActivity) this.getParent()).getTabHost().setCurrentTab(0);
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
			return true;
		else
		{
			if(v.onTouchEvent(event))
				return true;
			return false;
		}
	}
		
	// Refresh
	private Runnable InterfcaeRunnable = new Runnable() {
		public void run() {

			//뭔가 주기적으로 화면을 갱신 하여 준다.
			if(JNILibrary.doDataLoad() == 1) 
				Self.onRefresh();
			//1초에 한번씩 핸들러를 발생 시켜 준다.
	        InterfaceHandler.postDelayed(this, 1000);
		}
	};   
	
	Handler InterfaceHandler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use a custom layout file
        setContentView(R.layout.interfacelayout);

        // Setup our adapter 선택 했을 때 확장 기능이 있는 adapter 이다.
        Self = this;
        UpdateInterface = (ExpandableListView) findViewById(R.id.interfacelist);
        UpdateInterface.setOnTouchListener(this);
        UpdateInterface.setAdapter(new AllInterfaceList(this, getResources()));
        
    }
    
    public void onRefresh()
    {
    	JNILibrary.doDataSwap();
    	UpdateInterface.invalidateViews();
    }
        
    public boolean onCreateOptionsMenu(Menu optionMenu) 
    {
     	super.onCreateOptionsMenu(optionMenu);
     	optionMenu.add(0, 1, 0, getResources().getString(R.string.options_text));
       	optionMenu.add(0, 5, 0, getResources().getString(R.string.forceexit_text));
        
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
        case 5:
        	if(OSMonitorService.getInstance() != null)
        		OSMonitorService.getInstance().stopSelf();

        	JNILibrary.killSelf(this);
        	break;

        }
        
        return true;
    }


    @Override
    public void onPause() 
    {
    	InterfaceHandler.removeCallbacks(InterfcaeRunnable);
    	JNILibrary.doTaskStop();
    	super.onPause();
    }

    @Override
    protected void onResume() 
    {
    	JNILibrary.SetNetworkIP6To4(0);
    	JNILibrary.doTaskStart(JNILibrary.doTaskInterface);
    	InterfaceHandler.post(InterfcaeRunnable);
    	super.onResume();
    }
    
    //확장형 리스트뷰에 알맞은 어뎁터를 거스터 마이징 한 것이다.
    public class AllInterfaceList extends BaseExpandableListAdapter {
   	
    	private Resources ResourceManager = null;
        private Context mContext = null;

    	public AllInterfaceList(Context context, Resources resource)
        {
            this.mContext = context;
            this.ResourceManager = resource;
        }
        
        public Spanned getChild(int groupPosition, int childPosition) {
        	
        	DecimalFormat SpeedFormat = new DecimalFormat(",###");
        	
        	StringBuilder m_strBuf = new StringBuilder();
        	m_strBuf.setLength(0);
        	
        	m_strBuf.append(ResourceManager.getText(R.string.mac_text)+": ")
        			.append("<b>"+JNILibrary.GetInterfaceMac(groupPosition)+"</b><br />")
        			.append(ResourceManager.getText(R.string.rx_text))
        			.append(": <font color=\"#808080\">");
        	
        	long RxSize = JNILibrary.GetInterfaceInSize(groupPosition);
        	if(RxSize >= 1024*1024*1024)
        		m_strBuf.append((RxSize/(1024*1024*1024))+"G ("+SpeedFormat.format(RxSize).toString()+")");
        	else if(RxSize >= 1024*1024)
        		m_strBuf.append((RxSize/(1024*1024))+"M ("+SpeedFormat.format(RxSize).toString()+")");
        	else if(RxSize >= 1024)
        		m_strBuf.append((RxSize/1024)+"K ("+SpeedFormat.format(RxSize).toString()+")");
        	else 
        		m_strBuf.append(RxSize);
        	
        	m_strBuf.append("</font><br />")
        			.append(ResourceManager.getText(R.string.tx_text))
        			.append(": <font color=\"#808080\">");

        	long TxSize = JNILibrary.GetInterfaceOutSize(groupPosition);
        	if(TxSize >= 1024*1024*1024)
        		m_strBuf.append((TxSize/(1024*1024*1024))+"G ("+SpeedFormat.format(TxSize).toString()+")");
        	else if(TxSize >= 1024*1024)
        		m_strBuf.append((TxSize/(1024*1024))+"M ("+SpeedFormat.format(TxSize).toString()+")");
        	else if(TxSize >= 1024)
        		m_strBuf.append((TxSize/1024)+"K ("+SpeedFormat.format(TxSize).toString()+")");
        	else 
        		m_strBuf.append(JNILibrary.GetInterfaceInSize(groupPosition));
        	
        	m_strBuf.append("</font><br/>")
        			.append(ResourceManager.getText(R.string.status_text)+": ")
        			.append(JNILibrary.GetInterfaceFlags(groupPosition))
        			.append("<br/>");
			
			return Html.fromHtml(m_strBuf.toString());
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        public TextView getGenericView() {
            // Layout parameters for the ExpandableListView
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, 80);

            TextView textView = new TextView(mContext);
            textView.setLayoutParams(lp);
            
            // Center the text vertically
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL | Gravity.LEFT);

            // Set the text starting position
            if(CompareFunc.getScreenSize() == 2)
            	textView.setPadding(60, 5, 0, 0);
            else if( CompareFunc.getScreenSize() == 0)
            	textView.setPadding(20, 5, 0, 0);
            else 
            	textView.setPadding(36, 5, 0, 0);
            
            return textView;
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            TextView textView = getGenericView();
            textView.setText(getChild(groupPosition, childPosition));

            if(CompareFunc.getScreenSize() == 2)
            	textView.setPadding(60, 5, 0, 0);
            else if(CompareFunc.getScreenSize() == 0)
            	textView.setPadding(20, 5, 0 , 0);
            else
            	textView.setPadding(36, 5, 0, 0);

            if(groupPosition % 2 == 0)
	     		textView.setBackgroundColor(0x80444444);
	     	else
	     		textView.setBackgroundColor(0x80000000);

            return textView;
        }

        public Object getGroup(int groupPosition) {
        	String Info = ResourceManager.getText(R.string.interface_text)+": "+
         			   JNILibrary.GetInterfaceName(groupPosition)+ "\n"+
         			   ResourceManager.getText(R.string.ip_text)+": "+
         			   JNILibrary.GetInterfaceAddr(groupPosition)+"/"+
         			   JNILibrary.GetInterfaceNetMask(groupPosition) + " ";
     		if(!JNILibrary.GetInterfaceAddr6(groupPosition).equals(""))
     		{
     			Info += "\n"+
     					ResourceManager.getText(R.string.ip6_text)+": "+
     					JNILibrary.GetInterfaceAddr6(groupPosition)+"/"+
     					JNILibrary.GetInterfaceNetMask6(groupPosition) + " ";
     		}

         
     		return Info;
        }

        public int getGroupCount() {
           return JNILibrary.GetInterfaceCounts();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            TextView textView = getGenericView();
            textView.setText(getGroup(groupPosition).toString());
            
	     	if(groupPosition % 2 == 0)
	     		textView.setBackgroundColor(0x80444444);
	     	else
	     		textView.setBackgroundColor(0x80000000);

	     	return textView;
        }
        
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }
}
