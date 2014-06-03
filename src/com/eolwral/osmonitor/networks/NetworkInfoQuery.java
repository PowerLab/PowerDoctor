package com.eolwral.osmonitor.networks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class NetworkInfoQuery extends Thread
{

	private static NetworkInfoQuery singletone = null;

	public static NetworkInfoQuery getInstance()
	{
		if(singletone == null)
		{
			singletone = new NetworkInfoQuery();
            singletone.start();
		}
		return singletone;
	}    
    
    private final HashMap<String, String> CacheDNS = new HashMap<String, String>();
	private static LinkedList<String> QueryQueue = new LinkedList<String>();
	private final Semaphore QueryQueueLock = new Semaphore(1, true);
	
	@Override 
	public void run()
	{
		while(true)
		{
			if(!getCacheInfo())
			{
				try {
					sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void doCacheInfo(String IP)
	{
		if(CacheDNS.get(IP) != null)
			return;
		
		try {
			QueryQueueLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		QueryQueue.add(IP);
		QueryQueueLock.release();	
	}
	
	public boolean getCacheInfo()
	{
		if(QueryQueue.isEmpty())
			return false;
		
		try {
			QueryQueueLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		String SearchObj = QueryQueue.remove();
		
		QueryQueueLock.release();

		try
		{
			InetAddress DNSAddr = InetAddress.getByName(SearchObj);
			CacheDNS.put(SearchObj, DNSAddr.getHostName());
		} catch (UnknownHostException e) {
			CacheDNS.put(SearchObj, SearchObj);
		}
		
		
		return true;
	}
	
	public String GetDNS(String IP)
	{
		if(CacheDNS.get(IP) != null)
			return  CacheDNS.get(IP);
		return IP;
	}

}
