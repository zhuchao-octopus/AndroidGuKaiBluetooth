package com.zhuchao.android.bt.bt;

import android.app.Application;
import android.content.Intent;

public class ATBluetoothApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, ATBluetoothService.class));
    }
}
