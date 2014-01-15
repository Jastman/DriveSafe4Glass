/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.alert;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.drive.safe.glass.KeepAwakeService;
import com.drive.safe.glass.preference.PreferenceConstants;

public class AlertManager implements LocationListener {

	private static final String TAG = "AlertManager";

	/**
	 * No alert should be displayed
	 */
	public static final int NO_ALERT = 0;

	/**
	 * Play a wake-up beep sound, but don't show the alert
	 */
	public static final int BEEP_ONLY = 1;

	/**
	 * Play the wake-up beep sound and show the alert
	 */
	public static final int SHOW_ALERT = 2;

	/**
	 * Don't allow alerts to happen more frequently than this time in
	 * milliseconds
	 */
	private static final long AFTER_ALERT_DELAY = 9000;

	/**
	 * Don't allow beeps to happen more frequently than this time in
	 * milliseconds
	 */
	private static final long AFTER_BEEP_DELAY = 3000;

	/**
	 * The amount of time without an alert that it takes to reset mAlertCount
	 */
	private static final long ALERT_COUNT_RESET_TIME = 120000;

	/**
	 * The minimum speed in meters per second that is considered to be moving 5
	 * meters per second is about 11.18 miles per hour
	 */
	private static final float MIN_SPEED = 4;

	/**
	 * The maximum amount of time to wait to get a Location from GPS
	 */
	private static final long GPS_POLL_TIME = 6000;

	/**
	 * GPS data older than this amount is considered to be too old to be
	 * relevant
	 */
	private static final long SPEED_STALE_TIME = 12000;

	private KeepAwakeService.KeepAwakeBinder mServiceBinder;

	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (service instanceof KeepAwakeService.KeepAwakeBinder) {
				mServiceBinder = (KeepAwakeService.KeepAwakeBinder) service;
			}

			mContext.unbindService(this);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Do nothing
		}
	};

	private Context mContext;
	private SharedPreferences mPrefs;

	private LocationManager mLocMan;
	private Location mFirstLocation;
	private float mLastSpeed = 0;
	private long mLastSpeedTime = 0;

	private long mNextValidAlertTime = 0;
	private int mAlertCount = 0;


	public AlertManager(Context context, SharedPreferences prefs) {
		mContext = context;
		mPrefs = prefs;

		mLocMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		// Bind the service to get access to the updateOnUserFallingAsleep method
		mContext.bindService(new Intent(mContext, KeepAwakeService.class), mServiceConn, 0);
	}

	/**
	 * Determine if to show an alert, only play the beep sound file, or do
	 * nothing
	 */
	public int getAlertLevel() {
		long curTime = System.currentTimeMillis();

		int alertLevel = NO_ALERT;

		if (curTime - mNextValidAlertTime > ALERT_COUNT_RESET_TIME) {
			mAlertCount = 1;
			alertLevel = determineAlert();
		} else if (curTime > mNextValidAlertTime) {
			mAlertCount++;
			alertLevel = determineAlert();
		}

		if (alertLevel == BEEP_ONLY) {
			mNextValidAlertTime = curTime + AFTER_BEEP_DELAY;
		} else if (alertLevel == SHOW_ALERT) {
			mNextValidAlertTime = curTime + AFTER_ALERT_DELAY;
		}

		return alertLevel;
	}

	private int determineAlert() {
		if (mPrefs.getBoolean(PreferenceConstants.SHOW_ALERT_ONLY_IF_MOVING, PreferenceConstants.SHOW_ALERT_ONLY_IF_MOVING_DEFAULT) && !isMoving()) {
			// Preference to ignore sleep if we're not moving
			// and we're not moving

			// Don't bother trying to alert again until the GPS has
			// definitely polled again
			mNextValidAlertTime = System.currentTimeMillis() + GPS_POLL_TIME;

			return NO_ALERT;
		}

		if (!mPrefs.getBoolean(PreferenceConstants.SHOW_ALERT_ONLY_IF_MOVING, PreferenceConstants.SHOW_ALERT_ONLY_IF_MOVING_DEFAULT) || isMoving()) {
			int alertPreference = mPrefs.getInt(PreferenceConstants.SHOW_ALERT, PreferenceConstants.SHOW_ALERT_DEFAULT);
			if (alertPreference == PreferenceConstants.SHOW_ALERT_NEVER) {
				return BEEP_ONLY;
			} else if (mAlertCount > alertPreference) {
				mAlertCount = 0;
				return SHOW_ALERT;
			}
		}
		return BEEP_ONLY;
	}

	/**
	 * Try to determine if the user is moving or not
	 */
	private boolean isMoving() {
		Criteria speedCriteria = new Criteria();
		speedCriteria.setSpeedRequired(true);


		if (System.currentTimeMillis() - mLastSpeedTime > SPEED_STALE_TIME) {
			// This Location is not up to date anymore so it
			// does not provide useful information about current speed
			Log.d(TAG, "Last known location is too old, recalculating speed");
			
			String locationProvider = mLocMan.getBestProvider(speedCriteria, false);
			Log.d(TAG, "Location Provider being used: " + locationProvider);
			mLocMan.requestLocationUpdates(locationProvider, 0, 0, this);
			
			// We don't know what the speed is, so return false for now
			return false;
		}
		
		Log.d(TAG, "Last known speed is " + mLastSpeed + " meters per second, " + (System.currentTimeMillis() - mLastSpeedTime) + " millis ago");
		return mLastSpeed > MIN_SPEED;
	}

	// The following are LocationListener methods

	@Override
	public void onLocationChanged(Location location) {
		if(mFirstLocation == null){
			// This is the first data point to calculate speed
			mFirstLocation = location;
		} else {
			// This is the second data point
			// So the speed can be calculated
			
			float distance = Math.abs(location.distanceTo(mFirstLocation));
			
			// Keep this speed for future use (updateOnUserFallingAsleep will re-query AlertManager)
			mLastSpeed = (1000 * distance / (location.getTime() - mFirstLocation.getTime()));
			mLastSpeedTime = System.currentTimeMillis();

			Log.d(TAG, "Speed is "+mLastSpeed);
			
			// The location only has to be found once
			mLocMan.removeUpdates(this);
			mFirstLocation = null;

			// Call updateOnUserFallingAsleep because there's new GPS data
			mServiceBinder.updateOnUserFallingAsleep();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// Do nothing
	}

	@Override
	public void onProviderEnabled(String provider) {
		// Do nothing
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Do nothing
	}

}
