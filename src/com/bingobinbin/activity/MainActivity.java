
package com.bingobinbin.activity;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.bingobinbin.utils.R;
import com.umeng.bitmap.thread.FileDownloader;
import com.umeng.bitmap.utils.BitmapManager;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView imageView = (ImageView) findViewById(R.id.image_test);
        BitmapManager manager = new BitmapManager(MainActivity.this);
        manager.setDefaultBmp(BitmapFactory.decodeResource(getResources(),
        R.drawable.ic_launcher));
        manager.loadBitmap("http://img.blog.csdn.net/20140414134054375?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvd2FuZ2ppbnl1NTAx/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast",
        imageView);
        findViewById(R.id.load).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        FileDownloader fileDownloader = new FileDownloader(MainActivity.this,
                                "http://img.blog.csdn.net/20140414134054375?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvd2FuZ2ppbnl1NTAx/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast", 3);
                        try {
//                            fileDownloader.download(null);
//                             
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

}
