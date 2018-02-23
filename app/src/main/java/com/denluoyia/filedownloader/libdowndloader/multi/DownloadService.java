package com.denluoyia.filedownloader.libdowndloader.multi;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.denluoyia.filedownloader.Const;
import com.denluoyia.filedownloader.libdowndloader.multi.bean.FileInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by lzq on 2018/01/18.
 */

public class DownloadService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";


    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    public static final String ACTION_FINISHED = "ACTION_FINISHED";

    private static Map<String, MultiDownloadTask> mMap = new LinkedHashMap<>();
    private MyHandler mHandler = new MyHandler();
    private String fileDir = Const.FILE_DIR + "temp3/";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        FileInfo fileInfo = null;
        switch (action){
            case ACTION_START:
                fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                if (mMap.containsKey(fileInfo.getUrl())){ //如果已经存在 则直接返回
                    return super.onStartCommand(intent, flags, startId);
                }

                InitThread initThread = new InitThread(fileInfo);
                MultiDownloadTask.sExecutorService.execute(initThread);
                break;
            case ACTION_STOP:
                fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                if (mMap.containsKey(fileInfo.getUrl())){
                    MultiDownloadTask task = mMap.get(fileInfo.getUrl());
                    task.isPause = true;
                    mMap.remove(fileInfo.getUrl()); //移除下载队列
                }
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void removeTask(String downloadTask){
        mMap.remove(downloadTask);
    }

    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x00:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    MultiDownloadTask task = new MultiDownloadTask(DownloadService.this, fileInfo, 3);
                    task.download();
                    Log.e("启动下载task", "初始下载进度:");
                    mMap.put(fileInfo.getUrl(), task);
                    break;
            }
        }
    }

    class InitThread extends Thread {
        private FileInfo fileInfo;
        public InitThread(FileInfo fileInfo){
            this.fileInfo = fileInfo;
        }

        @Override
        public void run() {
            super.run();
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            try {
                URL url = new URL(fileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                int length = -1;
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    length = conn.getContentLength();
                }
                if (length < 0) return;
                File dir = new File(fileDir);
                if (!dir.exists()){
                    dir.mkdirs();
                }
                File file = new File(fileDir, getFileNameWithSuffInUrl(fileInfo.getUrl()));
                raf = new RandomAccessFile(file, "rwd");
                raf.setLength(length);
                //Message post Handler
                fileInfo.setLength(length);
                Log.e("初始化线程", fileInfo.getLength() + "");
                Message message = Message.obtain();
                message.what = 0x00;
                message.obj = fileInfo;
                mHandler.sendMessage(message);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if (conn != null){
                    conn.disconnect();
                }
                try {
                    if (raf != null){
                        raf.close();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

        }
    }

    public static String getFileNameWithSuffInUrl(String url) {
        String result = "";
        if (!TextUtils.isEmpty(url)) {
            result = url.substring(url.lastIndexOf("/") + 1, url.length());
        }
        return result;
    }
}
