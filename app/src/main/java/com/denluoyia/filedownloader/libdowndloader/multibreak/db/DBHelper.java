package com.denluoyia.filedownloader.libdowndloader.multibreak.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by lzq on 2018/01/17.
 */

public class DBHelper extends SQLiteOpenHelper{

    private static final String DB_NAME = "download2.db";
    private static final int VERSION = 1;
    public static final String TABLE_NAME = "multi_thread";

    private static DBHelper mDBHelper = null;


    public static DBHelper getInstance(Context context){
        if (mDBHelper == null){
            mDBHelper = new DBHelper(context);
        }

        return mDBHelper;
    }


    private final String SQL_CREATE = "create table multi_thread(_id integer primary key autoincrement, "
            + "thread_id integer, url text, start number, end number, finished number)";

    private final String SQL_DROP = "drop table if exists multi_thread";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQL_DROP);
        sqLiteDatabase.execSQL(SQL_CREATE);
    }

}
