package com.idreems.openvmAd;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.Push.PushDispatcher;
import com.idreems.openvmAd.PushHandler.PostLogHandler;
import com.idreems.openvmAd.PushHandler.RebootHandler;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.file.FileUtil;
import com.idreems.openvmAd.network.DownloadListener;
import com.idreems.openvmAd.persistence.Config;
import com.idreems.openvmAd.utils.LogUtil;
import com.idreems.openvmAd.utils.TimeUtil;
import com.idreems.openvmAd.utils.ToastUtils;
import com.idreems.openvmAd.utils.UpdateUtil;
import com.idreems.openvmAd.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ramonqlee on 7/23/16.
 */
public class BackgroundService extends Service {
    private static final String TAG = BackgroundService.class.getSimpleName();
    // 需要和HostReceiver同步
    public static final String HOST_ACTION = "com.idreems.openvmhost.aciton.APK_CHANGED";
    // 指令
    public static final String CMD_KEY = "cmd_key";
    public static final int INVALID_CMD = 0;
    public static final int INSTALL_APK = 0x100;
    public static final int UNINSTALL_APK = INSTALL_APK + 1;
    public static final int UPGRADE_APK = UNINSTALL_APK + 1;
//	public static final int LAUNCH_APK = UPGRADE_APK + 1;// 暂时不需要

    // 参数
    public static final String FILE_NAME = "file_name";    //  安装和升级时，需要传递该参数

    private static final long UPDATE_CHECK_INTERVAL = 5 * 60 * 1000;//检查间隔

    private Timer mUpdateTimer;
    private boolean mUpdatedApkDownloaded;
    private String mUpdateApkFilePath;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO 需要修改为定时检查新版本或者推送的方式
//        startAppVersionChecker(this);

        // TODO 需要修改为定时清理
        FileUtil.startClean(this);
        LogUtil.cleanJunkLogFiles();

//        startRebootMonitor();
//        registerPushObserver();
        return START_STICKY;
    }

    // TODO 待增加新版本检查功能
    private void startAppVersionChecker(final Context context) {
        //check app version
        final UpdateUtil.VersionCheckListener versionCheckListener = new UpdateUtil.VersionCheckListener() {
            @Override
            public void onFailure(IOException e) {
            }

            @Override
            public boolean onComplete(String serverVersion) {
                PackageInfo packageInfo = Utils.getPackageInfo(context, context.getPackageName());
                try {
                    if (TextUtils.isEmpty(serverVersion)) {
                        return false;
                    }
                    if (!TextUtils.isDigitsOnly(serverVersion)) {
                        return false;
                    }
                    boolean r = Integer.valueOf(serverVersion) > packageInfo.versionCode;
                    if (r) {
                        ToastUtils.show(R.string.new_app_version_found);
                    }
                    return r;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        };

        final DownloadListener filedownLoaderListener = new DownloadListener() {
            public void completed(final String filePath) {
                final String downloadedApkFilePath = filePath;
                final Runnable threadRunnable = new Runnable() {
                    @Override
                    public void run() {
                        final String finalApkCacheDir = Utils.getApkCacheDir(BackgroundService.this);
                        // 解压到最终的目录下，这样Host就不用再解压了
                        FileUtil.makesureDirExist(finalApkCacheDir);
                        final String mainAppFileName = BackgroundService.this.getPackageName() + Utils.getApkSuffix();
                        final String updateApkFileName = Utils.getUpdateApkFileName(BackgroundService.this);

                        // 检查升级文件和当前主文件apk文件哪个闲置，将其作为待升级的文件:原则和当前加载应用的versionCode不一致
                        //缺省为主应用的文件名
                        // 情况1. 包名命名，被加载；下载了新的apk，此时应该用升级文件名
                        // 情况2：升级文件名，被加载；下载了新的apk，此时应该用包名命名
                        String localUpdateFileName = mainAppFileName;
                        final String updateFilePath = String.format("%s%s%s", finalApkCacheDir, File.separator, updateApkFileName);
                        int updateFileVersionCode = Utils.getVersionCode(BackgroundService.this, updateFilePath);
                        PackageInfo packageInfo = Utils.getPackageInfo(BackgroundService.this, BackgroundService.this.getPackageName());

                        // 主文件
                        if (null != packageInfo) {
                            if (updateFileVersionCode != packageInfo.versionCode) {
                                localUpdateFileName = updateApkFileName;
                            }
                        }

                        if (Utils.extractFromApkAssets(downloadedApkFilePath, finalApkCacheDir, BackgroundService.this.getPackageName() + Utils.getApkSuffix(), localUpdateFileName)) {
                            mUpdateApkFilePath = String.format("%s%s%s", finalApkCacheDir, File.separator, localUpdateFileName);
                            mUpdatedApkDownloaded = true;//设置已经下载的标志
                            FileUtil.deleteFile(downloadedApkFilePath);

                            // 清理文件
//                            ToastUtils.show("App升级文件下载完毕 " + localUpdateFileName);
                            sendUpdateBroadcast(context);
                        }
                    }
                };
                new Thread(threadRunnable).start();
            }
        };

        // 开始定时检查
        try {
            if (null != mUpdateTimer) {
                mUpdateTimer.cancel();
                mUpdateTimer = null;
            }

            mUpdateTimer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (mUpdatedApkDownloaded) {
                        LogUtil.d("downloadedApk,waiting for update");
//                        发送升级通知，通知Host加载升级后的文件
                        sendUpdateBroadcast(context);
                        return;
                    }
                    UpdateUtil.startVersionChecker(getApplicationContext(), Consts.getAppVersionCheckUrl(BackgroundService.this), versionCheckListener, filedownLoaderListener);
                }
            };
            mUpdateTimer.schedule(task, 0, UPDATE_CHECK_INTERVAL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendUpdateBroadcast(Context context) {
        // 当前是否空闲
//        if (CoinHopperWrapper.sharedInstance(context).isPayouting()) {
//            LogUtil.d("isPayouting,wait for next time to update apk");
//            return;
//        }
//
//        ToastUtils.show(R.string.update_app_tip);
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // 直接重启
//                DeviceUtils.reboot();
//            }
//        }, 4000);


        // 不用设置，因为apk需要重新加载
        // TODO 通知host，升级apk
//        Intent intent = new Intent(HOST_ACTION);
//
//        intent.putExtra(CMD_KEY, UPGRADE_APK);
//        intent.putExtra(FILE_NAME, mUpdateApkFilePath);
//        sendBroadcast(intent);
    }

    class TimeTickerChangeReceiver extends BroadcastReceiver {
        private static final long MIN_REBOOT_INTERVAL = 30 * 60 * 1000;//机器最小的重启间隔
        private static final long ALLOW_TIME_INTEVAL = 5 * 60 * 1000;//超过时间，就不再重启了

        private void handleReceive(final Context context) {
            // 获取当前时间，如果超过了系统设定的时间，并且在一定的时间间隔内，则重启机器
            // 1. 时间进入到允许重启的时间了
            // 2. 检查是否满足最小的重启时间间隔内
            final Config config = Config.sharedInstance(context);
            final String rebootScheduleStr = config.getValue(Config.REBOOT_SCHEDULE);
            if (TextUtils.isEmpty(rebootScheduleStr)) {
                return;
            }

            final long currentTime = TimeUtil.getCheckedCurrentTimeInMills();

            // 追加上日期
            String[] split = rebootScheduleStr.split(":");
            int hour = Integer.valueOf(split[0]);
            int minutes = Integer.valueOf(split[1]);
            Date date = new Date(currentTime);
            date.setHours(hour);
            date.setMinutes(minutes);
            date.setSeconds(0);
            final long rebootTimeInMillis = date.getTime();

            Log.d(TAG, "rebootTimeInMillis = " + TimeUtil.formatFullTime(rebootTimeInMillis) + " currentTime = " + TimeUtil.formatFullTime(currentTime));
            // 1. 时间进入到允许重启的时间了
            if (rebootTimeInMillis < currentTime || rebootTimeInMillis - currentTime > ALLOW_TIME_INTEVAL) {
                return;
            }
            LogUtil.d("time to reboot");

            String lastRebootStr = config.getValue(Config.LAST_REBOOT);
            if (TextUtils.isEmpty(lastRebootStr)) {
                return;
            }

            try {
                long lastRebootInMills = Long.valueOf(lastRebootStr) * 1000;
                // 2. 检查是否满足最小的重启时间间隔内
                if (Math.abs(lastRebootInMills - currentTime) < MIN_REBOOT_INTERVAL) {
                    LogUtil.d("reboot recently");
                    return;
                }
                ToastUtils.show(R.string.reboot_tip);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LogUtil.d("try to reboot");
//                        DeviceUtils.reboot();
                    }
                }, 3000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            handleReceive(context);
        }
    }

    private TimeTickerChangeReceiver mTimeTickerReceiver;

    /**
     * 启动重启检测
     */
    private void startRebootMonitor() {
        if (null != mTimeTickerReceiver) {
            return;
        }
        try {
            mTimeTickerReceiver = new TimeTickerChangeReceiver();
            registerReceiver(mTimeTickerReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void registerPushObserver() {
        // 注册更多的Push处理器
        PushDispatcher.sharedInstance().addObserver(new PostLogHandler(getApplicationContext()));
        PushDispatcher.sharedInstance().addObserver(new RebootHandler(getApplicationContext()));
    }

}
