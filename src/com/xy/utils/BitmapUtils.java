package com.xy.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public class BitmapUtils {

	/**
	 * 将bitmap转化成String
	 * 
	 * @param listData
	 * @return
	 */
	public static ArrayList<HashMap<String[], String>> fromBitmapToString(
			ArrayList<HashMap<String[], Bitmap>> listData) {
		// 转化后的listDataAfter
		ArrayList<HashMap<String[], String>> listDataAfter = new ArrayList<HashMap<String[],String>>();
		for (int i = 0; i < listData.size(); i++) {
			// 得到当前的HashMap
			HashMap<String[],Bitmap> hashMap = listData.get(i);
			// 得到key, value
			Bitmap bitmap = hashMap.values().iterator().next();
			String[] info = hashMap.keySet().iterator().next();
			// 将Bitmap转化成String
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
			byte[] temp = Base64.encode(baos.toByteArray(), Base64.DEFAULT);
			String bp = new String(temp);
			// 将String[],和String添加到listData
			HashMap<String[],String> hashMap2 = new HashMap<String[], String>();
			hashMap2.put(info, bp);
			listDataAfter.add(hashMap2);
		}
		return listDataAfter;
	}
	
	/**
	 * 将String转化成Bitmap
	 * @param listData
	 * @return
	 */
	public static ArrayList<HashMap<String[], Bitmap>> fromStringToBitmap(
			ArrayList<HashMap<String[], String>> listData) {
		// 转化后的listDataAfter
		ArrayList<HashMap<String[], Bitmap>> listDataAfter = new ArrayList<HashMap<String[],Bitmap>>();
		for (int i = 0; i < listData.size(); i++) {
			// 得到当前的HashMap
			HashMap<String[],String> hashMap = listData.get(i);
			// 得到key, value
			String bp = hashMap.values().iterator().next();
			String[] info = hashMap.keySet().iterator().next();
			// 将String转化成Bitmap
			byte[] temp = Base64.decode(bp, Base64.DEFAULT);
			Bitmap bitmap = BitmapFactory.decodeByteArray(temp, 0, temp.length);
			// 将String[],和Bitmap添加到listData
			HashMap<String[],Bitmap> hashMap2 = new HashMap<String[], Bitmap>();
			hashMap2.put(info, bitmap);
			listDataAfter.add(hashMap2);
		}
		return listDataAfter;
	}
	
	
}
