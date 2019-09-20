package com.idreems.openvmAd.PushHandler;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvmAd.Push.PushObserver;
import com.idreems.openvmAd.persistence.Config;
import com.idreems.openvmAd.utils.DeviceUtils;
import com.idreems.openvmAd.utils.JsonUtils;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class RebootHandler implements PushObserver {
    private static final String TAG = RebootHandler.class.getSimpleName();

    private static final String NODE_ID = "node_id";
    private static final String HANDLER_TYPE = "reboot";
    private static final String HANDLER_TYPE_ALL = "rebootAll";

    private Context mContext;

    public RebootHandler(Context context) {
        mContext = context;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = JsonUtils.getString(jsonObject, HANDLER_TYPE_ALL);
            if (!TextUtils.isEmpty(type)) {
                DeviceUtils.reboot();
                return true;
            }

            type = JsonUtils.getString(jsonObject, HANDLER_TYPE);
            String pushNodeId = JsonUtils.getString(jsonObject, NODE_ID);
            Config config = Config.sharedInstance(mContext);
            final String localNodeId = config.getValue(Config.NODE_ID);
            Log.d(TAG, "nodeid = " + pushNodeId + " localNodeId=" + localNodeId + " pushType=" + type);
            if (TextUtils.isEmpty(localNodeId) || TextUtils.isEmpty(pushNodeId)) {
                return false;
            }

            if (!TextUtils.equals(localNodeId, pushNodeId)) {
                return false;
            }

            if (TextUtils.isEmpty(type)) {
                return false;
            }

            DeviceUtils.reboot();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
