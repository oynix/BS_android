package com.xy.mybs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;

import com.xy.adapter.DownloadHistoryListAdapter;
import com.xy.mybs.ui.MyImagePlayer;
import com.xy.mybs.ui.MyVidioPlayer;
import com.xy.utils.BitmapUtils;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class DownLoadHistory extends Activity implements OnItemClickListener {

	private String userName;
	private SharedPreferences sharedPre;
	private DownloadHistoryListAdapter adapter;
	private static String SHARED_FILE_NAME = "BSFile";
	private ArrayList<HashMap<String[], Bitmap>> listDataBitmap;
	private ArrayList<HashMap<String[], Bitmap>> listDataForAdapter = new ArrayList<HashMap<String[], Bitmap>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_history);
		// 初始化SharedPreferences
		sharedPre = getSharedPreferences(SHARED_FILE_NAME, 0);
		// 得到当前用户名
		userName = sharedPre.getString("lastUser", "");
		// 得到ListView
		ListView dh_listView = (ListView) findViewById(R.id.dh_list_view);
		// 得到保存文件的路径
		String absolutePath = getFilesDir().getAbsolutePath();
		downloadListDataFile = new File(absolutePath + "/" + "downloadListData.obj");
		try {
			// 得到DownloadListData ArrayList对象
			ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(downloadListDataFile));
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String[], String>> listDataString = (ArrayList<HashMap<String[], String>>) objIn
					.readObject();
			// 关闭流对象
			objIn.close();
			listDataBitmap = BitmapUtils.fromStringToBitmap(listDataString);
			// 初始化listData数据 和 适配器
			listDataForAdapter.clear();
			listDataForAdapter.addAll(listDataBitmap);
			adapter = new DownloadHistoryListAdapter(this, listDataForAdapter);
			// 设置适配器
			dh_listView.setAdapter(adapter);
			// 设置item的点击事件
			dh_listView.setOnItemClickListener(this);
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 点击ListView的item显示出来的气泡窗口
	 */
	private PopupWindow itemPopupWindow = null;

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// 显示之前先把原来的dismiss掉
		dismissPopupWindow();
		// 解析出popupWindow布局
		View contentView = View.inflate(getApplicationContext(), R.layout.dh_list_item_popup_window, null);
		// 得到当前item的位置 , 在屏幕上的位置 ,x坐标和y坐标
		int[] location = new int[2];
		view.getLocationInWindow(location);
		// 初始化气泡
		itemPopupWindow = new PopupWindow(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		// 设置背景 : 透明色
		itemPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// 显示气泡的位置
		itemPopupWindow.showAtLocation(parent, Gravity.END | Gravity.TOP, 0, location[1] + 10);
		// 得到当前item的文件名,
		String fileName = ((TextView) view.findViewById(R.id.filename)).getText().toString().trim();
		// 得到气泡里的控件
		// 打开按钮 : 根据文件名,打开文件, 然后显示给用户
		LinearLayout dh_open = (LinearLayout) contentView.findViewById(R.id.dh_open);
		// 删除按钮 : 根据文件名, 将文件从手机中删除
		LinearLayout dh_delete = (LinearLayout) contentView.findViewById(R.id.dh_delete);

		// 给 "打开" "删除" 设置监听器,将要操作的文件名传入
		dh_open.setOnClickListener(new MyItemClickListener(fileName));
		dh_delete.setOnClickListener(new MyItemClickListener(fileName));
	}

	/**
	 * item popupWindow气泡中的的进度圆形转动条
	 * 
	 */
	private ProgressDialog progressDialog = null;
	private File downloadListDataFile;

	class MyItemClickListener implements OnClickListener {

		private String absPath;

		private String fileDir;

		private File operatFile;

		public MyItemClickListener(String fileName) {
			// 2.得到系统目录
			File publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			absPath = publicDirectory.getAbsolutePath();
			// 3.得到文件目录
			fileDir = absPath + "/" + userName;
			// 得到要打开或者删除的文件
			operatFile = new File(fileDir + "/" + fileName);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.dh_open:
				// 打开
				lookoverOption();
				break;
			case R.id.dh_delete:
				// 删除
				deleteOption();
				break;
			}
		}

		// 在线查看操作
		private void lookoverOption() {
			dismissPopupWindow();
			// 1.先弹出ProgressDialog, 显示progress, 让界面锁住
			progressDialog = initProgressDialog("正在打开,请稍后...");
			progressDialog.show();
			// 如果存在,直接打开该文件
			if (operatFile.exists()) {
				// 4.progress消失
				progressDialog.dismiss();
				// 5.显示给用户,新建Activity ,显示媒体文件
				showMediaFile(operatFile);
			} else {
				// 4.progress消失
				progressDialog.dismiss();
				// 打开失败,则用消息对话框提示用户
				showMessageDialog("打开失败");
			}
		}

		// 删除操作
		private void deleteOption() {
			dismissPopupWindow();
			// 1.先弹出ProgressDialog, 显示progress, 让界面锁住
			progressDialog = initProgressDialog("删除中,请稍后...");
			progressDialog.show();
			// 5.如果存在,则删除,
			boolean isDelete = false;
			if (operatFile.exists()) {
				System.gc();
				isDelete = operatFile.delete();
			}
			// 如果删除成功,修改downloadLsitData,
			if (isDelete) {
				for (int i = 0; i < listDataBitmap.size(); i++) {
					HashMap<String[], Bitmap> hashMap = listDataBitmap.get(i);
					String[] strings = hashMap.keySet().iterator().next();
					String temp_name = strings[0];
					if (temp_name.equals(operatFile.getName())) {
						listDataBitmap.remove(i);
						break;
					}
				}
			}
			// 将新的listDataBitmap写回到手机
			try {
				ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(downloadListDataFile));
				objOut.writeObject(listDataBitmap);
				objOut.flush();
				objOut.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 更新适配器的listData
			listDataForAdapter.clear();
			listDataForAdapter.addAll(listDataBitmap);
			// 刷新页面显示内容
			adapter.notifyDataSetChanged();
			// 进度条对话框消失
			progressDialog.dismiss();
			// 弹出信息提示框
			showMessageDialog(isDelete ? "删除成功" : "删除失败");
		}
	}

	/**
	 * 隐藏listView点击后显示的PopupWindow
	 */
	public void dismissPopupWindow() {
		if (itemPopupWindow != null) {
			itemPopupWindow.dismiss();
		}
		itemPopupWindow = null;
	}

	/**
	 * 根据传入的message, 初始化一个ProgressDialog
	 * 
	 * @param message进度条对话框的提示信息
	 * @return 返回这个进度条对话框
	 */
	private ProgressDialog initProgressDialog(String message) {
		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(message);
		return progressDialog;
	}

	/**
	 * 根据传入的媒体文件路径,将文件显示出来 视频文件使用MyVideoPlayer Activity播放 图片文件使用MyImagePlayer
	 * Activity显示
	 * 
	 * @param file : 要显示的文件
	 */
	private void showMediaFile(File file) {
		// 得到数据路径
		Uri uri = Uri.fromFile(file);
		// 得到文件的扩展名, 从文件名中分离出
		String name = file.getName();
		Log.e("showMediaFile>>>fileName::::", name);
		Log.e("showMediaFile>>>uri.getEncodiPath::::", uri.getEncodedPath());
		String[] name_split = name.split("\\.");
		String schema = name_split[name_split.length - 1];
		Log.e("showMediaFile>>>fileSchema::::", schema);
		// 如果是mp4格式的文件,则用系统的视频播放器打开
		if ("mp4".equalsIgnoreCase(schema)) {
			Intent intent = new Intent(this, MyVidioPlayer.class);
			intent.setData(uri);
			startActivity(intent);
			dismissPopupWindow();
		} else {
			// 使用图片浏览器打开
			Intent intent = new Intent(this, MyImagePlayer.class);
			intent.setData(uri);
			startActivity(intent);
			dismissPopupWindow();
		}
	}

	/**
	 * 显示信息提示对话框
	 * 
	 * @param message
	 *            提示框中显示的内容
	 */
	private void showMessageDialog(String message) {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("提示");
		builder.setMessage(message);
		builder.setPositiveButton("确定", null);
		builder.create();
		builder.show();
	}
}
