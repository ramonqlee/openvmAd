package com.idreems.openvmAd;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.idreems.openvmAd.Push.PushDispatcher;
import com.idreems.openvmAd.PushHandler.SnapshotHandler;
import com.idreems.openvmAd.constant.Consts;
import com.idreems.openvmAd.multimedia.MainAdLinearLayout;
import com.idreems.openvmAd.multimedia.media.AdBody;
import com.idreems.openvmAd.utils.ReusableTimer;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import static com.igexin.push.core.g.R;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private ReusableTimer mTimer;
    private MainAdLinearLayout mPlayFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayFragment = (MainAdLinearLayout) findViewById(R.id.ad_main);
        View serverModeView = findViewById(R.id.server_mode_textview);
        serverModeView.setVisibility(Consts.PRODUCTION_ON ? View.GONE : View.VISIBLE);

        PushDispatcher.sharedInstance().addObserver(new SnapshotHandler(this));

        startAdRequest();
        // 3. 定时开关机功能,定时获取开关机的设置
        startTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        mPlayFragment.releaseAdRequest();
        stopTimer();
        super.onDestroy();
    }

    private void startAdRequest() {
        mPlayFragment.startAdRequest();
    }

    private void startTimer() {
        stopTimer();
        mTimer = new ReusableTimer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 同步定时开关机策略
                // 1. 定时开机
                // 2. 定时关机
            }
        }, 1 * Consts.ONE_MINUTE_IN_MS, 10 * Consts.ONE_MINUTE_IN_MS);
    }

    private void stopTimer() {
        if (null != mTimer) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private static List<AdBody> parseAdList(JSONArray data) {
        if (null == data) {
            return new ArrayList<>();
        }
        List<AdBody> ret = new ArrayList<>();
        try {
            for (int i = 0; i < data.length(); ++i) {
                ret.add(new AdBody(data.getJSONObject(i)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ret;
    }

}
