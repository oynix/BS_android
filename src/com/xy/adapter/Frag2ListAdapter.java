package com.xy.adapter;

import java.util.ArrayList;

import com.xy.mybs.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class Frag2ListAdapter extends BaseAdapter {
	private ArrayList<String> listData;
	private Context context;

	public Frag2ListAdapter(Context context, ArrayList<String> listData) {
		this.listData = listData;
		this.context = context;
	}

	@Override
	public int getCount() {
		return listData.size();
	}

	@Override
	public Object getItem(int position) {
		return listData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.frag2_list_cell, null);
			holder = new ViewHolder();
			holder.settingName = (TextView) convertView.findViewById(R.id.frag2_list_cell_text);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.settingName.setText(listData.get(position));
		return convertView;
	}

	private class ViewHolder {
		private TextView settingName;
	}
}
