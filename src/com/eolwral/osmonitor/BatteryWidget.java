package com.eolwral.osmonitor;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

//위젯과 관련된 소스 코드이다.

public class BatteryWidget extends AppWidgetProvider {
	
	//배터리의 상태는 BR을 등록한 후 방송을 수신해야 조사할 수 있다.
	//게다가 BR은 방송을 처리하는 동안에만 존재하는 임시 객체이므로 
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	
	}
	public void onDeleted(Context context) {
		
    }
	
}
