package com.relevance.photoextension.hotspot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.relevance.photoextension.R;
import com.relevance.photoextension.dao.MySQLiteHelper;
import com.relevance.photoextension.model.Hotspot;
import com.relevance.photoextension.model.Image;
import com.relevance.photoextension.utility.Constants;
import com.relevance.photoextension.utility.Utility;

@SuppressWarnings("deprecation")
public class HotspotDetails extends Activity implements OnClickListener,
		OnLongClickListener,OnTouchListener{

	private final ArrayList<Hotspot> allHotspot = new ArrayList<Hotspot>();
	private final ArrayList<ImageView> hotspotIcon = new ArrayList<ImageView>();
	private ViewGroup rootView;
	private View myView;
	private ImageView selectedImg;
	private ImageView recordIcon;
	private ImageView view_button;
	private ImageView mediaIcon;
	private EditText editText;
	private EditText editUrl;
	private MySQLiteHelper dbHelper;
	private static MediaPlayer mediaPlayer = null;
	private static MediaRecorder mediaRecorder = null;
	private String text;
	private String url;
	private String imageFilePath;
	private String selectedMediaPath = "";
	private String audioFilePath = "";
	private Dialog dlg = null;
	private boolean canPlay = true;
	private boolean canRecord = false;
	private boolean isRecording = false;
	private boolean isPlaying = false;
	private boolean isMediaPlaying = false;
	private boolean isHotspotVisible = false;
	private int index = 0;
	private int mediaEditFlag = 0;
	private int mediaIdFlag = 0;
	private static int imageLevelFlag = 0;
	protected static int densityDpi;
	
	private static final String TAG = "Touch";
	@SuppressWarnings("unused")
	private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;
	
	// These matrices will be used to scale points of the image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();
	Matrix matrixInitial = new Matrix();
	float iniScale;
	
	boolean isLoadedFirstTime = true;
	boolean isDialogOpen = false;
	
	// The 3 states (events) which the user is trying to perform
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;
	
	// these PointF objects are used to record the point(s) the user is touching
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;
	
	private ArrayList<ImageView> hotspots = new ArrayList<ImageView>();
	
	float [] valuesInitial =  new float[9];


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.view_hotspot);

		imageFilePath = getIntent().getStringExtra("imageName");

		rootView = (ViewGroup) findViewById(R.id.root);
		selectedImg = (ImageView) findViewById(R.id.imageCenter);
		view_button = (ImageView) findViewById(R.id.view_button);
		ImageView new_button = (ImageView) findViewById(R.id.view_button2);
		ImageView edit_button = (ImageView) findViewById(R.id.edit_button);

		view_button.setOnClickListener(this);
		view_button.setOnLongClickListener(this);
		new_button.setOnClickListener(this);
		edit_button.setOnClickListener(this);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		densityDpi = (int) (metrics.density * 160f);

		dbHelper = new MySQLiteHelper(getApplicationContext());

		drawHotspot();
		
		selectedImg.setOnTouchListener(this);
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			if (isDialogOpen) {
				return;
			}
			
			Utility.setImage(HotspotDetails.this, imageFilePath, selectedImg);
			updateHotspotPosition();
			
			if (isLoadedFirstTime) {
				isLoadedFirstTime = false;
				Matrix temp = selectedImg.getImageMatrix();
				matrixInitial.set(temp);
				matrix.set(temp);
				
				matrixInitial.getValues(valuesInitial);
			}
		}
	}

	private void drawHotspot() {
		
		allHotspot.addAll(dbHelper.getAllHotspot(new Image(imageFilePath)));
		
		if (!allHotspot.isEmpty()) {
			for (Hotspot h : allHotspot) {
				if (h.getXPosition() != 0 && h.getYPosition() != 0) {
					ImageView hotspot = new ImageView(this);
					hotspot.setImageResource(R.drawable.pin);
					RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
							48, 48);
					layoutParams.leftMargin = (int) h.getXPosition();
							//- densityDpi / 10;
					layoutParams.topMargin = (int) h.getYPosition();
							//- densityDpi / 10;
					hotspot.setLayoutParams(layoutParams);
					hotspot.setVisibility(View.INVISIBLE);
					hotspotIcon.add(hotspot);
//					hotspot.setOnClickListener(new OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							myView = v;
//							removeDialog(100);
//							showDialog(100);
//						}
//					});
					rootView.addView(hotspot);
				} else if (h.getXPosition() == 0 && h.getYPosition() == 0) {
					hotspotIcon.add(selectedImg);
					hotspots.add(selectedImg);
					view_button.setOnLongClickListener(this);
					imageLevelFlag = 1;
				}
			}
		} else {
			Toast.makeText(HotspotDetails.this,
					getResources().getString(R.string.no_hotspot),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK
				&& requestCode == Constants.RQS_OPEN_AUDIO_MP3) {
			Uri selectedMediaUri = data.getData();
			selectedMediaPath = Utility.getRealPathFromURI(this,
					selectedMediaUri);
			recordIcon.setImageResource(R.drawable.microphone_icon);
			audioFilePath = null;
			canRecord = true;
			canPlay = false;
			mediaIdFlag = 2;
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if (v.getId() == R.id.view_button) {
			if (imageLevelFlag != 0) {
				myView = selectedImg;
				removeDialog(100);
				showDialog(100);
			} else {
				Toast.makeText(
						HotspotDetails.this,
						getResources()
								.getString(R.string.no_imagelevel_hotspot),
						Toast.LENGTH_LONG).show();
			}
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.view_button:
			toggleHotspots();
			break;

		case R.id.view_button2:
			Toast.makeText(this,
					getResources().getString(R.string.new_feature),
					Toast.LENGTH_LONG).show();
			break;
			

		case R.id.edit_button:
			Intent intent = new Intent(HotspotDetails.this,
					AddHotspotToImage.class);
			intent.putExtra("imageName", imageFilePath);
			intent.putExtra("edit", true);
			startActivity(intent);
			finish();
			break;
		}
	}

	private void toggleHotspots() {
		for (ImageView x : hotspots) {
			if (x.equals(selectedImg))
				continue;
			if (isHotspotVisible)
				x.setVisibility(View.INVISIBLE);
			else
				x.setVisibility(View.VISIBLE);
		}
		isHotspotVisible = !isHotspotVisible;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		isDialogOpen = true;
		Dialog dialog = null;
		if (id == 100) {
			dialog = createViewDialog();
		} else if (id == 200) {
			dialog = createEditDialog();
		}
		return dialog;
	}

	private Dialog createViewDialog() {

		index = hotspots.indexOf(myView);

		isMediaPlaying = false;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = li.inflate(R.layout.dialog_in_viewmode, null);
		final TableLayout attach_rl = (TableLayout) v
				.findViewById(R.id.hotspot_attach);
		final RelativeLayout url_layout = (RelativeLayout) v
				.findViewById(R.id.hotspot_link_extended);
		attach_rl.setVisibility(View.GONE);
		url_layout.setVisibility(View.GONE);

		builder.setView(v);

		editText = (EditText) v.findViewById(R.id.round_text);
		editUrl = (EditText) v.findViewById(R.id.et_round_text2);
		recordIcon = (ImageView) v.findViewById(R.id.microphone);
		mediaIcon = (ImageView) v.findViewById(R.id.audio);
		ImageView webIcon = (ImageView) v.findViewById(R.id.web);
		ImageView attachButton = (ImageView) v.findViewById(R.id.attach);
		ImageView cancelButton = (ImageView) v.findViewById(R.id.cancelButton);
		ImageView editButton = (ImageView) v.findViewById(R.id.editButton);
		ImageView videoButton = (ImageView) v.findViewById(R.id.video);
		ImageView contactsButton = (ImageView) v.findViewById(R.id.contacts);
		ImageView locationButton = (ImageView) v.findViewById(R.id.location);
		ImageView shoppingButton = (ImageView) v.findViewById(R.id.shopping);

		text = allHotspot.get(index).getInputText();
		url = allHotspot.get(index).getInputUrl();

		if (url != null && !url.isEmpty()) {
			url_layout.setVisibility(View.VISIBLE);
			editUrl.setText(url);
		}

		editText.setFocusable(false);
		editUrl.setFocusable(false);

		if (text != null)
			editText.setText(text);

		int mediaFlag = allHotspot.get(index).getMediaTypeFlag();
		if (mediaFlag == 0) {
			if (allHotspot.get(index).getAudioFile() != null
					&& !allHotspot.get(index).getAudioFile().isEmpty()) {
				recordIcon.setImageResource(R.drawable.play);
				recordIcon.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						if (!isPlaying) {

							recordIcon.setImageResource(R.drawable.stop);
							startPlayingRecordedAudio();

						} else if (isPlaying && mediaPlayer != null) {

							stopPlayingRecordedAudio();
							recordIcon.setImageResource(R.drawable.play);
							Log.v(AUDIO_SERVICE, "recording stop played");
						}
					}
				});
			} else {
				recordIcon.setVisibility(View.INVISIBLE);
			}
			mediaIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg2) {
					Toast.makeText(
							HotspotDetails.this,
							getResources()
									.getString(R.string.no_audio_selected),
							Toast.LENGTH_SHORT).show();
				}
			});

		} else if (mediaFlag == 2) {
			recordIcon.setVisibility(View.INVISIBLE);
			if (allHotspot.get(index).getAudioFile() != null
					&& allHotspot.get(index).getAudioFile() != "") {

				mediaIcon.setImageResource(R.drawable.play_media3);
				mediaIcon.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						if (!isMediaPlaying) {
							isMediaPlaying = true;
							mediaIcon.setImageResource(R.drawable.stop_media);
							startPlayingRecordedAudio();

						} else if (isMediaPlaying && mediaPlayer != null) {
							try {
								mediaPlayer.stop();
								mediaPlayer.release();
							} catch (Exception e) {
								e.printStackTrace();
							}
							mediaPlayer = null;
							isMediaPlaying = false;
							mediaIcon.setImageResource(R.drawable.play_media3);
							Log.v(AUDIO_SERVICE, "Play stopped");
						}
					}
				});
			} else {
				mediaIcon.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg2) {
						Toast.makeText(
								HotspotDetails.this,
								getResources().getString(
										R.string.no_audio_selected),
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

		webIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String path = "";
				if (url != null) {
					if (!url.isEmpty()) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						if (!url.startsWith("http://")) {
							path = url.replace(url, "http://" + url);
						} else {
							path = url;
						}
						intent.setData(Uri.parse(path));
						startActivity(intent);
					} else {
						Toast.makeText(HotspotDetails.this,
								getResources().getString(R.string.empty_url),
								Toast.LENGTH_LONG).show();
					}
				}
			}
		});

		attachButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (attach_rl.getVisibility() != View.VISIBLE) {
					attach_rl.setVisibility(View.VISIBLE);
				} else {
					attach_rl.setVisibility(View.GONE);
				}
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((isPlaying || isMediaPlaying) && mediaPlayer != null) {
					stopPlayingRecordedAudio();
					recordIcon.setImageResource(R.drawable.play);
					Log.v(AUDIO_SERVICE, "recording played cancelled");
				}
				dlg.dismiss();
				removeDialog(100);
			}
		});

		editButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((isPlaying || isMediaPlaying) && mediaPlayer != null) {
					stopPlayingRecordedAudio();
					recordIcon.setImageResource(R.drawable.play);
					Log.v(AUDIO_SERVICE,
							"recording played cancelled due to edit");
				}
				dlg.dismiss();
				removeDialog(100);
				removeDialog(200);
				showDialog(200);
			}
		});

		videoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.video_annotation),
						Toast.LENGTH_LONG).show();
			}
		});

		locationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.poi_location),
						Toast.LENGTH_LONG).show();
			}
		});

		shoppingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.shopping_links),
						Toast.LENGTH_LONG).show();
			}
		});

		contactsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.assign_to_contacts),
						Toast.LENGTH_LONG).show();
			}
		});

		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if ((isPlaying || isMediaPlaying) && mediaPlayer != null) {
					stopPlayingRecordedAudio();
					recordIcon.setImageResource(R.drawable.play);
					Log.v(AUDIO_SERVICE, "recording played cancelled");
				}
				dlg.dismiss();
				removeDialog(100);
			}
		});
		dlg = builder.create();
		Window window = dlg.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();
		wlp.gravity = Gravity.BOTTOM;
		wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		window.setAttributes(wlp);

		return dlg;
	}

	private Dialog createEditDialog() {

		index = hotspots.indexOf(myView);

		reInitMedia();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = li.inflate(R.layout.dailog_in_editmode, null);
		final TableLayout attach_rl = (TableLayout) v
				.findViewById(R.id.hotspot_attach);
		final RelativeLayout url_layout = (RelativeLayout) v
				.findViewById(R.id.hotspot_link_extended);
		url_layout.setVisibility(View.GONE);
		attach_rl.setVisibility(View.GONE);

		builder.setView(v);

		editText = (EditText) v.findViewById(R.id.round_text);
		editUrl = (EditText) v.findViewById(R.id.et_round_text2);
		recordIcon = (ImageView) v.findViewById(R.id.microphone);
		ImageView attachButton = (ImageView) v.findViewById(R.id.attach);
		ImageView link_icon = (ImageView) v.findViewById(R.id.link);
		ImageView mediaIcon = (ImageView) v.findViewById(R.id.audio);
		ImageView saveButton = (ImageView) v.findViewById(R.id.saveButton);
		ImageView deleteButton = (ImageView) v.findViewById(R.id.deleteButton);
		ImageView backButton = (ImageView) v.findViewById(R.id.backButton);
		ImageView videoButton = (ImageView) v.findViewById(R.id.video);
		ImageView contactsButton = (ImageView) v.findViewById(R.id.contacts);
		ImageView locationButton = (ImageView) v.findViewById(R.id.location);
		ImageView shoppingButton = (ImageView) v.findViewById(R.id.shopping);

		text = allHotspot.get(index).getInputText();
		url = allHotspot.get(index).getInputUrl();

		if (text != null)
			editText.setText(text);
		if (url != null)
			editUrl.setText(url);

		if (allHotspot.get(index).getAudioFile() != null
				&& allHotspot.get(index).getAudioFile() != "") {
			if (allHotspot.get(index).getMediaTypeFlag() == 0) {
				recordIcon.setImageResource(R.drawable.play);
				audioFilePath = allHotspot.get(index).getAudioFile();
				isPlaying = false;
				canPlay = true;
				isRecording = false;
				canRecord = false;
				mediaIdFlag = 0;
			} else if (allHotspot.get(index).getMediaTypeFlag() == 2) {
				selectedMediaPath = allHotspot.get(index).getAudioFile();
				mediaIdFlag = 2;
			}
		}

		if (allHotspot.get(index).getInputText() != null
				&& allHotspot.get(index).getInputText() != "") {
			editText.setText(allHotspot.get(index).getInputText());
		}
		if (allHotspot.get(index).getInputUrl() != null
				&& allHotspot.get(index).getInputUrl() != "") {
			editUrl.setText(allHotspot.get(index).getInputUrl());
		}

		recordIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectedMediaPath != null && !selectedMediaPath.isEmpty()) {

					AlertDialog.Builder alertDialogBuilder2 = new AlertDialog.Builder(
							HotspotDetails.this);
					// set dialog message
					alertDialogBuilder2
							.setMessage(
									getResources().getString(
											R.string.overwrite_selected_media))
							.setCancelable(false)
							.setPositiveButton(android.R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											selectedMediaPath = null;
											mediaIdFlag = 0;
											mediaEditFlag++;

											if (!isRecording && canRecord) {
												recordIcon
														.setImageResource(R.drawable.stop);
												startRecordAudio();

											} else if (isRecording
													&& mediaRecorder != null) {
												stopRecordAudio();
												recordIcon
														.setImageResource(R.drawable.play);
												Log.v(AUDIO_SERVICE,
														"recording stopped now click to play");

											} else if (!isPlaying && canPlay) {
												recordIcon
														.setImageResource(R.drawable.stop);
												startPlayingRecordedAudio();

											} else if (isPlaying
													&& mediaPlayer != null) {
												stopPlayingRecordedAudio();
												recordIcon
														.setImageResource(R.drawable.play);
												Log.v(AUDIO_SERVICE,
														"recording stop played");
											}
										}
									})
							.setNegativeButton(android.R.string.no,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alertDialog2 = alertDialogBuilder2.create();
					alertDialog2.show();

				} else {
					if (!isRecording && canRecord) {
						recordIcon.setImageResource(R.drawable.stop);
						startRecordAudio();

					} else if (isRecording && mediaRecorder != null) {
						stopRecordAudio();
						recordIcon.setImageResource(R.drawable.play);
						Log.v(AUDIO_SERVICE,
								"recording stopped now click to play");

					} else if (!isPlaying && canPlay) {
						recordIcon.setImageResource(R.drawable.stop);
						startPlayingRecordedAudio();

					} else if (isPlaying && mediaPlayer != null) {
						stopPlayingRecordedAudio();
						recordIcon.setImageResource(R.drawable.play);
						Log.v(AUDIO_SERVICE, "recording stop played");
					}
					mediaIdFlag = 0;
					mediaEditFlag++;
				}
			}
		});

		attachButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (attach_rl.getVisibility() != View.VISIBLE) {
					attach_rl.setVisibility(View.VISIBLE);
				} else {
					attach_rl.setVisibility(View.GONE);
				}
			}
		});

		link_icon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (url_layout.getVisibility() != View.VISIBLE) {
					url_layout.setVisibility(View.VISIBLE);
					if (url != null && !url.isEmpty())
						editUrl.setText(url);
				} else {
					String urlData = editUrl.getText().toString();
					if (urlData != null && !urlData.isEmpty()) {
						if (Utility.isValidUrl(urlData)) {
							url = urlData;
							url_layout.setVisibility(View.GONE);
						} else {
							Toast.makeText(
									HotspotDetails.this,
									getResources().getString(
											R.string.invalid_url),
									Toast.LENGTH_LONG).show();
							editUrl.setFocusable(true);
						}
					} else {
						url_layout.setVisibility(View.GONE);
					}
				}
			}
		});

		mediaIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (audioFilePath != null) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							HotspotDetails.this);
					alertDialogBuilder.setTitle(getResources().getString(
							R.string.media_select));
					// set dialog message
					alertDialogBuilder
							.setMessage(
									getResources().getString(
											R.string.overwrite_selected_media))
							.setCancelable(false)
							.setPositiveButton(android.R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {

											startActivityToChooseAudio();
										}
									})
							.setNegativeButton(android.R.string.no,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
				} else {
					startActivityToChooseAudio();
				}
			}
		});

		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveTheChange();
			}
		});

		deleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						HotspotDetails.this);
				alertDialogBuilder.setTitle(getResources().getString(
						R.string.hotspot));
				alertDialogBuilder
						.setMessage(
								getResources().getString(
										R.string.delete_options))
						.setCancelable(false)
						.setPositiveButton(
								getResources().getString(
										R.string.delete_hotspot),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										if (myView.equals(selectedImg))
											imageLevelFlag = 0;
										if (!myView.equals(selectedImg)) {
											rootView.removeViewInLayout(myView);
											Log.d("After remove ",
													allHotspot.size() + "");
										}
										int pid = dbHelper
												.getImage(imageFilePath);
										dbHelper.deleteHotspot(
												allHotspot.get(index), pid);
										//hotspotIcon.remove(index);
										hotspots.remove(index);
										allHotspot.remove(index);
										allHotspot.clear();
										allHotspot.addAll(dbHelper
												.getAllHotspot(new Image(
														imageFilePath)));
										Toast.makeText(
												HotspotDetails.this,
												getResources()
														.getString(
																R.string.hotspot_deleted),
												Toast.LENGTH_LONG).show();
										handleEventBeforeFurtherAction();
										dlg.dismiss();
										removeDialog(200);
									}
								})
						.setNeutralButton(
								getResources().getString(
										R.string.delete_properties),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {

										allHotspot.get(index).setInputText("");
										allHotspot.get(index).setInputUrl("");
										allHotspot.get(index)
												.setAudioFile(null);

										String imageFilePath = getIntent()
												.getStringExtra("imageName");
										dbHelper.updateHotspot(
												allHotspot.get(index),
												new Image(imageFilePath));
										allHotspot.addAll(dbHelper
												.getAllHotspot(new Image(
														imageFilePath)));

										handleEventBeforeFurtherAction();
										dlg.dismiss();
										removeDialog(200);
										showDialog(200);
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										handleEventBeforeFurtherAction();
										dlg.dismiss();
										dialog.cancel();
									}
								});
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
			}
		});

		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleEventBeforeFurtherAction();
				dlg.dismiss();
				removeDialog(200);
			}
		});

		videoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.video_annotation),
						Toast.LENGTH_LONG).show();
			}
		});

		locationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.poi_location),
						Toast.LENGTH_LONG).show();
			}
		});

		shoppingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.shopping_links),
						Toast.LENGTH_LONG).show();
			}
		});

		contactsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HotspotDetails.this,
						getResources().getString(R.string.assign_to_contacts),
						Toast.LENGTH_LONG).show();
			}
		});

		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (isAnyChange()) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							HotspotDetails.this);

					alertDialogBuilder.setTitle(getResources().getString(
							R.string.hotspot));
					alertDialogBuilder
							.setMessage(
									getResources().getString(
											R.string.hotspot_save))
							.setCancelable(false)
							.setPositiveButton(android.R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											saveTheChange();
											dialog.cancel();
										}
									})
							.setNegativeButton(android.R.string.no,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
										}
									});
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
				}
				dlg.dismiss();
			}
		});
		dlg = builder.create();
		Window window = dlg.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();
		wlp.gravity = Gravity.BOTTOM;
		wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		window.setAttributes(wlp);

		return dlg;
	}

	private void saveTheChange() {

		url = editUrl.getText().toString();
		if (!url.isEmpty()) {
			if (Utility.isValidUrl(url)) {
				allHotspot.get(index).setInputText(
						editText.getText().toString());
				allHotspot.get(index).setInputUrl(editUrl.getText().toString());
				if (mediaIdFlag == 2) {
					allHotspot.get(index).setAudioFile(selectedMediaPath);
					allHotspot.get(index).setMediaTypeFlag(2);
				} else if (mediaIdFlag == 0) {
					allHotspot.get(index).setAudioFile(audioFilePath);
					allHotspot.get(index).setMediaTypeFlag(0);
				}
				dbHelper.updateHotspot(allHotspot.get(index), new Image(
						imageFilePath));
				allHotspot.addAll(dbHelper.getAllHotspot(new Image(
						imageFilePath)));
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(R.string.successfull_updation),
						Toast.LENGTH_SHORT).show();

				mediaEditFlag = 0;
				handleEventBeforeFurtherAction();
				dlg.dismiss();
				removeDialog(200);
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.invalid_url),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			allHotspot.get(index).setInputText(editText.getText().toString());
			allHotspot.get(index).setInputUrl(editUrl.getText().toString());
			if (mediaIdFlag == 2) {
				allHotspot.get(index).setAudioFile(selectedMediaPath);
				allHotspot.get(index).setMediaTypeFlag(2);
			} else if (mediaIdFlag == 0) {
				allHotspot.get(index).setAudioFile(audioFilePath);
				allHotspot.get(index).setMediaTypeFlag(0);
			}
			dbHelper.updateHotspot(allHotspot.get(index), new Image(
					imageFilePath));
			allHotspot.addAll(dbHelper.getAllHotspot(new Image(imageFilePath)));
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.successfull_updation),
					Toast.LENGTH_SHORT).show();

			mediaEditFlag = 0;
			handleEventBeforeFurtherAction();
			dlg.dismiss();
			removeDialog(200);
		}

	}

	private void stopRecordAudio() {
		try {
			mediaRecorder.stop();
			mediaRecorder.release();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		mediaRecorder = null;
		isRecording = false;
		canPlay = true;
		canRecord = false;
	}

	private void stopPlayingRecordedAudio() {
		try {
			mediaPlayer.stop();
			mediaPlayer.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mediaPlayer = null;
		if (isMediaPlaying) {
			isMediaPlaying = false;
		}
		if (isPlaying) {
			isPlaying = false;
		}
	}

	private void startRecordAudio() {
		isRecording = true;
		if (mediaRecorder != null) {
			mediaRecorder.release();
		}
		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		audioFilePath = Utility.getRecordedAudio(this);
		mediaRecorder.setOutputFile(audioFilePath);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mediaRecorder.prepare();
			mediaRecorder.start();
			Log.v(AUDIO_SERVICE, "recording is happenin");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startPlayingRecordedAudio() {
		if (!isMediaPlaying)
			isPlaying = true;
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		mediaPlayer = new MediaPlayer();
		FileInputStream fileInputStream = null;
		try {
			if (mediaEditFlag > 0) {
				fileInputStream = new FileInputStream(audioFilePath);
				mediaPlayer.setDataSource(fileInputStream.getFD());
			} else {
				fileInputStream = new FileInputStream(allHotspot.get(index)
						.getAudioFile());
				mediaPlayer.setDataSource(fileInputStream.getFD());
			}
			mediaPlayer.prepare();
			mediaPlayer.start();
			Log.v(AUDIO_SERVICE, "recording being played");
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					if (isPlaying || isMediaPlaying && mediaPlayer != null) {
						if (isPlaying)
							recordIcon.setImageResource(R.drawable.play);
						if (isMediaPlaying)
							mediaIcon.setImageResource(R.drawable.play_media3);
						stopPlayingRecordedAudio();
						Log.v(AUDIO_SERVICE, "recording completed");
					}
				}
			});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void reInitMedia() {
		canPlay = false;
		canRecord = true;
		isPlaying = false;
		isRecording = false;
	}

	private void startActivityToChooseAudio() {
		Intent intent = new Intent();
		intent.setType("audio/mp3");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(
				Intent.createChooser(intent, "Open Audio (mp3) file"),
				Constants.RQS_OPEN_AUDIO_MP3);
	}

	private boolean isAnyChange() {
		if (text.equals(editText.getText().toString())
				&& url.equals(editUrl.getText().toString())) {
			return false;
		}
		return true;
	}

	private void handleEventBeforeFurtherAction() {
		if (isRecording && mediaRecorder != null) {
			stopRecordAudio();
			recordIcon.setImageResource(R.drawable.microphone_icon);
			Log.v(AUDIO_SERVICE, "RStopped due to improper exit");
		} else if (isPlaying && mediaPlayer != null) {
			stopPlayingRecordedAudio();
			recordIcon.setImageResource(R.drawable.play);
			Log.v(AUDIO_SERVICE, "PStopped due to improper exit");
		}
		audioFilePath = null;
		selectedMediaPath = null;
		reInitMedia();
	}
	
	void dumpValue(ImageView view, String msc) {
		
        float[] values = new float[9];
        Matrix tempMat = view.getImageMatrix();
        tempMat.getValues(values);
        
        String msgOx = Integer.toString((int)values[2]);
        String msgOy = Integer.toString((int)values[5]);
        
        Log.e(msc + " X ", msgOx);
        Log.e(msc + " Y ", msgOy);
	}
	
	float scale = 1.0f;
	
	boolean isTouchFirst = true;
	
	int clickCount = 0;
	long startTime;
	static final int MAX_DURATION = 800;
	
	@Override
    public boolean onTouch(View v, MotionEvent event) 
    {		
        ImageView view = (ImageView) v;
        view.setScaleType(ImageView.ScaleType.MATRIX);
        
        //dumpValue(view, "At the Top");
        //return true;
        
        switch (event.getAction() & MotionEvent.ACTION_MASK) 
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
            	
            	if (clickCount == 0)
            		startTime = System.currentTimeMillis();
                
            	clickCount++;
            	
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG"); // write to LogCat
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted
                long time = System.currentTimeMillis() - startTime;
                if(clickCount == 2)
                {
                    if(time<= MAX_DURATION)
                    {
                    	matrix.set(matrixInitial);
                    	savedMatrix.set(matrixInitial);
                    	view.setImageMatrix(matrixInitial);
                    	view.setScaleType(ScaleType.FIT_CENTER);
                    	updateHotspotPosition();
                    }
                    clickCount = 0;
                }
                return true;
            	
            case MotionEvent.ACTION_POINTER_UP: // second finger lifted
                mode = NONE;
                Log.d(TAG, "mode=NONE");
                clickCount = 0;
                //duration = 0;
                
                float[] mxValues = new float[9];
                matrix.getValues(mxValues);
                
                if (mxValues[0] < valuesInitial[0]) {
                	matrix.set(matrixInitial);
                	savedMatrix.set(matrixInitial);
                	view.setImageMatrix(matrixInitial);
                	view.setScaleType(ScaleType.FIT_CENTER);
                } else {
                
	                RectF bmpRect = new RectF();
	                bmpRect.right = selectedImg.getDrawable().getIntrinsicWidth();
	                bmpRect.bottom = selectedImg.getDrawable().getIntrinsicHeight();
	                matrix.mapRect(bmpRect);
	                int bmpWidth = (int) bmpRect.width();
	                int bmpHeight = (int) bmpRect.height();
	
	                if (mxValues[2] + bmpWidth < selectedImg.getWidth()) {
	                	matrix.postTranslate(selectedImg.getWidth() - (mxValues[2] + bmpWidth), 0);
	                } 
	                
	                if (mxValues[5] + bmpHeight < selectedImg.getHeight()) {
	                	matrix.postTranslate(0, selectedImg.getHeight() - (mxValues[5] + bmpHeight));
	                } 
	                
	                if (mxValues[2] > 0) {
	                	matrix.postTranslate(-mxValues[2], 0);
	                }
	                
	                if (mxValues[5] > 0) {
	                	matrix.postTranslate(0, -mxValues[5]);
	                }
	                
	                view.setImageMatrix(matrix);
                }
                updateHotspotPosition();
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

            	oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM");
                }
                break;

            case MotionEvent.ACTION_MOVE:
            	
            	Log.e("DIS", Float.toString((int)event.getX() - start.x));
            	Log.e("DIS", Float.toString((int)event.getY() - start.y));
            	
            	if ((Math.abs(event.getX() - start.x) > 5) && 
            			(Math.abs(event.getY() - start.y) > 5 ))
            	{
                    clickCount = 0;
            	}
                  
                if (mode == DRAG) 
                { 
                	//Log.e("Mode", "DRAG");
                	scale = 1;
                    matrix.set(savedMatrix);
                    
                    float[] matrixValues = new float[9];
                    matrix.getValues(matrixValues);
                    
                    if (matrixValues[0] >= iniScale) {
                    	
                    	Matrix temp = selectedImg.getImageMatrix();
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                        
                        matrixValues = new float[9];
                        matrix.getValues(matrixValues);
                        
                        RectF bitmapRect = new RectF();
                        bitmapRect.right = selectedImg.getDrawable().getIntrinsicWidth();
                        bitmapRect.bottom = selectedImg.getDrawable().getIntrinsicHeight();

                        matrix.mapRect(bitmapRect);

                        int bitmapWidth = (int) bitmapRect.width();
                        int bitmapHeight = (int) bitmapRect.height();
                        
                        if (bitmapWidth > selectedImg.getWidth() && bitmapHeight > selectedImg.getHeight()) {
                        	
                        	if (matrixValues[2] > 0 ||
                        		matrixValues[5] > 0 ||
                        		matrixValues[2] + bitmapWidth < selectedImg.getWidth() ||
                        		matrixValues[5] + bitmapHeight < selectedImg.getHeight()) {
                        		
                        			matrix.set(temp);
                        	} else {
                              view.setImageMatrix(matrix);   
                              updateHotspotPosition();                      		
                        	}
                        	
                        } else if(bitmapWidth < selectedImg.getWidth() && bitmapHeight < selectedImg.getHeight() ){
                        	
                        	if (matrixValues[2] < 0 ||
                            		matrixValues[5] < 0 ||
                            		matrixValues[2] + bitmapWidth > selectedImg.getWidth() ||
                            		matrixValues[5] + bitmapHeight > selectedImg.getHeight()) {
                            		
                            			matrix.set(temp);
                            	} else {
                                  view.setImageMatrix(matrix);   
                                  updateHotspotPosition();                      		
                            	}
                        }
                        
                        else if(bitmapWidth < selectedImg.getWidth() && bitmapHeight > selectedImg.getHeight() ) {
                        	if (matrixValues[2] < 0 ||
                        			matrixValues[2] + bitmapWidth > selectedImg.getWidth() ||
                        			matrixValues[5] > 0 ||
                        			matrixValues[5] + bitmapHeight < selectedImg.getHeight()) {
                        		matrix.set(temp);
                        	} else {
                                view.setImageMatrix(matrix);   
                                updateHotspotPosition();                          		
                        	}
                        	
                        } else if(bitmapHeight < selectedImg.getHeight() && bitmapWidth > selectedImg.getWidth() ) {
                        	if (matrixValues[5] < 0 ||
                        			matrixValues[5] + bitmapHeight > selectedImg.getHeight() ||
                        			matrixValues[2] > 0 ||
                        			matrixValues[2] + bitmapWidth < selectedImg.getWidth()
                        			) {
                        		matrix.set(temp);
                        	} else {
                                view.setImageMatrix(matrix);   
                                updateHotspotPosition();                          		
                        	}
                        }
                    }
                } 
                else if (mode == ZOOM) 
                { 
                	Log.e("Mode", "ZOOM");
                    float newDist = spacing(event);
                    Log.d(TAG, "newDist=" + newDist);
                    if (newDist > 5f) 
                    {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                        
                        float[] matrixValues = new float[9];
                        matrix.getValues(matrixValues);
                        view.setImageMatrix(matrix);
                        //updateHotspotPosition();
                    }
                }
                break;
        }
        
        return true; // indicate event was handled
    }
	
    private void updateHotspotPosition() {
    	
    	// GET THE INITIAL MARIX VALUES
    	float [] iniValues = new float[9];
    	matrixInitial.getValues(iniValues);
    	
    	
    	// GET THE CURRENT MATRIX VALUES
    	float [] curValues = new float[9];
    	matrix.getValues(curValues);
    	
    	// GET THE SCALE
    	float scale = curValues[0]/iniValues[0];
    	
    	//REMOVING ALL THE HOTSPOT OLDLY POSITIONED
    	for (ImageView imgView : hotspots) {
    		rootView.removeView(imgView);
    	}
    	
    	hotspots.clear();
    	
    	//ADDING ALL THE HOTSPOT NEWLY POSITIONED
    	for (Hotspot hotspot : allHotspot) {
    		
        	//GET THE INITIAL HOTSPOT POSITION
        	int x0 = hotspot.getXPosition();
        	int y0 = hotspot.getYPosition();
        	float relativeX;
        	float relativeY;
        	if (scale == 1 && curValues[2] == iniValues[2] && curValues[5] == iniValues[5]) {
        		relativeX = x0 + 24;
        		relativeY = y0 + 24;
        	} else {
        			
     		   		float x1 = x0 + 24;
     		   		float y1 = y0 + 24;
        		
        	      relativeX = (curValues[2] + (x1 - iniValues[2])*scale);
        	      relativeY = (curValues[5] + (y1 - iniValues[5])*scale);
        	}
    		
            ImageView imgViewhotspot = new ImageView(this);
            imgViewhotspot.setImageResource(R.drawable.pin);
            imgViewhotspot.setImageResource(R.drawable.pin);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(48, 48);
            layoutParams.leftMargin = Math.round(relativeX - 24);
            layoutParams.topMargin = Math.round(relativeY - 24);
            imgViewhotspot.setLayoutParams(layoutParams);
            imgViewhotspot.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					myView = v;
					removeDialog(100);
					showDialog(100);
				}
			});
            if(isHotspotVisible) {
            	imgViewhotspot.setVisibility(View.VISIBLE);
            } else {
            	imgViewhotspot.setVisibility(View.INVISIBLE);
            }
            rootView.addView(imgViewhotspot);
            hotspots.add(imgViewhotspot);
    		
    	}
    }
    
    private float spacing(MotionEvent event) 
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) 
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

}
