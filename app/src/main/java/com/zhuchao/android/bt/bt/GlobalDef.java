package com.zhuchao.android.bt.bt;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.util.Log;

import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.SystemConfig;
import com.common.util.Util;
import com.zhuchao.android.bt.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;

public class GlobalDef {

    public static boolean mIfFavoriteNumber = false;

    public static long mLockClickTime = 0;
    public final static int KEY_LOCK_TIME = 1500;

    public static int mFavorite = 0;
    private final static String TAG = "GlobalDef.BT";

    public static boolean isLock() {
        if ((System.currentTimeMillis() - mLockClickTime) > KEY_LOCK_TIME) {
            return false;
        }
        return true;
    }

    public static void lock() {
        mLockClickTime = System.currentTimeMillis();
    }

    // public String findPairedMacByIndex(ArrayList<PhoneBook> info, int index){
    // if (index < info.size()){
    // return
    // }
    // }

    public final static int UI_TYPE_8702 = 1;
    public static int mUIType = -1;
    public static int mA2DPInside = -1;
    public static int mBTCellStatusBar = 0;

    public static void initParamter(Context c) {
        mContext = c;
        if (mUIType == -1) {
            mUIType = c.getResources().getInteger(R.integer.ui_type);
        }
        if (mA2DPInside == -1) {
            mA2DPInside = c.getResources().getInteger(R.integer.a2dp_inside);
            if (mA2DPInside == 1) {
                mA2DPInside = MachineConfig.getPropertyIntReadOnly(MachineConfig.KEY_BT_MUSIC_INSIDE);
            }
        }

        mBTCellStatusBar = SystemConfig.getIntProperty(c, SystemConfig.KEY_BT_CELL);

        if (MachineConfig.VALUE_SYSTEM_UI_KLD11_200.equals(ResourceUtil.mSystemUI)) {
            mFavorite = 1;
        } else {
            mFavorite = MachineConfig.getPropertyIntReadOnly(MachineConfig.KEY_BT_FAVORITE);
        }
    }

    public static boolean isA2DPSource() {
        int index = Util.getFileValue("/sys/class/ak/source/index");
        if (index == MyCmd.SOURCE_BT_MUSIC) {
            return true;
        }
        return false;
    }

    public static boolean mTouch3Switch = false;
    private static final String KEY_SWITCH = "switch";

    public static void getTouch3ConfigValue() {
        String value = MachineConfig.getPropertyOnce(MachineConfig.KEY_TOUCH3_IDENTIFY);
        // Log.d(TAG, "getTouch3ConfigValue: " + value);
        if (value != null && !value.isEmpty()) {
            JSONObject jobj;
            try {
                jobj = new JSONObject(value);
            } catch (JSONException e1) {
                e1.printStackTrace();
                return;
            }
            boolean enabled = false;
            try {
                enabled = jobj.getBoolean(KEY_SWITCH);
            } catch (JSONException e1) {
                enabled = false;
            }
            mTouch3Switch = enabled;
        }
    }

    public static String getFocusPackage() {

        String topPackageName = null;

        FileReader fr = null;
        try {
            fr = new FileReader("/sys/class/ak/mcu/public_audio_focus");
            BufferedReader reader = new BufferedReader(fr, 250);
            topPackageName = reader.readLine();
            reader.close();
            fr.close();
        } catch (Exception e) {
        }

        return topPackageName;

    }

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    public static boolean isAudioFocusGPS() {
        String focus = getFocusPackage();
        if (focus != null && mContext != null) {
            String gps = SystemConfig.getProperty(mContext, MachineConfig.KEY_GPS_PACKAGE);

            Log.d(TAG, focus + ":" + gps);
            if (focus.equals(gps)) {
                return true;
            }
        }
        return false;
    }

    private static int mSource;

    public static void setSource(int source) {
        mSource = source;
        if (source != MyCmd.SOURCE_BT_MUSIC && source != MyCmd.SOURCE_OTHERS_APPS) {
            abandonAudioFocus();
        }
    }

    public static int getSource() {
        return mSource;
    }

    private static boolean mPausedByTransientLossOfFocus;
    public static OnAudioFocusChangeListener mBtPhoneFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {

            Log.e(TAG, "OnAudioFocusChangeListener" + focusChange);

            switch (focusChange) {

                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (!isAudioFocusGPS()) {
                        if (GlobalDef.mSource == MyCmd.SOURCE_BT_MUSIC) {
                            //						ATBluetoothService
                            //								.musicKeyControl(MyCmd.Keycode.PLAY_PAUSE);
                            // if (mServiceBase.mPlayStatus ==
                            // BTMusicService.A2DP_INFO_PLAY) {
                            mPausedByTransientLossOfFocus = ATBluetoothService.musicKeyControl(MyCmd.Keycode.PAUSE);
                            // mServiceBase.doKeyControl(MyCmd.Keycode.PAUSE);
                            // }
                            BroadcastUtil.sendToCarServiceSetSource(mContext, MyCmd.SOURCE_OTHERS_APPS);
                        }
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = ATBluetoothService.musicKeyControl(MyCmd.Keycode.PLAY);
                        mPausedByTransientLossOfFocus = false;
                        BroadcastUtil.sendToCarServiceSetSource(mContext, MyCmd.SOURCE_BT_MUSIC);
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown audio focus change code");
            }
        }
    };
    ;

    public static void requestAudioFocus(Context context) {
        //		if (mAudioManager == null) {
        //			mAudioManager = (AudioManager) context
        //					.getSystemService(Context.AUDIO_SERVICE);
        //		}
        //
        //		Log.e(TAG, "requestAudioFocus:" + isAudioFocus);
        //		if (mAudioManager != null && !isAudioFocus) {
        //			isAudioFocus = true;
        //			mAudioManager.requestAudioFocus(mBtPhoneFocusListener,
        //					AudioManager.STREAM_MUSIC,
        //					AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //		}
    }

    private static AudioManager mAudioManager;
    private static boolean isAudioFocus = false;

    public static void abandonAudioFocus() {
        Log.e(TAG, "abandonAudioFocus");
        if (mAudioManager != null && mBtPhoneFocusListener != null && isAudioFocus) {
            mAudioManager.abandonAudioFocus(mBtPhoneFocusListener);
            isAudioFocus = false;

        }
    }

    public static boolean isTimeLate(String t1, String t2) {
        try {
            String ss[];
            String sss[];
            String ss1[];
            String ss2[];
            ss = t1.split("\r\n");
            sss = t2.split("\r\n");
            ss1 = ss[0].split("/");
            ss2 = sss[0].split("/");

            int y1 = Integer.valueOf(ss1[0]);
            int m1 = Integer.valueOf(ss1[1]);
            int d1 = Integer.valueOf(ss1[2]);

            int y2 = Integer.valueOf(ss2[0]);
            int m2 = Integer.valueOf(ss2[1]);
            int d2 = Integer.valueOf(ss2[2]);
            // int y1,m1,d1,h1,min1,s1;
            // int y2,m2,d2,h2,min2,s2;
            if (y1 > y2) {
                return true;
            } else if (y1 == y2) {
                if (m1 > m2) {
                    return true;
                } else {
                    if (m1 == m2) {
                        if (d1 > d2) {
                            return true;
                        } else if (d1 != d2) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }

            ss1 = ss[1].split(":");
            ss2 = sss[1].split(":");

            y1 = Integer.valueOf(ss1[0]);
            m1 = Integer.valueOf(ss1[1]);
            d1 = Integer.valueOf(ss1[2]);

            y2 = Integer.valueOf(ss2[0]);
            m2 = Integer.valueOf(ss2[1]);
            d2 = Integer.valueOf(ss2[2]);
            if (y1 > y2) {
                return true;
            } else if (y1 == y2) {
                if (m1 > m2) {
                    return true;
                } else {
                    if (m1 == m2) {
                        if (d1 > d2) {
                            return true;
                        } else if (d1 != d2) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } catch (Exception e) {

        }

        return false;
    }

}
