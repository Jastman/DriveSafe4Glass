/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.drive.safe.glass.view.LiveCardDrawer;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

public class KeepAwakeService extends Service {
	private static final String TAG = "KeepAwakeService";
	
	private static final String CARD_TAG = "DriveSafe4Glass_LiveCard";

	private Context mContext;

	private LiveCard mLiveCard;
	private TimelineManager mTimeline;
	
	private LiveCardDrawer mLiveCardDrawer;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		
		mTimeline = TimelineManager.from(mContext);
		mLiveCardDrawer = new LiveCardDrawer(mContext);
	}	

	@Override
	public IBinder onBind(Intent intent){
		return null;
	}	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		if(mLiveCard == null){
			//Create the live card and publish it
			
			mLiveCard = mTimeline.createLiveCard(CARD_TAG);
			
			// Setup the drawing of the card
			mLiveCard.setDirectRenderingEnabled(true);
			mLiveCard.getSurfaceHolder().addCallback(mLiveCardDrawer);
			
			// Setup what to do on tapping of the card
			Intent menuActivityIntent = new Intent(mContext, KeepAwakeMenuActivity.class);
			mLiveCard.setAction(PendingIntent.getActivity(mContext, 0, menuActivityIntent, 0));
			
			mLiveCard.publish(PublishMode.REVEAL);
			
		}else{
			//Move to the live card that currently exists
			
			mLiveCard.publish(PublishMode.REVEAL);
		}

		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		if(mLiveCard != null && mLiveCard.isPublished()){
			mLiveCard.getSurfaceHolder().removeCallback(mLiveCardDrawer);
			mLiveCard.unpublish();
			mLiveCard = null;
		}
		
		super.onDestroy();
	}

}
