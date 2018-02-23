package com.denluoyia.filedownloader.libdowndloader.single;



import java.io.File;

/**
 * 下载回调
 * Created by lzq on 2018/01/16.
 */

public interface IBreakDownloadCallback {

    /**
     * 下载开始
     */
    void onStart(FileBreakDownloader.Task task);

    /**
     * 下载失败
     */
    void onFailure(FileBreakDownloader.Task task, Exception e);

    /**
     * 下载成功
     */
    void onSuccess(File file);

    /**
     * 下载进度更新
     */
    void onProgress(long writeSize, long totalSize, String percent);

}
