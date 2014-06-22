package com.relevance.photoextension.utility;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.util.Log;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public final class Utility {

	private Utility() {
		// private constructor
	}

	public static String getCapturedImage() {

		// External sdcard location
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				Constants.DIRECTORY_NAME);
		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
			Log.d(Constants.DIRECTORY_NAME, "Oops! Failed create "
					+ Constants.DIRECTORY_NAME + " directory");
			return null;
		}
		// Create a timestamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());

		return mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp
				+ ".png";
	}

	public static String getRecordedAudio(Context context) {

		// Creating an internal directory
		File audioStorageDir = context.getDir(Constants.DIRECTORY_NAME,
				Context.MODE_PRIVATE);
		// Create the storage directory if it does not exist
		if (!audioStorageDir.exists() && !audioStorageDir.mkdirs()) {
			Log.d(Constants.DIRECTORY_NAME, "Oops! Failed create "
					+ Constants.DIRECTORY_NAME + " directory");
			return null;
		}
		// Create a timestamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());

		return audioStorageDir.getPath() + File.separator + "AUD_" + timeStamp
				+ ".3gp";
	}

	public static String getRealPathFromURI(Context context, Uri contentUri) {
		String filepath = "";
		String uriPath = contentUri.toString();
		// Handle local file and remove url encoding
		if (uriPath.startsWith("file://")) {
			filepath = uriPath.replace("file://", "");
			try {
				return URLDecoder.decode(filepath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		try {
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = context.getContentResolver().query(contentUri,
					projection, null, null, null);
			if (cursor != null && cursor.getCount() != 0) {
				int column_index = cursor
						.getColumnIndex(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				filepath = cursor.getString(column_index);
			}
		} catch (Exception e) {
			Log.e("Path Error", e.toString());
		}
		return filepath;
	}

	public static void setImage(Context context, String imageFilePath,
			ImageView selectedImg) {

		Bitmap imageBitmap = decodeSampledBitmapFromFile(imageFilePath,
				selectedImg.getWidth(), selectedImg.getHeight());
		int rotate = getCameraPhotoOrientation(context,
				Uri.fromFile(new File(imageFilePath)), imageFilePath);
		if (rotate != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(rotate);
			imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
					imageBitmap.getWidth(), imageBitmap.getHeight(), matrix,
					true);
		}
		if (imageBitmap.getWidth() > imageBitmap.getHeight()) {
			((Activity) context)
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			((Activity) context)
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		selectedImg.setScaleType(ScaleType.FIT_CENTER);
		selectedImg.setImageBitmap(imageBitmap);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
			final float totalPixels = width * height;
			// Anything more than 2x the requested pixels it'll sample down
			// further
			final float totalReqPixelsCap = reqWidth * reqHeight * 2;
			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}

	public static synchronized Bitmap decodeSampledBitmapFromFile(
			String filename, int reqWidth, int reqHeight) {
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filename, options);
	}

	public static int getCameraPhotoOrientation(Context context, Uri imageUri,
			String imagePath) {
		int rotate = 0;
		try {
			context.getContentResolver().notifyChange(imageUri, null);
			File imageFile = new File(imagePath);
			ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			}
			Log.d("Exif orientation: ", orientation + "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rotate;
	}

	public static void mediaScanForCapturedImage(Context context,
			String imageFilePath) {
		// Request scan of gallery to check for captured image
		Intent mediaScan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri uriSavedImage = Uri.fromFile(new File(imageFilePath));
		mediaScan.setData(uriSavedImage);
		context.sendBroadcast(mediaScan);
	}

	public static boolean isValidUrl(String url) {
		Pattern p = Patterns.WEB_URL;
		Matcher m = p.matcher(url);
		return m.matches();
	}

}
