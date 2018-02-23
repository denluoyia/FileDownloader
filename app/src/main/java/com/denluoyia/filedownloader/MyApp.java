package com.denluoyia.filedownloader;

import android.app.Application;

/**
 * Created by lzq on 2018/01/17.
 */

public class MyApp extends Application{

    private static MyApp mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static MyApp getApplication(){
        return mContext;
    }
}
