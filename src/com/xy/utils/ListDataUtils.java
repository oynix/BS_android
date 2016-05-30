package com.xy.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class ListDataUtils {
	/**
	 * 删除重复记录
	 * 
	 */
	public static ArrayList<HashMap<String[], String>> trimListData(ArrayList<HashMap<String[], String>> listData) {
		System.out.println("IndexUtils>trimArray>listData.size():" + listData.size());
		for (int i = 0; i < listData.size(); i++) {
			// 得到当前的HashMap
			HashMap<String[], String> temp = listData.get(i);
			String[] info = temp.keySet().iterator().next();
			// 得到文件名和用户名
			String fileName = info[0];
			String user = info[3];
			// 从当前位置的下一个位置开始查找
			for (int j = i + 1; j < listData.size(); j++) {
				// 得到当前的HashMap
				HashMap<String[], String> temp2 = listData.get(j);
				String[] info2 = temp2.keySet().iterator().next();
				// 得到文件名和用户名
				String fileName2 = info2[0];
				String user2 = info2[3];
				// 作出判断,如果相同,则删掉前面的,保留最新的
				if (fileName.equals(fileName2) && user.equals(user2)) {
					listData.remove(i);
					i--;
					break;
				}
			}
		}
		return listData;
	}
}
