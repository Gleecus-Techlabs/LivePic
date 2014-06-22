package com.relevance.photoextension.dao;

import java.util.ArrayList;

import com.relevance.photoextension.model.Hotspot;
import com.relevance.photoextension.model.Image;
import com.relevance.photoextension.utility.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

	public MySQLiteHelper(Context context) {
		super(context, Constants.DATABASE_NAME, null,
				Constants.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_IMAGE_TABLE = "CREATE TABLE " + Constants.TABLE_IMAGE
				+ " ( " + Constants.KEY_PID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + Constants.KEY_PATH
				+ " TEXT )";

		String CREATE_HOTSPOT_TABLE = "CREATE TABLE " + Constants.TABLE_HOTSPOT
				+ " ( " + Constants.KEY_HID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + Constants.KEY_PID
				+ " INTEGER , " + Constants.KEY_XPOSITION + " INTEGER, "
				+ Constants.KEY_YPOSITION + " INTEGER,"
				+ Constants.KEY_INPUT_TEXT + " TEXT, "
				+ Constants.KEY_AUDIO_FILE + " TEXT, "
				+ Constants.KEY_INPUT_URL + " TEXT, " + Constants.KEY_FLAG
				+ " INTEGER )";

		db.execSQL(CREATE_IMAGE_TABLE);
		db.execSQL(CREATE_HOTSPOT_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// drop older table if exists
		db.execSQL("DROP TABLE IF EXISTS");
		// create fresh table
		this.onCreate(db);
	}

	// Add image
	public void addImage(Image image) {
		SQLiteDatabase db = this.getWritableDatabase();
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(Constants.KEY_PATH, image.getImageName());
		db.insert(Constants.TABLE_IMAGE, null, values);
		db.close();
		Log.d(Constants.MYSQLITE_LOGTAG, "adding images to table");
	}

	// Get image id for the imagename
	public int getImage(String imageName) {
		SQLiteDatabase db = this.getReadableDatabase();
		// build query
		Cursor cursor = db.query(Constants.TABLE_IMAGE, Constants.COLUMNS1,
				" imageName = ?", new String[] { imageName }, null, null, null,
				null);
		cursor.moveToFirst();
		if (cursor.isFirst()) {
			int i = Integer.parseInt(cursor.getString(0));
			db.close();
			Log.d(Constants.MYSQLITE_LOGTAG, "retrieving image id " + i);
			return i;
		} else {
			db.close();
			// if not found
			return -1;
		}
	}

	// Add hotspot
	public void addHotspot(Hotspot hotspot, Image image) {
		int retPid = getImage(image.getImageName());
		if (retPid != -1) {
			Log.d(Constants.MYSQLITE_LOGTAG, hotspot.toString());
			SQLiteDatabase db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(Constants.KEY_PID, retPid);
			values.put(Constants.KEY_XPOSITION, hotspot.getXPosition());
			values.put(Constants.KEY_YPOSITION, hotspot.getYPosition());
			values.put(Constants.KEY_INPUT_TEXT, hotspot.getInputText());
			values.put(Constants.KEY_AUDIO_FILE, hotspot.getAudioFile());
			values.put(Constants.KEY_INPUT_URL, hotspot.getInputUrl());
			values.put(Constants.KEY_FLAG, hotspot.getMediaTypeFlag());
			db.insert(Constants.TABLE_HOTSPOT, null, values);
			db.close();
		}
	}

	// Get all hotspots
	public ArrayList<Hotspot> getAllHotspot(Image image) {
		int retPid = getImage(image.getImageName());
		ArrayList<Hotspot> hotspots = new ArrayList<Hotspot>();
		String query = "SELECT  * FROM " + Constants.TABLE_HOTSPOT
				+ " H WHERE " + "H.pid" + " = " + retPid;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		// going over each row, building hotspot and adding it to the list
		Hotspot hotspot = null;
		if (cursor.moveToFirst()) {
			do {
				hotspot = new Hotspot();
				hotspot.setPid(Integer.parseInt(cursor.getString(1)));
				hotspot.setXPosition(Integer.parseInt(cursor.getString(2)));
				hotspot.setYPosition(Integer.parseInt(cursor.getString(3)));
				hotspot.setInputText(cursor.getString(4));
				hotspot.setAudioFile(cursor.getString(5));
				hotspot.setInputUrl(cursor.getString(6));
				hotspot.setMediaTypeFlag(Integer.parseInt(cursor.getString(7)));
				hotspots.add(hotspot);
			} while (cursor.moveToNext());
			Log.d(Constants.MYSQLITE_LOGTAG, "getting all hotspots");
		}
		db.close();
		return hotspots;
	}

	// Deleting single hotpsot
	public void deleteHotspot(Hotspot hotspot, int pid) {
		SQLiteDatabase db = this.getWritableDatabase();
		String deleteQuery = "delete from hotspot where pid = '" + pid
				+ "' and xPosition = '" + hotspot.getXPosition()
				+ "' and yPosition = '" + hotspot.getYPosition() + "'";
		db.execSQL(deleteQuery);
		db.close();
		Log.d(Constants.MYSQLITE_LOGTAG,
				"deleted hotspot " + hotspot.toString());
	}

	// Deleting all hotpsots of an image
	public void deleteAllHotspot(int pid) {
		SQLiteDatabase db = this.getWritableDatabase();
		String deleteQuery = "delete from hotspot where pid = '" + pid + "'";
		db.execSQL(deleteQuery);
		db.close();
		Log.d(Constants.MYSQLITE_LOGTAG,
				"all hotspots of that image are deleted");
	}

	// Updating single hotspot
	public int updateHotspot(Hotspot hotspot, Image img) {
		int retPid = getImage(img.getImageName());
		if (retPid != -1) {
			SQLiteDatabase db = this.getWritableDatabase();
			// updating row
			String update = "update Hotspot set inputText = '"
					+ hotspot.getInputText() + "', audioFile = '"
					+ hotspot.getAudioFile() + "', inputUrl = '"
					+ hotspot.getInputUrl() + "', mediaTypeFlag = '" + hotspot.getMediaTypeFlag()
					+ "' where pid = " + retPid + " and xPosition = "
					+ hotspot.getXPosition() + " and yPosition = "
					+ hotspot.getYPosition();
			db.execSQL(update);
			db.close();
		}
		return 0;
	}
}
