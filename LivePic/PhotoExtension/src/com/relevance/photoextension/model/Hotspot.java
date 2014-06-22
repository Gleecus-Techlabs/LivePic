package com.relevance.photoextension.model;

import android.widget.ImageView;

public class Hotspot {

	private int pid;
	private int xPosition;
	private int yPosition;
	private String inputText;
	private String audioFile;
	private String inputUrl;
	private int mediaTypeFlag;
	private ImageView iView;

	public Hotspot() {
		// Default Constructor
	}

	public Hotspot(int xPosition, int yPosition, String inputText,
			String inputUrl, String audioFile, int mediaTypeFlag) {
		super();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.inputText = inputText;
		this.inputUrl = inputUrl;
		this.audioFile = audioFile;
		this.mediaTypeFlag = mediaTypeFlag;
		this.iView = null;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public int getXPosition() {
		return xPosition;
	}

	public void setXPosition(int xPosition) {
		this.xPosition = xPosition;
	}

	public int getYPosition() {
		return yPosition;
	}

	public void setYPosition(int yPosition) {
		this.yPosition = yPosition;
	}

	public String getInputText() {
		return inputText;
	}

	public void setInputText(String inputText) {
		this.inputText = inputText;
	}

	public String getAudioFile() {
		return audioFile;
	}

	public void setAudioFile(String audioFile) {
		this.audioFile = audioFile;
	}

	public String getInputUrl() {
		return inputUrl;
	}

	public void setInputUrl(String inputUrl) {
		this.inputUrl = inputUrl;
	}

	public int getMediaTypeFlag() {
		return mediaTypeFlag;
	}

	public void setMediaTypeFlag(int mediaTypeFlag) {
		this.mediaTypeFlag = mediaTypeFlag;
	}
	
	public ImageView getImageView() {
		return this.iView;
	}
	
	public void setImageView(ImageView iv) {
		this.iView = iv;
	}
}