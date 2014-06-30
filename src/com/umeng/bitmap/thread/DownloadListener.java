/**   
 * @Copyright: Umeng.com, Ltd. Copyright 2011-2015, All rights reserved 
 *
 * @Title: Down.java
 * @Package com.bingobinbin.mulThread
 * @Description: 
 *
 * @version V1.0   
 */
package com.umeng.bitmap.thread;
public interface DownloadListener {
    public void onDownloadSize(int size);
    public void onStart();
    public void onComplete(String filePath);
    public void onError();
}
