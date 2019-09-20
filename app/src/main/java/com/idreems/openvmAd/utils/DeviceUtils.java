package com.idreems.openvmAd.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.persistence.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by lizy on 15-7-25.
 */
public class DeviceUtils {
    private static final String ZERO_MAC = "00:00:00:00:00:00";
    private static int screenWidth, screenHeight;

    public static int getScreenWidth() {
        if (screenWidth < 100) {
            DisplayMetrics displayMetrics = MyApplication.getContext().getResources().getDisplayMetrics();
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }
        return screenWidth;
    }

    public static int getScreenHeight() {
        if (screenHeight < 100) {
            DisplayMetrics displayMetrics = MyApplication.getContext().getResources().getDisplayMetrics();
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }
        return screenHeight;
    }

    //
    public static boolean isLegalMac(String mac) {
        if (TextUtils.isEmpty(mac)) {
            return false;
        }

        if (TextUtils.equals(ZERO_MAC, mac)) {
            return false;
        }

        if (ZERO_MAC.length() != mac.length()) {
            return false;
        }
        return true;
    }

    // 获取ethernet的mac地址（一次获取，便保存到本地文件中）
    public static String getEthernetMac() {
        Config config = Config.sharedInstance(MyApplication.getMyApplication());
        String ethernetMac = config.getValue(Config.ETHERNET_MAC_ID_KEY);
        if (!TextUtils.isEmpty(ethernetMac)) {
//            Const.logD("getEthernetMac = " + ethernetMac);
            return ethernetMac;
        }

        String mac = "";
        try {
            Process p = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bf = new BufferedReader(isr);
            String line = null;
            if ((line = bf.readLine()) != null) {
                mac = line;
            }
            bf.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mac = TextUtils.isEmpty(mac) ? mac : mac.toUpperCase();
//        Const.logD("getEthernetMac = " + mac);
        config.saveValue(Config.ETHERNET_MAC_ID_KEY, mac);
        return mac;
    }

    // 获取wifi的mac地址（一次获取，便保存到本地文件中）
    private static String getMacAddress() {
        Config config = Config.sharedInstance(MyApplication.getMyApplication());
        String wifiMac = config.getValue(Config.WIFI_MAC_ID_KEY);
        if (!TextUtils.isEmpty(wifiMac)) {
            return wifiMac;
        }

        WifiManager wifiMng = (WifiManager) MyApplication.getMyApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfor = wifiMng.getConnectionInfo();
        String mac = wifiInfor.getMacAddress();

        mac = TextUtils.isEmpty(mac) ? mac : mac.toUpperCase();
//        Const.logD("wifiMac = " + mac);
        config.saveValue(Config.WIFI_MAC_ID_KEY, mac);

        return mac;
    }

    public static String getWifiMac() {
        return getMacAddress();
    }


    // 获取mac地址，优先获取有线mac地址，其次获取wifi mac地址
    public static String getPreferedMac() {
        String ethernetMac = getEthernetMac();
        if (isLegalMac(ethernetMac)) {
            return ethernetMac;
        }
        return getWifiMac();
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String model() {
        return Build.MODEL;
    }

    public static String osSDK() {
        return Build.VERSION.SDK;
    }

    public static boolean isEmulator() {
        // FIXME 此种方法判断，待改进（Build.FINGERPRINT unknown经验证，不准确）
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static String getIMEI(Context context) {
        if (null == context) {
            return "";
        }
        return ((TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE)).getDeviceId();
    }

    public static Display getScreenMetrics(Context context) {
        if (null == context) {
            return null;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
//        int width = wm.getDefaultDisplay().getWidth();//屏幕宽度
//        int height = wm.getDefaultDisplay().getHeight();//屏幕高度
    }

    public static void takeSnapShot(String filePath, Activity activity) {
        if (TextUtils.isEmpty(filePath) || null == activity) {
            return;
        }

        View v = activity.getWindow().getDecorView().getRootView();
        v.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);

        OutputStream out = null;
        File imageFile = new File(filePath);

        try {
            out = new FileOutputStream(imageFile);
            // choose JPEG format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (FileNotFoundException e) {
            // manage exception
        } catch (IOException e) {
            // manage exception
        } finally {

            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception exc) {
            }

        }
    }

    /**
     * 重启机器
     */
    public static void reboot() {
        String cmd = "su -c reboot";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean checkDeviceHasNavigationBar(Context activity) {

        //通过判断设备是否有返回键、菜单键(不是虚拟键,是手机屏幕外的按键)来确定是否有navigation bar
        boolean hasMenuKey = ViewConfiguration.get(activity)
                .hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap
                .deviceHasKey(KeyEvent.KEYCODE_BACK);

        if (!hasMenuKey && !hasBackKey) {
            // 做任何你需要做的,这个设备有一个导航栏
            return true;
        }
        return false;
    }
}
