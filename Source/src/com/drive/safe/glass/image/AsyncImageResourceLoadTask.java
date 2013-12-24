/**
 * @author Victor Kaiser-Pendergrast
 */

package com.drive.safe.glass.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

public class AsyncImageResourceLoadTask extends AsyncTask<Integer, Integer, Bitmap>{
	
	public interface ImageLoadListener {
		public void onImageLoad();
	}

	private Resources mResources;
	private ImageView mImageView;
	private ImageLoadListener mImageLoadListener;
	
	/**
	 * @param resources the Resources to load the image from
	 * @param imageView the ImageView that will have its image set
	 * @param listener the ImageLoadListener to call after loading the image
	 */
	public AsyncImageResourceLoadTask(Resources resources, ImageView imageView, ImageLoadListener listener){
		super();
		mResources = resources;
		mImageView = imageView;
		mImageLoadListener = listener;
	}
	
	/**
	 * @param resources the Resources to load the image from
	 * @param imageView the ImageView that will have its image set
	 * @param listener the ImageLoadListener to call after loading the image
	 */
	public AsyncImageResourceLoadTask(Resources resources, ImageView imageView){
		super();
		mResources = resources;
		mImageView = imageView;
	}
	
	/**
	 * @param imageView the ImageView that will have the image loaded into
	 */
	public void setImageView(ImageView imageView){
		mImageView = imageView;
	}
	
	/**
	 * @return The ImageView that will receive the loaded image 
	 */
	public ImageView getImageView(){
		return mImageView;
	}

	/**
	 * @return the resources that contain the image resource
	 */
	public Resources getResources() {
		return mResources;
	}

	/**
	 * @param resources the resources to load the image resource from
	 */
	public void setResources(Resources resources) {
		mResources = resources;
	}

	/**
	 * @return the listener that will be called when the image is loaded
	 */
	public ImageLoadListener getImageLoadListener() {
		return mImageLoadListener;
	}

	/**
	 * @param imageLoadListener the listener that will be called when the image is loaded
	 */
	public void setImageLoadListener(ImageLoadListener imageLoadListener) {
		mImageLoadListener = imageLoadListener;
	}

	/**
	 * @param resourceIds The resource ID of the image to load
	 */
	@Override
	protected Bitmap doInBackground(Integer... resourceIds) {
		if(resourceIds.length == 0){
			// There aren't any image resources
			return null;
		}
		
		return BitmapFactory.decodeResource(mResources, resourceIds[0]);
	}
	
	@Override
	protected void onPostExecute(Bitmap image){
		if(mImageView != null && image != null){
			mImageView.setImageBitmap(image);
		}
	}

}
