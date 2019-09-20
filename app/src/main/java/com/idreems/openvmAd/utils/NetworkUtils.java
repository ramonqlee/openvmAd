package com.idreems.openvmAd.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.callback.HttpCallback;
import com.idreems.openvmAd.multimedia.callback.LocalHttpCallback;
import com.idreems.openvmAd.multimedia.model.Result;
import com.idreems.openvmAd.persistence.Config;

import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Created by ramonqlee on 4/26/16.
 */
public class NetworkUtils {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final static Handler sUIHandler = new Handler(Looper.getMainLooper());
    private static final String GZIP_ENABLED = "";
    private static final String GZIP_DISABLED = "1";
    private static final long MAX_CACHE_COUNT = 1024;
    private static final Map<String, String> sNetworkMemoryCache = new HashMap<>();
    private static final Map<String, Long> sNetworkMemoryCacheTime = new HashMap<>();
    private static final Map<String, String> sPersistenceCacheKeysMap = new HashMap<>();
    static long lastLocalTimeInMills = 0;
    private static OkHttpClient sOkHttpClient;
    private static OkHttpClient sOkProxyHttpClient;

    private static void makesureOkHttpClientInited() {
        if (null != sOkHttpClient) {
            return;
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.proxy(Proxy.NO_PROXY);
        //创建TrustManager
        X509TrustManager xtm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };
        //这个好像是HOST验证
        X509HostnameVerifier hostnameVerifier = new X509HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }

            public void verify(String arg0, SSLSocket arg1) throws IOException {
            }

            public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
            }

            public void verify(String arg0, X509Certificate arg1) throws SSLException {
            }
        };
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{xtm}, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, xtm);
            builder.hostnameVerifier(hostnameVerifier);

        } catch (Exception e) {
            e.printStackTrace();
        }
        sOkHttpClient = builder
                .dispatcher(new Dispatcher(Executors.newCachedThreadPool(TaskUtils.defaultThreadFactory())))
                .build();
    }

    private static void makesureProxyOkHttpClientInitedIfNeeded() {
        if (null != sOkProxyHttpClient) {
            return;
        }
    }

    //如果有代理，返回代理版本;否则返回普通版本
    public static OkHttpClient getOkHttpClient(String url) {
        makesureProxyOkHttpClientInitedIfNeeded();
        makesureOkHttpClientInited();

        // 如果是访问本地局域网的ip，则用正常的client；否则的话，则优先使用代理的client
        String ip = Utils.getIP(url);
        if ((!TextUtils.isEmpty(ip) && NetUtil.internalIp(ip))) {
            if (Consts.logEnabled()) {
                Log.d(Consts.LOG_NET_TAG, "local net request,url = " + url);
            }
            return sOkHttpClient;
        }

        return (null != sOkProxyHttpClient) ? sOkProxyHttpClient : sOkHttpClient;
    }

    public static boolean isGzipEnabled(Context context) {
        //TODO 是否启动gzip
        if (null == context) {
            return true;
        }
        return !TextUtils.equals(Config.sharedInstance(context).getValue(Config.GZIP_ENABLED_KEY), GZIP_DISABLED);
    }

    public static void setGzip(boolean enable, Context context) {
        if (null == context) {
            return;
        }
        final String val = enable ? GZIP_ENABLED : GZIP_DISABLED;
        Config.sharedInstance(context).saveValue(Config.GZIP_ENABLED_KEY, val);
    }

    //true为该body已加密
    public static String getBodyString(Response response, boolean isAES) {
        if (null == response || !response.isSuccessful()) {
            return "";
        }
        try {
            ResponseBody body = response.body();
            if (null == body) {
                return "";
            }
            byte[] bytes = body.bytes();
            if (null == bytes || 0 == bytes.length) {
                return "";
            }
            Charset UTF_8 = Charset.forName("UTF-8");
            MediaType mediaType = body.contentType();
            Charset charset = mediaType != null ? mediaType.charset(UTF_8) : UTF_8;
            String dataBody = new String(bytes, charset.name()).trim();
            //remove unused string
            int head = dataBody.indexOf("{");
            int tail = dataBody.lastIndexOf("}");
            if (head >= 0 && tail >= 0) {
                dataBody = dataBody.substring(head, tail + 1);
            }


//            Log.d("TTT",str2HexStr(dataBody));
             /* if (5 <= Integer.parseInt(Consts.PROTOCOL_VERSION) &&!(dataBody.indexOf("{") == 0 && dataBody.lastIndexOf("}") == dataBody.length()-1 && dataBody.contains("\":"))){
                if (MessyCodeCheck.isMessyCode(dataBody)){
                    byte[] bytes1 = new byte[bytes.length];
                    System.arraycopy(bytes,0,bytes1,0,bytes.length);
                    String str = new String(mCrypt.decrypt(bytes1));
                    dataBody = str.trim();
                }
            }*/
            return dataBody;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static void getAndResponseOnMainThread(final String url, final HttpCallback callback) {
        getAndResponseOnMainThread(url, callback, 0);
    }

    public static void getAndResponseOnMainThread(final String url, final HttpCallback callback, final long cacheTimeInMs) {
        getAndResponseOnMainThread(url, callback, cacheTimeInMs, true);
    }

    public static void getLocalResponseOnThread(final String url, final String localUrl, final LocalHttpCallback callback, final long cacheTimeInMs) {
        if (!TextUtils.isEmpty(localUrl)) {
            HttpCallback callback1 = new HttpCallback() {
                @Override
                public void onFailure(IOException e) {
                    getAndResponseOnThread(url, callback, cacheTimeInMs, true, null);
                }

                @Override
                public void onResponse(String response, boolean isCache) {
                    getAndResponseOnThread(url, callback, cacheTimeInMs, true, response);
                }
            };
            NetworkUtils.getAndResponseOnThread(localUrl, callback1, cacheTimeInMs);
        } else {
            getAndResponseOnThread(url, callback, cacheTimeInMs, true, null);
        }
    }

    public static void getAndResponseOnThread(final String url, final LocalHttpCallback callback, final long cacheTimeInMs, boolean cacheInMemory, final String localresponse) {
        getAndResponseOnThread(url, new HttpCallback() {
            @Override
            public void onFailure(final IOException e) {
                if (null == callback) {
                    return;
                }
                callback.onFailure(e, localresponse);
            }

            @Override
            public void onResponse(final String response, boolean isCache) {
                if (null == callback) {
                    return;
                }
                callback.onResponse(response, localresponse);
            }
        }, cacheTimeInMs, cacheInMemory);
    }

    public static void getAndResponseOnMainThread(final String url, final HttpCallback callback, final long cacheTimeInMs, final boolean cacheInMemory) {
        getAndResponseOnThread(url, new HttpCallback() {
            @Override
            public void onFailure(final IOException e) {
                if (null == callback) {
                    return;
                }

                if (Looper.myLooper() == Looper.getMainLooper()) {
                    callback.onFailure(e);
                } else {
                    sUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(e);
                        }
                    });
                }

            }

            @Override
            public void onResponse(final String response, boolean isCache) {
                if (null == callback) {
                    return;
                }

                if (Looper.myLooper() == Looper.getMainLooper()) {
                    callback.onResponse(response, false);
                    return;
                }

                sUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(response, false);
                    }
                });
            }
        }, cacheTimeInMs, cacheInMemory);
    }

    public static void getAndResponseOnThread(final String url, final Callback callback) {
        try {
            if (Consts.logEnabled()) {
                LogUtil.d(Consts.LOG_NET_TAG, "request new url:" + url);
            }

            Request.Builder builder = new Request.Builder().url(url);
            if (!NetworkUtils.isGzipEnabled(MyApplication.getApplication())) {
                builder.addHeader("Accept-Encoding", "");
            }

            Request request = builder
                    .build();

            getOkHttpClient(url).newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (null != callback) {
                        callback.onFailure(call, e);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (null != callback) {
                            callback.onResponse(call, response);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            if (null != callback) {
                callback.onFailure(null, null);
            }
            ex.printStackTrace();
        }

    }

    public static void postResponsebyGosn(String sign, String time, String appJdId, String url, String json, final Callback callback) {
        String appid = "";
        try {
            appid = URLEncoder.encode(appJdId, "UTF-8");

            if (Consts.logEnabled()) {
                LogUtil.d("JD", "JD AD appid->" + appid + "  time->" + time + "  sign->" + sign);
                LogUtil.d("JD", "JD URL->" + url);
            }

            RequestBody body = RequestBody.create(JSON, json);

            //创建一个请求对象，传入URL地址和相关数据的键值对的对象
            Request request = new Request.Builder()
                    .addHeader("content-type", "application/json;charset:utf-8")
                    .addHeader("time", time)
                    .addHeader("appId", appid)
                    .addHeader("sign", sign)
                    .url(url)
                    .post(body).build();
            //创建一个能处理请求数据的操作类
            Call call = getOkHttpClient(url).newCall(request);

            //使用异步任务的模式请求数据
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (Consts.logEnabled()) {
                        Log.d("TAG", "JD onFailure--->" + e.getMessage());
                    }
                    if (null != callback) {
                        callback.onFailure(call, e);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    /** 4. 检验返回码*/
                    int statusCode = response.code();
                    if (statusCode == 200) {
                        if (null != callback) {
                            callback.onResponse(call, response);
                        }
                    } else {
                        if (null != callback) {
                            callback.onFailure(call, new IOException());
                        }
                    }
                    if (Consts.logEnabled()) {
                        Log.d("TAG", "NetworkUtils statusCode--->" + statusCode);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getAndResponsebyGosn(final String url, String json, final Callback callback) {


//        if (Consts.isLocalServer()) {
//            String ip = LocalhostUtils.getInstance(MyApplication.getContext()).getLocalServerIp();
//            if (!TextUtils.isEmpty(ip) && !url.contains("http://" + ip + ":" + LocalhostUtils.interPort)) {
//                if (null != callback) {
//                    callback.onFailure(null, null);
//                }
//                return;
//            }
//        }
        RequestBody body = RequestBody.create(JSON, json);
        //创建一个请求对象，传入URL地址和相关数据的键值对的对象
        Request request = new Request.Builder()
                .addHeader("content-type", "application/json;charset:utf-8")
//                .addHeader("Accept-Encoding","gzip")
                .addHeader("X-protocol-ver", "4.0")
                .url(url)
                .post(body).build();
        //创建一个能处理请求数据的操作类
        Call call = getOkHttpClient(url).newCall(request);

        //使用异步任务的模式请求数据
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                /** 4. 检验返回码*/
                int statusCode = response.code();
                if (statusCode == 200) {
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } else {
                    if (null != callback) {
                        callback.onFailure(call, new IOException());
                    }
                }
                if (Consts.logEnabled()) Log.d("TAG", "NetworkUtils statusCode--->" + statusCode);
            }
        });
    }

    public static void getAndResponseOnThread(final String url, final HttpCallback callback, final long cacheTimeInMs) {
        getAndResponseOnThread(url, callback, cacheTimeInMs, true);
    }

    public static void getAndResponseOnThread(final String url, final HttpCallback callback, final long cacheTimeInMs, final boolean cacheInMemory) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        // 是否检查缓存(这个key需要和版本等无关，确保不同版本之间，相同的接口key是一致的)
        // 去除 versioinCode,versionName,sign，形成key
        String toRemoves[] = {"versionCode", "versionName", "sign"};
        String tmp[] = url.split("&");
        String keyFormer = new String(url);
        for (String key : tmp) {
//            if(Consts.logEnabled()) LogUtil.d(Consts.LOG_TEST_TAG, "key: " + key);
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            for (String toRemove : toRemoves) {
                if (key.contains(toRemove)) {
                    keyFormer = keyFormer.replace(key, "");
                }
            }
        }

        final String PREVOIOUS_VERSION_CACHE_KEY = Utils.md5(keyFormer);//上一个版本的缓存key
        final String CACHE_KEY = Utils.md5(keyFormer);//当前版本的key
        if (cacheTimeInMs <= 0) {
            request(PREVOIOUS_VERSION_CACHE_KEY, CACHE_KEY, url, callback, cacheTimeInMs, cacheInMemory);
            return;
        }

        String response = getCache(CACHE_KEY, cacheTimeInMs, cacheInMemory, true);
        if (!TextUtils.isEmpty(response)) {

            if (Consts.logEnabled())
                LogUtil.d(Consts.LOG_NET_TAG, "cacheInMemory = " + cacheInMemory + " expired after = " + cacheTimeInMs + "---> return net cache for " + url);
            if (!TextUtils.isEmpty(getCache(CACHE_KEY, cacheTimeInMs, cacheInMemory, false))) {//没失效的话，不用继续了

                if (null != callback) {
                    callback.onResponse(response, true);
                }
                return;
            }
        }

        request(PREVOIOUS_VERSION_CACHE_KEY, CACHE_KEY, url, callback, cacheTimeInMs, cacheInMemory);
    }

    public static void request(final String previousVersionCacheKey, final String cacheKey, final String url, final HttpCallback callback, final long cacheTimeInMs, final boolean cacheInMemory) {
        // 和服务器进行时间同步
        final long startLocalTimeInMills = System.currentTimeMillis();
        getAndResponseOnThread(url, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (null == callback) {
                            return;
                        }

                        // 是否检查缓存(这个key需要和版本等无关，确保不同版本之间，相同的接口key是一致的)
                        // 去除 versioinCode,versionName,sign，形成key
                        String toRemoves[] = {"versionCode", "versionName", "sign"};
                        String tmp[] = url.split("&");
                        String keyFormer = new String(url);
                        for (String key : tmp) {
                            if (TextUtils.isEmpty(key)) {
                                continue;
                            }
                            for (String toRemove : toRemoves) {
                                if (key.contains(toRemove)) {
                                    keyFormer = keyFormer.replace(key, "");
                                }
                            }
                        }

                        if (Consts.logEnabled()) {
                            LogUtil.d(Consts.LOG_NET_TAG, "onFailure but try to recover from local cache,CACHE_KEY= " + cacheKey + " ,url =" + url);
                        }
                        boolean fromMemoryCache = true;
                        String response = getCache(cacheKey, cacheTimeInMs, fromMemoryCache, true);
                        if (!TextUtils.isEmpty(response)) {

                            if (Consts.logEnabled()) {
                                LogUtil.d(Consts.LOG_NET_TAG, "return memory cache in onFailure,url = " + url);
                            }
                            if (!TextUtils.isEmpty(getCache(cacheKey, cacheTimeInMs, fromMemoryCache, true))) {//没失效的话，不用继续了
                                callback.onResponse(response, true);
                                return;
                            }
                        }

                        fromMemoryCache = false;
                        response = getCache(cacheKey, cacheTimeInMs, fromMemoryCache, true);
                        if (!TextUtils.isEmpty(response)) {

                            if (Consts.logEnabled())
                                LogUtil.d(Consts.LOG_NET_TAG, "return file cache in onFailure,url = " + url);
                            if (!TextUtils.isEmpty(getCache(cacheKey, cacheTimeInMs, fromMemoryCache, true))) {//没失效的话，不用继续了
                                callback.onResponse(response, true);
                                return;
                            }
                        }

                        if (Consts.logEnabled()) {
                            LogUtil.d(Consts.LOG_NET_TAG, " no cache in onFailure " + url);
                        }
                        callback.onFailure(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (null == response || !response.isSuccessful() || null == response.body()) {
                                if (null != callback) {
                                    callback.onResponse("", false);
                                }
                                return;
                            }
                            final String resp = NetworkUtils.getBodyString(response, false);
                            if (null != callback) {
                                callback.onResponse(resp, false);
                            }
                            if (TextUtils.isEmpty(resp)) {
                                return;
                            }
                            try {
                                // 待修改为服务器的时间

                                JSONObject obj = new JSONObject(resp);
                                if (Math.abs(startLocalTimeInMills - lastLocalTimeInMills) > Consts.ONE_MINUTE_IN_MS) {
                                    lastLocalTimeInMills = startLocalTimeInMills;
                                    JSONObject result = obj.optJSONObject("result");
                                    if (null != result) {
                                        long timestamp = result.optLong("timestamp");
                                        if (timestamp > 53344665) {//现在时间要大于1971-09-10 17:57:45
                                            long serverTimeInMills = timestamp * 1000;
                                            long returnLocalTimeInMills = System.currentTimeMillis();
                                            serverTimeInMills = serverTimeInMills + (returnLocalTimeInMills - startLocalTimeInMills) / 2;

                                            // 保存到本地中，下次可以直接使用
                                            final long timeOffset = serverTimeInMills - returnLocalTimeInMills;
                                            TimeUtil.setLastCheckTimeOffsetInMs(MyApplication.getApplication(), timeOffset);
                                        }
                                    }
                                }

                                // cache now
                                boolean success = Result.isSuccess(obj.optInt("code"));
                                if (!success) {
                                    JSONObject resultJson = obj.optJSONObject("result");
                                    if (null != resultJson) {
                                        success = Result.isSuccess(resultJson.optInt("code"));
                                    }
                                }

                                // cache in file
                                if (!TextUtils.isEmpty(resp) && success) {
                                    putIntoCache(cacheKey, resp, cacheInMemory, false);
                                    putIntoCache(previousVersionCacheKey, "", false, true);//删除上一个版本的缓存
                                    if (Consts.logEnabled()) {
                                        LogUtil.d(Consts.LOG_NET_TAG, "cacheInMemory = " + cacheInMemory + " expired after = " + cacheTimeInMs + " cacheKey =" + cacheKey + "--->save net cache url=" + url);
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            if (null != callback) {
                                callback.onResponse("", false);
                            }
                        }
                    }
                }
        );
    }

    // 上传单个文件接口
    public static void postFile(String filePath, MediaType mediaType, String url, final Callback callback) {
        if (TextUtils.isEmpty(filePath) || null == mediaType || TextUtils.isEmpty(url)) {
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filePath, RequestBody.create(mediaType, new File(filePath)))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        getOkHttpClient(url).newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != response && null != response.body()) {
                        response.body().close();
                    }
                }
            }
        });
    }

    public static void post(String url, Map<String, String> maps, final Callback callback) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (Consts.logEnabled()) {
            Log.d(Consts.AD_TAG, "post url = " + url);
        }
        FormBody.Builder builder = new FormBody.Builder();
        if (null != maps && maps.size() > 0) {
            for (Map.Entry<String, String> entry : maps.entrySet()) {
                if (TextUtils.isEmpty(entry.getValue()) || TextUtils.isEmpty(entry.getKey())) {
                    continue;
                }

                builder.add(entry.getKey(), entry.getValue());
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        getOkHttpClient(url).newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != response && null != response.body()) {
                        response.body().close();
                    }
                }
            }
        });
    }

    public static boolean isConnected(@NonNull Context context) {
        if (null == context) {
            return false;
        }
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isWifiConnected(@NonNull Context context) {
        return isConnected(context, ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isMobileConnected(@NonNull Context context) {
        return isConnected(context, ConnectivityManager.TYPE_MOBILE);
    }

    private static boolean isConnected(@NonNull Context context, int type) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            NetworkInfo networkInfo = connMgr.getNetworkInfo(type);
            return networkInfo != null && networkInfo.isConnected();
        } else {
            return isConnected(connMgr, type);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isConnected(@NonNull ConnectivityManager connMgr, int type) {
        Network[] networks = connMgr.getAllNetworks();
        NetworkInfo networkInfo;
        for (Network mNetwork : networks) {
            networkInfo = connMgr.getNetworkInfo(mNetwork);
            if (networkInfo != null && networkInfo.getType() == type && networkInfo.isConnected()) {
                return true;
            }
        }
        return false;
    }

    public static String getSignedUrl(String url, Map<String, String> parameters) {
        return getSignedUrl(url, parameters, true);
    }

    public static String getSignedUrl(String url, Map<String, String> parameters, boolean withMac) {
        if (TextUtils.isEmpty(url) || null == parameters || 0 == parameters.size()) {
            return url;
        }

        // 增加协议版本信息
        parameters.put("version", Consts.PROTOCOL_VERSION);
        parameters.put("osmodel", DeviceUtils.model());
        parameters.put("sdk", DeviceUtils.osSDK());

        String deviceId = parameters.get("device_id");
        final String preferedMac = DeviceUtils.getPreferedMac();
        if (TextUtils.isEmpty(deviceId)) {
            parameters.put("device_id", preferedMac);
        }

        if (withMac && TextUtils.isEmpty(parameters.get("mac"))) {
            parameters.put("mac", preferedMac);
        }

        // 增加当前app的版本信息
        String versionCode = parameters.get("versionCode");

        StringBuilder r = new StringBuilder(url);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (-1 == r.indexOf("?")) {
                r.append('?');
            } else {
                r.append('&');
            }
            r.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }

        // 增加了签名验证
//        if (r.toString().contains(Consts.getAdBaseUrl()))
        {
            if (-1 == r.indexOf("?")) {
                r.append('?');
            } else {
                r.append('&');
            }
            r.append(String.format("sign=%s", getSign(parameters)));
        }
        return r.toString().trim();
    }

    /**
     * 参数数组按照键名进行升序排序，然后键值用空格连接成字符串$string，md5加密,MD5($string.$secret)。秘钥：fensihudongads20160801ifensi
     */
    private static String getSign(Map<String, String> parameters) {
        if (null == parameters || parameters.isEmpty()) {
            return "";
        }

        final String KEY = "fensihudongads20160801ifensi";
        //  排序
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(parameters.keySet());
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                if (TextUtils.isEmpty(lhs) || TextUtils.isEmpty(rhs)) {
                    return 0;
                }
                return lhs.compareTo(rhs);
            }
        });

        StringBuilder signBuilder = new StringBuilder();
        //remove m,c,a from server request
        final List<String> JUNK = new ArrayList<>();
        JUNK.add("m");
        JUNK.add("c");
        JUNK.add("a");
        for (String key : list) {
            if (-1 != JUNK.indexOf(key)) {
                continue;
            }
            signBuilder.append(String.format("%s%s", key, parameters.get(key)));
        }
        signBuilder.append(KEY);
        return Utils.md5(signBuilder.toString());
    }

    private static String getCacheTimeKey(String key) {
        return key + "_cachetimekey";
    }

    public static void clearMemoryCache(Context context) {
        sNetworkMemoryCache.clear();
        sNetworkMemoryCacheTime.clear();
    }

    public static void clearPersistenceCache(Context context) {
    }


    private static void putIntoCache(String key, String val, boolean cacheInMemory, boolean clearCacheOnly) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        if (clearCacheOnly) {
//            clearOldConfig(MyApplication.getApplication(), key);
            return;
        }

        final String CACHE_TIME_KEY = getCacheTimeKey(key);

        final long currentTime = TimeUtil.getCheckedCurrentTimeInMills();
        if (cacheInMemory) {
            if (sNetworkMemoryCache.size() > MAX_CACHE_COUNT) {
                sNetworkMemoryCacheTime.clear();
                sNetworkMemoryCache.clear();
            }
            sNetworkMemoryCacheTime.put(CACHE_TIME_KEY, currentTime);
            sNetworkMemoryCache.put(key, val);
            if (Consts.logEnabled())
                LogUtil.d(Consts.LOG_NET_TAG, "cacheInMemory = " + cacheInMemory + " key = " + key);
//            return;--whatever,cache in file
        }

        Config config = new Config(MyApplication.getApplication(), key);
        config.saveValue(key, val);
        config.saveValue(CACHE_TIME_KEY, TextUtils.isEmpty(val) ? "" : String.valueOf(currentTime));//值为空了，记录时间也就没意义了
        if (Consts.logEnabled())
            LogUtil.d(Consts.LOG_NET_TAG, "cacheInMemory = " + cacheInMemory + " key = " + key);
        // 记录下，方便清除
        sPersistenceCacheKeysMap.put(key, CACHE_TIME_KEY);
    }

    private static String getCache(String key, long cacheTimeInMs, boolean inMemCache, boolean ignoreObsolete) {
        if (TextUtils.isEmpty(key)) {
            return "";
        }
        final String CACHE_KEY = key;
        final String CACHE_TIME_KEY = getCacheTimeKey(CACHE_KEY);

        String cacheVal = "";
        Long lastCacheTime = null;
        // 内存缓存
        if (inMemCache) {
            cacheVal = sNetworkMemoryCache.get(key);
            lastCacheTime = sNetworkMemoryCacheTime.get(CACHE_TIME_KEY);
        } else {
            // 本地缓存，写入单独的文件
            final Config config = new Config(MyApplication.getApplication(), CACHE_KEY);
            String cacheTimeInMsStr = config.getValue(CACHE_TIME_KEY);
            cacheVal = config.getValue(CACHE_KEY);

            // 记录下，方便清除
            sPersistenceCacheKeysMap.put(CACHE_KEY, CACHE_TIME_KEY);

            if (!TextUtils.isEmpty(cacheTimeInMsStr) && TextUtils.isDigitsOnly(cacheTimeInMsStr)) {
                try {
                    lastCacheTime = Long.valueOf(cacheTimeInMsStr);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (Consts.logEnabled()) {
//            LogUtil.d(Consts.LOG_NET_TAG, "lastCacheTime = " + lastCacheTime + " key = " + key + " inMemCache = " + inMemCache);
            // 检查合法性：是否为空；是否过期
            if (null == lastCacheTime || TextUtils.isEmpty(cacheVal)) {
                return "";
            }
        }

        if (ignoreObsolete) {
            return cacheVal;
        }

        if (lastCacheTime != null) {
            if (Math.abs(TimeUtil.getCheckedCurrentTimeInMills() - lastCacheTime) <= cacheTimeInMs) {
                return cacheVal;
            }
        }


        return "";
    }

    //        url format:http://192.168.7.84:8000/ad/list/1/2/3/4
//        1：mac
//        2 : province_id
//        3 : city_id
//        4 :ad_position
    public static String getLocalAdUrl(String url, Map<String, String> parameters) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        StringBuilder r = new StringBuilder(url);
        // mac
        if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
            r.append("/");
        }
        r.append(DeviceUtils.getPreferedMac());

        // province_id
        if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
            r.append("/");
        }
        r.append(parameters.get("province_id"));

        // city_id
        if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
            r.append("/");
        }
        r.append(parameters.get("city_id"));

        // ad_position
        if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
            r.append("/");
        }
        if (TextUtils.isEmpty(parameters.get("ad_position"))) {
            r.append("0");
        } else {
            r.append(parameters.get("ad_position"));
        }

        return r.toString();
    }

    public static String getLocalUrl(String url, Map<String, String> parameters) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        StringBuilder r = new StringBuilder(url);
        try {

            if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
                r.append("/");
            }
            r.append(parameters.get("mac"));
            if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
                r.append("/");
            }
            r.append(parameters.get("localmac"));
            if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
                r.append("/");
            }
            r.append(parameters.get("versionName"));
            if (!TextUtils.equals(r.substring(r.length() - 1), "/")) {
                r.append("/");
            }
            r.append(parameters.get("model"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r.toString();
    }

    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {

                NetworkInterface networkInterface = en.nextElement();
                if (Build.VERSION.SDK_INT >= 9) {
                    if (!networkInterface.isUp())
                        continue;

                    List<InterfaceAddress> iaList = networkInterface.getInterfaceAddresses();
                    for (InterfaceAddress ia : iaList) {
                        InetAddress inetAddress = ia.getAddress();

                        if (!inetAddress.isLoopbackAddress()
                                && inetAddress instanceof Inet4Address) {
                            String hostAddress = inetAddress.getHostAddress();
                            if (hostAddress.indexOf(":") > 0) {

                            } else {
                                return hostAddress;
                            }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String getGatewayAddress() {
        try {
            Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
            while (eni.hasMoreElements()) {

                NetworkInterface networkCard = eni.nextElement();
                if (!networkCard.isUp()) { // 判断网卡是否在使用
                    continue;
                }

                String DisplayName = networkCard.getDisplayName();

                List<InterfaceAddress> addressList = networkCard.getInterfaceAddresses();
                Iterator<InterfaceAddress> addressIterator = addressList.iterator();
                while (addressIterator.hasNext()) {
                    InterfaceAddress interfaceAddress = addressIterator.next();
                    InetAddress address = interfaceAddress.getAddress();
                    if (!address.isLoopbackAddress()) {
                        String hostAddress = address.getHostAddress();

                        if (hostAddress.indexOf(":") > 0) {
                        } else {
                            String maskAddress = calcMaskByPrefixLength(interfaceAddress.getNetworkPrefixLength());
                            String gateway = calcSubnetAddress(hostAddress, maskAddress);

                            String broadcastAddress = null;
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast != null)
                                broadcastAddress = broadcast.getHostAddress();

                            Log.d("GGG", "DisplayName    =   " + DisplayName);
                            Log.d("GGG", "address        =   " + hostAddress);
                            Log.d("GGG", "mask           =   " + maskAddress);
                            Log.d("GGG", "gateway        =   " + gateway);
                            Log.d("GGG", "broadcast      =   " + broadcastAddress + "\n");
                            Log.d("GGG", "----- NetworkInterface  Separator ----\n\n");
                            return gateway;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String calcMaskByPrefixLength(int length) {

        int mask = 0xffffffff << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int maskParts[] = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }

        String result = "";
        result = result + maskParts[0];
        for (int i = 1; i < maskParts.length; i++) {
            result = result + "." + maskParts[i];
        }
        return result;
    }

    private static String calcSubnetAddress(String ip, String mask) {
        String result = "";
        try {
            // calc sub-net IP
            InetAddress ipAddress = InetAddress.getByName(ip);
            InetAddress maskAddress = InetAddress.getByName(mask);

            byte[] ipRaw = ipAddress.getAddress();
            byte[] maskRaw = maskAddress.getAddress();

            int unsignedByteFilter = 0x000000ff;
            int[] resultRaw = new int[ipRaw.length];
            for (int i = 0; i < resultRaw.length; i++) {
                resultRaw[i] = (ipRaw[i] & maskRaw[i] & unsignedByteFilter);
            }

            // make result string
            result = result + resultRaw[0];
            for (int i = 1; i < resultRaw.length; i++) {
                result = result + "." + resultRaw[i];
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void upLoadApecialUrl(Map<String, String> parameters, final HttpCallback callback) {

    }

    public static void checkUpdatePost(Map<String, String> parameters, final HttpCallback callback) {

    }


    /**
     * 获取本机 ip地址
     *
     * @return
     */
    public static String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
        if (isWifiApEnabled(context)) {
            return InetAddress.getByName("192.168.43.255");
        }
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

    protected static Boolean isWifiApEnabled(Context context) {
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = manager.getClass().getMethod("isWifiApEnabled");
            return (Boolean) method.invoke(manager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void postClickNumResponse(String url, Map<String, String> maps, final Callback callback) {

        FormBody.Builder builder = new FormBody.Builder();
        if (null != maps && maps.size() > 0) {
            for (Map.Entry<String, String> entry : maps.entrySet()) {
                if (TextUtils.isEmpty(entry.getValue()) || TextUtils.isEmpty(entry.getKey())) {
                    continue;
                }

                builder.add(entry.getKey(), entry.getValue());
            }
        }

        //创建一个请求对象，传入URL地址和相关数据的键值对的对象
        Request request = new Request.Builder()
                .addHeader("content-type", "application/json;charset:utf-8")
                .removeHeader("User-Agent")
//                .addHeader("User-Agent", UAUtils.getUserAgent())
                .url(url)
                .post(builder.build()).build();
        //创建一个能处理请求数据的操作类
        Call call = getOkHttpClient(url).newCall(request);

        //使用异步任务的模式请求数据
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                /** 4. 检验返回码*/
                int statusCode = response.code();
                if (statusCode == 200) {
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } else {
                    if (null != callback) {
                        callback.onFailure(call, new IOException());
                    }
                }
                if (Consts.logEnabled()) Log.d("TAG", "NetworkUtils statusCode--->" + statusCode);
            }
        });
    }


    //返回HttpURLConnection连接，如果是纯本地存储的话，返回代理的HttpURLConnection
    public static final HttpURLConnection getHttpURLConnection(String url, boolean proxyIfNeeded) {
        try {
            URL realURL = new URL(url);
            //如果是纯本地版本，则设置本地存储为代理，进行报文中转
            Proxy proxy = null;
            return (null == proxy) ? (HttpURLConnection) realURL.openConnection() : (HttpURLConnection) realURL.openConnection(proxy);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
