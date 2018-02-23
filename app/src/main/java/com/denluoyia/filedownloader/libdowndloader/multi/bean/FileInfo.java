package com.denluoyia.filedownloader.libdowndloader.multi.bean;

import java.io.Serializable;

/**
 * Created by lzq on 2018/01/17.
 */

public class FileInfo implements Serializable{

    private int id;
    private String url;
    private String fileName;
    private int length; //文件总长度
    private int finished; //文件下载已完成的进度


    public FileInfo() {
    }

    public FileInfo(int id, String url, String fileName, int length, int finished) {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.length = length;
        this.finished = finished;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getFinished() {
        return finished;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }
}
