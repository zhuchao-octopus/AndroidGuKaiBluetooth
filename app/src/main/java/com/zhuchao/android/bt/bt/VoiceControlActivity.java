package com.zhuchao.android.bt.bt;

import android.app.Activity;
import android.os.Bundle;

public class VoiceControlActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ATBluetoothService.showSpeechVoiceActivity(false);
        finish();
    }

}
