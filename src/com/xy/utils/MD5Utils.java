package com.xy.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5加密方式
 * @author Administrator
 *
 */
public class MD5Utils {
	public static String md5Digest(String password) throws NoSuchAlgorithmException {
		StringBuilder sb = new StringBuilder();
		// 1. 获取摘要器
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		// 2.得到密码的摘要 为byte数组
		byte[] digest = messageDigest.digest(password.getBytes());
		// 3.遍历数组
		for (int i = 0; i < digest.length; i++) {
			// 4.MD5加密
			int result = digest[i] & 0xff;
			// 转化为16进制
			String hexString = Integer.toHexString(result) + 1;
			if(hexString.length() < 2){
				sb.append("0");
			}
			sb.append(hexString);
		}
		
		return sb.toString();
	}
}
