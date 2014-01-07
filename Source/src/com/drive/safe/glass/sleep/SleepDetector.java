/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.sleep;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import com.drive.safe.glass.eye.EyeEventReceiver;
import com.drive.safe.glass.eye.EyeEventReceiver.EyeEventListener;
import com.drive.safe.glass.head.HeadEventReceiver;
import com.drive.safe.glass.head.HeadEventReceiver.HeadEventListener;
import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;


public class SleepDetector {
	private static final String TAG = "SleepDetector";

	/**
	 * An interface for a listener to receive sleep events
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
	private static final float MODIFIER_DOUBLE_BLINK = 5.5f;

	/**
	 * The acceleration on Glass's Z axis needs to be above this
	 * amount (in meters per second) to add to mSleepLevel
	 */
	private static final float HEAD_THRESHOLD = 2f;

	/**
	 * When adding head events to the sleep level, multiply
	 * the acceleration on the Z axis (minus HEAD_THRESHOLD) by this amount.
	 */
	private static final float MODIFIER_HEAD_SCALE_PER_MILLISECOND = 0.003f;

	/**
	 * Every millisecond, mSleepLevel is reduced by DEGRADATION_PER_MILLISECOND
	 */
	private static final float DEGRADATION_PER_MILLISECOND = 0.0005f;

	private Context mContext;

	// These are used for detecting blinks and winks
	private EyeGestureManager mEyeGestureManager;
	private EyeEventReceiver mEyeEventReceiver;
	private EyeEventListener mEyeEventListener;

	// Used for detecting head events
	private HeadEventReceiver mHeadEventReceiver;
	private HeadEventListener mHeadEventListener;

	private SleepListener mSleepListener;

	private float mSleepLevel = 0f;

	private long mLastEyeEvent = 0;
	private long mLastHeadEvent = 0;
	private long mLastEvent = 0;

	public SleepDetector(Context context, SleepListener listener) {
		this(context);
		mSleepListener = listener;
	}

	public SleepDetector(Context context) {
		mContext = context;

		// Setup for the eye gestures
		mEyeGestureManager = EyeGestureManager.from(mContext);

		mEyeEventListener = new EyeEventListener() {
			@Override
			public void onWink() {
				mSleepLevel += MODIFIER_WINK;
				degradeSleepLevel();
				checkSleepLevel();

				mLastEyeEvent = System.currentTimeMillis();
			}

			@Override
			public void onDoubleBlink() {
				mSleepLevel += MODIFIER_DOUBLE_BLINK;
				degradeSleepLevel();
				checkSleepLevel();

				mLastEyeEvent = System.currentTimeMillis();
			}
		};

		mEyeEventReceiver = new EyeEventReceiver(mEyeEventListener);

		// Setup for the head detector
		mHeadEventListener = new HeadEventListener(){
			public void onHeadEvent(float zMagnitude){
				addHeadEvent(zMagnitude);
				degradeSleepLevel();
				checkSleepLevel();

				mLastHeadEvent = System.currentTimeMillis();
			}
		};

		mHeadEventReceiver = new HeadEventReceiver(mContext, mHeadEventListener);
	}

	public void setupReceiver() {
		// Eye Events
		mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
		mEyeGestureManager.stopDetector(EyeGesture.WINK);

		mEyeGestureManager.enableDetectorPersistently(EyeGesture.DOUBLE_BLINK,
				true);
		mEyeGestureManager.enableDetectorPersistently(EyeGesture.WINK, true);

		IntentFilter eyeFilter = new IntentFilter(
				"com.google.glass.action.EYE_GESTURE");
		eyeFilter.setPriority(3000);

		mContext.registerReceiver(mEyeEventReceiver, eyeFilter);

		// Head Events
		mHeadEventReceiver.startListening();
	}

	public void removeReceiver() {
		// Eye Events
		mEyeGestureManager.stopDetector(EyeGesture.DOUBLE_BLINK);
		mEyeGestureManager.stopDetector(EyeGesture.WINK);

		mContext.unregisterReceiver(mEyeEventReceiver);

		// Head Events
		mHeadEventReceiver.stopListening();
	}

	public void setSleepListener(SleepListener listener) {
		mSleepListener = listener;
	}

	private void addHeadEvent(float zMagnitude){
		if(mLastHeadEvent != 0){
			float absMagnitude = Math.abs(zMagnitude);
			if(absMagnitude > HEAD_THRESHOLD){
				mSleepLevel += (absMagnitude - HEAD_THRESHOLD) * (System.currentTimeMillis() - mLastHeadEvent) * MODIFIER_HEAD_SCALE_PER_MILLISECOND;
			}

		}
	}

	private void degradeSleepLevel() {
		// Don't apply the degradation to the first event

		mLastEvent = Math.max(mLastEyeEvent, mLastHeadEvent);

		if (mLastEvent != 0) {
			// mSleepLevel has to be at least 0
			mSleepLevel = Math
					.max(0,
							mSleepLevel
									- ((System.currentTimeMillis() - mLastEvent) * DEGRADATION_PER_MILLISECOND));
		}
	}

	private void checkSleepLevel() {
		Log.d(TAG, "Sleep Level: " + mSleepLevel + "/" + SLEEP_THRESHOLD);
		if (mSleepLevel >= SLEEP_THRESHOLD) {
			// The user is falling asleep
			Log.i(TAG, "The user is falling asleep");

			if (mSleepListener != null) {
				mSleepListener.onUserFallingAsleep();
			}

			// Reset the sleep level
			mSleepLevel = 0;
		}
	}

}
