package com.zhuchao.android.bt.hw;

import android.util.Log;

import com.common.util.Util;

public class GOC extends BtCmd {

    public final static String TAG = "GOC";

    private void init() {

        addSendCmd(ATBluetooth.REQUEST_BT_MUSIC_MUTE, "VA");
        addSendCmd(ATBluetooth.REQUEST_BT_MUSIC_UNMUTE, "VB");

        addSendCmd(ATBluetooth.START_MODULE, "P1");
        addSendCmd(ATBluetooth.STOP_MODULE, "P0");

        addSendCmd(ATBluetooth.REQUEST_3CALL_ADD, "CZ");
        addSendCmd(ATBluetooth.REQUEST_3CALL_MERGE, "IU");
        addSendCmd(ATBluetooth.GET_HFP_INFO, "CY");

        addSendCmd(ATBluetooth.GET_HFP_INFO2, "QA");

        addSendCmd(ATBluetooth.REQUEST_SEARCH, "SD");
        addSendCmd(ATBluetooth.RETURN_STOP_SEARCH, "ST");

        addSendCmd(ATBluetooth.REQUEST_DISCONNECT, "CD");
        addSendCmd(ATBluetooth.REQUEST_CONNECT_BY_ADDR, "CC");


        addSendCmd(ATBluetooth.REQUEST_REQUEST_VERSION, "MY");

        addSendCmd(ATBluetooth.REQUEST_NAME, "MM");
        addSendCmd(ATBluetooth.REQUEST_PIN, "MN");

        addSendCmd(ATBluetooth.GET_PAIR_INFO, "MX");

        addSendCmd(ATBluetooth.REQUEST_SETTING, "MF");

        addSendCmd(ATBluetooth.REQUEST_CALL_LOG_OUT, "PH");
        addSendCmd(ATBluetooth.REQUEST_CALL_LOG_MISS, "PJ");
        addSendCmd(ATBluetooth.REQUEST_CALL_LOG_ANSWER, "PI");

        addSendCmd(ATBluetooth.REQUEST_A2DP_PLAY, "MS");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PAUSE, "MB");
        addSendCmd(ATBluetooth.REQUEST_A2DP_NEXT, "MD");
        addSendCmd(ATBluetooth.REQUEST_A2DP_PREV, "ME");


        addSendCmd(ATBluetooth.REQUEST_A2DP_ID3, "MZ");

        addSendCmd(ATBluetooth.CLEAR_PAIR_INFO, "CV");


        addReceiveCmd(ATBluetooth.RETURN_SEARCH_START, "CT");

        addReceiveCmd(ATBluetooth.RETURN_SEARCH_END, "IY");

        addReceiveCmd(ATBluetooth.RETURN_PAN_NET_CONNECT, "NC");
        addReceiveCmd(ATBluetooth.RETURN_PAN_NET_DISCONNECT, "NA");


        addSendCmd(ATBluetooth.REQUEST_PAIR_BY_ADDR, "PD");


        addSendCmd(ATBluetooth.REQUEST_VOICE_SWITCH_LOCAL, "CP");
        addSendCmd(ATBluetooth.REQUEST_VOICE_SWITCH_REMOTE, "CP");

        addSendCmd(ATBluetooth.REQUEST_MIC, "CM");

        addSendCmd(ATBluetooth.REQUEST_CALL, "CZ");
        addSendCmd(ATBluetooth.REQUEST_MIC_GAIN, "VM");
        addSendCmd(ATBluetooth.REQUEST_HANG, "CG");
        addSendCmd(ATBluetooth.REQUEST_EJECT, "CE");
        addSendCmd(ATBluetooth.REQUEST_ANSWER, "CF");

        addSendCmd(ATBluetooth.REQUEST_UPDATE_START, "UP");

        addReceiveCmd(ATBluetooth.RETURN_GOC_MUTE, "HA");
        addReceiveCmd(ATBluetooth.RETURN_GOC_UNMUTE, "HB");

        addReceiveCmd(ATBluetooth.RETURN_SIGNAL, "PS", TYPE_PARAM_STR1);
        //		addReceiveCmd(ATBluetooth.RETURN_BATTERY, "IW", TYPE_PARAM_STR1);
        /////////////////////////////////


        addSendCmd(ATBluetooth.REQUEST_IF_AUTO_CONNECT, "BA");

        addSendCmd(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE, "MG");
        addSendCmd(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE, "MH");
        addSendCmd(ATBluetooth.REQUEST_AUTO_ANSWER_ENABLE, "MP");
        addSendCmd(ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE, "MQ");


        addSendCmd(ATBluetooth.REQUEST_DTMF, "CX");


        addSendCmd(ATBluetooth.REQUEST_PHONE_BOOK, "PK");
        addSendCmd(ATBluetooth.REQUEST_POWER, "BE");
        addSendCmd(ATBluetooth.REQUEST_CONNECT_DEVICE, "CY");

        addSendCmd(ATBluetooth.REQUEST_STOP_PHONE_BOOK, "PS");
        addSendCmd(ATBluetooth.REQUEST_A2DP_TIME, "RE");
        addSendCmd(ATBluetooth.REQUEST_A2DP_CONNECT_STATUS, "MV");

        addSendCmd(ATBluetooth.REQUEST_A2DP_REPORT_ID3, "RN");


        addSendCmd(ATBluetooth.REQUEST_3CALL_ANSWER, "CK");
        addSendCmd(ATBluetooth.REQUEST_3CALL_HANG, "CI");
        addSendCmd(ATBluetooth.REQUEST_3CALL_HANG2, "CJ");


        addReceiveCmd(ATBluetooth.RETURN_CALL_END, "IF");
        //		addReceiveCmd(ATBluetooth.RETURN_CALLING, "IG");
        addReceiveCmd(ATBluetooth.RETURN_PHONE_BOOK, "PC");
        addReceiveCmd(ATBluetooth.RETURN_CALLLOG_END, "PL");

        //		addReceiveCmd(ATBluetooth.RETURN_HFP_INFO, "MG", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_CONNECT_STATUS, "ML", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_CONNECT_STATUS, "MU", TYPE_PARAM1);
        // addReceiveCmd(ATBluetooth.RETURN_HFP_INFO, "IA", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_NAME, "MM", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_PIN, "MN", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3_NAME, "M0", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3_ARTIST, "M1", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3_ALBUM, "M2", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_A2DP_ID3_TOTAL_TIME, "M6", TYPE_PARAM_STR1);
        addReceiveCmd(ATBluetooth.RETURN_GOC_BT_MODULE_MAC, "DB", TYPE_PARAM_STR1);
        // addReceiveCmd(ATBluetooth.RETURN_A2DP_CUR_TIME, "ROP",
        // TYPE_PARAM_STR1);


        addReceiveCmd(ATBluetooth.RETURN_A2DP_ON, "MB");

        addReceiveCmd(ATBluetooth.RETURN_A2DP_OFF, "MA");


        addReceiveCmd(ATBluetooth.RETURN_BT_CORE_ERROR, "TS", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.RETURN_MIC_GAIN, "VM", TYPE_PARAM1);
        addReceiveCmd(ATBluetooth.REQUEST_REQUEST_VERSION, "MW", TYPE_PARAM_STR1);


        //		addReceiveCmd(ATBluetooth.RETURN_PAIR_ADDR, "GPB");


    }

    // send
    public GOC() {
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
        }
        //		else if (what == ATBluetooth.REQUEST_MIC) {
        //			if (mMicMute) {
        //				obj = "0";
        //			} else {
        //				obj = "1";
        //			}
        //			mMicMute = !mMicMute;
        //		}
        else if (what == ATBluetooth.REQUEST_CONNECT_DEVICE) {
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

    private byte[] mParrotPreCmd = null;

    public int dataCallback(byte[] param2, int len) {

        for (int j = 0; j < len; ++j) {
            if (param2[j] == (byte) 0xff) {
                param2[j] = ';';
            }
        }
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
            if (ss.length > 1 && ss[ss.length - 1].length() > 0) {
                byte[] cmd = ss[ss.length - 1].getBytes();
                dataCallback2(cmd, cmd.length);
                Log.d(TAG, "2doParrotCmd:" + ss[ss.length - 1]);
            }
        }
        return 0;
    }

    public int dataCallback2(byte[] param, int len) {
        if (param[len - 1] == '\n') {
            len--;
        }
        if (mCallBack == null) {
            return 0;
        }

        int what = 0;
        int arg1 = 0;
        String obj2 = null;
        String obj3 = null;

        try {
            if (len == 3 && param[0] == 'S' && param[1] == 'P' && param[2] == 'S') {
                what = ATBluetooth.RETURN_OBD_DISCONNECT;
            } else if (param[0] == 'S' && len == 3) {


                arg1 = param[1] - '0';
                if (arg1 < 0 || arg1 > 6) {
                    Log.d(TAG, "HFP status err");
                    return 0;
                }
                if (arg1 == 6) {
                    what = ATBluetooth.RETURN_CALLING;
                } else {
                    what = ATBluetooth.RETURN_HFP_INFO;
                }
                if (what != 0 && mCallBack != null) {
                    mCallBack.callback(what, arg1, obj2, obj3);
                }

                what = 0;

                arg1 = param[2] - '0';
                if (arg1 == 2 || arg1 == 4) {
                    what = ATBluetooth.RETURN_A2DP_OFF;
                } else if (arg1 == 3) {
                    what = ATBluetooth.RETURN_A2DP_ON;
                }


            } else if (param[0] == 'T' && len == 2) {
                arg1 = param[1] - '0';
                if (arg1 == 1) {
                    mVoiceSwitchLocal = false;
                    what = ATBluetooth.RETURN_VOICE_SWITCH;
                } else if (arg1 == 0) {

                    mVoiceSwitchLocal = true;
                    what = ATBluetooth.RETURN_VOICE_SWITCH;
                }


            } else if (param[0] == 'I' && param[1] == 'X') {

                if ((param[2] - '0') == 9) {
                    arg1 = 1;
                } else {
                    arg1 = 0;
                }

                obj2 = new String(param, 3, 12);
                obj3 = new String(param, 15, len - 15);


                what = ATBluetooth.RETURN_SEARCH;
            } else if (param[0] == 'J' && param[1] == 'I') {
                what = ATBluetooth.RETURN_PAIR_INFO;
                arg1 = param[2] - '0';

                if ((param[3] - '0') == 9) {
                    arg1 |= 0x100;
                }


                obj2 = new String(param, 4, 12);
                obj3 = new String(param, 16, len - 16);

            } else if (param[0] == 'D' && param[1] == 'E') {

                String s = new String(param, 2, 6);
                if ("001f00".equals(s)) {
                    what = ATBluetooth.RETURN_SEARCH_TYPE;
                    arg1 = 1;
                }
            } else if (param[0] == 'I' && param[1] == 'C') {

                //				String s = 	new String(param, 2, 2);

                what = ATBluetooth.RETURN_CALL_OUT;
                obj2 = new String(param, 4, len - 4);

            } else if (param[0] == 'I' && param[1] == 'D') {

                //				String s = 	new String(param, 2, 2);

                what = ATBluetooth.RETURN_CALL_INCOMING;
                obj2 = new String(param, 4, len - 4);

            } else if (param[0] == 'I' && param[1] == 'G') {

                //				String s = 	new String(param, 2, 2);
                if (len > 4) {
                    what = ATBluetooth.RETURN_CALLING;
                    obj2 = new String(param, 4, len - 4);
                }

            } else if (param[0] == 'J' && param[1] == 'H') {
                what = ATBluetooth.RETURN_CONNECTED_MAC;
                arg1 = ATBluetooth.HFP_INFO_CONNECTED;
                if (len >= 14) {
                    obj2 = new String(param, 2, len - 2);
                }
            } else if (param[0] == 'M' && param[1] == 'F') {
                what = ATBluetooth.RETURN_SETTING;
                int i1 = param[3] - '0';
                int i2 = param[2] - '0';
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

                if (len >= 14) {
                    obj2 = new String(param, 2, len - 2);
                }

            }
            //			else if (param[0] == 'S' && param[1] == 'C') {
            //				what = ATBluetooth.RETURN_HFP_INFO;
            //				arg1 = ATBluetooth.HFP_INFO_CONNECTED;
            //				obj2 = new String(param, 2, len - 3);
            //			}
            else if (param[0] == 'P' && param[1] == 'A') {

                arg1 = param[2] - '0';
                if (arg1 == 1) {
                    what = ATBluetooth.RETURN_PHONE_BOOK_START;
                } else {
                    what = ATBluetooth.RETURN_PHONE_BOOK_CONNECT_STATUS;
                }
            } else if (len >= 3 && param[0] == 'R' && param[1] == 'O' && param[2] == 'P') {
                what = ATBluetooth.RETURN_A2DP_CUR_TIME;
                obj2 = new String(param, 3, len - 3);
            } else if (param[0] == 'P' && param[1] == 'B') {

                int find_split = param.length - 1;
                for (; find_split > 2; --find_split) {
                    if (param[find_split] == (byte) ';') {
                        break;
                    }
                }

                if (find_split < param.length) {
                    obj3 = new String(param, 2, find_split - 2);
                    obj2 = new String(param, find_split + 1, len - 1 - find_split);
                }

                what = ATBluetooth.RETURN_PHONE_BOOK_DATA;

            } else if (param[0] == 'P' && param[1] == 'D') {

                //				String date = null;
                //				int find_split = 3;
                //				int start = 3;
                //				for (;find_split<param.length;++find_split){
                //					if(param[find_split] == (byte)';'){
                //						if (obj3 == null){
                //							obj3 = new String(param, start, find_split-start);
                //							start = find_split+1;
                //						} else if (obj2 == null){
                //							obj2 = new String(param, start, find_split-start);
                //							start = find_split+1;
                //							date = new String(param, start, param.length-start);
                //
                //							arg1 = getTime(date);
                //							break;
                //						}
                //					}
                //				}
                if (param[2] == '[') {
                    obj2 = new String(param, 3, param.length - 3);
                } else {
                    obj2 = new String(param, 2, param.length - 3);
                }
                what = ATBluetooth.RETURN_CALLLOG_DATA;
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
                arg1 = (param[4] - '0') | ((param[2] - '0') << 8);
                what = ATBluetooth.RETURN_3CALL_END;
                //				Log.d(TAG, "what:="+ATBluetooth.RETURN_3CALL_START+"  obj2:"+obj2);
            } else if (param[0] == 'I' && param[1] == 'T') {
                obj2 = new String(param, 6, param.length - 6);
                arg1 = (param[4] - '0') | ((param[2] - '0') << 8);
                //				arg2 = param[2] - '0';
                what = ATBluetooth.RETURN_3CALL_STATUS;
                //				Log.d(TAG, "what:="+ATBluetooth.RETURN_3CALL_START+"  obj2:"+obj2);
            } else if (param[0] == 'U' && param[1] == 'P') {
                obj2 = new String(param, 2, param.length - 2);
                arg1 = 1;
                what = ATBluetooth.RETURN_UPDATE_STATUS;
            } else if (param[0] == 'D' && param[1] == 'F' && param[2] == 'U') {
                obj2 = new String(param, 3, param.length - 3);
                arg1 = 2;
                what = ATBluetooth.RETURN_UPDATE_STATUS;
            } else if (param[0] == 'M' && param[1] == 'I' && param[2] == 'C') {
                arg1 = (param[3] - '0');
                if (arg1 == 0) {
                    mMicMute = false;
                } else {
                    mMicMute = true;
                }
                what = ATBluetooth.RETURN_MIC_STATUS;
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
