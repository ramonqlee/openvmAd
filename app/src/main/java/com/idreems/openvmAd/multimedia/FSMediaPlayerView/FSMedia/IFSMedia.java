package com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia;

import com.idreems.openvmAd.constant.Consts;

import java.io.Serializable;

import static com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia.MediaType.IMAGE;
import static com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia.MediaType.VIDEO;

/**
 * Created by ramonqlee on 5/19/16.
 * 轮播媒体
 */
public class IFSMedia implements Serializable {
    public static final long INFINITE_END_TIME_IN_SEC = 0;// 结束时间0代表一直播放
    public String name;
    public long startTimeInSec;//上线时间
    public long endTimeInSec;//下线时间
    public int type;//1图片  2广告视频 3gif 4栏目 5直播
    public String url;
    public long stayTimeInMs;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public long getStartTimeInSec() {
        return 0;
    }

    public long getEndTimeInSec() {
        return stayTimeInMs/1000;
    }

    public boolean isImage() {
        return Consts.AD_IMAGE_TYPE == type;
    }

    public String getRequestId() {
        return url;
    }

    public MediaType getMediaType() {
        return isImage() ? IMAGE : VIDEO;
    }

    public String getPlatform() {
        return "NQ";
    }

    public boolean isSelfMedia() {
        return true;//目前仅支持自有广告
    }

    public String getAd_position() {
        return Consts.POSITIONADHOME;//目前只有一个广告
    }

    @Override
    public String toString() {
        return "IFSMedia{" +
                "name='" + name + '\'' +
                ", startTimeInSec=" + startTimeInSec +
                ", endTimeInSec=" + endTimeInSec +
                ", type=" + type +
                ", url='" + url + '\'' +
                ", stayTimeInMs=" + stayTimeInMs +
                '}';
    }

    public enum MediaType {
        VIDEO, IMAGE, GIF
    }
}
