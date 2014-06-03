package com.eslab.osmonitor.utility;

import java.util.ArrayList;

import android.content.Context;
import android.widget.Toast;

/*
 * 표준 편차를 게싼해주는 유틸리티 클래스 이다. 
 */
public class StandardDeviation {

	public final static int DOUBLE = 0;  
	private ArrayList<Double> mItemList;
	
	public StandardDeviation(int mode){
		if(mode == DOUBLE){
			mItemList = new ArrayList<Double>();
		}
		else{
			
		}
	}
	public boolean add(double arItem){
		mItemList.add(arItem);
		return true;
	}
	public double EvaluateSD(){
		double sd = 0.0;
		double sum = 0.0;
		double pow2sum = 0.0;
		double avg = 0.0;
		double pow2avg = 0.0;
		for(int i = 0; i<mItemList.size(); i++){
			sum += mItemList.get(i);	// 그냥합
			pow2sum += Math.pow(mItemList.get(i),2); // 제곱의 합
		}
		
		//평균 계산
		avg = sum/mItemList.size();
		pow2avg = pow2sum/mItemList.size();
		
		//표준편차 계산. 식 : 제곱평균 - 평균의 제곱 의 루트
		sd = Math.sqrt(pow2avg - Math.pow(avg, 2));
		
		return sd;
	}
	//저장된 List를 Toast message를 이용해서 출력하는 메서드이다. 
	public String printItemList(){
		String str ="";
		for(int i = 0; i<mItemList.size(); i++){
			str = str +" "+ mItemList.get(i);
		}
		return str;
	}
	public boolean clear(){
		mItemList.clear();
		return true;
	}
}
