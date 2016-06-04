package com.xy.mybs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

public class SplashActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		new Thread(){
			public void run() {
				SystemClock.sleep(2000);
				Intent intent = new Intent(SplashActivity.this, Login.class);
				startActivity(intent);
				finish();
				overridePendingTransition(R.anim.next_enter, R.anim.next_exit);
			};
		}.start();
	}
}
