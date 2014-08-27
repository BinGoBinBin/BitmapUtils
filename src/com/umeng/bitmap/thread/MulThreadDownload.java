package com.umeng.bitmap.thread;
/** 
 * @author liubin 
 * @date：Sep 17, 2013 4:23:45 PM  
 */

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import com.umeng.bitmap.utils.BitmapUtils;

public class MulThreadDownload {

	public void downlaod(final String path) throws Exception{
		//文件的长度
		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(10*1000);
		int length = conn.getContentLength();//得到文件的长度
		int threadnum = 3;//线程数
		int block = length%threadnum==0 ? length/threadnum : length/threadnum+1;//计算每条线程下载的数据长度
		File file = new File(BitmapUtils.getFileName(path));
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
		randomAccessFile.setLength(length);//把本地文件的长度设置为网络文件的长度
		randomAccessFile.close();
		for(int i=0 ; i < threadnum ; i++){
			new DownloadThread(block, url, file ,i).start();
		}
	}

	private final class DownloadThread extends Thread{
		private int block;//每条线程下载的数据长度
		private URL url;//下载路径
		private File file;//本地文件
		private int threaid;//线程id
		
		public DownloadThread(int block, URL url, File file, int i) {
			this.block = block;
			this.url = url;
			this.file = file;
			this.threaid = i;
		}

		@Override
		public void run() {
			try {
				int startpos = threaid * block;//计算该线程从文件的什么位置开始下载
				int endpos = (threaid+1) * block - 1;//计算该线程下载到文件的什么位置结束
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(15*1000);
				conn.setRequestProperty("Range", "bytes="+ startpos+"-"+ endpos);
				InputStream inputStream = conn.getInputStream();
				RandomAccessFile rfile = new RandomAccessFile(file, "rwd");
				rfile.seek(startpos);
				byte[] buffer = new byte[1024];
				int len = 0;
				while( (len = inputStream.read(buffer)) != -1){
					rfile.write(buffer, 0, len);
				}
				rfile.close();
				inputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}