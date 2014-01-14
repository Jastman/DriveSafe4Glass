/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass;

import com.drive.safe.glass.preference.PreferenceConstants;
import com.victor.kaiser.pendergrast.settings.GlassPreferenceActivity;
import com.victor.kaiser.pendergrast.settings.option.OptionsBuilder;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class PreferenceActivity extends GlassPreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		OptionsBuilder alertOptions = new OptionsBuilder();
		alertOptions.addOption(getString(R.string.pref_alert_first))
					.addOption(getString(R.string.pref_alert_second))
					.addOption(getString(R.string.pref_alert_third))
					.addOption(getString(R.string.pref_alert_never));
		// Default to show the second time
		addChoicePreference(PreferenceConstants.SHOW_ALERT, getString(R.string.pref_alert), alertOptions.build(), PreferenceConstants.SHOW_ALERT_DEFAULT);
		addTogglePreference(PreferenceConstants.ALLOW_ANALYTICS, getString(R.string.pref_analytics), PreferenceConstants.ALLOW_ANALYTICS_DEFAULT);
		
		buildAndShowOptions();
	}


}
