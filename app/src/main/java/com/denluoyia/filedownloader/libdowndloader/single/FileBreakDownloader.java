package com.denluoyia.filedownloader.libdowndloader.single;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;


import com.denluoyia.filedownloader.MyApp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 封装文件断点下载工具类 场景局限性 实时缓存进度 网络异常会中断
 * Created by lzq on 2018/01/16.
 */

@SuppressWarnings("all")
public class FileBreakDownloader {

    private static final AtomicReference<FileBreakDownloader> INSTANCE      = new AtomicReference<>();
    //private              List<String>                         downloadTasks = new ArrayList<>();
    private Map<String, Task> mMap = new HashMap<>();

    /** 获取实例，建议使用Application上下文 */
    public static FileBreakDownloader instance(Context context) {
        for (; ; ) {
            FileBreakDownloader instance = INSTANCE.get();
            if (instance != null) return instance;
            instance = new FileBreakDownloader(context);
            if (INSTANCE.compareAndSet(null, instance)) return instance;
        }
    }

    private Context mAppContext;

    private FileBreakDownloader(Context context){
        mAppContext = context;
    }

    public void download(Task task){
        for (String url : mMap.keySet()){
            if (url.equals(task.downloadURL)){
                return;
            }
        }
        task.setPause(false);
        task.start();
        //downloadTasks.add(task.downloadURL);
        mMap.put(task.downloadURL, task);
    }


    public void remove(String taskURL){
        pause(taskURL);
    }

    public void pause(String downloadURL){
        if (mMap.containsKey(downloadURL)){
            mMap.get(downloadURL).setPause(true);
            mMap.remove(downloadURL);
        }
    }


    public static class Task {

        /** 下载链接 */
        String                 downloadURL;
        /** 下载完成后文件存放地址 */
        String                 downloadFilePath;
        /** 下载回调 */
        IBreakDownloadCallback mIDownloadCallback;

        private Handler mHandler = new Handler(android.os.Looper.getMainLooper());
        private volatile long downloadedSize, totalSize;
        private String downloadFilePathTemp;
        private String fileName;

        private boolean isPause = false;

        public Task() {
        }

        public Task(String downloadURL, String downloadFilePath, IBreakDownloadCallback mIDownloadCallback){
            this.downloadURL = downloadURL;
            this.downloadFilePath = downloadFilePath;
            this.downloadFilePathTemp = downloadFilePath + "temp2/";
            this.mIDownloadCallback = mIDownloadCallback;
            this.fileName = getFileNameWithSuffInUrl(downloadURL);
        }

        public Task setDownloadURL(String url){
            this.downloadURL = url;
            this.fileName = getFileNameWithSuffInUrl(url);
            return this;
        }

        public Task setDownloadFilePath(String localFilePath){
            this.downloadFilePath = localFilePath;
            this.downloadFilePathTemp = localFilePath + "temp2/";
            return this;
        }

        public Task setBreakDownloadCallback(IBreakDownloadCallback callback){
            this.mIDownloadCallback = callback;
            return this;
        }

        public void setPause(boolean isPause){
            this.isPause = isPause;
        }

        public String getDownloadURL(){
            return this.downloadURL;
        }


        void start(){
            new Thread(downloadRunnable).start();
        }

        private Runnable downloadRunnable = new Runnable() {
            @Override
            public void run() {
                initThread();
                HttpURLConnection connection = null;
                RandomAccessFile  raf        = null;
                InputStream       input      = null;
                try {
                    URL url = new URL(downloadURL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5 * 1000);
                    connection.setRequestMethod("GET");
                    long start = SPUtil.getLong(downloadURL, 0L);
                    Log.e("开始下载:", start + "");

                    //DEBUG
                    connection.setRequestProperty("Range", "bytes=" + start + "-" + totalSize);

                    final File file = new File(downloadFilePathTemp, fileName);
                    //设置输出文件路径
                    raf = new RandomAccessFile(file, "rwd");
                    raf.seek(start);

                    int code = connection.getResponseCode();
                    if (code == HttpURLConnection.HTTP_PARTIAL || code == HttpURLConnection.HTTP_OK) { /**开放判断权限*/
                        mIDownloadCallback.onStart(Task.this);
                        input = connection.getInputStream();
                        byte[] data = new byte[1024 * 4];
                        int  len;
                        long time     = System.currentTimeMillis();
                        downloadedSize = start;
                        while ((len = input.read(data)) != -1) {
                            raf.write(data, 0, len);
                            downloadedSize += len;

                            // update every 500 mms
                            if (System.currentTimeMillis() - time > 500) {
                                time = System.currentTimeMillis();
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("downlaodSize", downloadedSize + " ");
                                        mIDownloadCallback.onProgress(downloadedSize, totalSize, Math.round(downloadedSize * 100 / totalSize) + "%");
                                    }
                                });
                            }
                            SPUtil.putLong(downloadURL, downloadedSize);
                            if (isPause){
                                FileBreakDownloader.instance(MyApp.getApplication()).remove(downloadURL); //移出任务
                                Log.i("test","暂停");
                                return;
                            }
                        }

                        SPUtil.putLong(downloadURL, 0); //默认清空 使用数据库可以把该条记录删除
                        FileBreakDownloader.instance(MyApp.getApplication()).remove(downloadURL); //移出任务

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mIDownloadCallback.onProgress(totalSize, totalSize, "100%");
                                mIDownloadCallback.onSuccess(file);
                            }
                        });
                    }

                } catch (final IOException e) {
                    e.printStackTrace();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mIDownloadCallback.onFailure(Task.this, e);
                        }
                    });

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    try {
                        if (input != null) {
                            input.close();
                        }
                        if (raf != null) {
                            raf.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        private void initThread() {
            HttpURLConnection conn = null;
            RandomAccessFile  raf  = null;
            try {
                URL url = new URL(downloadURL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                int code   = conn.getResponseCode();

                int length = -1;
                if (code == HttpURLConnection.HTTP_OK) {
                    length = conn.getContentLength();
                }

                if (length <= 0) {
                    return;
                }
                // create file if the file not exists
                File dir = new File(downloadFilePathTemp);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                raf = new RandomAccessFile(file, "rwd");
                raf.setLength(length);

                // set the length of file
                totalSize = length;

            } catch (final Exception e) {
                e.printStackTrace();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIDownloadCallback.onFailure(Task.this, e);
                    }
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    if (raf != null) {
                        raf.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取url地址的带后缀名的文件名
     *
     * @return 文件名
     */
    public static String getFileNameWithSuffInUrl(String url) {
        String result = "";
        if (!TextUtils.isEmpty(url)) {
            result = url.substring(url.lastIndexOf("/") + 1, url.length());
        }
        return result;
    }

}
