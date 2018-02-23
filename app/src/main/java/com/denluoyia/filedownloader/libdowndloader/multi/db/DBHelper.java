package com.denluoyia.filedownloader.libdowndloader.multi.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by lzq on 2018/01/17.
 */

public class DBHelper extends SQLiteOpenHelper{

    private static final String DB_NAME = "download.db";
    private static final int VERSION = 1;
    private static final String TABLE_NAME = "thread_info";

    private static DBHelper mDBHelper = null;


    public static DBHelper getInstance(Context context){
        if (mDBHelper == null){
            mDBHelper = new DBHelper(context);
        }

        return mDBHelper;
    }


    private final String SQL_CREATE = "create table thread_info(_id integer primary key autoincrement, "
            + "thread_id integer, url text, start integer, end integer, finished integer)";

    private final String SQL_DROP = "drop table if exists thread_info";

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
