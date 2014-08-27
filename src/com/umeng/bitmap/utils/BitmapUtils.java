
package com.umeng.bitmap.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.umeng.bitmap.thread.DownloadListener;
import com.umeng.bitmap.thread.FileDownloader;

public class BitmapUtils {

    private static String SAVE_PATH = "";
    /** 文件夹大小限制为30M */
    public static final int CACHE_SIZE_LIMIT = 30 * 1024 * 1024;
    private static String FOLDER = "heyhey";

    /**
     * 根据url获取生成文件名。如果是本地路径，则直接返回；否则将该url地址MD5后作为文件名</br>
     * 
     * @param url 图片的路径
     * @return 图片的文件名
     */
    public static String getFileName(String url) {
        String filePath = url;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            filePath = SAVE_PATH + Helper.md5(url);
        }
        return filePath;
    }

    /**
     * 根据url地址获取图片本地Stream</br>
     * 
     * @param url 图片的地址
     * @return 本地图片的Stream，否则返回null
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
     * 根据Bitmap跟Url地址保存图片</br>
     * 
     * @param url
     * @param mBitmap
     */
    public static void saveBitmap(String url, Bitmap mBitmap) {
        if (mBitmap == null) {
            return;
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
     * 根据指定的宽高设置相关参数，避免出现OOM现象</br>
     * 
     * @param url 图片得url地址
     * @param width 期望图片的宽
     * @param height 期望图片的高
     * @return BitmapFactory.Options对象
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
     * 关闭输入流</br>
     * 
     * @param inputStream 输入流
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
     * 关闭输出流</br>
     * 
     * @param outputStream 输出流
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
     * 根据图片的URL地址下载图片，并将下载的图片根据宽高缩放。注意：该方法使用单线程的方式下载图片。</br>
     * 
     * @param url 图片的url地址
     * @param width 图片的宽
     * @param height 图片的高
     * @return 图片的Bitmap对象
     */
    public static Bitmap downloadBitmap(String url, int width, int height) {
        Bitmap bitmap = null;
        try {
            InputStream inputStream = new URL(url).openStream();
            OutputStream outputStream = new FileOutputStream(new File(getFileName(url)));
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
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

    /**
     * 根据图片的URL地址下载图片，并将下载的图片根据宽高缩放。注意：该方法使用多线程断点续传的方式下载图片。</br>
     * 
     * @param context Context对象
     * @param url 图片的url地址
     * @param width 图片的宽
     * @param height 图片的高
     * @return url对应图片的Bitmap对象
     */
    public static Bitmap downloadBitmap(Context context, String url, int width, int height) {
        return downloadBitmap(context, url, width, height,null);
    }
    
    /**
     * 根据图片的URL地址下载图片，并将下载的图片根据宽高缩放。注意：该方法使用多线程断点续传的方式下载图片。</br>
     * 
     * @param context Context对象
     * @param url 图片的url地址
     * @param width 图片的宽
     * @param height 图片的高
     * @param listener 下载的回调函数
     * @return url对应图片的Bitmap对象
     */
    public static Bitmap downloadBitmap(Context context, String url, int width, int height,DownloadListener listener) {
        Bitmap bitmap = null;
        FileDownloader downloader = new FileDownloader(context, url, 3);
        try {
            downloader.download(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (!downloader.isFinish()) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if ( width < 0 || height < 0 ) {
            return null;
        }
        bitmap = BitmapFactory.decodeStream(getBitmapStream(url), null,
                getScaleBitmapOptions(url, width, height));
        return bitmap;
    }
    

    /**
     * 从SD卡上获取图片。如果不存在则返回null</br>
     * 
     * @param url 图片的url地址
     * @param width 期望图片的宽
     * @param height 期望图片的高
     * @return 代表图片的Bitmap对象
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
     * 获取目录下所有文件的大小</br>
     * 
     * @param file 目录文件
     * @return 该目录下所有文件的大小
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
     * 检查存在目录下文件是否超过默认大小，默认值为30M。如果超过则会删除30%的文件，删除策略采用LRU算法</br>
     * 
     * @param bitmapSize
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
     * 获取该目录下文件的修改时间</br>
     * 
     * @param file 目录文件
     * @return Map对象，存储该目录下所有文件的最后修改时间
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

    /**
     * 设置缓存目录</br>
     * 
     * @param directory 目录名称
     */
    public static void setBitmapCacheDir(String directory) {
        if (!TextUtils.isEmpty(directory)) {
            FOLDER = directory;
        }
        // 判断sd卡是否存在
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            SAVE_PATH = Environment.getExternalStorageDirectory().getPath()
                    + File.separator + FOLDER + File.separator;

        } else {
            SAVE_PATH = Environment.getDataDirectory().getPath() + File.separator
                    + FOLDER + File.separator;
        }
        File file = new File(SAVE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
    
    /**
     * 
     * 判断文件是否存在</br>
     * @param url 文件的url地址
     * @return 如果文件存在则返回true；否则返回false
     */
    public static boolean isExist(String url){
        String path = getFileName(url);
        File file= new File(path);
        if ( file.exists() ) {
            return true;
        }
        return false;
    }
    
    public static long getCacheSize(){
        File root = new File(SAVE_PATH);
        return getDirSize(root);
    }
    
    private static void  deleteCacheFile(File file){
        if ( file.isFile() ) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            for ( File f : files ) {
                deleteCacheFile(f);
            }
        }
    }
    
    public static boolean cleanCache(){
        File root = new File(SAVE_PATH);
        deleteCacheFile(root);
        long size = getDirSize(root);        
        return size == 0;
    }
    
}
