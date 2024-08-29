package com.zhuchao.android.bt.bt;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;


public class BR extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {

            //should power on the module first.
            //car service start this service


            Log.d("BroadcastReceiver", "bt ACTION_BOOT_COMPLETED");
            Intent it = new Intent(Intent.ACTION_RUN);
            it.setClass(context, ATBluetoothService.class);
            context.startService(it);

        }
    }

}
