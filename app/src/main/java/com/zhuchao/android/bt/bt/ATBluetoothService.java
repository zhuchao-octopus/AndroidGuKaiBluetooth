package com.zhuchao.android.bt.bt;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Presentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.common.util.AppConfig;
import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.SystemConfig;
import com.common.util.Util;
import com.common.util.UtilCarKey;
import com.rockchip.car.recorder.utils.SystemProperties;
import com.zhuchao.android.bt.R;

import com.zhuchao.android.bt.hw.ATBluetooth;
import com.zhuchao.android.bt.hw.ATBluetooth.TObject;
import com.zhuchao.android.bt.hw.Parrot;
import com.zhuchao.android.bt.view.VoicRecognitionView;
import com.zhuchao.android.fbase.MMLog;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ATBluetoothService extends Service {
    public static final String TAG = "ATBluetoothService";
    private static final boolean DEBUG = true;
    public static ArrayList<PhoneBook> mPhoneBookInfo = new ArrayList<PhoneBook>();
    public static ArrayList<PhoneBook> mPhoneBookSimInfo = new ArrayList<PhoneBook>();
    public static ArrayList<PhoneBook> mSearchInfo = new ArrayList<PhoneBook>();
    public static ArrayList<PhoneBook> mPairInfo = new ArrayList<PhoneBook>();

    private final static boolean IVT_KILL_BT_IF_SLEEP = false;
    private ATBluetooth mATBluetooth = null;
    @SuppressLint("StaticFieldLeak")
    public static ContactsUtils mContactsUtils = null;
    private long mIVTCrashTS0Time = 0;
    private int mIncomingTag = 0;

    private final static String ZLINK_BROAST = "com.zjinnova.zlink";
    public static ArrayList<PhoneBook> mCalllogMInfo = new ArrayList<PhoneBook>();
    public static ArrayList<PhoneBook> mCalllogRInfo = new ArrayList<PhoneBook>();
    public static ArrayList<PhoneBook> mCalllogOInfo = new ArrayList<PhoneBook>();
    private final static int MSG_NOTIFY_ICON = ATBluetooth.RETURN_MSG_MAX + 1;
    private final static int MSG_REBOOT_BT_CORE = ATBluetooth.RETURN_MSG_MAX + 2;
    private final static int MSG_SET_CONNECT_MODE = ATBluetooth.RETURN_MSG_MAX + 3;
    private final static int MSG_MEIDA_PLAY_DELAY = ATBluetooth.RETURN_MSG_MAX + 4;
    private final static int MSG_MEIDA_CHECK_PLAY = ATBluetooth.RETURN_MSG_MAX + 5;
    private final static int MSG_CHECK_LANGUAGE = ATBluetooth.RETURN_MSG_MAX + 6;

    private final static int MSG_REBOOT_BT_CORE_WITHOUTBTINSIDE = ATBluetooth.RETURN_MSG_MAX + 8;

    private final static int MSG_PARROT_CHECK_LONG_DISCONNECT = ATBluetooth.RETURN_MSG_MAX + 7;

    private final static int MSG_CHECK_HARDWARE_STATUS = ATBluetooth.RETURN_MSG_MAX + 10;
    private final static int MSG_INIT_CMD = ATBluetooth.RETURN_MSG_MAX + 11;
    private final static int MSG_CHECK_CONNECT_STATUS_ACC_POWER_ON = ATBluetooth.RETURN_MSG_MAX + 12;
    private final static int MSG_STOP_IVT_BLUELETD = ATBluetooth.RETURN_MSG_MAX + 13;
    private final static int MSG_START_IVT_BLUELETD = ATBluetooth.RETURN_MSG_MAX + 14;
    private final static int MSG_CHECK_HARDWARE_CRASH = ATBluetooth.RETURN_MSG_MAX + 15;
    private final static int MSG_CHECK_CALLLOG_SYNC = ATBluetooth.RETURN_MSG_MAX + 16;


    @Override
    public void onCreate() {
        MMLog.d(TAG, TAG + " onCreate!");

        ResourceUtil.updateAppUi(this);
        GlobalDef.initParamter(this);

        super.onCreate();
        mContext = this;

        getDefaultNameConfig();
        mBTCoreCrash = false;
        initHardware();
        mIncoming = new IncomingView();
        registerListener();
        SaveData.createDataBase(this);
        initSpeech();
        if (MachineConfig.getPropertyIntReadOnly(MachineConfig.KEY_BT_SUPPORT_MACTALK) == 1) {
            mContactsUtils = ContactsUtils.getInstanse(this);
        }
        setCustomDefConfig();
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            mOBDMac = getDataString(SAVE_GOC_OBD_MAC);
        }
        GlobalDef.getTouch3ConfigValue();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        mIncoming.hide();
        mIncoming = null;

        if (mATBluetooth != null) {
            mATBluetooth.removeHandler(TAG);
            mATBluetooth.destroy();
            mATBluetooth = null;
        }
        unregisterListener();
        super.onDestroy();
    }


    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            try {
                if (DEBUG) {
                    Log.d(TAG, "handleMessage:" + msg.what);
                }
                switch (msg.what) {
                    case ATBluetooth.RETURN_MIC_GAIN:
                        returnMicGain(msg.arg1);
                        break;
                    case ATBluetooth.RETURN_CMD_PWNG2_DISCONNECT:
                        Log.d(TAG, "RETURN_CMD_PWNG2_DISCONNECT:" + msg.arg1);
                        mHandler.removeMessages(MSG_PARROT_CHECK_LONG_DISCONNECT);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PARROT_CHECK_LONG_DISCONNECT, msg.arg1, 0), 2000);
                        break;
                    case MSG_PARROT_CHECK_LONG_DISCONNECT:

                        Log.d(TAG, ATBluetooth.mCurrentHFP + "MSG_PARROT_CHECK_LONG_DISCONNECT:" + msg.what);
                        if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED && !ATBluetoothActivity.mSearch) {
                            mHandler.removeMessages(MSG_PARROT_CHECK_LONG_DISCONNECT);
                            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PARROT_CHECK_LONG_DISCONNECT, msg.arg1, 0), 2000);
                            mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_INDEX, msg.arg1 + "");
                        }
                        break;
                    case MSG_START_IVT_BLUELETD:
                        if (IVT_KILL_BT_IF_SLEEP) {
                            Util.setProperty("ctl.start", "ivt_blueletd");
                        } else {

                            mATBluetooth.closeIvtModule(false);
                            mATBluetooth.write(ATBluetooth.START_MODULE);
                        }
                        break;
                    case MSG_STOP_IVT_BLUELETD:
                        if (IVT_KILL_BT_IF_SLEEP) {
                            Util.setProperty("ctl.stop", "ivt_blueletd");
                        } else {
                            mATBluetooth.write(ATBluetooth.STOP_MODULE);

                            mATBluetooth.closeIvtModule(true);
                        }
                        break;
                    case MSG_CHECK_CONNECT_STATUS_ACC_POWER_ON:
                        checkConnectAccPowerOn();
                        break;
                    case MSG_INIT_CMD:
                        initCmd();
                        break;
                    case MSG_CHECK_HARDWARE_STATUS:
                    case ATBluetooth.RETURN_BT_IVT_CRASH:
                        initHardware();
                        break;
                    case MSG_CHECK_HARDWARE_CRASH:
                        crashReset(false);
                        Log.d(TAG, "MSG_CHECK_HARDWARE_CRASH");
                        break;
                    case MSG_NOTIFY_ICON:
                        notifyManager(ATBluetooth.mCurrentHFP);
                        break;
                    case MSG_REBOOT_BT_CORE:
                        //					UtilSystem.killProcess("com.my.bt");
                        Log.d(TAG, "kill bt");
                        Util.sudoExec("kill:" + android.os.Process.myPid());
                        break;
                    case MSG_REBOOT_BT_CORE_WITHOUTBTINSIDE:
                        Log.d("fkkk", ATBluetoothActivity.isA2DPshow() + ":22123:" + GlobalDef.isA2DPSource() + ":");
                        Util.sudoExec("kill:" + android.os.Process.myPid());
                        break;
                    case MSG_SET_CONNECT_MODE:
                        if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTING && !ATBluetoothActivity.mSearch) {
                            mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE);
                        }
                        //	mHandler.sendEmptyMessageDelayed(MSG_SET_CONNECT_MODE, 2500);
                        break;
                    case MSG_MEIDA_PLAY_DELAY:
                        mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PLAY);
                        mHandler.sendEmptyMessageDelayed(MSG_MEIDA_CHECK_PLAY, 300);
                        Log.e(TAG, "MSG_MEIDA_PLAY_DELAY & start check" + mCheckTime);
                        break;
                    case MSG_MEIDA_CHECK_PLAY:
                        checkParrotPlayStatus();
                        break;
                    case MSG_CHECK_LANGUAGE:
                        checkParrotLanguage();
                        break;
                    case 1:
                        mIncoming.hide();
                        break;
                    case ATBluetooth.RETURN_DEVICE_INDEX: {
                        String obj2 = (String) ((ATBluetooth.TObject) msg.obj).obj2;

                        if (obj2 != null && ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {

                            mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_CONNECTED_MAC:
                        Log.d(TAG, "RETURN_CONNECTED_MAC:" + msg.arg1);
                        if (msg.arg1 == ATBluetooth.HFP_INFO_CONNECTED) {
                            String obj2 = (String) ((TObject) msg.obj).obj2;
                            if (obj2 != null && !obj2.equals(ATBluetooth.mCurrentMac)) {
                                initCurrenAndPhoneBook(obj2);
                            }
                        }
                        break;
                    case ATBluetooth.RETURN_HFP_INFO: {

                        Log.d(TAG, "RETURN_HFP_INFO:" + msg.arg1);
                        if (msg.arg1 == ATBluetooth.HFP_INFO_CONNECTED) {
                            String obj2 = (String) ((TObject) msg.obj).obj2;
                            if (obj2 != null && !obj2.equals(ATBluetooth.mCurrentMac)) {
                                initCurrenAndPhoneBook(obj2);

                            }
                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {

                                if (mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED) {
                                    mATBluetooth.write(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS);
                                }
                                if (ATBluetooth.mCurrentMac == null) {
                                    mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                                }

                                mATBluetooth.write(ATBluetooth.REQUEST_CMD_PPNO);

                            }
                        } else if (msg.arg1 < ATBluetooth.HFP_INFO_CONNECTED) {
                            ATBluetooth.mCurrentMac = null;
                            mPlayStatus = 0;
                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                sendA2DPPlayInfo();
                                mTimeAddParrotCallOutPhone = 0;
                                mPreParrotCallName = null;
                            } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                                sendA2DPPlayInfo();
                            }
                            if (mVoicRecognitionView != null) {
                                mVoicRecognitionView.hideMicButton();
                            }

                        }

                        if (ATBluetooth.mCurrentHFP != msg.arg1) {
                            if ((ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) && msg.arg1 == ATBluetooth.HFP_INFO_CONNECTED) {
                                if (ATBluetooth.mBTType != MachineConfig.VAULE_BT_TYPE_GOC) {
                                    mATBluetooth.write(ATBluetooth.GET_HFP_INFO);
                                }
                            }
                            if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CONNECTED) {
                                ATBluetooth.mCurrentHFP = msg.arg1;
                                Util.setProperty("bt_hfp", msg.arg1 + "");
                            }
                            if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INITIAL) {
                                mPhoneBookInfo.clear();
                                mPhoneBookSimInfo.clear();
                                mCalllogMInfo.clear();
                                mCalllogRInfo.clear();
                                mCalllogOInfo.clear();
                            }
                            onHFPChanged();
                        }

                        if (msg.arg1 > ATBluetooth.HFP_INFO_CONNECTED) {
                            showPhoneUI();
                            BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_ON);
                        }

                        notifyManager(ATBluetooth.mCurrentHFP);
                        Log.d(TAG, "mCurrentHFP:" + msg.arg1);
                        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CONNECTED) {
                            mFirstConnectCheckPlay = SystemClock.uptimeMillis();
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_HFP: {
                        if (DEBUG) Log.d(TAG, "RETURN_HFP" + "(" + msg.arg1 + ")");

                        if (ATBluetooth.mCurrentHFP != msg.arg1) {
                            ATBluetooth.mCurrentHFP = msg.arg1;
                            if (ATBluetooth.mCurrentHFP != 2) {
                                // mCall = 0;
                                mHandler.sendEmptyMessage(0);
                            }
                            onHFPChanged();
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_VOICE_SWITCH: {
                        mIncoming.updateVoice();

                        break;
                    }
                    case ATBluetooth.RETURN_CALL_OUT: {

                        if (ATBluetooth.isSupport3Call()) {
                            if (mCallNumber != null) {
                                return;
                            }
                        }
                        if (mVoicRecognitionView != null) {
                            mVoicRecognitionView.hideSpeech();
                        }

                        ATBluetooth.mCurrentHFP = ATBluetooth.HFP_INFO_CALLED;
                        mCallNumber = (String) ((TObject) msg.obj).obj2;
                        if ((((TObject) msg.obj).obj3) != null) {
                            mCallName = (String) ((TObject) msg.obj).obj3;
                        }
                        showPhoneUI();
                        String time = getTime();
                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                            Log.d("fkk", mPreParrotCallName + ":" + mCallName + "--mTimeAddParrotCallOutPhone:" + mTimeAddParrotCallOutPhone);
                            if (mTimeAddParrotCallOutPhone == 0) {
                                mPreParrotCallName = mCallName;
                                mCalllogOInfo.add(0, new PhoneBook(mCallNumber, mCallName, time));
                            } else if (mPreParrotCallName == null && mCallName != null) {
                                if (mCalllogOInfo.size() > 0) {
                                    PhoneBook pb = mCalllogOInfo.get(0);
                                    if (pb != null) {
                                        pb.mName = mCallName;
                                    }
                                }
                            }
                            mTimeAddParrotCallOutPhone = 1;
                        } else {
                            mCalllogOInfo.add(0, new PhoneBook(mCallNumber, mCallName, time));
                            SaveData.saveDataEx(ATBluetooth.mCurrentMac, mCallName, mCallNumber + "," + time, SaveData.TAG_LOG_OUT);
                            sendCallLogToCan();
                        }

                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_ON);
                        onHFPChanged();
                    }
                    break;
                    case ATBluetooth.RETURN_CALL_INCOMING:
                        if (ATBluetooth.isSupport3Call()) {
                            if (mCallNumber != null) {
                                return;
                            }
                        }
                        ATBluetooth.mCurrentHFP = ATBluetooth.HFP_INFO_INCOMING;
                        mCallNumber = (String) ((TObject) msg.obj).obj2;
                        if ((((TObject) msg.obj).obj3) != null) {
                            mCallName = (String) ((TObject) msg.obj).obj3;
                        }
                        showPhoneUI();
                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_ON);
                        onHFPChanged();
                        break;
                    case ATBluetooth.RETURN_CALL_END:
                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                            mTimeAddParrotCallOutPhone = 0;
                            mPreParrotCallName = null;
                            if (mCalllogOInfo.size() > 0) {
                                PhoneBook pb = mCalllogOInfo.get(0);
                                if (pb != null) {
                                    SaveData.saveDataEx(ATBluetooth.mCurrentMac, pb.mName, pb.mNumber + "," + pb.mPinyin, SaveData.TAG_LOG_OUT);
                                    sendCallLogToCan();
                                }
                            }
                        }

                        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
                            String time = getTime();
                            // mCallNumber = (String) ((TObject) msg.obj).obj2;
                            mCalllogMInfo.add(0, new PhoneBook(mCallNumber, mCallName, time));

                            SaveData.saveDataEx(ATBluetooth.mCurrentMac, mCallName, mCallNumber + "," + time, SaveData.TAG_LOG_MISS);
                            sendCallLogToCan();
                        }

                        ATBluetooth.mCurrentHFP = ATBluetooth.HFP_INFO_CONNECTED;
                        showPhoneUI();
                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_OFF);
                        onHFPChanged();

                        ATBluetooth.mCallingNum = 0;
                        ATBluetooth.m3CallNumber = null;
                        ATBluetooth.m3CallStatus = 0;
                        break;
                    case ATBluetooth.RETURN_CALLLOG_END:
                        autoDownloadCallLog();
                        break;
                    case ATBluetooth.RETURN_CALLLOG_DATA:
                        updateCallLog((String) ((TObject) msg.obj).obj2);
                        break;
                    case ATBluetooth.RETURN_PHONE_BOOK_START:
                        if (ATBluetooth.mAutoDownLoadCallLog) {
                            if (mDownCallLogType >= DOWN_CALLLOG_ANSWER && mDownCallLogType <= DOWN_CALLLOG_MISS) {
                                return;
                            }
                        }
                        break;
                    case MSG_CHECK_CALLLOG_SYNC:
                        stopCallLogSync();
                        break;
                    case ATBluetooth.RETURN_CALLING:

                        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {

                            mCallNumber = (String) ((TObject) msg.obj).obj2;
                            if ((((TObject) msg.obj).obj3) != null) {
                                mCallName = (String) ((TObject) msg.obj).obj3;
                            }
                            String time = getTime();
                            mCalllogRInfo.add(0, new PhoneBook(mCallNumber, mCallName, time));

                            SaveData.saveDataEx(ATBluetooth.mCurrentMac, mCallName, mCallNumber + "," + time, SaveData.TAG_LOG_RECEIVE);
                            sendCallLogToCan();
                        } else if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
                            if (mCallNumber == null || mCallNumber.length() == 0) {
                                mCallNumber = (String) ((TObject) msg.obj).obj2;
                                if ((((TObject) msg.obj).obj3) != null) {
                                    mCallName = (String) ((TObject) msg.obj).obj3;
                                }
                            }
                        }
                        if (m3CallChangeName) {
                            if (((TObject) msg.obj).obj2 != null) {
                                mCallNumber = (String) ((TObject) msg.obj).obj2;
                                if ((((TObject) msg.obj).obj3) != null) {
                                    mCallName = (String) ((TObject) msg.obj).obj3;
                                }
                                m3CallChangeName = false;
                            }
                        }

                        if (ATBluetooth.mCurrentHFP != ATBluetooth.HFP_INFO_INCOMING && ATBluetooth.mCurrentHFP != ATBluetooth.HFP_INFO_CALLED) {
                            BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_ON);
                        }
                        ATBluetooth.mCurrentHFP = ATBluetooth.HFP_INFO_CALLING;

                        showPhoneUI();

                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_MUTE_ONE_TIME);
                        onHFPChanged();
                        break;
                    case ATBluetooth.RETURN_GOC_BT_MODULE_MAC:
                        mATBluetooth.mBtModuleMac = (String) ((TObject) msg.obj).obj2;
                        break;
                    case ATBluetooth.RETURN_PIN:
                        mATBluetooth.mBtPin = (String) ((TObject) msg.obj).obj2;
                        resetDefaultPasswd(mATBluetooth.mBtPin);
                        break;
                    case ATBluetooth.RETURN_NAME:
                        mATBluetooth.mBtName = (String) ((TObject) msg.obj).obj2;
                        resetDefaultName(mATBluetooth.mBtName);
                        break;
                    case ATBluetooth.RETURN_A2DP_ON: {

                        mPlayStatus = ATBluetooth.A2DP_INFO_PLAY;

                        sendA2DPPlayInfo();
                    }
                    break;
                    case ATBluetooth.RETURN_A2DP_OFF: {
                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                            int status = mPlayStatus;
                            if (ATBluetooth.mCurrentHFP < ATBluetooth.A2DP_INFO_CONNECTED) {
                                status = 0;
                            } else {
                                if (msg.arg1 == 4) {
                                    if (mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED) {
                                        status = ATBluetooth.A2DP_INFO_CONNECTED;
                                    }
                                } else {
                                    if (mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED) {
                                        status = ATBluetooth.A2DP_INFO_CONNECTED;
                                    } else {
                                        status = ATBluetooth.A2DP_INFO_PAUSED;
                                    }
                                }
                            }
                            if (status == 0) {
                                mID3Name = "";
                                mID3Artist = "";
                                mID3Album = "";
                                mTotalTime = 0;
                            }
                            if (status != mPlayStatus) {
                                mPlayStatus = status;
                                sendA2DPPlayInfo();
                            }
                            break;
                        } else {
                            mPlayStatus = ATBluetooth.A2DP_INFO_PAUSED;
                        }

                        sendA2DPPlayInfo();
                    }
                    break;
                    case ATBluetooth.RETURN_A2DP_NEED_CONECT: {
                        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED && mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED) {
                            mATBluetooth.write(ATBluetooth.REQUEST_BT_MUSIC_CPCC);
                            mATBluetooth.write(ATBluetooth.REQUEST_CLMS);
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_CLMS: {
                        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED && mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED) {
                            mATBluetooth.write(ATBluetooth.REQUEST_BT_MUSIC_CPCC);
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_A2DP_CONNECT_STATUS: {

                        if (msg.arg1 < ATBluetooth.A2DP_INFO_CONNECTED) {
                            mID3Name = "";
                            mID3Artist = "";
                            mID3Album = "";
                        } else {
                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                if (mPlayStatus != msg.arg1) {
                                    if (mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED && msg.arg1 == ATBluetooth.A2DP_INFO_CONNECTED) {
                                        mATBluetooth.write(ATBluetooth.REQUEST_CLMS);
                                        if ("com.car.ui/com.my.btmusic.BTMusicActivity".equals(AppConfig.getTopActivity())) {
                                            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PLAY);
                                        }
                                    }
                                }
                            }
                        }
                        if (GlobalDef.mA2DPInside == 1) {
                            updateA2dpInsideStatus(msg.arg1);
                        }
                        mPlayStatus = msg.arg1;

                        Log.d("tt", (SystemClock.uptimeMillis() - mFirstConnectCheckPlay) + "bt:" + mPlayStatus);
                        if (msg.arg1 == ATBluetooth.A2DP_NEED_CONNECT && ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
                            mATBluetooth.write(ATBluetooth.REQUEST_BT_MUSIC_CPCC);
                            mATBluetooth.write(ATBluetooth.REQUEST_CLMS);
                        }
                        sendA2DPInfo();

                        //check
                        if (mFirstConnectCheckPlay != 0 && (SystemClock.uptimeMillis() - mFirstConnectCheckPlay) < 10000) {
                            if (mPlayStatus == ATBluetooth.A2DP_INFO_PLAY) {
                                Log.d("tt", "check bt play source:" + GlobalDef.isA2DPSource());
                                if (!GlobalDef.isA2DPSource()) {
                                    doMusicPause();
                                }
                                mFirstConnectCheckPlay = 0;
                            }
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_BT_CORE_ERROR:

                        Log.d(TAG, "ATBluetooth.RETURN_BT_CORE_ERROR!!" + msg.arg1 + mATBluetooth.getCloseIvtModeule());
                        if (msg.arg1 == 0) { // ts0
                            mIVTCrashTS0Time = SystemClock.uptimeMillis();
                        }
                        if (msg.arg1 == 2 && !mATBluetooth.getCloseIvtModeule() && (mIVTCrashTS0Time == 0 || ((SystemClock.uptimeMillis() - mIVTCrashTS0Time) > 8000))) {
                            crashReset(true);
                        }
                        break;
                    case ATBluetooth.RETURN_A2DP_ID3: {
                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                            parserID3((String) ((TObject) msg.obj).obj2);
                        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                            parserID3Parrot((String) ((TObject) msg.obj).obj2);
                        }

                    }
                    break;
                    case ATBluetooth.RETURN_A2DP_ID3_NAME:
                        mID3Name = ((String) ((TObject) msg.obj).obj2);
                        sendID3Info();
                        break;

                    case ATBluetooth.RETURN_A2DP_ID3_ARTIST:
                        mID3Artist = ((String) ((TObject) msg.obj).obj2);
                        sendID3Info();
                        break;

                    case ATBluetooth.RETURN_A2DP_ID3_ALBUM:
                        mID3Album = ((String) ((TObject) msg.obj).obj2);
                        sendID3Info();
                        break;
                    case ATBluetooth.RETURN_A2DP_ID3_TOTAL_TIME:
                        String s = ((String) ((TObject) msg.obj).obj2);
                        try {
                            mTotalTime = Integer.parseInt(s);
                            mTotalTime /= 1000;
                        } catch (Exception e) {
                            Log.d(TAG, "parserID3 int err!");
                            mTotalTime = 0;
                        }
                        sendID3Info();
                        break;
                    case ATBluetooth.RETURN_A2DP_CUR_TIME: {
                        try {
                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                                mCurTime = Integer.parseInt((String) ((TObject) msg.obj).obj2, 16);
                                mCurTime /= 1000;
                            } else {
                                String time = (String) ((TObject) msg.obj).obj2;
                                mCurTime = Integer.parseInt(time.substring(0, 4), 16);
                                mTotalTime = Integer.parseInt(time.substring(4, 8), 16);
                            }
                            sendA2DPTimeInfo();
                        } catch (Exception e) {
                            Log.d(TAG, "RETURN_A2DP_CUR_TIME int err!");
                            mCurTime = 0;
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_PAIR_INFO: {
                        TObject obj = (TObject) msg.obj;

                        int index = msg.arg1 & 0xff;
                        if (index == 1) {
                            mPairInfo.clear();
                        }
                        // check if exist
                        int i;
                        for (i = 0; i < mPairInfo.size(); ++i) {
                            PhoneBook pb = mPairInfo.get(i);
                            if (pb.mNumber != null && pb.mNumber.equals(obj.obj2) && pb.mName != null && pb.mName.equals(obj.obj3)) {
                                break;
                            }
                        }

                        if (i >= mPairInfo.size()) {
                            PhoneBook pb = new PhoneBook((String) obj.obj2, (String) obj.obj3, "" + ((msg.arg1 & 0xff00) >> 8));
                            pb.mIndex = (msg.arg1 & 0xff);
                            mPairInfo.add(pb);
                        }

                        if (((msg.arg1 & 0xff00) >> 8) == 1) {
                            notifyManagerObd(true);
                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                                Util.setProperty("ak.obd.addr", ((String) obj.obj2).toUpperCase());
                                Util.setProperty("ak.obd.name", ((String) obj.obj3));
                            }
                        }

                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                            if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED && ATBluetooth.mCurrentMac == null && ((msg.arg1 & 0x10000) != 0)) {
                                initCurrenAndPhoneBook((String) obj.obj2);

                                if (mPhoneBookInfo.size() > 0) {
                                    if (mVoicRecognitionView != null) {
                                        mVoicRecognitionView.showMicButton();
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_SEARCH: {
                        if (msg.obj != null) {
                            TObject obj = (TObject) msg.obj;

                            int i;
                            for (i = 0; i < mSearchInfo.size(); ++i) {
                                PhoneBook pb = mSearchInfo.get(i);

                                if (pb.mNumber != null && pb.mNumber.equals(obj.obj2)) {
                                    if (pb.mName == null || !pb.mName.equals(obj.obj3)) {
                                        pb.mName = (String) obj.obj3;
                                    }
                                    break;
                                }
                            }

                            if (i >= mSearchInfo.size()) {
                                mSearchInfo.add(new PhoneBook((String) obj.obj2, (String) obj.obj3, msg.arg1 + ""));
                            }
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_SEARCH_TYPE: {
                        if (msg.arg1 == 1) {
                            if (mSearchInfo.size() > 0) {
                                PhoneBook pb = mSearchInfo.get(mSearchInfo.size() - 1);
                                pb.mPinyin = "1";
                                mOBDMac = pb.mNumber;
                                saveData(SAVE_GOC_OBD_MAC, mOBDMac);
                            }
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_GOC_MUTE: {
                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_GOC_MUTE);
                        break;
                    }
                    case ATBluetooth.RETURN_GOC_UNMUTE: {
                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_GOC_UNMUTE);
                        break;
                    }
                    case ATBluetooth.RETURN_SEARCH_START: {
                        mSearchInfo.clear();
                        break;
                    }
                    case ATBluetooth.RETURN_OBD_DISCONNECT:
                    case ATBluetooth.RETURN_CLEAR_PAIR: {
                        notifyManagerObd(false);

                        break;
                    }
                    case ATBluetooth.RETURN_PAIR_ADDR:
                        if (msg.arg1 == 1) {
                            if (msg.obj != null) {
                                TObject obj = (TObject) msg.obj;
                                for (int i = 0; i < mSearchInfo.size(); ++i) {
                                    PhoneBook pb = mSearchInfo.get(i);
                                    if (pb.mNumber != null && pb.mNumber.equals(obj.obj2)) {
                                        if ("1".equals(pb.mPinyin)) {
                                            //									if (!mShowNotifyObdIcon){
                                            notifyManagerObd(true);
                                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                                                Util.setProperty("ak.obd.addr", mOBDMac);
                                                Util.setProperty("ak.obd.name", ((String) obj.obj3));
                                            }
                                            mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case ATBluetooth.RETURN_PBEI: {
                    }
                    break;
                    case ATBluetooth.RETURN_LANGUAGE: {
                        updateLanguage(msg.arg1);
                        break;
                    }
                    case ATBluetooth.RETURN_PPBU:
                        if (msg.arg1 == 7) {
                            mATBluetooth.write(ATBluetooth.REQUEST_VOICE_STATUS);
                        }
                        break;
                    case ATBluetooth.RETURN_CMD_OK: {
                        if (mCPGMUpdate) {
                            sendID3Info();
                            mCPGMUpdate = false;
                        }
                        break;
                    }

                    case ATBluetooth.RETURN_CPMC: {
                        TObject obj = (TObject) msg.obj;
                        mPreCmd = (String) obj.obj2;
                        break;
                    }
                    case ATBluetooth.RETURN_CPML: {
                        if (mPreCmd != null && !mPreCmd.equals("-1")) {
                            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_ID3, mPreCmd);
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_RRES: {
                        mVoicRecognitionView.returnResult(msg);
                        break;
                    }
                    case ATBluetooth.REQUEST_CMD_DGCD: {
                        TObject obj = (TObject) msg.obj;
                        sendA2DPCurPath((String) obj.obj2);
                        break;
                    }
                    case ATBluetooth.REQUEST_CMD_DGEC: {
                        mATBluetooth.write(ATBluetooth.REQUEST_CMD_DLSE, "1," + msg.arg1 + ",1");
                        mReqA2DPListNum = msg.arg1;
                        break;
                    }
                    case ATBluetooth.REQUEST_CMD_DSCD: {
                        mATBluetooth.write(ATBluetooth.REQUEST_CMD_DGCD);
                        break;
                    }
                    case ATBluetooth.REQUEST_CMD_DLSE: {
                        sendA2DPListInfo(msg.arg1);
                        break;
                    }
                    case ATBluetooth.REQUEST_REQUEST_VERSION: {
                        if (ATBluetoothActivity.mPausing && mRequestVersion) {
                            String obj2 = (String) ((TObject) msg.obj).obj2;
                            if (obj2 != null) {
                                Toast.makeText(mContext, obj2, Toast.LENGTH_LONG).show();
                            }
                            mRequestVersion = false;
                        }
                        break;
                    }
                    case ATBluetooth.REQUEST_UPDATE_BT: {
                        break;
                    }
                    case ATBluetooth.RETURN_3CALL_START: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        do3CallCome(msg.arg1 & 0xff, (msg.arg1 & 0xff00) >> 8, obj2);
                        break;
                    }
                    case ATBluetooth.RETURN_3CALL_END: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        do3CallEnd(msg.arg1 & 0xff, (msg.arg1 & 0xff00) >> 8, obj2);
                        break;
                    }
                    case ATBluetooth.RETURN_3CALL_STATUS: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        do3CallStatus(msg.arg1 & 0xff, (msg.arg1 & 0xff00) >> 8, obj2);
                        break;
                    }
                    case ATBluetooth.RETURN_UPDATE_STATUS: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        showUpdateUI(msg.arg1, obj2);
                        break;
                    }
                    case ATBluetooth.RETURN_BATTERY: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        mBattery = Integer.parseInt(obj2);
                        sendHPFBatterySignalInfo();
                        break;
                    }
                    case ATBluetooth.RETURN_SIGNAL: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        mSignal = Integer.parseInt(obj2);
                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                            mBattery = mSignal % 100;
                            mSignal = mSignal / 100;
                        }
                        sendHPFBatterySignalInfo();
                        break;
                    }
                    case ATBluetooth.RETURN_PAN_NET_CONNECT: {
                        notifyManagerNet(true);
                        break;
                    }
                    case ATBluetooth.RETURN_PAN_NET_DISCONNECT: {
                        notifyManagerNet(false);
                        break;
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            Handler hUI = mATBluetooth.getUIHandler();
            if (hUI != null) {
                hUI.sendMessage(hUI.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj));
            }
        }
    };


    private void showPhoneUI() {
        boolean retWake = false;

        switch (ATBluetooth.mCurrentHFP) {
            case ATBluetooth.HFP_INFO_CALLED:
            case ATBluetooth.HFP_INFO_INCOMING:
                if (!ATBluetoothActivity.mPausing) {
                    if (mCallName == null) {
                        mCallName = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, mCallNumber);
                    }
                } else {
                    mIncoming.show(true);
                }

                reportToCanbox(ATBluetooth.mCurrentHFP, mCallNumber);
                retWake = true;
                break;

            case ATBluetooth.HFP_INFO_CALLING:
                if (!ATBluetoothActivity.mPausing) {
                    // ATBluetoothService.mAC.showUI(R.id.dial);
                    if (mCallName == null) {
                        mCallName = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, mCallNumber);
                    }
                } else {
                    mIncoming.show(true);
                }
                mIncoming.startUpdateTime();

                reportToCanbox(ATBluetooth.mCurrentHFP, mCallNumber);

                retWake = true;
                break;
        }
        if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CALLED) {
            mIncoming.hide();
            mCallName = null;
            mCallNumber = null;

            reportToCanbox(ATBluetooth.mCurrentHFP, null);

            retWake = false;
        }

        if (retWake) {
            wakeLock();
        } else {
            wakeRelease();
        }
        returnCallInfo();
    }

    private boolean mRequestVersion = false;
    private String mBTVersion;
    private String mPreCmd;
    private boolean mCPGMUpdate = false;


    private void parserID3Parrot(String s) {
        if (s != null) {
            String[] ss = s.split(",");
            try {
                if (ss.length >= 2) {
                    switch (ss[0]) {
                        case "1":
                            mID3Name = ss[1].substring(1, ss[1].length() - 1);
                            mCPGMUpdate = true;
                            break;
                        case "2":
                            mID3Artist = ss[1].substring(1, ss[1].length() - 1);
                            mCPGMUpdate = true;
                            break;
                        case "3":
                            mID3Album = ss[1].substring(1, ss[1].length() - 1);
                            mCPGMUpdate = true;
                            break;
                        case "4":
                            // sendID3Info();
                            // mATBluetooth.write(ATBluetooth.REQUEST_A2DP_ID3);
                            // mATBluetooth.write(ATBluetooth.REQUEST_A2DP_TIME);
                            break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, s + "parserID3Parrot err:" + e);
            }
        }

    }

    private void parserID3(String s) {
        if (s != null) {

            String s2 = s.replace('|', ',');
            String[] ss = s2.split(",");

            try {
                mTotalTime = Integer.parseInt(ss[0]);
                mTotalTime /= 1000;
            } catch (Exception e) {
                Log.d(TAG, "parserID3 int err!");
                mTotalTime = 0;
            }

            if (ss.length >= 2) {
                mID3Name = ss[1];
            } else {
                mID3Name = "";
            }

            if (ss.length >= 3) {
                mID3Artist = ss[2];
            } else {

                mID3Artist = "";
            }

            if (ss.length >= 4) {
                mID3Album = ss[3];
            } else {
                mID3Album = "";
            }

        } else {
            mID3Name = "";
            mID3Artist = "";
            mID3Album = "";
            mTotalTime = 0;
        }
        sendID3Info();
        Log.d(TAG, "parserID3" + mTotalTime + ":" + mID3Name + ":" + mID3Artist + ":" + mID3Album);
    }

    public static int mTotalTime = 0;
    public static int mCurTime = 0;

    public static String mID3Name = "";
    public static String mID3Artist = "";
    public static String mID3Album = "";

    // private int mCall;
    public static String mCallNumber;
    private String mCallName;

    private void reportToCanbox(int status, String num) {
        Intent i = new Intent(MyCmd.BT_PHONE_BROADCAST);
        i.putExtra("status", status);
        i.putExtra("num", num);
        i.putExtra("name", mCallName);
        i.setPackage("com.my.out");
        Log.d("ffck1", "num=" + num);
        sendBroadcast(i);
    }

    private IncomingView mIncoming;
    private VoicRecognitionView mVoicRecognitionView;

    private final static int[] CALLING_NUM = new int[]{
            R.id.calling_num0, R.id.calling_num1, R.id.calling_num2, R.id.calling_num3, R.id.calling_num4, R.id.calling_num5, R.id.calling_num6, R.id.calling_num7, R.id.calling_num8,
            R.id.calling_num9, R.id.calling_num_jing, R.id.calling_num_xing,
    };

    private class IncomingView {
        private WindowManager mWindowManager;
        private WindowManager.LayoutParams mLayoutParams;

        private Presentation mPresentation = null;

        private View mView;
        private TextView mTextViewTime;
        private TextView mIncomingText;

        private TextView m3CallTextView;
        private TextView m3CallStatus;
        public View m3CallLayout;

        private ImageView mButonShow;
        private boolean isShow = false;

        private View mViewKeyboard;

        public void release() {
            hide();
            if (mPresentation != null) {
                mPresentation = null;
            }
        }

        public IncomingView() {
            String s = MachineConfig.getPropertyOnce(MachineConfig.KEY_SCREEN1_VIEW);
            if (s != null && s.contains(MachineConfig.VALUE_SCREEN1_VIEW_BT)) {
                DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
                Display[] display = displayManager.getDisplays();

                int layoutid = 0;
                if (display.length > 1) {

                    if (display[1].getWidth() == 800) {
                        layoutid = R.layout.incoming_800;
                    } else {
                        layoutid = R.layout.incoming_1024;
                    }

                    mView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(layoutid, null);

                    mPresentation = new Presentation(getApplicationContext(), display[1], R.style.TranslucentTheme);
                    WindowManager.LayoutParams lp = Objects.requireNonNull(mPresentation.getWindow()).getAttributes();
                    lp.alpha = 0.92f;
                    lp.width = LayoutParams.WRAP_CONTENT;
                    lp.height = LayoutParams.WRAP_CONTENT;

                    mPresentation.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
                    mPresentation.getWindow().setGravity(Gravity.BOTTOM);

                    mPresentation.setContentView(mView);
                }
            }

            if (mPresentation == null) {
                mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                mLayoutParams = new WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0, LayoutParams.TYPE_SYSTEM_ERROR, LayoutParams.FLAG_LAYOUT_NO_LIMITS | LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGBA_8888);

                mLayoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                mLayoutParams.alpha = 0.92f;

                mView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.incoming, null);

            }

            mViewKeyboard = mView.findViewById(R.id.keyboard_layout);
            mIncomingText = (TextView) mView.findViewById(R.id.incoming);

            m3CallLayout = mView.findViewById(R.id.layout_3call);
            m3CallTextView = (TextView) mView.findViewById(R.id.incoming_3call);
            m3CallStatus = (TextView) mView.findViewById(R.id.calling_time_3call);

            ((ImageView) mView.findViewById(R.id.answering)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    // hide();
                    doAnswer();
                }
            });
            ((ImageView) mView.findViewById(R.id.reject)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    doHang();
                    // hide();
                }
            });

            ((ImageView) mView.findViewById(R.id.calling_voice)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    doVoiceSwitch();

                }
            });

            if (isUISupport3Call()) {
                if (!ATBluetooth.isSupport3Call()) {
                    mView.findViewById(R.id.calling_3call_switch).setVisibility(View.GONE);
                    mView.findViewById(R.id.calling_3call_merge).setVisibility(View.GONE);
                } else {

                    mView.findViewById(R.id.calling_3call_switch).setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.calling_3call_merge).setVisibility(View.VISIBLE);

                    ((ImageView) mView.findViewById(R.id.calling_3call_switch)).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mATBluetooth.write(ATBluetooth.REQUEST_3CALL_ANSWER);

                        }
                    });

                    ((ImageView) mView.findViewById(R.id.calling_3call_merge)).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mATBluetooth.write(ATBluetooth.REQUEST_3CALL_MERGE);

                        }
                    });
                }
            }

            ((ImageView) mView.findViewById(R.id.keyboard)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleKeyboard();
                }
            });

            View v1;
            v1 = mView.findViewById(R.id.incoming_hide);
            if (v1 != null) {
                ((ImageView) v1).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMain(false);
                    }
                });
            }

            v1 = mView.findViewById(R.id.incoming_show);
            if (v1 != null) {
                mButonShow = ((ImageView) v1);
                ((ImageView) v1).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMain(true);
                    }
                });
            }

            for (int i : CALLING_NUM) {
                View v = mView.findViewById(i);
                if (v != null) {
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            doKeyNum(v);
                        }
                    });
                }
            }

            mTextViewTime = (TextView) mView.findViewById(R.id.calling_time);
            stopUpdateTime();
        }

        private boolean isUISupport3Call() {
            return (m3CallLayout != null);
        }

        private void showMain(boolean show) {
            //	mHandlerTime.removeMessages(0);

            if (show) {
                if (mButonShow != null) {
                    mButonShow.setVisibility(View.GONE);
                }
                if (mView.findViewById(R.id.incoming_main) != null) {
                    mView.findViewById(R.id.incoming_main).setVisibility(View.VISIBLE);
                }
            } else {
                if (mButonShow != null) {
                    mButonShow.setVisibility(View.VISIBLE);
                    mButonShow.setImageResource(R.drawable.ic_launcher);
                }
                if (mView.findViewById(R.id.incoming_main) != null) {
                    mView.findViewById(R.id.incoming_main).setVisibility(View.GONE);
                }
                //	mHandlerTime.sendEmptyMessageDelayed(1, 1000);
            }
        }

        private void toggleKeyboard() {
            if (mView.findViewById(R.id.keyboard_layout).getVisibility() == View.VISIBLE) {
                mView.findViewById(R.id.keyboard_layout).setVisibility(View.GONE);
            } else {
                mView.findViewById(R.id.keyboard_layout).setVisibility(View.VISIBLE);
            }

        }

        private void updateVoice() {
            if (isShow) {
                View v = mView.findViewById(R.id.calling_voice);
                if (mATBluetooth.getVoiceSwitchLocal()) {
                    ((ImageView) v).setImageResource(R.drawable.voice);
                } else {
                    ((ImageView) v).setImageResource(R.drawable.voice1);
                }

            }
        }

        @SuppressLint("DefaultLocale")
        private void updateTime() {
            if (mTextViewTime != null) {
                String s;
                if (ATBluetooth.mTime < 3600) {
                    s = String.format("%02d:%02d", (ATBluetooth.mTime / 60), (ATBluetooth.mTime % 60));
                } else {
                    s = String.format("%02d:%02d:%02d", (ATBluetooth.mTime / 3600), ((ATBluetooth.mTime % 3600) / 60), (ATBluetooth.mTime % 60));
                }


                if (isUISupport3Call()) {
                    String s2;
                    if (ATBluetooth.m3CallActiveIndex == 2) {
                        if (ATBluetooth.mTime2 < 3600) {
                            s2 = String.format("%02d:%02d", (ATBluetooth.mTime2 / 60), (ATBluetooth.mTime2 % 60));
                        } else {
                            s2 = String.format("%02d:%02d:%02d", (ATBluetooth.mTime2 / 3600), ((ATBluetooth.mTime2 % 3600) / 60), (ATBluetooth.mTime2 % 60));
                        }
                        s = getString(R.string.pause);
                    } else {
                        s2 = getString(R.string.pause);
                    }
                    m3CallStatus.setText(s2);
                }

                mTextViewTime.setText(s);
            }

            if (ATBluetooth.mCallingNum > 1) {
                ATBluetooth.mTime2++;
            }

            ATBluetooth.mTime++;
            mHandlerTime.removeMessages(0);
            mHandlerTime.sendEmptyMessageDelayed(0, 1000);
        }


        public void startUpdateTime() {
            if (isShow) {
                mTextViewTime.setText("");
                // mTextViewTime.setVisibility(View.VISIBLE);
                updateTime();
            }
        }

        private void stopUpdateTime() {
            mHandlerTime.removeMessages(0);
            mTextViewTime.setText("");
            if (ATBluetooth.mCurrentHFP != ATBluetooth.HFP_INFO_CALLING) {
                ATBluetooth.mTime = 0;
                ATBluetooth.mTime2 = 0;
            }
        }

        private final Handler mHandlerTime = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        updateTime();
                        break;
                    case 1:
                        int delay = 1000;
                        if (mButonShow != null && mButonShow.getVisibility() == View.VISIBLE) {
                            if (mButonShow.getDrawable() != null) {
                                mButonShow.setImageDrawable(null);
                            } else {
                                mButonShow.setImageResource(R.drawable.ic_launcher);
                            }
                            mHandlerTime.sendEmptyMessageDelayed(1, delay);
                        }
                        break;
                }
            }
        };

        public void setNumber(String s) {
            mIncomingText.setText(s);
        }

        public String getNumber() {
            return mIncomingText.getText().toString();
        }

        @SuppressLint("SetTextI18n")
        public void show(boolean visible) {
            if (mCanboxPhone) {
                hide();
                mCanboxPhone = false;
            }
            if (!isShow) {
                isShow = true;
                if (visible) {
                    mIncomingText.setVisibility(View.VISIBLE);
                    if (ATBluetooth.m3CallNumber == null || ATBluetooth.isSupport3Call()) {
                        if (mCallName == null) {
                            mCallName = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, mCallNumber);
                        }
                        if (mCallName == null) {
                            mIncomingText.setText(mCallNumber);
                        } else {
                            mIncomingText.setText(mCallName + " " + mCallNumber);
                        }
                    }

                } else {
                    mIncomingText.setVisibility(View.INVISIBLE);
                }
                if (mPresentation != null) {
                    mPresentation.show();
                } else {
                    mWindowManager.addView(mView, mLayoutParams);
                }

                mView.findViewById(R.id.keyboard_layout).setVisibility(View.GONE);

                if (ATBluetooth.mCallingNum > 1) {
                    if (ATBluetooth.mTime == 0) {
                        startUpdateTime();
                    }
                }
            }

            if (visible && mCallName != null) {
                if (ATBluetooth.m3CallNumber == null) {
                    Log.d("ccde", mCallName + " " + mCallNumber);
                    mIncomingText.setText(mCallName + " " + mCallNumber);
                }
            }

            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                if (mIncomingText.getText().length() == 0) {
                    if (mCallName == null) {
                        mCallName = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, mCallNumber);
                    }
                    if (mCallName == null) {
                        mIncomingText.setText(mCallNumber);
                    } else {
                        mIncomingText.setText(mCallName + " " + mCallNumber);
                    }
                }
            }
        }

        public void hide() {
            if (isShow) {
                if (mPresentation != null) {
                    mPresentation.dismiss();
                } else {

                    mWindowManager.removeView(mView);
                }

                isShow = false;
                showMain(true);
                stopUpdateTime();

                View v = mView.findViewById(R.id.calling_voice);
                if (v != null) {
                    ((ImageView) v).setImageResource(R.drawable.voice);
                }
            }
        }

        public void showCanbox() {
            if (!isShow) {
                isShow = true;

                mCanboxPhone = true;

                mIncomingText.setVisibility(View.GONE);

                if (mPresentation != null) {
                    mPresentation.show();
                } else {
                    mWindowManager.addView(mView, mLayoutParams);
                }

                mView.findViewById(R.id.keyboard_layout).setVisibility(View.VISIBLE);

                View v = mView.findViewById(R.id.calling_voice);
                if (v != null) {
                    ((ImageView) v).setImageResource(R.drawable.radio_power_off);
                }
            }

        }

        public void updateCanboxText(String text) {

            mIncomingText.setVisibility(View.VISIBLE);
            mIncomingText.setText(text);

        }
    }

    private boolean mCanboxPhone = false;
    boolean mShowNotifyIcon = false;
    boolean mShowNotifyObdIcon = false;
    boolean mShowNotifyNetIcon = false;
    private final String channel_id = TAG + ".MyChannel";

    private void notifyManager(int hfpState) {
        //	Log.d(TAG, "notifyManager:" + hfpState + ":" + GlobalDef.mBTCellStatusBar);
        if (hfpState > ATBluetooth.HFP_INFO_CONNECTED) {
            reportToCanbox(ATBluetooth.mCurrentHFP, mCallNumber);
        } else {
            reportToCanbox(ATBluetooth.mCurrentHFP, null);
        }
        sendHPFBatterySignalInfo();
        sendHFPInfo();
        checkCarPlayConnect();
        boolean show = false;
        if (hfpState >= ATBluetooth.HFP_INFO_CONNECTED) {
            show = true;
        } else {
            mDownCallLogSuccess = false;
        }

        if (!show) {
            ATBluetooth.mCallingNum = 0;
            ATBluetooth.m3CallNumber = null;
        }

        if (show != mShowNotifyIcon) {
            if (!ATBluetoothActivity.mPausing) {
                mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
            }
        }

        if (GlobalDef.mBTCellStatusBar == 1 && hfpState != 0) {
            return;
        }

        if (show != mShowNotifyIcon) {
            mShowNotifyIcon = show;

            try {
                ///CharSequence connectText;
                ///Notification notification = new Notification();
                Notification.Builder notificationBuilder = new Notification.Builder(mContext);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (show) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, ATBluetoothActivity.class), PendingIntent.FLAG_IMMUTABLE);
                        NotificationChannel channel = new NotificationChannel(channel_id, "MyChannel", NotificationManager.IMPORTANCE_DEFAULT);
                        notificationManager.createNotificationChannel(channel);
                        notificationBuilder.setChannelId(channel_id);
                        notificationBuilder.setSmallIcon(R.drawable.bt);
                        notificationBuilder.setContentIntent(contentIntent);
                    }
                    /// startForeground(R.string.bluetooth_settings, notification);
                    notificationManager.notify(R.string.bluetooth_settings, notificationBuilder.build());
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        notificationManager.cancel(R.string.bluetooth_settings);
                    } else {
                        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        notificationBuilder.setContentIntent(pendingintent);
                        notificationManager.notify(R.string.bluetooth_settings, notificationBuilder.build());
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "notifyManager systemui not ready:" + e);
                mHandler.removeMessages(MSG_NOTIFY_ICON);
                mHandler.sendEmptyMessageDelayed(MSG_NOTIFY_ICON, 2000);
            }
        }
    }

    private int mOBDId = 0x1234;

    private void notifyManagerObd(boolean show) {

        if (show != mShowNotifyObdIcon) {
            mShowNotifyObdIcon = show;
            try {
                ///CharSequence connectText;
                //Notification notification = new Notification();
                Notification.Builder notificationBuilder = new Notification.Builder(mContext);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (show) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, ATBluetoothActivity.class), PendingIntent.FLAG_IMMUTABLE);
                        NotificationChannel channel = new NotificationChannel(channel_id, "MyChannel", NotificationManager.IMPORTANCE_DEFAULT);
                        notificationManager.createNotificationChannel(channel);
                        notificationBuilder.setChannelId(channel_id);
                        notificationBuilder.setSmallIcon(R.drawable.obd_1200);
                        notificationBuilder.setContentIntent(contentIntent);
                    }
                    /// startForeground(R.string.bluetooth_settings, notification);
                    notificationManager.notify(mOBDId, notificationBuilder.build());
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        notificationManager.cancel(mOBDId);
                    } else {
                        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        notificationBuilder.setContentIntent(pendingintent);
                        notificationManager.notify(mOBDId, notificationBuilder.build());
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "notifyManager systemui not ready:" + e);
                mHandler.removeMessages(MSG_NOTIFY_ICON);
                mHandler.sendEmptyMessageDelayed(MSG_NOTIFY_ICON, 2000);
            }
        }
    }


    private void notifyManagerNet(boolean show) {

        if (show != mShowNotifyNetIcon) {
            mShowNotifyNetIcon = show;
            try {
                ///CharSequence connectText;
                ///Notification notification = new Notification();
                Notification.Builder notificationBuilder = new Notification.Builder(mContext);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                if (show) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, ATBluetoothActivity.class), PendingIntent.FLAG_IMMUTABLE);
                        NotificationChannel channel = new NotificationChannel(channel_id, "MyChannel", NotificationManager.IMPORTANCE_DEFAULT);
                        notificationManager.createNotificationChannel(channel);
                        notificationBuilder.setChannelId(channel_id);
                        notificationBuilder.setSmallIcon(R.drawable.stat_sys_ethernet_fully);
                        notificationBuilder.setContentIntent(contentIntent);
                    }
                    /// startForeground(R.string.bluetooth_settings,notification);
                    notificationManager.notify(mOBDId, notificationBuilder.build());
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        notificationManager.cancel(mOBDId);
                    } else {
                        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        notificationBuilder.setContentIntent(pendingintent);
                        notificationManager.notify(mOBDId, notificationBuilder.build());
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "notifyManager systemui not ready:" + e);
                mHandler.removeMessages(MSG_NOTIFY_ICON);
                mHandler.sendEmptyMessageDelayed(MSG_NOTIFY_ICON, 2000);
            }
        }
    }

    private static ATBluetoothService mContext;

    private void setHardWarePower(boolean b) {
        if (b) {
            Util.setFileValue("/sys/class/ak/source/bluetoothsw", 1);
            Util.do_exec("start ivt_blueletd");

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initHardware();
                }
            }, 1500);
        }
    }

    private void initCmd() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            mATBluetooth.write(ATBluetooth.REQUEST_IF_AUTO_CONNECT);
            mATBluetooth.write(ATBluetooth.GET_HFP_INFO);

            // mATBluetooth.write(ATBluetooth.REQUEST_NAME, "CAR KIT");

            mATBluetooth.write(ATBluetooth.REQUEST_NAME);
            mATBluetooth.write(ATBluetooth.REQUEST_PIN);

            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS);

            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_ID3);
            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_TIME);

            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_REPORT_ID3);

            mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_DEVICE);
            mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {

            mATBluetooth.write(ATBluetooth.REQUEST_SSP_MODE);

            mATBluetooth.write(ATBluetooth.REQUEST_GOC_BT_MODULE_MAC);
            mATBluetooth.write(ATBluetooth.REQUEST_NAME);

            mATBluetooth.write(ATBluetooth.REQUEST_PIN);

            mATBluetooth.write(ATBluetooth.REQUEST_DISCOVERABLE);
            mATBluetooth.write(ATBluetooth.REQUEST_CONNECTABLE);
            mATBluetooth.write(ATBluetooth.REQUEST_SNIFF_MODE);
            mATBluetooth.write(ATBluetooth.REQUEST_CMD_AADC, "1,20");
            mATBluetooth.write(ATBluetooth.REQUEST_CMD_AADC, "2,297");

            mATBluetooth.write(ATBluetooth.REQUEST_CMD_ALAC, "'Audio/Config/tango-algo.xml'");
            mATBluetooth.write(ATBluetooth.REQUEST_SET_VOLUME, "5,0,11");
            mATBluetooth.write(ATBluetooth.REQUEST_SET_VOLUME, "2,0,0");
            mATBluetooth.write(ATBluetooth.REQUEST_SET_VOLUME, "10,0,0");

            mATBluetooth.write(ATBluetooth.GET_HFP_INFO);
            mATBluetooth.write(ATBluetooth.GET_HFP_INFO2);
            // mATBluetooth.write(ATBluetooth.REQUEST_A2DP_ID3, "30");
            mATBluetooth.write(ATBluetooth.REQUEST_DEVICE_INDEX);

            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK_DOWN_STATUS);
            // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK_SYNC);

            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS);
            checkParrotLanguage();
            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_TIME);
            mATBluetooth.write(ATBluetooth.REQUEST_CLMS);
            mHandler.sendEmptyMessageDelayed(MSG_SET_CONNECT_MODE, 1000);
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {

            mATBluetooth.write(ATBluetooth.GET_HFP_INFO);

            mATBluetooth.write(ATBluetooth.GET_HFP_INFO2);
            // mATBluetooth.write(ATBluetooth.REQUEST_NAME, "CAR KIT");

            mATBluetooth.write(ATBluetooth.REQUEST_NAME);
            mATBluetooth.write(ATBluetooth.REQUEST_PIN);

            //			mATBluetooth.write(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS);
            //
            //			mATBluetooth.write(ATBluetooth.REQUEST_A2DP_ID3);
            //			mATBluetooth.write(ATBluetooth.REQUEST_A2DP_TIME);
            //
            //			mATBluetooth.write(ATBluetooth.REQUEST_A2DP_REPORT_ID3);
            //
            //			mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_DEVICE);
            mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);

            initBTMicGain();
        }
    }

    private final static int HARDWARE_CHECKTIME = 10;
    private int mCheckHardWareStatusTime = HARDWARE_CHECKTIME;

    private void initHardware() {
        Log.d(TAG, "initHardware:" + mCheckHardWareStatusTime);
        if (mATBluetooth == null) {
            mATBluetooth = ATBluetooth.create();
            if (mATBluetooth != null) {
                mATBluetooth.addHandler(TAG, mHandler);
            }
        }
        if (mATBluetooth != null) {
            mHandler.removeMessages(MSG_CHECK_HARDWARE_STATUS);
            mHandler.removeMessages(MSG_INIT_CMD);
            if (mATBluetooth.mHardwareStatus) {
                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                    mHandler.sendEmptyMessageDelayed(MSG_INIT_CMD, 2000);
                    mHandler.removeMessages(MSG_CHECK_CONNECT_STATUS_ACC_POWER_ON);
                    mHandler.sendEmptyMessageDelayed(MSG_CHECK_CONNECT_STATUS_ACC_POWER_ON, 4200);
                } else {
                    initCmd();
                }
                mCheckHardWareStatusTime = HARDWARE_CHECKTIME;
            } else {
                if (mCheckHardWareStatusTime > 0) {
                    mHandler.removeMessages(MSG_CHECK_HARDWARE_STATUS);
                    mHandler.sendEmptyMessageDelayed(MSG_CHECK_HARDWARE_STATUS, 2000);
                } else {
                    if (mCheckHardWareCrash > 0) {
                        mHandler.removeMessages(MSG_CHECK_HARDWARE_CRASH);
                        mHandler.sendEmptyMessageDelayed(MSG_CHECK_HARDWARE_CRASH, 700);
                        mCheckHardWareCrash--;
                    }
                }
                mATBluetooth.open();
                --mCheckHardWareStatusTime;
            }
        }
    }


    private final static int HARDWARE_CRASHTIME = 5;
    private int mCheckHardWareCrash = HARDWARE_CHECKTIME;

    private void crashReset(boolean ui) {
        Log.d("allen", "crashReset!!!!!!" + getPackageName());
        if (ATBluetooth.mBTType != MachineConfig.VAULE_BT_TYPE_IVT) { //only ivt need reset
            return;
        }
        if (ui) {
            if (mIncoming != null) {
                mIncoming.hide();
            }
            notifyManager(0);
            if (ATBluetoothActivity.mThis != null) {
                ATBluetoothActivity.mThis.finish();
            }
        }

        mATBluetooth.close();
        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_IVT_CRASH_MUTE);
        Util.doSleep(50);
        Util.setFileValue("/sys/class/ak/source/bluetoothsw", 0);
        mHandler.removeMessages(MSG_START_IVT_BLUELETD);
        mHandler.removeMessages(MSG_STOP_IVT_BLUELETD);
        Util.doSleep(1800);
        Util.setProperty("ctl.stop", "ivt_blueletd");
        Util.doSleep(100);
        Util.setFileValue("/sys/class/ak/source/bluetoothsw", 1);
        Util.doSleep(100);
        Util.setProperty("ctl.start", "ivt_blueletd");
        Util.doSleep(1000);
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        //Log.d(TAG, android.os.Process.myPid()+"crashReset!22!!!!!force stop:"+getPackageName());
        Util.sudoExec("kill:" + android.os.Process.myPid());
    }

    private final static int CONNECT_STATUS_ACC_POWER_ON_CHECKTIME = 2;
    private int mCheckAccPowerOnTime = 0;

    private void checkConnectAccPowerOn() { //no need now
    }

    public static boolean mBTCoreCrash = false;
    private String mDefaultName = null;
    private String mDefaultPasswd = null;
    private int mUpdateNameTime = 0;
    private final static String DEFAULT_NAME = "CAR KIT";
    private final static String CUSTOM1_NAME = "CAR-BT";
    private final static String DEFAULT_NAME_PARROT = "FC6000TN";
    private final static String DEFAULT_NAME_GOC = "CAR KIT-K";


    private final static String DEFAULT_PASSWD = "0000";
    private final static String DEFAULT_PASSWD_PARROT = "1234";

    private void getDefaultNameConfig() {

        String value = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_BT_NAME);
        if (value != null) {
            if (value.length() > 0) {
                mDefaultName = value;
            }
        } else {
            value = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);
            if (value != null) {
                if (value.equals(MachineConfig.VALUE_SYSTEM_UI_KLD1)) {
                    mDefaultName = CUSTOM1_NAME;
                }
            }
        }

        value = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_BT_PASSWD);
        if (value != null) {
            if (value.length() > 0) {
                mDefaultPasswd = value;
            }
        }

    }

    private void resetDefaultPasswd(String name) {
        String firstBootTag = SAVE_FIRST_BOOT_PASSWD + ATBluetooth.mBTType;
        if (getData(firstBootTag) != 1) {

            saveData(firstBootTag, 1);
            Log.d(TAG, "resetDefaultPasswd:" + name);
            if (mDefaultPasswd != null) {
                if (!mDefaultPasswd.equals(mATBluetooth.mBtPin)) {
                    mATBluetooth.write(ATBluetooth.REQUEST_PIN, mDefaultPasswd);
                }
            } else {
                String defaultName = null;
                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                    defaultName = DEFAULT_PASSWD;
                } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                    defaultName = DEFAULT_PASSWD_PARROT;
                }

                if (defaultName != null && !defaultName.equals(mATBluetooth.mBtPin)) {
                    mATBluetooth.write(ATBluetooth.REQUEST_PIN, defaultName);
                }
            }
        }
    }

    private void resetDefaultName(String name) {
        String firstBootTag = SAVE_FIRST_BOOT + ATBluetooth.mBTType;
        if (getData(firstBootTag) != 1) {

            saveData(firstBootTag, 1);
            Log.d(TAG, "resetDefaultName:" + name);
            if (mUpdateNameTime > 1) { // only update 2time
                return;
            }
            ++mUpdateNameTime;
            if (mDefaultName != null) {
                if (!mDefaultName.equals(mATBluetooth.mBtName)) {
                    mATBluetooth.write(ATBluetooth.REQUEST_NAME, mDefaultName);
                }
            } else {
                String defaultName = null;
                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                    defaultName = DEFAULT_NAME;
                } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                    defaultName = DEFAULT_NAME_PARROT;
                } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                    defaultName = DEFAULT_NAME_GOC;
                }

                if (defaultName != null && !defaultName.equals(mATBluetooth.mBtName)) {
                    mATBluetooth.write(ATBluetooth.REQUEST_NAME, defaultName);
                }
            }
        }
    }

    public static boolean musicKeyControl(int code) {
        boolean ret = true;
        if (mContext != null) {
            if (code == MyCmd.Keycode.PLAY) {
                if (mPlayStatus == ATBluetooth.A2DP_INFO_PLAY) {
                    ret = false;
                }
            } else if (code == MyCmd.Keycode.PAUSE) {
                if (mPlayStatus == ATBluetooth.A2DP_INFO_PAUSED) {
                    return false;
                }
            }
            mContext.doMusicKeyControl(code);
        }
        return ret;
    }

    public static int mPlayStatus = 0;

    private void doMusicKeyControl(int code) {
        switch (code) {

            case MyCmd.Keycode.CH_UP:
            case MyCmd.Keycode.KEY_SEEK_NEXT:
            case MyCmd.Keycode.KEY_TURN_A:
            case MyCmd.Keycode.NEXT:

            case MyCmd.Keycode.KEY_DVD_UP:
            case MyCmd.Keycode.KEY_DVD_RIGHT:
                mATBluetooth.write(ATBluetooth.REQUEST_A2DP_NEXT);
                break;
            case MyCmd.Keycode.CH_DOWN:
            case MyCmd.Keycode.KEY_SEEK_PREV:
            case MyCmd.Keycode.KEY_TURN_D:
            case MyCmd.Keycode.PREVIOUS:
            case MyCmd.Keycode.KEY_DVD_DOWN:
            case MyCmd.Keycode.KEY_DVD_LEFT:
                mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PREV);
                break;
            case MyCmd.Keycode.PLAY_PAUSE:
                // mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PP);
                if (mPlayStatus == ATBluetooth.A2DP_INFO_PLAY) {
                    doMusicPause();
                } else {
                    doMusicPlay();
                }
                break;
            case MyCmd.Keycode.PLAY:
                doMusicPlay();
                break;
            case MyCmd.Keycode.PAUSE:
                doMusicPause();
                break;
        }
    }

    private static final int TIME_LOCK_KEY = 1000;
    private long mLockTime;
    private int mCheckTime = 0;
    private int mShouldBePlayStatus = 0;

    private void checkParrotPlayStatus() {
        if (mPlayStatus >= ATBluetooth.A2DP_INFO_CONNECTED && mPlayStatus != ATBluetooth.A2DP_INFO_PLAY && mShouldBePlayStatus == ATBluetooth.A2DP_INFO_PLAY) {
            mCheckTime++;
            if (mCheckTime < 10) {
                mHandler.removeMessages(MSG_MEIDA_PLAY_DELAY);
                mHandler.sendEmptyMessageDelayed(MSG_MEIDA_PLAY_DELAY, 1500);
            }
        } else {
            mCheckTime = 0;
            mShouldBePlayStatus = 0;
        }
    }

    private void doMusicPause() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            if ((System.currentTimeMillis() - mLockTime) < TIME_LOCK_KEY) {
                Log.e(TAG, "doMusicPlay lock");
                return;
            }
            mLockTime = System.currentTimeMillis();

            mCheckTime = 0;
            mShouldBePlayStatus = 0;
        }
        mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PAUSE);
    }

    private void doMusicPlay() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            if ((System.currentTimeMillis() - mLockTime) < TIME_LOCK_KEY) {
                Log.e(TAG, "doMusicPlay lock");
                return;
            }
            mLockTime = System.currentTimeMillis();
            if (mPlayStatus < ATBluetooth.A2DP_INFO_CONNECTED) {
                mATBluetooth.write(ATBluetooth.REQUEST_BT_MUSIC_CPCC);
                mATBluetooth.write(ATBluetooth.REQUEST_CLMS);
            }
            mShouldBePlayStatus = ATBluetooth.A2DP_INFO_PLAY;
            mHandler.removeMessages(MSG_MEIDA_PLAY_DELAY);
            mHandler.sendEmptyMessageDelayed(MSG_MEIDA_PLAY_DELAY, 200);
            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS);
        } else {
            mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PLAY);
        }

    }

    private void sendKeyToZlink(int code) {
        Log.d("zlink", "sendKeyToZlink::" + code);
        Intent it = new Intent(ZLINK_BROAST);
        it.putExtra("command", "REQ_SPEC_FUNC_CMD");
        it.putExtra("specFuncCode", code);
        mContext.sendBroadcast(it);
    }

    private void setCarPlayerStatus(boolean b) {
        mCarPlayConnect = b;
        Log.d(TAG, "setCarPlayerStatus:" + b);
    }

    private void doPhoneKeyControl(int code) {
        if (mCarPlayConnect) {
            switch (code) {
                case MyCmd.Keycode.BT:
                    if (mZlinkPhoneOn) {
                        code = KeyEvent.KEYCODE_ENDCALL;
                    } else {
                        code = KeyEvent.KEYCODE_CALL;
                    }
                    break;
                case MyCmd.Keycode.BT_DIAL:
                    code = KeyEvent.KEYCODE_CALL;
                    break;
                case MyCmd.Keycode.BT_HANG:
                    code = KeyEvent.KEYCODE_ENDCALL;
                    break;
                default:
                    return;
            }
            sendKeyToZlink(code);
            return;
        }
        switch (code) {
            case MyCmd.Keycode.BT:
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                } else if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {

                    mATBluetooth.write(ATBluetooth.REQUEST_HANG);
                } else {
                    if (ATBluetoothActivity.mPausing) {
                        UtilCarKey.doKeyBT(mContext);
                    }
                }
                break;

            case MyCmd.Keycode.BT_DIAL:
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                } else {
                    if (!ATBluetoothActivity.mPausing) {
                        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
                            ATBluetoothActivity.doKey(code);
                        }
                    } else {
                        UtilCarKey.doKeyBT(mContext);
                    }
                }
                break;
            case MyCmd.Keycode.KEY_JOY_DOWN:
            case MyCmd.Keycode.BT_HANG:
                doHang();
                break;
            case MyCmd.Keycode.KEY_JOY_UP:
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                }
                break;
            case MyCmd.Keycode.NUMBER0:
            case MyCmd.Keycode.NUMBER1:
            case MyCmd.Keycode.NUMBER2:
            case MyCmd.Keycode.NUMBER3:
            case MyCmd.Keycode.NUMBER4:
            case MyCmd.Keycode.NUMBER5:
            case MyCmd.Keycode.NUMBER6:
            case MyCmd.Keycode.NUMBER7:
            case MyCmd.Keycode.NUMBER8:
            case MyCmd.Keycode.NUMBER9:
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "" + (code - MyCmd.Keycode.NUMBER0));
                }
                break;
            case MyCmd.Keycode.NUMBER_STAR:
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "" + ("*"));
                }
                break;
            case MyCmd.Keycode.NUMBER_POUND:
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "" + ("#"));
                }
                break;
            case MyCmd.Keycode.KEY_BT_VOICE_SPEAKER:
                // if (!mATBluetooth.getVoiceSwitchLocal()) {
                mATBluetooth.write(ATBluetooth.REQUEST_VOICE_SWITCH_LOCAL);
                // }
                break;
            case MyCmd.Keycode.KEY_BT_VOICE_PHONE:
                // if (mATBluetooth.getVoiceSwitchLocal()) {
                mATBluetooth.write(ATBluetooth.REQUEST_VOICE_SWITCH_REMOTE);
                // }
                break;
        }
    }

    private void doKeyNum(View v) {
        if (!mCanboxPhone) {
            mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "" + ((TextView) v).getText().charAt(0));
        } else {
            int i;
            try {
                char c = ((TextView) v).getText().charAt(0);

                if (c == '*') {
                    i = 0xa;
                } else if (c == '#') {
                    i = 0xb;
                } else {
                    i = Integer.parseInt(c + "");
                }
                i = 0x80 | i;
                sendCanboxInfo(0x85, i);
            } catch (Exception ignored) {
            }
        }
    }

    private void doVoiceSwitch() {
        if (!mCanboxPhone) {
            if (!GlobalDef.isLock()) {
                GlobalDef.lock();
                mATBluetooth.write(ATBluetooth.REQUEST_VOICE_SWITCH);
            }
        } else {
            sendCanboxInfo(0x85, 0);
        }
    }

    public static void doHangService() {
        if (mContext != null) {
            mContext.doHang();
        }
    }

    public static void doAnswerService() {
        if (mContext != null) {
            mContext.doAnswer();
        }
    }

    private void doHang() {
        if (!mCanboxPhone) {
            if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
                mATBluetooth.write(ATBluetooth.REQUEST_EJECT);
            } else if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING || ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLED) {

                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
                    if (ATBluetooth.mCallingNum > 1) {
                        if (ATBluetooth.m3CallStatus == ATBluetooth.HFP_INFO_INCOMING) {
                            mATBluetooth.write(ATBluetooth.REQUEST_3CALL_HANG);
                        } else {
                            mATBluetooth.write(ATBluetooth.REQUEST_3CALL_HANG2);
                        }
                    } else {
                        mATBluetooth.write(ATBluetooth.REQUEST_HANG);
                    }
                } else {
                    mATBluetooth.write(ATBluetooth.REQUEST_HANG);
                }
            }
        } else {
            sendCanboxInfo(0x85, 0x3);
        }
    }

    private void doAnswer() {
        if (!mCanboxPhone) {

            if (!ATBluetooth.isSupport3Call()) {
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
                    if (ATBluetooth.mCallingNum > 1) {
                        mATBluetooth.write(ATBluetooth.REQUEST_3CALL_ANSWER);
                    } else {
                        mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                    }
                } else {
                    mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                }
            } else {
                if (ATBluetooth.m3CallStatus == ATBluetooth.HFP_INFO_INCOMING) {
                    mATBluetooth.write(ATBluetooth.REQUEST_3CALL_ANSWER);
                } else {
                    mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                }
            }
        } else {
            sendCanboxInfo(0x85, 0x2);
        }
    }

    private void sendA2DPInfo() {
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_A2DP_STATUS);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, mPlayStatus);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mATBluetooth.mBtName);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA3, mATBluetooth.mBtPin);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA4, mATBluetooth.mBtModuleMac);

        Log.d("kkdf", ":" + mATBluetooth.mBtModuleMac);
        mContext.sendBroadcast(it);
    }

    //ww+ for txz
    private static int isVAEnabled = -1;
    private static int lastHFP = -1;
    private static String lastMac, lastNumber, lastName;

    public static void sendHFPInfoToVA(Context context, final int hfp, final String mac, final String number, final String name) {
        if (isVAEnabled == -1) {
            isVAEnabled = MachineConfig.getPropertyIntReadOnly("model_va");
            Log.d(TAG, "model_va=" + isVAEnabled);
        }
        if (isVAEnabled > 0) {
            String mode_va = null;
            try {
                mode_va = SystemProperties.get("ak.status.model_va", null);    //AKVoiceAssistant to set to identifies whether TXZ or AISpeech is installed
            } catch (Exception ignored) {
            }

            //			Log.d("AKVA", "hfp:" + lastHFP + ", " + hfp + ", " + mac + ", " + number + ", " + name);
            //hfp init=0,ready=1,connecting=2,connected=3,called=4,incomming=5,calling=6, 100=Contacts updated
            //txz 0:disconnect, 1:connect, 2:idle, 3:call, 4:incall, 5:calling
            int cur_hfp = hfp < 3 ? 0 : hfp;
            if (mode_va != null && mode_va.equals("1") && (lastHFP != cur_hfp || ((mac != null && !mac.equals(lastMac)) || (number != null && !number.equals(lastNumber)) || (name != null && !name.equals(lastName))))) {
                Intent it = new Intent("com.akspeech.action.BROADCAST_CMD_FROM_BT");
                it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_HFP_STATUS);
                it.putExtra(MyCmd.EXTRA_COMMON_DATA, cur_hfp);
                if (mac != null && !mac.isEmpty()) {
                    it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mac);
                }
                if (number != null && !number.isEmpty()) {
                    it.putExtra(MyCmd.EXTRA_COMMON_DATA3, number);
                }
                if (name != null && !name.isEmpty()) {
                    it.putExtra(MyCmd.EXTRA_COMMON_DATA4, name);
                }
                it.setPackage("com.ak.speechrecog");
                try {
                    context.sendBroadcast(it);
                } catch (Exception e) {
                    Log.e(TAG, "sendHFPInfoToVA failed: " + e);
                }

                if (hfp != 100) {
                    lastHFP = cur_hfp;
                    lastMac = mac;
                    lastNumber = number;
                    lastName = name;
                    Util.setProperty("ak_btinfo", (mac == null ? "" : mac) + "," + (number == null ? "" : number) + "," + (name == null ? "" : name));
                }
            }
        }
    }

    private void onHFPChanged() {
        sendHFPInfoToVA(this, ATBluetooth.mCurrentHFP, ATBluetooth.mCurrentMac, mCallNumber, mCallName);
    }

    private String findConectName() {
        String s = "";
        //		Log.d("eedc", "findConectName:" + mPairInfo.size()+":"+mSearchInfo.size());
        if (ATBluetooth.mCurrentMac != null) {
            for (PhoneBook pb : mPairInfo) {
                if (pb.mNumber.equals(ATBluetooth.mCurrentMac)) {
                    s = pb.mName;
                    break;
                }
            }

            if (s.length() <= 0) {
                for (PhoneBook pb : mSearchInfo) {
                    if (pb.mNumber.equals(ATBluetooth.mCurrentMac)) {
                        s = pb.mName;
                        break;
                    }
                }
            }
        }
        return s;
    }

    private void sendHFPInfo() {
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_HFP_STATUS);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, ATBluetooth.mCurrentHFP);
        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
            it.putExtra(MyCmd.EXTRA_COMMON_DATA2, findConectName());
        }
        mContext.sendBroadcast(it);


    }

    private int mBattery = 4;
    private int mSignal = 5;

    private void sendHPFBatterySignalInfo() {

        if (GlobalDef.mBTCellStatusBar == 0) {
            return;
        }
        Intent it;

        it = new Intent(MyCmd.BROADCAST_CMD_TO_CAR_SERVICE_BT);

        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_HFP_STATUS);

        int data = ATBluetooth.mCurrentHFP | mBattery << 8 | mSignal << 16;
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, data);
        //		it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mBattery);
        //		it.putExtra(MyCmd.EXTRA_COMMON_DATA3, mSignal);
        it.setPackage(AppConfig.PACKAGE_CAR_SERVICE);
        BroadcastUtil.sendToCarService(this, it);
    }

    private void sendA2DPPlayInfo() {
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_A2DP_STATUS);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, mPlayStatus);

        mContext.sendBroadcast(it);
    }

    private void sendID3Info() {
        if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED) {
            mID3Name = "";
            mID3Artist = "";
            mID3Album = "";
        }
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_ID3_INFO);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, mID3Name);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mID3Artist);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA3, mID3Album);

        Log.d("BtCmd", "sendID3Info" + mID3Name);
        mContext.sendBroadcast(it);

        updateAcitivtyID3(mID3Name, mID3Artist, mID3Album);
    }


    private void sendA2DPTimeInfo() {
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_TIME_STATUS);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, mCurTime);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mTotalTime);
        mContext.sendBroadcast(it);
    }

    private void sendA2DPCurPath(String path) {
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_A2DP_CUR_FOLDER);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, path);

        mContext.sendBroadcast(it);
    }

    private int mReqA2DPListNum;

    private void sendA2DPListInfo(int status) {
        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_A2DP_LIST_INFO);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, status);
        if (status == 0) {
            it.putExtra(MyCmd.EXTRA_COMMON_DATA2, Parrot.mMusicFileInfo);
        } else {
            it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mReqA2DPListNum);
        }

        mContext.sendBroadcast(it);
    }

    private void doCmd(Intent intent) {
        int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
        int data = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);

        switch (cmd) {
            case MyCmd.Cmd.BT_RECEIVE_DATA_KEY: {
                checkParrotA2DPDevice();
                doMusicKeyControl(data);
                break;
            }
            case MyCmd.Cmd.BT_REQUEST_A2DP_INFO: {
                checkParrotA2DPDevice();
                sendA2DPInfo();
                sendID3Info();
                break;
            }
            case MyCmd.Cmd.BT_REQUEST_A2DP_LIST_INFO:
                checkParrotA2DPDevice();
                String s = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA2);
                mATBluetooth.write(data, s);
                break;
            case MyCmd.Cmd.BT_REQUEST_HFP_INFO:
                sendHFPInfo();
                break;
            case MyCmd.Cmd.BT_REQUEST_CALLING_INFO:
                mRequestCallingInfo = data;
                returnCallInfo();
                break;
            case MyCmd.Cmd.BT_REQUEST_MIC_GAIN:

                String data2 = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA);
                mATBluetooth.write(ATBluetooth.REQUEST_MIC_GAIN, data2);
                break;
        }
    }

    private void returnMicGain(int param) {
        Log.d(TAG, "returnMicGain:" + param);

        Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
        it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_MIC_GAIN);
        it.putExtra(MyCmd.EXTRA_COMMON_DATA, param);
        mContext.sendBroadcast(it);

    }

    private int mRequestCallingInfo = 0;

    private void returnCallInfo() {
        Log.d(TAG, "returnCallInfo:" + mRequestCallingInfo + ":" + mCallNumber + ":" + mCallName);
        if (mRequestCallingInfo > 0) {
            Intent it = new Intent(MyCmd.BROADCAST_CMD_FROM_BT);
            it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_SEND_CALLING_INFO);
            it.putExtra(MyCmd.EXTRA_COMMON_DATA, ATBluetooth.mCurrentHFP);
            it.putExtra(MyCmd.EXTRA_COMMON_DATA2, mCallNumber);
            it.putExtra(MyCmd.EXTRA_COMMON_DATA3, mCallName);
            mContext.sendBroadcast(it);
        }
    }

    private void checkParrotA2DPDevice() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            if (Parrot.mMediaIndex == null) {
                mATBluetooth.write(ATBluetooth.REQUEST_CLMS);
            }
        }
    }

    private void sendCanboxInfo(int d0, int d1) {

        byte[] buf = new byte[]{(byte) d0, 0x01, (byte) d1};
        BroadcastUtil.sendCanboxInfo(this, buf);
    }

    private void updateCanbox(byte[] buf) {
        try {
            if ((buf[0] & 0xff) == 0xff) {
                if (buf[1] == 1) {
                    mCanNeedCalllog = true;
                    sendCallLogToCan();
                }
            }
            if (mIncoming == null) {
                return;
            }
            switch (buf[0]) {
                case 0x9:
                    if (buf[2] == 0x2) {
                        mIncoming.showCanbox();
                    } else if (buf[2] == 0x0) {
                        mIncoming.hide();
                    }
                    break;
                case 0x8:
                    String num = "";
                    String s;
                    for (int i = 2; i < buf.length && i < 11; ++i) {
                        s = null;
                        s = getBCD((buf[i] & 0xf0) >> 4);

                        if (s == null) {
                            break;
                        }
                        num += s;
                        s = null;
                        s = getBCD(buf[i] & 0xf);
                        if (s == null) {
                            break;
                        }

                        num += s;
                    }
                    mIncoming.updateCanboxText(num);
                    break;
            }
        } catch (Exception ignored) {

        }
    }

    private String getBCD(int c) {
        String s = null;
        if ((c & 0xf) == 0xF) {
            s = null;
        } else if ((c & 0xf) == 0xa) {
            s = "*";
        } else if ((c & 0xf) == 0xb) {
            s = "#";
        } else {
            s = "" + (c & 0xf);
        }
        return s;
    }

    private boolean mAccPower = true;

    private boolean mCarPlayConnect = false;

    private void checkCarPlayConnect() {
        if (mCarPlayConnect) {
            if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
                mATBluetooth.write(ATBluetooth.REQUEST_DISCONNECT);
                mDisconnectByZlink = ATBluetooth.mCurrentMac;
            }
        }
    }

    private boolean mZlinkPhoneOn = false;

    private void zLinkNotifyHFP(boolean on) {
        Log.d(TAG, "zLinkNotifyHFP:" + on + ":" + ATBluetooth.mCurrentHFP);
        if (ATBluetooth.mCurrentHFP < ATBluetooth.A2DP_INFO_CONNECTED) {
            mZlinkPhoneOn = on;
            if (on) {
                if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED) {
                    BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_CARPLAY_ON);

                    BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_ON);
                }
            } else {
                // String s = Util.getProperty("ak.af.btphone.on");
                if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CONNECTED) {
                    BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_CARPLAY_OFF);

                    BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_OFF);
                }
            }
        }
    }

    private int mHFPZLink = 0;

    private boolean mConnectedBeforeAccPowerOff = false;

    public void unregisterListener() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

    }

    private String mDisconnectByZlink;
    private long mLockClickTime = 0;
    private final static int KEY_LOCK_TIME = 1000;
    private int mCurParrotLang = 0;

    private void updateLanguage(int cur_lang) {
        int lang = 3;
        String locale = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();

        // Log.d("ff", locale + ":" + country);
        mCurParrotLang = cur_lang;
        if (locale != null) {

            if (locale.equals("cs")) {
                lang = 0;
            } else if (locale.equals("da")) {
                lang = 1;
            } else if (locale.equals("nl")) {
                lang = 2;
            } else if (locale.equals("en")) {
                //				if (country.equals("AU")) {
                //					lang = 5;
                //				} else if (country.equals("GB")) {
                //					lang = 4;
                //				} else {
                lang = 3;
                //				}
            } else if (locale.equals("fr")) {
                if (country.equals("CA")) {
                    lang = 7;
                } else {
                    lang = 6;
                }
            } else if (locale.equals("de")) {
                lang = 8;
            } else if (locale.equals("it")) {
                lang = 9;
            } else if (locale.equals("ko")) {
                lang = 10;
            } else if (locale.equals("zh")) {
                lang = 3;
            } else if (locale.equals("pl")) {
                lang = 12;
            } else if (locale.equals("pt")) {
                if (country.equals("BR")) {
                    lang = 13;
                } else {
                    lang = 14;
                }
            } else if (locale.equals("ru")) {
                lang = 15;
            } else if (locale.equals("es")) {
                lang = 16;
            } else if (locale.equals("tr")) {
                lang = 18;
            } else if (locale.equals("sv")) {
                lang = 19;
            } else if (locale.equals("jp")) {
                lang = 20;
            } else if (locale.equals("fa")) {
                lang = 21;
            } else if (locale.equals("th")) {
                lang = 22;
            }
        }

        Log.d(TAG, lang + ":initDVDLang:" + cur_lang + "::" + mCheckLangTime);
        if (lang != cur_lang) {
            mATBluetooth.write(ATBluetooth.SET_LANGUAGE, "" + lang);
        } else {
            mHandler.removeMessages(MSG_CHECK_LANGUAGE);
        }
    }

    private int mCheckLangTime = 0;

    private void checkParrotLanguage() {
        mATBluetooth.write(ATBluetooth.REQUEST_LANGUAGE);
        if (mCheckLangTime < 50) {
            mHandler.removeMessages(MSG_CHECK_LANGUAGE);
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_LANGUAGE, 3000);
            ++mCheckLangTime;
        }
    }


    private void initCurrenAndPhoneBook(String mac) {
        Log.d("ffc", "initCurrenAndPhoneBook");
        ATBluetooth.mCurrentMac = mac;
        saveData(SAVE_CONNECT_PHONE, ATBluetooth.mCurrentMac);
        SaveData.getPhoneBookData(mPhoneBookInfo, ATBluetooth.mCurrentMac);
        SaveData.getPhoneBookSimData(mPhoneBookSimInfo, ATBluetooth.mCurrentMac);
        if (mContactsUtils != null) {
            mContactsUtils.notifyWacTalk(ATBluetooth.mCurrentMac);
        }
        autoDownloadCallLog();
        sendCallLogToCan();
    }

    // for parrot
    public static void showMicButton(boolean show) {
        if (mContext != null) {
            if (mContext.mVoicRecognitionView != null) {
                if (show) {

                    mContext.mVoicRecognitionView.showMicButton();
                } else {

                    mContext.mVoicRecognitionView.hideMicButton();
                }
            }
        }
    }


    public static boolean showSpeechVoiceActivity(boolean force) {
        if (mContext != null) {
            mContext.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    showSpeechVoiceActivityEx(false);
                    if (!showSpeechVoiceActivityEx(false)) {
                        Toast.makeText(mContext, R.string.voicecontrol_notice, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        return false;
    }

    public static boolean showSpeechVoiceActivityEx(boolean force) {
        if (mContext != null) {
            if (mContext.mVoicRecognitionView != null) {
                if (mPhoneBookInfo.size() > 0) {
                    force = true;
                }
                return mContext.mVoicRecognitionView.showSpeech(force);

            }
        }
        return false;
    }

    public static boolean showSpeech(boolean force) {

        if (mContext != null) {
            if (ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) {
                return false;
            }

            if (mContext.mVoicRecognitionView != null) {
                return mContext.mVoicRecognitionView.showSpeech(force);
            }
        }
        return false;
    }

    private void initSpeech() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            mVoicRecognitionView = VoicRecognitionView.getInstance(this);
        }
    }

    private AlertDialog mAlertDialogUpdate = null;

    private void showUpdateUI(int param, String info) {
        if (mAlertDialogUpdate == null) {
            mAlertDialogUpdate = new AlertDialog.Builder(mContext).setTitle("update bt").create();

            mAlertDialogUpdate.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }

        mAlertDialogUpdate.setMessage(info);
        if (!mAlertDialogUpdate.isShowing()) {
            mAlertDialogUpdate.show();
        }
    }

    private void doUpdateModeule(String file) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
            }
        });
        mATBluetooth.write(ATBluetooth.REQUEST_UPDATE_START, file);
    }

    String mUpdatefile;

    private void updateModule() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {

            mUpdatefile = getUpdateFilePath("goc_update.dfu");

            if (mUpdatefile != null) {
                AlertDialog d = new AlertDialog.Builder(mContext).setMessage(mUpdatefile).setTitle(mContext.getString(R.string.bt_update)).setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        UpdateBtTask updateBtTask = new UpdateBtTask();
                        updateBtTask.execute();
                    }
                }).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                }).create();

                d.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

                d.show();
            } else {
                Toast.makeText(mContext, "Update File Not Found!", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(mContext, "Update File Not Support in this BT module!", Toast.LENGTH_LONG).show();
        }
    }

    private String getUpdateFilePath(String file) {
        List<StorageInfo> list = listAllStorage(mContext);

        for (int i = 0; i < list.size(); ++i) {


            try {
                StorageInfo si = list.get(i);
                if (si.mType == StorageInfo.TYPE_SD || si.mType == StorageInfo.TYPE_USB) {

                    File root = new File(si.mPath);
                    File[] files = root.listFiles();
                    for (File f : files) {
                        if (!f.isDirectory()) {
                            if (f.getPath().endsWith(file)) {
                                return f.getPath();
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
        return null;
    }

    private static List<StorageInfo> listAllStorage(Context context) {
        ArrayList<StorageInfo> storages = new ArrayList<StorageInfo>();
        if (Build.VERSION.SDK_INT > 23) {

            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            try {
                Class<?>[] paramClasses = {};
                Method getVolumeList = StorageManager.class.getMethod("getVolumes", paramClasses);
                Object[] params = {};
                List<Object> VolumeInfo = (List<Object>) getVolumeList.invoke(storageManager, params);

                if (VolumeInfo != null) {
                    for (Object volumeinfo : VolumeInfo) {

                        Method getPath = volumeinfo.getClass().getMethod("getPath", new Class[0]);

                        File path = (File) getPath.invoke(volumeinfo, new Object[0]);

                        Method getDisk = volumeinfo.getClass().getMethod("getDisk", new Class[0]);

                        Object diskinfo = getDisk.invoke(volumeinfo, new Object[0]);
                        int type = StorageInfo.TYPE_INTERAL;
                        if (diskinfo != null) {
                            Method isSd = diskinfo.getClass().getMethod("isSd", new Class[0]);

                            type = ((Boolean) isSd.invoke(diskinfo, new Object[0])) ? StorageInfo.TYPE_SD : StorageInfo.TYPE_USB;

                        }
                        StorageInfo si = new StorageInfo(path.toString(), type);
                        storages.add(si);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            storages.trimToSize();
        } else {
            String[] path = {
                    "/storage/usbdisk1/", "/storage/usbdisk2/", "/storage/usbdisk3/", "/storage/usbdisk4/", "/storage/sdcard1/", "/storage/sdcard2/"
            };
            for (String s : path) {
                File sdcardDir = new File(s);
                String state = Environment.getExternalStorageState(sdcardDir);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    StorageInfo si = new StorageInfo(s, 0);
                    storages.add(si);
                }
            }

        }
        return storages;
    }

    public static class StorageInfo {
        public String mPath;
        public String mState;
        public int mType;

        public final static int TYPE_INTERAL = 0;
        public final static int TYPE_SD = 1;
        public final static int TYPE_USB = 2;


        public StorageInfo(String path, int type) {
            mPath = path;
            mType = type;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class UpdateBtTask extends AsyncTask<Void, Integer, Integer> {

        UpdateBtTask() {
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(Void... params) {
            doUpdateModeule(mUpdatefile);
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    ///3call
    private void do3CallCome(int status, int index, String num) {

        if (!mIncoming.isUISupport3Call()) {
            ATBluetooth.mCallingNum++;
            if (ATBluetooth.mCallingNum > 1) {
                String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);
                String text = num;
                if (name != null) {
                    text = name + " " + num;
                }
                if (!text.equals(mIncoming.getNumber())) {
                    mIncoming.setNumber(text);
                    ATBluetooth.m3CallNumber = num;
                }
            }
        } else {
            if (index == 2) {
                ATBluetooth.mCallingNum = 2;
                mIncoming.m3CallLayout.setVisibility(View.VISIBLE);
                String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);
                String text = num;
                if (name != null) {
                    text = name + " " + num;
                }
                mIncoming.m3CallTextView.setText(text);
            } else {
                ATBluetooth.mCallingNum = 1;
                mIncoming.m3CallLayout.setVisibility(View.GONE);
            }
        }
        if (index == 2) {
            ATBluetooth.m3CallStatus = status;
            ATBluetooth.m3CallNumber = num;
        }

        Log.d("abcde", ":do3CallCome:" + status + ":" + index);
        if (status > ATBluetooth.HFP_INFO_CONNECTED) {
            ATBluetooth.m3CallActiveIndex = index;
        }
    }

    private boolean m3CallChangeName = false;

    private void do3CallEnd(int status, int index, String num) {

        if (!mIncoming.isUISupport3Call()) {
            ATBluetooth.mCallingNum--;
            if (ATBluetooth.m3CallNumber != null && ATBluetooth.m3CallNumber.equals(num)) {
                String text = mCallNumber;
                if (mCallName != null) {
                    text = mCallName + " " + mCallNumber;
                }
                if (!text.equals(mIncoming.getNumber())) {
                    mIncoming.setNumber(text);
                    setActivityNumberText(text);
                }
            }
        } else {
            if (ATBluetooth.mCallingNum > 0) {
                ATBluetooth.mCallingNum--;
                ATBluetooth.m3CallStatus = 0;
            }

            mIncoming.m3CallLayout.setVisibility(View.GONE);

            if (index == 1) {
                mIncoming.mIncomingText.setText(mIncoming.m3CallTextView.getText());
                mIncoming.mTextViewTime.setText(mIncoming.m3CallStatus.getText());
                ATBluetooth.mTime = ATBluetooth.mTime2;

                mCallNumber = ATBluetooth.m3CallNumber;

                mCallName = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, ATBluetooth.m3CallNumber);

            }
            mIncoming.m3CallTextView.setText("");
            mIncoming.m3CallStatus.setText("");
        }
    }

    private void do3CallStatus(int status, int index, String num) {
        if (status == 0) {
            String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);
            String text = num;
            if (name != null) {
                text = name + " " + num;
            }
            if (!text.equals(mIncoming.getNumber())) {
                if (!mIncoming.isUISupport3Call()) {
                    mIncoming.setNumber(text);
                }
            }
            if (index == 2) {
                ATBluetooth.m3CallStatus = ATBluetooth.HFP_INFO_CALLING;
                ATBluetooth.m3CallNumber = num;
                ATBluetooth.mCallingNum = 2;
            }
            ATBluetooth.m3CallActiveIndex = index;
        }
    }

    private void setActivityNumberText(String s) {
    }

    private void updateAcitivtyID3(String name, String artist, String album) {
        if (ATBluetoothActivity.mThis != null && ATBluetoothActivity.mThis.mDigit != null) {
            ATBluetoothActivity.mThis.updateAcitivtyID3(name, artist, album);
        }
    }

    private void setCustomDefConfig() {
        if (getData(SAVE_FIRST_BOOT_CONFIG) != 1) {

            saveData(SAVE_FIRST_BOOT_CONFIG, 1);

            if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(ResourceUtil.mSystemUI)) {

                mATBluetooth.write(ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE);
                mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE);
            }
        }

        if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI)) {
            mOBDId = 0xabcd;
        }
    }

    private void updateA2dpInsideStatus(int status) {
        Log.d("fkkk", ATBluetoothActivity.isA2DPshow() + ":" + GlobalDef.isA2DPSource() + ":" + status);
        if (ATBluetoothActivity.isA2DPshow() && GlobalDef.isA2DPSource()) {
            if (status >= ATBluetooth.A2DP_INFO_CONNECTED) {
                ATBluetoothActivity.showA2DPByConnect();
                mATBluetooth.write(ATBluetooth.REQUEST_A2DP_PLAY);
            }
        }
    }

    private WakeLock mWakeLock;

    @SuppressLint("InvalidWakeLockTag")
    private void wakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            mWakeLock.acquire();
        }
    }

    private void wakeRelease() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private long mTimeAddParrotCallOutPhone = 0; //just for parrot

    @SuppressLint("SimpleDateFormat")
    private String getTime() {
        String s;
        s = "yyyy/MM/dd";

        long time = System.currentTimeMillis();
        Date d1 = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat(s);
        String t1 = format.format(d1);
        format = new SimpleDateFormat("HH:mm:ss");
        String t2 = format.format(d1);
        s = t1 + "\r\n" + t2;
        return s;
    }

    private String mPreParrotCallName = null;
    //
    private long mFirstConnectCheckPlay = 0;

    private String mOBDMac = null;//for goc

    public static void hideIncoming() {
        if (mContext != null) {
            if (mContext.mIncoming != null) {
                mContext.mIncoming.hide();
            }
        }
    }

    public static int GetAutoDownCalllogType() {
        if (mContext != null) {
            return mContext.mDownCallLogType;
        }
        return -1;
    }

    private int mDownCallLogType = 0;
    private boolean mDownCallLogSuccess = false;
    public final static int DOWN_CALLLOG_ANSWER = 1;
    public final static int DOWN_CALLLOG_OUT = 2;
    public final static int DOWN_CALLLOG_MISS = 3;

    //auto download calllog
    private void autoDownloadCallLog() {
        if (ATBluetooth.mAutoDownLoadCallLog) {
            if (mDownCallLogType < DOWN_CALLLOG_MISS) {
                ++mDownCallLogType;
                int cmd = 0;
                switch (mDownCallLogType) {
                    case DOWN_CALLLOG_OUT:
                        cmd = ATBluetooth.REQUEST_CALL_LOG_OUT;
                        break;
                    case DOWN_CALLLOG_MISS:
                        cmd = ATBluetooth.REQUEST_CALL_LOG_MISS;
                        break;
                    case DOWN_CALLLOG_ANSWER:
                        cmd = ATBluetooth.REQUEST_CALL_LOG_ANSWER;
                        break;
                }
                mATBluetooth.write(cmd);
                checkCallLogSync();
            }  //test
        }
    }

    private void updateCallLog(String s) {
        mDownCallLogSuccess = true;
        ArrayList<PhoneBook> al = null;
        switch (mDownCallLogType) {
            case DOWN_CALLLOG_OUT:
                al = mCalllogOInfo;
                break;
            case DOWN_CALLLOG_MISS:
                al = mCalllogMInfo;
                break;
            case DOWN_CALLLOG_ANSWER:
                al = mCalllogRInfo;
                break;
        }

        String[] ss = s.split(";");

        String time = "";
        if (ss.length >= 3) {
            time = getTime(ss[2]);
        }


        String name = "";
        String num = "";
        if (ss.length >= 2) {
            name = ss[0];
            num = ss[1];
        } else {
            num = ss[0];
        }

        if (al != null) {
            al.add(al.size(), new PhoneBook(num, name, time));
        }
    }

    private void stopCallLogSync() {
        mHandler.removeMessages(MSG_CHECK_CALLLOG_SYNC);
        mDownCallLogType = 0;
    }

    private void checkCallLogSync() {
        mHandler.removeMessages(MSG_CHECK_CALLLOG_SYNC);
        mHandler.sendEmptyMessageDelayed(MSG_CHECK_CALLLOG_SYNC, 2200);
    }

    private String getTime(String t) {
        String ret = "";
        String[] ss = t.split("T");
        try {
            int y = Integer.parseInt(ss[0].substring(0, 4));
            int m = Integer.parseInt(ss[0].substring(4, 6));
            int d = Integer.parseInt(ss[0].substring(6, 8));
            int h = Integer.parseInt(ss[1].substring(0, 2));
            int min = Integer.parseInt(ss[1].substring(2, 4));
            int s = Integer.parseInt(ss[1].substring(4, 6));
            ret = y + "/" + m + "/" + d + "\r\n" + h + "/" + min + "/" + s;
        } catch (Exception ignored) {
        }
        return ret;
    }

    private boolean mCanNeedCalllog = false;

    private void sendCallLogToCan() {
        if (mCanNeedCalllog) {
            // find 5 new calllog

            if (mCalllogMInfo.size() == 0) {
                SaveData.getPhoneBookDataEx(mCalllogMInfo, ATBluetooth.mCurrentMac, SaveData.TAG_LOG_MISS);
            }
            if (mCalllogOInfo.size() == 0) {
                // mCalllogOInfo.clear();
                SaveData.getPhoneBookDataEx(mCalllogOInfo, ATBluetooth.mCurrentMac, SaveData.TAG_LOG_OUT);
            }
            if (mCalllogRInfo.size() == 0) {
                SaveData.getPhoneBookDataEx(mCalllogRInfo, ATBluetooth.mCurrentMac, SaveData.TAG_LOG_RECEIVE);
            }

            int iMiss = 0;
            int iOut = 0;
            int iAnswer = 0;
            int type = 0;

            PhoneBook pb;

            PhoneBook pbMiss;
            PhoneBook pbOut;
            PhoneBook pbAnswer;

            ArrayList<String> list = new ArrayList<String>();

            for (int i = 0; i < 5; ++i) {
                pbMiss = null;
                pbOut = null;
                pbAnswer = null;
                pb = null;
                type = 0;

                if (iMiss < mCalllogMInfo.size()) {
                    pbMiss = mCalllogMInfo.get(iMiss);
                }

                if (iOut < mCalllogOInfo.size()) {
                    pbOut = mCalllogOInfo.get(iOut);
                }

                if (iAnswer < mCalllogRInfo.size()) {
                    pbAnswer = mCalllogRInfo.get(iAnswer);
                }

                if (pbMiss != null || pbOut != null || pbAnswer != null) {
                    pb = findLatestPB(pbMiss, pbOut);
                    pb = findLatestPB(pb, pbAnswer);
                    if (pb == pbMiss) {
                        ++iMiss;
                        type = DOWN_CALLLOG_MISS;
                    } else if (pb == pbOut) {
                        ++iOut;
                        type = DOWN_CALLLOG_OUT;
                    } else {
                        type = DOWN_CALLLOG_ANSWER;
                        ++iAnswer;
                    }
                    if (pb != null) {
                        String s = type + ";" + pb.mNumber;
                        list.add(list.size(), s);
                    }
                } else {
                    break;
                }
            }
            Log.d("ccc", "" + list.size());
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); ++i) {
                    Log.d("ccc", "" + list.get(i));
                }

                Intent it;

                it = new Intent(MyCmd.BROADCAST_CMD_TO_CAR_SERVICE_BT);

                it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_PHONE_CALLLOG_LIST);
                it.putExtra(MyCmd.EXTRA_COMMON_OBJECT, list);
                it.setPackage(AppConfig.PACKAGE_CAR_SERVICE);
                BroadcastUtil.sendToCarService(this, it);
            }
        }
    }

    private PhoneBook findLatestPB(PhoneBook a, PhoneBook b) {
        if (a != null && b != null) {
            if (GlobalDef.isTimeLate(a.mPinyin, b.mPinyin)) {
                return a;
            } else {
                return b;
            }
        } else if (a != null) {
            return a;
        } else {
            return b;
        }
    }

    public static void syncCallLog() {
        if (mContext != null && !mContext.mDownCallLogSuccess) {
            mContext.mDownCallLogType = 0;
            mContext.autoDownloadCallLog();
        }
    }

    private void initBTMicGain() {
        String s = MachineConfig.getPropertyOnce(MachineConfig.KEY_BT_MIC_GAIN);
        if (s != null) {
            mATBluetooth.write(ATBluetooth.REQUEST_MIC_GAIN, s);
        }
    }


    private BroadcastReceiver mReceiver = null;

    public void registerListener() {
        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    switch (Objects.requireNonNull(action)) {
                        case MyCmd.BROADCAST_CMD_TO_BT:
                        case MyCmd.BROADCAST_CMD_LAUNCHER_TO_BT:
                            doCmd(intent);
                            break;
                        case MyCmd.ACTION_KEY_PRESS:

                            int code = intent.getIntExtra(MyCmd.EXTRAS_KEY_CODE, 0);

                            doPhoneKeyControl(code);
                            break;
                        case MyCmd.BROADCAST_CAR_SERVICE_SEND:
                            int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);

                            switch (cmd) {
                                case MyCmd.Cmd.SOURCE_CHANGE:
                                case MyCmd.Cmd.RETURN_CURRENT_SOURCE:
                                    int source = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);
                                    if (source == MyCmd.SOURCE_BT_MUSIC) {
                                        if (GlobalDef.getSource() != MyCmd.SOURCE_BT_MUSIC) {
                                            sendID3Info();
                                        }
                                        mATBluetooth.write(ATBluetooth.REQUEST_BT_MUSIC_UNMUTE);
                                    } else {
                                        mATBluetooth.write(ATBluetooth.REQUEST_BT_MUSIC_MUTE);
                                    }

                                    GlobalDef.setSource(source);
                                    break;
                            }

                            break;
                        case MyCmd.BROADCAST_MACHINECONFIG_UPDATE: {
                            String s = intent.getStringExtra(MyCmd.EXTRA_COMMON_CMD);
                            if (MachineConfig.KEY_SCREEN1_VIEW.equals(s)) {
                                if (mIncoming != null) {
                                    mIncoming.release();
                                    mIncoming = null;
                                }
                                mIncoming = new IncomingView();
                            } else if (MachineConfig.KEY_APP_HIDE.equals(s)) {
                                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                    if (mVoicRecognitionView.isHideMic()) {
                                        mVoicRecognitionView.hideMicButton();
                                    }
                                }
                            } else if (SystemConfig.KEY_LAUNCHER_UI_RM10.equals(s)) {
                                if (ATBluetoothActivity.mThis2 != null) {
                                    ATBluetoothActivity.mThis2.finish();
                                }
                            } else if (SystemConfig.KEY_BT_CELL.equals(s)) {
                                GlobalDef.mBTCellStatusBar = SystemConfig.getIntProperty(context, SystemConfig.KEY_BT_CELL);
                                if (GlobalDef.mBTCellStatusBar == 1) {
                                    sendHPFBatterySignalInfo();
                                    notifyManager(0);
                                } else {
                                    notifyManager(ATBluetooth.mCurrentHFP);
                                }
                            } else if (MachineConfig.KEY_TOUCH3_IDENTIFY.equals(s)) {
                                GlobalDef.getTouch3ConfigValue();
                            }
                            break;
                        }
                        case MyCmd.BROADCAST_REAL_POWER_OFF:
                            if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTING) {
                                if (ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) {

                                    BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_OFF);
                                }
                                if (ATBluetooth.mBTType != MachineConfig.VAULE_BT_TYPE_PARROT) {
                                    mATBluetooth.write(ATBluetooth.REQUEST_DISCONNECT);
                                }
                            }

                            break;
                        case MyCmd.BROADCAST_SEND_FROM_CAN:

                            byte[] buf = intent.getByteArrayExtra("buf");
                            if (buf != null) {
                                updateCanbox(buf);
                            } else { //for auto test
                                BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.AUTO_TEST_RESULT, MyCmd.SOURCE_BT, (mATBluetooth.mBtName != null && mATBluetooth.mBtName.length() > 0) ? 0 : 1);
                            }
                            break;
                        case Intent.ACTION_LOCALE_CHANGED:
                            mCheckLangTime = 0;
                            checkParrotLanguage();
                            initSpeech();
                            ResourceUtil.updateAppUi(context);
                            break;
                        case Intent.ACTION_CONFIGURATION_CHANGED:
                            ResourceUtil.updateAppUi(context);
                            Log.d(TAG, "Intent.ACTION_CONFIGURATION_CHANGED" + ATBluetoothActivity.mThis2);
                            if (ATBluetoothActivity.mThis2 != null) {
                                ATBluetoothActivity.updateBySystemConfig();
                            }
                            // updateLanguage(0);
                            break;
                        case MyCmd.BROADCAST_ACTIVITY_STATUS: {
                            String s = intent.getStringExtra(MyCmd.EXTRA_COMMON_CMD);

                            if (mVoicRecognitionView != null && s != null) {
                                if (s.startsWith("com.android.launcher")/*
									|| s.startsWith("com.my.bt")*/) {
                                    if (mPhoneBookInfo.size() > 0) {
                                        mVoicRecognitionView.showMicButton();
                                    }
                                } else {
                                    mVoicRecognitionView.hideMicButton();
                                }
                            }
                            break;
                        }
                        case MyCmd.BROADCAST_BT_UPDATE:
                            updateModule();
                            break;
                        case MyCmd.BROADCAST_BT_VERSION:
                            mRequestVersion = true;
                            mATBluetooth.write(ATBluetooth.REQUEST_REQUEST_VERSION);
                            break;
                        case MyCmd.BROADCAST_ACC_DELAY_POWER_OFF:
                            mAccPower = false;
                            setCarPlayerStatus(false);
                            if (mPlayStatus == ATBluetooth.A2DP_INFO_PLAY) {
                                doMusicPause();
                            }

                            if (DEBUG) {
                                Log.d(TAG, "BROADCAST_ACC_DELAY_POWER_OFF");
                            }
                            if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTING) {
                                if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {

                                    if (ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) {
                                        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.PHONE_OFF);
                                        if (mIncoming != null) {
                                            mIncoming.hide();
                                        }
                                        mCallName = null;
                                        mCallNumber = null;
                                    }

                                    if (mATBluetooth.getVoiceSwitchLocal()) {
                                        if (ATBluetooth.mBTType != MachineConfig.VAULE_BT_TYPE_IVT) {
                                            mATBluetooth.write(ATBluetooth.REQUEST_VOICE_SWITCH_REMOTE);
                                        }
                                    }

                                    mPlayStatus = 0;
                                    mID3Name = "";
                                    mID3Artist = "";
                                    mID3Album = "";
                                    sendA2DPInfo();
                                    sendID3Info();
                                }
                                mConnectedBeforeAccPowerOff = true;
                                notifyManager(0);
                            }

                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                                if (Util.isRKSystem()) {//car service do
                                    mHandler.removeMessages(MSG_START_IVT_BLUELETD);
                                    mHandler.removeMessages(MSG_STOP_IVT_BLUELETD);
                                    mHandler.sendEmptyMessageDelayed(MSG_STOP_IVT_BLUELETD, 500);
                                }
                            } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                Util.doSleep(10);
                                Util.setFileValue("/sys/class/ak/source/bluetoothsw", 0);
                            } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                                mATBluetooth.write(ATBluetooth.STOP_MODULE);
                            }
                            ATBluetooth.mCurrentHFP = 0;
                            onHFPChanged();
                            break;
                        case MyCmd.BROADCAST_ACC_DELAY_POWER_ON:
                            mAccPower = true;
                            if (DEBUG) {
                                Log.d(TAG, "BROADCAST_ACC_DELAY_POWER_ON");
                            }
                            if (mConnectedBeforeAccPowerOff) {
                                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                                    if (Util.isRKSystem()) {
                                    } else {
                                        mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_ADDR);
                                    }

                                }
                                mConnectedBeforeAccPowerOff = false;
                            }

                            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                                if (Util.isRKSystem()) {
                                    if (IVT_KILL_BT_IF_SLEEP) {
                                        Util.setProperty("ctl.start", "ivt_blueletd");
                                        mHandler.sendEmptyMessageDelayed(MSG_START_IVT_BLUELETD, 10);
                                        mHandler.sendEmptyMessageDelayed(MSG_REBOOT_BT_CORE, 1500);
                                    } else {

                                        mATBluetooth.closeIvtModule(false);
                                        mATBluetooth.write(ATBluetooth.START_MODULE);
                                        mHandler.sendEmptyMessageDelayed(MSG_REBOOT_BT_CORE_WITHOUTBTINSIDE, 1900); //test
                                        mHandler.sendEmptyMessageDelayed(MSG_INIT_CMD, 2000);
                                    }
                                    mATBluetooth.startcheckIVTErr();
                                }
                            } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                //mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_INDEX, "0");
                                Util.setFileValue("/sys/class/ak/source/bluetoothsw", 1);
                                Util.doSleep(1500);
                                initCmd();
                            } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                                //mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_INDEX, "0");
                                mATBluetooth.write(ATBluetooth.START_MODULE);
                                Util.doSleep(1500);
                                initCmd();
                            }
                            break;
                        case ZLINK_BROAST:
                            String status = intent.getStringExtra("status");
                            String command = intent.getStringExtra("command");
                            //						String phoneMode2 = intent.getStringExtra("phoneMode");
                            Log.d("ffkk", status + ":33:" + command + ":");
                            if ("CONNECTED".equals(status)) {
                                Util.setProperty("car_play_connect", "1");
                                String phoneMode = intent.getStringExtra("phoneMode");
                                if ("carplay_wired".equals(phoneMode) || "carplay_wireless".equals(phoneMode)) {
                                    setCarPlayerStatus(true);
                                    if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
                                        mATBluetooth.write(ATBluetooth.REQUEST_DISCONNECT);
                                        mDisconnectByZlink = ATBluetooth.mCurrentMac;
                                    }
                                    //								mATBluetooth.write(ATBluetooth.STOP_MODULE);
                                } else {
                                    setCarPlayerStatus(false);
                                    mDisconnectByZlink = null;
                                }

                            } else if ("DISCONNECT".equals(status)) {
                                if (mZlinkPhoneOn) {
                                    zLinkNotifyHFP(false);
                                }
                                Util.setProperty("car_play_connect", "0");
                                setCarPlayerStatus(false);
                                if (mDisconnectByZlink != null) {
                                    if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                        int index = -1; // find pair index
                                        for (int i = 0; i < mPairInfo.size(); ++i) {
                                            if (mPairInfo.get(i).mNumber.equals(mDisconnectByZlink)) {
                                                index = mPairInfo.get(i).mIndex;
                                                break;
                                            }
                                        }

                                        if (index != -1) {
                                            mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_INDEX, (index) + "");
                                        }

                                    } else {
                                        if (mAccPower) {
                                            mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_ADDR, mDisconnectByZlink);
                                        }
                                    }

                                    mDisconnectByZlink = null;
                                    zLinkNotifyHFP(false);
                                }
                            } else if ("PHONE_CALL_ON".equals(status)) {
                                zLinkNotifyHFP(true);
                            } else if ("PHONE_CALL_OFF".equals(status)) {
                                zLinkNotifyHFP(false);
                            }
                            break;
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(MyCmd.BROADCAST_CMD_TO_BT);
            iFilter.addAction(MyCmd.BROADCAST_CMD_LAUNCHER_TO_BT);
            iFilter.addAction(MyCmd.ACTION_KEY_PRESS);

            iFilter.addAction(MyCmd.BROADCAST_CAR_SERVICE_SEND);
            iFilter.addAction(MyCmd.BROADCAST_MACHINECONFIG_UPDATE);
            iFilter.addAction(MyCmd.BROADCAST_REAL_POWER_OFF);
            iFilter.addAction(MyCmd.BROADCAST_ACC_DELAY_POWER_OFF);
            iFilter.addAction(MyCmd.BROADCAST_ACC_DELAY_POWER_ON);
            iFilter.addAction(MyCmd.BROADCAST_SEND_FROM_CAN);
            iFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
            iFilter.addAction(MyCmd.BROADCAST_BT_UPDATE);
            iFilter.addAction(MyCmd.BROADCAST_BT_VERSION);
            iFilter.addAction(ZLINK_BROAST);

            iFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                iFilter.addAction(MyCmd.BROADCAST_ACTIVITY_STATUS);
            }

            registerReceiver(mReceiver, iFilter);
        }
    }


    private static final String SAVE_DATA = "ATBluetoothService";
    private static final String SAVE_FIRST_BOOT = "first_boot";
    private static final String SAVE_FIRST_BOOT_PASSWD = "passwd";
    private static final String SAVE_FIRST_BOOT_CONFIG = "first_boot_config";
    private static final String SAVE_CONNECT_PHONE = "reconnect_phone";
    private static final String SAVE_GOC_OBD_MAC = "goc_obd_mac";

    private void saveData(String s, long v) {
        SharedPreferences.Editor shareData = getSharedPreferences(SAVE_DATA, 0).edit();
        shareData.putLong(s, v);
        shareData.apply();
    }

    private long getData(String s) {
        SharedPreferences shareData = getSharedPreferences(SAVE_DATA, 0);
        return shareData.getLong(s, 0);
    }

    private void saveData(String s, String v) {
        SharedPreferences.Editor shareData = getSharedPreferences(SAVE_DATA, 0).edit();
        shareData.putString(s, v);
        shareData.apply();
    }

    private String getDataString(String s) {
        SharedPreferences shareData = getSharedPreferences(SAVE_DATA, 0);
        return shareData.getString(s, null);
    }
}
