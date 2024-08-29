package com.zhuchao.android.bt.bt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.common.util.Kernel;
import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.Util;
import com.common.util.UtilCarKey;
import com.common.util.UtilSystem;
import com.common.util.UtilSystem.StorageInfo;
import com.zhuchao.android.bt.R;
import com.zhuchao.android.bt.hw.ATBluetooth;
import com.zhuchao.android.bt.hw.ATBluetooth.TObject;
import com.zhuchao.android.bt.hw.Parrot;
import com.zhuchao.android.bt.view.SplitHScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ATBluetoothActivity extends Activity {
    private static final String TAG = "ATBluetoothActivity";
    private static final boolean DBG = false;

    private ATBluetooth mATBluetooth = null;

    private ImageView mTl0;
    private ImageView mTl1;
    private ImageView mTl2;
    private ImageView mTl3;
    private ListView mTList;
    private LinearLayout mTNormal;
    private LinearLayout mTrA2dp;
    private ListView mTrList;
    private LinearLayout mTrDial;
    public TextView mDigit;
    private RelativeLayout mTrListBg;
    private EditText mTrEdit;
    private ProgressBar mTrProgress;
    private TextView mDownloading;

    private Toast mToast;

    // private int mUpdateCallLogFlag = CALLLOG_FLAG_ALL;

    private static final int CALLLOG_FLAG_MISS = 0x1;
    private static final int CALLLOG_FLAG_OUT = 0x2;
    private static final int CALLLOG_FLAG_IN = 0x4;
    private static final int CALLLOG_FLAG_ALL = CALLLOG_FLAG_MISS | CALLLOG_FLAG_IN | CALLLOG_FLAG_OUT;

    private static final int MSG_UPDATE_PAIRED_LIST_HEIGHT = 0x100001;
    private Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case ATBluetooth.RETURN_CALLLOG_DATA:
                        if (mUI == R.id.calllog) {
                            switch (ATBluetoothService.GetAutoDownCalllogType()) {
                                case ATBluetoothService.DOWN_CALLLOG_ANSWER:
                                    if (mAdapterCallR != null) {
                                        mAdapterCallR.notifyDataSetChanged();
                                    }
                                    break;
                                case ATBluetoothService.DOWN_CALLLOG_OUT:
                                    if (mAdapterCallO != null) {
                                        mAdapterCallO.notifyDataSetChanged();
                                    }
                                    break;
                                case ATBluetoothService.DOWN_CALLLOG_MISS:
                                    if (mAdapterCallM != null) {
                                        mAdapterCallM.notifyDataSetChanged();
                                    }
                                    break;
                            }
                            Log.d("eeff", ":RETURN_CALLLOG_DATA");
                        }
                        break;
                    case ATBluetooth.RETURN_AUTOANSWER:
                        mSettingValueX[2] = msg.arg1;
                        mSettingValue[2] = getResources().getStringArray(mSettingArray[2])[mSettingValueX[2]];
                        mAdapter2.notifyDataSetChanged();

                        setSwitch(R.id.autoanswercheck, mSettingValueX[2]);
                        break;
                    case ATBluetooth.RETURN_AUTOCONNECT:

                        mSettingValueX[3] = msg.arg1;
                        mSettingValue[3] = getResources().getStringArray(mSettingArray[3])[mSettingValueX[3]];
                        mAdapter2.notifyDataSetChanged();

                        setSwitch(R.id.autoconnectcheck, mSettingValueX[3]);
                        break;
                    case ATBluetooth.RETURN_SETTING:
                        mSettingValueX[2] = msg.arg1 & 0xff;
                        mSettingValueX[3] = (msg.arg1 >> 8) & 0xff;
                        mSettingValue[2] = getResources().getStringArray(mSettingArray[2])[mSettingValueX[2]];
                        mSettingValue[3] = getResources().getStringArray(mSettingArray[3])[mSettingValueX[3]];
                        mAdapter2.notifyDataSetChanged();

                        setSwitch(R.id.autoanswercheck, mSettingValueX[2]);
                        setSwitch(R.id.autoconnectcheck, mSettingValueX[3]);
                        break;
                    case ATBluetooth.RETURN_PIN:
                        if (ATBluetooth.mShowPin) {
                            mSettingValue[1] = (String) ((ATBluetooth.TObject) msg.obj).obj2;
                            mAdapter2.notifyDataSetChanged();
                            updateConnectView();
                            setTextView(R.id.btpin, mSettingValue[1]);
                        }

                        break;
                    case ATBluetooth.RETURN_NAME:
                        mSettingValue[0] = (String) ((ATBluetooth.TObject) msg.obj).obj2;
                        mAdapter2.notifyDataSetChanged();
                        updateConnectView();

                        setTextView(R.id.btname, mSettingValue[0]);
                        break;
                    case ATBluetooth.RETURN_VOICE_SWITCH: {
                        if (mUI == R.id.dial && (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CALLED)) {
                            View v = findViewById(R.id.tl_2);
                            if (mATBluetooth.getVoiceSwitchLocal()) {
                                ((ImageView) v).setImageResource(R.drawable.voice);
                            } else {
                                ((ImageView) v).setImageResource(R.drawable.voice1);
                            }
                        }

                        break;
                    }
                    case ATBluetooth.RETURN_CALL_INCOMING:
                    case ATBluetooth.RETURN_CALL_OUT: {
                        if (!mPausing) {
                            ATBluetooth.TObject obj = (ATBluetooth.TObject) msg.obj;

                            updateCallingNumber((String) obj.obj2);
                            // String name = SaveData.findName(mPhoneBookInfo,
                            // (String) obj.obj2);
                            //
                            // if (!MachineConfig.VALUE_SYSTEM_UI20_RM10_1
                            // .equals(ResourceUtil.mSystemUI)) {
                            // if (name == null) {
                            // name = SaveData.findName(mPhoneBookSimInfo,
                            // (String) obj.obj2);
                            // }
                            // if (name == null) {
                            // mDigit.setText((String) obj.obj2);
                            // } else {
                            // mDigit.setText(name + " " + (String) obj.obj2);
                            // }
                            //
                            // } else {
                            // View v = findViewById(R.id.dial_info);
                            // if (v != null) {
                            // v.setVisibility(View.VISIBLE);
                            // }
                            //
                            // if (name == null) {
                            // name = (String) obj.obj2;
                            // }
                            //
                            // TextView tvName =(TextView)
                            // findViewById(R.id.calling_name);
                            // tvName.setText(name);
                            // TextView tvNumber =(TextView)
                            // findViewById(R.id.calling_number);
                            // tvNumber.setText((String) obj.obj2);
                            //
                            // v = findViewById(R.id.pbsearchlist);
                            // v.setVisibility(View.GONE);
                            //
                            //
                            // }
                            //
                            // if ((mLastUI == -1) && (mUI != R.id.dial)) {
                            // mLastUI = mUI;
                            // showUI(R.id.dial);
                            // }
                            //
                            // //
                            // mAdapterCallO.notifyDataSetChanged();
                            // if (msg.what == ATBluetooth.RETURN_CALL_OUT){
                            // startUpdateTime(R.string.dialing);
                            // } else {
                            // startUpdateTime(R.string.incoming);
                            // }

                        }
                    }
                    break;
                    case ATBluetooth.RETURN_CALLING: {
                        if (!mPausing) {
                            if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {

                                mAdapterCallR.notifyDataSetChanged();
                            }
                            startUpdateTime(0);
                        }
                        break;
                    }
                    case ATBluetooth.RETURN_CALL_END: {

                        resetCallEndUI();

                        if (mLastUI != -1) {
                            showUI(mLastUI);
                            mLastUI = -1;
                        }
                        mDigit.setText("");
                        stopUpdateTime();

                        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
                            mAdapterCallM.notifyDataSetChanged();
                        }

                        View v = findViewById(R.id.dial_info);
                        if (v != null) {
                            v.setVisibility(View.GONE);
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_PHONEBOOK_SIZE: {
                        if (msg.arg1 > 0) {
                            String s = Parrot.mDeviceIndex + ",1," + msg.arg1;
                            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK_SYNC, s);

                            mDownloading.setVisibility(View.VISIBLE);
                            mDownload = 1;

                            mPhoneBookInfo.clear();

                            mAdapterPhonebook.notifyDataSetChanged();
                            // ATBluetoothService.showMicButton(false);
                        } else {
                            if (Parrot.mSyncPhoneBookeStatus != -1 && Parrot.mSyncPhoneBookeStatus != 7) {
                                mDownload = 1;
                                mDownloading.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_PPDS: {
                        ATBluetooth.TObject obj = (ATBluetooth.TObject) msg.obj;
                        String s = ((String) obj.obj2);

                        if (Parrot.mSyncPhoneBookeStatus != 0) {
                            s = getString(R.string.updating);
                        } else {
                            s = getString(R.string.syncing);
                        }

                        mDownloading.setText(s + ":" + msg.arg1);

                    }
                    break;
                    case ATBluetooth.RETURN_PPBU: {
                        if (msg.arg1 == 7) {
                            if (mDownload == 1) {
                                mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK_SIZE, Parrot.mDeviceIndex);
                            }
                        }
                    }
                    break;
                    case ATBluetooth.RETURN_PHONE_BOOK_START: {

                        if (ATBluetooth.mAutoDownLoadCallLog) {
                            if (!mSendDownloadPhoneCmd) {
                                return;
                            }
                        }
                        mDownloading.setVisibility(View.VISIBLE);
                        switch (mPhoneBook) {
                            case 0:
                                if (mDownload == 0) {
                                    mPhoneBookInfo.clear();

                                    mAdapterPhonebook.notifyDataSetChanged();
                                }
                                mDownload = 1;

                                break;
                            case 1:
                                // if (mDownload == 0) {
                                // mPhoneBookSimInfo.clear();
                                // }
                                mDownload = 1;

                                break;
                        }

                        break;
                    }
                    case ATBluetooth.RETURN_PHONE_BOOK_DATA: {
                        addPhoneBook(msg);

                        break;
                    }
                    case ATBluetooth.RETURN_PHONE_BOOK: {
                        // mDownload = msg.arg1;
                        // if (mDownload != 1) {
                        // savePhoneBook();
                        continueDownPhoneBook();
                        // switch (mPhoneBook) {
                        // case 0:
                        // if (mDownPhonebookNew == 1) {
                        // mDownPhonebookNew = 0;
                        // SaveData.savePhoneBookData(mPhoneBookInfo,
                        // ATBluetooth.mCurrentMac);
                        // Log.d("allen", "size:"+mPhoneBookInfo.size());
                        // mToast.setText(getString(R.string.saving_phone_book));
                        // } else {
                        // mToast.setText(getString(R.string.total) + " "
                        // + mPhoneBookInfo.size() + " "
                        // + getString(R.string.record));
                        // }
                        // mToast.show();
                        // break;
                        // case 1:
                        //
                        // if (mDownPhonebookNew == 2) {
                        // mDownPhonebookNew = 0;
                        // SaveData.savePhoneBookData(mPhoneBookInfo,
                        // ATBluetooth.mCurrentMac + "sim");
                        //
                        // mToast.setText(getString(R.string.saving_phone_book));
                        // } else {
                        // mToast.setText(getString(R.string.total) + " "
                        // + mPhoneBookSimInfo.size() + " "
                        // + getString(R.string.record));
                        //
                        // }
                        // mToast.show();
                        // break;
                        // }
                        // }
                        break;
                    }
                    // case ATBluetooth.RETURN_HFP: {
                    // if (ATBluetooth.ATBluetooth.mCurrentHFP != msg.arg1) {
                    // ATBluetooth.ATBluetooth.mCurrentHFP = msg.arg1;
                    // if (ATBluetooth.ATBluetooth.mCurrentHFP == 2) {
                    // mATBluetooth.write(ATBluetooth.GET_PAIR_INFO, 0x01);
                    // } else {
                    // mCall = 0;
                    // mDigit.setText("");
                    // ATBluetooth.mCurrentMac = null;
                    // mPhoneBookInfo.clear();
                    // mPhoneBookSimInfo.clear();
                    // mCalllogMInfo.clear();
                    // mCalllogRInfo.clear();
                    // mCalllogOInfo.clear();
                    // mAdapter.notifyDataSetChanged();
                    // switch (mUI) {
                    // case R.id.dial:
                    // case R.id.calllog:
                    // case R.id.phonebook:
                    // case R.id.a2dp:
                    // showUI(R.id.pair);
                    // break;
                    // }
                    // mATBluetooth.write(ATBluetooth.GET_PAIR_INFO, 0x02);
                    // }
                    // }
                    // break;
                    // }
                    case ATBluetooth.RETURN_DEVICE_INDEX:
                    case ATBluetooth.RETURN_PAIR_INFO: {
                        // TObject obj = (TObject) msg.obj;
                        //
                        // int index = msg.arg1&0xff;
                        // if (index == 1) {
                        // mPairInfo.clear();
                        // }
                        // // check if exist
                        // int i;
                        // for (i = 0; i < mPairInfo.size(); ++i) {
                        // PhoneBook pb = mPairInfo.get(i);
                        // if (pb.mNumber != null && pb.mNumber.equals(obj.obj2)
                        // && pb.mName != null
                        // && pb.mName.equals(obj.obj3)) {
                        //
                        // break;
                        // }
                        // }
                        // if (i >= mPairInfo.size()) {
                        // mPairInfo.add(new PhoneBook((String) obj.obj2,
                        // (String) obj.obj3, ""+((msg.arg1&0xff00)>>8)));

                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                        if (mAdapterPaired != null) {
                            removePaired();
                            mAdapterPaired.notifyDataSetChanged();
                        }
                        // }
                        break;
                    }
                    case ATBluetooth.RETURN_CONNECTED_MAC:
                        if (msg.arg1 != ATBluetooth.HFP_INFO_CONNECTED) {
                            break;
                        }
                    case ATBluetooth.RETURN_HFP_INFO: {
                        if (msg.arg1 == ATBluetooth.HFP_INFO_CONNECTED) {
                            String obj2 = (String) ((ATBluetooth.TObject) msg.obj).obj2;
                            // if( obj2 == null){
                            // String obj3 = (String) ((TObject) msg.obj).obj3;
                            // if(obj3!=null){
                            // try {
                            // int index = Integer.valueOf(obj3) - 1;
                            // if (index > 0 && index <mPairInfo.size()){
                            // obj2 = mPairInfo.get(index).mNumber;
                            // }
                            // } catch (Exception e) {
                            //
                            // }
                            // }
                            // }
                            if (obj2 != null) {
                                //							ATBluetooth.mCurrentMac = obj2;
                                // setButtonEnable(true);
                            }
                            if (mSearch) {
                                stopSearch();
                                mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                            }

                        } else {
                            // ATBluetooth.mCurrentMac = null;
                            // setButtonEnable(false);
                        }

                        // if (ATBluetooth.mCurrentHFP != msg.arg1) {
                        // ATBluetooth.mCurrentHFP = msg.arg1;

                        updateConnectView();
                        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INITIAL) {
                            mDigit.setText("");
                            // ATBluetooth.mCurrentMac = null;
                        }
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }

                        if (mAdapterPhonebook != null) {
                            mAdapterPhonebook.notifyDataSetChanged();
                        }

                        if (mAdapterPhonebookSim != null) {
                            mAdapterPhonebookSim.notifyDataSetChanged();
                        }

                        if (mAdapterCallM != null) {
                            mAdapterCallM.notifyDataSetChanged();
                        }

                        if (mAdapterCallR != null) {
                            mAdapterCallR.notifyDataSetChanged();
                        }

                        if (mAdapterCallO != null) {
                            mAdapterCallO.notifyDataSetChanged();
                        }
                        if (msg.arg1 < ATBluetooth.HFP_INFO_CONNECTED) {
                            stopDownPhoneBook();
                            mDownloading.setVisibility(View.GONE);
                        }
                        // switch (mUI) {
                        // case R.id.dial:
                        // case R.id.calllog:
                        // case R.id.phonebook:
                        // case R.id.a2dp:
                        // showUI(R.id.pair);
                        // break;
                        // }

                        // }
                        break;
                    }
                    case ATBluetooth.RETURN_SEARCH: {
                        // if (msg.obj != null) {
                        // TObject obj = (TObject) msg.obj;
                        //
                        // int i;
                        // for (i = 0; i < mSearchInfo.size(); ++i) {
                        // PhoneBook pb = mSearchInfo.get(i);
                        // if (pb.mNumber != null
                        // && pb.mNumber.equals(obj.obj2)
                        // && pb.mName != null
                        // && pb.mName.equals(obj.obj3)) {
                        //
                        // break;
                        // }
                        // }
                        //
                        // if (i >= mSearchInfo.size()) {
                        // mSearchInfo.add(new PhoneBook((String) obj.obj2,
                        // (String) obj.obj3, msg.arg1+""));
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                        if (mAdapterPairAvailable != null) {
                            removePaired();
                            // mAdapterPairAvailable.notifyDataSetChanged();
                        }
                        // }
                        //
                        // }

                        break;
                    }

                    case ATBluetooth.RETURN_SEARCH_START: {
                        mSearchInfo.clear();
                        break;
                    }
                    case ATBluetooth.RETURN_SEARCH_END: {
                        mSearch = false;
                        mTrProgress.setVisibility(View.INVISIBLE);
                        break;
                    }
                    case ATBluetooth.RETURN_CMD_ERROR: {
                        // Log.d("ff", ""+msg.arg1);
                        if (ATBluetooth.REQUEST_SEARCH == msg.arg1) {
                            doParrotErrSearch();
                        }
                        break;
                    }
                    case MSG_PARROT_ERR_SEARCH:
                        mATBluetooth.write(ATBluetooth.REQUEST_SEARCH);
                        break;
                    case ATBluetooth.RETURN_PHONE_BOOK_CONNECT_STATUS: {
                        if (msg.arg1 != ATBluetooth.PHONE_BOOK_CONNECT_STATUS_OK && mDownload == 1) {
                            mToast.setText(getString(R.string.phone_book_connect_fail));
                            mToast.show();
                        }

                        break;
                    }
                    case ATBluetooth.REQUEST_REQUEST_VERSION:
                        if (mRequestVersion) {
                            String obj2 = (String) ((TObject) msg.obj).obj2;
                            if (obj2 != null) {
                                showDialogId(R.string.version, obj2);
                            }
                            mRequestVersion = false;
                        }
                        break;
                    case ATBluetooth.RETURN_CLEAR_PAIR:
                        clearPairLayout();
                        break;
                    case ATBluetooth.RETURN_PAIR_ADDR:
                        if (msg.arg1 == 1) {
                            if (msg.obj != null) {
                                TObject obj = (TObject) msg.obj;
                                for (int i = 0; i < mSearchInfo.size(); ++i) {
                                    PhoneBook pb = mSearchInfo.get(i);

                                    if (pb.mNumber != null && pb.mNumber.equals(obj.obj2)) {
                                        if ("1".equals(pb.mPinyin)) {
                                            clearPairLayout();
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case MSG_UPDATE_PAIRED_LIST_HEIGHT:
                        if (mPrePairedCount != msg.arg1) {
                            mPrePairedCount = msg.arg1;
                            updatePairedListHeight();
                        }
                        break;
                    case ATBluetooth.RETURN_3CALL_START: {
                        String obj2 = (String) ((TObject) msg.obj).obj2;
                        do3CallCome(msg.arg1, obj2);
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
                }

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    };

    private void doParrotErrSearch() {
        Log.d(TAG, mStartSearch + "check err:");
        if (mStartSearch >= 8) {
            mSearch = false;
            mTrProgress.setVisibility(View.INVISIBLE);
        } else if (mSearch) {
            ++mStartSearch;
            mHandler.sendEmptyMessageDelayed(MSG_PARROT_ERR_SEARCH, 500);
        }
    }

    private final static int MSG_PARROT_ERR_SEARCH = ATBluetooth.RETURN_MSG_MAX + 100;

    private void updateConnectView() {

        View v;
        TextView tv = (TextView) findViewById(R.id.disconnect_info);
        if (tv != null) {
            v = findViewById(R.id.disconnect_hide_info);
            if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED) {

                String s = String.format(getString(R.string.a2dp_disconnect_info), mATBluetooth.mBtName);

                s += String.format(getString(R.string.a2dp_disconnect_info2), mATBluetooth.mBtPin);
                tv.setText(s);
                tv.setVisibility(View.VISIBLE);

                if (v != null) {
                    v.setVisibility(View.GONE);
                }
                v = findViewById(R.id.device_info_layout);
                if (v != null) {
                    v.setVisibility(View.VISIBLE);
                    showUI(R.id.pair);
                }

                updatePairLayout();
            } else {
                tv.setVisibility(View.GONE);
                if (v != null) {
                    v.setVisibility(View.VISIBLE);
                }
                v = findViewById(R.id.device_info_layout);
                if (v != null) {
                    v.setVisibility(View.GONE);
                }
                clearPairLayout();
            }
        }
        v = findViewById(R.id.tv_devicename);
        if (v != null) {
            ((TextView) v).setText(mATBluetooth.mBtName);
        }
        v = findViewById(R.id.tv_pincode);
        if (v != null) {
            ((TextView) v).setText(mATBluetooth.mBtPin);
        }

        v = findViewById(R.id.tv_con_devicename);
        if (v != null) {
            ((TextView) v).setText(mATBluetooth.mBtName);
        }
        v = findViewById(R.id.tv_con_pincode);
        if (v != null) {
            ((TextView) v).setText(mATBluetooth.mBtPin);
        }

        v = findViewById(R.id.tv_con_phonename);
        if (v != null) {
            ((TextView) v).setText(findConectName());
        }

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

    public boolean mStartByA2DP = false;

    private void updateIntent(Intent it) {
        if (it != null) {
            int music = it.getIntExtra("music", 0);
            // music = 1;
            Log.d("fkkk", "!!!!!!!!!!:" + music);
            if (music == 1) {
                mStartByA2DP = true;
                if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
                    // showUI(R.id.dial);
                    showUI(R.id.a2dp);
                } else {
                    showUI(R.id.pair);
                }

            } else {
                Uri handle = it.getData();
                if (handle != null) {
                    String number = PhoneNumberUtils.getNumberFromIntent(it, this);
                    Log.d(TAG, "number:" + number);
                    if (number != null) {
                        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
                            showUI(R.id.dial);
                            mATBluetooth.write(ATBluetooth.REQUEST_CALL, number);
                            if (mDigit != null) {
                                mDigit.setText(number);
                            }
                        }
                    }

                }

                int page = it.getIntExtra("page", 0);
                if (page != 0) {
                    switch (page) {
                        case 1:
                            page = R.id.dial;
                            break;
                        case 2:
                            page = R.id.setting;
                            break;
                        default:
                            page = 0;
                            break;
                    }
                    if (page != 0) {
                        showUI(page);
                    }
                }

            }
        }
    }

    @Override
    protected void onNewIntent(Intent it) {
        // updateIntent(it);
        setIntent(it);
    }

    String system_ui;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Configuration c = getResources().getConfiguration();
        // c.smallestScreenWidthDp = 321;
        //
        // getResources().updateConfiguration(c, null);
        system_ui = ResourceUtil.updateAppUi(this);
        GlobalDef.initParamter(this);


        super.onCreate(savedInstanceState);
        if (ATBluetoothService.mBTCoreCrash) {
            finish();
        }
        mPhoneBookInfo = ATBluetoothService.mPhoneBookInfo;
        mPhoneBookSimInfo = ATBluetoothService.mPhoneBookSimInfo;
        // getWindow().setFormat(PixelFormat.TRANSLUCENT);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        // setContentView(R.layout.atbluetooth);

        mATBluetooth = ATBluetooth.create();
        if (ATBluetooth.isSupport3Call()) {
            setContentView(R.layout.layout_split_scroll_3call);
        } else {
            setContentView(R.layout.layout_split_scroll);
        }
        SplitHScrollView fl = (SplitHScrollView) findViewById(R.id.hscrollview_container);
        fl.clearFocus();
        fl.setFocusable(false);
        android.view.ViewGroup.LayoutParams lp = fl.getLayoutParams();
        lp.width = fl.WIDTH;
        fl.setLayoutParams(lp);

        mTNormal = (LinearLayout) findViewById(R.id.t_normal);
        mTl0 = (ImageView) findViewById(R.id.tl_0);
        mTl1 = (ImageView) findViewById(R.id.tl_1);
        mTl2 = (ImageView) findViewById(R.id.tl_2);
        mTl3 = (ImageView) findViewById(R.id.tl_3);
        mTrA2dp = (LinearLayout) findViewById(R.id.tr_a2dp);
        mTrListBg = (RelativeLayout) findViewById(R.id.tr_list_bg);
        mTrProgress = (ProgressBar) findViewById(R.id.tr_progress);
        mTrDial = (LinearLayout) findViewById(R.id.tr_dial);
        mDigit = (TextView) findViewById(R.id.digit);

        m3CallLayout = findViewById(R.id.layout_3call);
        m3CallTextView = (TextView) findViewById(R.id.digit_3call);
        m3CallTextName = (TextView) findViewById(R.id.dial_name_3call);
        m3CallStatus = (TextView) findViewById(R.id.dial_status_3call);

        intDialNumSearch();
        initLayoutPair();
        initLayoutPhonebook();
        initLayoutCallLogs();

        mTrEdit = (EditText) findViewById(R.id.tr_edit);
        mDownloading = (TextView) findViewById(R.id.downloading);
        if (mTrEdit != null) {
            mTrEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if ((mFilter != null) && (mFilter.getStatus() != AsyncTask.Status.FINISHED)) {
                        mFilter.cancel(true);
                    }
                    if (s != null && s.length() > 0) {
                        mFilter = (PhonebookFilter) new PhonebookFilter().execute(s);
                    } else {
                        if (mListViewPhonebook != null) {
                            mListViewPhonebook.setAdapter(mAdapterPhonebook);
                        } else {
                            if (mUI == R.id.phonebook) {
                                mTrList.setAdapter(mAdapterPhonebook);
                            }
                        }
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        (findViewById(R.id.n0)).setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CALLED) {
                    mDigit.setText(mDigit.getText() + "+");
                } else {
                    mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "+");
                }
                return true;
            }
        });
        (findViewById(R.id.delete)).setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CALLED) {
                    mDigit.setText("");
                } else {
                    if (isUISupport3Call()) {
                        if (ATBluetooth.mCallingNum > 1) {
                            mDigit.setText("");
                        }
                    }
                }
                return true;
            }
        });
        if (GlobalDef.mFavorite == 1) {
            View v = findViewById(R.id.favorite);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
            v = findViewById(R.id.favorite_line);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
        }

        mTrList = (ListView) findViewById(R.id.tr_list);

        if (mTrList != null) {

            if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(system_ui) || MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(system_ui) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(system_ui)) {
                mTrList.setOnItemLongClickListener(mOnLongClickListCalllogListener);
            } else {
                mTrList.setOnItemLongClickListener(mOnLongClickListListener);
            }

            mTrList.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    doClickListDialDirect(position);
                }
            });
        }
        mAdapter = new MyListAdapter(this);

        mAdapterPhonebook = new MyListAdapterEx(this, mPhoneBookInfo);
        mAdapterPhonebookSim = new MyListAdapterEx(this, mPhoneBookSimInfo);

        if (mLayoutCallLogs == null) {
            mAdapterCallM = new MyListAdapterEx(this, mCalllogMInfo);
            mAdapterCallO = new MyListAdapterEx(this, mCalllogOInfo);
            mAdapterCallR = new MyListAdapterEx(this, mCalllogRInfo);
        }

        mTList = (ListView) findViewById(R.id.t_list);
        mTList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int index = position;
                if (mSettingArray[position] == 0) {
                    LayoutInflater factory = LayoutInflater.from(ATBluetoothActivity.this);
                    final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
                    ((TextView) textEntryView.findViewById(R.id.edit)).setText(mSettingValue[position]);

                    if (mSettingName[position] == R.string.pin) {
                        if (!ATBluetooth.mSuppotEditPin) {
                            return;
                        }
                        ((TextView) textEntryView.findViewById(R.id.edit)).setInputType(EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
                    }

                    Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(ATBluetoothActivity.this, R.style.AlertDialogCustom))

                            .setTitle(mSettingName[position]).setView(textEntryView).setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mSettingValue[index] = ((TextView) textEntryView.findViewById(R.id.edit)).getText().toString();
                                    if (mSettingName[index] == R.string.device_name) {
                                        mATBluetooth.write(ATBluetooth.REQUEST_NAME, mSettingValue[index]);
                                    } else if (mSettingName[index] == R.string.pin) {
                                        mATBluetooth.write(ATBluetooth.REQUEST_PIN, mSettingValue[index]);
                                    }
                                    mAdapter2.notifyDataSetChanged();
                                }
                            }).setNegativeButton(R.string.alert_dialog_cancel, null).show();
                    // LayoutParams lp = d.getWindow().getAttributes();
                    // lp.dimAmount = 0.0f;
                    // d.getWindow().setAttributes(lp);
                } else {
                    Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(ATBluetoothActivity.this, R.style.AlertDialogCustom))

                            .setTitle(mSettingName[position]).setSingleChoiceItems(mSettingArray[position], mSettingValueX[position], new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mSettingValueX[index] = which;
                                    mSettingValue[index] = getResources().getStringArray(mSettingArray[index])[mSettingValueX[index]];
                                    if (mSettingName[index] == R.string.auto_connect) {
                                        if (which == 0) {
                                            mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE);
                                        } else {
                                            mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE);
                                        }
                                        // mATBluetooth
                                        // .write(ATBluetooth.REQUEST_AUTO_CONNECT,
                                        // mSettingValueX[index]);
                                    } else if (mSettingName[index] == R.string.auto_answer) {
                                        if (which == 0) {
                                            mATBluetooth.write(ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE);
                                        } else {
                                            mATBluetooth.write(ATBluetooth.REQUEST_AUTO_ANSWER_ENABLE);
                                        }
                                        // mATBluetooth
                                        // .write(ATBluetooth.REQUEST_AUTO_ANSWER,
                                        // mSettingValueX[index]);
                                    }
                                    mAdapter2.notifyDataSetChanged();
                                }
                            }).setNegativeButton(R.string.alert_dialog_cancel, null).show();
                    // LayoutParams lp = d.getWindow().getAttributes();
                    // lp.dimAmount = 0.0f;
                    // d.getWindow().setAttributes(lp);
                }
            }
        });
        mAdapter2 = new MyListAdapter2(this);
        mTList.setAdapter(mAdapter2);
        mAdapter2.notifyDataSetChanged();

        //		mATBluetooth = ATBluetooth.create();
        // findViewById(R.id.back).setOnLongClickListener(
        // new OnLongClickListener() {
        // @Override
        // public boolean onLongClick(View v) {
        // mATBluetooth.requestSource(0);
        // finish();
        // return true;
        // }
        // });

        mSearchABCDEF = findViewById(R.id.abcdef_page);

        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CONNECTED) {
            showUI(R.id.dial);

        } else {
            showUI(R.id.pair);
        }

        // createDataBase();
        initDialog();
        mTextViewTime = (TextView) findViewById(R.id.dial_status);

        // View kb = findViewById(R.id.phonebook_keyboard);
        // if (kb!=null){
        // kb.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View arg0) {
        // // TODO Auto-generated method stub
        // // arg0.setVisibility(View.GONE);
        // }
        // });
        // }
    }

    @Override
    protected void onResume() {
        if (mUpdateUIResource) {
            ResourceUtil.updateAppUi(this);
            mUpdateUIResource = false;
        }
        super.onResume();
        if (ATBluetoothService.mBTCoreCrash) {
            return;
        }
        // mATBluetooth.addHandler(TAG, mHandler);

        // AppConfig.updateSystemBackground(this, getWindow().getDecorView()
        // .findViewById(android.R.id.content));
        mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
        // mATBluetooth.write(ATBluetooth.GET_HFP_INFO);
        // mATBluetooth.write(ATBluetooth.REQUEST_CALL, 0xff);
        if (mUI == R.id.a2dp) {
            // mATBluetooth.requestSource();
            BroadcastUtil.sendToCarServiceSetSource(this, MyCmd.SOURCE_BT_MUSIC);

            ATBluetoothService.musicKeyControl(MyCmd.Keycode.PLAY);

        }
        if (GlobalDef.mA2DPInside == 1) {
            updateId3();
        }
        mPausing = false;

        mATBluetooth.write(ATBluetooth.GET_HFP_INFO);
        mThis = this;
        mThis2 = this;
        mATBluetooth.setUIHandler(mHandler);

        updateIntent(getIntent());

        mGpsRunAfter = openGps(this, getIntent());
        setIntent(null);

        wakeLock();

        if (ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) {
            updateCallingNumber(ATBluetoothService.mCallNumber);
            ATBluetoothService.hideIncoming();
            if (isUISupport3Call()) {
                if (ATBluetooth.m3CallStatus > ATBluetooth.HFP_INFO_CONNECTED) {
                    m3CallLayout.setVisibility(View.VISIBLE);
                    String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, ATBluetooth.m3CallNumber);
                    String text = ATBluetooth.m3CallNumber;
                    if (name != null) {
                        text = name + " " + ATBluetooth.m3CallNumber;
                    }
                    m3CallTextView.setText(text);
                }
            }
        }
        updateConnectView();

        if (!ATBluetooth.mShowPin) {
            mSettingValue[1] = getString(R.string.auto);
            if (mAdapter2 != null) {
                mAdapter2.notifyDataSetChanged();
            }
            setTextView(R.id.btpin, mSettingValue[1]);
        }

        initNameAndPinForGoc();
    }

    private void updateCallingNumber(String number) {
        String name = SaveData.findName(mPhoneBookInfo, (String) number);
        if (!MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI)) {
            if (name == null) {
                name = SaveData.findName(mPhoneBookSimInfo, (String) number);
            }
            if (name == null) {
                mDigit.setText((String) number);
            } else {
                mDigit.setText(name + " " + (String) number);
            }

        } else {
            View v = findViewById(R.id.dial_info);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }

            if (name == null) {
                name = (String) number;
            }

            TextView tvName = (TextView) findViewById(R.id.calling_name);
            tvName.setText(name);
            TextView tvNumber = (TextView) findViewById(R.id.calling_number);
            tvNumber.setText((String) number);

            v = findViewById(R.id.pbsearchlist);
            v.setVisibility(View.GONE);

        }

        if ((mLastUI == -1) && (mUI != R.id.dial)) {
            mLastUI = mUI;
            showUI(R.id.dial);
        }

        //
        mAdapterCallO.notifyDataSetChanged();
        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLED) {
            startUpdateTime(R.string.dialing);
        } else if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
            startUpdateTime(R.string.incoming);
        } else {
            startUpdateTime(-1);
        }

    }

    public boolean openGps(Context c, Intent it) {
        if (it != null) {
            int gps = it.getIntExtra("gps", 0);
            if (gps != 0) {
                // Kernel.doKeyEvent(Kernel.KEY_HOMEPAGE);
                UtilCarKey.doKeyGps(c);
                return true;
            }
        }
        return false;
    }

    boolean mGpsRunAfter = false;
    ;
    public static boolean mPausing = true;
    public boolean mUpdateUIResource = false;

    @Override
    protected void onPause() {
        mThis = null;
        mPausing = true;
        // mATBluetooth.sendHandler(ATBluetoothService.TAG, 0);
        if (mDownload == 1) {
            stopDownPhoneBook();// end
            // // if (mPhoneBook == 0) {
            // // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa5);
            // // } else {
            // // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa4);
            // // }
            mDownload = 0;
            mDownloading.setVisibility(View.GONE);
        }
        stopUpdateTime();
        // mDownloading.setVisibility(View.GONE);
        // finish();
        // if (ATBluetooth.mCurrentHFP == 2) {
        // savePhoneBookData(mPhoneBookInfo, ATBluetooth.mCurrentMac);
        // savePhoneBookData(mPhoneBookSimInfo, ATBluetooth.mCurrentMac +
        // "sim");
        // }

        // mATBluetooth.removeHandler(TAG);

        if (mATBluetooth != null && ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTING) {

            mATBluetooth.write(ATBluetooth.GET_HFP_INFO);
            mATBluetooth.write(ATBluetooth.GET_HFP_INFO2);

        }

        stopSearch();
        Log.d("fkkk", mGpsRunAfter + "pause!!!!!!!!!!:" + mStartByA2DP);
        if (!mGpsRunAfter) {
            mStartByA2DP = false;
        } else {
            mGpsRunAfter = false;
        }
        Log.d("fkkk", mGpsRunAfter + "::2222pause!!!!!!!!!!:" + mStartByA2DP);
        super.onPause();
        wakeRelease();

        View v = findViewById(R.id.dial_info);
        if (v != null) {
            v.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {

        if (mDownload == 1) {
            stopDownPhoneBook();// end

            mDownload = 0;
        }

        if ((mFilter != null) && (mFilter.getStatus() != AsyncTask.Status.FINISHED)) {
            mFilter.cancel(true);
            mFilter = null;
        }

        if ((mUpdateTextTask != null) && (mUpdateTextTask.getStatus() != AsyncTask.Status.FINISHED)) {
            mUpdateTextTask.cancel(true);
            mUpdateTextTask = null;
        }

        mATBluetooth.setUIHandler(null);
        // mATBluetooth.destroy();
        mATBluetooth = null;

        super.onDestroy();

        if (mThis2 == this) {
            mThis2 = null;
        }
        if (GlobalDef.mA2DPInside == 1 && GlobalDef.isA2DPSource()) {
            BroadcastUtil.sendToCarServiceSetSource(this, MyCmd.SOURCE_MX51);
        }
    }

    private MyListAdapter2 mAdapter2;

    private class MyListAdapter2 extends BaseAdapter {
        public MyListAdapter2(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            if (ATBluetooth.mSuppotAutoAnswer) {
                return mSettingName.length;
            } else {
                return mSettingName.length - 1;
            }
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
            TextView value;
        }

        private View newView(ViewGroup parent) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.t_list, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.name = (TextView) v.findViewById(R.id.name);
            vh.value = (TextView) v.findViewById(R.id.value);
            v.setTag(vh);
            return v;
        }

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();
            try {
                if (mSettingValue[position] == null && position == 2) {// for
                    // parrot
                    position++;
                }
                vh.name.setText(mSettingName[position]);
                vh.value.setText(mSettingValue[position]);

            } catch (Exception e) {

            }
        }

        private Context mContext;
    }

    private class MyListAdapter extends BaseAdapter {
        public MyListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            if (mUI == R.id.pair) {
                switch (mPair) {
                    case 0:
                        return mPairInfo.size();
                    case 1:
                        return mSearchInfo.size();
                }
            }
            return 0;
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

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();
            if (mUI == R.id.pair) {
                switch (mPair) {
                    case 0: {
                        PhoneBook pair = mPairInfo.get(position);
                        if (pair != null) {
                            if ((pair.mName != null) && (pair.mName.length() != 0)) {
                                vh.name.setText(pair.mName);
                            } else {
                                vh.name.setText(pair.mNumber);
                            }
                            vh.number.setVisibility(View.INVISIBLE);

                            if (pair.mNumber.equals(ATBluetooth.mCurrentMac)) {
                                vh.bt.setVisibility(View.VISIBLE);
                            } else {
                                vh.bt.setVisibility(View.INVISIBLE);
                            }
                        }
                        break;
                    }
                    case 1: {
                        PhoneBook search = mSearchInfo.get(position);
                        if (search != null) {
                            if ((search.mName != null) && (search.mName.length() != 0)) {
                                vh.name.setText(search.mName);
                            } else {
                                vh.name.setText(search.mNumber);
                            }
                            vh.number.setVisibility(View.INVISIBLE);
                            if (isConnectDevice(search.mNumber)) {
                                vh.bt.setVisibility(View.VISIBLE);

                                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                                    if (search.mName == null || search.mName.equals(search.mNumber)) {
                                        search.mName = getParrotPairDeviceName(search.mNumber);
                                        if (search.mName != null) {
                                            vh.name.setText(search.mName);
                                        }
                                    }
                                }

                            } else {
                                if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI)) {
                                } else {
                                    vh.bt.setVisibility(View.INVISIBLE);
                                }
                            }

                        }
                        break;
                    }
                }
            }
        }

        private Context mContext;
    }

    private MyListAdapter mAdapter;
    private MyListAdapterEx mAdapterPhonebook;
    private MyListAdapterEx mAdapterPhonebookSim;
    private MyListAdapterEx mAdapterCallM;
    private MyListAdapterEx mAdapterCallR;
    private MyListAdapterEx mAdapterCallO;

    private MyListAdapterEx mAdapterPhonebookFilter;

    private AdapterPhoneBookFavorite mAdapterPhonebookFavorite;

    public class MyListAdapterEx extends BaseAdapter {
        ArrayList<PhoneBook> mPb;
        private Context mContext;

        public MyListAdapterEx(Context context, ArrayList<PhoneBook> pb) {
            mContext = context;
            mPb = pb;
            if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(ResourceUtil.mSystemUI)) {
                mLayoutId = R.layout.phonebook_list;
            }
        }

        public MyListAdapterEx(Context context, ArrayList<PhoneBook> pb, int layout) {
            mContext = context;
            mPb = pb;
            mLayoutId = layout;
        }

        private int mLayoutId = R.layout.tr_list;

        private int mPos = -2;

        public void setSelected(int position) {
            mPos = position;
        }

        public int getSelected() {
            return mPos;
        }

        public PhoneBook getPhoneBook(int index) {
            if (mPb != null && mPb.size() > index) {
                return mPb.get(index);
            }
            return null;
        }

        public ArrayList<PhoneBook> getPhoneBookList() {
            return mPb;
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

            if (mPos != -2) {
                if (position == mPos) {
                    v.setBackgroundResource(R.drawable.pair_selected);
                } else {
                    v.setBackground(null);
                }
            }

            return v;
        }

        private class ViewHolder {
            TextView name;
            TextView number;
            ImageView bt;
            View line;
            TextView abc;
            TextView time;
        }

        private View newView(ViewGroup parent) {
            View v;

            v = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);

            ViewHolder vh = new ViewHolder();
            vh.name = (TextView) v.findViewById(R.id.name);
            vh.number = (TextView) v.findViewById(R.id.number);
            vh.bt = (ImageView) v.findViewById(R.id.bt);

            vh.line = v.findViewById(R.id.phone_book_list_line);
            View vv = v.findViewById(R.id.abc_tag);
            if (vv != null) {
                vh.abc = (TextView) vv;
            }
            vv = v.findViewById(R.id.calllog_time);
            if (vv != null) {
                vh.time = (TextView) vv;
            }

            v.setTag(vh);
            return v;
        }

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();

            PhoneBook book = null;
            if (mPb != null && position < mPb.size()) {

                book = mPb.get(position);
            }

            if (book != null) {
                String name = book.mName;
                if (name == null || name.length() == 0) {
                    name = getString(R.string.unknow);
                }

                vh.number.setText(book.mNumber);
                vh.number.setVisibility(View.VISIBLE);
                if (vh.bt != null) {
                    vh.bt.setVisibility(View.INVISIBLE);
                }
                if (vh.abc != null) {
                    try {
                        char c = 0;
                        if (book.mPinyin != null && book.mPinyin.length() > 0) {
                            c = book.mPinyin.charAt(0);
                        }
                        boolean valid = isValidChar(c);
                        if (!valid) {
                            Log.d("cccd", position + ":" + book.mName + ":" + book.mNumber);
                        }
                        if (valid) {

                            if (position < (mPb.size() - 1)) {
                                PhoneBook pb = mPb.get(position + 1);
                                if (vh.line != null) {
                                    if (pb.mName.equals(name)) {
                                        vh.line.setVisibility(View.INVISIBLE);
                                    } else {
                                        vh.line.setVisibility(View.VISIBLE);
                                    }
                                }

                            }

                            // if (position == 610){
                            // Log.d("ccd", "ff");
                            // }
                            if (position > 0) {
                                PhoneBook pb = mPb.get(position - 1);
                                if (pb.mPinyin != null && pb.mPinyin.length() > 0 && (pb.mPinyin.charAt(0) == c)) {

                                    // }
                                    // if (pb.mPinyin.charAt(0) == c) {
                                    vh.abc.setVisibility(View.INVISIBLE);
                                } else {
                                    vh.abc.setVisibility(View.VISIBLE);
                                }

                                if (pb.mName.equals(name)) {
                                    name = "";
                                }

                            } else {
                                vh.abc.setVisibility(View.VISIBLE);
                            }

                            vh.abc.setText(c + "");
                        } else {
                            vh.abc.setVisibility(View.INVISIBLE);
                        }
                    } catch (Exception e) {
                        Log.d("ccd", position + "::" + e);
                    }
                }
                vh.name.setText(name);

                if (vh.time != null) {
                    vh.time.setText(book.mPinyin);
                }
            }

        }

    }

    private boolean isValidChar(char c) {
        int ci = (int) c;
        int ca = (int) 'A';
        int cz = (int) 'Z';

        if ((ci >= ca && ci <= cz)) {
            return true;
        }
        ca = (int) 'А';
        cz = (int) 'Я';

        if ((ci >= ca && ci <= cz)) {
            return true;
        }

        return false;
    }

    private PhonebookFilter mFilter = null;

    private class PhonebookFilter extends AsyncTask<CharSequence, Void, ArrayList<PhoneBook>> {
        @Override
        protected ArrayList<PhoneBook> doInBackground(CharSequence... params) {
            if (isCancelled()) return null;
            // ArrayList<PhoneBook> phonebookS = new ArrayList<PhoneBook>();

            ArrayList<PhoneBook> phonebookE = new ArrayList<PhoneBook>();

            ArrayList<PhoneBook> phonebook = new ArrayList<PhoneBook>();
            CharSequence prefix = params[0];
            if ((prefix != null) && (prefix.length() > 0)) {
                String prefixString = prefix.toString().toUpperCase();
                ArrayList<PhoneBook> phonebookInfo = null;
                switch (mPhoneBook) {
                    case 0:
                        phonebookInfo = mPhoneBookInfo;
                        break;
                    case 1:
                        phonebookInfo = mPhoneBookSimInfo;
                        break;
                }
                int count = phonebookInfo.size();
                for (int i = 0; i < count; i++) {
                    PhoneBook book = phonebookInfo.get(i);


                    String name = book.mName;
                    String num = book.mNumber;
                    if (name != null) {
                        name = name.toUpperCase();
                    }
                    if (num != null) {
                        num = num.toUpperCase();
                    }
                    int addType = 0;
                    //					Log.d("allen", book.mName + ":" + book.mIndex); book.setIndex(i);
                    if ((name != null) && name.startsWith(prefixString)) {
                        addType = 1;
                        //						phonebook.add(book);
                    } else if ((book.mPinyin != null) && book.mPinyin.startsWith(prefixString)) {
                        addType = 1;
                        //						phonebook.add(book);
                    } else if ((num != null) && num.startsWith(prefixString)) {
                        addType = 1;
                        //						phonebook.add(book);
                    } else if ((name != null) && name.contains(prefixString)) {
                        addType = 2;
                        //						phonebookE.add(book);
                    } else if ((book.mPinyin != null) && book.mPinyin.contains(prefixString)) {
                        addType = 2;
                        //						phonebookE.add(book);
                    } else if ((num != null) && num.contains(prefixString)) {
                        addType = 2;
                        //						phonebookE.add(book);
                    }

                    if (addType != 0) {
                        if (GlobalDef.mFavorite == 1) {
                            book = new PhoneBook(book.mNumber, book.mName, book.mPinyin, i);
                        }

                        if (addType == 1) {
                            phonebook.add(book);
                        } else {
                            phonebookE.add(book);
                        }
                    }
                }

                // for (int i = 0; i < phonebookS.size(); i++) {
                // phonebook.add(phonebookS.get(i));
                // }

                for (int i = 0; i < phonebookE.size(); i++) {
                    phonebook.add(phonebookE.get(i));
                }
            }
            return phonebook;
        }

        @Override
        protected void onPostExecute(ArrayList<PhoneBook> result) {
            if (result == null) return;
            switch (mPhoneBook) {
                case 0:
                    if (mPhoneBookFilter != null) {
                        mPhoneBookFilter.clear();
                    }
                    mPhoneBookFilter = result;
                    break;
                case 1:
                    if (mPhoneBookSimFilter != null) {
                        mPhoneBookSimFilter.clear();
                    }
                    mPhoneBookSimFilter = result;
                    break;
            }

            mAdapterPhonebookFilter = new MyListAdapterEx(mThis, mPhoneBookFilter);

            if (mListViewPhonebook != null) {
                mListViewPhonebook.setAdapter(mAdapterPhonebookFilter);
            } else {
                mTrList.setAdapter(mAdapterPhonebookFilter);
            }
            mAdapterPhonebookFilter.notifyDataSetChanged();
        }
    }

    public int mUI;
    private int mCalllog;
    private int mPhoneBook;
    private int mPair;
    // private int ATBluetooth.mCurrentHFP = -1;

    private int mDownload;
    private boolean mSendDownloadPhoneCmd = false;
    // private int mCall;
    private String mCallNumber;
    private String mCallName;
    private int mLastUI = -1;

    private void showInputMethod(boolean s) {
        if (s) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(mTrEdit, 0);
        } else {
            if (getCurrentFocus() != null) {
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    private void hideTrEdit() {
        if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(ResourceUtil.mSystemUI)

                || MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(ResourceUtil.mSystemUI)) {
            return;
        }
        hideTrEditEx();
    }

    private void hideTrEditEx() {
        if (mTrEdit.getVisibility() == View.VISIBLE) {
            showInputMethod(false);
            mTrEdit.setText("");
            mTrEdit.setVisibility(View.GONE);
        }
    }

    private void showTrEdit() {
        // if (mTrEdit.getVisibility() == View.VISIBLE) {
        // hideTrEdit();
        // } else {
        mTrEdit.setVisibility(View.VISIBLE);
        mTrEdit.requestFocus();
        showInputMethod(true);
        // }
    }

    public void showUI(int id) {
        if (mSearchABCDEF != null) {
            if (R.id.phonebook == id) {
                mSearchABCDEF.setVisibility(View.VISIBLE);
            } else {
                mSearchABCDEF.setVisibility(View.GONE);
            }
        }

        if (R.id.pair != id) {
            hideLayoutPair();
        }

        if (R.id.phonebook != id) {
            hideLayoutPhonebook();
        }
        if (R.id.calllog != id) {
            hideLayoutCalllog();
        } else {
            ATBluetoothService.syncCallLog();
        }

        setSelPageButton(id);
        if (GlobalDef.mUIType == GlobalDef.UI_TYPE_8702) {
            showUI8702(id);
        } else {
            showUINormal(id);
        }
    }


    private boolean mIsShowMicMuteButton = false;

    private void fixOldUIMicMuteButton(int id) {
        if (mIsShowMicMuteButton) {
            if (id == R.id.dial) {
                mTl3.setVisibility(View.VISIBLE);
            }
        } else {
            ViewGroup.LayoutParams lp;
            lp = mTl0.getLayoutParams();
            if (id == R.id.dial) {
                lp.height = mTl1.getLayoutParams().height * 2;
                mTl0.setLayoutParams(lp);
                mTl3.setVisibility(View.GONE);
            } else {
                lp.height = mTl1.getLayoutParams().height;
                mTl0.setLayoutParams(lp);
                mTl3.setVisibility(View.VISIBLE);
            }
        }
    }

    public void showUINormal(int id) {
        stopSearch();
        if (mUI != id) {
            // if (mUI != 0) {
            // ((ImageView) findViewById(mUI)).getBackground().setLevel(0);
            // }
            // ((ImageView) findViewById(id)).getBackground().setLevel(1);
            fixOldUIMicMuteButton(id);
            mATBluetooth.sendHandler("ATBluetoothService", 0);
            setViewVisible(R.id.main_page_no_dial, View.VISIBLE);
            if (id == R.id.dial) {// mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK); //test
                // parrot

                mTrList.setVisibility(View.GONE);
                mTrList.setAdapter(null);
                mTList.setVisibility(View.GONE);
                mTList.setAdapter(null);
                mTrA2dp.setVisibility(View.GONE);
                mTl0.setImageResource(R.drawable.call);
                mTl0.getBackground().setLevel(0);
                mTl1.setImageResource(R.drawable.hung);
                mTl1.getBackground().setLevel(0);
                mTl2.setImageResource(R.drawable.voice);
                mTl2.getBackground().setLevel(0);
                // mTl2.setVisibility(View.VISIBLE);
                //				mTl3.setImageResource(R.drawable.mute1);
                updateMicStatus();

                mTl0.setVisibility(View.VISIBLE);
                mTl1.setVisibility(View.VISIBLE);
                mTl2.setVisibility(View.VISIBLE);
                //				mTl3.setVisibility(View.VISIBLE);

                // mTrA2dp.setVisibility(View.INVISIBLE);
                mTrListBg.setVisibility(View.GONE);
                mTrDial.setVisibility(View.VISIBLE);
                mTNormal.setVisibility(View.VISIBLE);
                mATBluetooth.sendHandler("ATBluetoothService", 1);

                mDownloading.setVisibility(View.GONE);
            } else if (id == R.id.calllog) {
                mTrList.setVisibility(View.VISIBLE);
                mTList.setVisibility(View.GONE);
                mTList.setAdapter(null);
                mTrA2dp.setVisibility(View.GONE);
                mTl0.setImageResource(R.drawable.call_missed);
                mTl0.getBackground().setLevel((mCalllog == 0) ? 1 : 0);
                mTl1.setImageResource(R.drawable.call_received);
                mTl1.getBackground().setLevel((mCalllog == 1) ? 1 : 0);
                mTl2.setImageResource(R.drawable.call_outgoing);
                mTl2.getBackground().setLevel((mCalllog == 2) ? 1 : 0);
                // mTl2.setVisibility(View.VISIBLE);
                mTl3.setImageResource(R.drawable.remove);
                // mTl3.setVisibility(View.VISIBLE);
                // mTrA2dp.setVisibility(View.INVISIBLE);
                mTrDial.setVisibility(View.INVISIBLE);
                mTrListBg.setVisibility(View.VISIBLE);
                mTNormal.setVisibility(View.VISIBLE);

                mTl0.setVisibility(View.VISIBLE);
                mTl1.setVisibility(View.VISIBLE);
                mTl2.setVisibility(View.VISIBLE);
                mTl3.setVisibility(View.VISIBLE);
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

                switch (mCalllog) {
                    case 0:
                        mTrList.setAdapter(mAdapterCallM);
                        mAdapterCallM.notifyDataSetChanged();
                        break;
                    case 1:
                        mTrList.setAdapter(mAdapterCallR);
                        mAdapterCallR.notifyDataSetChanged();
                        break;
                    case 2:
                        mTrList.setAdapter(mAdapterCallO);
                        mAdapterCallO.notifyDataSetChanged();
                        break;
                }
            } else if (id == R.id.phonebook) {
                mTrList.setVisibility(View.VISIBLE);
                mTList.setVisibility(View.GONE);
                mTList.setAdapter(null);
                mTrA2dp.setVisibility(View.GONE);
                mTl0.setImageResource(R.drawable.pb_phone);
                // mTl0.getBackground().setLevel((mPhoneBook == 0) ? 1 : 0);
                // mTl1.setImageResource(R.drawable.pb_sim);
                // mTl1.getBackground().setLevel((mPhoneBook == 1) ? 1 : 0);

                mTl1.setImageResource(R.drawable.search);
                mTl2.setImageResource(R.drawable.download);
                mTl2.getBackground().setLevel(0);
                // mTl2.setVisibility(View.VISIBLE);
                mTl3.setImageResource(R.drawable.remove);
                // mTl3.setVisibility(View.VISIBLE);
                // mTrA2dp.setVisibility(View.INVISIBLE);
                mTrDial.setVisibility(View.INVISIBLE);
                mTrListBg.setVisibility(View.VISIBLE);
                mTNormal.setVisibility(View.VISIBLE);

                mTl0.setVisibility(View.VISIBLE);
                mTl1.setVisibility(View.VISIBLE);
                mTl2.setVisibility(View.VISIBLE);
                mTl3.setVisibility(View.VISIBLE);

                mTrList.setAdapter(mAdapterPhonebook);

                mAdapterPhonebook.notifyDataSetChanged();

                if (mPhoneBookInfo.size() == 0) {
                    // mPhoneBookInfo.clear();// allen test
                    if (SaveData.getPhoneBookData(mPhoneBookInfo, ATBluetooth.mCurrentMac) > 0) {
                        // mAdapter.notifyDataSetChanged();
                    }
                }

                if (mPhoneBookSimInfo.size() == 0) {
                    if (SaveData.getPhoneBookSimData(mPhoneBookSimInfo, ATBluetooth.mCurrentMac) > 0) {
                        // mAdapter.notifyDataSetChanged();
                    }
                }

                // if (mPhoneBook == 0) {
                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa5);
                // } else {
                //
                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa4);
                // }

                if (mSorting || mDownload == 1) {
                    mDownloading.setVisibility(View.VISIBLE);
                }
            } else if (id == R.id.favorite) {
                mTrList.setVisibility(View.VISIBLE);
                mTList.setVisibility(View.GONE);
                mTList.setAdapter(null);
                mTrA2dp.setVisibility(View.GONE);
                // mTl0.setVisibility(View.GONE);
                mTl1.setVisibility(View.INVISIBLE);
                mTl2.setVisibility(View.INVISIBLE);
                mTl3.setVisibility(View.INVISIBLE);
                mTl0.setImageResource(R.drawable.remove);
                mTl0.getBackground().setLevel(0);
                mTl1.setImageDrawable(null);
                mTl2.setImageDrawable(null);
                // mTl2.setVisibility(View.VISIBLE);
                mTl3.setImageDrawable(null);
                // mTl3.setVisibility(View.VISIBLE);
                // mTrA2dp.setVisibility(View.INVISIBLE);
                mTrDial.setVisibility(View.INVISIBLE);
                mTrListBg.setVisibility(View.VISIBLE);
                mTNormal.setVisibility(View.VISIBLE);

                if (mPhoneBookInfo.size() == 0) {
                    // mPhoneBookInfo.clear();// allen test
                    if (SaveData.getPhoneBookData(mPhoneBookInfo, ATBluetooth.mCurrentMac) > 0) {
                        // mAdapter.notifyDataSetChanged();
                    }
                }

                if (mPhoneBookSimInfo.size() == 0) {
                    if (SaveData.getPhoneBookSimData(mPhoneBookSimInfo, ATBluetooth.mCurrentMac) > 0) {
                        // mAdapter.notifyDataSetChanged();
                    }
                }

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

                if (mAdapterPhonebookFavorite == null) {
                    mAdapterPhonebookFavorite = new AdapterPhoneBookFavorite(this);
                }
                mTrList.setAdapter(mAdapterPhonebookFavorite);

                mAdapterPhonebookFavorite.notifyDataSetChanged();

                // if (mPhoneBook == 0) {
                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa5);
                // } else {
                //
                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa4);
                // }

                if (mSorting || mDownload == 1) {
                    mDownloading.setVisibility(View.VISIBLE);
                }
            } else if (id == R.id.a2dp) {
                mTrList.setVisibility(View.GONE);
                mTrList.setAdapter(null);
                mTList.setVisibility(View.GONE);
                mTList.setAdapter(null);
                mTNormal.setVisibility(View.GONE);
                // mTl0.setImageResource(R.drawable.connect);
                // mTl0.getBackground().setLevel(0);
                // mTl1.setImageResource(R.drawable.disconnect);
                // mTl1.getBackground().setLevel(0);
                // mTl2.setVisibility(View.GONE);
                // mTl3.setVisibility(View.GONE);
                // mTrListBg.setVisibility(View.INVISIBLE);
                // mTrDial.setVisibility(View.INVISIBLE);
                mTrA2dp.setVisibility(View.VISIBLE);
                // mTNormal.setVisibility(View.VISIBLE);
                // mAdapter.notifyDataSetChanged();
                // mATBluetooth.requestSource();
                BroadcastUtil.sendToCarServiceSetSource(this, MyCmd.SOURCE_BT_MUSIC);

                GlobalDef.requestAudioFocus(this);
            } else if (id == R.id.pair) {
                mTList.setVisibility(View.GONE);
                mTList.setAdapter(null);
                mTrA2dp.setVisibility(View.GONE);
                mTl0.setImageResource(R.drawable.paired);
                mTl0.getBackground().setLevel((mPair == 0) ? 1 : 0);
                mTl1.setImageResource(R.drawable.search);
                mTl1.getBackground().setLevel((mPair == 1) ? 1 : 0);
                mTl2.setImageResource(R.drawable.disconnect);
                mTl2.getBackground().setLevel(0);
                // mTl2.setVisibility(View.VISIBLE);
                mTl3.setImageResource(R.drawable.remove);

                mTl0.setVisibility(View.VISIBLE);
                mTl1.setVisibility(View.VISIBLE);
                mTl2.setVisibility(View.VISIBLE);
                mTl3.setVisibility(View.VISIBLE);
                // mTl3.setVisibility(View.VISIBLE);
                // mTrA2dp.setVisibility(View.INVISIBLE);
                mTrDial.setVisibility(View.INVISIBLE);
                mTrListBg.setVisibility(View.VISIBLE);
                mTNormal.setVisibility(View.VISIBLE);

                mTrList.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();

                mTrList.setVisibility(View.VISIBLE);
                mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);

                if (mLayoutPair != null) {
                    mLayoutPair.setVisibility(View.VISIBLE);
                    setViewVisible(R.id.main_page_no_dial, View.GONE);
                }
            } else if (id == R.id.setting) {
                mTrList.setVisibility(View.GONE);
                mTrList.setAdapter(null);
                mTNormal.setVisibility(View.GONE);
                mTrA2dp.setVisibility(View.GONE);
                mTList.setVisibility(View.VISIBLE);

                mTList.setAdapter(mAdapter2);
                mATBluetooth.write(ATBluetooth.REQUEST_NAME);
                mATBluetooth.write(ATBluetooth.REQUEST_PIN);
                mATBluetooth.write(ATBluetooth.REQUEST_SETTING);
                mATBluetooth.write(ATBluetooth.REQUEST_AUTOCONNECT);
            }
            mUI = id;
            hideTrEdit();
        }
    }

    private ArrayList<PhoneBook> mSearchInfo = ATBluetoothService.mSearchInfo;// new
    // ArrayList<PhoneBook>();
    private ArrayList<PhoneBook> mPairInfo = ATBluetoothService.mPairInfo;// new
    // ArrayList<PhoneBook>();
    private ArrayList<PhoneBook> mPhoneBookInfo = ATBluetoothService.mPhoneBookInfo;
    private ArrayList<PhoneBook> mPhoneBookFilter = null;
    private ArrayList<PhoneBook> mPhoneBookSimInfo = ATBluetoothService.mPhoneBookSimInfo;
    private ArrayList<PhoneBook> mPhoneBookSimFilter = null;
    private ArrayList<PhoneBook> mCalllogMInfo = ATBluetoothService.mCalllogMInfo;
    private ArrayList<PhoneBook> mCalllogRInfo = ATBluetoothService.mCalllogRInfo;
    private ArrayList<PhoneBook> mCalllogOInfo = ATBluetoothService.mCalllogOInfo;

    public static ArrayList<PhoneBook> mSearchInfo2 = new ArrayList<PhoneBook>();

    private final int[] mSettingName = new int[]{
            R.string.device_name, R.string.pin, R.string.auto_answer, R.string.auto_connect
    };
    private static String[] mSettingValue = new String[4];
    private int[] mSettingValueX = new int[]{0, 0, -1, -1};
    private int[] mSettingArray = new int[]{
            0, 0, R.array.auto_array, R.array.auto_array
    };

    public void onTlClick(View v) {
        int id = v.getId();
        if (id == R.id.tl_0) {
            if (mUI == R.id.dial) {// if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CALLED) {
                // mCallNumber = mDigit.getText().toString();
                // if ((mCallNumber != null) && (mCallNumber.length() != 0)) {
                // if(mCallNumber.equals("******")){
                // mATBluetooth.write(ATBluetooth.REQUEST_REQUEST_VERSION);
                //
                // } else {
                // mCallName = null;
                // mATBluetooth.write(ATBluetooth.REQUEST_CALL,
                // mCallNumber);
                // }
                //
                // }
                // } else if (ATBluetooth.mCurrentHFP ==
                // ATBluetooth.HFP_INFO_CALLING) {
                // mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);
                // }
                doDial();
            } else if (mUI == R.id.calllog) {
                mCalllog = 0;
                mTl0.getBackground().setLevel(1);
                mTl1.getBackground().setLevel(0);
                mTl2.getBackground().setLevel(0);

                mTrList.setAdapter(mAdapterCallM);

                // mAdapter.notifyDataSetChanged();
            } else if (mUI == R.id.phonebook) {
                if (mDownload == 1) {
                    stopDownPhoneBook();
                    return;
                }
                if ((mDownload != 1) && (mPhoneBook == 0)) {
                    // showTrEdit();
                } else {
                    // hideTrEdit();
                }
                hideTrEdit();
                mPhoneBook = 0;
                mTl0.getBackground().setLevel(1);
                mTl1.getBackground().setLevel(0);

                mTrList.setAdapter(mAdapterPhonebook);
                mAdapterPhonebook.notifyDataSetChanged();

                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa5);
                // case R.id.a2dp:
                // mATBluetooth.write(ATBluetooth.REQUEST_AV, 1);
                // break;
            } else if (mUI == R.id.pair) {
                stopSearch();
                mPair = 0;
                mTl0.getBackground().setLevel(1);
                mTl1.getBackground().setLevel(0);
                mPairInfo.clear();
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
            } else if (mUI == R.id.favorite) {
                showDialogId(R.string.remove_pb_favorite);
            }
        } else if (id == R.id.tl_1) {
            if (mUI == R.id.dial) {
                doHang();
            } else if (mUI == R.id.calllog) {
                mCalllog = 1;
                mTl0.getBackground().setLevel(0);
                mTl1.getBackground().setLevel(1);
                mTl2.getBackground().setLevel(0);

                mTrList.setAdapter(mAdapterCallR);
            } else if (mUI == R.id.phonebook) {
                if (mDownload == 1) {
                    stopDownPhoneBook();// end
                    mDownload = 0;
                    return;
                }

                showTrEdit();

                // if ((mDownload != 1) && (mPhoneBook == 1)) {
                // showTrEdit();
                // } else {
                // hideTrEdit();
                // }
                // mPhoneBook = 1;
                //
                mTl0.getBackground().setLevel(0);
                mTl1.getBackground().setLevel(1);
                //
                // mTrList.setAdapter(mAdapterPhonebookSim);

                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa4);
                // case R.id.a2dp:
                // mATBluetooth.write(ATBluetooth.REQUEST_AV, 0);
                // break;
            } else if (mUI == R.id.pair) {
                mPair = 1;
                mTl0.getBackground().setLevel(0);
                mTl1.getBackground().setLevel(1);
                // mATBluetooth.write(ATBluetooth.REQUEST_HFP, 0);
                if (!mSearch) {
                    // stopSearch();
                    // } else {
                    if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                        mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE);
                        mStartSearch = 0;
                        Util.doSleep(200);
                    }
                    mATBluetooth.write(ATBluetooth.REQUEST_SEARCH);
                    mTrProgress.setVisibility(View.VISIBLE);

                    mSearchInfo.clear();
                    mAdapter.notifyDataSetChanged();
                    mSearch = true;
                } else {
                    stopSearch();
                }
            }
        } else if (id == R.id.tl_2) {
            if (mUI == R.id.dial) {// to do
                doVoiceSwitch();
            } else if (mUI == R.id.calllog) {
                mCalllog = 2;
                mTl0.getBackground().setLevel(0);
                mTl1.getBackground().setLevel(0);
                mTl2.getBackground().setLevel(1);

                mTrList.setAdapter(mAdapterCallO);
            } else if (mUI == R.id.phonebook) {
                stopDownPhoneBook();// end
                switch (mPhoneBook) {
                    case 0: {
                        showDialogId(R.string.download_pb_phone);
                        break;
                    }
                    case 1: {
                        showDialogId(R.string.download_pb_sim);
                        break;
                    }
                }
            } else if (mUI == R.id.pair) {
                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CONNECTED) {
                    showDialogId(R.string.if_diconnect);
                }
            }
        } else if (id == R.id.tl_3) {
            if (mUI == R.id.dial) {
                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                    ATBluetoothService.showSpeech(true);
                } else {
                    if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CALLED) {
                        if ((System.currentTimeMillis() - GlobalDef.mLockClickTime) > GlobalDef.KEY_LOCK_TIME) {
                            GlobalDef.mLockClickTime = System.currentTimeMillis();
                            mATBluetooth.write(ATBluetooth.REQUEST_MIC);
                            updateMicStatus();

                        }
                    }
                }
            } else if (mUI == R.id.calllog) {
                showDialogId(R.string.remove_calllog);
            } else if (mUI == R.id.phonebook) {
                if (mDownload == 1) {
                    stopDownPhoneBook();// end
                    mDownload = 0;
                }
                switch (mPhoneBook) {
                    case 0: {
                        showDialogId(R.string.remove_pb_phone);
                        break;
                    }
                    case 1: {
                        showDialogId(R.string.remove_pb_sim);
                        break;
                    }
                }
            } else if (mUI == R.id.pair) {// stopSearch();
                // switch (mPair) {
                // case 0: {
                showDialogId(R.string.remove_pair);
                // break;
                // }
                // case 1:
                // mSearchInfo.clear();
                // mAdapter.notifyDataSetChanged();
                // break;
                // }
            }
        }
    }

    private void updateMicStatus() {
        if (mUI != R.id.dial) {
            if (!mATBluetooth.getMicMute()) {
                mTl3.setImageResource(R.drawable.mute1);
            } else {
                mTl3.setImageResource(R.drawable.mute2);
            }
        }
    }

    private int mStartSearch = 0;

    private char numIdToChar(int id) {
        char c = 0;
        if (id == R.id.n0) {
            c = '0';
        } else if (id == R.id.n1) {
            c = '1';
        } else if (id == R.id.n2) {
            c = '2';
        } else if (id == R.id.n3) {
            c = '3';
        } else if (id == R.id.n4) {
            c = '4';
        } else if (id == R.id.n5) {
            c = '5';
        } else if (id == R.id.n6) {
            c = '6';
        } else if (id == R.id.n7) {
            c = '7';
        } else if (id == R.id.n8) {
            c = '8';
        } else if (id == R.id.n9) {
            c = '9';
        } else if (id == R.id.nx) {
            c = '*';
        } else if (id == R.id.nj) {
            c = '#';
        }
        return c;
    }

    //	private boolean mSendDTMF = false;
    public void onDClick(View v) {
        char c = numIdToChar(v.getId());

        if (m3CallLayout == null || m3CallLayout.getVisibility() == View.GONE) {
            if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CALLED) {
                mDigit.setText("" + mDigit.getText() + c);
            } else {
                mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "" + c);
                mDigit.setText("" + mDigit.getText() + c);
            }
        } else {
            if (ATBluetooth.mCallingNum < 2) {
                m3CallTextView.setText("" + m3CallTextView.getText() + c);
            } else {
                mATBluetooth.write(ATBluetooth.REQUEST_DTMF, "" + c);
                m3CallTextView.setText("" + m3CallTextView.getText() + c);
            }
        }
    }

    private void setButtonEnable(boolean b) {
        this.findViewById(R.id.dial).setEnabled(b);
    }

    private void setPageButtonNormal(int id, Drawable drawable) {

        View v = findViewById(id);
        if (v != null) {
            v.setBackground(drawable);

        }
    }

    private void clearFocus(int id) {

        View v = getCurrentFocus();
        if (v != null) {
            v.clearFocus();

        }
    }

    private boolean isFocusPageButton() {
        if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(ResourceUtil.mSystemUI)

                || MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(ResourceUtil.mSystemUI)) {
            return true;
        }
        return false;
    }

    private Drawable getNormalBackground() {
        Drawable d = null;
        if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(ResourceUtil.mSystemUI)

                || MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI)) {
            d = getResources().getDrawable(R.drawable.ico_button_normal);
        }
        return d;
    }

    private Drawable getSelectBackground() {
        Drawable d = null;

        d = getResources().getDrawable(R.drawable.ico_btn_sel);

        return d;
    }

    private void setSelect(int id, boolean b) {
        View v = findViewById(id);
        if (v != null) {
            v.setSelected(b);
        }
    }

    public void setSelPageButton(int id) {
        if (isFocusPageButton()) {

            if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(ResourceUtil.mSystemUI)

                    || MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(ResourceUtil.mSystemUI)) {
                setSelect(R.id.dial, false);
                setSelect(R.id.phonebook, false);
                setSelect(R.id.calllog, false);
                setSelect(R.id.setting, false);
                setSelect(R.id.pair, false);
                setSelect(R.id.a2dp, false);
                setSelect(id, true);
            } else {

                setSelect(id, true);
                Drawable d = getNormalBackground();
                setPageButtonNormal(R.id.dial, d);
                d = getNormalBackground();
                setPageButtonNormal(R.id.phonebook, d);
                d = getNormalBackground();
                setPageButtonNormal(R.id.calllog, d);
                d = getNormalBackground();
                setPageButtonNormal(R.id.setting, d);
                d = getNormalBackground();
                setPageButtonNormal(R.id.pair, d);
                d = getNormalBackground();
                setPageButtonNormal(R.id.a2dp, d);

                Drawable drawable = getSelectBackground();
                // Drawable drawable = getResources().getDrawable(
                // R.drawable.ico_btn_sel);
                setPageButtonNormal(id, drawable);

            }

        }
    }

    public void onBClick(View v) {
        if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED) {
            int id = v.getId();
            if (id == R.id.dial || id == R.id.calllog || id == R.id.phonebook || id == R.id.favorite) {
                return;
            } else if (id == R.id.a2dp) {//				if (GlobalDef.mA2DPInside == 1) {
                //					return;
                //				}
            }
        } else if (ATBluetooth.mCurrentHFP > ATBluetooth.HFP_INFO_CONNECTED) {
            return;
        }

        // if (v.getId() != R.id.phonebook) {
        // stopDownPhoneBook();
        // }

        int id = v.getId();
        if (id == R.id.dial || id == R.id.calllog || id == R.id.phonebook || id == R.id.setting || id == R.id.favorite) {
            if (mSearch) {
                mTrProgress.setVisibility(View.INVISIBLE);
            }

            // case R.id.a2dp:
            if (mSearch) {
                mTrProgress.setVisibility(View.VISIBLE);
            }
        } else if (id == R.id.pair) {// case R.id.a2dp:
            if (mSearch) {
                mTrProgress.setVisibility(View.VISIBLE);
            }
        }

        if (GlobalDef.mA2DPInside != 1) {
            if (v.getId() != R.id.a2dp) {
                showUI(v.getId());
            } else {
                UtilCarKey.doKeyBTMusic(this);
            }
        } else {
            showUI(v.getId());
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.prev) {
            ATBluetoothService.musicKeyControl(MyCmd.Keycode.PREVIOUS);
        } else if (id == R.id.pp) {
            ATBluetoothService.musicKeyControl(MyCmd.Keycode.PLAY_PAUSE);
        } else if (id == R.id.next) {
            ATBluetoothService.musicKeyControl(MyCmd.Keycode.NEXT);
        } else if (id == R.id.delete) {
            TextView tv = mDigit;
            if (isUISupport3Call()) {
                if (m3CallAdd || ((m3CallLayout.getVisibility() == View.VISIBLE) && ATBluetooth.m3CallActiveIndex == 2)) {
                    tv = m3CallTextView;
                    //					if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1
                    //							.equals(ResourceUtil.mSystemUI)) {
                    if (tv.length() <= 1) {
                        m3CallAdd = false;
                        m3CallLayout.setVisibility(View.GONE);
                        m3CallStatus.setText("");
                        set3CallDialText("", "");
                    }
                    //					}
                }
            }

            String str = tv.getText().toString();
            if (str.length() > 0) {
                tv.setText(str.substring(0, str.length() - 1));
            }
        } else if (id == R.id.menu) {
            UtilCarKey.doKeyGps(this);
        } else if (id == R.id.back) {// finish();
            doBackKey();
        } else if (id == R.id.home) {
            startActivity(new Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP).addCategory(Intent.CATEGORY_HOME));
        }

    }

    // /
    // add by allen
    UpdateTextTask mUpdateTextTask;

    // private static void sortPhoneBook(ArrayList<PhoneBook> pb) {
    // String name;
    //
    // for (int i = 0; i < pb.size(); ++i) {
    // name = pb.get(i).mName;
    // if (name != null) {
    // String a = PinyinConv.cn2py (name);
    // Log.d("aa",""+ a);
    // } else {
    //
    // }
    // }
    //
    // }
    private void savePhoneBook() {
        mSendDownloadPhoneCmd = false;
        // Log.d("allen", "size22:" + mPhoneBookInfo.size());

        // Log.d("allen", "size:" + mPhoneBookInfo.size());
        mUpdateTextTask = new UpdateTextTask();
        mUpdateTextTask.execute();

        // if (mPhoneBook == 0) {
        if (mPhoneBookInfo.size() > 0) {
            // SaveData.savePhoneBookData(mPhoneBookInfo,
            // ATBluetooth.mCurrentMac);
            // mToast.setText(getString(R.string.saving_phone_book));
            // mToast.show();
            // } else {
            if (GlobalDef.mFavorite != 1) {
                mToast.setText(getString(R.string.total) + " " + mPhoneBookInfo.size() + " " + getString(R.string.record));

                mToast.show();
            }

        }

        // ATBluetoothService.showMicButton(true);
        // }
        //
        // else if (mPhoneBook == 1) {
        //
        // if (mPhoneBookSimInfo.size() > 0) {
        // // SaveData.savePhoneBookData(mPhoneBookSimInfo,
        // // ATBluetooth.mCurrentMac + "sim");
        // mToast.setText(getString(R.string.saving_phone_book));
        // mToast.show();
        // } else {
        // mToast.setText(getString(R.string.total) + " "
        // + mPhoneBookSimInfo.size() + " "
        // + getString(R.string.record));
        //
        // mToast.show();
        //
        // }
        // }

        mPhoneBook = 0;
        // if()
        // mDownloading.setVisibility(View.GONE);
    }

    // private void savePhoneBookAsync() {
    //
    // Log.d("allen", "size:" + mPhoneBookInfo.size());
    //
    // if (mPhoneBook == 0) {
    // if (mPhoneBookInfo.size() > 0) {
    // SaveData.savePhoneBookData(mPhoneBookInfo,
    // ATBluetooth.mCurrentMac);
    // mToast.setText(getString(R.string.saving_phone_book));
    // mToast.show();
    // } else {
    // mToast.setText(getString(R.string.total) + " "
    // + mPhoneBookInfo.size() + " "
    // + getString(R.string.record));
    //
    // mToast.show();
    //
    // }
    //
    // }
    //
    // else if (mPhoneBook == 1) {
    //
    // if (mPhoneBookSimInfo.size() > 0) {
    // SaveData.savePhoneBookData(mPhoneBookSimInfo,
    // ATBluetooth.mCurrentMac + "sim");
    // mToast.setText(getString(R.string.saving_phone_book));
    // mToast.show();
    // } else {
    // mToast.setText(getString(R.string.total) + " "
    // + mPhoneBookSimInfo.size() + " "
    // + getString(R.string.record));
    //
    // mToast.show();
    //
    // }
    // }
    //
    // mPhoneBook = 0;
    // }
    private void addPhoneBookIVT(ATBluetooth.TObject obj) {
        mPhoneBookInfo.add(new PhoneBook((String) obj.obj2, (String) obj.obj3, PinyinConv.cn2py((String) obj.obj3)));

    }

    private void addPhoneBookParrot(Message msg) {
        // if (msg.arg1 == 1) {
        // mDownloading.setVisibility(View.VISIBLE);
        // mPhoneBookInfo.clear();
        // mAdapterPhonebook.notifyDataSetChanged();
        // mDownload = 1;
        // }

        PhoneBook pb;

        ATBluetooth.TObject obj = (ATBluetooth.TObject) msg.obj;
        String name = (String) obj.obj3;
        String num = (String) obj.obj2;

        if (name != null) {
            if (num != null) {
                num = num.replace("-", "");
                num = num.replace(" ", "");
            }
            pb = new PhoneBook(num, name, PinyinConv.cn2py(name), msg.arg1);
            mPhoneBookInfo.add(pb);
        } else if (num != null) {
            num = num.replace("-", "");
            num = num.replace(" ", "");
            if (mPhoneBookInfo.size() > 0) {
                pb = mPhoneBookInfo.get(mPhoneBookInfo.size() - 1);
                if (pb.mIndex == msg.arg1) {
                    if (pb.mNumber == null) {
                        pb.mNumber = num;
                    } else {
                        name = pb.mName;
                        pb = new PhoneBook(num, name, PinyinConv.cn2py(name), msg.arg1);
                        mPhoneBookInfo.add(pb);
                    }
                } else {
                    pb = new PhoneBook(num, name, PinyinConv.cn2py(name), msg.arg1);
                    mPhoneBookInfo.add(pb);
                }
            } else {
                pb = new PhoneBook(num, name, PinyinConv.cn2py(name), msg.arg1);
                mPhoneBookInfo.add(pb);
            }
        }
        // Log.d("tt", name +":"+num+":"+mPhoneBookInfo.size());

    }

    private void addPhoneBook(Message msg) {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            addPhoneBookIVT((TObject) msg.obj);
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            addPhoneBookParrot(msg);
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            addPhoneBookIVT((TObject) msg.obj);
        }

        mAdapterPhonebook.notifyDataSetChanged();
        if (mUI == R.id.phonebook) {
            if (mDownload == 1) {
                mDownloading.setText(mPhoneBookInfo.size() + " " + getString(R.string.record));
                // mToast.show();

            }
        }
    }

    private int mStartDownIndex = 0;

    private void startDownPhoneBook() {
        mSendDownloadPhoneCmd = true;
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            startDownPhoneBookIVT();
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
            startDownPhoneBookParrot();
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            startDownPhoneBookGOC();
        }
    }

    private int mPBNum = 0;

    private void startDownPhoneBookParrot() {

        mStartDownIndex = 0;
        if (mPhoneBook == 0) {

            // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK_DOWN_STATUS,1);

            // if (Parrot.mSyncPhoneBookeStatus == 7) {
            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK_SIZE, Parrot.mDeviceIndex);
            // } else {
            // mDownload = 1;
            // mDownloading.setVisibility(View.VISIBLE);
            // }

            // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK,
            // ATBluetooth.PHONE_BOOK_TYPE_PHONE, 0);

            mDownloading.setText("");
        } else if (mPhoneBook == 1) {
            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, ATBluetooth.PHONE_BOOK_TYPE_SIM, 0);
        }
        Log.d("allen", "start!" + mPhoneBook);

    }

    private void startDownPhoneBookGOC() {
        mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK);
        mDownloading.setText("");
    }

    private void startDownPhoneBookIVT() {

        mStartDownIndex = 0;
        if (mPhoneBook == 0) {
            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, ATBluetooth.PHONE_BOOK_TYPE_PHONE, 0);

            mDownloading.setText("");
        } else if (mPhoneBook == 1) {
            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, ATBluetooth.PHONE_BOOK_TYPE_SIM, 0);
        }
        Log.d("allen", "start!" + mPhoneBook);

    }

    // private void continueDownPhoneBook() {
    // if (mDownload == 0) {
    // return;
    // }
    // int curSize = 0;
    // if (mPhoneBook == 0) {
    // curSize = mPhoneBookInfo.size();
    // } else if (mPhoneBook == 1) {
    // curSize = mPhoneBookSimInfo.size();
    // }
    //
    // if (mStartDownIndex == curSize ||
    // (curSize-mStartDownIndex)<ATBluetooth.DOWNLOAD_NUM_ONETIME) {
    // mDownload = 0;
    // mAdapterPhonebook.notifyDataSetChanged();
    // // mDownloading.setText( " " + getString(R.string.sorting));
    // savePhoneBook();
    //
    // // mHandler.postDelayed(new Runnable(){
    // // public void run(){
    // // savePhoneBook();
    // // }
    // // }, 1);
    // } else {
    // mStartDownIndex = curSize;
    // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK,
    // ATBluetooth.PHONE_BOOK_TYPE_PHONE, mStartDownIndex);
    // }
    //
    // }
    private void continueDownPhoneBook() {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            continueDownPhoneBookIVT();
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {

            continueDownPhoneBookParrot();

        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {

            continueDownPhoneBookParrot();

        }
    }

    private void continueDownPhoneBookIVT() {
        if (mDownload == 0) {
            return;
        }
        int curSize = 0;
        if (mPhoneBook == 0) {
            curSize = mPhoneBookInfo.size();
        } else if (mPhoneBook == 1) {
            curSize = mPhoneBookSimInfo.size();
        }

        if (mStartDownIndex == curSize || (curSize - mStartDownIndex) < ATBluetooth.DOWNLOAD_NUM_ONETIME) {
            if (mPhoneBook == 0) {
                mPhoneBook = 1;
                startDownPhoneBook();
            } else {
                mDownload = 0;
                mAdapterPhonebook.notifyDataSetChanged();
                // mDownloading.setText( " " + getString(R.string.sorting));
                savePhoneBook();
            }
        } else {
            mStartDownIndex = curSize;
            mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, ATBluetooth.PHONE_BOOK_TYPE_PHONE, mStartDownIndex);
        }

    }

    private void continueDownPhoneBookParrot() {
        if (mDownload == 0) {
            return;
        }

        mDownload = 0;
        mAdapterPhonebook.notifyDataSetChanged();
        // mDownloading.setText( " " + getString(R.string.sorting));
        savePhoneBook();

    }

    private void stopDownPhoneBook() {
        if (mDownload == 1) {
            mDownload = 0;

            mATBluetooth.write(ATBluetooth.REQUEST_STOP_PHONE_BOOK);

            // savePhoneBook();
            mSendDownloadPhoneCmd = false;
        }
        if (mUpdateTextTask != null) {
            mUpdateTextTask.cancel(true);
            mUpdateTextTask = null;
            // Util.doSleep(50);
        } else {
            // Util.doSleep(10);
        }
    }

    public static boolean mSearch = false;

    private void stopSearch() {
        if (mSearch) {
            // mPair = 0;
            //
            // mTl0.getBackground().setLevel(1);
            // mTl1.getBackground().setLevel(0);

            mSearch = false;
            mATBluetooth.write(ATBluetooth.RETURN_STOP_SEARCH);

            mTrProgress.setVisibility(View.INVISIBLE);

            mAdapter.notifyDataSetChanged();
        }
    }

    private void onClickPairedListItem(int position) {

        if ("1".equals(mPairInfo.get(position).mPinyin)) {
            mATBluetooth.write(ATBluetooth.REQUEST_PAIR_BY_ADDR, mPairInfo.get(position).mNumber);// connect

            mToast.setText(getString(R.string.connecting) + " " + mPairInfo.get(position).mName);
            mToast.show();

        } else {

            if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CONNECTED) {
                showDialogId(R.string.if_diconnect);
            } else {

                connectBT(mPairInfo.get(position).mNumber, position);
                // mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_ADDR,
                // mPairInfo.get(position).mNumber);//

                mToast.setText(getString(R.string.connecting) + " " + mPairInfo.get(position).mName);
                mToast.show();

            }
        }

    }

    private void connectBT(String mac, int index) {
        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_ADDR, mac);
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
            mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_ADDR, mac);
        } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {

            index = -1; // find pair index
            for (int i = 0; i < mPairInfo.size(); ++i) {
                if (mPairInfo.get(i).mNumber.equals(mac)) {
                    index = mPairInfo.get(i).mIndex;
                }
            }


            if (index == -1) {
                mATBluetooth.write(ATBluetooth.REQUEST_PAIR_BY_ADDR, mac);
            } else {
                mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_INDEX, (index) + "");
            }

        }
    }

    private void onClickSearchListItem(int position) {

        // stopSearch();
        // if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
        if (mSearch) {
            return;
        }
        // }

        if ("1".equals(mSearchInfo.get(position).mPinyin)) {
            mATBluetooth.write(ATBluetooth.REQUEST_PAIR_BY_ADDR, mSearchInfo.get(position).mNumber);// connect
        } else {

            if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CONNECTED) {
                showDialogId(R.string.if_diconnect);
            } else {
                stopSearch();
                Util.doSleep(20);

                connectBT(mSearchInfo.get(position).mNumber, -1);

                // mATBluetooth.write(ATBluetooth.REQUEST_CONNECT_BY_ADDR,
                // mSearchInfo.get(position).mNumber);//

                mToast.setText(getString(R.string.connecting) + " " + mSearchInfo.get(position).mName);
                mToast.show();

            }
        }

    }

    private int mDialogTitle;
    AlertDialog.Builder mBuilder;

    private void initDialog() {
        mBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        mBuilder.setPositiveButton(R.string.alert_dialog_ok, mDialogOkClick);
        mBuilder.setNegativeButton(R.string.alert_dialog_cancel, null);
    }

    private static final int DIALOG_CALL_LOG = 1;

    public void showDialogId(int title) {
        showDialogId(title, null);
    }

    private void showDialogId(int title, String s) {
        mDialogTitle = title;

        if (title != 0) {
            if (s == null) {
                mBuilder.setTitle(title);
            } else {
                mBuilder.setTitle(s);
            }

            mBuilder.show();
        }
    }

    private PhoneBook mCalllogForDial;
    private DialogInterface.OnClickListener mDialogOkClick = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
            int preTitle = mDialogTitle;
            if (mDialogTitle == R.string.if_diconnect) {
                mATBluetooth.write(ATBluetooth.REQUEST_DISCONNECT);
                stopSearch();
            } else if (mDialogTitle == R.string.remove_one_pair) {
                if (mSearch) {
                    stopSearch();
                    Util.doSleep(400);
                }

                if (mForFavoriteIndex >= 0 && mForFavoriteIndex < mPairInfo.size()) {
                    PhoneBook pb = mPairInfo.get(mForFavoriteIndex);
                    mATBluetooth.write(ATBluetooth.CLEAR_PAIR_INFO, pb.mIndex);
                    mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                    mPairInfo.clear();
                    mAdapter.notifyDataSetChanged();
                }
            } else if (mDialogTitle == R.string.remove_pair) {
                if (mSearch) {
                    stopSearch();
                    Util.doSleep(400);
                }
                mPairInfo.clear();
                mSearchInfo.clear();
                mATBluetooth.write(ATBluetooth.CLEAR_PAIR_INFO);

                mAdapter.notifyDataSetChanged();

                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT || ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_GOC) {
                    mATBluetooth.write(ATBluetooth.REQUEST_DISCONNECT);
                } else if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                    SaveData.clearPhoneBookData(ATBluetooth.mCurrentMac);
                    if (ATBluetoothService.mContactsUtils != null) {
                        ATBluetoothService.mContactsUtils.clear(ATBluetooth.mCurrentMac);
                        ATBluetoothService.mContactsUtils.notifyWacTalk(ATBluetooth.mCurrentMac);
                    }
                }
            } else if (mDialogTitle == R.string.remove_pb_sim) {
                hideTrEdit();
                mPhoneBookSimInfo.clear();
                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0x80);
                mAdapterPhonebookSim.notifyDataSetChanged();
                SaveData.clearPhoneBookDataSim(ATBluetooth.mCurrentMac);
                if (ATBluetoothService.mContactsUtils != null) {
                    ATBluetoothService.mContactsUtils.clear(ATBluetooth.mCurrentMac);
                    ATBluetoothService.mContactsUtils.notifyWacTalk(ATBluetooth.mCurrentMac);
                }
            } else if (mDialogTitle == R.string.remove_pb_phone) {
                hideTrEdit();
                mPhoneBookInfo.clear();
                // mATBluetooth.write(ATBluetooth.REQUEST_DEL_PHONE_BOOK);
                mAdapterPhonebook.notifyDataSetChanged();
                SaveData.clearPhoneBookData(ATBluetooth.mCurrentMac);
                if (ATBluetoothService.mContactsUtils != null) {
                    ATBluetoothService.mContactsUtils.clear(ATBluetooth.mCurrentMac);
                    ATBluetoothService.mContactsUtils.notifyWacTalk(ATBluetooth.mCurrentMac);
                }
                // ATBluetoothService.showMicButton(false);
            } else if (mDialogTitle == R.string.remove_calllog) {
                mCalllogMInfo.clear();
                mCalllogRInfo.clear();
                mCalllogOInfo.clear();

                mAdapterCallM.notifyDataSetChanged();
                mAdapterCallR.notifyDataSetChanged();
                mAdapterCallO.notifyDataSetChanged();

                SaveData.clearCalllogData(ATBluetooth.mCurrentMac);
            } else if (mDialogTitle == R.string.download_pb_sim) {
                hideTrEdit();

                // mStartDownload = 0;
                // mATBluetooth.write(ATBluetooth.REQUEST_PHONE_BOOK, 0xa6);

                startDownPhoneBook();
            } else if (mDialogTitle == R.string.download_pb_phone) {
                hideTrEdit();
                // mStartDownload = 0;
                if (mUI != R.id.phonebook) {
                    showUI(R.id.phonebook);
                }
                //				SaveData.setFirstConnectNeedDown(ATBluetooth.mCurrentMac, false);

                startDownPhoneBook();
            } else if (mDialogTitle == DIALOG_CALL_LOG) {
                if (mCalllogForDial != null) {
                    mATBluetooth.write(ATBluetooth.REQUEST_CALL,

                            mCalllogForDial.mNumber);
                }
            } else if (mDialogTitle == R.string.add_to_favorite) {
                if (mAdapterPhonebookFavorite == null) {
                    mAdapterPhonebookFavorite = new AdapterPhoneBookFavorite(mThis);
                }


                mAdapterPhonebookFavorite.addToFavorite(mFavoriteList, mForFavoriteIndex);
            } else if (mDialogTitle == R.string.delete_favorite) {
                if (mAdapterPhonebookFavorite != null) {
                    mAdapterPhonebookFavorite.remove(mForFavoriteIndex);
                    mAdapterPhonebookFavorite.notifyDataSetChanged();
                }
            } else if (mDialogTitle == R.string.remove_pb_favorite) {
                if (mAdapterPhonebookFavorite != null) {
                    mAdapterPhonebookFavorite.removeAll();
                    mAdapterPhonebookFavorite.notifyDataSetChanged();
                }
            } else if (mDialogTitle == R.string.connection) {
                doConnect();
            } else if (mDialogTitle == R.string.if_del) {
                doDelOneRecord();

                // case R.string.remove_pb_phone:
                //
                // break;
                // case R.string.remove_pb_phone:
                //
                // break;
                // case R.string.remove_pb_phone:
                //
                // break;
            }
            if (preTitle == mDialogTitle) {
                mDialogTitle = 0;
            }
        }
    };
    private boolean mRequestVersion = false;

    private void doDial() {
        if (ATBluetooth.mCurrentHFP <= ATBluetooth.HFP_INFO_CALLED) {
            mCallNumber = mDigit.getText().toString();
            if ((mCallNumber != null) && (mCallNumber.length() != 0)) {
                if (mCallNumber.equals("**000**")) {
                    doUpdateIVT();
                } else if (mCallNumber.equals("**1234567890987**")) {
                    updateBtLib();
                }

                if (mCallNumber.equals("******")) {
                    mRequestVersion = true;
                    mATBluetooth.write(ATBluetooth.REQUEST_REQUEST_VERSION);
                } else {
                    mCallName = null;
                    mATBluetooth.write(ATBluetooth.REQUEST_CALL, mCallNumber);
                }

            }
        } else if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
            // mATBluetooth.write(ATBluetooth.REQUEST_ANSWER);

            if (m3CallTextView != null && m3CallTextView.getVisibility() == View.VISIBLE && ATBluetooth.mCallingNum <= 1) {
                mATBluetooth.write(ATBluetooth.REQUEST_3CALL_ADD, m3CallTextView.getText().toString());
            } else {
                ATBluetoothService.doAnswerService();
            }
        } else if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
            ATBluetoothService.doAnswerService();
        }
    }

    private void doHang() {
        // if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_INCOMING) {
        // mATBluetooth.write(ATBluetooth.REQUEST_EJECT);
        // } else {
        // mATBluetooth.write(ATBluetooth.REQUEST_HANG);
        // }
        ATBluetoothService.doHangService();
    }

    private void doVoiceSwitch() {
        if (ATBluetooth.mCurrentHFP >= ATBluetooth.HFP_INFO_CALLED) {
            if (!GlobalDef.isLock()) {
                GlobalDef.lock();
                mATBluetooth.write(ATBluetooth.REQUEST_VOICE_SWITCH);

            }
        }
    }

    public static ATBluetoothActivity mThis;
    public static ATBluetoothActivity mThis2;

    public static void doKey(int key) {
        if (mThis != null) {
            mThis.doKeyEx(key);
        }

    }

    private void doNum(int n) {

    }

    private void doKeyEx(int key) {
        if (mUI != R.id.dial) {
            showUI(R.id.dial);
        }

        switch (key) {
            case MyCmd.Keycode.BT_DIAL:
                doDial();
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
                mDigit.setText("" + mDigit.getText() + (key - MyCmd.Keycode.NUMBER0));
                break;
            case MyCmd.Keycode.NUMBER_STAR:
                mDigit.setText("" + mDigit.getText() + "*");
                break;
            case MyCmd.Keycode.NUMBER_POUND:
                mDigit.setText("" + mDigit.getText() + "#");
                break;
        }

    }

    // update time
    TextView mTextViewTime;

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
                    if (ATBluetooth.m3CallStatus == ATBluetooth.HFP_INFO_INCOMING) {
                        s2 = getString(R.string.incoming);
                    } else if (ATBluetooth.m3CallStatus == ATBluetooth.HFP_INFO_CALLED) {
                        s2 = getString(R.string.calling);
                    } else {
                        if (ATBluetooth.mTime2 < 3600) {
                            s2 = String.format("%02d:%02d", (ATBluetooth.mTime2 / 60), (ATBluetooth.mTime2 % 60));

                        } else {
                            s2 = String.format("%02d:%02d:%02d", (ATBluetooth.mTime2 / 3600), ((ATBluetooth.mTime2 % 3600) / 60), (ATBluetooth.mTime2 % 60));
                        }
                    }
                    s = getString(R.string.pause);
                } else {
                    s2 = getString(R.string.pause);
                }
                m3CallStatus.setText(s2);
            }
            mTextViewTime.setText(s);
        }

        mHandlerTime.removeMessages(0);
        if (!mPausing) {
            ATBluetooth.mTime++;
            if (ATBluetooth.mCallingNum > 1) {
                ATBluetooth.mTime2++;
            }
            mHandlerTime.sendEmptyMessageDelayed(0, 1000);
        }
    }

    private void startUpdateTime(int text) {
        if (text == 0) {
            if (ATBluetooth.mCallingNum < 2) {
                ATBluetooth.mTime = 0;
            }
            ATBluetooth.mTime2 = 0;
            updateTime();
        } else if (text == -1) {
            updateTime();
        } else {
            mTextViewTime.setText(text);
        }
    }

    private void stopUpdateTime() {
        mHandlerTime.removeMessages(0);
        if (mTextViewTime != null) {
            mTextViewTime.setText("");
        }
        if (mDigit != null) {
            mDigit.setText("");
        }

        if (m3CallLayout != null) {
            m3CallLayout.setVisibility(View.GONE);
            m3CallAdd = false;
        }
    }

    private Handler mHandlerTime = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updateTime();
        }
    };

    private final static int CHECK_SAVING = 10000;
    private Handler mHandlerSort = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CHECK_SAVING) {
                Log.d("allen", "CHECK_SAVING" + mUpdateTextTask);
                if (mUpdateTextTask != null) {
                    if (mToast != null) {
                        mToast.cancel();
                        mToast = Toast.makeText(mThis, "", Toast.LENGTH_SHORT);
                    }

                    mToast.setText(getString(R.string.saving_phone_book));
                    mToast.show();
                    mHandlerSort.sendEmptyMessageDelayed(CHECK_SAVING, 2000);
                }
            } else if (msg.what >= 100) {

                mDownloading.setVisibility(View.GONE);
                mAdapterPhonebook.notifyDataSetChanged();
            } else {

                mDownloading.setText(getString(R.string.sorting) + "  " + msg.what + "%");
            }
        }
    };
    boolean mSorting = false;

    class UpdateTextTask extends AsyncTask<Void, Integer, Integer> {

        UpdateTextTask() {
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(Void... params) {

            if (ATBluetooth.mCurrentMac == null || mPhoneBookInfo.size() == 0) {
                mHandlerSort.post(new Runnable() {
                    public void run() {
                        mDownloading.setVisibility(View.GONE);
                    }
                });
                return 0;
            }
            String mMacForSave = null;

            ArrayList<PhoneBook> mSaveList = null;

            //			if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_IVT) {
            mSorting = true;
            // mAdapterPhonebook.notifyDataSetChanged();

            mHandlerSort.sendEmptyMessage(0);
            SaveData.sortPhoneBook(mPhoneBookInfo, mHandlerSort, this);
            mSorting = false;

            //			}
            mHandlerSort.sendEmptyMessage(100);
            mHandlerSort.post(new Runnable() {
                public void run() {

                    mToast.setText(getString(R.string.saving_phone_book));
                    mToast.show();
                    if (GlobalDef.mFavorite == 1) { // 200 ui do this
                        mHandlerSort.sendEmptyMessageDelayed(CHECK_SAVING, 2000);
                    }
                }
            });

            if (mPhoneBook == 0) {
                mSaveList = mPhoneBookInfo;
                mMacForSave = SaveData.TAG_PHONE_BOOK + ATBluetooth.mCurrentMac;

            } else if (mPhoneBook == 1) {
                mSaveList = mPhoneBookSimInfo;
                mMacForSave = SaveData.TAG_PHONE_BOOK_SIM + ATBluetooth.mCurrentMac;
            }

            mPhoneBook = 0;
            long l = System.currentTimeMillis();
            int i = 0;
            try {

                if ((mSaveList != null) && (mSaveList.size() > 0)) {

                    SaveData.createTable(mMacForSave);
                    PhoneBook pb;
                    char mPrePy = 0;
                    char cPy = 0;

                    for (i = 0; i < mSaveList.size(); ++i) {
                        if (isCancelled()) {
                            Log.d("allen", "doInBackground cancel");
                            break;
                        }

                        pb = mSaveList.get(i);

                        if (i == 0) {
                            SaveData.mCurExistPhoneTagChar = "";
                        }

                        if (pb.mPinyin != null && pb.mPinyin.length() > 0) {
                            cPy = pb.mPinyin.charAt(0);
                            ;
                            if (mPrePy != cPy) {
                                SaveData.mCurExistPhoneTagChar += cPy;
                            }
                            mPrePy = cPy;
                        }

                        if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {

                            SaveData.saveData(mMacForSave, pb.mName, pb.mNumber, pb.mIndex);
                        } else {

                            SaveData.saveData(mMacForSave, pb.mName, pb.mNumber);
                        }
                        try {
                            if (ATBluetoothService.mContactsUtils != null) {
                                ATBluetoothService.mContactsUtils.add(pb.mName, pb.mNumber, ATBluetooth.mCurrentMac);
                            }
                        } catch (Exception e) {

                            Log.d(TAG, "mContactsUtils:" + e);
                        }
                        // saveData(mMacForSave, " pb.mNumber);
                        // ++i;
                        // Log.e("savePhoneBookData", pb.mName + ":" +
                        // pb.mNumber);

                        // Log.d("dd", this + ":doInBackground" + i);
                    }

                    if (ATBluetoothService.mContactsUtils != null) {
                        ATBluetoothService.mContactsUtils.notifyWacTalk(ATBluetooth.mCurrentMac);
                    }
                    ATBluetoothService.sendHFPInfoToVA(ATBluetoothActivity.this, 100, ATBluetooth.mCurrentMac, null, null);
                }
            } catch (Exception e) {

                Log.d(TAG, i + "doInBackground err:" + e);
            }

            mUpdateTextTask = null;

            Log.d("allen", "doInBackground finish");

            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    private void doBackKey() {

        if (mDownload == 1) {
            stopDownPhoneBook();// end
            mDownload = 0;
        }

        View kb = findViewById(R.id.phonebook_keyboard);
        if (kb != null && kb.getVisibility() == View.VISIBLE) {
            kb.setVisibility(View.GONE);
            return;
        }

        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Util.isRKSystem() && (keyCode == KeyEvent.KEYCODE_F7 || keyCode == KeyEvent.KEYCODE_F8 || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_TAB)) {
            return super.onKeyDown(keyCode, event);
        }
        boolean ret = true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                doBackKey();
                break;
            }
            // case KeyEvent.KEYCODE_DPAD_UP: {
            // return doButtonUp();
            //
            // }
            case KeyEvent.KEYCODE_F7:
                return rollKeyTranslate2(MyCmd.Keycode.KEY_ROOL_LEFT);
            case KeyEvent.KEYCODE_F8:
                return rollKeyTranslate2(MyCmd.Keycode.KEY_ROOL_RIGHT);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return keyevntTranslate(keyCode);
            case KeyEvent.KEYCODE_TAB:
                if (event.isShiftPressed()) {
                    ret = rollKeyTranslate(MyCmd.Keycode.KEY_ROOL_LEFT);
                } else {
                    ret = rollKeyTranslate(MyCmd.Keycode.KEY_ROOL_RIGHT);
                }
                break;
            // case KeyEvent.KEYCODE_MEDIA_NEXT:
            // rollKeyTranslate(MyCmd.Keycode.KEY_ROOL_RIGHT);
            // break;
            // case KeyEvent.KEYCODE_F2:
            // case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            // rollKeyTranslate(MyCmd.Keycode.KEY_ROOL_LEFT);
            // break;
            // case KeyEvent.KEYCODE_DPAD_RIGHT:
            // case KeyEvent.KEYCODE_DPAD_LEFT:
            // return keyevntTranslate(keyCode);

            default:
                return super.onKeyDown(keyCode, event);
        }

        // if(!ret){
        // return super.onKeyDown(keyCode, event);
        // }
        return true;
    }

    private int mTest = 0;

    private int isNeedToRoll(int id, int keyCode) {
        int ret = 0;
        // if (MachineConfig.VALUE_CAR_TYPE_LEXUS.equals(mCarType)) {
        // if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        // switch (id) {
        // case R.id.back:
        // // case R.id.down:
        // // ret = R.id.lv_scan_usb;
        // break;
        // }
        // } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        // switch (id) {
        // case R.id.home:
        //
        // // ret = R.id.lv_scan_usb;
        // break;
        // }
        //
        // }
        //
        // } else {

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            switch (id) {
                // case R.id.back:
                // ret = R.id.home;
                // break;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            switch (id) {
                // case R.id.home:
                // ret = R.id.back;
                // break;
            }

        }
        // }

        return ret;
    }

    private boolean keyevntTranslate(int keyCode) {
        boolean ret = false;
        View v = getCurrentFocus();
        if (v != null) {
            int id = isNeedToRoll(v.getId(), keyCode);
            if (id != 0) {
                v.clearFocus();
                findViewById(id).requestFocus();
                ret = true;
            }
        }
        return ret;

    }

    private boolean rollKeyTranslate2(int keyCode) {
        boolean ret = false;
        View v = getCurrentFocus();
        if (v == null) {
            // v = findViewById(R.id.home);
        }

        int id = 0;
        if (v != null) {
            id = v.getId();
        }

        if (id != 0) {
            int key = 0;
            switch (keyCode) {
                case MyCmd.Keycode.KEY_ROOL_RIGHT:
                    if (id == R.id.dial || id == R.id.calllog || id == R.id.phonebook || id == R.id.pair || id == R.id.setting || id == R.id.a2dp) {
                        key = Kernel.KEY_RIGHT;
                    }
                    break;
                case MyCmd.Keycode.KEY_ROOL_LEFT:
                    if (id == R.id.calllog || id == R.id.phonebook || id == R.id.pair || id == R.id.setting || id == R.id.a2dp || id == R.id.dial) {
                        key = Kernel.KEY_LEFT;
                    }
                    break;
            }

            if (key != 0) {
                Kernel.doKeyEvent(key);
                ret = true;
            } else {
                findViewById(R.id.dial).requestFocus();
                findViewById(R.id.dial).requestFocusFromTouch();
            }
        } else {
            findViewById(R.id.dial).requestFocus();
            findViewById(R.id.dial).requestFocusFromTouch();
        }

        return ret;

    }

    private boolean rollKeyTranslate(int keyCode) {
        boolean ret = false;
        View v = getCurrentFocus();
        if (v == null) {
            // v = findViewById(R.id.home);
        }
        int nextFocus = 0;
        if (v != null) {
            int id = v.getId();// isNeedToRoll(v.getId(), keyCode);
            int key = 0;

            switch (keyCode) {
                case MyCmd.Keycode.KEY_ROOL_RIGHT:
                    // case R.id.home:
                    if (id == R.id.prev || id == R.id.pp || id == R.id.next || id == R.id.dial || id == R.id.calllog || id == R.id.phonebook || id == R.id.pair || id == R.id.setting || id == R.id.calling_voice || id == R.id.reject || id == R.id.n1 || id == R.id.n2 || id == R.id.n3 || id == R.id.n4 || id == R.id.n5 || id == R.id.n6 || id == R.id.n7 || id == R.id.n8 || id == R.id.n9) {// case R.id. a2dp:
                        //
                        // case R.id.calling_speak:


                        key = Kernel.KEY_RIGHT;
                    } else if (id == R.id.tl_0 || id == R.id.tl_1 || id == R.id.tl_2) {
                        key = Kernel.KEY_DOWN;
                    } else if (id == R.id.delete) {
                        nextFocus = R.id.n1;
                    } else if (id == R.id.nx) {
                        nextFocus = R.id.n4;
                    } else if (id == R.id.n0) {
                        nextFocus = R.id.n7;
                        // case R.id.n9:
                        // nextFocus = R.id.nx;
                        // break;
                        // case R.id.nj:
                        // nextFocus = R.id.num_dial;
                        // break;
                    }
                    break;
                case MyCmd.Keycode.KEY_ROOL_LEFT:
                    // case R.id.back:
                    if (id == R.id.pp || id == R.id.next || id == R.id.calllog || id == R.id.phonebook || id == R.id.pair || id == R.id.setting || id == R.id.a2dp || id == R.id.answering || id == R.id.calling_voice || id == R.id.reject || id == R.id.nx || id == R.id.n2 || id == R.id.n3 || id == R.id.n0 || id == R.id.n5 || id == R.id.n6 || id == R.id.nj || id == R.id.n8 || id == R.id.n9) {// case R.id.n3:
                        // case R.id.n2:
                        // case R.id.n6:
                        // case R.id.n5:
                        // case R.id.n9:
                        // case R.id.n8:
                        // case R.id.n0:
                        // case R.id.nj:

                        // case R.id.dial:


                        key = Kernel.KEY_LEFT;
                        // case R.id.tl_0:
                    } else if (id == R.id.tl_1 || id == R.id.tl_2 || id == R.id.tl_3) {
                        key = Kernel.KEY_UP;
                    } else if (id == R.id.n1) {
                        nextFocus = R.id.delete;
                    } else if (id == R.id.n4) {
                        nextFocus = R.id.nx;
                    } else if (id == R.id.n7) {
                        nextFocus = R.id.n0;
                    }
                    break;

            }

            if (id == R.id.tr_list || id == R.id.t_list) {// Log.d("aa", "" + ((ListView) v).getCount() + ":"
                // + ((ListView) v).getSelectedItemPosition());

                switch (keyCode) {
                    case MyCmd.Keycode.KEY_ROOL_RIGHT:

                        if (((ListView) v).getSelectedItemPosition() < (((ListView) v).getCount() - 1)) {
                            key = Kernel.KEY_DOWN;
                        }
                        break;
                    case MyCmd.Keycode.KEY_ROOL_LEFT:
                        if (((ListView) v).getSelectedItemPosition() > 0) {
                            key = Kernel.KEY_UP;
                        }
                        break;
                }
                ret = true;
            }
            if (key != 0) {
                Kernel.doKeyEvent(key);
                ret = true;
            } else if (nextFocus != 0) {
                v.clearFocus();

                ret = true;
                findViewById(nextFocus).requestFocus();
            } else {
                switch (keyCode) {
                    case MyCmd.Keycode.KEY_ROOL_RIGHT:
                        if (id == R.id.tl_3) {
                            ret = true;
                        }
                        break;
                    case MyCmd.Keycode.KEY_ROOL_LEFT:
                        if (id == R.id.tl_0) {
                            ret = true;
                        }
                        break;
                }
            }
        }

        return ret;

    }

    private void resetCallEndUI() {
        if (mUI == R.id.dial) {
            ((ImageView) findViewById(R.id.tl_3)).setImageResource(R.drawable.mute1);
            ((ImageView) findViewById(R.id.tl_2)).setImageResource(R.drawable.voice);
        }

    }

    // private boolean isIVTConnectDevice(String mac){
    // if (mac.equals(ATBluetooth.mCurrentMac)) {
    // return true;
    // } else {
    // return false;
    // }
    //
    // }
    //
    // private boolean isParrotConnectDevice(String mac) {
    //
    // for (PhoneBook pb : mPairInfo) {
    // if (pb.mNumber.equals(mac)) {
    // return true;
    // }
    // }
    // return false;
    // }
    private void showPairList() {

    }

    private String getParrotPairDeviceName(String mac) {

        for (PhoneBook pb : mPairInfo) {
            if (pb.mNumber.equals(mac)) {
                return pb.mName;
            }
        }
        return null;
    }

    private boolean isConnectDevice(String mac) {
        if (mac.equals(ATBluetooth.mCurrentMac)) {
            return true;
        } else {
            return false;
        }

        // if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
        // return isParrotConnectDevice(mac);
        // } else {
        // return isIVTConnectDevice(mac);
        // }

    }

    // /new ui ......... before is bad ui...
    private void setViewVisible(int id, int visibility) {
        View v = findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void setSwitch(int id, int on) {
        ImageView v = (ImageView) findViewById(id);
        if (v != null) {
            Drawable d;

            d = v.getDrawable();
            if (d != null) {
                d.setLevel(on);
            } else {
                d = v.getBackground();
                if (d != null) {
                    d.setLevel(on);
                }
            }
        }
    }

    private void setTextView(int id, String text) {
        TextView v = (TextView) findViewById(id);
        if (v != null) {
            v.setText(text);
        }
    }

    public void showUI8702(int id) {
        stopSearch();

        View v = findViewById(R.id.btconnected_layout);
        if (v != null) {
            if (id == R.id.setting || id == R.id.dial) {
                if (id == R.id.dial) {
                    if (mDigit.length() == 0) {
                        v.setVisibility(View.VISIBLE);
                    } else {
                        v.setVisibility(View.GONE);
                    }
                } else {
                    v.setVisibility(View.VISIBLE);
                }
            } else {
                v.setVisibility(View.GONE);
            }
        }
        if (mUI != id) {
            mATBluetooth.sendHandler("ATBluetoothService", 0);// ??
            if (R.id.dial == id) {

                setViewVisible(R.id.main_page_no_dial, View.GONE);
                setViewVisible(R.id.layout_set, View.GONE);

                mTrA2dp.setVisibility(View.GONE);

                mTrDial.setVisibility(View.VISIBLE);

                mATBluetooth.sendHandler("ATBluetoothService", 1);

                mTrList.setAdapter(null);
                mTList.setAdapter(null);
            } else if (R.id.setting == id) {
                setViewVisible(R.id.main_page_no_dial, View.GONE);
                setViewVisible(R.id.layout_set, View.VISIBLE);

                mTrDial.setVisibility(View.GONE);

                mTrList.setAdapter(null);
                mTList.setAdapter(null);
                mTrA2dp.setVisibility(View.GONE);

                mATBluetooth.write(ATBluetooth.REQUEST_NAME);
                mATBluetooth.write(ATBluetooth.REQUEST_PIN);
                mATBluetooth.write(ATBluetooth.REQUEST_SETTING);
                mATBluetooth.write(ATBluetooth.REQUEST_AUTOCONNECT);

            } else if (R.id.a2dp == id) {

                //				if (ATBluetooth.mCurrentHFP < ATBluetooth.HFP_INFO_CONNECTED) {
                //					return;
                //				}

                setViewVisible(R.id.main_page_no_dial, View.GONE);
                setViewVisible(R.id.layout_set, View.GONE);

                mTrDial.setVisibility(View.GONE);

                mTrList.setAdapter(null);
                mTList.setAdapter(null);

                mTrA2dp.setVisibility(View.VISIBLE);
                ATBluetoothService.musicKeyControl(MyCmd.Keycode.PLAY);
                // mATBluetooth.requestSource();
                BroadcastUtil.sendToCarServiceSetSource(this, MyCmd.SOURCE_BT_MUSIC);
                GlobalDef.requestAudioFocus(this);
            } else {
                setViewVisible(R.id.main_page_no_dial, View.VISIBLE);
                setViewVisible(R.id.layout_set, View.GONE);
                if (id == R.id.calllog) {
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

                    if (mLayoutCallLogs == null) {

                        mTrList.setVisibility(View.VISIBLE);
                        mTList.setVisibility(View.GONE);
                        mTList.setAdapter(null);
                        mTl1.setVisibility(View.VISIBLE);
                        mTl2.setVisibility(View.VISIBLE);
                        mTl3.setVisibility(View.VISIBLE);
                        // mTrA2dp.setVisibility(View.GONE);
                        mTl0.setImageResource(R.drawable.call_missed);
                        mTl0.getBackground().setLevel((mCalllog == 0) ? 1 : 0);
                        mTl1.setImageResource(R.drawable.call_received);
                        mTl1.getBackground().setLevel((mCalllog == 1) ? 1 : 0);
                        mTl2.setImageResource(R.drawable.call_outgoing);
                        mTl2.getBackground().setLevel((mCalllog == 2) ? 1 : 0);
                        mTl3.setImageResource(R.drawable.remove);
                        // mTrDial.setVisibility(View.INVISIBLE);
                        mTrListBg.setVisibility(View.VISIBLE);
                        mTNormal.setVisibility(View.VISIBLE);

                        switch (mCalllog) {
                            case 0:
                                mTrList.setAdapter(mAdapterCallM);
                                mAdapterCallM.notifyDataSetChanged();
                                break;
                            case 1:
                                mTrList.setAdapter(mAdapterCallR);
                                mAdapterCallR.notifyDataSetChanged();
                                break;
                            case 2:
                                mTrList.setAdapter(mAdapterCallO);
                                mAdapterCallO.notifyDataSetChanged();
                                break;
                        }

                    } else {

                        mLayoutCallLogs.setVisibility(View.VISIBLE);
                        // mListViewPhonebook.setAdapter(mAdapterPhonebook);
                        setViewVisible(R.id.main_page_no_dial, View.GONE);
                        setViewVisible(R.id.layout_set, View.GONE);

                        if (mLayoutPhonebook != null) {
                            mLayoutPhonebook.setVisibility(View.GONE);
                        }

                        if (mLayoutPair != null) {
                            mLayoutPair.setVisibility(View.GONE);
                        }

                        switch (mCalllog) {
                            case 0:
                                mListViewCallLogs.setAdapter(mAdapterCallM);
                                mAdapterCallM.notifyDataSetChanged();
                                break;
                            case 1:
                                mListViewCallLogs.setAdapter(mAdapterCallR);
                                mAdapterCallR.notifyDataSetChanged();
                                break;
                            case 2:
                                mListViewCallLogs.setAdapter(mAdapterCallO);
                                mAdapterCallO.notifyDataSetChanged();
                                break;
                        }
                        updateCallLogPageSel();
                    }

                    mTrDial.setVisibility(View.INVISIBLE);
                    mTrA2dp.setVisibility(View.GONE);
                } else if (id == R.id.phonebook) {
                    if (mLayoutPhonebook == null) {
                        mTrList.setVisibility(View.VISIBLE);
                        mTList.setVisibility(View.GONE);
                        mTList.setAdapter(null);
                        mTl1.setVisibility(View.VISIBLE);
                        mTl2.setVisibility(View.VISIBLE);
                        mTl3.setVisibility(View.VISIBLE);
                        mTl0.setImageResource(R.drawable.pb_phone);
                        mTl1.setImageResource(R.drawable.search);
                        mTl2.setImageResource(R.drawable.download);
                        mTl2.getBackground().setLevel(0);
                        mTl3.setImageResource(R.drawable.remove);
                        mTrListBg.setVisibility(View.VISIBLE);
                        mTNormal.setVisibility(View.VISIBLE);

                        mTrList.setAdapter(mAdapterPhonebook);

                    } else {
                        mLayoutPhonebook.setVisibility(View.VISIBLE);
                        mListViewPhonebook.setAdapter(mAdapterPhonebook);
                        setViewVisible(R.id.main_page_no_dial, View.GONE);
                        setViewVisible(R.id.layout_set, View.GONE);

                        if (mLayoutCallLogs != null) {
                            mLayoutCallLogs.setVisibility(View.GONE);
                        }

                        if (mLayoutPair != null) {
                            mLayoutPair.setVisibility(View.GONE);
                        }
                    }

                    mTrDial.setVisibility(View.INVISIBLE);
                    mTrA2dp.setVisibility(View.GONE);

                    mAdapterPhonebook.notifyDataSetChanged();

                    if (mSorting || mDownload == 1) {
                        mDownloading.setVisibility(View.VISIBLE);
                    }
                } else if (id == R.id.favorite) {
                    if (mLayoutFavorate == null) {

                        mTrList.setVisibility(View.VISIBLE);
                        mTList.setVisibility(View.GONE);
                        mTList.setAdapter(null);
                        mTrA2dp.setVisibility(View.GONE);
                        // mTl0.setVisibility(View.GONE);
                        mTl1.setVisibility(View.INVISIBLE);
                        mTl2.setVisibility(View.INVISIBLE);
                        mTl3.setVisibility(View.INVISIBLE);
                        mTl0.setImageResource(R.drawable.remove);
                        mTl0.getBackground().setLevel(0);
                        mTl1.setImageDrawable(null);
                        mTl2.setImageDrawable(null);
                        // mTl2.setVisibility(View.VISIBLE);
                        mTl3.setImageDrawable(null);
                        // mTl3.setVisibility(View.VISIBLE);
                        // mTrA2dp.setVisibility(View.INVISIBLE);
                        mTrDial.setVisibility(View.INVISIBLE);
                        mTrListBg.setVisibility(View.VISIBLE);
                        mTNormal.setVisibility(View.VISIBLE);


                        if (mAdapterPhonebookFavorite == null) {
                            mAdapterPhonebookFavorite = new AdapterPhoneBookFavorite(this);
                        }
                        mTrList.setAdapter(mAdapterPhonebookFavorite);

                        mAdapterPhonebookFavorite.notifyDataSetChanged();


                        if (mSorting || mDownload == 1) {
                            mDownloading.setVisibility(View.VISIBLE);
                        }

                    } else {

                    }

                    mTrDial.setVisibility(View.INVISIBLE);
                    mTrA2dp.setVisibility(View.GONE);

                    //					mAdapterPhonebook.notifyDataSetChanged();

                    if (mSorting || mDownload == 1) {
                        mDownloading.setVisibility(View.VISIBLE);
                    }
                } else if (id == R.id.pair) {
                    if (mLayoutPair == null) {
                        mTList.setVisibility(View.GONE);
                        mTList.setAdapter(null);

                        mTl1.setVisibility(View.VISIBLE);
                        mTl2.setVisibility(View.VISIBLE);
                        mTl3.setVisibility(View.VISIBLE);
                        mTl0.setImageResource(R.drawable.paired);
                        mTl0.getBackground().setLevel((mPair == 0) ? 1 : 0);
                        mTl1.setImageResource(R.drawable.search);
                        mTl1.getBackground().setLevel((mPair == 1) ? 1 : 0);
                        mTl2.setImageResource(R.drawable.disconnect);
                        mTl2.getBackground().setLevel(0);
                        mTl3.setImageResource(R.drawable.remove);

                        mTrListBg.setVisibility(View.VISIBLE);
                        mTNormal.setVisibility(View.VISIBLE);

                        mTrList.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();

                        mTrList.setVisibility(View.VISIBLE);
                    } else {
                        mLayoutPair.setVisibility(View.VISIBLE);
                        mAdapterPaired.notifyDataSetChanged();
                        setViewVisible(R.id.main_page_no_dial, View.GONE);
                        setViewVisible(R.id.layout_set, View.GONE);

                        if (mLayoutCallLogs != null) {
                            mLayoutCallLogs.setVisibility(View.GONE);
                        }

                        if (mLayoutPhonebook != null) {
                            mLayoutPhonebook.setVisibility(View.GONE);
                        }
                    }
                    mTrDial.setVisibility(View.INVISIBLE);
                    mTrA2dp.setVisibility(View.GONE);

                    mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                } else if (id == R.id.setting) {
                    mTrList.setVisibility(View.GONE);
                    mTrList.setAdapter(null);
                    mTNormal.setVisibility(View.GONE);
                    mTrA2dp.setVisibility(View.GONE);
                    mTList.setVisibility(View.VISIBLE);

                    mTList.setAdapter(mAdapter2);
                    mATBluetooth.write(ATBluetooth.REQUEST_NAME);
                    mATBluetooth.write(ATBluetooth.REQUEST_PIN);
                    mATBluetooth.write(ATBluetooth.REQUEST_SETTING);
                    mATBluetooth.write(ATBluetooth.REQUEST_AUTOCONNECT);
                }
            }
            mUI = id;
            hideTrEdit();

        }
    }

    private TextView m3CallTextView;
    private TextView m3CallStatus;
    private View m3CallLayout;
    private TextView m3CallTextName;

    private void show3Call() {

    }

    private boolean isUISupport3Call() {
        return (m3CallLayout != null);
    }

    private boolean m3CallAdd = false;

    private void do3CallAdd() {
        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {

            if (ATBluetooth.mCallingNum < 2) {
                m3CallAdd = true;
                m3CallLayout.setVisibility(View.VISIBLE);
                m3CallStatus.setText("");
                set3CallDialText("", "");
                //				m3CallTextView.setText("");
            }
        }
    }

    private void do3Call3Merge() {
        if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CALLING) {
            mATBluetooth.write(ATBluetooth.REQUEST_3CALL_MERGE);
        }
    }


    private void do3CallCome(int index, String num) {
        if (!isUISupport3Call()) {
            if (ATBluetooth.mCallingNum > 1) {
                String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);
                String text = num;
                if (name != null) {
                    text = name + " " + num;
                }
                mCallNumber = mDigit.getText().toString();
                mDigit.setText(text);
            }
        } else {
            if (ATBluetooth.mCallingNum > 1) {
                m3CallLayout.setVisibility(View.VISIBLE);
                String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);

                set3CallDialText(name, num);
            } else {
                m3CallLayout.setVisibility(View.GONE);
            }
        }
    }

    private void do3CallEnd(int status, int index, String num) {

        if (!isUISupport3Call()) {
            if (ATBluetooth.m3CallNumber != null && ATBluetooth.m3CallNumber.equals(num)) {
                String text = mCallNumber;
                if (mCallName != null) {
                    text = mCallName + " " + mCallNumber;
                }
                mDigit.setText(text);
            }
        } else {
            m3CallLayout.setVisibility(View.GONE);

            if (index == 1) {
                if (!MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI)) {
                    mDigit.setText(m3CallTextView.getText());

                } else {
                    //					TextView tvName = (TextView) findViewById(R.id.calling_name);
                    //					if (m3CallTextName.getText().length()>0){
                    //						tvName.setText(m3CallTextName.getText());
                    //					}else{
                    //						tvName.setText(m3CallTextView.getText());
                    //					}
                    //					TextView tvNumber = (TextView) findViewById(R.id.calling_number);
                    //					tvNumber.setText(m3CallTextView.getText());
                    updateCallingNumber(m3CallTextView.getText().toString());
                    if (mDigit.getText().length() > 0 && !m3CallTextView.getText().equals(mDigit.getText())) {
                        mDigit.setText(m3CallTextView.getText());
                    }
                }

                mTextViewTime.setText(m3CallStatus.getText());
                ATBluetooth.mTime = ATBluetooth.mTime2;
            }
            set3CallDialText("", "");
            m3CallStatus.setText("");
        }
    }

    private void do3CallStatus(int status, int index, String num) {
        // ATBluetooth.mCallingNum--;
        if (status == 0) {
            if (!isUISupport3Call()) {
                String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);
                String text = num;
                if (name != null) {
                    text = name + " " + num;
                }
                //

                mDigit.setText(text);
            } else {
                if (index == 2) {
                    m3CallLayout.setVisibility(View.VISIBLE);
                    String name = SaveData.findName(mPhoneBookInfo, mPhoneBookSimInfo, num);
                    set3CallDialText(name, num);


                }
            }
        }
    }

    public void onDialclick(View v) {
        int id = v.getId();
        if (id == R.id.dial_dialout) {
            doDial();
        } else if (id == R.id.dial_handup) {
            doHang();
        } else if (id == R.id.dial_voiceswitch) {
            doVoiceSwitch();
        } else if (id == R.id.call3_swtich) {
            mATBluetooth.write(ATBluetooth.REQUEST_3CALL_ANSWER);
        } else if (id == R.id.call3_add) {
            do3CallAdd();
        } else if (id == R.id.call3_merge) {
            do3Call3Merge();
        }
    }

    public void onSetClick(View v) {
        int id = v.getId();
        if (id == R.id.btname) {
            showModifyDeviceNameDialog();
        } else if (id == R.id.btpin) {
            if (ATBluetooth.mShowPin) {
                showModifyDevicePinDialog();
            }
        } else if (id == R.id.autoanswercheck) {
            if (mSettingValueX[2] == 1) {
                mSettingValueX[2] = 0;
                mATBluetooth.write(ATBluetooth.REQUEST_AUTO_ANSWER_DISABLE);
            } else {
                mSettingValueX[2] = 1;
                mATBluetooth.write(ATBluetooth.REQUEST_AUTO_ANSWER_ENABLE);
            }
            setSwitch(v.getId(), mSettingValueX[2]);
        } else if (id == R.id.autoconnectcheck) {
            if (mSettingValueX[3] == 1) {
                mSettingValueX[3] = 0;
                mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE);
            } else {
                mSettingValueX[3] = 1;
                mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_ENABLE);
            }
            setSwitch(v.getId(), mSettingValueX[3]);
        }
    }

    private void showModifyDeviceNameDialog() {
        LayoutInflater factory = LayoutInflater.from(ATBluetoothActivity.this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
        ((TextView) textEntryView.findViewById(R.id.edit)).setText(mSettingValue[0]);

        Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(ATBluetoothActivity.this, R.style.AlertDialogCustom))

                .setTitle(R.string.device_name).setView(textEntryView).setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mSettingValue[0] = ((TextView) textEntryView.findViewById(R.id.edit)).getText().toString();
                        mATBluetooth.write(ATBluetooth.REQUEST_NAME, mSettingValue[0]);

                    }
                }).setNegativeButton(R.string.alert_dialog_cancel, null).show();
    }

    private void showModifyDevicePinDialog() {
        LayoutInflater factory = LayoutInflater.from(ATBluetoothActivity.this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
        ((TextView) textEntryView.findViewById(R.id.edit)).setText(mSettingValue[1]);

        ((TextView) textEntryView.findViewById(R.id.edit)).setInputType(EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);

        Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(ATBluetoothActivity.this, R.style.AlertDialogCustom)).setTitle(R.string.pin).setView(textEntryView).setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mSettingValue[1] = ((TextView) textEntryView.findViewById(R.id.edit)).getText().toString();
                mATBluetooth.write(ATBluetooth.REQUEST_PIN, mSettingValue[1]);

                mAdapter2.notifyDataSetChanged();
            }
        }).setNegativeButton(R.string.alert_dialog_cancel, null).show();
    }

    // /for favorite
    private int mForFavoriteIndex = -1;
    private ArrayList<PhoneBook> mFavoriteList = null;
    private OnItemLongClickListener mOnLongClickListListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            Log.d("ddd", arg1 + ":" + arg2 + ":" + arg3);
            int position = arg2;
            if (mPhoneBookFilter != null && mTrEdit.getVisibility() == View.VISIBLE) {
                try {
                    mForFavoriteIndex = mPhoneBookFilter.get(position).mIndex;
                } catch (Exception e) {
                    mForFavoriteIndex = position;
                }
            } else {
                mForFavoriteIndex = position;
            }
            // TODO Auto-generated method stub
            if (mUI == R.id.calllog) {
                if (!(GlobalDef.mFavorite == 1)) {
                    return false;
                }
                switch (mCalllog) {
                    case 0:
                        mCalllogForDial = mCalllogMInfo.get(position);
                        mFavoriteList = mCalllogMInfo;
                        break;
                    case 1:
                        mCalllogForDial = mCalllogRInfo.get(position);
                        mFavoriteList = mCalllogRInfo;
                        break;
                    case 2:
                        mCalllogForDial = mCalllogOInfo.get(position);
                        mFavoriteList = mCalllogOInfo;
                        break;
                    default:
                        mFavoriteList = null;
                }
                if (mFavoriteList != null) {
                    showDialogId(R.string.add_to_favorite, (((mCalllogForDial.mName != null) && (mCalllogForDial.mName.length() != 0)) ? mCalllogForDial.mName : mCalllogForDial.mNumber) + "  " + getString(R.string.add_to_favorite));
                }
            } else if (mUI == R.id.phonebook) {
                if (!(GlobalDef.mFavorite == 1)) {
                    return false;
                }

                final PhoneBook book;
                switch (mPhoneBook) {
                    case 0:
                        if ((mPhoneBookFilter != null) && (mTrEdit.getVisibility() == View.VISIBLE) && (mTrEdit.getText().length() > 0)) {

                            if (position < mPhoneBookFilter.size()) {
                                book = mPhoneBookFilter.get(position);
                                //							mFavoriteList = mPhoneBookFilter;
                            } else {
                                return false;
                            }

                        } else {
                            book = mPhoneBookInfo.get(position);
                            //						mFavoriteList = mPhoneBookInfo;
                        }
                        mFavoriteList = mPhoneBookInfo;
                        break;
                    default:
                        book = null;
                }
                if (book != null) {
                    String title = "";
                    if ((book.mName != null) && (book.mName.length() != 0)) {
                        title += book.mName;
                    } else {
                        title += book.mNumber;
                    }

                    showDialogId(R.string.add_to_favorite, title + "  " + getString(R.string.add_to_favorite));

                }
            } else if (mUI == R.id.pair) {
                if (ATBluetooth.mBTType != MachineConfig.VAULE_BT_TYPE_PARROT) {
                    return false;
                }

                if (mPair != 1) {
                    showDialogId(R.string.remove_one_pair, getString(R.string.remove_one_pair));
                }
                // break;
                return true;
            } else if (mUI == R.id.favorite) {
                if (mAdapterPhonebookFavorite != null) {
                    PhoneBook book = mAdapterPhonebookFavorite.getPhoneBook(position);
                    if (book != null) {
                        String title = "";
                        if ((book.mName != null) && (book.mName.length() != 0)) {
                            title += book.mName;
                        } else {
                            title += book.mNumber;
                        }

                        showDialogId(R.string.delete_favorite, title + "  " + getString(R.string.delete_favorite));
                    }
                }
            }
            return true;
        }
    };

    private TextView mName;
    private TextView mArtist;
    private TextView mAlbum;

    private void updateId3() {
        try {
            mName = (TextView) findViewById(R.id.song);
            mArtist = (TextView) findViewById(R.id.artist);
            mAlbum = (TextView) findViewById(R.id.album);
            updateAcitivtyID3(ATBluetoothService.mID3Name, ATBluetoothService.mID3Artist, ATBluetoothService.mID3Album);

        } catch (Exception e) {

        }
    }

    public void updateAcitivtyID3(String name, String artist, String album) {
        if (mName != null) {
            mName.setText(name);
        }
        if (mArtist != null) {
            mArtist.setText(artist);
        }
        if (mAlbum != null) {
            mAlbum.setText(album);
        }
    }

    public void onClickSearch(View v) {
        if (v.getId() == R.id.search_text_xing) {
            mTrList.setSelection(mTrList.getCount());
        } else {
            doSearchChar((char) ('A' + (v.getId() - R.id.search_text_a)));
        }
    }

    private View mSearchABCDEF;

    private void doSearchChar(char c) {
        int i = -1;

        do {
            i = getPhoneBookindex(c);
            if (i != -1) {
                break;
            }
        } while (c < 'Z');

        // Log.e(TAG, c + ":" + i + ":" + mAdapterPhonebook.getCount());

        if (i != -1) {
            mTrList.setSelection(i);
        }
    }

    private int getPhoneBookindex(char c) {
        int ret = -1;

        for (int i = 0; i < mPhoneBookInfo.size(); ++i) {
            if (mPhoneBookInfo.get(i).mPinyin != null && mPhoneBookInfo.get(i).mPinyin.length() > 0) {
                if (mPhoneBookInfo.get(i).mPinyin.charAt(0) >= (c)) {
                    ret = i;
                    break;
                }
            }
        }
        return ret;
    }

    private ListView mListViewDialSearch;
    private PhonebookDialFilter mFilterDial = null;
    private MyListAdapterSearchDial mAdapterPhonebookFilterDial;
    private ArrayList<PhoneBook> mPhoneBookFilterDial = null;

    private void intDialNumSearch() {
        View v = findViewById(R.id.pbsearchlist);

        if (v != null) {
            mListViewDialSearch = (ListView) v;
            mListViewDialSearch.setVisibility(View.GONE);

            mListViewDialSearch.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    PhoneBook book = mPhoneBookFilterDial.get(position);
                    mATBluetooth.write(ATBluetooth.REQUEST_CALL, book.mNumber);

                    String s = "";
                    if (book.mName != null) {
                        s += book.mName;
                    }
                    s += book.mNumber;
                    mDigit.setText(s);
                }
            });

            mDigit.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if ((mFilterDial != null) && (mFilterDial.getStatus() != AsyncTask.Status.FINISHED)) {
                        mFilterDial.cancel(true);

                    }
                    View v = findViewById(R.id.btconnected_layout);

                    if (s != null && s.length() > 0) {
                        mFilterDial = (PhonebookDialFilter) new PhonebookDialFilter().execute(s);

                        View dial_info = findViewById(R.id.dial_info);
                        if (dial_info != null && dial_info.getVisibility() == View.VISIBLE) {

                        } else {
                            mListViewDialSearch.setVisibility(View.VISIBLE);
                        }

                        if (v != null) {
                            v.setVisibility(View.GONE);
                        }
                    } else {
                        mListViewDialSearch.setVisibility(View.GONE);
                        if (v != null) {
                            if (mUI == R.id.dial) {
                                v.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private CharSequence prefix;

    private class PhonebookDialFilter extends AsyncTask<CharSequence, Void, ArrayList<PhoneBook>> {
        @Override
        protected ArrayList<PhoneBook> doInBackground(CharSequence... params) {
            if (isCancelled()) return null;

            ArrayList<PhoneBook> phonebookE = new ArrayList<PhoneBook>();

            ArrayList<PhoneBook> phonebook = new ArrayList<PhoneBook>();
            prefix = params[0];
            if ((prefix != null) && (prefix.length() > 0)) {
                String prefixString = prefix.toString().toUpperCase();
                ArrayList<PhoneBook> phonebookInfo = null;

                phonebookInfo = mPhoneBookInfo;

                int count = phonebookInfo.size();
                for (int i = 0; i < count; i++) {
                    PhoneBook book = phonebookInfo.get(i);

                    String name = book.mName;
                    String num = book.mNumber;
                    if (name != null) {
                        name = name.toUpperCase();
                    }
                    if (num != null) {
                        num = num.toUpperCase();
                    }

                    // Log.d("allen", book.mName + ":" + prefixString);
                    if ((name != null) && name.startsWith(prefixString)) {
                        phonebook.add(book);
                    } else if ((book.mPinyin != null) && book.mPinyin.startsWith(prefixString)) {
                        phonebook.add(book);
                    } else if ((num != null) && num.startsWith(prefixString)) {
                        phonebook.add(book);
                    } else if ((name != null) && name.contains(prefixString)) {
                        phonebookE.add(book);
                    } else if ((book.mPinyin != null) && book.mPinyin.contains(prefixString)) {
                        phonebookE.add(book);
                    } else if ((num != null) && num.contains(prefixString)) {
                        phonebookE.add(book);
                    }

                }

                for (int i = 0; i < phonebookE.size(); i++) {
                    phonebook.add(phonebookE.get(i));
                }
            }
            return phonebook;
        }

        @Override
        protected void onPostExecute(ArrayList<PhoneBook> result) {
            if (result == null) return;
            if (mPhoneBookFilterDial != null) {
                mPhoneBookFilterDial.clear();
            }
            mPhoneBookFilterDial = result;

            mAdapterPhonebookFilterDial = new MyListAdapterSearchDial(mThis, mPhoneBookFilterDial);

            mListViewDialSearch.setAdapter(mAdapterPhonebookFilterDial);
            mAdapterPhonebookFilterDial.notifyDataSetChanged();

        }
    }

    public class MyListAdapterSearchDial extends BaseAdapter {
        ArrayList<PhoneBook> mPb;
        private Context mContext;

        public MyListAdapterSearchDial(Context context, ArrayList<PhoneBook> pb) {
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
            View v = LayoutInflater.from(mContext).inflate(R.layout.dial_search_list, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.name = (TextView) v.findViewById(R.id.name);
            vh.number = (TextView) v.findViewById(R.id.number);
            vh.bt = (ImageView) v.findViewById(R.id.bt);
            v.setTag(vh);
            return v;
        }

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();

            PhoneBook book = null;
            if (mPb != null && position < mPb.size()) {

                book = mPb.get(position);
            }

            if (book != null) {
                String name = book.mName;
                if (name == null || name.length() == 0) {
                    name = getString(R.string.unknow);
                }

                vh.name.setText(name);
                // vh.number.setText(book.mNumber);
                try {
                    SpannableString spannableString = new SpannableString(book.mNumber);

                    if (prefix != null) {
                        int start;
                        // start = book.mNumber.startsWith(mCallNumber);
                        start = book.mNumber.indexOf(prefix.toString(), 0);
                        // spannableString.setSpan(new
                        // ForegroundColorSpan(Color.parseColor(#00ff00")), 0,
                        // 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE));
                        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#00ff00")), start, start + prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        vh.number.setText(spannableString);
                    } else {
                        vh.number.setText(book.mNumber);
                    }
                } catch (Exception e) {
                    vh.number.setText(book.mNumber);
                }

                vh.number.setVisibility(View.VISIBLE);
                vh.bt.setVisibility(View.INVISIBLE);
            }

        }

    }

    private View mLayoutPair;

    private ListView mListViewPaired;
    private ListView mListViewPairAvailable;

    private MyListAdapterPair mAdapterPaired;
    private MyListAdapterSearch mAdapterPairAvailable;

    private void updatePairLayout() {
        if (mAdapterPairAvailable != null) {
            // mAdapterPairAvailable.setSelected(-1);
            mAdapterPairAvailable.notifyDataSetChanged();
        }
        if (mAdapterPaired != null) {
            // mAdapterPaired.setSelected(-1);
            mAdapterPaired.notifyDataSetChanged();
        }
    }

    private void removePaired() {
        for (PhoneBook pb : mPairInfo) {
            for (PhoneBook ps : mSearchInfo) {
                if (ps.mNumber.equals(pb.mNumber)) {
                    mSearchInfo.remove(ps);
                    break;
                }
            }
        }
        if (mAdapterPairAvailable != null) {
            mAdapterPairAvailable.notifyDataSetChanged();
        }
    }

    private void clearPairLayout() {
        if (mAdapterPairAvailable != null) {
            mAdapterPairAvailable.setSelected(-1);
            mAdapterPairAvailable.notifyDataSetChanged();
        }
        if (mAdapterPaired != null) {
            mAdapterPaired.setSelected(-1);
            mAdapterPaired.notifyDataSetChanged();
        }
    }

    private void initLayoutPair() {
        mLayoutPair = findViewById(R.id.layout_pair);
        if (mLayoutPair != null) {
            mListViewPaired = (ListView) findViewById(R.id.list_paired);
            mAdapterPaired = new MyListAdapterPair(this);
            mListViewPaired.setAdapter(mAdapterPaired);

            mListViewPairAvailable = (ListView) findViewById(R.id.list_pair_available);
            mAdapterPairAvailable = new MyListAdapterSearch(this);
            mListViewPairAvailable.setAdapter(mAdapterPairAvailable);

            mListViewPaired.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    mAdapterPairAvailable.setSelected(-1);
                    mAdapterPairAvailable.notifyDataSetChanged();

                    mAdapterPaired.setSelected(position);
                    mAdapterPaired.notifyDataSetChanged();
                }
            });

            mListViewPairAvailable.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mAdapterPairAvailable.setSelected(position);
                    mAdapterPairAvailable.notifyDataSetChanged();

                    mAdapterPaired.setSelected(-1);
                    mAdapterPaired.notifyDataSetChanged();
                }
            });
        }
    }

    private void hideLayoutPair() {
        if (mLayoutPair != null) {
            mLayoutPair.setVisibility(View.GONE);
        }
    }

    public void onPariClick(View v) {
        int id = v.getId();
        if (id == R.id.pair_search) {
            mPair = 1;
            if (!mSearch) {
                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                    mATBluetooth.write(ATBluetooth.REQUEST_AUTO_CONNECT_DISABLE);
                    mStartSearch = 0;
                    Util.doSleep(200);
                }
                mATBluetooth.write(ATBluetooth.REQUEST_SEARCH);
                mTrProgress.setVisibility(View.VISIBLE);

                mSearchInfo.clear();
                mAdapterPairAvailable.notifyDataSetChanged();
                mSearch = true;
            } else {
                stopSearch();
            }
        } else if (id == R.id.pair_connect) {
            if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(ResourceUtil.mSystemUI)) {

                int position = mAdapterPaired.getSelected();

                if (position >= 0) {

                    if (!mPairInfo.get(position).mNumber.equals(ATBluetooth.mCurrentMac)) {
                        showDialogId(R.string.connection);
                    }
                } else {
                    position = mAdapterPairAvailable.getSelected();
                    if (position >= 0) {
                        if (!mSearchInfo.get(position).mNumber.equals(ATBluetooth.mCurrentMac)) {
                            showDialogId(R.string.connection);
                        }
                    }
                }
            } else {
                doConnect();
            }

            //
            // String mac = null;
            // String name = null;
            // String py = null;
            // if (position == -1) {
            // position = mAdapterPairAvailable.getSelected();
            // if (position != -1) {
            // mac = mSearchInfo.get(position).mNumber;
            // name = mSearchInfo.get(position).mName;
            // py = mSearchInfo.get(position).mPinyin;
            // }
            // } else {
            // mac = mPairInfo.get(position).mNumber;
            // name = mPairInfo.get(position).mName;
            // py = mPairInfo.get(position).mPinyin;
            // }
            //
            // if (mac != null) {
            // if (mac.equals(ATBluetooth.mCurrentMac)) {
            //
            // } else {
            //
            // if ("1".equals(py)) {
            // mATBluetooth.write(ATBluetooth.REQUEST_PAIR_BY_ADDR,
            // mac);// connect
            //
            // mToast.setText(getString(R.string.connecting) + " "
            // + mac);
            // mToast.show();
            //
            // } else {
            // if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CONNECTED) {
            // showDialogId(R.string.if_diconnect);
            // } else {
            // connectBT(mac, position);
            // mToast.setText(getString(R.string.connecting) + " "
            // + name);
            // mToast.show();
            // }
            // }
            //
            //
            //
            // }
            // }
        } else if (id == R.id.pair_disconnect) {
            int p = mAdapterPaired.getSelected();
            if (p >= 0) {
                PhoneBook pb = mPairInfo.get(p);

                if (ATBluetooth.mCurrentHFP == ATBluetooth.HFP_INFO_CONNECTED) {
                    if (pb != null && !"1".equals(pb.mPinyin)) {
                        showDialogId(R.string.if_diconnect);
                    }
                }
            }
        } else if (id == R.id.pair_del) {
            int p = mAdapterPaired.getSelected();
            if (p < 0) {
                p = mAdapterPairAvailable.getSelected();
                if (p < 0) {

                    // showDialogId(R.string.remove_pair);
                } else {
                    mAdapterPairAvailable.setSelected(-1);
                    if (mSearchInfo.size() > 0) {
                        mSearchInfo.remove(p);
                        mAdapterPairAvailable.notifyDataSetChanged();
                    }
                }
            } else {

                mAdapterPaired.setSelected(-1);
                PhoneBook pb = mPairInfo.get(p);
                if (pb.mNumber.equals(ATBluetooth.mCurrentMac)) {
                    mATBluetooth.write(ATBluetooth.REQUEST_DISCONNECT);
                    if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                        Util.doSleep(300);
                    } else {
                        Util.doSleep(10);
                    }
                }

                if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                    mATBluetooth.write(ATBluetooth.CLEAR_PAIR_INFO, pb.mIndex + "");
                } else {
                    ++p;
                    mATBluetooth.write(ATBluetooth.CLEAR_PAIR_INFO, p + "");
                }

                mPairInfo.clear();

                mAdapterPaired.notifyDataSetChanged();

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                    }
                }, 100);

                // if (ATBluetooth.mBTType !=
                // MachineConfig.VAULE_BT_TYPE_PARROT) {
                // Util.doSleep(100);
                // mATBluetooth.write(ATBluetooth.GET_PAIR_INFO);
                // }

            }
        }
    }

    // 获取Listview所有Item高度总和
    public int getAllItemListViewHeight(ListView listView) {
        BaseAdapter adapter = (BaseAdapter) listView.getAdapter();

        int totalHeight = -1;// 总的高度
        try {
            if (adapter != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    View listItem = adapter.getView(i, null, listView);
                    listItem.measure(0, 0);
                    totalHeight += listItem.getMeasuredHeight();
                }
                // 加上分割线高度
                totalHeight += (listView.getDividerHeight() * (adapter.getCount() - 1));
            } else {
                // will return -1
            }
        } catch (Exception e) {
            totalHeight = -1;
        }
        return totalHeight;
    }

    private void updatePairedListHeight() {
        if (MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI)) {
            return;
        }

        int hei = getAllItemListViewHeight(mListViewPaired);
        if (hei < 0) {
            hei = 0;
        } else {
            hei += 5;
        }

        ViewGroup.LayoutParams lp = mListViewPaired.getLayoutParams();
        lp.height = hei;
        mListViewPaired.setLayoutParams(lp);

    }

    private void updatePaireSearchListHeight() {
        if (MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI)) {
            return;
        }

        int hei = getAllItemListViewHeight(mListViewPairAvailable);
        if (hei < 0) {
            hei = 0;
        } else {
            hei += 8;
        }

        ViewGroup.LayoutParams lp = mListViewPairAvailable.getLayoutParams();
        lp.height = hei;
        mListViewPairAvailable.setLayoutParams(lp);
    }

    private class MyListAdapterSearch extends BaseAdapter {
        public MyListAdapterSearch(Context context) {
            mContext = context;
        }

        private int mC;

        @Override
        public int getCount() {
            if (mC != mSearchInfo.size()) {
                mC = mSearchInfo.size();
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        updatePaireSearchListHeight();
                    }
                });
            }
            return mSearchInfo.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private int mPos = -1;

        public void setSelected(int position) {
            mPos = position;
        }

        public int getSelected() {
            return mPos;
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

            if (position == mPos) {

                v.setBackgroundResource(R.drawable.pair_selected);
            } else {
                v.setBackground(null);
            }
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

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();

            PhoneBook search = mSearchInfo.get(position);
            if (search != null) {
                if ((search.mName != null) && (search.mName.length() != 0)) {
                    vh.name.setText(search.mName);
                } else {
                    vh.name.setText(search.mNumber);
                }
                vh.number.setVisibility(View.INVISIBLE);
                if (isConnectDevice(search.mNumber)) {
                    vh.bt.setVisibility(View.INVISIBLE);

                    if (ATBluetooth.mBTType == MachineConfig.VAULE_BT_TYPE_PARROT) {
                        if (search.mName == null || search.mName.equals(search.mNumber)) {
                            search.mName = getParrotPairDeviceName(search.mNumber);
                            if (search.mName != null) {
                                vh.name.setText(search.mName);
                            }
                        }
                    }

                } else {
                    if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(ResourceUtil.mSystemUI)) {
                        if ("1".equals(search.mPinyin)) {
                            vh.bt.setImageResource(R.drawable.match_iconobd);
                        } else {
                            vh.bt.setImageResource(R.drawable.match_iconphone);
                        }
                        vh.bt.setVisibility(View.VISIBLE);
                    } else {
                        vh.bt.setVisibility(View.INVISIBLE);
                    }
                }

            }

        }

        private Context mContext;
    }

    private int mPrePairedCount = 0;

    private class MyListAdapterPair extends BaseAdapter {
        public MyListAdapterPair(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            if (mPrePairedCount != mPairInfo.size()) {
                // mPrePairedCount = mPairInfo.size();
                // Log.d(TAG, "++"+mPairInfo.size());
                mHandler.removeMessages(MSG_UPDATE_PAIRED_LIST_HEIGHT);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_PAIRED_LIST_HEIGHT, mPairInfo.size(), 0), 500);
                // mHandler.post(new Runnable() {
                //
                // @Override
                // public void run() {
                // // TODO Auto-generated method stub
                // updatePairedListHeight();
                // }
                // });
            }
            return mPairInfo.size();

        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private int mPos = -1;

        public void setSelected(int position) {
            mPos = position;
        }

        public int getSelected() {
            return mPos;
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

            if (position == mPos) {
                v.setBackgroundResource(R.drawable.pair_selected);
            } else {
                v.setBackground(null);
            }

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

        private void bindView(View v, int position, ViewGroup parent) {
            ViewHolder vh = (ViewHolder) v.getTag();
            PhoneBook pair = mPairInfo.get(position);
            if (pair != null) {
                if ((pair.mName != null) && (pair.mName.length() != 0)) {
                    vh.name.setText(pair.mName);
                } else {
                    vh.name.setText(pair.mNumber);
                }
                vh.number.setVisibility(View.INVISIBLE);

                if (pair.mNumber.equals(ATBluetooth.mCurrentMac)) {
                    vh.bt.setVisibility(View.VISIBLE);
                    if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(ResourceUtil.mSystemUI)) {
                        vh.name.setTextColor(0xff00ff00);
                        if ("1".equals(pair.mPinyin)) {
                            vh.bt.setImageResource(R.drawable.match_iconobd);
                        } else {
                            vh.bt.setImageResource(R.drawable.match_iconphone);
                        }
                    }

                } else {
                    if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(ResourceUtil.mSystemUI) || MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(ResourceUtil.mSystemUI)) {
                        if ("1".equals(pair.mPinyin)) {
                            vh.name.setTextColor(0xff00ff00);
                        } else {
                            vh.name.setTextColor(0xffffffff);
                        }

                        if ("1".equals(pair.mPinyin)) {
                            vh.bt.setImageResource(R.drawable.match_iconobd);
                        } else {
                            vh.bt.setImageResource(R.drawable.match_iconphone);
                        }
                        vh.bt.setVisibility(View.VISIBLE);
                    } else {
                        vh.bt.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        private Context mContext;
    }

    public static void updateBySystemConfig() {
        if (mThis2 != null) {
            if (mPausing) {
                mThis2.mUpdateUIResource = true;
            } else {
                ResourceUtil.updateAppUi(mThis2);
            }
            // int ui = mThis2.mUI;
            // mThis2.mUI = -1;
            // mThis2.showUI(ui);
            // boolean restart = false;;
            // restart = !mPausing;
            // mThis2.finish();
            // if(restart){
            // UtilSystem.doRunActivity(mThis2, "com.my.bt",
            // "com.my.bt.ATBluetoothActivity" );
            // }
        }
    }

    public static boolean isA2DPshow() {
        if (mThis2 != null) {
            return mThis2.mStartByA2DP;
        }
        return false;
    }

    public static void showA2DPByConnect() {
        if (mThis2 != null) {
            mThis2.showUI(R.id.a2dp);
        }
        // return false;
    }

    private WakeLock mWakeLock;

    private void wakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            // mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
            // | PowerManager.SCREEN_DIM_WAKE_LOCK
            // | PowerManager.ON_AFTER_RELEASE, TAG);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            mWakeLock.acquire();
        }

    }

    private void wakeRelease() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void doUpdateIVT() {
        List<StorageInfo> ls = UtilSystem.listAllStorage(this);
        File f;
        for (StorageInfo si : ls) {
            if (si.mType == StorageInfo.TYPE_USB || si.mType == StorageInfo.TYPE_SD) {
                String file = si.mPath + "/DSP_parameter.txt";
                f = new File(file);
                if (f.exists()) {
                    Util.sudoExec("cp:" + file + ":/data/bluesoleil/");
                    Util.sudoExec("sync");
                    Util.doSleep(200);
                    return;
                }
            }
        }
    }

    private void updateBtLib() {
        List<StorageInfo> ls = UtilSystem.listAllStorage(this);
        File f;
        for (StorageInfo si : ls) {
            if (si.mType == StorageInfo.TYPE_USB || si.mType == StorageInfo.TYPE_SD) {
                String file = si.mPath + "/libbt_platform.so";
                f = new File(file);
                if (f.exists()) {
                    Util.sudoExec("cp:" + file + ":/oem/lib/i140/");
                    Util.doSleep(10);
                    file = si.mPath + "/libbluelet.so";
                    Util.sudoExec("cp:" + file + ":/oem/lib/i140/");
                    Util.doSleep(10);
                    file = si.mPath + "/blueletd";
                    Util.sudoExec("cp:" + file + ":/oem/lib/i140/");

                    Util.sudoExec("sync");
                    Util.doSleep(200);
                    Util.sudoExec("reboot");
                    return;
                }
            }
        }
        Toast.makeText(this, "没找到蓝牙升级文件", Toast.LENGTH_LONG).show();
    }

    private void doConnect() {
        int position;
        position = mAdapterPaired.getSelected();

        if (position >= 0 && position < mPairInfo.size()) {
            if (!mPairInfo.get(position).mNumber.equals(ATBluetooth.mCurrentMac)) {
                onClickPairedListItem(position);
            }

        } else {
            position = mAdapterPairAvailable.getSelected();
            if (position >= 0 && position < mSearchInfo.size()) {
                if (!mSearchInfo.get(position).mNumber.equals(ATBluetooth.mCurrentMac)) {
                    onClickSearchListItem(position);
                }
            }
        }
    }

    // new phonebook layout
    private View mLayoutPhonebook;

    private ListView mListViewPhonebook;

    private void initLayoutPhonebook() {
        mLayoutPhonebook = findViewById(R.id.layout_phonebook);
        if (mLayoutPhonebook != null) {
            mListViewPhonebook = (ListView) findViewById(R.id.list_phonebook);

            View v;
            for (int i = R.id.keya; i <= R.id.keyz; ++i) {
                v = findViewById(i);
                if (v != null) {
                    v.setOnClickListener(mOnClickListenerSearchABC);
                }
            }

            for (int i = R.id.keyА; i <= R.id.keyЯ; ++i) {
                v = findViewById(i);
                if (v != null) {
                    v.setOnClickListener(mOnClickListenerSearchABC);
                }
            }

            mListViewPhonebook.setOnItemLongClickListener(mOnLongClickListCalllogListener);

            mListViewPhonebook.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI)) {
                        doClickListDialDirect(position);
                    } else {
                        //								mAdapterPhonebook.setSelected(position);
                        //								mAdapterPhonebook.notifyDataSetChanged();
                        try {
                            MyListAdapterEx la = (MyListAdapterEx) mListViewPhonebook.getAdapter();

                            la.setSelected(position);
                            mCalllogForDial = la.getPhoneBookList().get(position);
                            la.notifyDataSetChanged();
                        } catch (Exception e) {

                        }
                    }
                }
            });

        }
    }

    private void hideLayoutPhonebook() {
        if (mLayoutPhonebook != null) {
            mLayoutPhonebook.setVisibility(View.GONE);
        }
    }

    public void onPhonebookClick(View v) {
        View kb = findViewById(R.id.phonebook_keyboard);
        if (kb != null && kb.getVisibility() == View.VISIBLE) {
            kb.setVisibility(View.GONE);
            return;
        }
        int id = v.getId();
        if (id == R.id.phone_download) {
            stopDownPhoneBook();
            showDialogId(R.string.download_pb_phone);
        } else if (id == R.id.phone_search) {
            showTrEdit();
        } else if (id == R.id.phone_search_abc) {
            showKeyboard(true);
        } else if (id == R.id.phone_dial) {
            doPhoneDial();
        } else if (id == R.id.phone_del) {
            showDialogId(R.string.remove_pb_phone);
        }
    }

    private View.OnClickListener mOnClickListenerSearchABC = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            int i = 0;

            char c = 0;
            for (i = R.id.keya; i <= R.id.keyz; ++i) {
                if (id == i) {
                    break;
                }
            }

            if (i != R.id.keyz) {
                c = ((TextView) v).getText().charAt(0);
            }

            if (c == 0) {
                for (i = R.id.keyА; i <= R.id.keyЯ; ++i) {
                    if (id == i) {
                        break;
                    }
                }
                if (i != R.id.keyЯ) {
                    c = ((TextView) v).getText().charAt(0);
                }
            }

            if (c != 0) { // find result
                doSearchCharEx(c);
            }
            showKeyboard(false);
        }
    };

    private void doSearchCharEx(char c) {
        int i = -1;

        do {
            i = getPhoneBookindex(c);
            if (i != -1) {
                break;
            }
        } while (c < 'Z');

        // Log.e(TAG, c + ":" + i + ":" + mAdapterPhonebook.getCount());

        if (i != -1) {
            mListViewPhonebook.setSelection(i);
        }
    }

    private void showKeyboard(boolean b) {
        View kb = findViewById(R.id.phonebook_keyboard);
        if (kb != null) {
            if (b) {
                kb.setVisibility(View.VISIBLE);
                View v;
                CharSequence c;
                for (int i = R.id.keya; i <= R.id.keyz; ++i) {
                    v = findViewById(i);

                    if (v != null) {
                        c = ((TextView) v).getText();
                        if (SaveData.mCurExistPhoneTagChar.contains(c)) {
                            v.setEnabled(true);
                        }
                    }
                }


                v = findViewById(R.id.phonebook_keyboard_ru);
                if (v != null) {

                    if ("ru".equals(Locale.getDefault().getLanguage()) || MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI)) {
                        v.setVisibility(View.VISIBLE);
                        for (int i = R.id.keyА; i <= R.id.keyЯ; ++i) {
                            v = findViewById(i);

                            if (v != null) {
                                c = ((TextView) v).getText();
                                if (SaveData.mCurExistPhoneTagChar.contains(c)) {
                                    v.setEnabled(true);
                                }
                            }
                        }
                    } else {

                        v.setVisibility(View.GONE);
                    }
                }
            } else {
                kb.setVisibility(View.GONE);
            }
        }
    }

    private void doPhoneDial() {
        int position;
        MyListAdapterEx la = (MyListAdapterEx) mListViewPhonebook.getAdapter();

        position = la.getSelected();

        if (position < 0) {
            return;
        }

        // mAdapterPhonebook.setSelected(-1);
        final PhoneBook book;

        book = la.getPhoneBook(position);
        if (book != null) {

            mATBluetooth.write(ATBluetooth.REQUEST_CALL, book.mNumber);
        }
    }

    // new call logs

    private View mLayoutCallLogs;

    private ListView mListViewCallLogs;

    private void hideLayoutCalllog() {
        if (mLayoutCallLogs != null) {
            mLayoutCallLogs.setVisibility(View.GONE);
        }
    }

    private void initLayoutCallLogs() {
        mLayoutCallLogs = findViewById(R.id.layout_phonebook_call_logs);
        if (mLayoutCallLogs != null) {
            mListViewCallLogs = (ListView) findViewById(R.id.list_call_logs);

            mAdapterCallM = new MyListAdapterEx(this, mCalllogMInfo, R.layout.calllog_list);
            mAdapterCallO = new MyListAdapterEx(this, mCalllogOInfo, R.layout.calllog_list);
            mAdapterCallR = new MyListAdapterEx(this, mCalllogRInfo, R.layout.calllog_list);

            if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(system_ui) || MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(system_ui) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(system_ui)) {
                mListViewCallLogs.setOnItemLongClickListener(mOnLongClickListCalllogListener);
            }

            mListViewCallLogs.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(ResourceUtil.mSystemUI)) {
                        doClickListDialDirect(position);
                    } else {
                        switch (mCalllog) {
                            case 0:
                                mAdapterCallM.setSelected(position);
                                mAdapterCallM.notifyDataSetChanged();
                                break;
                            case 1:
                                mAdapterCallR.setSelected(position);
                                mAdapterCallR.notifyDataSetChanged();
                                break;
                            case 2:
                                mAdapterCallO.setSelected(position);
                                mAdapterCallO.notifyDataSetChanged();
                                break;
                        }
                    }
                }
            });

        }
    }

    private void updateCallLogPageSel() {
        int id = R.id.call_log_miss;
        View v = findViewById(R.id.call_log_miss);
        v.setSelected(false);
        v = findViewById(R.id.call_log_received);
        v.setSelected(false);
        v = findViewById(R.id.call_log_call_out);
        v.setSelected(false);

        switch (mCalllog) {
            case 0:
                id = R.id.call_log_miss;
                break;
            case 1:
                id = R.id.call_log_received;
                break;
            case 2:
                id = R.id.call_log_call_out;
                break;
        }
        //
        //
        v = findViewById(id);
        v.setSelected(true);
    }

    public void onCallLogClick(View v) {

        int position = -1;
        MyListAdapterEx ap = null;
        int id = v.getId();
        if (id == R.id.call_log_miss) {
            mCalllog = 0;

            mListViewCallLogs.setAdapter(mAdapterCallM);
            updateCallLogPageSel();
        } else if (id == R.id.call_log_received) {
            mCalllog = 1;
            mListViewCallLogs.setAdapter(mAdapterCallR);
            updateCallLogPageSel();
        } else if (id == R.id.call_log_call_out) {
            mCalllog = 2;
            mListViewCallLogs.setAdapter(mAdapterCallO);
            updateCallLogPageSel();
        } else if (id == R.id.call_log_dial) {
            switch (mCalllog) {
                case 0:
                    ap = mAdapterCallM;
                    break;
                case 1:
                    ap = mAdapterCallR;
                    break;
                case 2:
                    ap = mAdapterCallO;
                    break;
            }
            if (ap != null) {
                position = ap.getSelected();
            }
            if (position < 0) {
                return;
            }

            final PhoneBook book;

            book = ap.getPhoneBook(position);

            if (book != null) {
                mATBluetooth.write(ATBluetooth.REQUEST_CALL, book.mNumber);
            }
        } else if (id == R.id.call_log_del) {
            doDelOneRecord();
        } else if (id == R.id.call_log_del_all) {
            showDialogId(R.string.remove_calllog);
        }
    }

    private void doDelOneRecord() {
        MyListAdapterEx ap = null;
        int position = -1;

        if (mUI == R.id.calllog) {
            switch (mCalllog) {
                case 0:
                    ap = mAdapterCallM;
                    break;
                case 1:
                    ap = mAdapterCallR;
                    break;
                case 2:
                    ap = mAdapterCallO;
                    break;
            }
        } else if (mUI == R.id.phonebook) {
            ap = mAdapterPhonebook;
        }
        if (ap != null) {
            position = ap.getSelected();
            ap.setSelected(-1);
        }
        if (position < 0) {
            return;
        }

        ArrayList<PhoneBook> pb = ap.getPhoneBookList();
        if (pb != null && pb.size() > position) {
            pb.remove(position);
        }
        ap.notifyDataSetChanged();
    }

    //
    private void doClickListDialDirect(int position) {
        if (mUI == R.id.calllog) {
            switch (mCalllog) {
                case 0:
                    mCalllogForDial = mCalllogMInfo.get(position);
                    break;
                case 1:
                    mCalllogForDial = mCalllogRInfo.get(position);
                    break;
                case 2:
                    mCalllogForDial = mCalllogOInfo.get(position);
                    break;
                default:
                    mCalllogForDial = null;
            }
            String title = getString(R.string.if_dial);

            if (mCalllogForDial != null) {
                showDialogId(DIALOG_CALL_LOG, title + (((mCalllogForDial.mName != null) && (mCalllogForDial.mName.length() != 0)) ? mCalllogForDial.mName : mCalllogForDial.mNumber) + "?");
            }
        } else if (mUI == R.id.phonebook) {
            final PhoneBook book;
            switch (mPhoneBook) {
                case 0:
                    if ((mPhoneBookFilter != null) && (mTrEdit.getVisibility() == View.VISIBLE) && (mTrEdit.getText().length() > 0)) {
                        book = mPhoneBookFilter.get(position);
                    } else {
                        book = mPhoneBookInfo.get(position);
                    }
                    break;
                case 1:
                    if ((mPhoneBookSimFilter != null) && (mTrEdit.getVisibility() == View.VISIBLE) && (mTrEdit.getText().length() > 0)) {
                        book = mPhoneBookSimFilter.get(position);
                    } else {
                        book = mPhoneBookSimInfo.get(position);
                    }
                    break;
                default:
                    book = null;
            }
            if (book != null) {
                String title = getString(R.string.if_dial);
                if ((book.mName != null) && (book.mName.length() != 0)) {
                    title += book.mName;
                } else {
                    title += book.mNumber;
                }
                title += "?";
                Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(ATBluetoothActivity.this, R.style.AlertDialogCustom))

                        .setTitle(title).setPositiveButton(R.string.alert_dialog_ok, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mATBluetooth.write(ATBluetooth.REQUEST_CALL, book.mNumber);
                            }
                        }).setNegativeButton(R.string.alert_dialog_cancel, null).show();
                // LayoutParams lp = d.getWindow().getAttributes();
                // lp.dimAmount = 0.0f;
                // d.getWindow().setAttributes(lp);
            }
        } else if (mUI == R.id.pair) {
            switch (mPair) {
                case 0:
                    onClickPairedListItem(position);
                    break;
                case 1:
                    onClickSearchListItem(position);

                    break;
            }
        } else if (mUI == R.id.favorite) {
            final PhoneBook bookFavorite;
            if (mAdapterPhonebookFavorite != null) {
                bookFavorite = mAdapterPhonebookFavorite.getPhoneBook(position);
            } else {
                bookFavorite = null;
            }

            if (bookFavorite != null) {
                String title = getString(R.string.if_dial);
                if ((bookFavorite.mName != null) && (bookFavorite.mName.length() != 0)) {
                    title += bookFavorite.mName;
                } else {
                    title += bookFavorite.mNumber;
                }
                title += "?";
                Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(ATBluetoothActivity.this, R.style.AlertDialogCustom))

                        .setTitle(title).setPositiveButton(R.string.alert_dialog_ok, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mATBluetooth.write(ATBluetooth.REQUEST_CALL, bookFavorite.mNumber);
                            }
                        }).setNegativeButton(R.string.alert_dialog_cancel, null).show();
            }
        }
    }

    private void updateDialNumText() {

    }

    private OnItemLongClickListener mOnLongClickListCalllogListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {


            int position = arg2;
            // TODO Auto-generated method stub
            if (mUI == R.id.calllog) {
                switch (mCalllog) {
                    case 0:
                        mAdapterCallM.setSelected(position);
                        mCalllogForDial = mCalllogMInfo.get(position);
                        break;
                    case 1:
                        mAdapterCallR.setSelected(position);
                        mCalllogForDial = mCalllogRInfo.get(position);
                        break;
                    case 2:
                        mAdapterCallO.setSelected(position);
                        mCalllogForDial = mCalllogOInfo.get(position);
                        break;
                }
            } else if (mUI == R.id.phonebook) {
                try {
                    MyListAdapterEx la = (MyListAdapterEx) mListViewPhonebook.getAdapter();

                    if (la == mAdapterPhonebookFilter) {
                        return true;
                    }

                    la.setSelected(position);
                    mCalllogForDial = la.getPhoneBookList().get(position);
                } catch (Exception e) {

                }
            }
            showDialogId(R.string.if_del, getString(R.string.if_del) + " " + (((mCalllogForDial.mName != null) && (mCalllogForDial.mName.length() != 0)) ? mCalllogForDial.mName : mCalllogForDial.mNumber) + "?");


            return true;
        }
    };

    private void set3CallDialText(String name, String num) {
        if (m3CallTextName == null) {
            if (name != null && name.length() > 0) {
                num = name + " " + num;
            }
        } else {
            if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(ResourceUtil.mSystemUI)) {
                if (name == null || name.length() == 0) {
                    name = num;
                }
            }
            m3CallTextName.setText(name);
        }

        m3CallTextView.setText(num);
    }


    // favorate
    private View mLayoutFavorate;

    private MotionEvent mMotionEventDelayed = null;
    private final Handler mHandler3Finger = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (mMotionEventDelayed != null) {
                dispatchTouchEvent(mMotionEventDelayed);
            }
        }
    };

    private boolean mFor3Finger = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        if (GlobalDef.mTouch3Switch) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                if (mMotionEventDelayed == null) {
                    mMotionEventDelayed = ev;//ev.copy();
                    mHandler3Finger.removeMessages(0);
                    mHandler3Finger.sendEmptyMessageDelayed(0, 60);
                    return true;
                } else {
                    mMotionEventDelayed = null;
                }
            } else if (ev.getAction() == MotionEvent.ACTION_POINTER_3_DOWN) {
                mHandler.removeMessages(0);
                mFor3Finger = true;
                ev.setAction(MotionEvent.ACTION_UP);
                return super.dispatchTouchEvent(ev);
            } else if (ev.getAction() == MotionEvent.ACTION_UP) {
                mMotionEventDelayed = null;
                mHandler3Finger.removeMessages(0);
                if (mFor3Finger) {
                    mFor3Finger = false;
                    return true;
                }
            }

            if (mFor3Finger || mMotionEventDelayed != null) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }


    //for bug, sometime send cmd to goc_sdk, sdk receive cmd delayed x seconds.
    private void initNameAndPinForGoc() {
        Log.d(TAG, "initNameAndPinForGoc name:" + mATBluetooth.mBtName + "pin:" + mATBluetooth.mBtPin);
        if (mSettingValue[0] == null && mATBluetooth.mBtName != null && mATBluetooth.mBtName.length() > 0) {
            mSettingValue[0] = mATBluetooth.mBtName;
        }

        if (mSettingValue[1] == null && mATBluetooth.mBtPin != null && mATBluetooth.mBtPin.length() > 0) {
            mSettingValue[1] = mATBluetooth.mBtPin;
        }

        if (mSettingValue[1] != null) {
            mAdapter2.notifyDataSetChanged();
            setTextView(R.id.btpin, mSettingValue[1]);
        }
        if (mSettingValue[0] != null) {
            mAdapter2.notifyDataSetChanged();
            updateConnectView();

            setTextView(R.id.btname, mSettingValue[0]);
        }
    }
}
