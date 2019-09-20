package com.idreems.openvmAd.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.idreems.openvmAd.constant.Consts;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class TaskUtils {

    private static ExecutorService sCachedThreadPool;
    private static ExecutorService sFixedThreadPool;

    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    public static void shutDownFixedExecutorService() {
        try {
            if (null == sFixedThreadPool) {
                return;
            }
            sFixedThreadPool.shutdownNow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void runOnFixedExecutorService(final Runnable r) {
        if (null == r) {
            return;
        }
        if (null == sFixedThreadPool) {
            sFixedThreadPool = Executors.newFixedThreadPool(25, TaskUtils.defaultThreadFactory());
        }
        sFixedThreadPool.execute(r);
    }

    //线程池中执行任务
    public static void runOnExecutorService(final Runnable r) {
        if (null == r) {
            return;
        }
        try {
            if (null == sCachedThreadPool) {
                sCachedThreadPool = Executors.newCachedThreadPool(TaskUtils.defaultThreadFactory());
            }
            sCachedThreadPool.execute(r);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 判断当前应用程序处于前台还是后台
     */
    public static boolean isApplicationBroughtToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceRunning(Context context, String serviceName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> infos = am.getRunningServices(100);
        boolean isAppServiceRunning = false;
        for (ActivityManager.RunningServiceInfo info : infos) {
            String name = info.service.getClassName();
            if (name.equals(serviceName)) {
                isAppServiceRunning = true;
                break;
            }
        }

        if (Consts.logEnabled()) {
            LogUtil.d("ActivityService isRun()", serviceName + " 程序  ...isServiceRunning......" + isAppServiceRunning);
        }
        return isAppServiceRunning;
    }

    /**
     * 获得属于桌面的应用的应用包名称
     *
     * @return 返回包含所有包名的字符串列表
     */
    private static List<String> getHomes(Context context) {
        List<String> names = new ArrayList<String>();
        PackageManager packageManager = context.getPackageManager();
        //属性
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : resolveInfo) {
            names.add(ri.activityInfo.packageName);
        }
        return names;
    }

    /**
     * 判断当前界面是否是桌面
     */
    public static boolean isHome(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        List<String> strs = getHomes(context);
        if (strs != null && strs.size() > 0) {
            return strs.contains(rti.get(0).topActivity.getPackageName());
        } else {
            return false;
        }
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = (Integer) field.get(o);
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    /**
     * The default thread factory.
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            if (Consts.logEnabled()) {
                Log.d(Consts.LOG_TEST_TAG, "DefaultThreadFactory newThread = " + t);
            }
            return t;
        }
    }
}
