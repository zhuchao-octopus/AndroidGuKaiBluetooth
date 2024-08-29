package com.zhuchao.android.bt.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.common.util.AppConfig;
import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.Util;
import com.zhuchao.android.bt.R;

import com.zhuchao.android.bt.bt.ATBluetoothService;
import com.zhuchao.android.bt.bt.BroadcastUtil;
import com.zhuchao.android.bt.bt.PhoneBook;
import com.zhuchao.android.bt.bt.SaveData;
import com.zhuchao.android.bt.hw.ATBluetooth;
import com.zhuchao.android.bt.hw.ATBluetooth.TObject;

import java.util.ArrayList;

public class VoicRecognitionView {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager.LayoutParams mLayoutParamsMicButton;

    private View mViewSpeech;
    private View mViewMicButton;
    private TextView mTitle;
    private boolean isShowSpeech = false;
    private boolean isShowMicButton = false;
    private Context mContext;

    public static VoicRecognitionView mThis;

    public static VoicRecognitionView getInstance(Context context) {
        if (mThis != null) {
            mThis.hideSpeech();
            mThis.hideMicButton();
            mThis = null;
        }
        // if (mThis == null) {
        mThis = new VoicRecognitionView(context);
        // }
        return mThis;
    }

    public static VoicRecognitionView getInstance() {
        return mThis;
    }

    private ListView mList;
    private MyListAdapterEx mAdapterPhonebook;
    private ArrayList<PhoneBook> mPB = new ArrayList<PhoneBook>();
    ;

    public VoicRecognitionView(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams(660, 380, 0, 0, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.RGBA_8888);
        mLayoutParams.gravity = Gravity.CENTER;
        // mLayoutParams.alpha = 0.92f;

        mLayoutParamsMicButton = new WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0, LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGBA_8888);
        mLayoutParamsMicButton.gravity = Gravity.TOP | Gravity.LEFT;
        mLayoutParamsMicButton.alpha = 0.80f;

        DisplayManager displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays();

        if (display[0].getWidth() == 800) {
            mLayoutParamsMicButton.x = 800 - 90;
            mLayoutParamsMicButton.y = 480 - 240;
        } else if (display[0].getWidth() == 1024) {
            mLayoutParamsMicButton.x = 1024 - 90;
            mLayoutParamsMicButton.y = 600 - 260;

        }

        mViewSpeech = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.speech, null);

        mList = (ListView) mViewSpeech.findViewById(R.id.t_list);
        mTitle = (TextView) mViewSpeech.findViewById(R.id.speech_title);
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < mPB.size()) {
                    PhoneBook pb = mPB.get(position);
                    if (pb.mNumber != null) {
                        mATBluetooth.write(ATBluetooth.REQUEST_CALL, pb.mNumber);
                        // Util.doSleep(300);
                        // hideSpeech();
                    }
                }
            }
        });
        mAdapterPhonebook = new MyListAdapterEx(mContext, mPB);

        mViewMicButton = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.mic_button, null);

        Drawable d = Drawable.createFromPath("/mnt/paramter/icon/mic_button.png");
        if (d != null) {
            ((ImageView) mViewMicButton.findViewById(R.id.mic_button)).setImageDrawable(d);
        }

        mViewMicButton.findViewById(R.id.mic_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                showSpeech(false);

            }
        });

        mViewMicButton.findViewById(R.id.mic_button).setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                float x = event.getRawX();
                float y = event.getRawY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchX = x;
                        mTouchY = y;
                        mOldX = mLayoutParamsMicButton.x;
                        mOldY = mLayoutParamsMicButton.y;
                        mHaveMove = false;
                        v.setAlpha(0.5f);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Log.d("dd", ""+mHaveMove);
                        if (((x - mTouchX) != 0) || ((y - mTouchY) != 0)) {
                            mHaveMove = true;
                        }

                        if (!mHaveMove) {
                            break;
                        }

                        mLayoutParamsMicButton.x = (int) (mOldX + x - mTouchX);
                        mLayoutParamsMicButton.y = (int) (mOldY + y - mTouchY);
                        if (mLayoutParamsMicButton.x < 0) {
                            mLayoutParamsMicButton.x = 0;
                        }
                        if (mLayoutParamsMicButton.y < 0) {
                            mLayoutParamsMicButton.y = 0;
                        }

                        mWindowManager.updateViewLayout(mViewMicButton, mLayoutParamsMicButton);

                        break;
                    case MotionEvent.ACTION_UP:
                        v.setAlpha(0.8f);
                        if (mHaveMove) {
                            return true;
                        }
                        break;
                }
                return false;
            }
        });

        mViewSpeech.findViewById(R.id.back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!isShowSpeech) {
                    showSpeech(false);
                } else {
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            hideSpeech();
                        }
                    }, 400);
                }
            }
        });

        mATBluetooth = ATBluetooth.create();

        AppConfig.updateHideAppConfig();

        mSystemUI = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);
        if (mSystemUI != null) {
            if (mSystemUI.equals(MachineConfig.VALUE_SYSTEM_UI_KLD7_1992)) {
                View v = mViewSpeech.findViewById(R.id.voice_textview_dail);
                if (v != null) {
                    v.setVisibility(View.GONE);
                }
            }
        }
    }

    private String mSystemUI;

    private final static int MIN_MOVE = 2;
    private float mTouchX;
    private float mTouchY;
    private int mOldX;
    private int mOldY;
    private boolean mHaveMove;
    private ATBluetooth mATBluetooth;
    private final static int MSG_TIMEOUT = 8000;
    private Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (mTimeToDial == 0) {
                String num = null;
                if (mPB != null && mPB.size() > 0) {
                    num = mPB.get(0).mNumber;
                }
                if (isShowSpeech && num != null) {
                    mATBluetooth.write(ATBluetooth.REQUEST_CALL, num);
                }
                hideSpeech();
            } else {
                --mTimeToDial;
                mTitle.setText(mContext.getString(R.string.speech_note1) + "  " + mTimeToDial + "s");
                mHandler.sendEmptyMessageDelayed(0, 1000);

            }
        }
    };

    public boolean showSpeech(boolean force) {

        if (ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) {
            return true;
        }

        if (!isShowMicButton && !force) {
            return false;
        } else if (force) {
            if (isHideMic()) {
                return false;
            }
        }

        if (!isShowSpeech) {
            isShowSpeech = true;
            mWindowManager.addView(mViewSpeech, mLayoutParams);
            BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.VOICE_ON);

            //			showPage(PAGE_MAIN);
            startSpeech();
        }
        return true;
    }

    public void hideSpeech() {
        Log.d("BtCmd", "hideSpeech:");
        mRecordingSeq = SEQ_NONE;
        mPrepareDialNum = null;
        if (isShowSpeech) {
            mHandler.removeMessages(0);
            mWindowManager.removeView(mViewSpeech);
            isShowSpeech = false;

            mRepeatTime = 0;
            mATBluetooth.write(ATBluetooth.REQUEST_RREC, "0");

            if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CONNECTED) {
                BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.BT_PHONE_STATUS, MyCmd.PhoneStatus.VOICE_OFF);
            }
        }
    }

    public boolean isHideMic() {
        boolean b = AppConfig.isHidePackage("com.zhuchao.android.bt.bt.VoiceControlActivity");
        if (b) {
            int ret = MachineConfig.getPropertyIntReadOnly(MachineConfig.KEY_BT_VOICE_BUTTON);
            if (ret != 0) {
                b = false;
            }
        }
        return b;
    }

    public void showMicButton() {
        if (!isShowMicButton && !isHideMic()) {
            isShowMicButton = true;
            mWindowManager.addView(mViewMicButton, mLayoutParamsMicButton);
        }
    }

    public void hideMicButton() {
        if (isShowMicButton) {
            hideSpeech();
            try {
                mWindowManager.removeView(mViewMicButton);
            } catch (Exception e) {

            }
            isShowMicButton = false;
        }
    }

    private void startSpeech() {

        if (mRecordingSeq == SEQ_CALL_START) {
            doCallPrepare();
        } else if (mRecordingSeq == SEQ_DIAL_START) {
            doDialContinue();
        } else {
            mATBluetooth.write(ATBluetooth.REQUEST_VOICE_STATUS);
            mATBluetooth.write(ATBluetooth.REQUEST_RREC, "1");
            Util.doSleep(300);
            mATBluetooth.write(ATBluetooth.REQUEST_RPRE, "1");
            Util.doSleep(300);
            mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1,1,3,1001");

            showPage(PAGE_MAIN);
        }
        // else if(mRecordingSeq == SEQ_DIAL_START){
        // doCallPrepare();
        // }

    }

    private int mRepeatTime = 0;
    private final static int MAX_REPEAT_TIME = 3;
    private ArrayList<PhoneBook> mPhoneBookInfo = ATBluetoothService.mPhoneBookInfo;

    private final static String RESULT_CALL = "101";
    private final static String RESULT_CALL_PREPARE = "102";
    private final static String RESULT_CALL_NAME = "201";
    private final static String RESULT_DIAL2 = "103";
    private final static String RESULT_DIAL = "104";
    private final static String RESULT_YES = "401";
    private final static String RESULT_DIAL_CONFIRM = "602";
    private final static String RESULT_CANCEAL = "9999";

    public String findResultCmdId(String result, String cmdId, String CmdValue, String retId) {
        int start = 0;
        int end = 0;
        if (result.startsWith("'")) {
            start = 1;
        }
        if (result.endsWith("'")) {
            end = result.length() - 1;
        }
        result = result.substring(start, end);
        String ret = null;
        boolean type = false;
        String ss[] = result.split("\\|");
        for (String pat : ss) {
            String sss[] = pat.split("=");
            if (sss.length >= 2) {
                if (sss[0].equals(cmdId) && sss[1].equals(CmdValue)) {
                    type = true;
                }
            }
        }

        if (type) {
            for (String pat : ss) {
                String sss[] = pat.split("=");
                if (sss.length >= 2) {
                    if (sss[0].equals(retId)) {
                        ret = sss[1];
                    }
                }
            }
        }

        return ret;
    }

    public String findResultCmd(String result) {
        String ret = null;
        if (result != null) {
            String ss[] = result.split(",");
            for (String pat : ss) {
                ret = findResultCmdId(pat, "type", "cmdid", "id");
                if (ret != null) {
                    break;
                }
            }
        }
        return ret;
    }

    public String findResultPhoneBook(String result) {
        String ret = null;
        if (result != null) {
            String ss[] = result.split(",");
            for (String pat : ss) {
                ret = findResultCmdId(pat, "type", "list_contacts", "id");
                if (ret != null) {
                    break;
                }
            }
        }
        return ret;
    }

    public String findResultNumber(String result) {
        String ret = null;
        if (result != null) {
            String ss[] = result.split(",");
            for (String pat : ss) {
                String number = findResultCmdId(pat, "type", "DigitSequence", "id");
                if (number != null) {
                    ret = "";
                    String sss[] = number.split(" ");
                    for (String n : sss) {
                        if (n.equals("10000")) {
                            ret += "0";
                        } else if (n.equals("10001")) {
                            ret += "1";
                        } else if (n.equals("10002")) {
                            ret += "2";
                        } else if (n.equals("10003")) {
                            ret += "3";
                        } else if (n.equals("10004")) {
                            ret += "4";
                        } else if (n.equals("10005")) {
                            ret += "5";
                        } else if (n.equals("10006")) {
                            ret += "6";
                        } else if (n.equals("10007")) {
                            ret += "7";
                        } else if (n.equals("10008")) {
                            ret += "8";
                        } else if (n.equals("10009")) {
                            ret += "9";
                        } else if (n.equals("9001")) {
                            ret += "+";
                        } else if (n.equals("9002")) {
                            ret += "#";
                        } else if (n.equals("9003")) {
                            ret += "*";
                        }
                    }

                    break;
                }
            }
        }
        return ret;
    }

    private final static int SEQ_NONE = 0;
    private final static int SEQ_CALL_START = 10;
    private final static int SEQ_CALL_NAME = 11;
    private final static int SEQ_DIAL_START = 21;
    private final static int SEQ_DIAL_COTINUE = 22;

    private int mRecordingSeq = 0;

    private void doCallPrepare() {
        // switch (mRecordingSeq) {
        // case SEQ_NONE:
        mRecordingSeq = SEQ_CALL_START;
        mPrepareDialNum = "";
        mPB.clear();
        mATBluetooth.write(ATBluetooth.REQUEST_RPRE, "2");
        Util.doSleep(10);
        mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1,1,3,1002");

        // break;
        // }
    }

    private String mPrepareDialNum = null;

    private void doCallName(String result, int index) {
        mRecordingSeq = SEQ_CALL_NAME;
        String ret = null;
        if (result != null) {
            String ss[] = result.split(",");
            String say = "";
            for (int i = 0; i < ss.length; ++i) {
                String pat = ss[i];
                ret = findResultCmdId(pat, "type", "list_contacts", "id");
                if (ret != null) {

                    String device = findResultCmdId(pat, "type", "list_contacts", "device");
                    say = "'type=list_contacts|device=" + device + "|id=" + ret + "'";

                    String pb_id = findResultPhoneBook(result);
                    String num = "";
                    if (pb_id != null) {
                        int id = -1;

                        try {
                            id = Integer.valueOf(pb_id);
                        } catch (Exception e) {
                            id = -1;
                        }
                        if (id != -1) {
                            for (PhoneBook pb : mPhoneBookInfo) {
                                if (pb.mIndex == id) {
                                    num = pb.mNumber;

                                    // add to list
                                    // num
                                    //									Log.d("ee", "find phone book" + pb.mIndex
                                    //											+ ":" + pb.mName + "" + pb.mNumber);
                                    int j = 0;
                                    for (; j < mPB.size(); ++j) {
                                        if (mPB.get(j).equals(pb)) {
                                            break;
                                        }
                                    }
                                    if (j >= mPB.size()) {
                                        mPB.add(pb);
                                    }
                                }
                            }
                        }

                    }

                    if (index == 1) {
                        mPrepareDialNum = num;
                        mATBluetooth.write(ATBluetooth.REQUEST_RPRE, "4");
                        Util.doSleep(10);

                        mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1,1,3,1004," + say + ",'type=list|id=3'");
                    }
                    // mATBluetooth.write(ATBluetooth.REQUEST_RPMT,
                    // "1004,'type=list_contacts|device=P2|id=7','type=list|id=3'");
                }
            }
        }

    }

    private void doDialPrepare() {


        mRecordingSeq = SEQ_DIAL_START;
        mPrepareDialNum = "";
        mATBluetooth.write(ATBluetooth.REQUEST_RPRE, "5");
        Util.doSleep(10);
        mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1,1,3,1005");

    }

    private void doDialContinue() {

        if (mPrepareDialNum != null) {
            String say = "";
            for (int i = 0; i < mPrepareDialNum.length(); ++i) {
                if (i != 0) {
                    say += " ";
                }
                say += mPrepareDialNum.charAt(i);


            }

            mATBluetooth.write(ATBluetooth.REQUEST_RPRE, "6");
            Util.doSleep(10);
            mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1,1,3,1006,'type=custom|text=" + say + "'");

            if (mPrepareDialNum.length() > 0) {
                if (mPB.size() == 0) {
                    PhoneBook pb = new PhoneBook();
                    mPB.add(pb);
                }
                PhoneBook pb = mPB.get(0);
                pb.mNumber = mPrepareDialNum;
                String name = SaveData.findName(mPhoneBookInfo, null, mPrepareDialNum);

                pb.mName = name;

                showPage(PAGE_LIST);
            }

        }

    }

    private void doYES() {
        if (mPrepareDialNum != null) {
            mATBluetooth.write(ATBluetooth.REQUEST_CALL, mPrepareDialNum);
            mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1,0,3,5004");
            hideSpeech();
        }

    }

    private void doDialNum(String result) {

        String ret = null;
        if (result != null) {
            String ss[] = result.split(",");
            String say = "";
            for (int i = 0; i < ss.length; ++i) {
                String pat = ss[i];
                if (i < 3) {
                    say += ss[i];
                    if (i == 2) {
                        say += "'";
                    }
                }
                ret = findResultCmdId(pat, "type", "list_contacts", "id");
                if (ret != null) {
                    mATBluetooth.write(ATBluetooth.REQUEST_RPRE, "3");
                    Util.doSleep(10);

                    // mATBluetooth.write(ATBluetooth.REQUEST_RPMT,
                    // "1003,"+pat);
                    mATBluetooth.write(ATBluetooth.REQUEST_RPMT, "1004,'type=list_contacts|device=P2|id=7','type=list|id=3'");
                }
            }
        }

    }

    public void returnResult(Message msg) {

        TObject obj = (TObject) msg.obj;

        Log.d("vv", msg.arg1 + ":" + obj.obj2 + ":" + obj.obj3 + ":" + isShowSpeech);

        if (!isShowSpeech) {
            return;
        }

        if (mRecordingSeq == SEQ_NONE) {

        }

        if (msg.arg1 > 0) {

            if (obj.obj2 != null) {
                int index = Integer.valueOf(((String) obj.obj2));

                if (index > 1 && mRecordingSeq != SEQ_CALL_NAME) {
                    return;
                }

                String cmd = findResultCmd(((String) obj.obj3));
                if (RESULT_CALL_PREPARE.equals(cmd) || RESULT_CALL.equals(cmd)) {

                    doCallPrepare();
                } else if (RESULT_CALL_NAME.equals(cmd)) {
                    //					if(index == 1){
                    doCallName(((String) obj.obj3), index);
                    //					} else {

                    //					}
                    if (msg.arg1 == index) {
                        if (mPB.size() > 0) {
                            showPage(PAGE_LIST);
                        }
                    }

                } else if (RESULT_DIAL2.equals(cmd) || RESULT_DIAL.equals(cmd)) {
                    if (MachineConfig.VALUE_SYSTEM_UI_KLD7_1992.equals(mSystemUI)) {
                        repeatSpeech();
                    } else {
                        doDialPrepare();
                    }


                } else if (RESULT_YES.equals(cmd) || RESULT_DIAL_CONFIRM.equals(cmd)) {
                    doYES();
                } else if (SEQ_DIAL_START == mRecordingSeq) {
                    if (MachineConfig.VALUE_SYSTEM_UI_KLD7_1992.equals(mSystemUI)) {
                        repeatSpeech();
                    } else {
                        String number = findResultNumber(((String) obj.obj3));

                        if (number != null) {
                            if (mPrepareDialNum == null) {
                                mPrepareDialNum = "";
                            }
                            mPrepareDialNum += number;
                            doDialContinue();
                        }
                    }
                } else if (RESULT_CANCEAL.equals(cmd)) {
                    hideSpeech();
                } else {
                    repeatSpeech();
                }
                // else if (RESULT_CALL.equals(cmd)) {
                // String pb_id = findResultPhoneBook(((String) obj.obj3));
                //
                // if (pb_id == null) {
                // // doCallPrepare("");
                // } else {
                // int id = -1;
                //
                // try {
                // id = Integer.valueOf(pb_id);
                // } catch (Exception e) {
                // id = -1;
                // }
                // if (id != -1) {
                // for (PhoneBook pb : mPhoneBookInfo) {
                // if (pb.mIndex == id) {
                // // add to list
                // // num
                // Log.d("ee", "find phone book" + pb.mIndex
                // + ":" + pb.mName + "" + pb.mNumber);
                // // if (msg.arg1 == 1) {
                // // num = pb.mNumber;
                // // } else {
                // int i = 0;
                // for (; i < mPB.size(); ++i) {
                // if (mPB.get(i).equals(pb)) {
                // break;
                // }
                // }
                // if (i >= mPB.size()) {
                // mPB.add(pb);
                // }
                // // }
                // }
                // }
                // }
                //
                // }
                // } else if (RESULT_DIAL.equals(cmd)) {
                // String number = findResultNumber(((String) obj.obj3));
                //
                // // int id = -1;
                // //
                // // try {
                // // id = Integer.valueOf(number);
                // // } catch (Exception e) {
                // // id = -1;
                // // }
                // // if(id!=-1){
                // PhoneBook pb = new PhoneBook(number, " ", null);
                // mPB.add(pb);
                // // }
                // }

                // Log.d("vv", cmd + ":" + msg.arg1 + ":" + index);
                // // if (msg.arg1 == 1) {
                // // mATBluetooth.write(ATBluetooth.REQUEST_CALL, num);
                // // hideSpeech();
                // // } else
                // if (msg.arg1 == index) {
                // if (mPB.size() > 0) {
                // showPage(PAGE_LIST);
                // mTimeToDial = 4;
                // mHandler.sendEmptyMessageDelayed(0, 200);
                // } else {
                // hideSpeech();
                // }
                // }
            } else {

            }

        } else {
            repeatSpeech();
        }

    }


    private void repeatSpeech() {
        if (mRepeatTime < MAX_REPEAT_TIME) {
            mRepeatTime++;
            startSpeech();
        } else {
            hideSpeech();
        }
    }

    private int mTimeToDial;

    public void diconnect() {
        hideMicButton();
        hideSpeech();
    }

    private int mPage = 0;
    private final static int PAGE_MAIN = 0;
    private final static int PAGE_LIST = 1;

    private void showPage(int page) {
        mPage = page;
        if (page == PAGE_MAIN) {
            mViewSpeech.findViewById(R.id.list).setVisibility(View.GONE);
            mViewSpeech.findViewById(R.id.main).setVisibility(View.VISIBLE);
            mTitle.setText(mContext.getString(R.string.help));

            mList.setAdapter(null);
            mPB.clear();
        } else if (page == PAGE_LIST) {

            mTitle.setText(mContext.getString(R.string.speech_note1));
            mList.setAdapter(mAdapterPhonebook);
            mAdapterPhonebook.notifyDataSetChanged();
            mViewSpeech.findViewById(R.id.list).setVisibility(View.VISIBLE);
            mViewSpeech.findViewById(R.id.main).setVisibility(View.GONE);
        }
    }

    private class MyListAdapterEx extends BaseAdapter {
        ArrayList<PhoneBook> mPb;
        private Context mContext;

        public MyListAdapterEx(Context context, ArrayList<PhoneBook> pb) {
            mContext = context;
            mPb = pb;
        }

        @Override
        public int getCount() {
            return mPb.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position, parent);
            return v;
        }

        private class ViewHolder {
            TextView name;
            TextView number;
            ImageView bt;
        }

        private View newView(ViewGroup parent) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.tr_list, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.name = (TextView) v.findViewById(R.id.name);
            vh.number = (TextView) v.findViewById(R.id.number);
            vh.bt = (ImageView) v.findViewById(R.id.bt);
            v.setTag(vh);
            return v;
        }

        private float textSize = 0;

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();

            PhoneBook book = null;
            if (mPb != null && position < mPb.size()) {

                book = mPb.get(position);
            }

            if (book != null) {
                String name = book.mName;
                if (name == null || name.length() == 0) {
                    name = mContext.getString(R.string.unknow);
                }

                vh.name.setText(name);
                vh.number.setText(book.mNumber);
                vh.number.setVisibility(View.VISIBLE);
                vh.bt.setVisibility(View.INVISIBLE);
            }

            if (textSize == 0) {
                textSize = vh.number.getTextSize();
            }

            if (position == 0) {
                //				vh.number.setScaleX(1.2f);
                //				vh.number.setScaleY(1.2f);
                //				vh.name.setScaleX(1.2f);
                //				vh.name.setScaleY(1.2f);
                //				vh.number.setTextSize(textSize + 0.02f);
                vh.number.setTextColor(0xffff0000);
                vh.name.setTextColor(0xffff0000);
                //				float ss = vh.number.getTextSize();
                //				vh.name.setTextSize(textSize + 0.02f);
            }
        }

    }
}
