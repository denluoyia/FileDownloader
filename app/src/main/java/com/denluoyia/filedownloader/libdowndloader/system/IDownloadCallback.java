package com.denluoyia.filedownloader.libdowndloader.system;

import java.io.File;

/**
 * Downloading callback
 * Created by lzq on 2018/01/17.
 */
public interface IDownloadCallback {

    void onStart(String downloadURL);

    void onProgress(long writeSize, long totalSize, String percent);

    void onSuccess(File file);

    void onFailure(String downloadURL);
}
