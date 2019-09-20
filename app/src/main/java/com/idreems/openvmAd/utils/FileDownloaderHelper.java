package com.idreems.openvmAd.utils;

import android.app.Application;

import com.liulishuo.filedownloader.FileDownloader;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by ramonqlee_macpro on 2019/5/7.
 */

public class FileDownloaderHelper {

    // 超时设置 单位 秒
    private static final int READ_TIMEOUT = 5 * 60;
    private static final int WRITE_TIMEOUT = 5 * 60;
    private static final int CONNECT_TIMEOUT = 30;

    /**
     * 在主进程的 Application 的 onCreate 里调用
     */
    public static void setup(Application application) {
        FileDownloader.setup(application);
    }

    /**
     * 在 FileDownLoader 进程的 Application 的 onCreate 里调用，绕过 SSL 认证代码如果不在 FileDownLoader 进程调用也无效
     */
    public static void initInDownloadProcess(Application application) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        // 添加 SSL 认证
        SSLTrustManager.addVerify(builder);

        FileDownloader.setupOnApplicationOnCreate(application)
                .connectionCreator(new OkHttp3Connection.Creator(builder))// 自实现 OkHttp3Connection
                .commit();
    }
}
