package com.bingobinbin.activity;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.bingobinbin.mulThread.FileDownloader;
import com.bingobinbin.utils.BitmapManager;
import com.bingobinbin.utils.R;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ImageView imageView = (ImageView) findViewById(R.id.image_test);
//		 BitmapManager manager = new BitmapManager(this);
//		 manager.setDefaultBmp(BitmapFactory.decodeResource(getResources(),
//		 R.drawable.ic_launcher));
//		 manager.loadBitmap("http://www.umeng.com/images/pic/banner_module_social.png",
//		 imageView);
		FileDownloader fileDownloader = new FileDownloader(this,
		 "http://www.umeng.com/images/pic/banner_module_social.png", 3);
		try {
			fileDownloader.download(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
