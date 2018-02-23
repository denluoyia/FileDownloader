package com.denluoyia.filedownloader.libdowndloader.multibreak;

import java.io.File;

/**
 * 下载回调
 * Created by lzq on 2018/01/16.
 */

public interface IMultiDownloadCallback {

    /**
     * 下载开始
     */
    void onStart(DownloadTask task);

    /**
     * 下载失败
     */
    void onFailure(DownloadTask task, Exception e);

    /**
     * 下载成功
     */
    void onSuccess(File file);

    /**
     * 下载进度更新
     */
    void onProgress(long writeSize, long totalSize, String percent);

}
