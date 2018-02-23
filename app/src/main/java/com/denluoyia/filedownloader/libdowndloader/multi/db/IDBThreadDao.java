package com.denluoyia.filedownloader.libdowndloader.multi.db;

import com.denluoyia.filedownloader.libdowndloader.multi.bean.ThreadInfo;

import java.util.List;

/**
 * 操作数据库的接口
 * Created by lzq on 2018/01/18.
 */

public interface IDBThreadDao {

    void insertThread(ThreadInfo threadInfo);

    void deleteThread(String url);

    void updateThread(String url, int threadId, int finished);

    List<ThreadInfo> queryThreads(String url);

    public boolean isExists(String url, int threadId);
}
