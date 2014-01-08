/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.widget.ImageView;

/**
 * Activity showing the options menu.
 */
public class KeepAwakeAlertActivity extends Activity {
	private static final String TAG = "MenuActivity";

	private KeepAwakeService.KeepAwakeBinder mServiceBinder;

	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (service instanceof KeepAwakeService.KeepAwakeBinder) {
				mServiceBinder = (KeepAwakeService.KeepAwakeBinder) service;
			}

			unbindService(this);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Do nothing
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.card_alert);

		// Make image red
		ImageView image = (ImageView) findViewById(R.id.card_image);
		image.setColorFilter(0xFFcc3333);

		// Bind the service to get access to the getDirectionsToRestArea method
		bindService(new Intent(this, KeepAwakeService.class), mServiceConn, 0);
	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
			// User tapped touchpad, get directions
			if(mServiceBinder != null){
				mServiceBinder.getDirectionsToRestArea();
			}
			
			finish();
			return true;
		} 

		return super.onKeyDown(keycode, event);
	}

}
