package com.bingobinbin.utils;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.ImageView;

public class BitmapManager {

	private static HashMap<String, SoftReference<Bitmap>> cache;
	private static ExecutorService pool;
	private static Map<ImageView, String> imageViews;
	private Bitmap defaultBmp;
	private Context mContext;
	private static int[] screen;
	//加载图片并且加载成功后显示在ImageView
	private static final int BITMAP_RESERSH = 0x01;
	//仅仅加载图片
	private static final int BITMAP_LOAD = 0x02;
	@SuppressLint("HandlerLeak")
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BITMAP_RESERSH:
				if(msg.obj instanceof Holder){
					Holder holder = (Holder) msg.obj;
					String tag = imageViews.get(holder.mImageView);
					if (tag != null && tag.equals(holder.url)) {
						if (msg.obj != null) {
							holder.mImageView.setImageBitmap(holder.mBitmap);
							cache.put(Helper.md5(holder.url), new SoftReference<Bitmap>(holder.mBitmap));
							// 向SD卡中写入图片缓存
//							BitmapUtils.saveBitmap(holder.url, holder.mBitmap);
							
						}
					}
				}
				break;

			case BITMAP_LOAD:
				
				break;
			}
		}
	};
	static {
		cache = new HashMap<String, SoftReference<Bitmap>>();
		pool = Executors.newFixedThreadPool(5); // 固定线程池
		imageViews = Collections
				.synchronizedMap(new WeakHashMap<ImageView, String>());
	}
	
	public BitmapManager(Context context) {
		this.mContext = context;
		init(mContext);
	}

	public BitmapManager(Bitmap def,Context context) {
		this.defaultBmp = def;
		this.mContext = context;
		init(mContext);
	}

	/**
	 * <p>@Title: loadBitmap</p>
	 * <p>@Description:根据图片的宽高加载图片资源 
	 * </p> 
	 * @param url 图片url
	 * @param width 图片宽
	 * @param height 图片高
	 * @return  返回图片
	 * @throws
	 */
	public Bitmap loadBitmap(String url, int width, int height) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
		// 判断传入的宽高
		if (width < 0 || height < 0) {
			width = width < 0 ? height : width;
			height = height < 0 ? width : height;
			width = width < 0 ? screen[0] : width;
			height = height < 0 ? screen[1] : height;
		}
		// 从缓存中获取Bitmap
		Bitmap bitmap = getBitmapFromCache(url, width, height);
		if (bitmap != null) {
			return bitmap;
		}

		// 从网络上加载图片
		bitmap = downloadBitmap(url, width, height);
		if (bitmap != null) {
			cache.put(Helper.md5(url), new SoftReference<Bitmap>(bitmap));
//			BitmapUtils.saveBitmap(url, bitmap);
		}
		return bitmap;
	}

	/**
	 * 设置默认图片
	 * 
	 * @param bmp
	 */
	public void setDefaultBmp(Bitmap bmp) {
		defaultBmp = bmp;
	}

	/**
	 * 加载图片
	 * 
	 * @param url
	 * @param imageView
	 */
	public void loadBitmap(String url, ImageView imageView) {
		loadBitmap(url, imageView, this.defaultBmp, screen[0], screen[1]);
	}

	/**
	 * 加载图片-可设置加载失败后显示的默认图片
	 * 
	 * @param url
	 * @param imageView
	 * @param defaultBmp
	 */
	public void loadBitmap(String url, ImageView imageView, Bitmap defaultBmp) {
		loadBitmap(url, imageView, defaultBmp, screen[0], screen[1]);
	}

	/**
	 * 加载图片-可指定显示图片的高宽
	 * 
	 * @param url
	 * @param imageView
	 * @param width
	 * @param height
	 */
	private void loadBitmap(String url, ImageView imageView, Bitmap defaultBmp,
			int width, int height) {
		imageViews.put(imageView, url);
		Bitmap bitmap = getBitmapFromCache(url, width, height);

		if (bitmap != null) {
			// 显示缓存图片
			imageView.setImageBitmap(bitmap);
		} else {
			// 线程加载网络图片
			imageView.setImageBitmap(defaultBmp);
			queueJob(url, imageView, width, height);
		}
	}

	/**
	 * <p>
	 * @Title: getBitmapFromCache
	 * </p>
	 * <p>
	 * @Description:从缓存中获取图片
	 * </p>
	 * @param url
	 *            图片的URL
	 * @param width
	 *            图片宽
	 * @param height
	 *            图片高
	 * @return Bitmap
	 * @throws
	 */
	private Bitmap getBitmapFromCache(String url, int width, int height) {
		Bitmap bitmap = null;
		// 从内存中获取
		if (cache.containsKey(Helper.md5(url))) {
			bitmap = cache.get(Helper.md5(url)).get();
			if (bitmap != null) {
				return bitmap;
			}
		}
		// 从SD卡上获取
		bitmap = BitmapUtils.getBitmapFromSDCard(url, width, height);
		if (bitmap != null) {
			cache.put(Helper.md5(url), new SoftReference<Bitmap>(bitmap));
		}
		return bitmap;
	}

	/**
	 * 从网络中加载图片
	 * 
	 * @param url
	 * @param imageView
	 * @param width
	 * @param height
	 */
	public void queueJob(final String url, final ImageView imageView,
			final int width, final int height) {
		// 将图片下载任务加入到线程池
		pool.execute(new Runnable() {
			public void run() {
				Message message = Message.obtain();
				Bitmap bitmap = downloadBitmap(url, width, height);
				Holder holder = new Holder(bitmap, url, imageView);
				message.obj = holder;
				message.what = BITMAP_RESERSH;
				handler.sendMessage(message);
			}
		});
	}

	/**
	 * <p>
	 * @Title: downloadBitmap
	 * </p>
	 * <p>
	 * @Description: 从网络上获取图片
	 * 
	 * </p>
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
	private Bitmap downloadBitmap(String url, int width, int height) {
//		return BitmapUtils.downloadBitmap(url, width, height);
		return BitmapUtils.downloadBitmap(mContext,url, width, height);
	}

	/**
	 * <p>@Title: init</p>
	 * <p>@Description:初始化缓存相关的资源 
	 *
	 * </p> 
	 * @param context      
	 * @throws
	 */
	private static void init(Context context) {
		screen = Helper.getScreenWidthHeight(context);
		BitmapUtils.setCacheDirectory(context);
	}
	/**
	 * <p>@ClassName: Holder</p>
	 * <p>@Description:存储异步加载的相关数据
	 * </p> 
	 */
	private class Holder{
		Bitmap mBitmap;
		String url;
		ImageView mImageView;
		public Holder(Bitmap mBitmap, String url, ImageView mImageView) {
			super();
			this.mBitmap = mBitmap;
			this.url = url;
			this.mImageView = mImageView;
		}
	}
}