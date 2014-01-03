package com.drive.safe.glass.eye;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import com.drive.safe.glass.eye.EyeEventReceiver.EyeEventListener;
import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;

public class SleepDetector {
	private static final String TAG = "SleepDetector";
	
	/**
	 * An interface for a listener to receive
	 * sleep events
	 */
	public static interface SleepListener {
		public void onUserFallingAsleep();
	}

	/**
	 * If mSleepLevel gets above SLEEP_THRESHOLD, then consider the user is
	 * falling asleep
	 */
	private static final float SLEEP_THRESHOLD = 8f;

	/**
	 * When the user performs the wink gesture, add MODIFIER_WINK to mSleepLevel
	 */
	private static final float MODIFIER_WINK = 7f;

	/**
	 * When the user performs the wink gesture, add MODIFIER_DOUBLE_BLINK to
	 * mSleepLevel
	 */
	private static final float MODIFIER_DOUBLE_BLINK = 3f;

	/**
	 * Every millisecond, mSleepLevel is reduced by DEGRADATION_PER_MILLISECOND
	 */
	private static final float DEGRADATION_PER_MILLISECOND = 0.001f;

	
	private Context mContext;
	
	private EyeGestureManager mEyeGestureManager;
	private EyeEventReceiver mEyeEventReceiver;
	private EyeEventListener mEyeEventListener;
	
	private SleepListener mSleepListener;

	private float mSleepLevel = 0f;

	private long mLastEyeEvent = 0;
	
	public SleepDetector(Context context, SleepListener listener){
		this(context);
		mSleepListener = listener;
	}

	public SleepDetector(Context context) {
		mContext = context;

		mEyeGestureManager = EyeGestureManager.from(mContext);
		
		mEyeEventListener = new EyeEventListener() {
			@Override
			public void onWink() {
				mSleepLevel += MODIFIER_WINK;
				degradeSleepLevel();
				checkSleepLevel();
			}

			@Override
			public void onDoubleBlink() {
				mSleepLevel += MODIFIER_DOUBLE_BLINK;
				degradeSleepLevel();
				checkSleepLevel();
			}
		};

		mEyeEventReceiver = new EyeEventReceiver(mEyeEventListener);
	}

	public void setupReceiver() {
		mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
		mEyeGestureManager.stopDetector(EyeGesture.WINK);

        mEyeGestureManager.enableDetectorPersistently(EyeGesture.DOUBLE_BLINK, true);
        mEyeGestureManager.enableDetectorPersistently(EyeGesture.WINK, true);

        IntentFilter eyeFilter = new IntentFilter("com.google.glass.action.EYE_GESTURE");
        eyeFilter.setPriority(3000);

        mContext.registerReceiver(mEyeEventReceiver, eyeFilter);
	}
	
	public void removeReceiver() {
		mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
		mEyeGestureManager.stopDetector(EyeGesture.WINK);

        mContext.unregisterReceiver(mEyeEventReceiver);
	}
	
	public void setSleepListener(SleepListener listener){
		mSleepListener = listener;
	}

	private void degradeSleepLevel() {
		// Don't apply the degradation to the first eye event
		if (mLastEyeEvent != 0) {
			// mSleepLevel has to be at least 0
			mSleepLevel = Math.max(0, mSleepLevel - ((System.currentTimeMillis() - mLastEyeEvent) *  DEGRADATION_PER_MILLISECOND));
		}

		mLastEyeEvent = System.currentTimeMillis();
	}

	private void checkSleepLevel() {
		if (mSleepLevel >= SLEEP_THRESHOLD) {
			// The user is falling asleep
			Log.i(TAG, "The user is falling asleep");
			
			if(mSleepListener != null){
				mSleepListener.onUserFallingAsleep();
			}
		}
	}
	

}
