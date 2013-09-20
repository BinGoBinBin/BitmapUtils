package com.bingobinbin.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.PrivateCredentialPermission;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.bingobinbin.mulThread.FileDownloader;

public class BitmapUtils {

	private static String SAVE_PATH = "/mnt/sdcard/";
	/** 文件夹大小限制为30M */
	public static final int CACHE_SIZE_LIMIT = 30 * 1024 * 1024;

	/**
	 * <p>
	 * 
	 * @Title: getFileName
	 *         </p>
	 *         <p>
	 * @Description:根据url获取图片名
	 * 
	 *                         </p>
	 * 
	 * @param url
	 *            下载图片的url
	 * @return
	 * @throws
	 */
	public static String getFileName(String url) {
		String filePath = url;
		if (url.startsWith("http://") || url.startsWith("https://")) {
			filePath = SAVE_PATH + Helper.md5(url);
		}
		return filePath;
	}

	/**
	 * <p>
	 * 
	 * @Title: getBitmapStream
	 *         </p>
	 *         <p>
	 * @Description:根据url获取图片的Inputstream
	 * 
	 *                                    </p>
	 * 
	 * @param url
	 * @return
	 * @throws
	 */
	public static InputStream getBitmapStream(String url) {
		InputStream is = null;
		try {
			try {
				is = new FileInputStream(new File(getFileName(url)));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (is == null || is.available() <= 0) {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("BitmapUtil", "读取图片流出错" + e.toString());
		}
		return is;
	}

	/**
	 * <p>
	 * 
	 * @Title: saveBitmap
	 *         </p>
	 *         <p>
	 * @Description:
	 * 
	 *               </p>
	 * 
	 * @param url
	 * @param mBitmap
	 * @throws
	 */
	public static void saveBitmap(String url, Bitmap mBitmap) {
		if(mBitmap == null){
			return ;
		}
		int bitmapSize = mBitmap.getRowBytes() * mBitmap.getHeight();
		checkStorageAvailable(bitmapSize);
		try {
			BufferedOutputStream outputStream = new BufferedOutputStream(
					new FileOutputStream(getFileName(url)));
			int quality = 100;
			if (bitmapSize > CACHE_SIZE_LIMIT) {
				quality = 80;
			}
			mBitmap.compress(CompressFormat.PNG, quality, outputStream);
			outputStream.flush();
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * 
	 * @Title: getScaleBitmapOptions
	 *         </p>
	 *         <p>
	 * @Description: 获取图片的Option
	 * 
	 *               </p>
	 * 
	 * @param url
	 * @param width
	 * @param height
	 * @return
	 * @throws
	 */
	private static BitmapFactory.Options getScaleBitmapOptions(String url,
			int width, int height) {
		InputStream inputStream = getBitmapStream(url);
		if (inputStream == null) {
			return null;
		}
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
		bmpFactoryOptions.inJustDecodeBounds = true;
		try {
			BitmapFactory.decodeStream(inputStream, null, bmpFactoryOptions);

			int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight
					/ height);
			int widthRatio = (int) Math
					.ceil(bmpFactoryOptions.outWidth / width);

			/*
			 * If both of the ratios are greater than 1, one of the sides of the
			 * image is greater than the screen
			 */
			if (heightRatio > 1 && widthRatio > 1) {
				if (heightRatio > widthRatio) {
					// Height ratio is larger, scale according to it
					bmpFactoryOptions.inSampleSize = heightRatio;
				} else {
					// Width ratio is larger, scale according to it
					bmpFactoryOptions.inSampleSize = widthRatio;
				}
			}

			// Decode it for real
			bmpFactoryOptions.inJustDecodeBounds = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 关闭java 层的stream
		closeInputStream(inputStream);

		return bmpFactoryOptions;
	}

	/**
	 * <p>
	 * 
	 * @Title: closeInputStream
	 *         </p>
	 *         <p>
	 * @Description:关闭 InputStream
	 * 
	 *                 </p>
	 * 
	 * @param inputStream
	 * @throws
	 */
	private static void closeInputStream(InputStream inputStream) {
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * 
	 * @Title: closeOutputStream
	 *         </p>
	 *         <p>
	 * @Description: 关闭OutputStream
	 * 
	 *               </p>
	 * 
	 * @param outputStream
	 * @throws
	 */
	private static void closeOutputStream(OutputStream outputStream) {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * 
	 * @Title: downloadBitmap
	 *         </p>
	 *         <p>
	 * @Description: 根据 url重网络上获取图片
	 * 
	 *               </p>
	 * 
	 * @param url
	 *            图片url
	 * @param width
	 *            图片宽
	 * @param height
	 *            图片高
	 * @return
	 * @throws
	 */
	public static Bitmap downloadBitmap(String url, int width, int height) {
		Bitmap bitmap = null;
		try {
			InputStream inputStream = new URL(url).openStream();
			OutputStream outputStream = new FileOutputStream(new File(getFileName(url)));
			byte[] buffer = new byte[1024];
			int len = 0;
			while ( (len = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer,0,len);
			}
			outputStream.flush();
			closeOutputStream(outputStream);
			closeInputStream(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bitmap = BitmapFactory.decodeStream(getBitmapStream(url), null,
				getScaleBitmapOptions(url, width, height));
		return bitmap;
	}

	public static Bitmap downloadBitmap(Context context,String url,int width,int height){
    	Bitmap bitmap = null;
    	FileDownloader downloader = new FileDownloader(context, url, 3);
    	try {
			downloader.download(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	while( !downloader.isFinish()  ){
    		try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
		bitmap = BitmapFactory.decodeStream(getBitmapStream(url), null,
                getScaleBitmapOptions(url, width, height));
		return bitmap;
    }

	/**
	 * <p>
	 * 
	 * @Title: getBitmapFromSDCard
	 *         </p>
	 *         <p>
	 * @Description: 从SD卡上获取图片
	 * 
	 *               </p>
	 * 
	 * @param url
	 *            图片url
	 * @param width
	 *            图片宽
	 * @param height
	 *            图片高
	 * @return
	 * @throws
	 */
	public static Bitmap getBitmapFromSDCard(String url, int width, int height) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File(getFileName(url)));
			if (inputStream != null && inputStream.available() > 0) {
				Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null,
						getScaleBitmapOptions(url, width, height));
				return bitmap;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * <p>
	 * 
	 * @Title: setSaveDirectory
	 *         </p>
	 *         <p>
	 * @Description:设置缓存目录
	 * 
	 *                     </p>
	 * 
	 * @param context
	 * @throws
	 */
	public static void setCacheDirectory(Context context) {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED) && context != null) {
//			try {
//				PackageInfo packageItemInfo = context.getPackageManager()
//						.getPackageInfo(context.getPackageName(), 0);
//				if (!TextUtils.isEmpty(packageItemInfo.packageName)) {
//					SAVE_PATH = packageItemInfo.packageName
//							.replace(".", "/") + "/";
//					new File(SAVE_PATH).mkdirs();
//				}
//			} catch (NameNotFoundException e) {
//				e.printStackTrace();
//			}
//			
			String packageName = context.getPackageName();
			if(TextUtils .isEmpty(packageName)){
				SAVE_PATH = packageName.replace(".", "/")+"/";
				File file = new File(SAVE_PATH);
				if(!file.exists()){
					file.mkdirs();
				}
			}
		} else {
			SAVE_PATH = "/data/data/";
		}
	}

	/**
	 * <p>
	 * 
	 * @Title: getDirSize
	 *         </p>
	 *         <p>
	 * @Description:获取缓存目录的容量
	 * 
	 *                        </p>
	 * 
	 * @param file
	 * @return
	 * @throws
	 */
	private static long getDirSize(File file) {
		long total = 0;
		if (file.exists()) {
			if (file.isFile()) {
				return file.length();
			} else if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					total += getDirSize(f);
				}
			}
		}
		return total;
	}

	/**
	 * <p>
	 * 
	 * @Title: checkStorageAvailable
	 *         </p>
	 *         <p>
	 * @Description:检查存储空间是否足够 </p>
	 * 
	 * @param bitmapSize
	 * @throws
	 */
	private static void checkStorageAvailable(long bitmapSize) {
		long dirSize = getDirSize(new File(SAVE_PATH));
		if (dirSize + bitmapSize > CACHE_SIZE_LIMIT) {
			Map<Long, String> map = getFilePathAndModyTime(new File(SAVE_PATH));
			long deleteSize = 0;
			long size = (int) (dirSize * 0.3);
			File file = null;
			for (Long time : map.keySet()) {
				file = new File(map.get(time));
				deleteSize += file.length();
				if (deleteSize < size) {
					file.delete();
				} else {
					break;
				}
			}
		}
	}

	/**
	 * <p>
	 * 
	 * @Title: getFilePathAndModyTime
	 *         </p>
	 *         <p>
	 * @Description:获取文件绝对路径跟修改时间 </p>
	 * 
	 * @param file
	 * @return
	 * @throws
	 */
	private static Map<Long, String> getFilePathAndModyTime(File file) {
		Map<Long, String> map = new HashMap<Long, String>();
		if (file.isFile()) {
			map.put(file.lastModified(), file.getAbsolutePath());
		} else if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				map.putAll(getFilePathAndModyTime(f));
			}
		}
		return map;
	}
}
