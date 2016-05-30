package com.xy.mybs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.xy.utils.MD5Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

public class Register extends Activity implements OnClickListener, TextWatcher {

	private String server_ip;
	protected static final int REGISTE_SUCCESS = 1;
	protected static final int REGISTE_FALSE = 0;
	private static final int ILLEGEL_CHARACTER = 2;
	protected static final int REGISTE_ERROR = 3;
	private EditText register_account;
	private EditText register_password_check;
	private EditText register_password;
	private Button btn_register;
	private Toast toast;
	private String acount;
	private String pass;
	private String pass_check;
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REGISTE_FALSE:
				toast = Toast.makeText(getApplicationContext(), "该用户名已被使用", Toast.LENGTH_SHORT);
				toast.show();
				break;
			case REGISTE_ERROR:
				toast = Toast.makeText(getApplicationContext(), "系统错误，请重试", Toast.LENGTH_SHORT);
				toast.show();
				break;
			case REGISTE_SUCCESS:
				toast = Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT);
				toast.show();
				Intent intent = new Intent(getApplication(), Login.class);
				startActivity(intent);
				finish();
				break;
			case ILLEGEL_CHARACTER:
				toast = Toast.makeText(getApplicationContext(), "请输入合法字符", Toast.LENGTH_SHORT);
				toast.show();
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_act);

		// 得到Manifest文件中meta-data中的服务器IP地址
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(),
					PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// 得到三个输入控件
		register_account = (EditText) findViewById(R.id.register_account);
		register_password = (EditText) findViewById(R.id.register_password);
		register_password_check = (EditText) findViewById(R.id.register_password_check);

		register_account.addTextChangedListener(this);
		// 得到注册按钮控件
		btn_register = (Button) findViewById(R.id.btn_register);
		// 设置点击监听器
		btn_register.setOnClickListener(this);

	}

	/**
	 * 控件点击监听器
	 * 
	 * @param v
	 */

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_register:
			btnRegisterPressed();
		}
	}

	/**
	 * 注册按钮的点击事件
	 */
	private void btnRegisterPressed() {
		// 得到控件中的内容
		acount = register_account.getText().toString().trim();
		pass = register_password.getText().toString().trim();
		pass_check = register_password_check.getText().toString().trim();
		// 先判断是否全部输入
		if ("".equals(acount) || "".equals(pass) || "".equals(pass_check)) {
			// 未输入完整则Toast提示
			toast = Toast.makeText(this, "请输入完整", Toast.LENGTH_SHORT);
			toast.show();
		} else {
			// 判断两个密码框内容是否一致
			if (pass.equals(pass_check)) {
				// 如果一致，则创建子线程，连接服务器，进行注册操作
				new Thread() {
					@Override
					public void run() {
						Message msg = new Message();
						try {
							// 将用户注册信息发送至服务器端进行验证
							boolean isSuccess = registe(acount, pass);
							// 注册成功
							if (isSuccess) {
								msg.what = REGISTE_SUCCESS;
							} else {
								msg.what = REGISTE_FALSE;
							}

						} catch (ClassNotFoundException e) {
							e.printStackTrace();
							msg.what = REGISTE_ERROR;
						} catch (IOException e) {
							e.printStackTrace();
							msg.what = REGISTE_ERROR;
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
							msg.what = REGISTE_ERROR;
						}
						handler.sendMessage(msg);
					};
				}.start();
			} else {
				// 不一致则Toast提示
				toast = Toast.makeText(this, "两次密码输入不一致", Toast.LENGTH_SHORT);
				toast.show();
			}
		}

	}

	/**
	 * 进行注册操作，返回一个boolean类型的值，标志是否注册成功 true 代表成功 false 代表用户名已经使用
	 * 
	 * @param acount
	 * @param pass
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException 
	 */
	private boolean registe(String acount, String pass) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		// 封装数据：注册名和密码
		String[] data = new String[] { acount, MD5Utils.md5Digest(pass) };
		// 注册URL
		String address = "http://" + server_ip + "/BSServer/servlet/RegisterServlet";
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
		// 返回账户和密码是否正确
		ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
		Boolean result = (Boolean) objIn.readObject();
		objIn.close();
		objOut.close();
		return result.booleanValue();

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		String editable = register_account.getText().toString();  
        String str = stringFilter(editable.toString());
        if(!editable.equals(str)){
        	register_account.setText(str);
        	Message msg = new Message();
        	msg.what = ILLEGEL_CHARACTER;
        	handler.dispatchMessage(msg);
            //设置新的光标所在位置  
        	register_account.setSelection(str.length());
        }		
	}
	public static String stringFilter(String str)throws PatternSyntaxException{     
	      // 只允许字母、数字和汉字      
	      String   regEx  =  "[^a-zA-Z0-9\u4E00-\u9FA5]";                     
	      Pattern   p   =   Pattern.compile(regEx);     
	      Matcher   m   =   p.matcher(str);     
	      return   m.replaceAll("").trim();     
	  }

}
