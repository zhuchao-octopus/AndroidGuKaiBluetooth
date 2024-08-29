package com.zhuchao.android.bt.bt;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhuchao.android.bt.R;

import com.zhuchao.android.bt.hw.ATBluetooth;

import java.util.ArrayList;

public class AdapterPhoneBookFavorite extends BaseAdapter {
    ArrayList<PhoneBook> mPb = new ArrayList<PhoneBook>();
    ArrayList<Integer> mFavoriteTag = new ArrayList<Integer>();

    private ArrayList<PhoneBook> mCalllogMInfo = ATBluetoothService.mCalllogMInfo;
    private ArrayList<PhoneBook> mCalllogRInfo = ATBluetoothService.mCalllogRInfo;
    private ArrayList<PhoneBook> mCalllogOInfo = ATBluetoothService.mCalllogOInfo;
    private ArrayList<PhoneBook> mPhoneBookInfo = ATBluetoothService.mPhoneBookInfo;
    // private ArrayList<PhoneBook> mPhoneBookSimInfo =
    // ATBluetoothService.mPhoneBookSimInfo;

    private Context mContext;

    private final static int TAG_PHONE_BOOK = 0 << 24;
    private final static int TAG_CALLLOG_M = 10 << 24;
    ;
    private final static int TAG_CALLLOG_R = 20 << 24;
    ;
    private final static int TAG_CALLLOG_O = 30 << 24;
    ;

    public AdapterPhoneBookFavorite(Context context) {
        mContext = context;
        loadFavorite();
    }

    private int getTag(ArrayList<PhoneBook> l) {
        int ret = TAG_PHONE_BOOK;
        if (l == mPhoneBookInfo) {
            ret = TAG_PHONE_BOOK;
        } else if (l == mCalllogMInfo) {
            ret = TAG_CALLLOG_M;
        } else if (l == mCalllogRInfo) {
            ret = TAG_CALLLOG_R;
        } else if (l == mCalllogOInfo) {
            ret = TAG_CALLLOG_O;
        }
        return ret;
    }

    private ArrayList<PhoneBook> getArrayList(int tag) {
        if (tag == TAG_PHONE_BOOK) {
            return mPhoneBookInfo;
        }
        if (tag == TAG_CALLLOG_M) {
            return mCalllogMInfo;
        }
        if (tag == TAG_CALLLOG_R) {
            return mCalllogRInfo;
        }
        if (tag == TAG_CALLLOG_O) {
            return mCalllogOInfo;
        }
        return null;
    }

    private void loadFavorite() {
        int num = getData(SAVE_NUM);
        mPb.clear();
        mFavoriteTag.clear();
        for (int i = 0; i < num; ++i) {
            int tag = getData(SAVE_TAG + i);
            ArrayList<PhoneBook> list = getArrayList(tag & 0xff000000);
            if (list != null) {
                int index = tag & 0xffffff;
                if (index >= 0 && index < list.size()) {
                    PhoneBook pb = list.get(index);
                    mPb.add(pb);
                    mFavoriteTag.add(tag);
                }
            }
        }
    }


    public PhoneBook getPhoneBook(int index) {
        if (index >= 0 && index < mPb.size()) {
            return mPb.get(index);
        }
        return null;
    }

    public void removeAll() {
        mPb.clear();
        mFavoriteTag.clear();
        clearData();
    }

    public void remove(int index) {
        if (index >= 0 && index < mPb.size() && index < mFavoriteTag.size()) {
            mPb.remove(index);
            mFavoriteTag.remove(index);
            saveAll();
        }
    }

    private void saveAll() {
        clearData();

        saveData(SAVE_NUM, mFavoriteTag.size());
        for (int i = 0; i < mFavoriteTag.size(); ++i) {
            int tag = mFavoriteTag.get(i);
            saveData(SAVE_TAG + i, tag);
        }
    }

    public void addToFavorite(ArrayList<PhoneBook> l, int index) {
        addToFavorite(l, index, true);
    }

    public void addToFavorite(ArrayList<PhoneBook> l, int index, boolean save) {
        if (!isExist(l.get(index))) {
            int tag = getTag(l) | index;
            mPb.add(l.get(index));
            mFavoriteTag.add(tag);
            if (save) {
                saveData(SAVE_NUM, mFavoriteTag.size());
                saveData(SAVE_TAG + (mFavoriteTag.size() - 1), tag);
            }

        }
    }


    private boolean isExist(PhoneBook pb) {
        for (int i = 0; i < mFavoriteTag.size(); ++i) {
            PhoneBook p = mPb.get(i);
            if (pb.mName == p.mName && pb.mNumber == p.mNumber) {
                return true;
            }
        }
        return false;
    }

    private final static String SAVE_PATH = "favorite";
    private final static String SAVE_TAG = "tag";
    private final static String SAVE_NUM = "num";

    public void saveData(String s, int v) {
        if (ATBluetooth.mCurrentMac != null) {
            saveDataEx(ATBluetooth.mCurrentMac + SAVE_PATH, s, v);
        }
    }

    private void saveDataEx(String data, String s, int v) {
        SharedPreferences.Editor sharedata = mContext.getSharedPreferences(data, 0).edit();
        sharedata.putInt(s, v);
        sharedata.commit();
    }

    private void clearData() {
        SharedPreferences.Editor sharedata = mContext.getSharedPreferences(ATBluetooth.mCurrentMac + SAVE_PATH, 0).edit();
        sharedata.clear();
        sharedata.commit();
    }


    public int getData(String s) {
        return getDataEx(ATBluetooth.mCurrentMac + SAVE_PATH, s);
    }

    private int getDataEx(String data, String s) {
        SharedPreferences sharedata = mContext.getSharedPreferences(data, 0);
        int ret = sharedata.getInt(s, 0);
        return ret;
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

    private void bindView(View v, int position, ViewGroup parent) {
        ViewHolder vh = (ViewHolder) v.getTag();

        PhoneBook book = null;
        if (mPb != null && position < mPb.size()) {

            book = mPb.get(position);
        }

        if (book != null) {
            String name = book.mName;
            if (name == null || name.length() == 0) {
                name = mContext.getResources().getString(R.string.unknow);
            }

            vh.name.setText(name);
            vh.number.setText(book.mNumber);
            vh.number.setVisibility(View.VISIBLE);
            vh.bt.setVisibility(View.INVISIBLE);
        }

    }

}
