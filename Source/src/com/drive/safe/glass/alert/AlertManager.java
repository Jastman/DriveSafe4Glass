/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.alert;

import android.content.SharedPreferences;
import android.util.Log;

import com.drive.safe.glass.preference.PreferenceConstants;

public class AlertManager {

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
	private static final long AFTER_ALERT_DELAY = 12000;

	/**
	 * The amount of time without an alert that it takes to reset mAlertCount
	 */
	private static final long ALERT_COUNT_RESET_TIME = 120000;

	private SharedPreferences mPrefs;
	private long mLastAlert = 0;
	private int mAlertCount = 0;

	public AlertManager(SharedPreferences prefs) {
		mPrefs = prefs;
	}

	/**
	 * Determine if to show an alert, only play the beep sound file, or do
	 * nothing
	 */
	public int getAlertLevel() {
		long curTime = System.currentTimeMillis();

		int alertLevel = NO_ALERT;

		if (curTime - mLastAlert > ALERT_COUNT_RESET_TIME) {
			mAlertCount = 1;
			alertLevel = determineAlert();
		} else if (curTime - mLastAlert > AFTER_ALERT_DELAY) {
			mAlertCount++;
			alertLevel = determineAlert();
		}

		if (alertLevel != NO_ALERT) {
			mLastAlert = curTime;
		}

		return alertLevel;
	}

	private int determineAlert() {
		int alertPreference = mPrefs.getInt(PreferenceConstants.SHOW_ALERT, PreferenceConstants.SHOW_ALERT_DEFAULT);
		if (alertPreference == PreferenceConstants.SHOW_ALERT_NEVER) {
			return BEEP_ONLY;
		} else if (mAlertCount > alertPreference) {
			mAlertCount = 0;
			return SHOW_ALERT;
		}
		return BEEP_ONLY;
	}
}
