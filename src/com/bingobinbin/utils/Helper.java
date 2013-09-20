package com.bingobinbin.utils;

import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.DisplayMetrics;

/**
 * Provides utility methods that can be used to perform common operations. 
 * 
 * @author Jhen
 *
 */
public class Helper {
	
	public static final String LINE_SEPARATOR =  System.getProperty("line.separator");
	//screen[0]:宽，screen[1]:高
	private static int[] screen = null;
	/**
	 *  MD5 加密
	 * @param str
	 * @return 32位16进制 密文 或 ""
	 */
	public static String md5(String str) {
		if (str == null) {
		    return null;
		}
		try {
			byte[] defaultBytes = str.getBytes();
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte[] messageDigest = algorithm.digest();
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append(String.format("%02X", messageDigest[i]));
			}

			return hexString.toString();

		} catch (Exception e) {
			return str.replaceAll("[^[a-z][A-Z][0-9][.][_]]", "");
		}
	}

	/**
	 * Get the literal description of the size.
	 * <p>
	 * 单位 B  精度 0
	 * </p>
	 * <p>
	 * 单位 K  精度 0
	 * </p>
	 * <p>
	 * 单位 M  精度 0.0
	 * </p>
	 * <p>
	 * 单位 G  精度 0.00
	 * </p>
	 * 
	 * @param bytes
	 * @return The literal description, such as "Size: 54K" 
	 */
	public static String getFileSizeDescription(Context context, long bytes) {
		String value = "";
		if (bytes < 1000) {
			value = (int) bytes + "B";
		} else if (bytes < 1000000) {
			value = Math.round(bytes / 1000.0) + "K";
		} else if (bytes < 1000000000) {
			DecimalFormat df = new DecimalFormat("#0.0");
			value = df.format(bytes / 1000000.0) + "M";
		} else {
			DecimalFormat df = new DecimalFormat("#0.00");
			value = df.format(bytes / 1000000000.0) + "G";
		}
		return value;
	}
	
	/**
	 * 根据 packageName 打开对应的app
	 * @param context
	 * @param packageName 对应app Manifest package
	 */
	public static void openApp(Context context, String packageName) {
		PackageManager pm = context.getPackageManager();
		Intent i = pm.getLaunchIntentForPackage(packageName);
		context.startActivity(i);
	}

	/**
	 *  调用系统中能展示 uri 对应资源的组件展示 uri 资源
	 *  如：uri = "http://www.google.com" 是一个 http:// 协议的 uri 则调用系统中浏览器打开Google 主页
	 * @param context
	 * @param uri 
	 * @return
	 */
	public static boolean openUrlSchema(Context context, String uri) {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
			context.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Returns true if the string is null or 0-length.
	 * 
	 * @param str
	 *            the string to be examined
	 * @return true if str is null or zero length
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	/**
	 * Determine if a baseUrl string is in absolute or relative form. A empty string
	 * "" or null para will return false.
	 * 
	 * @param url
	 *            url string.
	 * @return true if it is a absolute path starting with "http://" or
	 *         "https://", case ignored.
	 */
	public static boolean isAbsoluteUrl(String url) {
		if (isEmpty(url)) {
		    return false;
		}
		else {
			String lowerUrl = url.trim().toLowerCase();
			return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
		}
	}
	
	/**
	 * 以 yyyy-MM-dd HH:mm:ss 格式化输出当前时间
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public static String getDateTime() {	
		Date date = new Date();
		return getTimeString(date);
	}
	

	/**
	 * 以 yyyy-MM-dd HH:mm:ss 格式化输出时间
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public static String getTimeString(Date date) {
		if(date==null) {
			return "";
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");		
		return dateFormat.format(date);
	}
	
	/**
	 * <p>@Title: getScreenWidthHeight</p>
	 * <p>@Description: 获取屏幕的分辨率 
	 *
	 * </p> 
	 * @param context 上下文
	 * @return  返回屏幕的宽高。screen[0]:宽，screen[1]:高
	 * @throws
	 */
	public static int[] getScreenWidthHeight(Context context){
		if(screen != null){
			return screen;
		}
		synchronized (Helper.class) {
			DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
			screen = new int[2];
			screen[0] = displayMetrics.widthPixels;
			screen[1] = displayMetrics.heightPixels;
			return screen;
		}
	}
}
