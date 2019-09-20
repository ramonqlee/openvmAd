package com.idreems.openvmAd.utils;

import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;

/**
 * Created by lizy on 15-9-1.
 */
public class QRCodeUtil {
    //uri   Fresco 获取缓存的bitmap
    public static Bitmap fetchFrescoBitmap(Uri uri) {
        ImageRequest imageRequest = ImageRequest.fromUri(uri);
        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(imageRequest, null);
        BinaryResource resource = ImagePipelineFactory.getInstance()
                .getMainFileCache().getResource(cacheKey);
        if (null == resource) {
            return null;
        }
        File file = ((FileBinaryResource) resource).getFile();

        return Utils.decodeSampledBitmapFromFile(file.getPath(), 4);
    }
}
