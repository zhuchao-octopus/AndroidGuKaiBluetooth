package com.zhuchao.android.bt.hw;

import android.util.Log;

import com.common.util.Util;

import java.util.ArrayList;

public abstract class BtCmd {

    static final String TAG = "BtCmd";
    public static final int TYPE_CMD_RETURN = 0x1 << 0;

    public static final int TYPE_PARAM_INT1 = 0x1 << 1;

    public static final int TYPE_PARAM_STR1 = 0x1 << 2;

    public static final int TYPE_PARAM_STR2 = 0x1 << 3;

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_PARAM1 = TYPE_PARAM_INT1;
    public static final int TYPE_PARAM12 = TYPE_PARAM_INT1 | TYPE_PARAM_STR1;
    public static final int TYPE_PARAM123 = TYPE_PARAM_INT1 | TYPE_PARAM_STR1 | TYPE_PARAM_STR2;
    public static final int TYPE_PARAM23 = TYPE_PARAM_STR1 | TYPE_PARAM_STR2;

    protected ArrayList<SendCmdData> mSendCmd = new ArrayList<SendCmdData>();
    protected ArrayList<ReceiveCmdData> mReceiveCmd = new ArrayList<ReceiveCmdData>();

    public boolean mVoiceSwitchLocal = true;
    public boolean mMicMute = false;

    public class SendCmdData {
        int mId;
        int mType;
        String mCmd;

        public SendCmdData(int id, String cmd, int type) {
            mId = id;
            mCmd = cmd;
            mType = type;
        }
    }

    public class ReceiveCmdData {
        int mId;
        int mType;
        String mCmd;
        // byte mLen1;
        int mLen2;
        int mLen3;

        public ReceiveCmdData(int id, String cmd, int type, byte end1, int len2, int len3) {
            mId = id;
            mCmd = cmd;
            mType = type;

            mLen2 = len2;
            mLen3 = len3;
        }

        public ReceiveCmdData(int id, String cmd, int type) {
            mId = id;
            mCmd = cmd;
            mType = type;
        }
    }

    public void addSendCmd(int id, String cmd, int type) {
        mSendCmd.add(new SendCmdData(id, cmd, type));
    }

    public void addSendCmd(int id, String cmd) {
        mSendCmd.add(new SendCmdData(id, cmd, TYPE_NORMAL));
    }

    public void addReceiveCmd(int id, String cmd, int type) {
        mReceiveCmd.add(new ReceiveCmdData(id, cmd, type));
    }

    public void addReceiveCmd(int id, String cmd) {
        mReceiveCmd.add(new ReceiveCmdData(id, cmd, TYPE_NORMAL));
    }

    // public void addSendCmd(String cmd) {
    // addSendCmd(cmd, TYPE_NORMAL);
    // }

    public byte[] getCmd(int what, int arg1, int arg2, String obj) {
        byte[] cmd = null;
        for (SendCmdData data : mSendCmd) {
            if (data.mId == what) {
                int len = 5 + data.mCmd.length();
                if (obj != null) {
                    len += obj.length();
                }

                cmd = new byte[len + 1];
                cmd[0] = 'A';
                cmd[1] = 'T';
                cmd[2] = '#';
                cmd[len - 2] = '\r';
                cmd[len - 1] = '\n';
                cmd[len] = 0;

                Util.byteArrayCopy(cmd, data.mCmd.getBytes(), 3, 0, data.mCmd.length());

                if (obj != null) {
                    Util.byteArrayCopy(cmd, obj.getBytes(), 3 + data.mCmd.length(), 0, obj.length());
                }
                break;
            }
        }
        return cmd;
    }

    public int dataCallback(byte[] param, int len) {
        if (mCallBack == null) {
            return 0;
        }
        int what = 0;
        int arg1 = 0;
        String obj2 = null;
        String obj3 = null;
        int step = 2;
        for (ReceiveCmdData data : mReceiveCmd) {

            if (data.mCmd != null) {
                byte[] buf = data.mCmd.getBytes();

                if (data.mType == TYPE_CMD_RETURN) {
                    if (param[0] == buf[0]) {

                        break;
                    }
                } else {

                    if (buf.length >= 2) {
                        if (param[0] == buf[0] && param[1] == buf[1]) {
                            what = data.mId;

                            try {
                                if ((data.mType & TYPE_PARAM_INT1) != 0) {
                                    arg1 = param[step] - '0';
                                    ++step;
                                }

                                if ((data.mType & TYPE_PARAM_STR1) != 0) {
                                    if ((data.mType & TYPE_PARAM_STR2) == 0) {
                                        data.mLen2 = (len - step);
                                    }
                                    obj2 = new String(param, step, data.mLen2);
                                    step += data.mLen2;
                                }

                                if ((data.mType & TYPE_PARAM_STR2) != 0) {
                                    obj3 = new String(param, step, (len - step));
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "dataCallback err:" + e);
                            }

                            break;
                        }
                    }
                }
            }
        }

        if (what != 0) {
            mCallBack.callback(what, arg1, obj2, obj3);
        }
        return 0;
    }

    public void setCallback(IBtCallback cb) {
        mCallBack = cb;
    }

    IBtCallback mCallBack;

}
