package com.xy.mybs.ui;

import com.xy.mybs.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

public class MyImagePlayer extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_image_player);
		
		Intent intent = getIntent();
		Uri uri = intent.getData();
		ImageView im = (ImageView) findViewById(R.id.my_image_player);
		im.setImageURI(uri);
		im.setBackgroundColor(Color.BLACK);
		im.requestFocus();
	}
}
