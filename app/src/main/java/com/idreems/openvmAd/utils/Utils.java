package com.idreems.openvmAd.utils;


import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.request.ImageRequest;
import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.file.DirUtil;
import com.idreems.openvmAd.file.FileUtil;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import pl.droidsonroids.gif.GifDrawable;

/**
 */
public class Utils {
    private static final String APK_SUFFIX = ".apk";
    private static final String UPDATE_FILE_SUFFIX = "_born200911022330";
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private static final int READ_BUFFER_SIZE = 4096*10;//40k

    public static String getApkSuffix() {
        return APK_SUFFIX;
    }

    public static String getUpdateFileSuffix() {
        return UPDATE_FILE_SUFFIX;
    }

    public static String getUpdateApkFileName(Context context) {
        if (null == context) {
            return "";
        }
        return context.getPackageName() + UPDATE_FILE_SUFFIX + APK_SUFFIX;
    }

    public static void safeStartSettings(Context context) {
        try {
            context.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException ex) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings")));
            } catch (ActivityNotFoundException e) {

            }
        }
    }

    public static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    public static boolean isFileExist(String fileFullName) {
        return new File(fileFullName).exists();
    }

    public static String getTempCacheDir(Context context) {
        return String.format("%s%s%s", context.getFilesDir(), File.separator, "mytempcache");
    }

    public static String getCacheDir(Context context) {
        return String.format("%s%s%s", context.getFilesDir(), File.separator, "mycache");
    }

    public static String getDirUnderFileDir(Context context, String subDir) {
        if (null == context || TextUtils.isEmpty(subDir)) {
            return "";
        }
        return String.format("%s%s%s", context.getFilesDir(), File.separator, subDir);
    }

    // 放到一个host和plugin都可以访问的地方
    public static String getApkCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "apks");
    }

    public static String getLogCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "logs");
    }

    public static String getWebviewCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "www");
    }

    public static String getWebviewCandidateCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "www2");
    }

    public static String getRebootLogCacheDir(Context context) {
        String r = String.format("%s%s%s", DirUtil.getAppCacheDir().getPath(), File.separator, "rebootlogs");
        new File(r).mkdirs();
        return r;
    }

    public static String getTempVideoCacheDir(Context context) {
        String r = String.format("%s%s%s", DirUtil.getAppCacheDir().getPath(), File.separator, "tmpvideos");
        new File(r).mkdirs();
        return r;
    }

    public static String getFrescoMainCacheDir(Context context) {
        String r = String.format("%s%s%s", DirUtil.getAppCacheDir().getPath(), File.separator, "fresco_main");
        new File(r).mkdirs();
        return r;
    }

    public static String getFrescoSmallImageCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir().getPath(), File.separator, "fresco_small");
    }

    public static String getVideoCacheFileName(IFSMedia media) {
        if (null == media) {
            return "temp" + Math.random();
        }

        String fileUri = media.getUrl();
        if (TextUtils.isEmpty(fileUri)) {
            fileUri = media.getName();
        }
        return Utils.md5(fileUri);
    }

    public static String getTmpApkheDir(Context context) {
        String r = String.format("%s%s%s", DirUtil.getAppCacheDir().getPath(), File.separator, "tmpapk");
        new File(r).mkdirs();
        return r;
    }

    public static String getFileNameByUrl(String url) {
        return Utils.md5(url);
    }

    public static void moveFile(String srcFileName, String destFileName, boolean rewrite) {
        File fromFile = new File(srcFileName);
        File toFile = new File(destFileName);

        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            return;
        }
        if (!fromFile.canRead()) {
            return;
        }
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (toFile.exists() && rewrite) {
            toFile.delete();
        }
        //当文件不存时，canWrite一直返回的都是false
        try {
            java.io.FileInputStream fosfrom = new java.io.FileInputStream(fromFile);
            FileOutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c); //将内容写到新文件当中
            }
            fosfrom.close();
            fosto.close();

            // delete original file
            if (fromFile.exists()) {
                fromFile.delete();
            }
        } catch (Exception ex) {
            Log.e("Test", ex.getMessage());
        }
    }

    public static void getDirFiles(List<String> fileList, String path) {
        File[] allFiles = new File(path).listFiles();
        if (null == allFiles) {
            return;
        }
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile()) {
                fileList.add(file.getName());
            } else if (file.isDirectory()) {
                getDirFiles(fileList, file.getAbsolutePath());
            }
        }
    }

    // -1代表参数非法或者文件非法
    public static int getVersionCode(Context context, String apkPath) {
        final int INVALID_VERSION_CODE = -1;
        if (null == context || TextUtils.isEmpty(apkPath) || !new File(apkPath).exists()) {
            return INVALID_VERSION_CODE;
        }
        try {
            final PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, 0);
            return null != info ? info.versionCode : INVALID_VERSION_CODE;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return INVALID_VERSION_CODE;
    }

    public static PackageInfo getPackageInfo(Context context, String packageName) {
        if (null == context || TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO 从apk的asset目录中提取所需的文件
    public static boolean extractFromApkAssets(final String apkFile, final String destPath, final String filteredFile, String saveAsFileName) {
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            destFile.mkdirs();
        }

        return unzip(new File(apkFile), destFile, filteredFile, saveAsFileName);
    }

    public static boolean unzip(final File file, final File destination, String filteredFileName, String saveAsFileName) {
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
            String workingDir = destination.getAbsolutePath() + "/";

            byte buffer[] = new byte[4096];
            int bytesRead;
            ZipEntry entry = null;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    File dir = new File(workingDir, entry.getName());
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
//                            Log.i(LOG_TAG, "[DIR] "+entry.getName());
                } else {
                    if (!TextUtils.isEmpty(filteredFileName) && !TextUtils.isEmpty(entry.getName()) &&
                            !entry.getName().contains(filteredFileName)) {
                        continue;
                    }
                    FileOutputStream fos = new FileOutputStream(workingDir + saveAsFileName);
                    while ((bytesRead = zin.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
//                            Log.i(LOG_TAG, "[FILE] "+entry.getName());
                }
            }
            zin.close();
            return true;

//                    Log.i(LOG_TAG, "COMPLETED in "+(ELAPSED_TIME/1000)+" seconds.");
        } catch (Exception e) {
//                    Log.e(LOG_TAG, "FAILED");
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 执行拷贝任务
     *
     * @param asset 需要拷贝的assets文件路径
     * @return 拷贝成功后的目标文件句柄
     * @throws IOException
     */
    public static boolean copyAssetFileTo(AssetManager assetManager,
                                          String assetName, String destDir) throws IOException {
        if (null == assetManager || TextUtils.isEmpty(assetName)
                || TextUtils.isEmpty(destDir)) {
            return false;
        }
        InputStream source = assetManager.open(assetName);
        File destinationFile = new File(destDir + File.separator
                + assetName);
        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }
        destination.close();
        return true;
    }


    public static String getVideoCacheDir(Context context) {
        final String r = String.format("%s%s%s", DirUtil.getAppCacheDir().getPath(), File.separator, "videos");
        new File(r).mkdirs();
        return r;
    }

    public static boolean isInFrescoCache(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        Uri uri = Uri.parse(url);
        boolean r = imagePipeline.isInBitmapMemoryCache(uri) | imagePipeline.isInDiskCacheSync(uri);
        if (Consts.logEnabled()) {
            Log.d(Consts.LOG_TEST_TAG, "image url is in cache = " + r + " url = " + url);
        }
        return r;
    }

    public static void displayImage(SimpleDraweeView view, Uri uri) {
        displayImage(view, uri, null);
    }

    public static void displayImage(final SimpleDraweeView view, Uri uri, ControllerListener controllerListener) {
        if (null == view || null == uri) {
            return;
        }
        try {
            AbstractDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder();
            boolean isGif = Consts.isGIF(uri.getPath());
            // GIF with Glide
            if (isGif) {
                playGif(view, uri, null);
                return;
            }

            builder.setUri(uri);
            if (null != controllerListener) {
                builder.setControllerListener(controllerListener);
            } else {
                builder.setControllerListener(new ControllerListener() {
                    @Override
                    public void onSubmit(String id, Object callerContext) {

                    }

                    @Override
                    public void onFinalImageSet(String id, @Nullable Object imageInfo, @Nullable Animatable animatable) {
                        if (null != view) {
                            view.getHierarchy().setPlaceholderImage(null);
                        }
                    }

                    @Override
                    public void onIntermediateImageSet(String id, @Nullable Object imageInfo) {

                    }

                    @Override
                    public void onIntermediateImageFailed(String id, Throwable throwable) {

                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {

                    }

                    @Override
                    public void onRelease(String id) {

                    }
                });
            }

            builder.setOldController(view.getController());
            builder.setAutoPlayAnimations(isGif);

            DraweeController controller = builder.build();
            view.setController(controller);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void playGif(final ImageView view, Uri uri, final DownloadUtil.OnDownloadListener listener) {
        if (null == view || null == uri) {
            return;
        }
        try {
            if (!UriUtil.isNetworkUri(uri)) {
                view.setImageDrawable(new GifDrawable(view.getContext().getContentResolver(), uri));
                return;
            }

            //先远程下载，再显示
            String url = uri.toString(), destFileDir, destFileName;
            destFileDir = Utils.getFrescoMainCacheDir(view.getContext());//将文件保存到fresco的缓存下，利用fresco的缓存机制，进行管理
            destFileName = String.format("%s.gif", MD5Util.getStringMD5(url));

            final String path = String.format("%s%s%s", destFileDir, File.separator, destFileName);
            boolean fileExist = FileUtil.isFileExist(path);
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "play gif, fileExist = " + fileExist + " url = " + url + " path = " + path);
            }

            if (fileExist) {
                if (null != listener) {
                    listener.onDownloadSuccess(new File(path));
                }
                try {
                    view.setImageDrawable(new GifDrawable(path));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }

            DownloadUtil.get().download(url, destFileDir, destFileName, new DownloadUtil.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(final File file) {
                    if (null != listener) {
                        listener.onDownloadSuccess(file);
                    }
                    MyApplication.getUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                view.setImageDrawable(new GifDrawable(path));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onDownloading(int progress) {

                }

                @Override
                public void onDownloadFailed(Exception e) {
                    if (null != listener) {
                        listener.onDownloadFailed(e);
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void displayImage(SimpleDraweeView view, ImageRequest imageRequest) {
        displayImage(view, imageRequest, null);
    }

    public static void displayImage(final SimpleDraweeView view, ImageRequest imageRequest, ControllerListener controllerListener) {
        if (null == view || null == imageRequest) {
            return;
        }
        try {
            AbstractDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder();
            final Uri uri = imageRequest.getSourceUri();
            boolean isGif = Consts.isGIF(uri.getPath());
            // GIF with Glide
            if (isGif) {
                playGif(view, uri, null);
                return;
            }

            builder.setImageRequest(imageRequest);

            if (null != controllerListener) {
                builder.setControllerListener(controllerListener);
            } else {
                builder.setControllerListener(new ControllerListener() {
                    @Override
                    public void onSubmit(String id, Object callerContext) {
                    }

                    @Override
                    public void onFinalImageSet(String id, @Nullable Object imageInfo, @Nullable Animatable animatable) {
                        if (null != view) {
                            view.getHierarchy().setPlaceholderImage(null);
                        }
                    }

                    @Override
                    public void onIntermediateImageSet(String id, @Nullable Object imageInfo) {

                    }

                    @Override
                    public void onIntermediateImageFailed(String id, Throwable throwable) {

                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {

                    }

                    @Override
                    public void onRelease(String id) {

                    }
                });
            }

            builder.setOldController(view.getController());
            builder.setAutoPlayAnimations(isGif);

            DraweeController controller = builder.build();
            view.setController(controller);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // 暂时支持rtmp和http
    public static boolean isPlayableUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("http") || lowerUrl.contains("rtmp") || isLocalFile(url);
    }

    public static boolean isLocalFile(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("file:");
    }

    public static String urlEncodeURL(String url) {
        try {
            String result = URLEncoderEx.encode(url, "UTF-8");
//            if(Const.logEnabled()) Const.logD(Const.PLAYER_TAG, "urlEncodeURL url = " + url);
//            if(Const.logEnabled()) Const.logD(Const.PLAYER_TAG, "urlEncodeURL     = " + result);
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static boolean isLiveShow(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return (url.contains("rtmp") | url.contains("m3u8"));
    }


    public static String getIP(String res) {
        StringBuilder ip = new StringBuilder();
        if (!TextUtils.isEmpty(res)) {
//            Matcher m = Pattern.compile("((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\:\\d{1,5})").matcher(res);
            Matcher m = Pattern.compile("((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))").matcher(res);
            while (m.find()) {
//                Log.d("This", "ip:" + m.group(1));
                ip.append(m.group(1));
                ip.append(".");
            }
            if (ip.length() > 0 && TextUtils.equals(".", ip.substring(ip.length() - 1))) {
                ip.deleteCharAt(ip.length() - 1);
            }
        }
        return ip.toString();
    }


    /*
    scale :缩放的比例
     */
    public static Bitmap decodeSampledBitmapFromFile(String pathName, int scale) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        //图片格式压缩
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // Calculate inSampleSize
        options.inSampleSize = scale;
        if (Consts.logEnabled()) {
            Log.d(Consts.LOG_TEST_TAG, "图片宽=" + options.outWidth + "图片高=" + options.outHeight + " options.inSampleSize = " + options.inSampleSize);
        }
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public static void clearJunk(final Context context, final List<String> inUseFileNames) {
        // 清除不再使用的视频文件
        // 遍历视频目录下的所有文件，删除
        TaskUtils.runOnExecutorService(new Runnable() {
            @Override
            public void run() {
                String dir = getVideoCacheDir(MyApplication.getContext());
                List<String> files = new ArrayList<String>();
                getDirFiles(files, dir);
                files.removeAll(inUseFileNames);

                for (int i = files.size() - 1; i >= 0; i--) {
                    File file = new File(String.format("%s%s%s", dir, File.separator, files.get(i)));
                    file.delete();
                    if (Consts.logEnabled()) {
                        Log.d(Consts.LOG_TAG, "remove junk file " + file.getPath());
                    }
                }
            }
        });
    }
}
