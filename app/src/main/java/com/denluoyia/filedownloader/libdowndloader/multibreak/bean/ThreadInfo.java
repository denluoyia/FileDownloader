package com.denluoyia.filedownloader.libdowndloader.multibreak.bean;

/**
 * Created by lzq on 2018/01/17.
 */

public class ThreadInfo {

    private int id;
    private String url;
    private long start; //开始
    private long end;  //结束点
    private long finished; //完成进度

    public ThreadInfo() {
    }

    public ThreadInfo(int id, String url, long start, long end, long finished) {
        this.id = id;
        this.url = url;
        this.start = start;
        this.end = end;
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


    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        return "[thread_id=" + id + ", url=" + url + ", start=" + start + ", end=" + end + ", finished=" + finished + "]";
    }
}
