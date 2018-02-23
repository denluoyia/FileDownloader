package com.denluoyia.filedownloader.libdowndloader.single;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.denluoyia.filedownloader.MyApp;

/**
 * Created by lzq on 2018/01/17.
 */

public class SPUtil {
    private static SharedPreferences sp;

    private static SharedPreferences getSp(Context context) {
        if (sp == null) {
            sp = context.getSharedPreferences("mySp", Context.MODE_PRIVATE);
        }
        return sp;
    }

    /**
     * 存int缓存
     */
    public static void putLong(String key, long value) {
        getSp(MyApp.getApplication()).edit().putLong(key, value).commit();
    }

    /**
     * 取int缓存
     */
    public static Long getLong(String key, Long defValue) {
        return getSp(MyApp.getApplication()).getLong(key, defValue);
    }


}
