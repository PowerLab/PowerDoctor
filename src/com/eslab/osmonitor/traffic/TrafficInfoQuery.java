package com.eslab.osmonitor.traffic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.eolwral.osmonitor.CompareFunc;
import com.eolwral.osmonitor.JNIInterface;
import com.eolwral.osmonitor.R;


public class TrafficInfoQuery extends Thread
{
	//JNI를 통해서 리눅스 커널을 읽어오는 함수이다.
	private static JNIInterface JNILibrary = JNIInterface.getInstance();
	
	private static TrafficInfoQuery singletone = null; //자기 자신의 Type으로 가지고 있는다.
	private static PackageManager AppInfo = null; // 설치된 App의 목록과 이름 아이콘을 얻어오는 클래스이다.
	private static Resources  ResInfo = null;
	
	//Main ListView의 onCreate에서 이 메서드를 실행 시킴으로서 Thread를 시작 시킨다.
	public static TrafficInfoQuery getInstance(Context context)
	{
		if(singletone == null)
		{
			singletone = new TrafficInfoQuery(); // 프로세스 정보를 저장할 객체이다.
            AppInfo = context.getPackageManager(); // 페키지 매니저를 얻어온다.
            ResInfo = context.getResources(); // 아이콘 출력을 위한 리소스를 얻어 온다.
            singletone.start();//getInstance를 하는 순간 Thrad를 시작 시킨다. 즉 이것이 외부에서 이 Thread를 시작시키는 방법인것 같다.
		}
		
		return singletone; // null 아니라면 기존에 생성한 것을 사용 한다.
	}
	
	//각 프로세스별로 표시해줄 정보를 가지고 있는 클래스이다.
	class ProcessInstance
	{
		public String Name;
		public Drawable Icon;
		public String Package;
		public int CellTraffic; //내가 추가한것
		public int wifiTraffic;	//내가 추가한것
	}
	
	/*
	 * ListView에 채워진 내용을 업데이트 하기위해서 3가지 종류의 HashMap을 가지고 있다.
	 */
    private final HashMap<String, Boolean> CacheExpaned = new HashMap<String, Boolean>(); //Detail버튼을 눌렀는지 여부
    private final HashMap<String, Boolean> CacheSelected = new HashMap<String, Boolean>(); // 선택이 되었는지 여부
    // 라인당 프로세스 정보들의 집함, 따라서 특이하게 이름당 processInstance의 값을 가지고 있는 형태를 유지하고 있다.
    private final HashMap<String, ProcessInstance> ProcessCache = new HashMap<String, ProcessInstance>(); 
	
	public void doCacheInfo(int position)
	{
		ProcessInstance CacheInstance = ProcessCache.get(JNILibrary.GetProcessName(position));
		/*
		 * null 아니라면 리턴하는 것이다. 즉 쉽게 말해서 ListView의 데이터는 Native 자료구조에서 읽어온 데이터로 뿌린것이다.
		 * 그 데이터를 뿌리기전에 항시 캐쉬정보로 이곳에 넣어주는것이다. 이미 들어있다면 또다시 작업을 반복해 줄 필요가없는 것이다.
		 * 이렇게해서 자동적으로 ListView에 의해서 뿌려주지 않아도 정해진 시간마다 갱신되어지는 값을 Thread에 의해서 주기적으로
		 * 갱신되어 ListView에 출력해 줄 수가 있다.
		 */
		if(CacheInstance != null)
			return;
		
		try {
			/*
			 * 세마포어를 하는 이유는 ListView즉 UI Thread에 의해서 접근이 되어 지기도 하지만.
			 * 자체적인 쿼리 Thread에 의해서도 접근되어진다. 따라서 크리티컬 섹션 영역으로 지정해 주었다.
			 */
			QueryQueueLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//현재 포지션의 프로세스 정보를 삽입하여 준다. (프로세스 이름, 프로세스 오너, 프로세스 UID )
		QueryQueue.add(new WaitCache(JNILibrary.GetProcessName(position),
				JNILibrary.GetProcessOwner(position), JNILibrary.GetProcessUID(position)));
		QueryQueueLock.release();

		//중복된 작업을 막기위해서 HashMap에다가 현재 작업한 프로세스를 저장해 준다.
		CacheInstance = new ProcessInstance();
		CacheInstance.Name = JNILibrary.GetProcessName(position);
		ProcessCache.put(JNILibrary.GetProcessName(position),
					      CacheInstance);
		
		return;
	}

	private class WaitCache
	{
		private final String ItemName;
		private final String ItemOwner;
		private final int ItemUID;
		public WaitCache(String Name, String Owner, int UID)
		{
			ItemName = Name;
			ItemOwner = Owner;
			ItemUID = UID;
		}
		
		public String getName()
		{
			return ItemName;
		}

		public String getOwner()
		{
			return ItemOwner;
		}
		
		public int getUID()
		{
			return ItemUID;
		}
	}
	//링크드 리스트 타입으로 캐쉬를 저장하는 객체를 생성 한다.
    private static LinkedList<WaitCache> QueryQueue = new LinkedList<WaitCache>();
    //세마포어를 설정하기 위해서 설정 하였다.
	private final Semaphore QueryQueueLock = new Semaphore(1, true);
    
	
	@Override 
	public void run()
	{
		//이부분이 반복되어 수행 되어 진다.
		while(true)
		{	//true면 0.5sec delay를 줘서 기다리고, 아니라면 그냥 그대로 연산을 처리 한다.
			//Chase에 내용이 아무것도 없다면 지체없이 데이터를 읽어 온다.
			if(!getCacheInfo())
			{
				try {
					/*
					 *  데이터가 없다면 아직 리스트뷰가 최워지기 전이므로 0.5초간 기다린다. 
					 *  그렇다면 그동안 리스트뷰가 모두 갱신 되어서 hashMap안에 유효한 데이터가 모두 들어 있을 것이다.
					 */
					sleep(500); 
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * 여기서 해주는 주된 작업은  proc/stat를 통해서 읽어온 프로세스 정보들을
	 * 가공하여 보여준다. 좀더 명확하게 (e.g 아이콘도 표시하고, 이름도 페키지 이름이 아니라, 어플리케이션이름으로 바꾼다. 
	 */
	public boolean getCacheInfo()
	{
		//링크드리스트의 큐가 Empty라면 false를 리턴한다.
		if(QueryQueue.isEmpty())
			return false;
		
		try {
			//큐에는 하나의  Thread만이 접근해야 하기 때문에 세마포어를 걸어 놓는다.
			QueryQueueLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//최근에 방문한 원소를 삭제 한다. 하여 돌려준다.
		WaitCache SearchObj = QueryQueue.remove();
		
		//세마포어를 해제 한다. 
		QueryQueueLock.release();
		
		PackageInfo appPackageInfo = null;
		String PackageName = null;
		//이름에 ':'문자를 포함하고 있다면 :전까지의 내용을 PackageName으로 정의한다.
		if(SearchObj.getName().contains(":")){
			PackageName = SearchObj.getName().substring(0,SearchObj.getName().indexOf(":"));
		}
		else
			PackageName = SearchObj.getName();
		
		// for system user 시스템 기본 App을 목록에서 제거한다. 
		if(SearchObj.getOwner().contains("system") && SearchObj.getName().contains("system"))
			PackageName = "android";
		
		try {  
			//해당 PackageName에 대한 정보를 PackageManager로 부터 받아 온다. 
			appPackageInfo = AppInfo.getPackageInfo(PackageName, 0);
		} catch (NameNotFoundException e) {}
		
		//PackageName에 대한 정보가 없을경우 SearchObj에서 UID를 얻어온다.
		if(appPackageInfo == null && SearchObj.getUID() >0)
		{
			//UID를 통해서 Package Name을 확보 한다.
			String[] subPackageName = AppInfo.getPackagesForUid(SearchObj.getUID());
				
			if(subPackageName != null)
			{
				for(int PackagePtr = 0; PackagePtr < subPackageName.length; PackagePtr++)
				{
					if (subPackageName[PackagePtr] == null)
						continue;
					
					try {  
						appPackageInfo = AppInfo.getPackageInfo(subPackageName[PackagePtr], 0);
						PackagePtr = subPackageName.length;
					} catch (NameNotFoundException e) {}						
				}
			}
		}
		
		ProcessInstance CacheInstance = new ProcessInstance();
		
		//획득한 packageName을 기록 한다.
		CacheInstance.Package = PackageName;
	
		if(appPackageInfo != null)
		{  
			CacheInstance.Name = appPackageInfo.applicationInfo.loadLabel(AppInfo).toString();
			CacheInstance.Icon = resizeImage(appPackageInfo.applicationInfo.loadIcon(AppInfo));
		}
		else if(PackageName.equals("System"))
		{ 
			CacheInstance.Name = PackageName;
			CacheInstance.Icon = resizeImage(ResInfo.getDrawable(R.drawable.system));
		}
		else
			CacheInstance.Name = PackageName;
		
		ProcessCache.put(SearchObj.getName(), CacheInstance);
		
		return true;
	}
	
	public Boolean getExpaned(int position)
	{
		Boolean Flag = CacheExpaned.get(JNILibrary.GetProcessPID(position)+"");
		if(Flag == null)
			Flag = false;
		
		return Flag;
	}
	
	public void setExpaned(int position, Boolean Flag)
	{
//		ProcessInstance CacheInstance = ProcessCache.get(JNILibrary.GetProcessName(position));
	//	CacheInstance.Expaned = Flag;
		//ProcessCache.put(JNILibrary.GetProcessName(position), CacheInstance);
		CacheExpaned.put(JNILibrary.GetProcessPID(position)+"", Flag);
		return;
	}
	
	public Boolean getSelected(int position)
	{
		Boolean Flag = CacheSelected.get(JNILibrary.GetProcessPID(position)+"");
		if(Flag == null)
			Flag = false;
		
		return Flag;
	}
	
	public void setSelected(int position, Boolean Flag)
	{
		CacheSelected.put(JNILibrary.GetProcessPID(position)+"", Flag);
		return;
	}
	
	public ArrayList<String> getSelected()
	{
		ArrayList<String> selectPID = new ArrayList<String>();
        Iterator<String> It = CacheSelected.keySet().iterator();
        while (It.hasNext())
        {
        	String cacheKey = (String) It.next();
        	if(CacheSelected.get(cacheKey) == true)
        		selectPID.add(cacheKey);
        }
        
        return selectPID;
	}
	
	public void clearSelected()
	{
		CacheSelected.clear();
		return;
	}

	
	public String getPackageName(int position) 
	{
		return ProcessCache.get(JNILibrary.GetProcessName(position)).Name;
	}

	public String getPacakge(int position)
	{
		return ProcessCache.get(JNILibrary.GetProcessName(position)).Package;
	}
	
	public int getProcessPID(int position)
	{
		return JNILibrary.GetProcessPID(position);
	}
	
	public String getProcessThreads(int position)
	{
		return JNILibrary.GetProcessThreads(position)+"";
	}

	public String getProcessLoad(int position)
	{
		return JNILibrary.GetProcessLoad(position)+"%";
	}

	public String getProcessMem(int position)
	{
		if(JNILibrary.GetProcessRSS(position) > 1024) 
			return (JNILibrary.GetProcessRSS(position)/1024)+"M";
		return JNILibrary.GetProcessRSS(position)+"K";
	}
	
	
	private StringBuilder appbuf = new StringBuilder();
	//뭔가 받은 포지션 만큼 작업을 해주는 것 같다.
	//position 값으로 데이터의 위치를 가져오는것 같다. 
	public String getAppInfo(int position) {
		appbuf.setLength(0);
		
		if(JNILibrary.GetProcessRSS(position) > 1024) {
			appbuf.append("\tProcess: ")
				  .append(JNILibrary.GetProcessName(position))
			      .append("\n\tMemory: ")
				  .append(JNILibrary.GetProcessRSS(position)/1024)
				  .append("M\t  Thread: ")
				  .append(JNILibrary.GetProcessThreads(position))
				  .append("\t  Load: ")
				  .append(JNILibrary.GetProcessLoad(position))
				  .append("%\n\tSTime: ")
				  .append(JNILibrary.GetProcessSTime(position))
				  .append("\t  UTime: ")
				  .append(JNILibrary.GetProcessUTime(position))
				  .append("\n\tUser: ")
				  .append(JNILibrary.GetProcessOwner(position))
				  .append("\t  UID: ")
				  .append(JNILibrary.GetProcessUID(position))
				  .append("\t  Status: ");
		}
		else {
			appbuf.append("\tProcess: ")
				  .append(JNILibrary.GetProcessName(position))
				  .append("\n\tMemory: ")
				  .append(JNILibrary.GetProcessRSS(position))
				  .append("K\t  Threads: ")
				  .append(JNILibrary.GetProcessThreads(position))
				  .append("\t  Load: ")
				  .append(JNILibrary.GetProcessLoad(position))
				  .append("%\n\tSTime: ")
				  .append(JNILibrary.GetProcessSTime(position))
				  .append("\t  UTime: ")
				  .append(JNILibrary.GetProcessUTime(position))
				  .append("\n\tUser: ")
				  .append(JNILibrary.GetProcessOwner(position))
				  .append("\t  UID: ")
				  .append(JNILibrary.GetProcessUID(position))
				  .append("\t  Status: ");		  
		}
		
		String Status = JNILibrary.GetProcessStatus(position).trim();
		if(Status.compareTo("Z") == 0)
			appbuf.append("Zombie");
		else if(Status.compareTo("S") == 0)
			appbuf.append("Sleep");
		else if(Status.compareTo("R") == 0)
			appbuf.append("Running");
		else if(Status.compareTo("D") == 0)
			appbuf.append("Wait IO");
		else if(Status.compareTo("T") == 0)
			appbuf.append("Stop");
		else 
			appbuf.append("Unknown");

		return appbuf.toString();
	}
	
	public Drawable getAppIcon(int position) 
	{
		return ProcessCache.get(JNILibrary.GetProcessName(position)).Icon;
	}
	
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