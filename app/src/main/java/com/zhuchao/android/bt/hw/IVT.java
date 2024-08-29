package com.zhuchao.android.bt.hw;

import android.util.Log;

public class IVT extends BtCmd {

    public final static String TAG = "IVT";

    private void init() {

        // ivt nosupport
        //		addSendCmd(ATBluetooth.REQUEST_3CALL_ADD, "CZ");
        //		addSendCmd(ATBluetooth.REQUEST_3CALL_MERGE, "IU");

        addSendCmd(ATBluetooth.GET_HFP_INFO, "CY");
        addSendCmd(ATBluetooth.REQUEST_DISCONNECT, "CD");
        addSendCmd(ATBluetooth.GET_PAIR_INFO, "MX");
        addSendCmd(ATBluetooth.REQUEST_SEARCH, "GD");
        addSendCmd(ATBluetooth.RETURN_STOP_SEARCH, "GC");
        addSendCmd(ATBluetooth.REQUEST_NAME, "MM");
        addSendCmd(ATBluetooth.REQUEST_REQUEST_VERSION, "TI");

        addSendCmd(ATBluetooth.START_MODULE, "BE1");
        addSendCmd(ATBluetooth.STOP_MODULE, "BE0");

        addSendCmd(ATBluetooth.REQUEST_IF_AUTO_CONNECT, "BA");

        addSendCmd(ATBluetooth.CLEAR_PAIR_INFO, "MR");
        addSendCmd(ATBluetooth.REQUEST_VOICE_SWITCH_LOCAL, "CO");
        addSendCmd(ATBluetooth.REQUEST_VOICE_SWITCH_REMOTE, "CO");
        addSendCmd(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE, "MG");
        addSendCmd(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE, "MH");
        addSendCmd(ATBluetooth.REQUEST_AUTO_ANSWER_ENABLE, "MP");
        addSendCmd(ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE, "MQ");
        addSendCmd(ATBluetooth.REQUEST_MIC, "CM");

        addSendCmd(ATBluetooth.REQUEST_SETTING, "MF");

        addSendCmd(ATBluetooth.REQUEST_PIN, "MN");
        addSendCmd(ATBluetooth.REQUEST_CALL, "CW");
        addSendCmd(ATBluetooth.REQUEST_HANG, "CG");
        addSendCmd(ATBluetooth.REQUEST_EJECT, "CF");
        addSendCmd(ATBluetooth.REQUEST_ANSWER, "CE");
        addSendCmd(ATBluetooth.REQUEST_DTMF, "CX");

        addSendCmd(ATBluetooth.REQUEST_CONNECT_BY_ADDR, "CC");
        addSendCmd(ATBluetooth.REQUEST_PAIR_BY_ADDR, "GPC");

        addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK, "PA");
        addSendCmd(ATBluetooth.REQUEST_POWER, "BE");
        addSendCmd(ATBluetooth.REQUEST_CONNECT_DEVICE, "CY");

        addSendCmd(ATBluetooth.REQUEST_STOP_PHONE_BOOK, "PW");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PLAY, "MA");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PAUSE, "MB");
        addSendCmd(ATBluetooth.REQUEST_A2DP_NEXT, "MD");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PREV, "ME");
        addSendCmd(ATBluetooth.REQUEST_A2DP_ID3, "RF");
        addSendCmd(ATBluetooth.REQUEST_A2DP_TIME, "RE");
        addSendCmd(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS, "MV");

        addSendCmd(ATBluetooth.REQUEST_A2DP_REPORT_ID3, "RN");


        addSendCmd(ATBluetooth.REQUEST_3CALL_ANSWER, "CK");
        addSendCmd(ATBluetooth.REQUEST_3CALL_HANG, "CI");
        addSendCmd(ATBluetooth.REQUEST_3CALL_HANG2, "CJ");

        addReceiveCmd(ATBluetooth.RETURN_SEARCH_END, "GE");

        addReceiveCmd(ATBluetooth.RETURN_CALL_OUT, "IC", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_CALL_INCOMING, "ID", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_CALL_END, "IF");
        addReceiveCmd(ATBluetooth.RETURN_CALLING, "IR", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_SIGNAL, "IY", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_BATTERY, "IW", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_PHONE_BOOK, "PC");

        addReceiveCmd(ATBluetooth.RETURN_HFP_INFO, "MG", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_CONNECT_STATUS, "ML", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_CONNECT_STATUS, "MU", TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_HFP_INFO, "IA", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_NAME, "MM", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_PIN, "MN", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3, "RN", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_A2DP_CUR_TIME, "ROP",
        // TYPE_PARAM_STR1);

        addReceiveCmd(ATBluetooth.RETURN_SEARCH_START, "GD", TYPE_CMD_RETURN);

        addReceiveCmd(ATBluetooth.RETURN_A2DP_ON, "RB");

        addReceiveCmd(ATBluetooth.RETURN_A2DP_OFF, "RA");


        addReceiveCmd(ATBluetooth.RETURN_BT_CORE_ERROR, "TS", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.REQUEST_REQUEST_VERSION, "IS", TYPE_PARAM_STR1);


        //		addReceiveCmd(ATBluetooth.RETURN_PAIR_ADDR, "GPB");


    }

    // send
    public IVT() {
        init();
    }

    //	private boolean mVoiceSwitchLocal = true;
    //	private boolean mMicMute = false;

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
            obj = arg1 + "," + arg2 + "," + (arg2 + ATBluetooth.DOWNLOAD_NUM_ONETIME);
        } else if (what == ATBluetooth.REQUEST_CONNECT_DEVICE) {
            obj = "1";
        }
        // else if (what == ATBluetooth.REQUEST_A2DP_PP) {
        // if (mPlay) {
        // what = ATBluetooth.REQUEST_A2DP_PAUSE;
        // } else {
        // what = ATBluetooth.REQUEST_A2DP_PLAY;
        // }
        // }

        return super.getCmd(what, arg1, arg2, obj);
    }

    // receive
    public int dataCallback(byte[] param, int len) { // special , we do it
        // ourself

        if (mCallBack == null) {
            return 0;
        }

        int what = 0;
        int arg1 = 0;
        String obj2 = null;
        String obj3 = null;

        try {
            if (param[0] == 'G' && param[1] == 'F') {
                String s = new String(param, 15, 8);
                if ("00001F00".equals(s)) {
                    arg1 = 1;
                } else {
                    arg1 = 0;
                }
                obj2 = new String(param, 2, 12);
                obj3 = new String(param, 23, len - 23);


                Log.d("allen", s + ":" + obj3);
                what = ATBluetooth.RETURN_SEARCH;
            } else if (param[0] == 'M' && param[1] == 'X') {
                what = ATBluetooth.RETURN_PAIR_INFO;
                arg1 = param[2] - '0';

                String s = new String(param, 3, 8);
                if ("00001F00".equals(s)) {
                    arg1 |= 0x100;
                }

                obj2 = new String(param, 11, 12);
                obj3 = new String(param, 23, len - 23);
            } else if (param[0] == 'M' && param[1] == 'F') {
                what = ATBluetooth.RETURN_SETTING;
                int i1 = param[2] - '0';
                int i2 = param[3] - '0';
                arg1 = i1 | (i2 << 8);
            } else if (param[0] == 'G' && param[1] == 'E') {
                what = ATBluetooth.RETURN_SEARCH_END;
            } else if (param[0] == 'I' && param[1] == 'A') {
                what = ATBluetooth.RETURN_HFP_INFO;
                arg1 = ATBluetooth.HFP_INFO_INITIAL;
            } else if (param[0] == 'I' && param[1] == 'V') {
                what = ATBluetooth.RETURN_HFP_INFO;
                arg1 = ATBluetooth.HFP_INFO_CONNECTING;
            } else if (param[0] == 'I' && param[1] == 'B') {
                what = ATBluetooth.RETURN_HFP_INFO;
                arg1 = ATBluetooth.HFP_INFO_CONNECTED;
                obj2 = new String(param, 2, len - 3);
            }
            //			else if (param[0] == 'S' && param[1] == 'C') {
            //				what = ATBluetooth.RETURN_HFP_INFO;
            //				arg1 = ATBluetooth.HFP_INFO_CONNECTED;
            //				obj2 = new String(param, 2, len - 3);
            //			}
            else if (param[0] == 'P' && param[1] == 'S') {
                what = ATBluetooth.RETURN_PHONE_BOOK_CONNECT_STATUS;
                arg1 = param[3] - '0';
            } else if (len >= 3 && param[0] == 'R' && param[1] == 'O' && param[2] == 'P') {
                what = ATBluetooth.RETURN_A2DP_CUR_TIME;
                obj2 = new String(param, 3, len - 3);
            } else if (param[0] == 'P' && param[1] == 'B') {
                what = ATBluetooth.RETURN_PHONE_BOOK_DATA;
                arg1 = param[2] - '0';

                String strLenName;
                int nLenName;
                String strLenNum;
                int nLenNum;
                strLenName = new String(param, 5, 2);
                nLenName = Integer.parseInt(strLenName);
                strLenNum = new String(param, 7, 2);
                nLenNum = Integer.parseInt(strLenNum);

                obj3 = new String(param, 9, nLenName);
                obj2 = new String(param, 9 + nLenName, nLenNum);

            } else if (param[0] == 'M' && param[1] == 'C') {
                mVoiceSwitchLocal = true;
                what = ATBluetooth.RETURN_VOICE_SWITCH;
            } else if (param[0] == 'M' && param[1] == 'D') {
                mVoiceSwitchLocal = false;
                what = ATBluetooth.RETURN_VOICE_SWITCH;
            } else if (param[0] == 'G' && param[1] == 'P' && param[2] == 'B') {
                obj2 = new String(param, 3, 12);
                arg1 = param[15] - '0';
                what = ATBluetooth.RETURN_PAIR_ADDR;
            } else if (param[0] == 'I' && param[1] == 'P') {
                obj2 = new String(param, 6, param.length - 6);
                arg1 = (param[4] - '0') | ((param[2] - '0') << 8);
                what = ATBluetooth.RETURN_3CALL_START;
                //				Log.d(TAG, "what:="+ATBluetooth.RETURN_3CALL_START+"  obj2:"+obj2);
            } else if (param[0] == 'I' && param[1] == 'Q') {
                obj2 = new String(param, 6, param.length - 6);
                arg1 = param[2] - '0';
                what = ATBluetooth.RETURN_3CALL_END;
                //				Log.d(TAG, "what:="+ATBluetooth.RETURN_3CALL_START+"  obj2:"+obj2);
            } else if (param[0] == 'I' && param[1] == 'T') {
                obj2 = new String(param, 6, param.length - 6);
                arg1 = (param[4] - '0') | ((param[2] - '0') << 8);
                //				arg2 = param[2] - '0';
                what = ATBluetooth.RETURN_3CALL_STATUS;
                //				Log.d(TAG, "what:="+ATBluetooth.RETURN_3CALL_START+"  obj2:"+obj2);
            }

            //			else if (param[0] == 'I' && param[1] == 'F') {
            //				mVoiceSwitchLocal = false;
            //				mMicMute = false;
            //				what = ATBluetooth.RETURN_CALL_END;
            //			}

            // else if (param[0] == 'M' && param[1] == 'A') {
            // mPlay = false;
            // } else if (param[0] == 'M' && param[1] == 'B') {
            // mPlay = true;
            // }

            else if (param[0] == 'E') {

                int code = 0;// to do ...
                if (param[1] == '0' && param[2] == '0') {

                    if (param[3] == 'G' && param[4] == 'D') {
                        what = ATBluetooth.RETURN_SEARCH_START;
                    } else if (param[3] == 'P' && param[4] == 'A') {
                        what = ATBluetooth.RETURN_PHONE_BOOK_START;
                    } else if (param[3] == 'M' && param[4] == 'R') {
                        what = ATBluetooth.RETURN_CLEAR_PAIR;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "dataCallback err" + e);
        }
        if (what != 0 && mCallBack != null) {
            mCallBack.callback(what, arg1, obj2, obj3);

            // mCallBack.callback(what, arg1, obj2, obj3);//test
        } else {
            return super.dataCallback(param, len);
        }
        return 0;
    }

    //
    public int mPPDS;
    public int mPPBU;

}
