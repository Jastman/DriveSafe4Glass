/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.bluetooth;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BluetoothManager {
	
	private static final String TAG = "BluetoothManager";
	
	private BluetoothAdapter mBtAdapter;
	
	public BluetoothManager(){
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	public boolean isConnected(){
		Log.d(TAG, "Bluetooth state: "+mBtAdapter.getState());
		return mBtAdapter.getState() == BluetoothAdapter.STATE_CONNECTED;
	}

}
