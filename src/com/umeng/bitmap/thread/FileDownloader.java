/**   
 * @Copyright: Umeng.com, Ltd. Copyright 2011-2015, All rights reserved 
 *
 * @Title: FileDownload.java
 * @Package com.bingobinbin.mulThread
 * @Description: 
 *
 * @author Honghui He  
 * @version V1.0   
 */

package com.umeng.bitmap.thread;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.umeng.bitmap.utils.BitmapUtils;

import android.content.Context;

/**
 * 文件下载类，采用多线程断点续传，提高下载速度。
 */
public class FileDownloader {
    private Context context;
    private FileService fileService;
    /* 已下载文件长度 */
    private int downloadSize = 0;
    /* 原始文件长度 */
    private int fileSize = 0;
    /* 线程数 */
    private DownloadThread[] threads;
    /* 本地保存文件 */
    private File saveFile;
    /* 缓存各线程下载的长度 */
    private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();
    /* 每条线程下载的长度 */
    private int block;
    /* 下载路径 */
    private String downloadUrl;
    private boolean isFinish = false;

    /**
     * 获取线程数
     */
    public int getThreadSize() {
        return threads.length;
    }

    /**
     * 获取文件大小
     * 
     * @return
     */
    public int getFileSize() {
        return fileSize;
    }

    /**
     * 累计已下载大小
     * 
     * @param size
     */
    protected synchronized void append(int size) {
        downloadSize += size;
    }

    /**
     * 更新指定线程最后下载的位置
     * 
     * @param threadId 线程id
     * @param pos 最后下载的位置
     */
    protected void update(int threadId, int pos) {
        this.data.put(threadId, pos);
    }

    /**
     * 保存记录文件
     */
    protected synchronized void saveLogFile() {
        this.fileService.update(this.downloadUrl, this.data);
    }

    /**
     * 构建文件下载器
     * 
     * @param downloadUrl 下载路径
     * @param fileSaveDir 文件保存目录
     * @param threadNum 下载线程数
     */
    public FileDownloader(Context context, String downloadUrl,
            int threadNum) {
        try {
            this.context = context;
            this.downloadUrl = downloadUrl;
            this.isFinish = false;
            fileService = new FileService(this.context);
            URL url = new URL(this.downloadUrl);
            this.threads = new DownloadThread[threadNum];
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(40 * 1000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty(
                    "Accept",
                    "image/gif, image/png, image/x-png, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            conn.setRequestProperty("Accept-Language", "zh-CN");
            conn.setRequestProperty("Referer", downloadUrl);
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                this.fileSize = conn.getContentLength();// 根据响应获取文件大小
                if (this.fileSize <= 0) {
                    throw new RuntimeException("Unkown file size ");
                }
                String fileName = BitmapUtils.getFileName(downloadUrl);
                this.saveFile = new File(fileName);/* 保存文件 */
                Map<Integer, Integer> logdata = fileService
                        .getData(downloadUrl);
                if (logdata.size() > 0) {
                    for (Map.Entry<Integer, Integer> entry : logdata.entrySet())
                        data.put(entry.getKey(), entry.getValue());
                }
                this.block = (this.fileSize % this.threads.length) == 0 ? this.fileSize
                        / this.threads.length
                        : this.fileSize / this.threads.length + 1;
                if (this.data.size() == this.threads.length) {
                    for (int i = 0; i < this.threads.length; i++) {
                        this.downloadSize += this.data.get(i + 1);
                    }
                }
            } else {
                throw new RuntimeException("server no response ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始下载文件。注意，该方法执行在主线程中。
     * 
     * @param listener 监听下载数量的变化,如果不需要了解实时下载的数量,可以设置为null
     * @return 已下载文件大小
     * @throws Exception
     */
    public int download(DownloadListener listener) throws Exception {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(
                    this.saveFile, "rw");
            if (this.fileSize > 0) {
                randomAccessFile.setLength(this.fileSize);
            }
            randomAccessFile.close();
            URL url = new URL(this.downloadUrl);
            if (this.data.size() != this.threads.length) {
                this.data.clear();
                for (int i = 0; i < this.threads.length; i++) {
                    this.data.put(i + 1, 0);
                }
            }
            for (int i = 0; i < this.threads.length; i++) {
                int downLength = this.data.get(i + 1);
                if (downLength < this.block
                        && this.downloadSize < this.fileSize) { // 该线程未完成下载时,继续下载
                    this.threads[i] = new DownloadThread(this, url,
                            this.saveFile, this.block, this.data.get(i + 1),
                            i + 1);
                    this.threads[i].setPriority(7);
                    this.threads[i].start();
                } else {
                    this.threads[i] = null;
                }
            }
            this.fileService.save(this.downloadUrl, this.data);
            boolean notFinish = true;// 下载未完成
            while (notFinish) {// 循环判断是否下载完毕
                Thread.sleep(100);
                notFinish = false;// 假定下载完成
                for (int i = 0; i < this.threads.length; i++) {
                    if (this.threads[i] != null && !this.threads[i].isFinish()) {
                        notFinish = true;// 下载没有完成
                        if (this.threads[i].getDownLength() == -1) {// 如果下载失败,再重新下载
                            this.threads[i] = new DownloadThread(this, url,
                                    this.saveFile, this.block,
                                    this.data.get(i + 1), i + 1);
                            this.threads[i].setPriority(7);
                            this.threads[i].start();
                        }
                    }
                }
                // 正在下载回调
                if (listener != null) {
                    listener.onDownloadSize(this.downloadSize);
                }
                // 下载完成回调
                if (getFileSize() == downloadSize) {
                    isFinish = true;
                    if (listener != null) {
                        listener.onComplete(downloadUrl);
                    }
                }
            }
            fileService.delete(this.downloadUrl);
        } catch (Exception e) {
            e.printStackTrace();
            isFinish = true;
            if (listener != null) {
                listener.onError();
            }
        }
        return this.downloadSize;
    }

    /**
     * 获取Http响应头字段
     * 
     * @param http
     * @return
     */
    public static Map<String, String> getHttpResponseHeader(
            HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0;; i++) {
            String mine = http.getHeaderField(i);
            if (mine == null)
                break;
            header.put(http.getHeaderFieldKey(i), mine);
        }
        return header;
    }

    /**
     * 
     * 下载是否完成</br>
     * @return 如果下载完成则返回true；否则返回false
     */
    public boolean isFinish() {
        return isFinish;
    }
}
