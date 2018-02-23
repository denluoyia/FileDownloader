package com.denluoyia.filedownloader;

import android.text.TextUtils;

import com.denluoyia.filedownloader.libdowndloader.multibreak.DownloadTask;
import com.denluoyia.filedownloader.libdowndloader.multibreak.FileMultiDownloader;
import com.denluoyia.filedownloader.libdowndloader.multibreak.IMultiDownloadCallback;
import com.denluoyia.filedownloader.libdowndloader.single.FileBreakDownloader;
import com.denluoyia.filedownloader.libdowndloader.single.IBreakDownloadCallback;
import com.denluoyia.filedownloader.libdowndloader.system.Downloader;
import com.denluoyia.filedownloader.libdowndloader.system.IDownloadCallback;


/**
 * Created by lzq on 2018/01/25.
 */

public class FileDownloadUtils {

    /**
     * 系统DownloadManager封装下载
     */
    public static long downloadFileByMethod1(String url, IDownloadCallback callback){
        String localFilePath = Const.FILE_DIR + "temp1/" + getFileNameWithSufferInUrl(url);
        Downloader.Task task = new Downloader.Task();
        task.setDownloadURL(url)
                .setDownloadFilePath(localFilePath)
                .setDownloadCallback(callback);
        Downloader.getInstance(MyApp.getApplication()).download(task);
        return task.getDownloadId();
    }

    public static void cancelDownloadMethod1ById(long id){
        Downloader.getInstance(MyApp.getApplication()).cancelDownload(id);
    }


    /** 单线程断点下载 */
    public static void downloadFileByMethod2(String url, IBreakDownloadCallback callback){
        String downloadLocalPath = Const.FILE_DIR; //在temp2目录下
        FileBreakDownloader.Task task = new FileBreakDownloader.Task();
        task.setDownloadURL(url)
                .setDownloadFilePath(downloadLocalPath)
                .setBreakDownloadCallback(callback);
        FileBreakDownloader.instance(MyApp.getApplication()).download(task);
    }

    /** 暂停 */
    public static void cancelDownloadMethod2(String url){
        FileBreakDownloader.instance(MyApp.getApplication()).pause(url);
    }



    /** 多线程断点下载 */
    public static void downloadFileByMethod4(String url, IMultiDownloadCallback callback){
        String fileLocalPath = Const.FILE_DIR + "temp4/";
        DownloadTask task = new DownloadTask();
        task.setDownloadURL(url)
                .setDownloadFilePath(fileLocalPath)
                .setMultiDownloadCallback(callback);
        FileMultiDownloader.instance(MyApp.getApplication()).download(task);

    }

    public static void cancelDownloadMethod4(String url){
        FileMultiDownloader.instance(MyApp.getApplication()).pauseTask(url);
    }


    /* 获取url地址的带后缀名的文件名
     *  @return 文件名
     */
    public static String getFileNameWithSufferInUrl(String url) {
        String result = "";
        if (!TextUtils.isEmpty(url)) {
            result = url.substring(url.lastIndexOf("/") + 1, url.length());
        }
        return result;
    }

}
