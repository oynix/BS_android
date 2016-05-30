package com.xy.adapter;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MainActivityFragmentAdapter extends FragmentPagerAdapter {

	// 这个是存放Fragment的数组，待会从MainActivity中传过来就行了
	private ArrayList<Fragment> fragmentArray;

	// 自己添加一个构造函数从MainActivity中接收这个Fragment数组
	public MainActivityFragmentAdapter(FragmentManager fm, ArrayList<Fragment> fragmentArray) {
		this(fm);
		this.fragmentArray = fragmentArray;
	}

	public MainActivityFragmentAdapter(FragmentManager fm) {
		super(fm);
	}

	// 这个函数的作用是当切换到第arg0个页面的时候调用。
	@Override
	public Fragment getItem(int arg0) {
		return this.fragmentArray.get(arg0);
	}

	@Override
	public int getCount() {
		return this.fragmentArray.size();
	}

}
