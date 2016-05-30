package com.xy.fragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.xy.adapter.Frag1ListAdapter;
import com.xy.mybs.R;
import com.xy.mybs.ui.MyImagePlayer;
import com.xy.mybs.ui.MyVidioPlayer;
import com.xy.utils.BitmapUtils;
import com.xy.utils.ListDataUtils;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class Fragment1 extends Fragment implements OnTouchListener, OnScrollListener, OnClickListener, OnItemClickListener {

	private String server_ip;
	private static final int OPENREQUESTCODE = 0;
	private static final int CAPUTREREQUESTCODE = 1;
	private static final int IMGREQUESTCODE = 2;
	private static final int GET_LIST_DATA_FINISHED = 3;
	private static final int FILE_UPLOAD_FINISH = 4;
	private static final int SERVER_LIST_DATA_CHANGE = 5;
	private static final int DOWNLOAD_FINISH = 6;
	private static final int DELETE_FINISH = 8;
	private static String SHARED_FILE_NAME = "BSFile";
	private View view;
	private File file;// 要上传的文件
	private File currentDownloadedFile;// 当前下载的文件
	private ImageButton btn_upload;
	private String userName;
	private SharedPreferences sharedPre;
	private ListView listView;
	private Frag1ListAdapter adapter;
	private ArrayList<HashMap<String[], Bitmap>> listDataForAdapter = new ArrayList<HashMap<String[], Bitmap>>();
	private ArrayList<HashMap<String[], Bitmap>> listDataFromServer;
	private ArrayList<HashMap<String[], String>> listDataFromServerString;
	public Handler handler;

	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment1, null);
		// 初始化fragment
		init();
		return view;
	}

	/**
	 * 初始化fragment1
	 */
	private void init() {

		// 处理子线程的消息
		handler = new Handler(getActivity().getMainLooper()) {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case GET_LIST_DATA_FINISHED:
					// 当完成接收到服务器发送来的上传历史列表的信息listData
					listDataForAdapter.clear();
					listDataForAdapter.addAll(listDataFromServer);
					// 刷新控件显示
					adapter.notifyDataSetChanged();
					break;
				case FILE_UPLOAD_FINISH:
					// 发送文件信息至服务器
					System.out.println("开始发送文件信息>>>>>>>");
					sendFileInfoToServer();
					break;
				case SERVER_LIST_DATA_CHANGE:
					// 文件信息发送完成时,服务器端文件信息已经更新,所以重新读取listData
					loadListViewData();
					break;
				case DOWNLOAD_FINISH:
					// 文件下载完成时,弹出AlertDialog提示用户
					String info = (String) msg.obj;
					showMessageDialog(info);
					// 将下载的文件信息写入到downloadListData中
					refreshDownloadListData();
					break;
				case DELETE_FINISH:
					// 文件删除完成时,弹出AlertDialog提示用户
					String info2 = (String) msg.obj;
					showMessageDialog(info2);
					// 文件删除完成后,服务器端listData文件信息已经更新,所以重新读取listData
					loadListViewData();
					break;
				}
			}
		};
		// 得到Manifest文件中meta-data中的服务器IP地址
		try {
			ApplicationInfo applicationInfo = getActivity().getPackageManager()
					.getApplicationInfo(getActivity().getPackageName(), PackageManager.GET_META_DATA);
			server_ip = applicationInfo.metaData.getString("server_ip");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		// 得到上传按钮
		btn_upload = (ImageButton) view.findViewById(R.id.btn_upload);
		// 设置监听事件
		btn_upload.setOnClickListener(this);
		// 显示上传历史的lsitView控件
		listView = (ListView) view.findViewById(R.id.frag1_list_view);
		// 设置适配器
		adapter = new Frag1ListAdapter(getContext(), listDataForAdapter);
		listView.setAdapter(adapter);
		// 给listView设置点击事件监听器
		listView.setOnItemClickListener(this);
		// 给listView设置滑动监听器
		listView.setOnScrollListener(this);
		// 初始化sharedPreference
		sharedPre = getActivity().getSharedPreferences(SHARED_FILE_NAME, 0);
		// 得到当前用户名
		userName = sharedPre.getString("lastUser", "");
		// 初始化lsitView : 包括从服务器得到数据, 和显示在页面上
		initListView();
	}

	/**
	 * 更新downloadListData,供显示"下载历史"使用
	 * 保存在手机中的名字为"downloadListData.obj"
	 */
	@SuppressWarnings("unchecked")
	protected void refreshDownloadListData() {
		// 得到保存文件的路径
		String absolutePath = getContext().getFilesDir().getAbsolutePath();
		File downloadListDataFile = new File(absolutePath + "/" + "downloadListData.obj");
		// 存储都以String方式存储,若以Bitmap会报错,原因Bitmap为实现Serializable接口
		ArrayList<HashMap<String[], String>> downloadListData ;
		try {
			// 从手机本地中读取该数据文件,获取downloadListData
			ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(downloadListDataFile));
			downloadListData = (ArrayList<HashMap<String[], String>>) objIn.readObject();
			objIn.close();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// 如果是第一次使用,手机中没有该文件,报FileNotFoundException,此时新建一个即可
			downloadListData = new ArrayList<HashMap<String[],String>>();
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			downloadListData = new ArrayList<HashMap<String[],String>>();
		}
		// 获取新下载的文件信息
		String fileName = currentDownloadedFile.getName();
		String filePath = currentDownloadedFile.getAbsolutePath();
		String fileSize = Formatter.formatFileSize(getContext(), currentDownloadedFile.length());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d hh:mm:ss", new Locale("zh_cn"));
		String downloadTime = sdf.format(new Date());

		// 获取当前上传文件的格式
		String[] path_arr = fileName.split("\\.");
		String format = path_arr[path_arr.length - 1];
		// 若果是视频文件的话,用ThumbnailUtils.createVideoThumbnail获取视频的缩略图
		Bitmap thumbnail;
		if ("mp4".equalsIgnoreCase(format)) {
			thumbnail = ThumbnailUtils.createVideoThumbnail(filePath, Images.Thumbnails.MICRO_KIND);
		} else {
			// 否则使用ThumbnailUtils.extractThumbnail获取图片的缩略图
			thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(filePath), 96, 96);
		}
		// 封装ArrayList中的HashMap数据项
		String[] info = new String[] { fileName, fileSize, downloadTime, userName };
		HashMap<String[], Bitmap> HMItem = new HashMap<String[], Bitmap>();
		HMItem.put(info, thumbnail);
		// 创建Bitmap类型ArrayList,添加数据
		ArrayList<HashMap<String[], Bitmap>> listData = new ArrayList<HashMap<String[], Bitmap>>();
		listData.add(HMItem);
		// Bitmap->String 类型转换,并删除重复项,用downloadLsitData接收返回数据
		downloadListData = ListDataUtils.trimListData(BitmapUtils.fromBitmapToString(listData));
		// 将downlListData重新写入到文件中
		try {
			ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(downloadListDataFile));
			objOut.writeObject(downloadListData);
			objOut.flush();
			objOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 点击ListView的item显示出来的气泡窗口
	 */
	private PopupWindow itemPopupWindow = null;

	/**
	 * listView 的item点击事件
	 * 
	 * @param parent
	 * @param view
	 * @param position
	 * @param id
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// 显示之前先把原来的dismiss掉
		dismissPopupWindow();
		// 解析出popupWindow布局
		View contentView = View.inflate(getContext(), R.layout.frag1_list_item_popup_window, null);
		// 得到当前item的位置
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
		// 在线查看 : 根据文件名,从服务器下载至手机, 然后显示给用户
		LinearLayout lookover = (LinearLayout) contentView.findViewById(R.id.lookover);
		// 下载至手机 : 根据文件名, 从服务器下载至手机, 需要将文件信息保存,
		LinearLayout download = (LinearLayout) contentView.findViewById(R.id.download);
		// 从服务器删除 : 根据文件名, 将服务器端文件删除
		LinearLayout delete = (LinearLayout) contentView.findViewById(R.id.delete);

		// 给 "查看" "下载" "删除" 设置监听器,将要操作的文件名传入
		lookover.setOnClickListener(new MyItemClickListener(fileName));
		download.setOnClickListener(new MyItemClickListener(fileName));
		delete.setOnClickListener(new MyItemClickListener(fileName));
	}

	/**
	 * item popupWindow气泡中的的进度圆形转动条
	 * 
	 */
	private ProgressDialog progressDialog = null;

	class MyItemClickListener implements OnClickListener {
		/**
		 * 在用户点击item之后,fileName便会被赋值,为被点击item的fileName
		 */
		private String fileName;

		private String absPath;

		public MyItemClickListener(String fileName) {
			this.fileName = fileName;
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.lookover:
				// 在线查看
				System.out.println(fileName + "查看");
				lookoverOption();
				break;
			case R.id.download:
				// 下载至本地
				System.out.println(fileName + "下载");
				downloadOption();
				break;
			case R.id.delete:
				// 从服务器删除操作
				System.out.println(fileName + "删除");
				deleteOption();
				break;
			}
		}

		// 在线查看操作
		private void lookoverOption() {
			dismissPopupWindow();
			// 1.先弹出ProgressDialog,显示progress, 让界面锁住
			progressDialog = initProgressDialog("请求中,请稍后...");
			progressDialog.show();
			// 2.先从缓存文件夹下查看是否已经缓存:若已经缓存则直接进行下一步,若没有再去服务器请求
			File cacheDir = getContext().getCacheDir();
			// 缓存的绝对路径 : absPath = /data/data/com.xy.mybs/cache
			absPath = cacheDir.getAbsolutePath();
			File f = new File(absPath + "/" + fileName);
			Log.e("isExist", "是否存在" + f.exists());
			// 如果存在,直接打开该文件
			if (f.exists()) {
				// 4.progress消失
				progressDialog.dismiss();
				// 5.显示给用户,新建Activity ,显示媒体文件
				showMediaFile(f);
			} else {
				// 如果不存在,连接服务器请求
				new Thread() {
					public void run() {
						try {
							File file = getMediaFileFromServer(userName+"\\"+fileName, absPath + "/" + fileName);
							Log.e("FilePath", "文件绝对路径>>>::" + file.getAbsolutePath());
							// 4.progress消失
							progressDialog.dismiss();
							// 5.显示下载的媒体文件(.mp4  .jpg),新建Activity显示
							showMediaFile(file);
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				}.start();
			}
		}

		// 下载操作
		private void downloadOption() {
			dismissPopupWindow();
			// 1.先弹出ProgressDialog, 显示progress, 让界面锁住
			progressDialog = initProgressDialog("下载中,请稍后...");
			progressDialog.show();
			// 2.先从服务器得到这个文件,存入本地
			File publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			Log.e("publicDirectory.getAbsolutePath()>>>>>>>>>>>>>", publicDirectory.getAbsolutePath());
			absPath = publicDirectory.getAbsolutePath();
			new Thread() {
				public void run() {
					try {
						// 判断公共下载文件目录下的/userName是否存在,如果不存在则创建
						File userDir = new File(absPath + "/" + userName);
						if (!userDir.exists()) {
							userDir.mkdir();
						}
						// 3.下载该文件
						File file = getMediaFileFromServer(userName+"\\"+fileName, absPath + "/" + userName + "/"+ fileName);
						currentDownloadedFile = file;
						Log.e("FilePath", "文件绝对路径>>>::" + file.getAbsolutePath());
						// 4.progress消失
						progressDialog.dismiss();
						// 6.弹出AlertDialog提示用户
						Message msg = new Message();
						msg.what = DOWNLOAD_FINISH;
						msg.obj = "下载成功\r\n" + absPath + "/" + userName + "/" + fileName;
						handler.sendMessage(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
			}.start();
		}

		// 删除操作
		private void deleteOption() {
			dismissPopupWindow();
			// 1.先弹出ProgressDialog, 显示progress, 让界面锁住
			progressDialog = initProgressDialog("删除中,请稍后...");
			progressDialog.show();
			// 2.将文件名发送至服务器, 服务器端删除该文件名的文件
			new Thread(){
				public void run() {
					boolean isDelete = sendFileNameToServerForDelete(userName + "\\" +fileName);
					// 4.progress消失
					progressDialog.dismiss();
					Message msg = new Message();
					msg.what = DELETE_FINISH;
					if (isDelete) {
						msg.obj = fileName + "\r\n删除成功";
					} else {
						msg.obj = fileName + "\r\n删除失败";
					}
					// 6.将删除结果发送给Handler,并弹出AlertDialog提示用户,通知Handler重新获取listData
					handler.sendMessage(msg);
				};
			}.start();
		}
	}
	/**
	 * 将文件名发送至服务器端,服务器端删除该文件名的文件
	 */
	private boolean sendFileNameToServerForDelete(String userName_fileName) {
		try {
			String ipAddr = "http://" + server_ip + "/BSServer/servlet/DeleteFileServlet";
			// 建立连接设置基本属性
			URL url = new URL(ipAddr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-type", "application/x-java-serialized-object");
			conn.connect();
			Log.e("DELETEFILE", "sendFileNameToServerForDelete是否连接成功");
			// 将封装好的信息写出到服务器
			ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
			objOut.writeObject(userName_fileName);
			objOut.flush();
			objOut.close();

			// 输出服务器端反馈
			ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
			Boolean feedBack = (Boolean) objIn.readObject();
			System.out.println(feedBack + ">>>>>>>>>>>>..DeleteFilFfeedback>>>>>>>>>>>>>>>>>");
			return feedBack.booleanValue();

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	/**
	 * 显示信息提示对话框
	 * @param message 提示框中显示的内容
	 */
	private void showMessageDialog(String message) {
		AlertDialog.Builder builder = new Builder(getContext());
		builder.setTitle("提示");
		builder.setMessage(message);
		builder.setPositiveButton("确定", null);
		builder.create();
		builder.show();
	}

	/**
	 * 根据传入的message, 初始化一个ProgressDialog
	 * 
	 * @param message进度条对话框的提示信息
	 * @return 返回这个进度条对话框
	 */
	private ProgressDialog initProgressDialog(String message) {
		progressDialog = new ProgressDialog(getContext());
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
			Intent intent = new Intent(getActivity(), MyVidioPlayer.class);
			intent.setData(uri);
			startActivity(intent);
		} else {
			// 使用图片浏览器打开
			Intent intent = new Intent(getActivity(), MyImagePlayer.class);
			intent.setData(uri);
			startActivity(intent);
		}
	}

	/**
	 * 从服务器端请求文件
	 * 
	 * @param fileName
	 *            : 要请求的文件名
	 * @param fileAbsPath
	 *            : 得到文件要保存的路径 :例如 "/data/download/example.mp4"
	 * @return 将得到的这个文件已File形式返回给调用者
	 * @throws Exception
	 */
	private File getMediaFileFromServer(String userName_fileName, String fileAbsPath) throws Exception {
		String str = "http://" + server_ip + "/BSServer/servlet/MediaFileDownloadServlet";
		// 得到服务器端请求数据的URL
		URL url = new URL(str);
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
		connection.connect();

		// 将调用者传入的"用户名\\文件名"参数发送至服务器
		ObjectOutputStream osw = new ObjectOutputStream(connection.getOutputStream());
		osw.writeObject(userName_fileName);
		osw.flush();

		// 接收服务器端发来的MediaFile数据
		BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
		File file = new File(fileAbsPath);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
		byte[] buf = new byte[1024];
		int len = 0;
		while ((len = bis.read(buf)) != -1) {
			bos.write(buf, 0, len);
		}
		bos.close();
		bis.close();
		osw.close();
		// 将得到的文件返回
		return file;
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		// 清空缓存目录文件
		File cacheDir = getContext().getCacheDir();
		File[] files = cacheDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			System.gc();
			files[i].delete();
			Log.e("CacheFile", files[i].getAbsolutePath());
		}
	}
	/**
	 * 将listView初始化:从服务器端请求该用户的上传历史文件信息,并显示在控件上
	 * 简单说就是新建一个子线程,获取服务器端的listView
	 */
	private void initListView() {
		// 新建线程从服务器获取listData
		new Thread() {
			@Override
			public void run() {
				try {
					// 从服务器端请求data
					loadListViewData();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * 从服务器端加载用来显示在listView中的数据,并通过handler刷新listView内容
	 * listDataFromServer : 将返回的结果传值给全局变量listDataFromServer
	 * 
	 */
	private void loadListViewData() {
		new Thread() {
			@Override
			public void run() {
				try {
					// 从服务器端请求data
					String str = "http://" + server_ip + "/BSServer/servlet/GetListViewDataServlet";
					// 得到服务器端请求数据的URL
					URL url = new URL(str);
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
					connection.connect();

					// 得到当前用户额用户名,并将当前用户名发送给服务器
					ObjectOutputStream osw = new ObjectOutputStream(connection.getOutputStream());
					osw.writeObject(userName);
					osw.flush();
					osw.close();
					// 接收服务器端发来的listData数据
					ObjectInputStream objIn = new ObjectInputStream(connection.getInputStream());
					listDataFromServerString = (ArrayList<HashMap<String[], String>>) objIn.readObject();
					objIn.close();
					// 将String转化成为Bitmap
					listDataFromServer = BitmapUtils.fromStringToBitmap(listDataFromServerString);
					// 创建message，发送给handler，更新主线程view
					Message msg = new Message();
					msg.what = GET_LIST_DATA_FINISHED;
					handler.sendMessage(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * 上传按钮监听事件, 和三个上传类型按钮的监听事件
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_upload:// 上传按钮点击
			btnUploadPressed();
			break;
		case R.id.btn_tupian_upload:// 图片上传
			btnTuPianUploadPressed();
			break;
		case R.id.btn_bendi_upload:// 本地视频上传
			btnPaiSheUploadPressed();
			break;
		case R.id.btn_paishe_upload:// 拍摄视频上传
			btnBenDiUploadPressed();
			break;
		}
	}

	/**
	 * 页面最低侧的上传按钮点击后, 弹出的PopupWindow
	 */
	private PopupWindow btnPopupWindow;

	/**
	 * 点击页面底部的上传按钮，弹出选择上传类型的popupWindow
	 */
	@SuppressLint("InflateParams")
	private void btnUploadPressed() {
		dismissPopupWindow();
		// 得到popupWindow的显示视图
		View view = LayoutInflater.from(getContext()).inflate(R.layout.popupwindow, null);
		// 得到三种上传方式的控件
		ImageButton btn_tupian_upload = (ImageButton) view.findViewById(R.id.btn_tupian_upload);
		ImageButton btn_bendi_upload = (ImageButton) view.findViewById(R.id.btn_bendi_upload);
		ImageButton btn_paishe_upload = (ImageButton) view.findViewById(R.id.btn_paishe_upload);
		// 给三种上传控件添加监听事件
		btn_tupian_upload.setOnClickListener(this);
		btn_bendi_upload.setOnClickListener(this);
		btn_paishe_upload.setOnClickListener(this);
		// 初始化popupWindow,必须设置背景
		btnPopupWindow = new PopupWindow(view, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
		btnPopupWindow.setTouchable(true);// 默认为true
		// 使用Animation必须给里面的view设置,不能给popupWindow设置
		// view.setAnimation(animation);
		btnPopupWindow.setAnimationStyle(R.style.PopupAnimation);
		btnPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// 点击外部不消失
		// BtnpPpupWindow.setOutsideTouchable(false); //不好使
		// 设置弹出的位置
		btnPopupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
	}

	/**
	 * 本地视频文件上传按钮点击事件监听
	 */
	private void btnBenDiUploadPressed() {
		btnPopupWindow.dismiss();
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(intent, CAPUTREREQUESTCODE);
	}

	/**
	 * 拍摄视频文件上传按钮点击事件监听
	 */
	private void btnPaiSheUploadPressed() {
		btnPopupWindow.dismiss();
		Intent intentOpen = new Intent(Intent.ACTION_GET_CONTENT);
		intentOpen.setType("video/*");
		startActivityForResult(intentOpen, OPENREQUESTCODE);
	}

	/**
	 * 图片文件上传按钮点击事件监听
	 */
	private void btnTuPianUploadPressed() {
		btnPopupWindow.dismiss();
		Intent intentImg = new Intent(Intent.ACTION_GET_CONTENT);
		intentImg.setType("image/*");
		startActivityForResult(intentImg, IMGREQUESTCODE);
	}

	/**
	 * mediaFilePath ; 当前上传文件的路径 
	 * mediaFilePathForInfo : 传输信息生成缩略图和得到文件名使用的路径
	 */
	private String mediaFilePath;
	private String mediaFilePathForInfo;

	/**
	 * 打开或者拍摄的返回Activity onActivityResult方法
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// 打开视频和选择视频都为空时
		if (data == null) {
			if (mediaFilePath == null) {
				// 告知用户未选择文件
				Toast.makeText(getContext(), R.string.no_select_video, Toast.LENGTH_SHORT).show();
			}
			return;
		} else {
			Uri uri = data.getData();
			// 拍摄视频
			if (requestCode == CAPUTREREQUESTCODE) {
				if (resultCode == Activity.RESULT_OK) {
					mediaFilePath = getVideoPath(uri);
					mediaFilePathForInfo = mediaFilePath;
					// 将数据发送至服务器
					sendToServer();
				} else if (resultCode == Activity.RESULT_CANCELED) {
					Toast.makeText(getContext(), R.string.no_capture_video, Toast.LENGTH_SHORT).show();
				}
			} else if (requestCode == OPENREQUESTCODE) {
				if (resultCode == Activity.RESULT_OK) {
					mediaFilePath = uri.getPath();
					if (uri.getScheme().startsWith("content")) {
						mediaFilePath = getVideoPath(uri);
					}
					mediaFilePathForInfo = mediaFilePath;
					// 将数据发送至服务器
					sendToServer();
				}
			} else if (requestCode == IMGREQUESTCODE) {
				if (resultCode == Activity.RESULT_OK) {
					mediaFilePath = uri.getPath();
					if (uri.getScheme().startsWith("content")) {
						mediaFilePath = getVideoPath(uri);
					}
					mediaFilePathForInfo = mediaFilePath;
					// 将数据发送至服务器
					sendToServer();
				}
			}
		}
	}

	/**
	 * 转换uri
	 * 
	 * @param uri
	 * @return
	 */
	protected String getVideoPath(Uri uri) {
		Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
		if (cursor.moveToFirst()) {
			mediaFilePath = cursor.getString(cursor.getColumnIndex("_data"));
		}
		return mediaFilePath;
	}

	/**
	 * 将数据发送到服务器
	 */
	private void sendToServer() {
		MyAsyncTask myAsyncTask = new MyAsyncTask();
		myAsyncTask.execute("http://" + server_ip + "/BSServer/servlet/ReceiveFileData");
	}

	/**
	 * 异步任务线程
	 * 
	 * @author Administrator
	 *
	 */
	public class MyAsyncTask extends AsyncTask<String, Integer, String> {

		String message = null;
		ProgressDialog mProgressDialog = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mProgressDialog = new ProgressDialog(getContext());
			mProgressDialog.setCancelable(true);
			mProgressDialog.setMessage("文件上传中...");
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setProgress(0);
			mProgressDialog.setMax(100);
			mProgressDialog.show();
		}

		/**
		 * 更新上传进度条
		 * 
		 * @param values
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if (values[0] < 100) {
				mProgressDialog.setProgress(values[0]);
			}
		}

		/**
		 * 后台任务完成时执行此方法
		 * 
		 * @param s
		 */
		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			mProgressDialog.dismiss();
			// 如果上传成功,则置空mediaFilePath,供下次上传文件使用
			if (s.startsWith("上传成功")) {
				mediaFilePath = null;
			}
			// 弹出对话框提示用户:上传成功或是失败
			new AlertDialog.Builder(getContext()).setMessage(s).setTitle(R.string.resulttip)
					.setPositiveButton(R.string.alert_dialog_btn_text, null).setCancelable(false).show();
		}

		/**
		 * 后台主要操作
		 * 
		 * @param params
		 * @return
		 */
		@Override
		protected String doInBackground(String... params) {
			try {
				file = new File(mediaFilePath);
				// 创建要传输的数据
				String userName_fileName = userName + "\\" + file.getName();
				Log.e("FilePathToUpdate", mediaFilePath + "<<<<<<");
				if (file.exists()) {
					// 读取媒体文件
					FileInputStream fis = new FileInputStream(file);
					// 创建连接并设置必要参数
					URL url = new URL(params[0]);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setDoOutput(true);
					connection.setDoInput(true);
					connection.setConnectTimeout(3000);
					connection.setReadTimeout(3000);
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-type", "application/x-java-serialized-object");

					OutputStream out = connection.getOutputStream();
					BufferedOutputStream bufo = new BufferedOutputStream(out);
					// 将"用户名\\文件名"写入到服务器
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufo);
					objectOutputStream.writeObject(userName_fileName);
					objectOutputStream.flush();
					// 刷新之后继续传输文件数据
					int total = fis.available();
					byte[] buf = new byte[1024];
					int count = 0;
					int len;
					while ((len = fis.read(buf)) != -1) {
						bufo.write(buf, 0, len);
						count += len;
						publishProgress((int) ((count / (float) total) * 100));
					}
					bufo.flush();
					fis.close();
					// bufo.close();
					// 读取服务器返回信息
					InputStream in = connection.getInputStream();
					BufferedReader bufr = new BufferedReader(new InputStreamReader(in));
					while ((message = bufr.readLine()) != null) {
						if (message.startsWith("OK")) {
							// 文件发送成功,发送消息至handler,让其发送fileInfo
							Message msg = new Message();
							msg.what = FILE_UPLOAD_FINISH;
							handler.sendMessage(msg);
							return "上传成功！";
						} else if (message.endsWith("NO")) {
							return "上传失败，请重试！";
						}
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return "出错了，请重试！";
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return "出错了，请重试！";
			} catch (ProtocolException e) {
				e.printStackTrace();
				return "出错了，请重试！";
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				return "网络连接超时";
			} catch (IOException e) {
				e.printStackTrace();
				return "出错了，请重试！";
			}
			return "未知错误，请重试！";
		}
	}

	/**
	 * 将文件的信息发送至服务器 ,包括:fileName fileSize uploadTime
	 */
	public void sendFileInfoToServer() {
		new Thread() {
			public void run() {
				String[] pathArr = mediaFilePathForInfo.split("/");
				String fileName = pathArr[pathArr.length - 1];
				Log.e("FILEPATH", fileName);
				String fileSize = Formatter.formatFileSize(getContext(), file.length());
				Log.e("FILEPATH", fileSize);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d hh:mm:ss", new Locale("zh_cn"));
				String uploadTime = sdf.format(new Date());
				Log.e("FILEPATH", uploadTime);
				// filePath表示视频文件路径
				// kind表示类型，可以有两个选项，
				// 分别是Images.Thumbnails.MICRO_KIND和Images.Thumbnails.MINI_KIND，
				// 其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96,
				// 当然读了代码你会发现，你也可以传入任意的int型数字，
				// 只是就不会对获取的bitmap进行相关的设置，
				// 我们可以自己使用extractThumbnail( Bitmap source, int width, int
				// height)
				// 方法对返回的bitmap进行相关设置。

				// 获取当前上传文件的格式
				String[] path_arr = fileName.split("\\.");
				String format = path_arr[path_arr.length - 1];
				// 若果是视频文件的话,用ThumbnailUtils.createVideoThumbnail获取视频的缩略图
				Bitmap thumbnail;
				if ("mp4".equalsIgnoreCase(format)) {
					thumbnail = ThumbnailUtils.createVideoThumbnail(mediaFilePathForInfo, Images.Thumbnails.MICRO_KIND);
				} else {
					// 否则使用ThumbnailUtils.extractThumbnail获取图片的缩略图
					thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(mediaFilePathForInfo), 96, 96);
				}
				// 将传输使用的文件路径置空,供下次使用,生成缩略图需要使用该路径
				mediaFilePathForInfo = null;
				// 封装数据
				String[] info = new String[] { fileName, fileSize, uploadTime, userName };
				HashMap<String[], Bitmap> fileInfo = new HashMap<String[], Bitmap>();
				// 得到封装数据fileInfo
				fileInfo.put(info, thumbnail);
				ArrayList<HashMap<String[], Bitmap>> listData = new ArrayList<HashMap<String[], Bitmap>>();
				listData.add(fileInfo);
				// 将Bitmap转化成String
				ArrayList<HashMap<String[], String>> listData_Stirng = BitmapUtils.fromBitmapToString(listData);
				// 获得上传地址http://localhost:8080/BSServer/servlet/ReceiveFileInfoServlet
				String ipAddr = "http://" + server_ip + "/BSServer/servlet/ReceiveFileInfoServlet";
				try {
					// 建立连接设置基本属性
					URL url = new URL(ipAddr);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setDoOutput(true);
					conn.setDoInput(true);
					conn.setUseCaches(false);
					conn.setConnectTimeout(3000);
					conn.setReadTimeout(3000);
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-type", "application/x-java-serialized-object");
					conn.connect();
					Log.e("FILEPATH", "是否连接成功");
					// 将封装好的信息写出到服务器
					ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
					objOut.writeObject(listData_Stirng);
					objOut.flush();
					objOut.close();
					Log.e("FILEPATH", "是否发送成功");

					// 输出服务器端反馈
					ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
					String feedBack = (String) objIn.readObject();
					System.out.println(feedBack + ">>>>>>>>>>>>..feedback>>>>>>>>>>>>>>>>>");
					Message msg = new Message();
					msg.what = SERVER_LIST_DATA_CHANGE;
					handler.sendMessage(msg);

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		dismissPopupWindow();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		dismissPopupWindow();
		return true;
	}
}
