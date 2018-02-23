package com.denluoyia.filedownloader.libdowndloader.system;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 使用系统自带的 DownloadManager 封装
 * Created by lzq on 2018/01/17.
 */

@SuppressWarnings("all")
public class Downloader {

    public static final String TAG = "Downloader";

    private static volatile AtomicReference<Downloader> INSTANCE = new AtomicReference<>();
    private Context mContext;
    private static DownloadManager mDownloadManager;
    private Map<Long, Task> mMap = new HashMap<>();


    private Downloader(Context context){
        this.mContext = context;
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public static Downloader getInstance(Context context){
        for (; ;){
            Downloader manager = INSTANCE.get();
            if (manager != null) return manager;
            manager = new Downloader(context);
            if (INSTANCE.compareAndSet(null, manager)) return manager;
        }
    }


    public void download(Task task){
        task.start();
        Log.i(TAG, "start downloadId=" + task.getDownloadId());
        mMap.put(task.getDownloadId(), task);
    }

    public void cancelDownload(long downloadId){
        if (downloadId == -1){
            Log.i(TAG, "downloadId = -1, no cancel");
            return;
        }
        Task task = mMap.get(downloadId);
        if (!mMap.containsKey(downloadId) || task == null){
            Log.i(TAG, "no this task in the downloadMap");
            return;
        }
        mMap.remove(downloadId);
        task.cancel();
        Log.i(TAG, "has canceled downloadId=" + downloadId);
    }


    public void download(Task[] tasks){
        if (tasks == null) return;
        for (Task task : tasks) {
            download(task);
        }
    }

    public void cancle(long[] downloadIds){
        for (long downloadId : downloadIds){
            cancelDownload(downloadId);
        }
    }

    /**
     * 必须设置{@link Task#downloadURL},{@link Task#downloadFilePath},{@link Task#mIDownloadCallback}
     */
    public static class Task {

        String downloadURL;
        String downloadFilePath;
        IDownloadCallback mIDownloadCallback;
        long downloadId = -1;


        private CharSequence mTitle;  //通知栏标题
        private CharSequence mDescription; //通知栏描述
        private int mNotificationVisibility = VISIBILITY_VISIBLE; //状态通知栏可见类型
        private String mMimeType;  //下载文件类型 ->下载管理Ui中点击某个已下载完成文件及下载完成点击通知栏提示都会根据mimeType去打开文件
        private boolean allScan = true; //是否支持可扫描
        private boolean mIsOnlyNetworkWifi, mIsVisibleInDownloadsUi, deleteExists = true; //是否只支持wifi环境下

        public Task setDownloadURL(String url){
            this.downloadURL = url;
            return this;
        }

        public Task setDownloadFilePath(String filePath){
            this.downloadFilePath = filePath;
            return this;
        }

        public Task setDownloadCallback(IDownloadCallback callback){
            this.mIDownloadCallback = callback;
            return this;
        }

        public Task setNotificationVisibility(int notificationType){
            this.mNotificationVisibility = notificationType;
            return this;
        }

        public Task setMimeType(String mimeType){
            this.mMimeType = mimeType;
            return this;
        }

        public Task setTitle(CharSequence title){
            this.mTitle = title;
            return this;
        }

        public Task setDescription(CharSequence description){
            this.mDescription = description;
            return this;
        }

        public Task setAllScan(boolean allScan){
            this.allScan = allScan;
            return this;
        }

        public Task setOnlyNetworkWifi(boolean onlyNetworkWifi){
            this.mIsOnlyNetworkWifi = onlyNetworkWifi;
            return this;
        }

        /**
         *显示在下载界面，true即下载后的文件在下载管理里显示 false:不显示
         */
        public Task setIsVisibleInDownloadsUi(boolean visibleInDownloadsUi){
            this.mIsVisibleInDownloadsUi = visibleInDownloadsUi;
            return this;
        }


        /** 获取当前下载任务Id*/
        public long getDownloadId(){
            return this.downloadId;
        }


        private long writeSize, totalSize;
        private String percentProgress;
        private Handler mHandler = new Handler(Looper.getMainLooper());
        private TimerTask mTimerTask; //一个抽象类，它的子类代表一个可以被Timer计划的任务

        DownloadManager.Request create(){
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            File file = new File(downloadFilePath);
            if (deleteExists) file.delete(); //此处可以根据需求设置
            request.setDestinationUri(Uri.fromFile(file)); //设置下载文件存储路径
            request.setNotificationVisibility(mNotificationVisibility);
            request.setTitle(mTitle);
            request.setDescription(mDescription);
            request.setMimeType(mMimeType);
            if (allScan) request.allowScanningByMediaScanner();
            if (mIsOnlyNetworkWifi) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            request.setVisibleInDownloadsUi(mIsVisibleInDownloadsUi);
            Log.i(TAG, "a request has created....");
            return request;
        }

        /** 开始下载 */
        void start(){
            downloadId = mDownloadManager.enqueue(create()); //加入一个下载队列
            Log.i(TAG, "download a task downloadId=" + downloadId);
            final DownloadManager.Query query = new DownloadManager.Query();
            final Timer mTimer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    Cursor cursor = mDownloadManager.query(query.setFilterById(downloadId));
                    if (cursor != null && cursor.moveToFirst()){
                        if (cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) == DownloadManager.STATUS_SUCCESSFUL){
                            mTimerTask.cancel();
                            mTimer.purge(); //从task queue里移除所有标记为canceled的task, purge方法就是用来释放内存引用的
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mIDownloadCallback.onProgress(totalSize, totalSize, "100%");
                                    mIDownloadCallback.onSuccess(new File(downloadFilePath));
                                    mHandler.removeCallbacks(mUpdateProgressRunnable);
                                }
                            });
                            return;
                        }

                        writeSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (totalSize > 0) mHandler.post(mUpdateProgressRunnable);
                    }
                    if (cursor != null) cursor.close();
                }

            };

            mTimer.schedule(mTimerTask, 0, 1000); //周期1s执行一次
        }


        private Runnable mUpdateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                percentProgress = String.valueOf(writeSize * 100 / totalSize) + "%";
                Log.i(TAG, "downloading update...writeSize=" + writeSize + ", totalSize=" + totalSize + ", progress=" + percentProgress);
                mIDownloadCallback.onProgress(writeSize, totalSize, percentProgress);
            }
        };


        void cancel(){
            mDownloadManager.remove(downloadId);
        }
    }

    /*
     * 设置在通知栏是否显示下载通知(下载进度), 有 3 个值可选:
     * DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
     * VISIBILITY_VISIBLE:                   下载过程中可见, 下载完后自动消失 (默认)
     * VISIBILITY_VISIBLE_NOTIFY_COMPLETED:  下载过程中和下载完成后均可见
     * VISIBILITY_HIDDEN:                    始终不显示通知
     */
    public static final int VISIBILITY_HIDDEN = 2;  //始终不显示通知
    public static final int VISIBILITY_VISIBLE = 0; //下载过程中可见, 下载完后自动消失 (默认)
    public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1; //下载过程中和下载完成后均可见
    public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3; //只有下载完成可见


}
