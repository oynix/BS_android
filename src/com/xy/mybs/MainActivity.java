package com.xy.mybs;

import java.util.ArrayList;

import com.xy.adapter.MainActivityFragmentAdapter;
import com.xy.fragment.Fragment1;
import com.xy.fragment.Fragment2;
import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnClickListener{

	private ViewPager mPager;
	private ImageView cursor;
	private View t1, t2;
	private float offset = 0;
	private int currIndex = 0;
	private Fragment1 frag1;
	private Fragment2 frag2;
	private int bmpW;
	private int mResIds[];
	private ImageView mTab_btns[];
	private static final int nTabCnt = 2;
	private Toast toast = null;

	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// ▓▆█▆▼初始化标识游标
		initCursor();
		// 初始化标题
		initTitleContent();
		// 初始化ViewPager
		initViewPager();
		toast = Toast.makeText(this, "再点一次退出程序", Toast.LENGTH_SHORT);
	}

	private void initTitleContent() {
		t1 = findViewById(R.id.tab1);
		t2 = findViewById(R.id.tab2);
		t1.setOnClickListener(new MyOnClickListener(0));
		t2.setOnClickListener(new MyOnClickListener(1));

		// 存放图标名字的id的数组
		mResIds = new int[2 * nTabCnt];
		mResIds[0] = R.drawable.index;
		mResIds[1] = R.drawable.index_selected;
		mResIds[2] = R.drawable.person;
		mResIds[3] = R.drawable.person_selected;

		// 存放两个ImageView的数组
		mTab_btns = new ImageView[nTabCnt];
		mTab_btns[0] = (ImageView) findViewById(R.id.iv1);
		mTab_btns[1] = (ImageView) findViewById(R.id.iv2);
	}

	private void initViewPager() {
		mPager = (ViewPager) findViewById(R.id.viewpager);

		ArrayList<Fragment> fragmentArray = new ArrayList<Fragment>();
		frag1 = new Fragment1();
		frag2 = new Fragment2();
		fragmentArray.add(frag1);
		fragmentArray.add(frag2);
		mPager.setAdapter(new MainActivityFragmentAdapter(getSupportFragmentManager(), fragmentArray));
		mPager.setCurrentItem(0);
		mPager.addOnPageChangeListener(new MyOnPageChangeListener());
	}

	private void initCursor() {
		cursor = (ImageView) findViewById(R.id.active_bar);

		bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.cursor).getWidth();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;
		offset = (screenW / nTabCnt - bmpW) / 2f;

		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
		}
		return false;
	}

	private void exit() {
		if (toast.getView().getParent() != null) {
			finish();
		} else {
			// 初始化Toast
			toast.show();
		}
	}

	// 页顶图片的点击事件监听器
	public class MyOnClickListener implements View.OnClickListener {
		private int index = 0;

		public MyOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			mPager.setCurrentItem(index);
		}
	};

	// Fragment改变时的监听器
	public class MyOnPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageSelected(int arg0) {
			
			// 隐藏itemPopupWindow
			frag1.dismissPopupWindow();

			// 初始化平移动画

			Animation animation = null;
			float nStart = currIndex * (2 * offset + bmpW);
			float nEnd = arg0 * (2 * offset + bmpW);
			animation = new TranslateAnimation(nStart, nEnd, 0, 0);
			animation.setDuration(400);
			animation.setFillAfter(true);
			cursor.startAnimation(animation);

			// 改变ImageView的src属性
			mTab_btns[arg0].setImageResource(mResIds[arg0 * 2 + 1]);
			mTab_btns[currIndex].setImageResource(mResIds[currIndex * 2]);
			currIndex = arg0;
		}

		// float nStart = currIndex * (2 * offset + bmpW);
		// float last_end = nStart;

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			//
			// Animation animation = null;
			// float m_nStart = last_end;
			// float m_nEnd = m_nStart + arg1 * m_screenW / nTabCnt;
			// animation = new TranslateAnimation(m_nStart, m_nEnd, 0, 0);
			// last_end = m_nEnd;
			// animation.setFillAfter(true);
			// cursor.startAnimation(animation);
			// Log.e(TAG, arg0 + ">>scrolling" + arg1 + ">>>>" + arg2 +
			// "start>>>>" + m_nStart);

		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	@Override
	public void onClick(View v) {
		frag1.dismissPopupWindow();
	}
}
