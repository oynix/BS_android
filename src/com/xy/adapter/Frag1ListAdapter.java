package com.xy.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.xy.mybs.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Frag1ListAdapter extends BaseAdapter {

	private ArrayList<HashMap<String[], Bitmap>> listData;
	private Context context;

	public Frag1ListAdapter(Context context, ArrayList<HashMap<String[], Bitmap>> listData) {
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
			convertView = LayoutInflater.from(context).inflate(R.layout.frag1_list_cell, null);
			holder = new ViewHolder();
			holder.list_cell_img = (ImageView) convertView.findViewById(R.id.list_cell_img);
			holder.filename = (TextView) convertView.findViewById(R.id.filename);
			holder.uploadtime = (TextView) convertView.findViewById(R.id.uploadtime);
			holder.filesize = (TextView) convertView.findViewById(R.id.filesize);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.list_cell_img.setImageBitmap(listData.get(position).values().iterator().next());
		holder.filename.setText(listData.get(position).keySet().iterator().next()[0]);
		holder.filesize.setText("文件大小:"+listData.get(position).keySet().iterator().next()[1]);
		holder.uploadtime.setText("上传时间:"+listData.get(position).keySet().iterator().next()[2]);
		return convertView;
	}

	private class ViewHolder {
		private ImageView list_cell_img;
		private TextView filename, uploadtime, filesize;
	}
}
