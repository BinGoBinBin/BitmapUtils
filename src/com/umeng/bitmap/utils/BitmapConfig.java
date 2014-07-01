/**
 * 
 */
package com.umeng.bitmap.utils;

import java.util.List;
import java.util.Set;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

/**
 * 
 */
public class BitmapConfig {

    //是否在wifi环境下预加载图片
    private boolean isDownloadWifi = true;
    private Set<String> mDownloadUrlSet = null;
    private static BitmapConfig instance = new BitmapConfig();
    
    private BitmapConfig(){}
    
    public static BitmapConfig getInstance(){
        return instance;
    }
    
    /**
     * 
     * 设置需要加载的图片列表</br>
     * @param paths 图片的url地址列表
     */
    public void setDownloadUrlList(Set<String> paths){
        this.mDownloadUrlSet = paths;
    }
    
    //获取需要下载的图片地址列表
    public Set<String> getDownloadList(){
        return  mDownloadUrlSet;
    }
    
    public void removeDownloadedList(List<String> lists){
        mDownloadUrlSet.removeAll(lists);
    }
    
    //设置是否只在wifi环境下加载图片
    public void setDownloadWifi(boolean status){
        this.isDownloadWifi = status;
    }
    
    //是否在wifi下加载图片列表
    public boolean isDownloadWIfi(){
        return isDownloadWifi;
    }
    
    /**
     * 
     * 获取当前环境Wifi是否可用</br>
     * @param context Context对象
     * @return true如果当前wifi可用；否则返回false
     */
    public boolean getWifiAvaliable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context  
                .getSystemService(Context.CONNECTIVITY_SERVICE); 
        State state = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if ( state == State.CONNECTED || state == State.CONNECTING ) {
            return true;
        }
        return false;
    }
    
}
