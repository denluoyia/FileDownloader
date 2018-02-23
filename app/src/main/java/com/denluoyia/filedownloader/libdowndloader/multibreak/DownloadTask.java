package com.denluoyia.filedownloader.libdowndloader.multibreak;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.denluoyia.filedownloader.MyApp;
import com.denluoyia.filedownloader.libdowndloader.multibreak.bean.ThreadInfo;
import com.denluoyia.filedownloader.libdowndloader.multibreak.db.DBUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多线程断点下载 之 【不使用广播和服务】
 * Created by lzq on 2018/01/18.
 */

/**
 * 以下三个参数必须设置
 * {@link DownloadTask#downloadURL},{@link DownloadTask#downloadFilePath},{@link DownloadTask#mCallback}
 */
@SuppressWarnings("all")
public class DownloadTask {

    /** 下载链接 */
    String                 downloadURL;
    /** 下载完成后文件存放地址 */
    String                 downloadFilePath;
    /** 下载回调 */
    IMultiDownloadCallback mCallback;

    // 一个没有限制最大线程数的线程池
    public static ExecutorService sExecutorService = Executors.newCachedThreadPool();

    int mThreadCount = 3; //设置默认线程数为3

    private Handler mHandler = new Handler(android.os.Looper.getMainLooper());
    private volatile long downloadedSize = 0, totalSize;
    private String fileName;
    private boolean isPause = false; //否定暂停下载
    private List<DownloadThread> mThreadList; //保存所有的下载任务


    public DownloadTask() {
    }

    public DownloadTask(String downloadURL, String downloadFilePath, IMultiDownloadCallback callback){
        this.downloadURL = downloadURL;
        this.downloadFilePath = downloadFilePath;
        this.mCallback = callback;
        this.fileName = getFileNameWithSuffInUrl(downloadURL);
    }

    public DownloadTask setDownloadURL(String url){
        this.downloadURL = url;
        this.fileName = getFileNameWithSuffInUrl(downloadURL);
        return this;
    }

    public DownloadTask setDownloadFilePath(String path){
        this.downloadFilePath = path;
        return this;
    }

    public DownloadTask setMultiDownloadCallback(IMultiDownloadCallback callback){
        this.mCallback = callback;
        return this;
    }

    public DownloadTask setThreadCount(int count){
        this.mThreadCount = count;
        return this;
    }

    public DownloadTask setIsPause(boolean isPause){
        this.isPause = isPause;
        return this;
    }

    public String getDownloadURL(){
        return this.downloadURL;
    }

    public void download(){
        sExecutorService.execute(new InitThread());
    }

    private void initDownload(){
        List<ThreadInfo> list = DBUtil.queryThreads(downloadURL);
        if (list.size() == 0){ /** 首次进行下载 数据库无缓存*/
            long block = totalSize / mThreadCount;
            for (int i = 0; i < mThreadCount; i++){
                long start = i * block;
                long end = (i + 1) * block - 1;
                if (i == mThreadCount -1){
                    end = totalSize - 1;
                }
                ThreadInfo threadInfo = new ThreadInfo(i, downloadURL, start, end, 0);
                list.add(threadInfo);
            }
        }
        Log.e("listSize", list.size() + "");
        mThreadList = new ArrayList<>();
        for (ThreadInfo threadInfo : list){
            Log.e("线程信息", threadInfo.toString());
            DownloadThread downloadThread = new DownloadThread(threadInfo);
            if (!DBUtil.isExists(threadInfo.getUrl(), threadInfo.getId())){
                DBUtil.insertThread(threadInfo);
            }
            sExecutorService.execute(downloadThread);
            mThreadList.add(downloadThread);
        }
    }

    private void checkAllFinished(){
        boolean allFinished = true;
        for (DownloadThread downloadThread : mThreadList){
            if (!downloadThread.isFinished){
                allFinished = false;
                break;
            }
        }

        if (allFinished){
            /** 数据删除缓存数据 */
            DBUtil.deleteThread(downloadURL);
            FileMultiDownloader.instance(MyApp.getApplication()).removeTask(downloadURL);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onProgress(totalSize, totalSize, "100%");
                    mCallback.onSuccess(new File(downloadFilePath, fileName));
                }
            });
        }
    }


    class DownloadThread extends Thread {
        ThreadInfo threadInfo;
        private boolean isFinished;

        public DownloadThread(ThreadInfo threadInfo){
            this.threadInfo = threadInfo;
        }

        @Override
        public void run() {
            super.run();
            HttpURLConnection connection = null;
            RandomAccessFile  raf        = null;
            InputStream input      = null;
            try {
                URL url = new URL(downloadURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5 * 1000);
                connection.setRequestMethod("GET");
                long start = threadInfo.getStart() + threadInfo.getFinished();
                Log.e("开始下载:", start + "");

                //DEBUG
                connection.setRequestProperty("Range", "bytes=" + start + "-" + threadInfo.getEnd());

                final File file = new File(downloadFilePath, fileName);
                //设置输出文件路径
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);

                int code = connection.getResponseCode();
                if (code == HttpURLConnection.HTTP_PARTIAL || code == HttpURLConnection.HTTP_OK) {
                    //onStart()可以是线程信息
                    //mCallback.onStart(DownloadTask.this);

                    input = connection.getInputStream();
                    byte[] data = new byte[1024];
                    int  len;
                    long time     = System.currentTimeMillis();
                    downloadedSize += threadInfo.getFinished();
                    while ((len = input.read(data)) != -1) {
                        raf.write(data, 0, len);
                        downloadedSize += len;
                        threadInfo.setFinished(threadInfo.getFinished() + len);

                        // update every 500 mms
                        if (System.currentTimeMillis() - time > 1000) {
                            time = System.currentTimeMillis();
                            //写入数据库
                            DBUtil.updateThread(threadInfo.getUrl(), threadInfo.getId(), threadInfo.getFinished());
                            //回调更新主线程UI
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e("downloadSize/totalSize", downloadedSize + " / " + totalSize);
                                    mCallback.onProgress(downloadedSize, totalSize, Math.round(downloadedSize * 100 / totalSize) + "%");
                                }
                            });
                        }

                        if (isPause){
                            return;
                        }
                    }

                    isFinished = true;
                    Log.e("线程" + threadInfo.getId(), "下载完成");
                    //TODO查看全部是否完成
                    checkAllFinished();
                }

            } catch (final IOException e) {
                e.printStackTrace();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onFailure(DownloadTask.this, e);
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
    }

    private static final int INIT_START = 0;
    private InnerHandler mInnerHandler = new InnerHandler();
    private class InnerHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case INIT_START:
                    initDownload();
                    break;
            }
        }
    }

    class InitThread extends Thread {
        @Override
        public void run() {
            super.run();
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            try {
                URL url = new URL(downloadURL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();

                int length = -1;
                if (code == HttpURLConnection.HTTP_OK) {
                    length = conn.getContentLength();
                }

                if (length <= 0) {
                    return;
                }
                // create file if the file not exists
                File dir = new File(downloadFilePath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                raf = new RandomAccessFile(file, "rwd");
                raf.setLength(length);

                // set the length of file
                totalSize = length;
                Log.e("initThread", totalSize + "");
                mInnerHandler.sendEmptyMessage(INIT_START);
            } catch (final Exception e) {
                e.printStackTrace();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onFailure(DownloadTask.this, e);
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
