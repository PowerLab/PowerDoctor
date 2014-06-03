package com.eslab.osmonitor.traffic;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.eolwral.osmonitor.CompareFunc;
import com.eolwral.osmonitor.JNIInterface;
import com.eolwral.osmonitor.R;
import com.eolwral.osmonitor.processes.ProcessInfoQuery;

/*
 * 어플리케이션별 Traffic 정보를 출력해주는 Class이다.
 */
public class ApplicationTrafficList extends ListActivity implements
		OnGestureListener, OnTouchListener, ListView.OnScrollListener {
	private static ApplicationTrafficList Self = null;
	private static JNIInterface JNILibrary = JNIInterface.getInstance();

	// 것이다.

	private ProcessInfoQuery ProcessInfo = null;

	// TextView
	private TextView mWIFIStateText = null;
	private TextView RunProcess = null;
	private TextView MemTotal = null;
	private TextView MemFree = null;
	private TextView mWIFITxtext = null;
	private TextView mWIFIRxtext = null;

	private static DecimalFormat MemoryFormat = new DecimalFormat(",000");

	// Gesture
	private GestureDetector gestureScanner = new GestureDetector(this);;
	List<ActivityManager.RunningAppProcessInfo> appList2;
	private static boolean GestureSingleTap = false;

	// 설치된 어플리케이션의 리스트 이다.
	private ArrayList<Integer> items = new ArrayList();
	private PackageManager mPackageManager;
	ActivityManager activityManager;

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gestureScanner.onTouchEvent(me);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		try {
			if (Math.abs(e1.getY() - e2.getY()) > CompareFunc.SWIPE_MAX_OFF_PATH)
				return false;
			else if (e1.getX() - e2.getX() > CompareFunc.SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) {
				// 옆으로 밀어서 넘기는 제스처를 제거 한다.
				// ((TabActivity)
				// this.getParent()).getTabHost().setCurrentTab(1);
			} else if (e2.getX() - e1.getX() > CompareFunc.SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > CompareFunc.SWIPE_THRESHOLD_VELOCITY) {
				// 옆으로 밀어서 넘기는 제스처를 제거 한다.
				// ((TabActivity)
				// this.getParent()).getTabHost().setCurrentTab(4);
			} else
				return false;
		} catch (Exception e) {
			// nothing
		}

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// performLongClick();
		return;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
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

		if (gestureScanner.onTouchEvent(event)) {

			if (GestureSingleTap == true)
				v.onTouchEvent(event);

			return true;
		} else {

			if (v.onTouchEvent(event))
				return true;
			return false;
		}

	}

	// 핸들러에 의해서 동작하는 Thread이다.
	private Runnable uiRunnable = new Runnable() {
		public void run() {

			// 3G통신의 데이터 RX(수신) KByte 값이다.
			if (TrafficStats.getMobileRxBytes() == TrafficStats.UNSUPPORTED) {
				MemTotal.setText("UNSUPPORTED!");
			} else {
				MemTotal.setText(MemoryFormat.format(TrafficStats
						.getMobileRxBytes() / 1024)
						+ "KB");
			}
			// 3G통신의 데이터 TX(송신) KByte 값이다.
			if (TrafficStats.getMobileTxBytes() == TrafficStats.UNSUPPORTED) {
				MemFree.setText("UNSUPPORTED!");
			} else {
				MemFree.setText(MemoryFormat.format(TrafficStats
						.getMobileTxBytes() / 1024)
						+ "KB");
			}

			// WIFI 통신의 데이터 RX(수신) KByte 값이다.
			if (TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED) {
				mWIFIRxtext.setText("UNSUPPORTED!");
			} else {
				mWIFIRxtext.setText(MemoryFormat.format(TrafficStats
						.getTotalRxBytes() / 1024)
						+ "KB");
			}
			// WIFI 통신의 데이터 TX(송신) KByte 값이다.
			if (TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED) {
				mWIFITxtext.setText("UNSUPPORTED!");
			} else {
				mWIFITxtext.setText(MemoryFormat.format(TrafficStats
						.getTotalTxBytes() / 1024)
						+ "KB");
			}
			appList2 = activityManager.getRunningAppProcesses();
			RunProcess.setText(""+appList2.size());
			// 0.05초 마다 핸들러를 발생 시키기 때문에 응답성을 매우 높일 수가 있다.
			uiHandler.postDelayed(this, 50);
		}
	};

	private Handler uiHandler = new Handler();
	private ActivityManager ActivityMan = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gestureScanner = new GestureDetector(this);

		// Use a custom layout file
		setContentView(R.layout.traffic_list);

		// UI Thread에서 관리하는 View들이다.
		RunProcess = (TextView) findViewById(R.id.Traffic_RunProcessText);
		MemTotal = (TextView) findViewById(R.id.Traffic_MemTotalText);
		MemFree = (TextView) findViewById(R.id.Traffic_MemFreeText);

		mWIFIRxtext = (TextView) findViewById(R.id.Traffic_MemTotalText2);
		mWIFITxtext = (TextView) findViewById(R.id.Traffic_MemFreeText2);

		// Tell the list view which view to display when the list is empty
		// empty일때 empty라는 글자를 표시해 주기 위함 이다.
		getListView().setEmptyView(findViewById(R.id.Traffic_empty));

		// Use our own list adapter
		// ListActivity를 상속 받았기 떄문에 이런식으로 처리 한다.
		// Self는 자기 자신에 대한 객체 이다.
		Self = this;
		Self.getListView().setOnTouchListener(this); // 리스너를 등록해 준다.

		ActivityMan = (ActivityManager) getSystemService(ACTIVITY_SERVICE); // 엑티비티
		// 관리자를
		// 얻어온다.

		mPackageManager = getPackageManager();

		// 실행중인 프로세스의 리스트를 얻어오기 위함 이다.
		activityManager = (ActivityManager) this
				.getSystemService(ACTIVITY_SERVICE);
		appList2 = activityManager.getRunningAppProcesses();

		Iterator it = appList2.iterator();
		// 순회 하면서 실행중 프로세서들의 pid값을 얻어온다.
		while (it.hasNext()) {
			ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) it
					.next();
			items.add(info.pid);
		}
		setListAdapter(new ProcessListAdapter(this,appList2)); //커스터마이즈한 어뎁터를 달아준다.
		/*setListAdapter(new MyAdapter(this, R.layout.my_app_row,
				R.id.traffic_pid, appList2));*/
	}

	// 어뎁터를 갱신해주는 기능을 한다.
	public void onRefresh() {

	}

	@Override
	public void onPause() {
		uiHandler.removeCallbacks(uiRunnable);
		super.onPause();
	}

	// create에서 ProcessInfoQuery 클래스의 Thread를 수행 한다면 이건에서는 JIN로 작성된 Native
	// Thread를 수행한다.
	@Override
	protected void onResume() {

		// 정치를 체크 했을 때 반응하는 것과 4개의 메이저 정보를 출력하는 것을 담당하는 핸들러이다. 0.05초 마다 한번씩 동작하게
		// 되어 진다.
		uiHandler.post(uiRunnable);
		super.onResume();
	}

	// ArrayAdapter를 상속한 커스텀 어댑터 --> 데이터를 보기 좋게 커스터마이징
	class MyAdapter extends ArrayAdapter {

		public MyAdapter(Context context, int resource, int textViewResourceId,
				List items) {
			super(context, resource, textViewResourceId, items);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub

			// 레이아웃을 확장 한다.
			View row = getLayoutInflater().inflate(R.layout.my_app_row, null);

			// 현재 위치의 아이템을 얻어온다.
			ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) getItem(position);

			TextView PidTextView = (TextView) row
					.findViewById(R.id.traffic_pid);
			TextView ApplciationNameTextView = (TextView) row
					.findViewById(R.id.traffic_application_name);
			ImageView IconTextView = (ImageView) row
					.findViewById(R.id.traffic_appicon);

			PidTextView.setText(String.format("%d", info.pid));
			ApplciationNameTextView.setText(info.processName);
			try {
				IconTextView.setImageDrawable(mPackageManager
						.getApplicationIcon(mPackageManager
								.getNameForUid(info.uid)));
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return row;
		}

	}

	// 커스터 마이즈한 리스트 어뎁터 이다. 결국 모든것은 이 어뎁터에 의해서 수행되어 질 것이다.
	private class ProcessListAdapter extends BaseAdapter {

		public ProcessListAdapter(Context context, List<ActivityManager.RunningAppProcessInfo> arList) {
			mContext = context;
			mList = arList;
		}

		public int getCount() {
			
			return appList2.size();
		}

		public Object getItem(int position) {
			return position; // 현재 위치에대한 정보
		}

		public long getItemId(int position) {
			return position; // 현재 아이디에 대한 정보
		}

		// 실제적으로 ListView를 채우는 코드이다.
		public View getView(int position, View convertView, ViewGroup parent) {

			ProcessDetailView sv = null;
			PackageInfo appPackageInfo = null;
			// 현재 위치의 아이템을 얻어온다.
			ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) mList.get(position);
			/*mPackageManager
			.getApplicationIcon(info.processName)*/
			
			//이름에 ':'문자를 포함하고 있다면 :전까지의 내용을 PackageName으로 정의한다. 이렇게 해야 정상적으로 icon과 label을 추출 할 수 있다.	
			if(info.processName.contains(":")){
				PackageName = info.processName.substring(0,info.processName.indexOf(":"));
			}
			else
				PackageName = info.processName;
			
			//해당 PackageName에 대한 정보를 PackageManager로 부터 받아 온다. 
			try {
				appPackageInfo = mPackageManager.getPackageInfo(PackageName, 0);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//PackageName에 대한 정보가 없을경우 SearchObj에서 UID를 얻어온다.
			if(appPackageInfo == null && info.uid >0)
			{
				//UID를 통해서 Package Name을 확보 한다.
				String[] subPackageName = mPackageManager.getPackagesForUid(info.uid);
					
				if(subPackageName != null)
				{
					for(int PackagePtr = 0; PackagePtr < subPackageName.length; PackagePtr++)
					{
						if (subPackageName[PackagePtr] == null)
							continue;
						
						try {  
							appPackageInfo = mPackageManager.getPackageInfo(subPackageName[PackagePtr], 0);
							PackagePtr = subPackageName.length;
						} catch (NameNotFoundException e) {}						
					}
				}
			}
			//페키지 이름으로는 식별이 힘들기 때문에 페키지이름을 어플리케이션 라벨 이름으로 변환 시켜준다.
			LabelName = appPackageInfo.applicationInfo.loadLabel(mPackageManager).toString();
			Icon = resizeImage(appPackageInfo.applicationInfo.loadIcon(mPackageManager));
			
		/*	if (convertView == null) {
			*/	
					sv = new ProcessDetailView(mContext,Icon, info.pid, info.uid, LabelName,position, 
									TrafficStats.getUidRxBytes(info.uid),
									TrafficStats.getUidTxBytes(info.uid));
			
					/*	} else {
				sv = (ProcessDetailView) convertView;
					sv.setView(Icon,
									info.pid,info.uid, 
									LabelName, position);
			
			
			}*/

			return sv;
		}

		private Context mContext;
		private List<ActivityManager.RunningAppProcessInfo> mList;
		private String PackageName = null;
		private String LabelName = null;
		public Drawable Icon;
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
		private TextView AppRxField;
		private TextView AppTxField;

		private boolean Expanded = false;

		public ProcessDetailView(Context context,Drawable Icon, int PID ,int UID,
				String Name,int position, long appRx, long appTx) {
			super(context);
			this.setColumnStretchable(2, true);

			// this.setOrientation(VERTICAL);

			PIDField = new TextView(context);// 프로세스 ID를 표시한다.
			IconField = new ImageView(context);// 어플리케이션 아이콘을 표시 한다.
			NameField = new TextView(context);// 어플리케이션의 이름을 표시 한다.
			AppRxField = new TextView(context);// App별 Rx 정보 이다.
			AppTxField = new TextView(context);// App별 Tx 정보 이다.
			
			PIDField.setText("" + PID+"("+UID+")"); // PID 정보를 삽입한다.

			IconField.setImageDrawable(Icon);
			IconField.setPadding(8, 3, 3, 3);

			NameField.setText(Name);

			PIDField.setGravity(Gravity.LEFT);
			PIDField.setPadding(3, 3, 3, 3);

			// 스크린 사이즈에 따른 처리를 해주는 것이다.
			if (CompareFunc.getScreenSize() == 2)
				PIDField.setWidth(90);
			else if (CompareFunc.getScreenSize() == 0)
				PIDField.setWidth(35);
			else
				PIDField.setWidth(55);

			NameField.setPadding(3, 3, 3, 3);
			NameField.setGravity(Gravity.LEFT);
			NameField.setWidth(getWidth() - IconField.getWidth());
	

			/*
			 * 최종적으로 TableRow를 셍상히야 값을 달아준다. PID Icon(application) Name
			 * Value(load값) DetailField(Icon)
			 */
			TitleRow = new TableRow(context);
			TitleRow.addView(PIDField);
			TitleRow.addView(IconField);
			TitleRow.addView(NameField);
			addView(TitleRow);

			// 어플리케이션별 Rx 정보이다.
			AppRxField.setText("\tRx: "+MemoryFormat.format(appRx)+"Byte");
			addView(AppRxField);
			// 어플리케이션별 Tx 정보이다.
			AppTxField.setText("\tTx: "+MemoryFormat.format(appTx)+"Byte");
			addView(AppTxField);
		
			// 포지션마다 백그라운드 색을 다르게 한다.
			if (position % 2 == 0)
				setBackgroundColor(0x80444444);
			else
				setBackgroundColor(0x80000000);

		}
		public void setContext(String AppInfo) {
			AppRxField.setText(AppInfo);
		}

		public void setView(Drawable Icon,int PID, int UID, String Name, int position) {

			IconField.setImageDrawable(Icon);
			PIDField.setText("" + PID+"("+UID+")");
			NameField.setText(Name);

			if (position % 2 == 0)
				setBackgroundColor(0x80444444);
			else
				setBackgroundColor(0x80000000);
		}

		public void setView(int PID, int position) {

			IconField.setImageDrawable(null);
			// IconField.setVisibility(View.GONE);
			// DetailField.setVisibility(View.INVISIBLE);
			PIDField.setText("" + PID);
			NameField.setText("Loading");

			if (position % 2 == 0)
				setBackgroundColor(0x80444444);
			else
				setBackgroundColor(0x80000000);
		}

		/**
		 * Convenience method to expand or hide the dialogue
		 */
		public void setExpanded(boolean expanded) {
			AppRxField.setVisibility(expanded ? VISIBLE : GONE);
		}

		public void setMultiSelected(boolean selected) {
			if (selected)
				setBackgroundColor(0x803CC8FF);
		}

		public boolean onTouchEvent(MotionEvent event) {
			if (event.getX() > getWidth() / 3 * 2)
				Expanded = true;
			else if (event.getX() <= getWidth() / 3 * 2)
				Expanded = false;

			return super.onTouchEvent(event);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub

	}
	//applciation icon을 resize해줘야 같은 크기로 List에 출력 되어 지게된다.
	private Drawable resizeImage(Drawable Icon) {

		if(CompareFunc.getScreenSize() == 2)
		{
			Bitmap BitmapOrg = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888); 
			Canvas BitmapCanvas = new Canvas(BitmapOrg);
			Icon.setBounds(0, 0, 60, 60);
			Icon.draw(BitmapCanvas); 
	        return new BitmapDrawable(BitmapOrg);
		}
		else if (CompareFunc.getScreenSize() == 0)
		{
			Bitmap BitmapOrg = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888); 
			Canvas BitmapCanvas = new Canvas(BitmapOrg);
			Icon.setBounds(0, 0, 10, 10);
			Icon.draw(BitmapCanvas); 
	        return new BitmapDrawable(BitmapOrg);
		}
		else
		{
			Bitmap BitmapOrg = Bitmap.createBitmap(22, 22, Bitmap.Config.ARGB_8888); 
			Canvas BitmapCanvas = new Canvas(BitmapOrg);
			Icon.setBounds(0, 0, 22, 22);
			Icon.draw(BitmapCanvas); 
	        return new BitmapDrawable(BitmapOrg);
		}
    }
}
