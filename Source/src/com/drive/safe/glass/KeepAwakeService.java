/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.drive.safe.glass.eye.SleepDetector;
import com.drive.safe.glass.view.LiveCardDrawer;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

public class KeepAwakeService extends Service implements SleepDetector.SleepListener {
	private static final String TAG = "KeepAwakeService";

	private static final String CARD_TAG = "DriveSafe4Glass_LiveCard";

	/**
	 * A binder that allows other parts of the application to the speech
	 * capability
	 */
	public class KeepAwakeBinder extends Binder {
		/**
		 * Get directions to a rest area
		 */
		public void getDirectionsToRestArea() {
			Intent directionsIntent = new Intent(Intent.ACTION_VIEW);
			directionsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			directionsIntent.setData(Uri.parse("google.navigation:q=rest+area"));
			getApplication().startActivity(directionsIntent);

			// Stop KeepAwakeService now that the user is in navigation
			stopKeepAwakeService();
		}
	}

	private final KeepAwakeBinder mBinder = new KeepAwakeBinder();

	private Context mContext;

	private TextToSpeech mTTS;

	private SleepDetector mSleepDetector;

	private LiveCard mLiveCard;
	private TimelineManager mTimeline;

	private LiveCardDrawer mLiveCardDrawer;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		mTimeline = TimelineManager.from(mContext);
		mLiveCardDrawer = new LiveCardDrawer(mContext);

		mTTS = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Nothing to do here
			}
		});

		mSleepDetector = new SleepDetector(mContext, this);
		mSleepDetector.setupReceiver();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mLiveCard == null) {
			// Create the live card and publish it

			mLiveCard = mTimeline.createLiveCard(CARD_TAG);

			// Setup the drawing of the card
			mLiveCard.setDirectRenderingEnabled(true);
			mLiveCard.getSurfaceHolder().addCallback(mLiveCardDrawer);

			// Setup what to do on tapping of the card
			Intent menuActivityIntent = new Intent(mContext, KeepAwakeMenuActivity.class);
			mLiveCard.setAction(PendingIntent.getActivity(mContext, 0, menuActivityIntent, 0));

			mLiveCard.publish(PublishMode.REVEAL);
		} else {
			// Move to the live card that currently exists
			// There might be a better way to do this
			mLiveCard.unpublish();
			mLiveCard.publish(PublishMode.REVEAL);
		}

		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {
			mLiveCard.getSurfaceHolder().removeCallback(mLiveCardDrawer);
			mLiveCard.unpublish();
			mLiveCard = null;
		}

		mSleepDetector.removeReceiver();
		
		super.onDestroy();
	}

	/**
	 * Stops the KeepAwakeService (for all intensive purposes, stops the entire
	 * application)
	 */
	private void stopKeepAwakeService() {
		mSleepDetector.removeReceiver();
		stopService(new Intent(mContext, KeepAwakeService.class));
	}

	@Override
	public void onUserFallingAsleep() {
		Log.i(TAG, "User is falling asleep");
		
		mTTS.speak(getString(R.string.speech_wake_up), TextToSpeech.QUEUE_FLUSH, null);
		startActivity(new Intent(mContext, KeepAwakeMenuActivity.class));
	}

}
