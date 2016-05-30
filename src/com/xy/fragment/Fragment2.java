package com.xy.fragment;

import java.util.ArrayList;

import com.xy.adapter.Frag2ListAdapter;
import com.xy.mybs.ChangePassword;
import com.xy.mybs.DownLoadHistory;
import com.xy.mybs.Login;
import com.xy.mybs.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Fragment2 extends Fragment implements OnItemClickListener, OnClickListener {
	
	private static int CHANGE_PASSWORD_REQUEST_CODE = 1;
	private static String SHARED_FILE_NAME = "BSFile";
	private ListView lv;
	private View view;
	private Editor editor;
	private String nickName;
	private Button btn_logoff;
	private String lastusername;
	private SharedPreferences sharedPre;
	private Frag2ListAdapter listAdapter;
	private TextView frag2_username_text;
	private ArrayList<String> frag2ListData;

	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment2, null);
		init();
		return view;
	}

	/**
	 * 初始化操作
	 */
	private void init() {
		// 初始化页面底侧的"退出登录"按钮
		btn_logoff = (Button) view.findViewById(R.id.btn_logoff);
		btn_logoff.setOnClickListener(this);
		// 存储数据使用
		sharedPre = getActivity().getSharedPreferences(SHARED_FILE_NAME, 0);
		editor = sharedPre.edit();
		// 初始化显示当前用户控件的显示内容文字
		frag2_username_text = (TextView) view.findViewById(R.id.frag2_username_text);
		lastusername = sharedPre.getString("lastUser", "user");
		nickName = sharedPre.getString(lastusername+"nickName", lastusername);
		frag2_username_text.setText("昵称:" + nickName +"\r\n["+lastusername+"]");
		LinearLayout face = (LinearLayout) view.findViewById(R.id.frag2_face);
		face.getBackground().setAlpha(100);

		lv = (ListView) view.findViewById(R.id.frag2_list_view);
		frag2ListData = new ArrayList<String>();
		frag2ListData.add("下载历史");
		frag2ListData.add("设置昵称");
		frag2ListData.add("修改密码");
		listAdapter = new Frag2ListAdapter(getActivity(), frag2ListData);
		// 给listView设置适配器
		lv.setAdapter(listAdapter);
		// 给listView设置item点击监听器
		lv.setOnItemClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_logoff:
			// 页面最低侧 退出当前登录的按钮
			editor.putBoolean("Login", false).commit();
			Intent intent = new Intent(getActivity().getApplicationContext(), Login.class);
			getActivity().startActivity(intent);
			getActivity().finish();
			break;
		}
	}

	/**
	 * listView item 点击事件操作
	 * @param parent : listView
	 * @param view : itemView
	 * @param position : 点击位置
	 * @param id : 点击item的ID
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch(position){
		case 0:
			// 下载历史选项
			showDownloadHistory();
			break;
		case 1:
			// 设置昵称选项
			setNickName();
			break;
		case 2:
			// 修改密码
			changePassword();
			break;
		}
	}

	/**
	 * 显示下载历史 : 打开一个新的Activity
	 * 遍历Download目录中的的文件名,将文件名与listData
	 * 中相同的选择出来,显示在页面上.
	 */
	private void showDownloadHistory() {
		// 打开新的Activity,显示已经下载的文件的列表信息
		Intent intent = new Intent(getContext(), DownLoadHistory.class);
		startActivity(intent);
	}

	/**
	 * 修改密码操作
	 */
	private void changePassword() {
		Intent intent = new Intent(getContext(), ChangePassword.class);
		startActivityForResult(intent, CHANGE_PASSWORD_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CHANGE_PASSWORD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			btn_logoff.performClick();
		}
	}
	/**
	 * 设置昵称功能
	 */
	private EditText editText;
	private void setNickName() {
		// 1.弹出输入对话框
		editText = new EditText(getContext());
		editText.setBackground(null);
		editText.setHint("请输入昵称");
		editText.setTextSize(25f);
		Builder builder = new AlertDialog.Builder(getContext());
		builder.setView(editText);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 得到用户输入的文本
				nickName = editText.getText().toString().trim();
				// 写入到sharedPreference
				editor.putString(lastusername+"nickName", nickName);
				editor.commit();
				// 更新页面显示的昵称
				frag2_username_text.setText("当前用户:" + nickName +"\r\n["+lastusername+"]");
			}
		});
		builder.setNegativeButton("取消", null);
		builder.create();
		builder.show();
	}

}
