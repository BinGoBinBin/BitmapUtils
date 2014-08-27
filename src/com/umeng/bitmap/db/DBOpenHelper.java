/**   
 * @Copyright: Umeng.com, Ltd. Copyright 2011-2015, All rights reserved 
 *
 * @Title: DBOpenHelper.java
 * @Package com.bingobinbin.db
 * @Description: 
 *
 * @author Honghui He  
 * @version V1.0   
 */
package com.umeng.bitmap.db;
/** 
 * @author liubin 
 * @date：Sep 17, 2013 4:24:43 PM  
 */
/**<p>@ClassName: DBOpenHelper</p>
 * <p>@Description:
 *
 * </p> 
 * 
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {
	private static final String DBNAME = "file_downloader.db";
	private static final int VERSION = 1;
	
	public DBOpenHelper(Context context) {
		super(context, DBNAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS filedownlog (id integer primary key autoincrement, downpath varchar(100), threadid INTEGER, downlength INTEGER)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS filedownlog");
		onCreate(db);
	}

}
