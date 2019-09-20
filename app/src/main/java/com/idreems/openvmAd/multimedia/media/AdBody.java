package com.idreems.openvmAd.multimedia.media;

import android.text.TextUtils;

import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.FSMediaPlayerView.FSMedia.IFSMedia;
import com.idreems.openvmAd.utils.JsonUtils;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 7/27/16.
 */
public class AdBody {

    /**
     * id: "11",
     * name: "全国广告2",
     * starttime: "1469462400",
     * endtime: "1469894399",
     * ad_type: "1",
     * ad_link: "",//可以是乐视云的视频id；可以是url；也可以是相对当前服务器的相对路径
     * ad_time: "30",
     * banner: "",
     * advertiser: "粉丝互动",
     * area: "0"
     */
    public String name;
    public long startTimeInSec;
    public long endTimeInSec;
    public int type;//1图片  2广告视频 3gif 4栏目 5直播
    public String url;
    public long stayTimeInMs;

    public IFSMedia toIFSMedia() {
        IFSMedia media = new IFSMedia();

        media.name = name;
        media.startTimeInSec = startTimeInSec;
        media.endTimeInSec = endTimeInSec;
        media.url = url;
        media.type = type;
        media.stayTimeInMs = stayTimeInMs;

        return media;
    }

    public AdBody(JSONObject object) {
        if (null == object) {
            return;
        }
        /**
         * "name":"IMG_1397.jpg",
         "start_time":1552492800,
         "end_time":1569859200,
         "type":2,
         "url":"http://fsimg.fensihudong.com/niuqu/IMG_1397.jpg",
         "stay_time":30000
         */
        name = JsonUtils.getString(object, "name");
        // 解析广告类型
        type = JsonUtils.getInt(object, "type");
        startTimeInSec = JsonUtils.getLong(object, "start_time");
        endTimeInSec = JsonUtils.getLong(object, "end_time");
        url = JsonUtils.getString(object, "url");
        stayTimeInMs = JsonUtils.getLong(object, "stay_time");
    }

    public AdBody() {
    }

    public boolean isImage() {
        return Consts.AD_IMAGE_TYPE == type;
    }

    public boolean equals(AdBody body) {
        if (null == body) {
            return false;
        }
        return TextUtils.equals(this.url, body.url);
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        return TextUtils.equals(this.url, ((AdBody) obj).url);
    }
}
