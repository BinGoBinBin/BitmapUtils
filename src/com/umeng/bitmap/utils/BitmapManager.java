package com.umeng.bitmap.utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.umeng.bitmap.thread.DownloadListener;

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
							BitmapUtils.saveBitmap(holder.url, holder.mBitmap);
							
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
	 * 
	 * 加载图片。获取获取顺序为：缓存 - SD卡 - 网络下载</br>
	 * @param url 图片的url地址
	 * @param width 期望图片的宽
	 * @param height 期望图片的高
	 * @return 图片对应的Bitmap对象
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
			BitmapUtils.saveBitmap(url, bitmap);
		}
		return bitmap;
	}

	/**
	 * 设置默认图片。该图片在加载图片时显示，加载完成后显示目标图片
	 * 
	 * @param bmp 默认图片
	 */
	public void setDefaultBmp(Bitmap bmp) {
		defaultBmp = bmp;
	}

	/**
	 * 加载图片，加载完成后设置给对于的ImageView对象并显示
	 * 
	 * @param url 图片的url地址
	 * @param imageView 需要显示该图片的ImageView对象
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
	 * 
	 * 从缓存中获取图片</br>
	 * @param url 图片的url地址
	 * @param width 期望图片的宽
	 * @param height 期望图片的高
	 * @return 图片对于的Bitmap对象
	 */
	private Bitmap getBitmapFromCache(String url, int width, int height) {
	    String cacheKey = Helper.md5(url);
		Bitmap bitmap = null;
		// 从内存中获取
		if (cache.containsKey(cacheKey)) {
			bitmap = cache.get(cacheKey).get();
			if (bitmap != null) {
				return bitmap;
			}
		}
		// 从SD卡上获取
		bitmap = BitmapUtils.getBitmapFromSDCard(url, width, height);
		if (bitmap != null) {
			cache.put(cacheKey, new SoftReference<Bitmap>(bitmap));
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
	 * 
	 * 根据图片的url地址下载图片</br>
	 * @param url 图片的url地址
	 * @param width 图片的宽
	 * @param height 图片的高
	 * @return 图片对于的Bitmap对象
	 */
	private Bitmap downloadBitmap(String url, int width, int height) {
		return BitmapUtils.downloadBitmap(mContext,url, width, height,null);
	}

	/**
     * 
     * 根据图片的url地址下载图片</br>
     * @param url 图片的url地址
     * @param width 图片的宽
     * @param height 图片的高
     * @return 图片对于的Bitmap对象
     */
    private Bitmap downloadBitmap(String url, int width, int height,DownloadListener listener) {
        return BitmapUtils.downloadBitmap(mContext,url, width, height,listener);
    }
    
	/**
	 * 
	 * 初始化相关参数。比如：获取手机屏幕的宽高，默认根据包名设置图片的缓存路径</br>
	 * @param context
	 */
	private static void init(Context context) {
		screen = Helper.getScreenWidthHeight(context);
		String packageName = context.getPackageName();
		BitmapUtils.setBitmapCacheDir(packageName);
	}

	/**
	 * 一个简单的容器，保存相关的参数
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
	
	/**
	 *
	 * 根据图片列表加载图片</br>
	 */
	public void preDownloadImage(){
	    BitmapConfig config = BitmapConfig.getInstance();
	    List<String> invalidUrls = new ArrayList<String>();
	    if ( config.getWifiAvaliable(mContext) ) {
	        Set<String> lists = config.getDownloadList();
	        Iterator<String> iterator = lists.iterator();
	        String url = null;
	        while ( iterator.hasNext() ) {
                url = iterator.next();
                if ( BitmapUtils.isExist(url) ) {
                    continue;
                }
                Task task = new Task(url);
                task.execute();
                while (!task.isFinish) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
                if ( task.isFinish) {
                    invalidUrls.add(url);
                }
            }
	        config.removeDownloadedList(invalidUrls);
	    }
	}
	
	class Task{
	    public boolean isFinish = false;
	    String url = null;
	    
	    Task(String url){
	        this.url = url;
	    }
	    
	    void execute(){
	        downloadBitmap(url, -1, -1,new DownloadListener() {
	            
	            @Override
	            public void onStart() {
	            }
	            
	            @Override
	            public void onError() {
	                setStatus();
	            }
	            
	            @Override
	            public void onDownloadSize(int size) {
	            }
	            
	            @Override
	            public void onComplete(String filePath) {
	                setStatus();
	            }
	        });
	    }
	    
	    public void setStatus(){
	        isFinish = true;
	    }
	    
	}
	
	
}