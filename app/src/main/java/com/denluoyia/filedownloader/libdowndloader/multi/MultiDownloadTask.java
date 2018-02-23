package com.denluoyia.filedownloader.libdowndloader.multi;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.denluoyia.filedownloader.Const;
import com.denluoyia.filedownloader.libdowndloader.multi.bean.FileInfo;
import com.denluoyia.filedownloader.libdowndloader.multi.bean.ThreadInfo;
import com.denluoyia.filedownloader.libdowndloader.multi.db.DBDaoImp;
import com.denluoyia.filedownloader.libdowndloader.multi.db.IDBThreadDao;

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
 * 多线程断点下载文件任务
 * Created by lzq on 2018/01/18.
 */

public class MultiDownloadTask {

    private Context mContext;
    private FileInfo mFileInfo;
    private IDBThreadDao mDao;
    private volatile long downloadSize = 0;
    private int threadCount = 3; //默认为3 可是随便设置
    public boolean isPause;
    private List<DownloadThread> mThreadList;

    // 一个没有限制最大线程数的线程池
    public static ExecutorService sExecutorService = Executors.newCachedThreadPool();


    public MultiDownloadTask(Context context, FileInfo fileInfo, int threadCount){
        this.mContext = context;
        this.mFileInfo = fileInfo;
        this.threadCount = threadCount;
        this.mDao = new DBDaoImp(context);
    }


    public void download(){
        Log.e("download", "download");
        List<ThreadInfo> list = mDao.queryThreads(mFileInfo.getUrl());
        Log.e("download", "数据库查询" + list.size());
        if (list.size() == 0){
            int length = mFileInfo.getLength();
            Log.e("length:", mFileInfo.getLength() + "");

            int block = length / threadCount;
            for (int i = 0; i < threadCount; i++){
                int start = i * block;
                int end = (i + 1) * block - 1;
                if (i == threadCount - 1){ //最后一个block到最后 不管多还是少
                    end = length - 1;
                }
                ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), start, end, 0);
                Log.e("线程",threadInfo.toString());
                list.add(threadInfo);
            }
        }

        mThreadList = new ArrayList<>();
        for (ThreadInfo threadInfo : list){
            Log.e("download", threadInfo.toString());
            DownloadThread downloadThread = new DownloadThread(threadInfo);

            //如果数据库没有 则进行插入
            if (!mDao.isExists(mFileInfo.getUrl(), downloadThread.threadInfo.getId())){
                mDao.insertThread(threadInfo);
            }
            // 使用线程池执行下载任务
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
            mDao.deleteThread(mFileInfo.getUrl());
            Intent intent = new Intent(DownloadService.ACTION_FINISHED);
            mContext.sendBroadcast(intent);
            DownloadService.removeTask(mFileInfo.getUrl());
        }
    }


    class DownloadThread extends Thread {
        private ThreadInfo threadInfo;
        private boolean isFinished = false;

        public DownloadThread(ThreadInfo threadInfo){
            this.threadInfo = threadInfo;
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            InputStream in = null;

            try {
                URL url = new URL(threadInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000 * 5);
                conn.setRequestMethod("GET");

                int start = threadInfo.getStart() + threadInfo.getFinished();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + threadInfo.getEnd());
                File file = new File(Const.FILE_DIR + "temp3/", getFileNameWithSuffInUrl(threadInfo.getUrl()));
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                downloadSize += threadInfo.getFinished();
                long time = System.currentTimeMillis();
                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_PARTIAL || code == HttpURLConnection.HTTP_OK){
                    Log.e("开始下载线程", threadInfo.getId() + "");
                    in = conn.getInputStream();
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = in.read(bytes)) != -1){
                        raf.write(bytes, 0, len);
                        downloadSize += len;
                        threadInfo.setFinished(threadInfo.getFinished() + len);
                        if (System.currentTimeMillis() - time > 1000){
                            time = System.currentTimeMillis();
                            mDao.updateThread(threadInfo.getUrl(), threadInfo.getId(), threadInfo.getFinished());
                            //广播更新进度
                            Intent intent = new Intent(DownloadService.ACTION_UPDATE);
                            intent.putExtra("percentProgress", downloadSize * 100 / mFileInfo.getLength() + "%");
                            mContext.sendBroadcast(intent);
                        }

                        if (isPause){
                            return;
                        }
                    }
                    //当前线程下载完毕
                    isFinished = true;
                    checkAllFinished();
                }

            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if (conn != null){
                    conn.disconnect();
                }
                try {
                    if (in != null){
                        in.close();
                    }
                    if (raf != null){
                        raf.close();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            super.run();
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
