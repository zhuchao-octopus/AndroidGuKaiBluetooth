package com.zhuchao.android.bt.hw;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.common.util.Util;
import com.zhuchao.android.bt.bt.GlobalDef;

import java.util.ArrayList;
import java.util.Locale;

public class Parrot extends BtCmd {

    private void init() {

        addSendCmd(ATBluetooth.REQUEST_UPDATE_START, "*PBSU=1");
        addSendCmd(ATBluetooth.REQUEST_REQUEST_VERSION, "+CGMREX");
        addSendCmd(ATBluetooth.REQUEST_BT_MUSIC_CPCC, "*CPCC=");
        addSendCmd(ATBluetooth.REQUEST_CMD_PPNO, "*PPNO=");

        addSendCmd(ATBluetooth.REQUEST_CMD_AADC, "*AADC=");
        addSendCmd(ATBluetooth.REQUEST_CMD_ALAC, "*ALAC=");

        addSendCmd(ATBluetooth.REQUEST_SET_VOLUME, "*AVOL=");
        addSendCmd(ATBluetooth.REQUEST_VOICE_STATUS, "*RSTS?");
        addSendCmd(ATBluetooth.REQUEST_RREC, "*RREC=");
        addSendCmd(ATBluetooth.REQUEST_RPRE, "*RPRE=");
        addSendCmd(ATBluetooth.REQUEST_RPMT, "*RPMT=");

        addSendCmd(ATBluetooth.REQUEST_SSP_MODE, "*PSSP=0");
        addSendCmd(ATBluetooth.REQUEST_DISCOVERABLE, "*PSDM=1");
        addSendCmd(ATBluetooth.REQUEST_CONNECTABLE, "*PSCA=1");
        addSendCmd(ATBluetooth.REQUEST_SNIFF_MODE, "*PDSM=0");

        addSendCmd(ATBluetooth.REQUEST_DEVICE_INDEX, "*PLRU");
        addSendCmd(ATBluetooth.GET_HFP_INFO, "*PIND=?");
        addSendCmd(ATBluetooth.GET_HFP_INFO2, "*PLCC");
        addSendCmd(ATBluetooth.REQUEST_DISCONNECT, "*PSBD=");
        addSendCmd(ATBluetooth.GET_PAIR_INFO, "*PLPD");
        addSendCmd(ATBluetooth.REQUEST_SEARCH, "*PBDI=1");
        addSendCmd(ATBluetooth.RETURN_STOP_SEARCH, "*PBDI=0");
        addSendCmd(ATBluetooth.REQUEST_NAME, "*PSFN");

        addSendCmd(ATBluetooth.REQUEST_IF_AUTO_CONNECT, "BA");

        addSendCmd(ATBluetooth.CLEAR_PAIR_INFO, "*PPDE");
        addSendCmd(ATBluetooth.REQUEST_VOICE_SWITCH_LOCAL, "*PATR=0");
        addSendCmd(ATBluetooth.REQUEST_VOICE_SWITCH_REMOTE, "*PATR=1");
        addSendCmd(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE, "*PSCM=1");
        addSendCmd(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE, "*PSCM=0");
        addSendCmd(ATBluetooth.REQUEST_AUTO_ANSWER_ENABLE, "MP");
        addSendCmd(ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE, "MQ");
        addSendCmd(ATBluetooth.REQUEST_MIC, "CM");

        addSendCmd(ATBluetooth.REQUEST_AUTOCONNECT, "*PSCM?");

        addSendCmd(ATBluetooth.REQUEST_PIN, "*PPAU?");
        addSendCmd(ATBluetooth.REQUEST_CALL, "D");
        addSendCmd(ATBluetooth.REQUEST_HANG, "+CHUP");
        addSendCmd(ATBluetooth.REQUEST_EJECT, "+CHUP");
        addSendCmd(ATBluetooth.REQUEST_ANSWER, "A");
        addSendCmd(ATBluetooth.REQUEST_DTMF, "+VTS=");

        addSendCmd(ATBluetooth.REQUEST_CONNECT_BY_INDEX, "*PSBD=P");
        addSendCmd(ATBluetooth.REQUEST_PAIR_BY_ADDR, "*PADDR=");

        addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK, "*PPMS=P1");

        addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK_DOWN_STATUS, "*PEDS=1");
        addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK_SYNC, "*PVCARD=");
        addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK_SIZE, "*PVCARD=");
        addSendCmd(ATBluetooth.REQUEST_DEL_PHONE_BOOK, "*PDCT=P1,1");
        // addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK, "*PVCARD=P1,1,504");
        addSendCmd(ATBluetooth.REQUEST_POWER, "BE");
        addSendCmd(ATBluetooth.REQUEST_CONNECT_DEVICE, "CY");

        addSendCmd(ATBluetooth.REQUEST_STOP_PHONE_BOOK, "PW");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PLAY, "*CPLY=2");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PAUSE, "*CPLY=1");
        addSendCmd(ATBluetooth.REQUEST_A2DP_NEXT, "*CPLY=3");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PREV, "*CPLY=4");
        addSendCmd(ATBluetooth.REQUEST_A2DP_ID3, "*CPGM=");
        // addSendCmd(ATBluetooth.REQUEST_A2DP_TIME, "*DSCD=S1");
        addSendCmd(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS, "*CGPS");

        addSendCmd(ATBluetooth.REQUEST_A2DP_REPORT_ID3, "RN");

        addSendCmd(ATBluetooth.REQUEST_LANGUAGE, "*RSCL?");
        addSendCmd(ATBluetooth.SET_LANGUAGE, "*RSCL=");

        addSendCmd(ATBluetooth.REQUEST_CLMS, "*CLMS=0");
        addSendCmd(ATBluetooth.REQUEST_CMD_DGBM, "*DGBM=");
        addSendCmd(ATBluetooth.REQUEST_CMD_DSCD, "*DSCD=");
        addSendCmd(ATBluetooth.REQUEST_CMD_DGCD, "*DGCD=");
        addSendCmd(ATBluetooth.REQUEST_CMD_DGEC, "*DGEC=");
        addSendCmd(ATBluetooth.REQUEST_CMD_DLSE, "*DLSE=");
        addSendCmd(ATBluetooth.REQUEST_CMD_PBSCEX, "*PBSCEX=0");
        addSendCmd(ATBluetooth.REQUEST_CMD_DLPE, "*DLPE=");

        // addSendCmd(ATBluetooth.REQUEST_DSCD,
        // "*ALAC='Audio/Config/tango-algo.xml'");

        //		addReceiveCmd(ATBluetooth.RETURN_LANGUAGE, "*RSCL:", TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_PPBU, "*PPBU:", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3, "*CPGM:", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3, "*PBCV:", TYPE_PARAM_STR1);
        //		addReceiveCmd(ATBluetooth.RETURN_CMD_PWNG, "*PWNG:", TYPE_PARAM_STR1);

        // addReceiveCmd(ATBluetooth.RETURN_CALL_OUT, "IC", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_CALL_INCOMING, "ID",
        // TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_CALL_END, "IF");
        // addReceiveCmd(ATBluetooth.RETURN_CALLING, "IR", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_PHONE_BOOK, "PC");

        // addReceiveCmd(ATBluetooth.RETURN_HFP_INFO, "MG", TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_A2DP_CONNECT_STATUS, "ML",
        // TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_A2DP_CONNECT_STATUS, "MU",
        // TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_HFP_INFO, "IA", TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_NAME, "+PSFN", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_PIN, "MN", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3, "RN", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_A2DP_CUR_TIME, "ROP",
        // TYPE_PARAM_STR1);

        // addReceiveCmd(ATBluetooth.RETURN_SEARCH_START, "GD",
        // TYPE_CMD_RETURN);

        // addReceiveCmd(ATBluetooth.RETURN_A2DP_ON, "RB");

        // addReceiveCmd(ATBluetooth.RETURN_A2DP_OFF, "RA");

        // addReceiveCmd(ATBluetooth.RETURN_PAIR_ADDR, "GPB");

    }

    // send
    public Parrot() {
        init();
        mAutoAnswer = getData(SAVE_ANSWER);
        Log.d("ffck2", ":" + mAutoAnswer);
    }

    // private boolean mVoiceSwitchLocal = true;
    // private boolean mMicMute = false;

    public int mAutoAnswer = 0;

    public byte[] getCmd(int what, int arg1, int arg2, String obj) {

        if (what == ATBluetooth.REQUEST_VOICE_SWITCH) {
            if (mVoiceSwitchLocal) {
                what = ATBluetooth.REQUEST_VOICE_SWITCH_REMOTE;
            } else {
                what = ATBluetooth.REQUEST_VOICE_SWITCH_LOCAL;
            }
            mVoiceSwitchLocal = !mVoiceSwitchLocal;
        } else if (what == ATBluetooth.REQUEST_MIC) {
            if (mMicMute) {
                obj = "0";
            } else {
                obj = "1";
            }
            mMicMute = !mMicMute;
        } else if (what == ATBluetooth.REQUEST_PHONE_BOOK) {
            // obj = arg1 + "," + arg2 + ","
            // + (arg2 + ATBluetooth.DOWNLOAD_NUM_ONETIME);
            obj = null;
        } else if (what == ATBluetooth.REQUEST_CONNECT_DEVICE) {
            obj = "1";
        } else if (what == ATBluetooth.REQUEST_CONNECT_BY_INDEX) {
            obj = obj + ",1";
        } else if (what == ATBluetooth.REQUEST_NAME) {
            if (obj != null) {
                obj = "='" + obj + "'";
            } else {
                obj = "?";
            }
        } else if (what == ATBluetooth.REQUEST_DISCONNECT) {
            if (mDeviceIndex != null) {
                obj = mDeviceIndex + ",0";
            }
        } else if (what == ATBluetooth.REQUEST_DEVICE_INDEX) {
            // mDeviceIndex = null;
        } else if (what == ATBluetooth.REQUEST_A2DP_ID3 || what == ATBluetooth.REQUEST_PHONE_BOOK_SYNC) {
            mPreNeedOKCmd = what;
        } else if (what == ATBluetooth.REQUEST_BT_MUSIC_CPCC) {
            if (mMediaIndex != null) {
                if (obj == null) {
                    obj = mMediaIndex + ",0";
                } else {
                    obj = mMediaIndex + "," + obj;
                }
                // obj = ATBluetooth.mCurrentMac.replace("P", "S") + ",0";
            }
        }
        // else if (what == ATBluetooth.CLEAR_PAIR_INFO) {
        // obj = ATBluetooth.mCurrentMac;
        //
        // }
        else if (what == ATBluetooth.REQUEST_CMD_DGCD || what == ATBluetooth.REQUEST_CMD_DLSE || what == ATBluetooth.REQUEST_CMD_DSCD) {
            if (obj == null) {
                obj = mMediaIndex + ",1";
            } else {
                obj = mMediaIndex + "," + obj;
            }
        } else if (what == ATBluetooth.REQUEST_CMD_DGEC) {
            obj = mMediaIndex;
        } else if (what == ATBluetooth.REQUEST_CMD_PPNO) {
            obj = mDeviceIndex;
        } else if (what == ATBluetooth.CLEAR_PAIR_INFO) {
            if (obj != null) {
                obj = "=P" + obj;
            }
        } else if (what == ATBluetooth.REQUEST_AUTO_ANSWER_ENABLE) {
            mAutoAnswer = 1;

            if (what != 0 && mCallBack != null) {
                mCallBack.callback(ATBluetooth.RETURN_AUTOANSWER, mAutoAnswer, null, null);
            }
            saveData(SAVE_ANSWER, mAutoAnswer);
            //			return  ;
        } else if (what == ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE) {
            mAutoAnswer = 0;
            if (what != 0 && mCallBack != null) {
                mCallBack.callback(ATBluetooth.RETURN_AUTOANSWER, mAutoAnswer, null, null);
            }
            saveData(SAVE_ANSWER, mAutoAnswer);
            //			return  ;
        } else if (what == ATBluetooth.REQUEST_AUTOCONNECT) {

            //			mAutoAnswer = getData(SAVE_ANSWER);
            if (what != 0 && mCallBack != null) {
                mCallBack.callback(ATBluetooth.RETURN_AUTOANSWER, mAutoAnswer, null, null);
            }
        }
        //		else if (what == ATBluetooth.REQUEST_HANG
        //				|| what == ATBluetooth.REQUEST_EJECT) {
        //			obj = getPhoneActivityIndex()+"";
        //		}
        // else if (what == ATBluetooth.REQUEST_A2DP_PP) {
        // if (mPlay) {
        // what = ATBluetooth.REQUEST_A2DP_PAUSE;
        // } else {
        // what = ATBluetooth.REQUEST_A2DP_PLAY;
        // }
        // }

        byte[] cmd = null;
        for (SendCmdData data : mSendCmd) {
            if (data.mId == what) {
                int len = 4 + data.mCmd.length();
                if (obj != null) {
                    len += obj.length();
                }

                if (what == ATBluetooth.REQUEST_CALL) {
                    len++;
                }
                cmd = new byte[len];
                cmd[0] = 'A';
                cmd[1] = 'T';
                cmd[len - 2] = '\r';
                cmd[len - 1] = '\n';

                if (what == ATBluetooth.REQUEST_CALL) {
                    cmd[len - 3] = ';';
                }

                Util.byteArrayCopy(cmd, data.mCmd.getBytes(), 2, 0, data.mCmd.length());

                if (obj != null) {
                    Util.byteArrayCopy(cmd, obj.getBytes(), 2 + data.mCmd.length(), 0, obj.length());
                }
                break;
            }
        }
        if (cmd != null) {
            mPreCmd = what;
        }
        return cmd;

        // return super.getCmd(what, arg1, arg2, obj);
    }

    private byte[] mParrotPreCmd = null;

    public int dataCallback(byte[] param2, int len) {

        byte[] param = new byte[len];
        Util.byteArrayCopy(param, param2, 0, 0, len);

        String s;
        String slog;
        if (mParrotPreCmd != null) {
            byte[] old = param;
            int newLen = mParrotPreCmd.length + len;
            param = new byte[newLen];
            Util.byteArrayCopy(param, mParrotPreCmd, 0, 0, mParrotPreCmd.length);
            Util.byteArrayCopy(param, old, mParrotPreCmd.length, 0, len);
            len = newLen;
        }

        s = (new String(param));

        // Log.d("bb", "doParrotCmd:" + s);

        String[] ss = s.split("\r\n");
        boolean endCmd = false;
        if (len > 2 && param[len - 2] == '\r' && param[len - 1] == '\n') {
            mParrotPreCmd = null;
            endCmd = true;
        } else {

            int i = len - 1;
            for (; i > 2; --i) {
                if (param[i - 2] == '\r' && param[i - 1] == '\n') {
                    i = i - 2;
                    break;
                }
            }
            int pre_len = len;
            if (i > 2) {
                pre_len = len - i;
            } else {
                i = 0;
            }
            mParrotPreCmd = new byte[pre_len];

            Util.byteArrayCopy(mParrotPreCmd, param, 0, i, pre_len);

            slog = (new String(mParrotPreCmd));
        }
        for (int i = 0; i < ss.length - 1; ++i) {
            if (ss[i].length() > 0) {
                byte[] cmd = ss[i].getBytes();
                dataCallback2(cmd, cmd.length);

                Log.d(TAG, "doParrotCmd:" + ss[i]);
            }
            // Log.d(TAG, "doParrotCmd:" + ss[i]);
        }
        if (endCmd) {
            if (ss[ss.length - 1].length() > 0) {
                byte[] cmd = ss[ss.length - 1].getBytes();
                dataCallback2(cmd, cmd.length);
                Log.d(TAG, "2doParrotCmd:" + ss[ss.length - 1]);
            }
        }
        return 0;
    }

    // receive

    public int dataCallback2(byte[] param, int len) { // special , we do it
        // ourself

        if (mCallBack == null) {
            return 0;
        }

        int what = 0;
        int arg1 = 0;
        String obj2 = null;
        String obj3 = null;

        try {
            // if (param[0] == 'G' && param[1] == 'F') {
            // String s = new String(param, 15, 8);
            // if ("00001F00".equals(s)) {
            // arg1 = 1;
            // } else {
            // arg1 = 0;
            // }
            // obj2 = new String(param, 2, 12);
            // obj3 = new String(param, 23, len - 23);
            //
            // Log.d("allen", s + ":" + obj3);
            // what = ATBluetooth.RETURN_SEARCH;
            // } else

            if (param[0] == '*' && param[1] == 'P' && param[2] == 'L' && param[3] == 'P' && param[4] == 'D') {

                what = ATBluetooth.RETURN_PAIR_INFO;
                arg1 = param[7] - '0';

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");

                obj2 = ss[2];
                obj3 = ss[1].substring(1, ss[1].length() - 1);

                if (ss.length > 2 && ss[0].equals(mDeviceIndex)) {
                    arg1 |= 0x10000;

                }

            } else if (param[0] == 'M' && param[1] == 'F') {
                what = ATBluetooth.RETURN_SETTING;
                int i1 = param[2] - '0';
                int i2 = param[3] - '0';
                arg1 = i2 | (i1 << 8);
            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'B' && param[3] == 'D' && param[4] == 'I') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (ss[0].equals("END")) {
                    what = ATBluetooth.RETURN_SEARCH_END;
                } else {

                    // if(ss[0].equals("I1")){
                    // mCallBack.callback(ATBluetooth.RETURN_SEARCH_START, 0,
                    // null, null);
                    // }

                    obj2 = ss[2];
                    obj3 = ss[1].substring(1, ss[1].length() - 1);

                    // Log.d("allen", s + ":" + obj3);

                    what = ATBluetooth.RETURN_SEARCH;

                }

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'B' && param[3] == 'E' && param[4] == 'I') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                obj2 = ss[0];

                what = ATBluetooth.RETURN_PBEI;

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'L' && param[3] == 'R' && param[4] == 'U') {

                if (mDeviceIndex == null) {
                    obj2 = mDeviceIndex = new String(param, 6, len - 6);
                    what = ATBluetooth.RETURN_DEVICE_INDEX;
                }

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'I' && param[3] == 'N' && param[4] == 'D') {

                if (param[6] == '1') {
                    if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED) {
                        what = ATBluetooth.RETURN_HFP_INFO;
                        arg1 = ATBluetooth.HFP_INFO_CONNECTED;
                        String s = new String(param, 0, len);
                        String[] ss = s.split(",");
                        obj3 = new String(param, 7, 1);
                    }

                }

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'S' && param[3] == 'F' && param[4] == 'N') {

                what = ATBluetooth.RETURN_NAME;
                obj2 = new String(param, 7, len - 8);

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'I' && param[3] == 'E' && param[4] == 'V') {

                if (param[6] == '1') {
                    if (param[8] == '0') {
                        what = ATBluetooth.RETURN_HFP_INFO;
                        arg1 = ATBluetooth.HFP_INFO_INITIAL;
                        clearPhoneStatus();
                        mSyncPhoneBookeStatus = -1;
                    } else if (param[6] == '1') {
                        what = ATBluetooth.RETURN_HFP_INFO;
                        arg1 = ATBluetooth.HFP_INFO_CONNECTED;
                    }
                }

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'L' && param[3] == 'C' && param[4] == 'C') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");

                int index = Integer.valueOf(ss[0]);

                if (param[8] == '0') {
                    what = ATBluetooth.RETURN_CALLING;
                    arg1 = ATBluetooth.HFP_INFO_CALLING;
                    checkPhoneStatus(index, 1);
                } else if (param[8] == '2' || param[8] == '3') {
                    what = ATBluetooth.RETURN_CALL_OUT;
                    arg1 = ATBluetooth.HFP_INFO_CALLED;
                    checkPhoneStatus(index, 1);
                } else if (param[8] == '4') {
                    what = ATBluetooth.RETURN_CALL_INCOMING;
                    arg1 = ATBluetooth.HFP_INFO_INCOMING;
                    checkPhoneStatus(index, 1);

                    if (mAutoAnswer == 1) {
                        mHandlerDelayAutoAnswer.removeMessages(0);
                        mHandlerDelayAutoAnswer.sendEmptyMessageDelayed(0, 800);
                    }
                } else {

                    if (!checkPhoneStatus(index, 0)) {
                        what = ATBluetooth.RETURN_CALL_END;
                        arg1 = ATBluetooth.HFP_INFO_CONNECTED;
                    }
                }

                if (ss.length > 7) {
                    obj2 = ss[7].substring(1, ss[7].length() - 1);
                    obj3 = ss[5].substring(1, ss[5].length() - 1);
                    obj3 = obj3.replace(";", " ");
                } else if (ss.length > 5) {
                    obj2 = ss[5].substring(1, ss[5].length() - 1);
                }

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'B' && param[3] == 'S' && param[4] == 'N') {
                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (ss[1].equals("0") && ss[2].equals("1")) {
                    what = ATBluetooth.RETURN_HFP_INFO;
                    arg1 = ATBluetooth.HFP_INFO_CONNECTED;
                    mDeviceIndex = ss[0];
                    // obj2 = ss[0];
                } else if (!ss[1].equals("0") && ss[2].equals("1")) {
                    what = ATBluetooth.RETURN_HFP_INFO;
                    arg1 = ATBluetooth.HFP_INFO_INITIAL;
                    clearPhoneStatus();
                    // obj3 = new String(param, 7, 1);
                    mSyncPhoneBookeStatus = -1;
                }

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'P' && param[3] == 'A' && param[4] == 'U') {
                obj2 = new String(param, 9, 4);

                what = ATBluetooth.RETURN_PIN;
            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'S' && param[3] == 'C' && param[4] == 'M') {

                arg1 = param[6] - '0';

                what = ATBluetooth.RETURN_AUTOCONNECT;
            } else if (param[0] == '*' && param[1] == 'C' && param[2] == 'G' && param[3] == 'P' && param[4] == 'S') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (ss.length > 2) {
                    int state = Integer.valueOf(ss[0]);
                    int audio;
                    switch (state) {
                        case 0:
                        case 8:
                            arg1 = ATBluetooth.A2DP_INFO_CONNECTED;
                            break;
                        case 1:
                            // audio = Integer.valueOf(ss[1]);
                            // if (audio == 2) {
                            // arg1 = ATBluetooth.A2DP_INFO_PLAY;
                            // } else if (audio == 1 || audio == 0) {
                            arg1 = ATBluetooth.A2DP_INFO_PAUSED;
                            // } else {
                            // arg1 = ATBluetooth.A2DP_INFO_PAUSED;
                            // }

                            break;
                        case 2:

                            // audio = Integer.valueOf(ss[1]);
                            // if (audio == 2) {
                            // arg1 = ATBluetooth.A2DP_INFO_PLAY;
                            // } else if (audio == 1 || audio == 0) {
                            // arg1 = ATBluetooth.A2DP_INFO_PAUSED;
                            // } else {
                            arg1 = ATBluetooth.A2DP_INFO_PLAY;
                            // }
                            break;
                        // case 3:
                        // case 4:
                        // case 5:
                        case 6:
                            arg1 = ATBluetooth.A2DP_INFO_INITIAL;
                            break;
                        // case 7:
                        // case 8:
                        case 9: {
                            audio = Integer.valueOf(ss[1]);
                            if (audio == 2) {
                                arg1 = ATBluetooth.A2DP_INFO_PLAY;
                            } else if (audio == 1 || audio == 0) {
                                arg1 = ATBluetooth.A2DP_INFO_PAUSED;
                            } else {
                                return 0;
                            }
                        }
                        break;
                        default:
                            return 0;
                    }

                } else {
                    arg1 = ATBluetooth.A2DP_NEED_CONNECT;
                }
                what = ATBluetooth.RETURN_A2DP_CONNECT_STATUS;
                // if (state == 6 || state == 10|| state == 7|| state == 0) {
                // if (state == 6 || state == 10) {
                // arg1 = ATBluetooth.A2DP_INFO_INITIAL;
                // } if (state == 0){
                // arg1 = ATBluetooth.A2DP_INFO_CONNECTED;
                // } else {
                // return 0;
                // }
                // } else {
                // int audio = Integer.valueOf(ss[1]);
                // if (audio == 2) {
                // arg1 = ATBluetooth.A2DP_INFO_PLAY;
                // } else if (audio == 1) {
                // arg1 = ATBluetooth.A2DP_INFO_PAUSED;
                // } else {
                // return 0;
                // }
                // }

            } else if (param[0] == '*' && param[1] == 'C' && param[2] == 'P' && param[3] == 'M' && param[4] == 'C') {

                obj2 = new String(param, 6, len - 6);
                what = ATBluetooth.RETURN_CPMC;
            } else if (param[0] == '*' && param[1] == 'C' && param[2] == 'P' && param[3] == 'M' && param[4] == 'L') {

                what = ATBluetooth.RETURN_CPML;
            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'V' && param[3] == 'C' && param[4] == 'A' && param[5] == 'R' && param[6] == 'D') {

                String s = new String(param, 8, len - 8);
                String[] ss = s.split(",");
                if (ss.length == 2) {
                    if (ss[0].startsWith("END")) {
                        what = ATBluetooth.RETURN_PHONE_BOOK;
                    } else if (mPreCmd == ATBluetooth.REQUEST_PHONE_BOOK_SIZE) {

                        // Log.d("fk", (new String(param, 0, len))+"::"+ss[1]);
                        arg1 = Integer.valueOf(ss[1]);
                        what = ATBluetooth.RETURN_PHONEBOOK_SIZE;
                    }
                } else if (ss.length >= 4) {
                    int property = Integer.valueOf(ss[2]);
                    if (property == 1 || property == 4) {
                        if (property == 1) {
                            obj3 = ss[3].substring(1, ss[3].length() - 1);
							/*
							 * 
							 * 
[HSTI_PB_SORTING_TYPE] 
- 0 (FIRSTNAME) : Name order is First Name - Last Name
- 1 (LASTNAME) : Name order is Last Name - First Name
- 2 (SOUND_FIRSTNAME) : Name order is Sound Field - First Name - Last Name
- 3 (SOUND_LASTNAME) : Name order is Sound Field - Last Name - First Name
							 */
                            String[] name = obj3.split(";");

                            String firstName = ""; //default is 1
                            String lastName = "";

                            if (name.length > 0) {
                                lastName = name[0];
                            }
                            if (name.length > 1) {
                                firstName = name[1];
                            }

                            if (mPhoneBookNameSeq == 0) {
                                if (name.length > 0) {
                                    firstName = name[0];
                                }
                                if (name.length > 1) {
                                    lastName = name[1];
                                }
                            }

                            String locale = Locale.getDefault().getLanguage();
                            if (!locale.equals("zh")) {
                                obj3 = firstName;
                                if (obj3.length() > 0) {
                                    obj3 += " ";
                                }
                                obj3 += lastName;
                            } else {
                                obj3 = lastName;
                                if (obj3.length() > 0) {
                                    obj3 += " ";
                                }
                                obj3 += firstName;
                            }

                            //obj3 = obj3.replace(";", "");
                        } else if (property == 4) {
                            obj2 = ss[3].substring(1, ss[3].length() - 1);
                        }
                        arg1 = Integer.valueOf(ss[1]);
                        what = ATBluetooth.RETURN_PHONE_BOOK_DATA;
                    }


                }
            } else if (param[0] == '*' && param[1] == 'R' && param[2] == 'S' && param[3] == 'T' && param[4] == 'S') {

                what = ATBluetooth.RETURN_VOICE_STATUS;
            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'P' && param[3] == 'N' && param[4] == 'O') {
                String s = new String(param, 6, 1);
                mPhoneBookNameSeq = Integer.valueOf(s);
            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'P' && param[3] == 'D' && param[4] == 'S') {

                String s = new String(param, 6, len - 6);
                arg1 = Integer.valueOf(s);
                what = ATBluetooth.RETURN_PPDS;
            } else if (param[0] == '*' && param[1] == 'C' && param[2] == 'L' && param[3] == 'M' && param[4] == 'S') {

                String s = new String(param, 6, len - 6);

                String[] ss = s.split(",");
                arg1 = 0;
                if (ss[6].equals("1")) {
                    arg1 = 1;
                    mMediaIndex = ss[0];
                }
                what = ATBluetooth.RETURN_CLMS;

            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'P' && param[3] == 'B' && param[4] == 'U') {

                String s = new String(param, 6, len - 6);
                arg1 = Integer.valueOf(s);
                mSyncPhoneBookeStatus = arg1;
                what = ATBluetooth.RETURN_PPBU;

            } else if (param[0] == '*' && param[1] == 'R' && param[2] == 'R' && param[3] == 'E' && param[4] == 'S') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (ss.length >= 4) {
                    arg1 = mRRESNum;
                    obj2 = ss[1];
                    if (ss.length > 4) {
                        obj3 = ss[4] + "," + ss[5];
                    }

                } else if (ss.length >= 3) {
                    arg1 = Integer.valueOf(ss[1]);
                    if (arg1 == 1) { // su
                        arg1 = mRRESNum = Integer.valueOf(ss[2]);
                    } else {
                        mRRESNum = 0;
                        arg1 = -arg1;
                    }
                }
                what = ATBluetooth.RETURN_RRES;
            } else if (param[0] == '*' && param[1] == 'D' && param[2] == 'G' && param[3] == 'C' && param[4] == 'D') {

                String s = new String(param, 6, len - 6);

                //				String s3 = new String(param, 0, len - 0);
                //				byte[] test = s.getBytes("GBK");
                //				String s2 = new String(test, 0, test.length);

                String[] ss = s.split(",");
                if (!ss[0].startsWith("END")) {
                    obj2 = mCurA2DPPath = ss[1].substring(1, ss[1].length() - 1);
                    what = ATBluetooth.REQUEST_CMD_DGCD;
                }
            } else if (param[0] == '*' && param[1] == 'D' && param[2] == 'G' && param[3] == 'E' && param[4] == 'C') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (!ss[0].startsWith("END")) {
                    arg1 = mCurA2DPFileNum = Integer.valueOf(ss[1]);
                    what = ATBluetooth.REQUEST_CMD_DGEC;
                }
            } else if (param[0] == '*' && param[1] == 'D' && param[2] == 'L' && param[3] == 'S' && param[4] == 'E') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (!ss[0].startsWith("END")) {
                    if (ss[1].equals("1")) {
                        mMusicFileInfo.clear();
                    }
                    String info = ss[2];
                    for (int i = 3; i < ss.length; ++i) {
                        info += "," + ss[i];
                    }
                    mMusicFileInfo.add(info);

                    arg1 = Integer.valueOf(ss[1]);
                } else {
                    arg1 = 0;
                }
                what = ATBluetooth.REQUEST_CMD_DLSE;
            } else if (param[0] == '*' && param[1] == 'D' && param[2] == 'S' && param[3] == 'C' && param[4] == 'D') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                if (ss[0].startsWith("END")) {
                    if (ss[1].equals("0")) {

                        what = ATBluetooth.REQUEST_CMD_DSCD;
                    }
                }
            } else if (param[0] == '*' && param[1] == 'R' && param[2] == 'S' && param[3] == 'C' && param[4] == 'L') {

                String s = new String(param, 6, len - 6);
                String[] ss = s.split(",");
                arg1 = Integer.valueOf(ss[0]);

                what = ATBluetooth.RETURN_LANGUAGE;


            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'B' && param[3] == 'C' && param[4] == 'V') {

                obj2 = new String(param, 6, len - 6);
                what = ATBluetooth.REQUEST_REQUEST_VERSION;


            } else if (param[0] == '+' && param[1] == 'C' && param[2] == 'G' && param[3] == 'M' && param[4] == 'R' && param[5] == 'E' && param[6] == 'X') {

                obj2 = new String(param, 8, len - 8);
                what = ATBluetooth.REQUEST_REQUEST_VERSION;


            } else if (param[0] == '*' && param[1] == 'P' && param[2] == 'W' && param[3] == 'N' && param[4] == 'G') {

                String s = new String(param, 6, len - 6);

                String[] ss = s.split(",");
                if (ss[0].equals("2")) {
                    arg1 = Integer.valueOf(ss[1].substring(1, 2));
                    what = ATBluetooth.RETURN_CMD_PWNG2_DISCONNECT;
                }


            }


            // else if (param[0] == 'I' && param[1] == 'F') {
            // mVoiceSwitchLocal = false;
            // mMicMute = false;
            // what = ATBluetooth.RETURN_CALL_END;
            // }

            // else if (param[0] == 'M' && param[1] == 'A') {
            // mPlay = false;
            // } else if (param[0] == 'M' && param[1] == 'B') {
            // mPlay = true;
            // }

            else if (param[0] == 'E' && param[1] == 'R' && param[2] == 'R' && param[3] == 'O' && param[4] == 'R') {
                String s = new String(param, 6, len - 6);
                if (s.equals("203")) {

                    what = ATBluetooth.RETURN_HFP_INFO;
                    arg1 = ATBluetooth.HFP_INFO_INITIAL;
                    clearPhoneStatus();
                } else {
                    // Log.d("ff", s+":"+mPreCmd);
                    what = ATBluetooth.RETURN_CMD_ERROR;
                    arg1 = mPreCmd;
                    obj2 = s;
                }

            } else if (param[0] == 'O' && param[1] == 'K') {
                what = ATBluetooth.RETURN_CMD_OK;
                arg1 = mPreNeedOKCmd;
                // if(mPreNeedOKCmd == ATBluetooth.REQUEST_PHONE_BOOK_SYNC){
                // what = ATBluetooth.RETURN_PHONE_BOOK_START;
                // }
                mPreNeedOKCmd = 0;
            }
        } catch (Exception e) {
            Log.d(TAG, "cmd err" + e);
            return 0;
        }

        if (what != 0 && mCallBack != null) {
            mCallBack.callback(what, arg1, obj2, obj3);

            // mCallBack.callback(what, arg1, obj2, obj3);//test
        } else {

            if (mCallBack == null) {
                return 0;
            }
            int step = 6;
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
        }
        return 0;
    }

    private int[] mActivityPhone = new int[6];

    private int getPhoneActivityIndex() {
        for (int i = 0; i < mActivityPhone.length; ++i) {
            if (mActivityPhone[i] == 1) {
                return i;
            }
        }
        return 0;
    }

    private boolean checkPhoneStatus(int index, int activity) {
        if (index < mActivityPhone.length) {
            mActivityPhone[index] = activity;
            if (activity == 0) {
                for (int i = 0; i < mActivityPhone.length; ++i) {
                    if (mActivityPhone[i] == 1) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private void clearPhoneStatus() {
        for (int i = 0; i < mActivityPhone.length; ++i) {
            mActivityPhone[i] = 0;
        }
    }

    private int mPhoneBookNameSeq = 1;
    private int mPreCmd = 0;
    private int mPreNeedOKCmd = 0;
    private int mRRESNum = 0;
    public static String mDeviceIndex;
    public static String mMediaIndex = null;

    // a2dp list . only support Apple
    public String mCurA2DPPath = "";
    public int mCurA2DPFileNum = 0;
    public static ArrayList<String> mMusicFileInfo = new ArrayList<String>();

    public static int mSyncPhoneBookeStatus = -1;


    private static String SAVE_DATA = "parrot";
    private static String SAVE_ANSWER = "auto_answer";

    private void saveData(String s, int v) {
        if (GlobalDef.getContext() != null) {
            SharedPreferences.Editor sharedata = GlobalDef.getContext().getSharedPreferences(SAVE_DATA, 0).edit();

            sharedata.putInt(s, v);
            sharedata.commit();
        }
    }

    private int getData(String s) {
        if (GlobalDef.getContext() != null) {
            SharedPreferences sharedata = GlobalDef.getContext().getSharedPreferences(SAVE_DATA, 0);
            return sharedata.getInt(s, 0);
        }
        return 0;
    }

    private Handler mHandlerDelayAutoAnswer = new Handler() {
        public void handleMessage(Message msg) {
            if (mCallBack != null) {
                mCallBack.callback(ATBluetooth.PARROT_DO_AUTON_ANSWER, 0, null, null);
            }
        }
    };
}
