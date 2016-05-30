package com.xy.mybs;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.xy.utils.MD5Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Login extends Activity implements OnClickListener, TextWatcher {
	
	private String server_ip ;
	private static final int ILLEGEL_CHARACTER = 12;
	private EditText accountET, passwordET;
	private Button login_btn;
	private SharedPreferences sharedPre;
	private Editor editor;
	private String name;
	private String password;
	private Toast toast;
	private static String SHARED_FILE_NAME = "BSFile";

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// 密码输入不正确时，拒绝登录
			case 0:
				Toast.makeText(getApplicationContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
				break;

			// 密码输入正确时，登录，修改最后一次登录的用户名
			case 1:
				editor.putBoolean("Login", true).commit();
				editor.putString("lastUser", name).commit();
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intent);
				finish();
				break;
			case ILLEGEL_CHARACTER:
				toast = Toast.makeText(getApplicationContext(), "请输入合法字符", Toast.LENGTH_SHORT);
				toast.show();
				break;
			
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_act);
		
		// 得到Manifest文件中meta-data中的服务器IP地址
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		sharedPre = getApplicationContext().getSharedPreferences(SHARED_FILE_NAME, 0);
		editor = sharedPre.edit();
		boolean isLogin = sharedPre.getBoolean("Login", false);
		// 如果是登录状态，即 上次未注销登录，直接进入
		if (isLogin) {
			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			startActivity(intent);
			finish();
		}
		// 找到两个输入控件
		accountET = (EditText) findViewById(R.id.account);
		passwordET = (EditText) findViewById(R.id.password);
		login_btn = (Button) findViewById(R.id.btn_login);

		login_btn.setOnClickListener(this);
		accountET.addTextChangedListener(this);

		// 给“新用户”设置跳转Activity
		TextView new_user = (TextView) findViewById(R.id.new_user);
		new_user.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			// 登录按钮的点击触发,得到用户输入的用户名和密码
			name = accountET.getText().toString().trim();
			password = passwordET.getText().toString().trim();
			// 判断输入是否有空的情况,有 则提示用户输入完整
			if ("".equals(name) || "".equals(password)) {
				Toast.makeText(this, R.string.input_complete, Toast.LENGTH_SHORT).show();
			} else {
				// 否则,创建子线程连接服务器
				new Thread() {
					@Override
					public void run() {
						try {
							// 连接服务器验证密码
							loginOption(name, password);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
			break;
			// 新用户注册 文本 的点击事件,跳转到新用户注册Activity
		case R.id.new_user:
			Intent intent = new Intent();
			intent.setClass(this, Register.class);
			startActivity(intent);
			break;
		}
	}

	/**
	 * 登录操作
	 * @param name
	 * @param password
	 * @throws Exception
	 */
	private void loginOption(String name, String password) throws Exception {
		// 封装数据，将加密之后的password传送给服务器端
		String[] data = new String[] { name, MD5Utils.md5Digest(password) };
		String address = "http://" + server_ip + "/BSServer/servlet/LoginServlet";
		URL url = new URL(address);
		// 得到HttpURLConnection连接对象
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// 设置相关属性
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setConnectTimeout(4000);
		connection.setReadTimeout(4000);
		connection.setRequestProperty("Content-type", "application/x-java-serialized-object");
		connection.setRequestMethod("POST");
		// 将用户名和密码封装的对象写到服务器
		connection.connect();
		ObjectOutputStream objOut = new ObjectOutputStream(connection.getOutputStream());
		objOut.writeObject(data);
		objOut.flush();
		objOut.close();
		// 返回账户和密码是否正确
		ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
		Boolean result = (Boolean) objIn.readObject();
		objIn.close();
		// 将返回结果发送给Handler
		if (result) {
			handler.sendEmptyMessage(1);
		} else {
			handler.sendEmptyMessage(0);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

	/**
	 * 监听用户名输入框:只允许驶入输入中文/大小写字母/数字,禁止其他字符输入
	 */
	@Override
	public void afterTextChanged(Editable s) {
		String editable = accountET.getText().toString();
		// 将输入的内容过滤
		String str = stringFilter(editable.toString());
		// 如果过滤后的内容与原内容不相同,则说明输入其他字符.弹窗提示并将其他字符删除
		if (!editable.equals(str)) {
			accountET.setText(str);
			Message msg = new Message();
			msg.what = ILLEGEL_CHARACTER;
			handler.dispatchMessage(msg);
			// 设置新的光标所在位置
			accountET.setSelection(str.length());
		}
	}

	/**
	 * 输入字符过滤器
	 * @param str
	 * @return
	 * @throws PatternSyntaxException
	 */
	public static String stringFilter(String str) throws PatternSyntaxException {
		// 只允许字母、数字和汉字
		String regEx = "[^a-zA-Z0-9\u4E00-\u9FA5]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	}
}
