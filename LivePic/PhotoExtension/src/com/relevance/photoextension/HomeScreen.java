package com.relevance.photoextension;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TableLayout;
import android.widget.Toast;

import com.relevance.photoextension.dao.MySQLiteHelper;
import com.relevance.photoextension.hotspot.AddHotspotToImage;
import com.relevance.photoextension.hotspot.HotspotDetails;
import com.relevance.photoextension.model.GridViewImageAdapter;
import com.relevance.photoextension.utility.Constants;
import com.relevance.photoextension.utility.ImageUtils;
import com.relevance.photoextension.utility.Utility;

public class HomeScreen extends Activity implements OnClickListener,OnDrawerOpenListener,OnDrawerCloseListener {

	private ImageView upArrow;
	private TableLayout tableLayout;
	private String imageFilePath = "";
	private boolean upArrowPress = false;
	private SlidingDrawer sldr;
	private Context context;
	
	//GridView utility variables
	private ImageUtils utils;
    private ArrayList<String> imagePaths = new ArrayList<String>();
    private GridViewImageAdapter adapter;
    private GridView gridView;
    private int columnWidth;

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
		//Initializing GirdView to Main layout
		gridView = (GridView) findViewById(R.id.grid_view);
		
		ImageView cameraButton = (ImageView) findViewById(R.id.home_camera);
		ImageView galleryButton = (ImageView) findViewById(R.id.home_gallery);
		upArrow = (ImageView) findViewById(R.id.up_button);
		tableLayout = (TableLayout) findViewById(R.id.tableLayout1);
		sldr = (SlidingDrawer) this.findViewById(R.id.slidingDrawer1);

		cameraButton.setOnClickListener(this);
		galleryButton.setOnClickListener(this);
		upArrow.setOnClickListener(this);
		
		//Listen for open event
				sldr.setOnDrawerOpenListener(this);

		        // Listen for close event
				sldr.setOnDrawerCloseListener(this);
				
				cameraButton.setOnClickListener(this);
				galleryButton.setOnClickListener(this);
				upArrow.setOnClickListener(this);
				
				if(upArrowPress) {
					//sldr.animateOpen();
					sldr.open();
				}
				else {
					sldr.close();
					//sldr.animateClose();
				}
				
		/*
		if (upArrowPress) {
			upArrow.setVisibility(View.INVISIBLE);
			tableLayout.setVisibility(View.VISIBLE);
		} else {
			upArrow.setVisibility(View.VISIBLE);
			tableLayout.setVisibility(View.INVISIBLE);
		}
		*/
				
				utils = new ImageUtils(this);
				 
		        // Initilizing Grid View
		        InitilizeGridLayout();
		 
		        // loading all image paths from SD card
		        imagePaths = utils.getFilePaths();
		 
		        // Gridview adapter
		        adapter = new GridViewImageAdapter(HomeScreen.this, imagePaths,
		                columnWidth);
		 
		        // setting grid view adapter
		        gridView.setAdapter(adapter);
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
			//upArrowPress = false;
			upArrowPress = true;
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
			//upArrowPress = false;
			upArrowPress = true;
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
	
	@Override
	public void onDrawerClosed() {
		// TODO Auto-generated method stub
        Toast.makeText(getApplicationContext(),"Drawer Closed", Toast.LENGTH_SHORT).show();
        upArrowPress = false;
	}

	@Override
	public void onDrawerOpened() {
		// TODO Auto-generated method stub
		Toast.makeText(getApplicationContext(),"Drawer Opened", Toast.LENGTH_SHORT).show();
        upArrowPress = true;
	}
	
	//Initializing GridView Properties
	private void InitilizeGridLayout() {
        Resources r = getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Constants.GRID_PADDING, r.getDisplayMetrics());
 
        columnWidth = (int) ((utils.getScreenWidth() - ((Constants.NUM_OF_COLUMNS + 1) * padding)) / Constants.NUM_OF_COLUMNS);
 
        gridView.setNumColumns(Constants.NUM_OF_COLUMNS);
        gridView.setColumnWidth(columnWidth);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setPadding((int) padding, (int) padding, (int) padding,
                (int) padding);
        gridView.setHorizontalSpacing((int) padding);
        gridView.setVerticalSpacing((int) padding);
        
      //Setting OnItemClickListener for the GridView Images
        gridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
				int position, long id) {
				//Taking path of the images from the GridView
				imageFilePath = adapter.getItem(position).toString();
			   Toast.makeText(getApplicationContext(),
					   adapter.getItem(position).toString(), Toast.LENGTH_SHORT).show();
			   //opening the selected image in Edit Mode
			   Intent intent = new Intent(getApplicationContext(), HotspotDetails.class);
				intent.putExtra("imageName", imageFilePath);
				startActivity(intent);
			}
		});
    }
}
