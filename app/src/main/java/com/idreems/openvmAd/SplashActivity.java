package com.idreems.openvmAd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.idreems.openvmAd.constant.Consts;

public class SplashActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        delayEnterMainActivity();
        // FIXME 测试，发布时关闭
        if (!Consts.PRODUCTION_ON) {
            testCase();
        }
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    private void delayEnterMainActivity() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                startActivity(intent);

                finish();
            }
        }, 5000);
    }

    private void testCase() {
    }

    private void testCrash() {
        String test ="";
        if (TextUtils.isEmpty(test))
        {
            test = null;
        }
        test.toString();
    }
}
