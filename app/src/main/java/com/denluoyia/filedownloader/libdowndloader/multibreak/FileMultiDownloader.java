package com.denluoyia.filedownloader.libdowndloader.multibreak;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by lzq on 2018/01/18.
 */

public class FileMultiDownloader {

    private static final AtomicReference<FileMultiDownloader> INSTANCE      = new AtomicReference<>();
    private Map<String, DownloadTask> mMap = new LinkedHashMap<>();

    /** 获取实例，建议使用Application上下文 */
    public static FileMultiDownloader instance(Context context) {
        for (; ; ) {
            FileMultiDownloader instance = INSTANCE.get();
            if (instance != null) return instance;
            instance = new FileMultiDownloader(context);
            if (INSTANCE.compareAndSet(null, instance)) return instance;
        }
    }

    private Context mAppContext;
    private FileMultiDownloader(Context context){
        mAppContext = context;
    }


    public void download(DownloadTask task){
        if (mMap.containsKey(task.downloadURL)){ //如果正在下载 则直接返回
            return;
        }

        task.download();
        mMap.put(task.downloadURL, task);
    }


    public void removeTask(String downloadURL){
        if (mMap.containsKey(downloadURL)){
            mMap.remove(downloadURL);
        }
    }

    public void pauseTask(String downloadURL){
        if (mMap.containsKey(downloadURL)){
            DownloadTask task = mMap.get(downloadURL);
            task.setIsPause(true);
            mMap.remove(downloadURL);
        }
    }
}
