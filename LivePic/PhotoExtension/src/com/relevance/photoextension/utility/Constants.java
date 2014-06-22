package com.relevance.photoextension.utility;

public class Constants {

	public static final String DIRECTORY_NAME = "LivePic";
	public static final String DATABASE_NAME = "LivePic.db";
	public static final String MYSQLITE_LOGTAG = "MySQLiteHelper";
	public static final String ADD_HOTSPOT_LOGTAG = "AddHotspotToImage";

	public static final int GALLERY_REQUEST_CODE = 100;
	public static final int CAMERA_REQUEST_CODE = 200;
	public static final int DATABASE_VERSION = 1;
	public static final int RQS_OPEN_AUDIO_MP3 = 1;

	// Table names
	public static final String TABLE_IMAGE = "Image";
	public static final String TABLE_HOTSPOT = "Hotspot";

	// Image table columns names
	public static final String KEY_PID = "pid";
	public static final String KEY_PATH = "imageName";
	public static final String[] COLUMNS1 = { KEY_PID, KEY_PATH };

	// Hotspot table columns names
	public static final String KEY_HID = "hid";
	public static final String KEY_XPOSITION = "xPosition";
	public static final String KEY_YPOSITION = "yPosition";
	public static final String KEY_INPUT_TEXT = "inputText";
	public static final String KEY_AUDIO_FILE = "audioFile";
	public static final String KEY_INPUT_URL = "inputUrl";
	public static final String KEY_FLAG = "mediaTypeFlag";

}
