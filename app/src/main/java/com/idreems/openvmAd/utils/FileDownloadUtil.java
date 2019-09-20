package com.idreems.openvmAd.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.file.FileUtil;
import com.idreems.openvmAd.network.DownloadListener;
import com.idreems.openvmAd.network.NetworkUtils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by ramonqlee on 8/6/16.
 */
public class FileDownloadUtil {
    private static final String TAG = "FileDownloadUtil";
    private static final int BUFFER_SIZE = 1024 * 4;
    private static List<String> sDownloadingTaskList = new ArrayList<String>();

    public static synchronized void startFileDownload(Context context, final String url, final DownloadListener downloadListener) {
        final String tempCacheDir = Utils.getTempCacheDir(context);
        final String tempCachedFileName = String.format("%s%s%s", tempCacheDir, File.separator, Utils.getFileNameByUrl(url));
        FileUtil.makesureDirExist(tempCacheDir);

        BaseDownloadTask task = FileDownloader.getImpl().create(url).setPath(tempCachedFileName);
        task.setListener(new FileDownloadListener() {
            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if (null == downloadListener) {
                    return;
                }
                downloadListener.progress(soFarBytes, totalBytes);
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                if (null == downloadListener) {
                    return;
                }
                downloadListener.completed(task.getPath());
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                if (null == downloadListener) {
                    return;
                }
                downloadListener.error(e);
            }

            @Override
            protected void warn(BaseDownloadTask task) {
            }
        });

        if (FileDownloadStatus.isIng(FileDownloader.getImpl().getStatus(task.getUrl(),task.getPath()))) {
            Log.d(TAG, "url's file is downloading " + url);
            return;
        }
        task.start();
    }

    public static synchronized void startFileDownloadOkHttp(Context context, final String url, final DownloadListener downloadListener) {

        final String tempCacheDir = Utils.getTempCacheDir(context);
        final String tempCachedFileName = String.format("%s%s%s", tempCacheDir, File.separator, Utils.getFileNameByUrl(url));
        FileUtil.makesureDirExist(tempCacheDir);

        // FIXME 后续考虑加入进度回调
        if (-1 != sDownloadingTaskList.indexOf(url)) {
            LogUtil.d(TAG, "in downloading");
            return;
        }
//        ToastUtils.show("开始下载文件 " + url);

        NetworkUtils.getAndResponseOnThread(url, new Callback() {

            private FileOutputStream getFileOutputStream(String path) throws IOException {
                if (TextUtils.isEmpty(path)) {
                    return null;
                }

                File file = new File(path);
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        throw new IOException(String.format("create new file error  %s", file.getAbsolutePath()));
                    }
                }

                return new FileOutputStream(file, false);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() && null != downloadListener) {
                    downloadListener.error(null);
                }
                sDownloadingTaskList.add(url);
                final long contentLength = response.body().contentLength();

                // 写入文件
                // fetching datum
                InputStream inputStream = null;
                FileOutputStream accessFile = getFileOutputStream(tempCachedFileName);
                long soFarBytes = 0;
                try {
                    if (null == accessFile) {
                        sDownloadingTaskList.remove(url);
                        if (null != downloadListener) {
                            downloadListener.error(null);
                        }
                        return;
                    }
                    // Step 1, get input stream
                    inputStream = response.body().byteStream();

                    byte[] buff = new byte[BUFFER_SIZE];

                    // enter fetching loop(Step 2->6)
                    do {
                        // Step 2, read from input stream.
                        int byteCount = inputStream.read(buff);
                        if (byteCount == -1) {
                            break;
                        }

                        // Step 3, writ to file
                        accessFile.write(buff, 0, byteCount);

                        // Step 4, adapter sofar
                        soFarBytes += byteCount;
                        if (null != downloadListener) {
                            downloadListener.progress(soFarBytes, contentLength);
                            Log.d(TAG, "progess sofarBytes = " + soFarBytes + " contentLength=" + contentLength);
                        }
                        // Step 5, check whether file is changed by others

                        // Step 6, check pause

                    } while (true);

                    sDownloadingTaskList.remove(url);
                    if (null != downloadListener) {
                        downloadListener.completed(tempCachedFileName);
                    }
                } catch (Exception ex) {
                    sDownloadingTaskList.remove(url);
                    if (null != downloadListener) {
                        downloadListener.error(ex);
                    }
                    ex.printStackTrace();
                }
                if (accessFile != null) {
                    accessFile.close();
                }
            }
        });
    }
}
