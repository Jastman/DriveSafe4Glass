/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.drive.safe.glass.alert.AlertManager;
import com.drive.safe.glass.analytics.APIKeys;
import com.drive.safe.glass.preference.PreferenceConstants;
import com.drive.safe.glass.sleep.SleepDetector;
import com.drive.safe.glass.view.LiveCardDrawer;
import com.flurry.android.FlurryAgent;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

public class KeepAwakeService extends Service implements SleepDetector.SleepListener {
	private static final String TAG = "KeepAwakeService";

	private static final String CARD_TAG = "DriveSafe4Glass_LiveCard";

	/**
	 * About how long it takes to speak the message in milliseconds
	 */
	private static final long SPEECH_TIME = 12000;

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

	private MediaPlayer mPlayer;
	private TextToSpeech mTTS;

	private SleepDetector mSleepDetector;

	private LiveCard mLiveCard;
	private TimelineManager mTimeline;

	private LiveCardDrawer mLiveCardDrawer;

	private SharedPreferences mPrefs;

	private AlertManager mAlertManager;

	/**
	 * Whether or not to speak the alert after playing the beep sound
	 */
	private boolean mSpeak = true;

	/**
	 * Whether or not analytics are allowed
	 */
	private boolean mAnalyticsEnabled = true;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		mTimeline = TimelineManager.from(mContext);
		mLiveCardDrawer = new LiveCardDrawer(mContext);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		mAnalyticsEnabled = mPrefs.getBoolean(PreferenceConstants.ALLOW_ANALYTICS, PreferenceConstants.ALLOW_ANALYTICS_DEFAULT);

		mAlertManager = new AlertManager(mPrefs);

		mTTS = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Nothing to do here
			}
		});

		mPlayer = MediaPlayer.create(this, R.raw.beeps);
		mPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (mSpeak) {
					mTTS.speak(getString(R.string.speech_wake_up), TextToSpeech.QUEUE_FLUSH, null);
				}
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

		// Analytics
		if (mAnalyticsEnabled) {
			FlurryAgent.onStartSession(this, APIKeys.FLURRY_API_KEY);
			FlurryAgent.logEvent("Started");
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

		mTTS.shutdown();

		// Analytics
		if (mAnalyticsEnabled) {
			FlurryAgent.onEndSession(this);
		}

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

		int alertLevel = mAlertManager.getAlertLevel();

		if (alertLevel == AlertManager.NO_ALERT) {
			return;
		} else if (alertLevel == AlertManager.BEEP_ONLY) {
			// Only play the beep sound,
			// so set mSpeak to false
			mSpeak = false;
			mPlayer.start();
		} else if (alertLevel == AlertManager.SHOW_ALERT) {
			// Play beep sound, speak alert
			// and show alert
			mSpeak = true;
			mPlayer.start();

			Intent alertIntent = new Intent(mContext, KeepAwakeAlertActivity.class);
			alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			alertIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			getApplication().startActivity(alertIntent);
		} else {
			Log.e(TAG, "Unknown alertLevel: " + alertLevel);
		}
	}

}
