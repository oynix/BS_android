package com.xy.mybs.ui;

import com.xy.mybs.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class MyVidioPlayer extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_video_player);

		Intent intent = getIntent();
		Uri uri = intent.getData();

		VideoView videoView = (VideoView) findViewById(R.id.my_video_player);
		videoView.setVideoURI(uri);
		videoView.setMediaController(new MediaController(this));
		videoView.start();
		videoView.requestFocus();
	}
}
