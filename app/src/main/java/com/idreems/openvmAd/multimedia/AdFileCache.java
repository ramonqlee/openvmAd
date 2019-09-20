package com.idreems.openvmAd.multimedia;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequest;
import com.idreems.openvmAd.MyApplication;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.utils.TaskUtils;
import com.idreems.openvmAd.utils.Utils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 7/29/16.
 */
public class AdFileCache {
    private static final int SINGLE_FILE_MAX_CACHE_SIZE = 2 * 4 * ByteConstants.MB;// 只缓存小文件(15秒广告的大小：2M*15/8=4,放大一倍)

    // 预加载所有的广告
    // 1. 视频缓存到专门的视频目录下
    // 2. 图片用fresco预先加载
    public static void preloadMediaSet(List<IFSMedia> ifsMediaList) {
        List<String> inUseFiles = new ArrayList<>();
        for (IFSMedia item : ifsMediaList) {
            if (null == item) {
                continue;
            }
            preloadMedia(item);
            if (!item.isImage()) {
                inUseFiles.add(Utils.getVideoCacheFileName(item));
            }
        }
        Utils.clearJunk(MyApplication.getApplication(), inUseFiles);
    }

    public static void preloadMedia(IFSMedia media) {
        if (!Utils.isPlayableUrl(media.getUrl())) {
            return;
        }

        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "preloadAdFile url=" + media.getUrl());
        }

        if (media.isImage()) {
            Fresco.getImagePipeline().prefetchToDiskCache(ImageRequest.fromUri(media.getUrl()), MyApplication.getContext());
        } else {
            startVideoCache(media, true);
        }
    }


    //forceDownload：强制下载，对于开启代理的情况，也强制下载
    public static void startVideoCache(IFSMedia media, boolean forceDownload) {
        if (!forceDownload) {
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "VideoUtil.isHttpCacheServerOpen");
            }
            return;
        }

        String url = media.getUrl();

        // FIXME 待增加缓存
        if (TextUtils.isEmpty(url)) {
            return;
        }

        //FIXME 查看当前剩余的空间，如果没有足够的空间，就不要缓存了
        String cacheName = Utils.getVideoCacheFileName(media);
        startVideoCache(cacheName, url);
    }

    public static void startVideoCache(String cacheName, String url) {
        // 忽略直播流
        if (Utils.isLiveShow(url)) {
            return;
        }

        if (TextUtils.isEmpty(cacheName) || TextUtils.isEmpty(url)) {
            return;
        }
        final String cachedFileName = String.format("%s%s%s", Utils.getVideoCacheDir(MyApplication.getContext()), File.separator, cacheName);
        if (Utils.isFileExist(cachedFileName)) {
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "startVideoCache url's file existed cacheName = " + cacheName + " -->" + cachedFileName);
            }
            return;
        }

        if (Consts.logEnabled()) {
            Log.d(Consts.PLAYER_TAG, "startVideoCache start download url's file  as " + cachedFileName + " " + url);
        }

        final String tempCachedFileName = String.format("%s%s%s", Utils.getTempVideoCacheDir(MyApplication.getContext()), File.separator, cacheName);
        final BaseDownloadTask task = FileDownloader.getImpl().create(url).setPath(tempCachedFileName);
        task.setListener(new FileDownloadListener() {
            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                // 只缓存小文件(15秒广告的大小：2M*15/8=4,放大一倍)
                if (totalBytes < SINGLE_FILE_MAX_CACHE_SIZE) {
                    return;
                }

                task.pause();

                if (Consts.logEnabled()) {
                    Log.e(Consts.LOG_TAG, "task pause for larger file " + task.getUrl());
                }
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                // TODO 移动到最终的缓存目录下
                if (null == task || TextUtils.isEmpty(task.getPath())) {
                    return;
                }
                final String filePath = task.getPath();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Utils.moveFile(filePath, cachedFileName, true);
                    }
                };
                TaskUtils.runOnExecutorService(r);
                if (Consts.logEnabled()) {
                    Log.d(Consts.PLAYER_TAG, "startVideoCache completed,url=" + task.getUrl());
                }
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
            }

            @Override
            protected void warn(BaseDownloadTask task) {
            }
        });

        if (FileDownloadStatus.isIng(FileDownloader.getImpl().getStatus(task.getUrl(), task.getPath()))) {
            if (Consts.logEnabled()) {
                Log.d(Consts.PLAYER_TAG, "startVideoCache url's file is downloading " + url);
            }
            return;
        }
        task.start();
    }

    public static void clearVideoCache(final Context context) {
        /**
         *   /data/data/com.xxx.xxx/cache - 应用内缓存（注：对应方法getCacheDir()）
         /data/data/com.xxx.xxx/databases - 应用内数据库
         /data/data/com.xxx.xxx/files - 应用内文件（注：对应方法getFilesDir())
         */
//        DataCleanManager.cleanInternalCache(context);
//        DataCleanManager.cleanDatabases(context);
//        DataCleanManager.cleanFiles(context);
//
//        //清除自定义路径下的文件
//        String fresco_mainFile = Utils.getFrescoMainCacheDir(context);
//        String fresco_small = Utils.getFrescoSmallImageCacheDir(context);
//        String videoCache = Utils.getVideoCacheDir(context);
//        DataCleanManager.cleanCustomCache(fresco_mainFile);
//        DataCleanManager.cleanCustomCache(fresco_small);
//        DataCleanManager.cleanCustomCache(videoCache);
    }
}
