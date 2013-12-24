package com.drive.safe.glass;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

public class KeepAwakeService extends Service {
	private static final String TAG = "KeepAwakeService";
	
	private static final String CARD_TAG = "DriveSafe4Glass_LiveCard";

	private Context mContext;

	private LiveCard mLiveCard;
	private TimelineManager mTimeline;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		
		mTimeline = TimelineManager.from(mContext);

	
	}	

	@Override
	public IBinder onBind(Intent intent){
		
		return null;
	}	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		if(mLiveCard == null){
			//TODO: Have to create the live card
			mLiveCard = mTimeline.createLiveCard(CARD_TAG);


		}else{
			//TODO: Move to the live card that currently exists

		}


		return Service.START_STICKY;
	}	

}
