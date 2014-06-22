package com.relevance.photoextension;

import java.io.File;

import com.relevance.photoextension.dao.MySQLiteHelper;
import com.relevance.photoextension.hotspot.AddHotspotToImage;
import com.relevance.photoextension.hotspot.HotspotDetails;
import com.relevance.photoextension.utility.Constants;
import com.relevance.photoextension.utility.Utility;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.Toast;

public class HomeScreen extends Activity implements OnClickListener {

	private ImageView upArrow;
	private TableLayout tableLayout;
	private String imageFilePath = "";
	private boolean upArrowPress = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_screen);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initilize();
	}

	private void initilize() {
		ImageView cameraButton = (ImageView) findViewById(R.id.home_camera);
		ImageView galleryButton = (ImageView) findViewById(R.id.home_gallery);
		upArrow = (ImageView) findViewById(R.id.up_button);
		tableLayout = (TableLayout) findViewById(R.id.tableLayout1);

		cameraButton.setOnClickListener(this);
		galleryButton.setOnClickListener(this);
		upArrow.setOnClickListener(this);

		if (upArrowPress) {
			upArrow.setVisibility(View.INVISIBLE);
			tableLayout.setVisibility(View.VISIBLE);
		} else {
			upArrow.setVisibility(View.VISIBLE);
			tableLayout.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.home_camera: // Start camera lunch activity
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			imageFilePath = Utility.getCapturedImage();
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(imageFilePath)));
			startActivityForResult(intent, Constants.CAMERA_REQUEST_CODE);
			break;

		case R.id.home_gallery:
			// Request scan of gallery to check for existing images
			Intent scanIntent = new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			sendBroadcast(scanIntent);

			// Start activity to choose photo from gallery
			Intent galleryIntent = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(galleryIntent,
					Constants.GALLERY_REQUEST_CODE);
			break;

		case R.id.up_button:
			upArrowPress = true;
			upArrow.setVisibility(View.INVISIBLE);
			tableLayout.setVisibility(View.VISIBLE);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Constants.GALLERY_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			int found = -1;
			imageFilePath = Utility.getRealPathFromURI(this, data.getData());

			MySQLiteHelper dbHelper = new MySQLiteHelper(
					getApplicationContext());
			found = dbHelper.getImage(imageFilePath);

			if (found == -1) {
				Intent intent = new Intent(this, AddHotspotToImage.class);
				intent.putExtra("imageName", imageFilePath);
				intent.putExtra("edit", false);
				startActivity(intent);
			} else {
				Intent intent = new Intent(this, HotspotDetails.class);
				intent.putExtra("imageName", imageFilePath);
				startActivity(intent);
			}
			upArrowPress = false;
		}

		else if (requestCode == Constants.CAMERA_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(this, AddHotspotToImage.class);
				intent.putExtra("imageName", imageFilePath);
				startActivity(intent);

				Utility.mediaScanForCapturedImage(this, imageFilePath);
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.capture_cancelled),
						Toast.LENGTH_SHORT).show();
			}
			upArrowPress = false;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("imageName", imageFilePath);
		outState.putBoolean("ifPressed", upArrowPress);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		imageFilePath = savedInstanceState.getString("imageName");
		upArrowPress = savedInstanceState.getBoolean("ifPressed");
	}
}
