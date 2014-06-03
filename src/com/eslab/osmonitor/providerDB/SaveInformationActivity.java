package com.eslab.osmonitor.providerDB;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

import com.eolwral.osmonitor.R;

public class SaveInformationActivity extends Activity {

	final int _ID = 0;
	final int POWER = 1;
	final int CPULOADAVG = 2;
	final int CPULOADSD = 3;
	final int CPUFREQAVG = 4;
	final int CPUFREQSD = 5;
	final int LEDTIMEAVG = 6;
	final int LEDBRIGHTAVG = 7;
	final int LEDBRIGHTSD = 8;
	final int COUNT = 9;
	final int STARTPOINT = 10;
	final int ENDPOINT = 11;
	final int VOLTAGEAVG = 12;
	final int VOLTAGESD = 13;
	final int WIFI_TIME_AVG = 14;
	final int WIFI_PACKET_RATE_AVG = 15;
	final int WIFI_PACKET_RATE_SD = 16;
	
	private ExpandableListView mMyList;	
	private SQLiteDatabase mDb;	// SQLite DB에 접근하기위한 변수
	private Cursor mCursor;
	private DatabaseHelper mSaveDatabases;
	// 커스터 마이징 한 어뎁터이다.
	MyExpandableListAdapter mAdapter;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loadsave);

		mMyList = (ExpandableListView)findViewById(R.id.gamesavelistview);
		mSaveDatabases = new DatabaseHelper(this);
		mDb = mSaveDatabases.getWritableDatabase();

		CursorRefresh();
		
		mAdapter = new MyExpandableListAdapter(this, mCursor, 
				android.R.layout.simple_expandable_list_item_2, 
				R.layout.chunk_list_item);
		
		mMyList.setAdapter(mAdapter);
	}
	OnClickListener pOnClickListener = new OnClickListener() {

		public void onClick(View v) {
		}
	};
	
	public void CursorRefresh(){
		mCursor = mDb.rawQuery("select _id, power, cpu_load_avg, cpu_load_sd," +
				   "cpu_freq_avg, cpu_freq_sd, led_time_avg, " +
				   "led_bright_avg, led_bright_sd, " +
				   "count, start_point, end_point, voltage_avg, voltage_sd, " +
				   "wifi_on_time, wifi_avg_packet_rate, wifi_sd_packet_rate " +
				   "from chunkset", null);
	}
	//option menu를 생성한다. 
	public boolean onCreateOptionsMenu(Menu optionMenu) 
    {
     	super.onCreateOptionsMenu(optionMenu);
     	optionMenu.add(0, 1, 0, getResources().getString(R.string.options_dbtable_delete)); 
    	return true;
    }

	// option menu에 대한 처리를 담당 한다.
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch(item.getItemId())
        {
        	case 1:
    			// 삭제시 포지션 값을 화면에 출력 하여 준다.
    			Toast.makeText(SaveInformationActivity.this,String.format("모든 데이터를 삭제 하셨습니다."), Toast.LENGTH_SHORT)
    			.show();	
    			mDb.delete("chunkset",null,null);
    			//커서의 내용을 다시 변경 한다.
    			CursorRefresh();
    			//어뎁터의 커서를 갱신 시킨다.
    			mAdapter.changeCursor(mCursor);
        	break;
        }
        return true;
    }
    public class MyExpandableListAdapter extends ResourceCursorTreeAdapter {

    	// 생성자로 기본적인 자료를 넘겨 주어야 한다. 
		public MyExpandableListAdapter(Context context, Cursor cursor,
				int groupLayout, int childLayout) {
			super(context, cursor, groupLayout, childLayout);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor,
				boolean isLastChild) {
			// TODO Auto-generated method stub
			TextView mInfoView = (TextView)view.findViewById(R.id.chunk_detail_info);
/*			
			mInfoView.setPadding(3, 3, 3, 3); 
			mInfoView.setTextSize(8);*/
			mInfoView.setText("CPU LOAD : "+cursor.getString(CPULOADAVG)+"%\t\t\tSD:"+cursor.getString(CPULOADSD)+"\n");
			mInfoView.append("CPU Freq : "+cursor.getString(CPUFREQAVG)+"hz\t\t\tSD:"+cursor.getString(CPUFREQSD)+"\n");
			mInfoView.append("LED TIME AVG : "+cursor.getString(LEDTIMEAVG)+"\t\t\tTOTAL TIME:"+cursor.getString(COUNT)+"sec\n");
			mInfoView.append("LED BRIGHT AVG : "+cursor.getString(LEDBRIGHTAVG)+"(max:255)\t\t\tSD:"+cursor.getString(LEDBRIGHTSD)+"\n");
			mInfoView.append("VOLTAGE : "+cursor.getString(VOLTAGEAVG)+"V\t\t\tSD:"+cursor.getString(VOLTAGESD)+"\n");
			mInfoView.append("WIFI Time : "+cursor.getString(WIFI_TIME_AVG)+"\n");
			mInfoView.append("WIFI Packet Rate: "+cursor.getString(WIFI_PACKET_RATE_AVG)+"\t\t\tSD:"+cursor.getString(WIFI_PACKET_RATE_SD)+"\n");
		}
/*		
		@Override
		public View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			// TODO Auto-generated method stub
			return super.newChildView(context, cursor, isLastChild, parent);
		}*/

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			// TODO Auto-generated method stub	
			TwoLineListItem mTwoLineList = (TwoLineListItem)view;
			TextView mInfoView = mTwoLineList.getText1();
			TextView mInfoView2 = mTwoLineList.getText2();
			
			mInfoView.setTextSize(15);
			mInfoView.setTextColor(Color.GREEN);
			mInfoView2.setTextSize(15);
			mInfoView2.setTextColor(Color.BLUE);
			mInfoView.setText(cursor.getString(_ID)+" Power: "+cursor.getString(POWER)+"mW");
			mInfoView2.setText(" Range:\t"+cursor.getString(STARTPOINT)+"% ~ "+cursor.getString(ENDPOINT)+"%");
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			// TODO Auto-generated method stub
			Cursor childCurosr = mDb.rawQuery("select _id, power, cpu_load_avg, cpu_load_sd," +
					   "cpu_freq_avg, cpu_freq_sd, led_time_avg, " +
					   "led_bright_avg, led_bright_sd, " +
					   "count, start_point, end_point, voltage_avg, voltage_sd, " +
					   "wifi_on_time, wifi_avg_packet_rate, wifi_sd_packet_rate " +
					   "from chunkset where _id = "+groupCursor.getString(_ID), null);
			return childCurosr;
		}
 
    }

/*
	//커서 어뎁터의 커스터 마이징 원하는 값을 출력 해주기 위해서 한다.
	private final class ContactListItemAdapter extends ResourceCursorAdapter {
		public ContactListItemAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final ContactListItemCache cache = (ContactListItemCache) view.getTag();
			//캐쉬에서 꺼내는 작업
			TextView bitemId = cache.itemId;
			TextView bitemPower = cache.itemPower;
			TextView bitemCpuLoadAvg = cache.itemCpuLoadAvg;
			TextView bitemCpuFreqAvg = cache.itemCpuFreqAvg;
			TextView bitemLedTimeAvg = cache.itemLedTimeAvg;
			TextView bitemLedBrightAvg = cache.itemLedBrightAvg;
			TextView bitemCount = cache.itemCount;
			TextView bitemRange = cache.itemRange;
			TextView bitemVoltageAvg = cache.itemVoltageAvg;
			
			bitemId.setText(cursor.getString(_ID));
			bitemPower.setText(cursor.getString(POWER));
			bitemCpuLoadAvg.setText(cursor.getString(CPULOADAVG));
			bitemCpuFreqAvg.setText(cursor.getString(CPUFREQAVG));
			bitemLedTimeAvg.setText(cursor.getString(LEDTIMEAVG));
			bitemLedBrightAvg.setText(cursor.getString(LEDBRIGHTAVG));
			bitemCount.setText(cursor.getString(COUNT));
			bitemRange.setText(cursor.getString(RANGE));
			bitemVoltageAvg.setText(cursor.getString(VOLTAGE));
		}
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = super.newView(context, cursor, parent);
			ContactListItemCache cache = new ContactListItemCache();
			cache.itemId = (TextView) view.findViewById(R.id.chunk_id);
			cache.itemPower = (TextView) view.findViewById(R.id.chunk_power);
			cache.itemCpuLoadAvg = (TextView) view.findViewById(R.id.chunk_cpuloadavg);
			cache.itemCpuFreqAvg = (TextView) view.findViewById(R.id.chunk_cpufreqavg);
			cache.itemLedTimeAvg = (TextView) view.findViewById(R.id.chunk_ledtimeavg);
			cache.itemLedBrightAvg = (TextView) view.findViewById(R.id.chunk_ledbrightavg);
			cache.itemCount = (TextView) view.findViewById(R.id.chunk_count);
			cache.itemRange = (TextView) view.findViewById(R.id.chunk_range);
			cache.itemVoltageAvg = (TextView) view.findViewById(R.id.chunk_voltage);
			
			view.setTag(cache);
			return view;
		}
	}
	*/
    
	final static class ContactListItemCache {
		public TextView itemId;
		public TextView itemPower;
		public TextView itemCpuLoadAvg;
		public TextView itemCpuFreqAvg;
		public TextView itemLedTimeAvg;
		public TextView itemLedBrightAvg;
		public TextView itemCount;
		public TextView itemRange;
		public TextView itemVoltageAvg;
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//DB를 쓰고난 다음에 닫아 주어야 한다.
		mDb.close();
		mCursor.close();
	}
	
}
