package com.zhuchao.android.bt.hw;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.common.util.MachineConfig;
import com.common.util.Util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ATBluetooth {
    private static final String TAG = "ATBluetooth";
    private static final boolean DBG = false;


    static {
        System.loadLibrary("bt");
    }

    public static boolean mAutoDownLoadCallLog = false;
    public static int mBTType = MachineConfig.VAULE_BT_TYPE_IVT;
    public static int mBTTypeOriginal = MachineConfig.VAULE_BT_TYPE_IVT;
    public static boolean mSuppotAutoAnswer = true;
    public static boolean mSuppotEditPin = true;

    public static int mIVTI145 = 0;
    public static String m3CallNumber;
    public static int mCallingNum = 0;
    public static int m3CallActiveIndex = 0;
    public static int m3CallStatus = 0;

    private static int getCurrenIVTBt() {
        int ivt_type = 0;
        String str = null;
        try {
            Process psProcess = Runtime.getRuntime().exec("ls -l /oem/lib");

            psProcess.waitFor();

            InputStream inputStream = psProcess.getInputStream();
            InputStreamReader buInputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(buInputStreamReader);

            for (int i = 0; i < 10; ++i) {
                str = bufferedReader.readLine();
                if (str == null) {
                    break;
                }
                if (str.contains("blueletd -> ./i140/blueletd")) {
                    ivt_type = 0;
                    break;
                } else if (str.contains("blueletd -> ./i145/blueletd")) {
                    ivt_type = 1;
                    break;
                }
            }
            inputStream.close();
            buInputStreamReader.close();
            bufferedReader.close();
            Log.d(TAG, "ivt_type:" + ivt_type);
        } catch (Exception e) {

        }
        return ivt_type;
    }

    public static boolean isSupport3Call() {

        //		if (MachineConfig.VALUE_SYSTEM_UI35_KLD813
        //						.equals(ResourceUtil.mSystemUI)) {
        //			return true;
        //		}

        if (mBTType == MachineConfig.VAULE_BT_TYPE_GOC_8761 || mBTType == MachineConfig.VAULE_BT_TYPE_GOC || mBTType == MachineConfig.VAULE_BT_TYPE_GOC_RF210) {
            return true;
        }
        return false;

        //		return true;
    }

    public static boolean mShowPin = true;

    private static void initBTType() {
        //mBTType = MachineConfig.VAULE_BT_TYPE_PARROT;//test
        String s = MachineConfig.getProperty(MachineConfig.KEY_BT_TYPE);
        if (s == null) {
            s = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_BT_TYPE);
        }
        if (s != null) {
            try {
                mBTType = Integer.valueOf(s);
                mBTTypeOriginal = mBTType = mBTType & 0xff;
            } catch (Exception e) {

            }

            //			if(mBTType == MachineConfig.VAULE_BT_TYPE_PARROT){
            //				Util.sudoExec("chmod:666:/dev/ttySAC2");
            //			}
            //			if (mBTType < TYPE_IVT || TYPE_IVT > TYPE_PARROT) {
            //				mBTType = TYPE_IVT;
            //			}
        }

        if (mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            mShowPin = false;
        }

        if (mBTType == MachineConfig.VAULE_BT_TYPE_GOC_RF210 || mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            s = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_BT_AUTO_DOWNLOAD_CALLLOG);
            if (MachineConfig.VALUE_ON.equals(s)) {
                mAutoDownLoadCallLog = true;
            }
        }

        if (mBTType == MachineConfig.VAULE_BT_TYPE_GOC_8761 || mBTType == MachineConfig.VAULE_BT_TYPE_GOC_RF210) {
            mBTType = MachineConfig.VAULE_BT_TYPE_GOC;
        }

        if (mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            mIVTI145 = getCurrenIVTBt();
        }


        //		mBTType = MachineConfig.VAULE_BT_TYPE_GOC;//test
    }

    public ATBluetooth() {
        if (mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            mBtCmd = new IVT();
        } else if (mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            mBtCmd = new GOC();
        } else if (mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            mBtCmd = new Parrot();
            mSuppotAutoAnswer = false;
            mSuppotEditPin = false;
        } else {
            mBtCmd = new IVT();
        }
        mBtCmd.setCallback(mCallBack);
    }

    public boolean getVoiceSwitchLocal() {
        return mBtCmd.mVoiceSwitchLocal;
    }

    public boolean getMicMute() {
        return mBtCmd.mMicMute;
    }

    public void setVoiceSwitchLocal(boolean b) {
        mBtCmd.mMicMute = b;
    }

    public static int mTime;
    public static int mTime2;

    public static String mCurrentMac;
    public static int mCurrentHFP = 0;// public status for share

    private BtCmd mBtCmd;

    private static ATBluetooth mATBluetooth;// = new ATBluetooth();
    // private static int mCount = 0;

    public static ATBluetooth create() {
        if (mATBluetooth == null) {
            initBTType();
            mATBluetooth = new ATBluetooth();
        }
        if (!mATBluetooth.mHardwareStatus) {
            mATBluetooth.open();
        }
        return mATBluetooth;
    }

    public void destroy() {
        if (mATBluetooth != null) {
            mATBluetooth.write(0xff);
            mATBluetooth = null;
        }
    }

    public boolean mHardwareStatus = false;

    public boolean open() {
        int ret = mATBluetooth.write(0x80, mBTType);
        Log.d("abc", "" + ret);
        if (ret == 0) {
            mHardwareStatus = true;
        } else {
            mHardwareStatus = false;
            Util.setFileValue("/sys/class/ak/source/bluetoothsw", 1);
            if (mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
                Util.setProperty("ctl.start", "ivt_blueletd");
            } else if (mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {

            }
        }

        if (mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            startcheckIVTErr();
        }
        return mHardwareStatus;
    }

    public void close() {
        mHardwareStatus = false;
        write(0xff);
    }

    public static final int REQUEST_POWER = 0x01;
    public static final int RETURN_CONNECTED_MAC = 0x02;
    public static final int GET_PAIR_INFO = 0x03;
    public static final int RETURN_PAIR_INFO = 0x04;
    public static final int CLEAR_PAIR_INFO = 0x05;
    public static final int REQUEST_HFP = 0x06;
    public static final int RETURN_HFP = 0x07;
    public static final int GET_HFP_INFO = 0x08;
    public static final int RETURN_HFP_INFO = 0x09;
    public static final int REQUEST_CALL = 0x0a;
    public static final int RETURN_CALL = 0x0b;
    public static final int REQUEST_VOICE_SWITCH = 0x0c;
    public static final int RETURN_VOICE_SWITCH = 0x0d;
    public static final int REQUEST_AV = 0x0e;
    public static final int RETURN_AV = 0x0f;
    public static final int GET_AV_INFO = 0x10;
    public static final int RETURN_AV_INFO = 0x11;
    public static final int GET_A2DP_INFO = 0x12;
    public static final int RETURN_A2DP_INFO = 0x13;
    public static final int REQUEST_AV_PLAY = 0x14;
    public static final int RETURN_AV_PLAY = 0x15;
    public static final int REQUEST_PHONE_BOOK = 0x16;
    public static final int RETURN_PHONE_BOOK = 0x17;
    public static final int RETURN_PHONE_BOOK_DATA = 0x18;
    public static final int REQUEST_MIC = 0x19;
    public static final int REQUEST_CALL_LOG = 0x1a;
    public static final int RETURN_CALL_LOG = 0x1b;
    public static final int REQUEST_SEARCH = 0x1c;
    public static final int RETURN_SEARCH = 0x1d;
    public static final int REQUEST_NAME = 0x1e;
    public static final int RETURN_NAME = 0x1f;
    public static final int REQUEST_PIN = 0x20;
    public static final int RETURN_PIN = 0x21;
    public static final int REQUEST_SETTING = 0x22;
    public static final int RETURN_SETTING = 0x23;
    public static final int REQUEST_AUTO_CONNECT = 0x24;
    public static final int REQUEST_AUTO_ANSWER = 0x25;
    public static final int REQUEST_DISCONNECT = 0x26;

    public static final int RETURN_STOP_SEARCH = 0x27;


    public static final int REQUEST_AUTOCONNECT = 0x28;
    public static final int RETURN_AUTOCONNECT = 0x29;
    public static final int REQUEST_AUTOANSWER = 0x30;
    public static final int RETURN_AUTOANSWER = 0x31;

    public static final int START_MODULE = 0x32;
    public static final int STOP_MODULE = 0x33;


    public static final int RETURN_OBD_DISCONNECT = 0x34;


    public static final int REQUEST_CALL_LOG_OUT = 0x35;
    public static final int REQUEST_CALL_LOG_MISS = 0x36;
    public static final int REQUEST_CALL_LOG_ANSWER = 0x37;
    public static final int RETURN_CALLLOG_DATA = 0x38;
    public static final int RETURN_CALLLOG_END = 0xcc;

    public static final int REQUEST_CONNECT_BY_INDEX = 0x39;
    public static final int REQUEST_CONNECT_BY_ADDR = 0x40;

    public static final int REQUEST_EJECT = 0x41;
    public static final int REQUEST_HANG = 0x42;
    public static final int REQUEST_ANSWER = 0x43;
    public static final int REQUEST_DTMF = 0x44;
    public static final int REQUEST_VOICE_SWITCH_LOCAL = 0x45;
    public static final int REQUEST_VOICE_SWITCH_REMOTE = 0x46;
    public static final int REQUEST_MIC_MUTE = 0x47;
    public static final int REQUEST_MIC_UNMUTE = 0x48;
    public static final int REQUEST_AUTO_CONNECT_ENABLE = 0x49;
    public static final int REQUEST_AUTO_CONNECT_DISABLE = 0x4a;
    public static final int REQUEST_AUTO_ANSWER_ENABLE = 0x4b;
    public static final int REQUEST_AUTO_ANSWER_DISABLE = 0x4c;

    public static final int RETURN_SEARCH_START = 0x4d;
    public static final int RETURN_SEARCH_END = 0x4e;

    public static final int REQUEST_A2DP_PP = 0x4f;
    public static final int REQUEST_A2DP_PLAY = 0x50;
    public static final int REQUEST_A2DP_PAUSE = 0x51;
    public static final int REQUEST_A2DP_PREV = 0x52;
    public static final int REQUEST_A2DP_NEXT = 0x53;

    public static final int RETURN_CALL_OUT = 0x54;
    public static final int RETURN_CALL_INCOMING = 0x55;
    public static final int RETURN_CALL_END = 0x56;
    public static final int RETURN_CALLING = 0x57;

    public static final int REQUEST_REQUEST_VERSION = 0x58;
    public static final int RETURN_PHONE_BOOK_START = 0x59;

    //goc use
    public static final int RETURN_SEARCH_TYPE = 0x5a;
    public static final int RETURN_GOC_MUTE = 0x5b;
    public static final int RETURN_GOC_UNMUTE = 0x5c;

    public static final int REQUEST_3CALL_ADD = 0x5d;
    public static final int REQUEST_3CALL_MERGE = 0x5e;


    public static final int REQUEST_STOP_PHONE_BOOK = 0x60;
    public static final int RETURN_PHONE_BOOK_CONNECT_STATUS = 0x61;

    public static final int RETURN_A2DP_ON = 0x62;
    public static final int RETURN_A2DP_OFF = 0x63;

    public static final int REQUEST_A2DP_ID3 = 0x64;
    public static final int RETURN_A2DP_ID3 = 0x65;
    public static final int REQUEST_A2DP_TIME = 0x66;
    public static final int RETURN_A2DP_TIME = 0x67;
    public static final int RETURN_A2DP_CUR_TIME = 0x68;
    public static final int RETURN_A2DP_CONNECT_STATUS = 0x69;

    public static final int REQUEST_CONNECT_DEVICE = 0x6a;

    public static final int REQUEST_A2DP_REPORT_ID3 = 0x6b;

    public static final int REQUEST_A2DP_CONNECT_STATUS = 0x6c;

    public static final int REQUEST_IF_AUTO_CONNECT = 0x6d;

    public static final int REQUEST_PAIR_BY_ADDR = 0x6e;

    public static final int RETURN_PAIR_ADDR = 0x6f;

    public static final int RETURN_CLEAR_PAIR = 0x70;
    //ivt used
    public static final int RETURN_BT_CORE_ERROR = 0x71;

    public static final int RETURN_BT_IVT_CRASH = 0x72;

    //parrot modeul..
    public static final int REQUEST_PHONE_BOOK_DOWN_STATUS = 0x73;//PEDS : Enable phonebook download status
    public static final int REQUEST_PHONE_BOOK_SYNC = 0x74;//PPMS : Start manual phonebook synchronization
    public static final int RETURN_SYNC_STATUS = 0x75;//PPMS : Start manual phonebook synchronization
    public static final int REQUEST_DEL_PHONE_BOOK = 0x76;

    public static final int REQUEST_DISCOVERABLE = 0x77;
    public static final int REQUEST_CONNECTABLE = 0x78;
    public static final int REQUEST_SNIFF_MODE = 0x79;
    public static final int REQUEST_SSP_MODE = 0x7a;
    public static final int RETURN_PBEI = 0x7b;
    public static final int REQUEST_DEVICE_INDEX = 0x7c;
    public static final int RETURN_DEVICE_INDEX = 0x7d;
    public static final int GET_HFP_INFO2 = 0x7e;
    public static final int REQUEST_LANGUAGE = 0x7f;
    public static final int RETURN_LANGUAGE = 0x80;
    public static final int SET_LANGUAGE = 0x81;
    public static final int REQUEST_CPGM = 0x82;
    public static final int RETURN_CMD_OK = 0x83;
    public static final int RETURN_CPMC = 0x84;
    public static final int RETURN_CPML = 0x85;
    public static final int RETURN_PHONEBOOK_SIZE = 0x86;
    public static final int REQUEST_VOICE_STATUS = 0x87;
    public static final int RETURN_VOICE_STATUS = 0x88;
    public static final int REQUEST_RREC = 0x89;
    public static final int RETURN_RREC = 0x8a;
    public static final int REQUEST_RPRE = 0x8b;
    public static final int RETURN_RPRE = 0x8c;
    public static final int REQUEST_RPMT = 0x8d;
    public static final int RETURN_RPMT = 0x8e;
    public static final int RETURN_PPBU = 0x8f;
    public static final int RETURN_RRES = 0x90;
    public static final int RETURN_PPDS = 0x91;
    public static final int REQUEST_SET_VOLUME = 0x92;
    public static final int REQUEST_BT_MUSIC_CPCC = 0x93;
    public static final int REQUEST_CLMS = 0x94;
    public static final int REQUEST_DSCD = 0x95;
    public static final int RETURN_A2DP_NEED_CONECT = 0x96;
    public static final int RETURN_CLMS = 0x97;
    public static final int REQUEST_PHONE_BOOK_SIZE = 0x98;//PPMS : Start manual phonebook synchronization
    public static final int RETURN_CMD_ERROR = 0x99;
    public static final int REQUEST_CMD_AADC = 0x9a;
    public static final int REQUEST_CMD_ALAC = 0x9b;


    public static final int REQUEST_CMD_DGBM = 0x9c;
    public static final int REQUEST_CMD_DSCD = 0x9d;
    public static final int REQUEST_CMD_DGCD = 0x9e;
    public static final int REQUEST_CMD_DGEC = 0x9f;
    public static final int REQUEST_CMD_DLSE = 0xa0;
    public static final int REQUEST_CMD_PBSCEX = 0xa1;
    public static final int REQUEST_CMD_DLPE = 0xa2;
    public static final int REQUEST_CMD_PPNO = 0xa3;
    public static final int RETURN_CMD_PWNG2_DISCONNECT = 0xa4;


    public static final int REQUEST_BT_MUSIC_MUTE = 0xa5;
    public static final int REQUEST_BT_MUSIC_UNMUTE = 0xa6;

    public static final int RETURN_BATTERY = 0xa7;
    public static final int RETURN_SIGNAL = 0xa8;
    public static final int RETURN_MIC_STATUS = 0xa9;
    public static final int REQUEST_MIC_GAIN = 0xaa;
    public static final int RETURN_MIC_GAIN = 0xab;

    public static final int RETURN_3CALL_START = 0xb0;
    public static final int RETURN_3CALL_END = 0xb1;
    public static final int RETURN_3CALL_STATUS = 0xb2;

    public static final int REQUEST_3CALL_ANSWER = 0xb3;
    public static final int REQUEST_3CALL_HANG = 0xb4;
    public static final int REQUEST_3CALL_HANG2 = 0xb5;


    public static final int RETURN_A2DP_ID3_NAME = 0xb6;
    public static final int RETURN_A2DP_ID3_ARTIST = 0xb7;
    public static final int RETURN_A2DP_ID3_ALBUM = 0xb8;
    public static final int RETURN_A2DP_ID3_TOTAL_TIME = 0xb9;


    public static final int RETURN_PAN_NET_CONNECT = 0xba;
    public static final int RETURN_PAN_NET_DISCONNECT = 0xbc;
    public static final int REQUEST_GOC_BT_MODULE_MAC = 0xbd;
    public static final int RETURN_GOC_BT_MODULE_MAC = 0xbe;
    public static final int PARROT_DO_AUTON_ANSWER = 0xbf;
    public static final int RETURN_UPDATE_STATUS = 0xee;
    public static final int REQUEST_UPDATE_START = 0xef;

    public static final int REQUEST_UPDATE_BT = 0xf0;

    public static final int REQUEST_SOURCE = 0x0301;
    public static final int REQUEST_SOURCE_CALL = 0x0302;

    public static final int HFP_INFO_INITIAL = 0;
    public static final int HFP_INFO_READY = 1;
    public static final int HFP_INFO_CONNECTING = 2;
    public static final int HFP_INFO_CONNECTED = 3;
    public static final int HFP_INFO_CALLED = 4;
    public static final int HFP_INFO_INCOMING = 5;
    public static final int HFP_INFO_CALLING = 6;


    public static final int A2DP_NEED_CONNECT = -1;
    public static final int A2DP_INFO_INITIAL = 0;
    public static final int A2DP_INFO_READY = 1;
    public static final int A2DP_INFO_CONNECTING = 2;
    public static final int A2DP_INFO_CONNECTED = 3;
    public static final int A2DP_INFO_PLAY = 4;
    public static final int A2DP_INFO_PAUSED = 5;

    public static final int PHONE_BOOK_TYPE_SIM = 0;
    public static final int PHONE_BOOK_TYPE_PHONE = 1;
    public static final int PHONE_BOOK_TYPE_CALL_OUT = 2;
    public static final int PHONE_BOOK_TYPE_MISS = 3;
    public static final int PHONE_BOOK_TYPE_CALL_IN = 4;

    public static final int PHONE_BOOK_CONNECT_STATUS_OK = 0;
    public static final int PHONE_BOOK_CONNECT_STATUS_FAIL = 1;
    public static final int PHONE_BOOK_CONNECT_STATUS_DISCONNECT = 2;

    public static final int WRITE_DATA = 770;

    public static final int RETURN_MSG_MAX = 0x100000;

    public String mBtName = "";
    public String mBtPin = "";
    public String mBtModuleMac = "";

    private boolean mCloseIvtModule = false;

    public void closeIvtModule(boolean b) {
        mCloseIvtModule = b;
    }

    public boolean getCloseIvtModeule() {
        return mCloseIvtModule;
    }

    private native int nativeSendCommand(int what, int arg1, int arg2, byte[] obj);

    private long mLastCmdWrite = 0;

    public int write(int what, int arg1, int arg2, String obj) {
        //		if (mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
        //			if ((System.currentTimeMillis() - mLastCmdWrite) < 80) {
        //				Log.e(TAG, "write too fast return test" + what+":"+(System.currentTimeMillis() - mLastCmdWrite));
        //				//Util.doSleep(200);
        //				return 0;
        //			}
        //			mLastCmdWrite = System.currentTimeMillis();
        //		}

        Log.e(TAG, what + ":" + arg1 + ":" + arg2 + ":" + obj);
        if (mCloseIvtModule) {
            Log.e("write", "write close");
            return 0;
        }
        byte[] buf = mBtCmd.getCmd(what, arg1, arg2, obj);
        if (buf != null) {

            Log.e("write", (new String(buf)) + " len:" + buf.length);

            return nativeSendCommand(WRITE_DATA, arg1, arg2, buf);
        } else {
            return nativeSendCommand(what, arg1, arg2, buf);
        }

    }

    public int writeDirect(int what, byte[] buf) {
        return nativeSendCommand(what, 0, 0, buf);
    }

    //	public int write(byte[] buf) {
    //		return nativeSendCommand(WRITE_DATA, 0, 0, buf);
    //	}

    public int write(int what) {
        return write(what, 0, 0, null);
    }

    public int write(int what, int arg1) {
        return write(what, arg1, 0, null);
    }

    public int write(int what, int arg1, int arg2) {
        return write(what, arg1, arg2, null);
    }

    public int write(int what, int arg1, String obj) {
        return write(what, arg1, 0, obj);
    }

    public int write(int what, String obj) {
        return write(what, 0, 0, obj);
    }

    public void requestSource(int source) {
        write(REQUEST_SOURCE, (1 << 7) | (1 << 6), source);
    }

    public void requestSource() {
        requestSource(8);
    }

    public class THandler {
        public THandler(String tag, Handler handler) {
            mTag = tag;
            mHandler = handler;
        }

        String mTag;
        Handler mHandler;
    }

    private ArrayList<THandler> mTHandler = new ArrayList<THandler>();

    public void addHandler(String tag, Handler handler) {
        synchronized (mTHandler) {
            mTHandler.add(new THandler(tag, handler));
        }
    }

    public void removeHandler(String tag) {
        synchronized (mTHandler) {
            for (THandler thandler : mTHandler) {
                if ((thandler.mTag != null) && thandler.mTag.equals(tag)) {
                    mTHandler.remove(thandler);
                    break;
                }
            }
        }
    }

    public Handler getHandler(String tag) {
        for (THandler thandler : mTHandler) {
            if ((thandler.mTag != null) && thandler.mTag.equals(tag)) {
                return thandler.mHandler;
            }
        }
        return null;
    }

    public void sendHandler(String tag, int what, int arg1, int arg2, Object obj) {
        synchronized (mTHandler) {
            try {
                if (getHandler(tag) != null) {
                    getHandler(tag).obtainMessage(what, arg1, arg2, obj).sendToTarget();
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    public void sendHandler(String tag, int what) {
        sendHandler(tag, what, 0, 0, null);
    }

    public void sendHandler(String tag, int what, Object obj) {
        sendHandler(tag, what, 0, 0, obj);
    }

    public void sendHandler(String tag, int what, int arg1, int arg2) {
        sendHandler(tag, what, arg1, arg2, null);
    }

    public class TObject {
        public Object obj2;
        public Object obj3;

        public TObject(Object _obj2, Object _obj3) {
            obj2 = _obj2;
            obj3 = _obj3;
        }
    }

    IBtCallback mCallBack = new IBtCallback() {
        public void callback(int what, int arg1, String obj2, String obj3) {
            btCallback(what, arg1, obj2, obj3);
        }
    };

    private void btCallback(int what, int arg1, String obj2, String obj3) {
        // Log.e("A onCallback", what+":"+arg1+":"+obj2+":"+obj3);
        if (what == PARROT_DO_AUTON_ANSWER) {
            write(ATBluetooth.REQUEST_ANSWER);
            return;
        }
        synchronized (mTHandler) {
            for (THandler thandler : mTHandler) {
                try {
                    thandler.mHandler.obtainMessage(what, arg1, arg1, new TObject(obj2, obj3)).sendToTarget();
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }
    }

    private Handler mHandlerUI;

    public void setUIHandler(Handler h) {
        mHandlerUI = h;
    }

    public Handler getUIHandler() {
        return mHandlerUI;
    }

    // for delay send message.
    private Handler mHandlerDelay = new Handler() {
        public void handleMessage(Message msg) {
            write(msg.what, msg.arg1, msg.arg2);
            // super.handleMessage(msg);
        }
    };

    public void write_delay(int cmd, int arg1, int arg2, int delay) {
        mHandlerDelay.sendMessageDelayed(mHandlerDelay.obtainMessage(cmd, arg1, arg2), delay);
    }

    // /new ivt

    private void dataCallback(byte[] param, int len) {
        String s = (new String(param));
        Log.d(TAG, "dataCallback:" + s);
        //	if (s != null && s.startsWith("CRASH")) {
        // close();
        //	Log.d(TAG, "dataCallback:!!!!!!!!!!!!!!!!!!!!!" + s);

        //  the blueletd service work bad.should kill and reset.
        //mCallBack.callback(RETURN_BT_CORE_ERROR, 2, null, null);
        // end .......

        // just stop start soket. if blueletd service is work ok but crash.
        //			close();
        //			mHandlerCheckIvtErr.sendEmptyMessageDelayed(1, 1800);
        //end
        //		return;
        //	}
        // if (len < 2)
        // return;
        mCrashIVTLetdTime = System.currentTimeMillis();
        mCrashIVTLetd = 0;
        mBtCmd.dataCallback(param, len);
    }

    private void updateCallback(int len) {

        Log.d(TAG, "updateCallback:" + len);
        if (mCallBack != null) {
            mCallBack.callback(REQUEST_UPDATE_BT, len, null, null);
        }
    }

    public static final int DOWNLOAD_NUM_ONETIME = 5000;

    ////////////////for test

    private Handler mHandlerCheckIvtErr = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mHandlerCheckIvtErr.removeMessages(0);

                    if ((SystemClock.uptimeMillis() - mStartWatchDogTime) > IVT_WATCH_DOG_CHECK_TOTAL_TIME) {
                        Log.d(TAG, "!!!!!!!!! stop check ivt carsh");
                        return;
                    }

                    int delay = IVT_MSG_TIME;
                    if ((System.currentTimeMillis() - mCrashIVTLetdTime) >= IVT_MSG_TIME) { // check
                        mCrashIVTLetd++;
                        write(ATBluetooth.REQUEST_NAME);

                        if (mCrashIVTLetd >= 3) {
                            Log.d(TAG, "ivt carsh?? " + mCrashIVTLetd);

                            //						close();

                            //						mHandlerCheckIvtErr.sendEmptyMessageDelayed(1, 1800);
                            //						Util.doSleep(2000);
                            //
                            //						mCallBack.callback(RETURN_BT_IVT_CRASH, 0, null, null);
                            //						open();
                            mCallBack.callback(RETURN_BT_CORE_ERROR, 2, null, null);

                            mCrashIVTLetd = -5;
                            delay = 5000;
                        }
                    }
                    mHandlerCheckIvtErr.sendEmptyMessageDelayed(0, delay);
                    break;
                case 1:
                    mCallBack.callback(RETURN_BT_IVT_CRASH, 0, null, null);
                    break;
            }

        }
    };

    public void startcheckIVTErr() {
        mStartWatchDogTime = SystemClock.uptimeMillis();
        Log.d(TAG, "startcheckIVTErr: " + mCrashIVTLetd);
        mHandlerCheckIvtErr.removeMessages(0);
        mHandlerCheckIvtErr.sendEmptyMessageDelayed(0, IVT_MSG_TIME);
    }

    private final static int IVT_MSG_TIME = 1400;

    private final static int IVT_WATCH_DOG_CHECK_TOTAL_TIME = 30000;

    private long mCrashIVTLetdTime = 0;
    private long mStartWatchDogTime = 0;
    private int mCrashIVTLetd = 0;

}
