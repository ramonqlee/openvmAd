package com.idreems.openvmAd;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.FileNameGenerator;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.internal.Supplier;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.DefaultExecutorSupplier;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.idreems.openvmAd.Crash.CrashHandler;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.file.DirUtil;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMediaPlayer;
import com.idreems.openvmAd.utils.FileDownloaderHelper;
import com.idreems.openvmAd.utils.LogUtil;
import com.idreems.openvmAd.utils.Utils;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 */
public class MyApplication extends Application {
    public static final int MAX_DISK_CACHE_SIZE = 100 * ByteConstants.MB;
    public static final int MAX_SMALL_IMAGE_DISK_CACHE_SIZE = 100 * ByteConstants.MB;
    public static final int MAX_MEMORY_CACHE_SIZE = 5 * ByteConstants.MB;

    private static final String TAG = MyApplication.class.getSimpleName();
    private static Application sApplication;
    static Application application;
    private HttpProxyCacheServer proxy;
    private static Handler sUIHandler = new Handler(Looper.getMainLooper());
    private FSMediaPlayer mMainVideoAdCountryPlayer = new FSMediaPlayer();

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        // 异常处理，不需要处理时注释掉这两句即可！
        CrashHandler crashHandler = CrashHandler.getInstance();
        // 注册crashHandler
        crashHandler.init(getApplicationContext());

        FileDownloader.init(this);
        // 是否第三方进程
        String processName = getProcessName(this, android.os.Process.myPid());
        Log.d(TAG, "processName = " + processName + " packageName = " + getPackageName());
        if (!TextUtils.equals(processName, getPackageName())) {
            return;
        }
        sApplication = this;
        callTest();
        try {
            init();
            Intent intent = new Intent(this, BackgroundService.class);
            startService(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Context getContext() {
        return sApplication;
    }

    public static MyApplication getMyApplication() {
        return (MyApplication) sApplication;
    }

    private void init() {
        DirUtil.init(this);

        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(MyApplication.this, getConfigureCaches(this));
        }

        FileDownloaderHelper.setup(this);
        FileDownloaderHelper.initInDownloadProcess(this);

        startSDcardListen();
    }

    private ImagePipelineConfig getConfigureCaches(Context context) {
        int maxCacheSize = MAX_MEMORY_CACHE_SIZE;
        int maxCacheEntries = Integer.MAX_VALUE;

        final MemoryCacheParams bitmapCacheParams = new MemoryCacheParams(
                maxCacheSize,// 内存缓存中总图片的最大大小,以字节为单位。
                maxCacheEntries,// 内存缓存中图片的最大数量。
                MAX_MEMORY_CACHE_SIZE,// 内存缓存中准备清除但尚未被删除的总图片的最大大小,以字节为单位。
                Integer.MAX_VALUE,// 内存缓存中准备清除的总图片的最大数量。
                MAX_MEMORY_CACHE_SIZE);// 内存缓存中单个图片的最大大小。

        Supplier<MemoryCacheParams> mSupplierMemoryCacheParams = new Supplier<MemoryCacheParams>() {
            @Override
            public MemoryCacheParams get() {
                return bitmapCacheParams;
            }
        };

        ImagePipelineConfig.Builder builder = OkHttpImagePipelineConfigFactory.newBuilder(context, new OkHttpClient());
        builder.setBitmapMemoryCacheParamsSupplier(mSupplierMemoryCacheParams);
        builder.setBitmapsConfig(Bitmap.Config.RGB_565);
        builder.setDownsampleEnabled(true);
        builder.setResizeAndRotateEnabledForNetwork(true);// 对网络图片进行resize处理，减少内存消耗
        int processorsCount = Runtime.getRuntime().availableProcessors();
        if (processorsCount <= 0) {
            processorsCount = 1;
        }
        builder.setExecutorSupplier(new DefaultExecutorSupplier(processorsCount));

        //默认图片的磁盘配置
        String temp = Utils.getFrescoMainCacheDir(context);
        if (Consts.logEnabled()) Log.d(Consts.LOG_TEST_TAG, "setMainDiskCacheConfig =" + temp);
        DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryPath(new File(temp))
                .setMaxCacheSize(MAX_DISK_CACHE_SIZE)
                .build();
        builder.setMainDiskCacheConfig(diskCacheConfig);

        //小图片的磁盘配置（用于广告等不经常发生变化的图片的缓存）
        temp = Utils.getFrescoSmallImageCacheDir(context);
        if (Consts.logEnabled())
            Log.d(Consts.LOG_TEST_TAG, "getFrescoSmallImageCacheDir =" + temp);
        DiskCacheConfig diskSmallCacheConfig = DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryPath(new File(temp))
                .setMaxCacheSize(MAX_SMALL_IMAGE_DISK_CACHE_SIZE)
                .build();
        builder.setSmallImageDiskCacheConfig(diskSmallCacheConfig);
        return builder.build();
    }

    public void startSDcardListen() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.setPriority(1000);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_NOFS);
        intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intentFilter.addDataScheme("file");
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                // TODO 检查u盘中包含按照格式命名的配置文件，并且文件内容合法

                Intent mainIntent = new Intent(context, SerialPortPreferences.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                //todo
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
            } else {
            }
        }
    };

    private void callTest() {
        // FIXME 测试功能,发布时需要去除

    }


    /**
     * @return null may be returned if the specified process not found
     */
    public static String getProcessName(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }


    private HttpProxyCacheServer newProxy() {
        String dir = Utils.getVideoCacheDir(this);
        if (Consts.logEnabled())
            LogUtil.d(Consts.PLAYER_TAG, "HttpProxyCacheServer dir = " + dir);

        return new HttpProxyCacheServer.Builder(this)
                .cacheDirectory(new File(dir))
                .fileNameGenerator(new FileNameGenerator() {
                    @Override
                    public String generate(String url) {
                        String endStr = url.substring(url.lastIndexOf("."));
                        final String key = Utils.md5(url) + endStr;
//                        if (Consts.logEnabled())
//                            Log.d(Consts.PLAYER_TAG, "HttpProxyCacheServer fileNameGenerator url = " + url + " key = " + key);
                        return key;
                    }
                })
                .maxCacheSize(Consts.MAX_VIDEO_CACHE_SIZE)
                .build();
    }

    public static MyApplication getApplication() {
        return (MyApplication) application;
    }

    //可能为空
    public static HttpProxyCacheServer getVideoCacheProxy(Context context) {
        try {
            MyApplication app = (MyApplication) context.getApplicationContext();
            return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public FSMediaPlayer getMainVideoCountryPlayer() {
        //是否启用http缓存
        mMainVideoAdCountryPlayer.setHttpProxyCacheServer(MyApplication.getVideoCacheProxy(this));
        return mMainVideoAdCountryPlayer;
    }

    public static Handler getUIHandler() {
        return sUIHandler;
    }
}
