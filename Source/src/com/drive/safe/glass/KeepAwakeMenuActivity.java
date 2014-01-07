package com.drive.safe.glass;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Activity showing the options menu.
 */
public class KeepAwakeMenuActivity extends Activity {
	private static final String TAG = "MenuActivity";


	private KeepAwakeService.KeepAwakeBinder mServiceBinder;

	private ServiceConnection mServiceConn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service){
			if(service instanceof KeepAwakeService.KeepAwakeBinder){
				mServiceBinder = (KeepAwakeService.KeepAwakeBinder) service;
			}
			
			unbindService(this);
		}

		@Override
		public void onServiceDisconnected(ComponentName name){
			//Do nothing
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Bind the service to get access to the getDirectionsToRestArea method
		bindService(new Intent(this, KeepAwakeService.class), mServiceConn, 0);
	}

	@Override
	public void onResume() {
		super.onResume();
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.keep_awake, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection.
		switch (item.getItemId()) {
		case R.id.directions:
			Log.d(TAG, "Directions");
			if(mServiceBinder != null){
				mServiceBinder.getDirectionsToRestArea();
			}
			finish();
			return true;
		case R.id.stop:
			Log.d(TAG, "Stopping");
			stopService(new Intent(this, KeepAwakeService.class));
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing else to do, closing the Activity.
		finish();
	}
}
