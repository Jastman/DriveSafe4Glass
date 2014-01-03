/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.view;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.drive.safe.glass.R;
import com.drive.safe.glass.image.AsyncImageResourceLoadTask;
import com.drive.safe.glass.image.AsyncImageResourceLoadTask.ImageLoadListener;

public class LiveCardView extends FrameLayout {
	public static final String TAG = "LiveCardView";

	public static interface DrawListener {
		public void onCallForDraw();
	}

	private ImageView mImageView;
	private TextView mTextTitle;

	private Context mContext;

	private DrawListener mDrawListener;

	public LiveCardView(Context context) {
		this(context, null, 0);
	}

	public LiveCardView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LiveCardView(Context context, AttributeSet attrs, int style) {
		super(context, attrs, style);

		mContext = context;

		LayoutInflater.from(context).inflate(R.layout.card_half_image_with_title, this);

		mImageView = (ImageView) findViewById(R.id.card_image);
		mTextTitle = (TextView) findViewById(R.id.card_title);
	}

	/**
	 * Set the image from the resource. Once the image is loaded, onCallForDraw
	 * will be called in the DrawListener
	 */
	public void setImage(int resource) {
		AsyncImageResourceLoadTask loadTask = new AsyncImageResourceLoadTask(mContext.getResources(), mImageView, new ImageLoadListener() {
			@Override
			public void onImageLoad() {
				if(mDrawListener != null){
					mDrawListener.onCallForDraw();
				}
			}
		});
		
		loadTask.execute(resource);
	}

	public void setTitle(String html) {
		mTextTitle.setText(Html.fromHtml(html));
	}

	public void setDrawListener(DrawListener drawer) {
		mDrawListener = drawer;
	}
	
	public DrawListener getDrawListener(){
		return mDrawListener;
	}

}
