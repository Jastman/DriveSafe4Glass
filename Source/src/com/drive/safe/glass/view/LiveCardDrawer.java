/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.view;

import com.drive.safe.glass.R;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

public class LiveCardDrawer implements SurfaceHolder.Callback, LiveCardView.DrawListener {
	private static final String TAG = "LiveCardDrawer";

	private final LiveCardView mLiveCardView;

	private SurfaceHolder mHolder;
	private Context mContext;

	public LiveCardDrawer(Context context) {
		mContext = context;

		mLiveCardView = new LiveCardView(mContext);

		mLiveCardView.setDrawListener(this);
		
		mLiveCardView.setTitle(mContext.getString(R.string.text_keeping_you_awake));
		mLiveCardView.setImage(R.drawable.ic_drive_50);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Measure and layout the view with the canvas dimensions.
		int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
		int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

		mLiveCardView.measure(measuredWidth, measuredHeight);
		mLiveCardView.layout(0, 0, mLiveCardView.getMeasuredWidth(), mLiveCardView.getMeasuredHeight());
		
		draw(mLiveCardView);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "Surface created");
		mHolder = holder;
		
		draw(mLiveCardView);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "Surface destroyed");
		mHolder = null;
	}
	
	/**
	 * Draw the view in the SurfaceHolder's canvas
	 */
	private void draw(View view){
		if(mHolder == null){
			return;
		}
		
		Canvas canvas;
		try{
			canvas = mHolder.lockCanvas();
		} catch (Exception e){
			e.printStackTrace();
			return;
		}
		
		if(canvas != null){
			//We can draw the view
			view.draw(canvas);
			mHolder.unlockCanvasAndPost(canvas);
		}
	}

	/**
	 * Redraw the LiveCardView when it wants
	 * ie. when the LiveCardView is done loading its image
	 */
	@Override
	public void onCallForDraw() {
		draw(mLiveCardView);
	}

}
