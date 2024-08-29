package com.zhuchao.android.bt.bt;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.common.util.AppConfig;
import com.common.util.MyCmd;
import com.common.util.Util;
import com.rockchip.car.recorder.utils.SystemProperties;

import java.util.List;

public class BroadcastUtil {

    private final static String TAG = "BT_BroadcastUtil";

    public final static void sendCanboxInfo(Context context, String name, int value1, int value2, int value3) {
        Intent i = new Intent(name);
        i.putExtra("value1", value1);
        i.putExtra("value2", value2);
        i.putExtra("value3", value3);
        sendToCarService(context, i);
    }

    public final static void sendCanboxInfo(Context context, byte[] buf) {
        Intent i = new Intent(MyCmd.BROADCAST_SEND_TO_CAN_FROM_BT);
        i.putExtra("buf", buf);
        sendToCarService(context, i);
    }

    public final static void sendToCarServiceSetSource(Context context, int source) {
        sendToCarService(context, MyCmd.Cmd.SET_SOURCE, source);
    }

    public final static void sendToCarService(Context context, Intent it) {
        it.setPackage(AppConfig.PACKAGE_CAR_SERVICE);
        context.sendBroadcast(it);
    }

    private static final String[] CARPLAY_APK = {
            "com.suding.speedplay", "com.zjinnova.zlink"
    };

    private static List<ResolveInfo> apps;

    public static void initCarPlaySettings(Context context) {
        // init carplay id
        int carplay_uid = 0;
        if (apps == null) {
            PackageManager mPackageManager = context.getPackageManager();
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);

            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            apps = mPackageManager.queryIntentActivities(mainIntent, 0);
        }
        for (ResolveInfo appInfo : apps) {
            for (String packageName : CARPLAY_APK) {
                if (packageName.equals(appInfo.activityInfo.packageName)) {
                    carplay_uid = appInfo.activityInfo.applicationInfo.uid;
                    Log.d(TAG, "initGpsSettings carplay uid" + packageName + "" + carplay_uid);
                    SystemProperties.set("ak.af.carplay.uid", "" + carplay_uid);
                    SystemProperties.set("ak.af.carplay.package", "" + packageName);
                    break;
                }
            }
        }
    }

    public final static void sendToCarService(Context context, int cmd, int data) {

        if (MyCmd.Cmd.BT_PHONE_STATUS == cmd) {
            if (data == MyCmd.PhoneStatus.PHONE_ON) {
                // requestAudioFocus(context);
                Util.setProperty("ak.af.btphone.on", "1");
            } else if (data == MyCmd.PhoneStatus.PHONE_OFF) {
                // abandonAudioFocus();
                Util.setProperty("ak.af.btphone.on", "0");
            } else if (data == MyCmd.PhoneStatus.PHONE_CARPLAY_ON) {
                Util.setProperty("ak.af.carplayphone.on", "1");
                String id = Util.getProperty("ak.af.carplay.uid");
                if (id == null || id.length() == 0) {
                    initCarPlaySettings(context);
                }
            } else if (data == MyCmd.PhoneStatus.PHONE_CARPLAY_OFF) {
                Util.setProperty("ak.af.carplayphone.on", "0");

                Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
                it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_CARPLAY_STATUS);
                it.putExtra(MyCmd.EXTRA_COMMON_DATA, 0);

                context.sendBroadcast(it);

            }

            Log.d("kkdf", Util.getProperty("ak.af.carplay.uid") + "::" + data + ":" + Util.getProperty("ak.af.carplayphone.on"));
        }

        Intent it;

        it = new Intent(MyCmd.BROADCAST_CMD_TO_CAR_SERVICE_BT);

        it.putExtra(MyCmd.EXTRA_COMMON_CMD, cmd);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, data);
        it.setPackage(AppConfig.PACKAGE_CAR_SERVICE);
        sendToCarService(context, it);
    }

    public final static void sendToCarService(Context context, int cmd, int data, int data2) {
        Intent it;

        it = new Intent(MyCmd.BROADCAST_CMD_TO_CAR_SERVICE_BT);

        it.putExtra(MyCmd.EXTRA_COMMON_CMD, cmd);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, data);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA2, data2);
        it.setPackage(AppConfig.PACKAGE_CAR_SERVICE);
        sendToCarService(context, it);
    }

    public final static void sendToCarService(Context context, int cmd, int data, int data2, int data3) {
        Intent it;

        it = new Intent(MyCmd.BROADCAST_CMD_TO_CAR_SERVICE_BT);

        it.putExtra(MyCmd.EXTRA_COMMON_CMD, cmd);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, data);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA2, data2);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA3, data3);
        it.setPackage(AppConfig.PACKAGE_CAR_SERVICE);
        sendToCarService(context, it);
    }
}
