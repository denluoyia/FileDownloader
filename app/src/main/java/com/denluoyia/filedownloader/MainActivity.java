package com.denluoyia.filedownloader;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.denluoyia.filedownloader.libdowndloader.multibreak.DownloadTask;
import com.denluoyia.filedownloader.libdowndloader.multibreak.IMultiDownloadCallback;
import com.denluoyia.filedownloader.libdowndloader.multi.DownloadService;
import com.denluoyia.filedownloader.libdowndloader.multi.bean.FileInfo;
import com.denluoyia.filedownloader.libdowndloader.single.FileBreakDownloader;
import com.denluoyia.filedownloader.libdowndloader.single.IBreakDownloadCallback;
import com.denluoyia.filedownloader.libdowndloader.system.IDownloadCallback;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private final String url = "http://ct.51voa.com:88/v/a-year-marked-by-disaster-violence.mp4";
    private TextView progressText1, progressText2, progressText3,progressText4;
    private ProgressBar progressBar1, progressBar2, progressBar3, progressBar4;
    private MyReceiver myReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressText1 = findViewById(R.id.progressText1);
        progressText2 = findViewById(R.id.progressText2);
        progressText3 = findViewById(R.id.progressText3);
        progressText4 = findViewById(R.id.progressText4);

        progressBar1 = findViewById(R.id.progressBar1);
        progressBar2 = findViewById(R.id.progressBar2);
        progressBar3 = findViewById(R.id.progressBar3);
        progressBar4 = findViewById(R.id.progressBar4);

        findViewById(R.id.btnStart1).setOnClickListener(this);
        findViewById(R.id.btnStart2).setOnClickListener(this);
        findViewById(R.id.btnStart3).setOnClickListener(this);
        findViewById(R.id.btnStart4).setOnClickListener(this);
        findViewById(R.id.btnStop1).setOnClickListener(this);
        findViewById(R.id.btnStop2).setOnClickListener(this);
        findViewById(R.id.btnStop3).setOnClickListener(this);
        findViewById(R.id.btnStop4).setOnClickListener(this);


        /** 注册非静态广播 */
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadService.ACTION_START);
        intentFilter.addAction(DownloadService.ACTION_UPDATE);
        intentFilter.addAction(DownloadService.ACTION_FINISHED);
        intentFilter.addAction(DownloadService.ACTION_STOP);
        registerReceiver(myReceiver, intentFilter);

        checkInitPermissions();
    }


    private long downloadId;
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnStart1:
                downloadId = FileDownloadUtils.downloadFileByMethod1(url, new IDownloadCallback() {
                    @Override
                    public void onStart(String downloadURL) {
                        Log.e("下载1 - onStart", downloadURL);
                    }

                    @Override
                    public void onProgress(long writeSize, long totalSize, String percent) {
                        progressText1.setText(percent);
                        progressBar1.setProgress(Integer.valueOf(percent.replace("%", "")));
                    }

                    @Override
                    public void onSuccess(File file) {
                        Toast.makeText(MainActivity.this, "下载成功" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String downloadURL) {
                        Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.btnStop1:
                FileDownloadUtils.cancelDownloadMethod1ById(downloadId);
                break;

            case R.id.btnStart2:
                FileDownloadUtils.downloadFileByMethod2(url, new IBreakDownloadCallback() {
                    @Override
                    public void onStart(FileBreakDownloader.Task task) {
                        Log.e("下载2 - onStart", task.getDownloadURL());
                    }

                    @Override
                    public void onFailure(FileBreakDownloader.Task task, Exception e) {
                        Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(File file) {
                        Toast.makeText(MainActivity.this, "下载成功" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(long writeSize, long totalSize, String percent) {
                        progressText2.setText(percent);
                        progressBar2.setProgress(Integer.valueOf(percent.replace("%", "")));
                    }
                });

                break;
            case R.id.btnStop2:
                FileDownloadUtils.cancelDownloadMethod2(url);
                break;

            case R.id.btnStart3:
                Intent intent = new Intent(this, DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra("fileInfo", new FileInfo(1, url, FileDownloadUtils.getFileNameWithSufferInUrl(url), 0, 0));
                startService(intent);
                break;

            case R.id.btnStop3:
                Intent intent1 = new Intent(this, DownloadService.class);
                intent1.setAction(DownloadService.ACTION_STOP);
                intent1.putExtra("fileInfo", new FileInfo(1, url, FileDownloadUtils.getFileNameWithSufferInUrl(url), 0, 0));
                startService(intent1);
                break;

            case R.id.btnStart4:
                FileDownloadUtils.downloadFileByMethod4(url, new IMultiDownloadCallback() {
                    @Override
                    public void onStart(DownloadTask task) {
                        Log.e("下载4 - onStart", task.getDownloadURL());
                    }

                    @Override
                    public void onFailure(DownloadTask task, Exception e) {
                        Toast.makeText(MainActivity.this, "下载4失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(File file) {
                        Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(long writeSize, long totalSize, String percent) {
                        progressText4.setText(percent);
                        progressBar4.setProgress(Integer.valueOf(percent.replace("%", "")));
                    }
                });
                break;

            case R.id.btnStop4:
                FileDownloadUtils.cancelDownloadMethod4(url);
                break;

        }
    }


    //权限6.0设置
    public static final int REQUEST_PERMISSION_CODE = 100;
    private void checkInitPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{
                    //放入需要授予的权限，例如需要写入的权限
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])){
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
                }else{
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
                }
            }
        } else {
            handleAfterPermissions();
        }
    }

    private void  handleAfterPermissions(){
        String path = Const.FILE_DIR;
        (new File(path)).mkdirs();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE){
            handleAfterPermissions();
        }
    }


    /** 声明一个广播接收器 */
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case DownloadService.ACTION_UPDATE:
                    /** 在此处实现下载进度的更新 */
                    String percent = intent.getStringExtra("percentProgress");
                    progressText3.setText(percent);
                    progressBar3.setProgress(Integer.valueOf(percent.replace("%", "")));
                    break;

                case DownloadService.ACTION_FINISHED:
                    /** 下载完成 */
                    progressText3.setText("100%");
                    progressBar3.setProgress(100);
                    Toast.makeText(MainActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    }

    @Override
    /** 注销广播 */
    protected void onDestroy() {
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

}
