package com.denluoyia.filedownloader.libdowndloader.multibreak.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.denluoyia.filedownloader.MyApp;
import com.denluoyia.filedownloader.libdowndloader.multibreak.bean.ThreadInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzq on 2018/01/18.
 */

@SuppressWarnings("all")
public class DBUtil {

    private static DBHelper mDBHelper = DBHelper.getInstance(MyApp.getApplication());
    private static final String tableName = DBHelper.TABLE_NAME;


    public static synchronized void insertThread(ThreadInfo threadInfo) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("thread_id", threadInfo.getId());
        values.put("url", threadInfo.getUrl());
        values.put("start", threadInfo.getStart());
        values.put("end", threadInfo.getEnd());
        values.put("finished", threadInfo.getFinished());
        db.insert(tableName, null, values);
        db.close();
    }


    public static synchronized void deleteThread(String url) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.delete(tableName, "url = ?", new String[]{url});
        db.close();
    }


    public static synchronized void updateThread(String url, int thread_id, long finished) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("finished", finished);
        String whereCaluse = "url = ? and thread_id = ?";
        String[] whereArgs = new String[]{url, thread_id + ""};
        db.update(tableName,values, whereCaluse, whereArgs);
        db.close();
    }

    /**
     * 线程查询
     * @param url url地址
     * @return
     */

    public static List<ThreadInfo> queryThreads(String url) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        List<ThreadInfo> list = new ArrayList<>();

        Cursor cursor = db.query(tableName, null, "url = ?", new String[]{url}, null, null, null);
        while (cursor.moveToNext()){
            ThreadInfo threadInfo = new ThreadInfo();
            threadInfo.setId(cursor.getInt(cursor.getColumnIndex("thread_id")));
            threadInfo.setUrl(url);
            threadInfo.setStart(cursor.getInt(cursor.getColumnIndex("start")));
            threadInfo.setEnd(cursor.getInt(cursor.getColumnIndex("end")));
            threadInfo.setFinished(cursor.getInt(cursor.getColumnIndex("finished")));
            list.add(threadInfo);
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 判断线程是否为空
     * @param url  线程url
     * @param thread_id 线程id
     * @return
     */

    public static boolean isExists(String url, int thread_id) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(tableName, null, "url = ? and thread_id = ?", new String[]{url, thread_id + ""}, null, null, null);
        boolean exists = cursor.moveToNext();
        cursor.close();
        db.close();
        return exists;
    }
}
