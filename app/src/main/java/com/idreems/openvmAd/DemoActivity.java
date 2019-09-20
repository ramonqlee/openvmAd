package com.idreems.openvmAd;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.idreems.openvmAd.utils.ToastUtils;


public class DemoActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        Button quitButton = (Button)findViewById(R.id.quit_button);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button payoutButton = (Button)findViewById(R.id.payout_button);
        payoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payoutClick();
            }
        });
    }

    public void payoutClick() {
        // 获取出币数，并出币
        EditText editText = (EditText) findViewById(R.id.edit_digit_input);
        String countStr = editText.getText().toString();
        if (TextUtils.isEmpty(countStr)) {
            ToastUtils.show("请输入纯数字");
            return;
        }

        if (!TextUtils.isDigitsOnly(countStr)) {
            ToastUtils.show("请输入纯数字");
            return;
        }

        try {
            int count = Integer.valueOf(countStr);
            payout(count);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void payout(int count) {
    }

}
