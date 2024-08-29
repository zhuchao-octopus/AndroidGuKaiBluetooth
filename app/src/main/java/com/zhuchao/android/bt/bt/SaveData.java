package com.zhuchao.android.bt.bt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;

/**
 * @date 2010-1-22
 */

public class SaveData {
    private final static String TAG = "SaveData";
    private static Handler mHandlerForSavePhoneBook = new Handler();

    private static ArrayList<PhoneBook> mSaveList = null;
    private static String mMacForSave = null;
    private static int mSaveingPhonebook = -1;
    public final static String TAG_PHONE_BOOK = "a";
    public final static String TAG_PHONE_BOOK_SIM = "s";
    public final static String TAG_LOG_OUT = "b";
    public final static String TAG_LOG_MISS = "c";
    public final static String TAG_LOG_RECEIVE = "d";


    public final static String TAG_AUTO_DOWN = "t";

    public static String mCurExistPhoneTagChar = "";
    public static char mPrePy = 0;
    // private static Context mContext;
    //
    // private static void init(Context c) {
    // mContext = c;
    // }

    public static void clearCalllogData(String mac) {

        clearPhoneBookData(mac, TAG_LOG_OUT);
        clearPhoneBookData(mac, TAG_LOG_MISS);
        clearPhoneBookData(mac, TAG_LOG_RECEIVE);
    }

    public static void clearPhoneBookData(String mac) {
        clearPhoneBookData(mac, TAG_PHONE_BOOK);
        setFirstConnectNeedDown(mac, false);
    }

    public static void clearPhoneBookDataSim(String mac) {
        clearPhoneBookData(mac, TAG_PHONE_BOOK_SIM);
    }

    public static void clearPhoneBookData(String mac, String tag) {
        try {
            mDataBase.delete(tag + mac, null, null);
        } catch (Exception e) {
            if (e != null) Log.e(TAG, e.getMessage());
        }
    }

    public static void savePhoneBookData(ArrayList<PhoneBook> saveList, String mac) {
        savePhoneBookDataEx(saveList, mac, TAG_PHONE_BOOK);
    }

    private static void savePhoneBookDataEx(ArrayList<PhoneBook> saveList, String mac, String tag) {
        Log.e(TAG, "" + mac);
        mac = tag + mac;
        if ((saveList != null) && (saveList.size() > 0) && (mac != null)) {
            if (mSaveingPhonebook == 2) {
                mSaveingPhonebook = 0;
                return;
            }
            mSaveList = saveList;
            mMacForSave = mac;
            mSaveingPhonebook = 1;
            mHandlerForSavePhoneBook.postDelayed(new Runnable() {
                public void run() {
                    int i = 0;
                    createTable(mMacForSave);
                    for (PhoneBook pb : mSaveList) {

                        if (i == 0) {
                            mCurExistPhoneTagChar = "";
                        }

                        if (mPrePy != pb.mPinyin.charAt(0)) {
                            mCurExistPhoneTagChar += pb.mPinyin.charAt(0);
                        }
                        mPrePy = pb.mPinyin.charAt(0);
                        //						Log.d("cccd", i+":"+mCurExistPhoneTagChar);

                        if (mSaveingPhonebook == 2) {
                            break;
                        }
                        saveData(mMacForSave, pb.mName, pb.mNumber);
                        // saveData(mMacForSave, " pb.mNumber);
                        ++i;

                        Log.d("dd", "savePhoneBookDataEx" + i);
                        // Log.e("savePhoneBookData", pb.mName + ":" +
                        // pb.mNumber);
                    }
                    mSaveingPhonebook = 0;
                }
            }, 200);
        }
    }

    public static boolean sortPhoneBook(ArrayList<PhoneBook> pb, Handler h, AsyncTask as) {

        try {
            PhoneBook pb1, pb2;

            int size = pb.size();

            int pre = 0;

            for (int i = 1; i < size; ++i) {
                if (as.isCancelled()) {
                    Log.d("allen", "AsyncTask cancel");
                    return false;
                }
                pb1 = pb.get(i);
                char a = PinyinConv.cn2pyHead(pb1.mName);

                int p = (i * 100 / size);
                if (h != null && p > pre) {
                    h.sendEmptyMessage(p);
                    pre = p;
                }
                for (int j = i - 1; j >= 0; --j) {
                    if (as.isCancelled()) {

                        Log.d("allen", "AsyncTask cancel");
                        return false;
                    }
                    pb2 = pb.get(j);
                    char b = PinyinConv.cn2pyHead(pb2.mName);

                    //					if (isHebrewNum(a) && !isHebrewNum(b)) {
                    //						pb.set(j, pb1);
                    //						pb.set(j + 1, pb2);
                    //					} else {
                    if (a < b) {
                        pb.set(j, pb1);
                        pb.set(j + 1, pb2);

                        // Util.doSleep(10);
                    } else {
                        // Util.doSleep(5);

                        break;
                    }
                    //					}
                }

            }
        } catch (Exception e) {

            Log.d(TAG, "sortPhoneBook err:" + e);


        }
        return true;
    }

    private static boolean isHebrewNum(char c) {
        if (c >= 1488 && c <= 1514) {
            return true;
        }
        return false;
    }

    public static int getPhoneBookData(ArrayList<PhoneBook> saveList, String mac) {
        return getPhoneBookDataEx(saveList, mac, TAG_PHONE_BOOK);
    }

    public static int getPhoneBookSimData(ArrayList<PhoneBook> saveList, String mac) {
        return getPhoneBookDataEx(saveList, mac, TAG_PHONE_BOOK_SIM);
    }

    public static int getPhoneBookDataEx(ArrayList<PhoneBook> saveList, String mac, String tag) {
        mac = tag + mac;
        boolean calllog = false;
        if (tag.equals(TAG_LOG_MISS) || tag.equals(TAG_LOG_RECEIVE) || tag.equals(TAG_LOG_OUT)) {
            calllog = true;
        }
        if (saveList != null && mac != null) {
            saveList.clear();
            int id;
            String name;
            String number;
            Cursor c = queryAllData(mac);
            char cPy = 0;
            if (c != null) {
                c.moveToFirst();
                for (int i = 0; i < c.getCount(); ++i) {
                    name = c.getString(c.getColumnIndex(KEY_NAME));
                    number = c.getString(c.getColumnIndex(KEY_NUM));
                    id = c.getInt(c.getColumnIndex(KEY_ID));
                    c.moveToNext();
                    if (!calllog) {
                        String py = PinyinConv.cn2py(name);
                        if (i == 0) {
                            mCurExistPhoneTagChar = "";
                        }

                        //						Log.d("ccc", py+":"+i);
                        if (py != null && py.length() > 0) {
                            cPy = py.charAt(0);
                            if (mPrePy != cPy) {
                                mCurExistPhoneTagChar += cPy;
                            }
                            mPrePy = cPy;
                        }


                        saveList.add((new PhoneBook(number, name, py, id)));
                    } else {
                        String[] nn = number.split(",");
                        number = nn[0];
                        String time = null;
                        if (nn.length > 1) {
                            time = nn[1];
                        }
                        saveList.add(0, (new PhoneBook(number, name, time, id)));
                    }
                }
            }
            //			sortPhoneBook(saveList);
            return saveList.size();
        }
        return 0;
    }

    public static void saveData(String path, String s, String v) {

        insert(path, s, v);
    }

    public static void saveData(String path, String s, String v, int id) {

        insert(path, s, v, id);
    }

    public static long saveDataEx(String mac, String num, String tag) {
        String table = tag + mac;
        String DB_CREATE2 = "CREATE TABLE " + table + " (" + KEY_ID + " integer , " + KEY_NAME + " text not null, " + KEY_NUM + " text not null);";

        try {
            mDataBase.execSQL(DB_CREATE2);
        } catch (Exception e) {
            if (e != null) Log.e(TAG, e.getMessage());
        }

        ContentValues newValues = new ContentValues();

        newValues.put(KEY_NAME, "");
        newValues.put(KEY_NUM, num);

        // mDataBase.insert(table, null, newValues);

        long nn = 0;
        nn = mDataBase.insert(table, null, newValues);

        return nn;

    }

    public static long saveDataEx(String mac, String name, String num, String tag) {
        if (num == null) {
            return 0;
        }
        if (name == null) {
            name = "";
        }
        String table = tag + mac;
        String DB_CREATE2 = "CREATE TABLE " + table + " (" + KEY_ID + " integer , " + KEY_NAME + " text not null, " + KEY_NUM + " text not null);";

        try {
            mDataBase.execSQL(DB_CREATE2);
        } catch (Exception e) {
            if (e != null) Log.e(TAG, e.getMessage());
        }

        ContentValues newValues = new ContentValues();

        newValues.put(KEY_NAME, name);
        newValues.put(KEY_NUM, num);

        // mDataBase.insert(table, null, newValues);

        long nn = 0;
        nn = mDataBase.insert(table, null, newValues);

        return nn;

    }

    public static SQLiteDatabase mDataBase;

    public static void createDataBase(Context c) {
        mDataBase = c.openOrCreateDatabase("DateBase1.db", Context.MODE_PRIVATE, null);
    }

    public static void createTable(String table) {
        try {
            mDataBase.delete(table, null, null);
        } catch (Exception e) {
            if (e != null) Log.e(TAG, e.getMessage());
        }

        String DB_CREATE2 = "CREATE TABLE " + table + " (" + KEY_ID + " integer, " + KEY_NAME + " text not null, " + KEY_NUM + " text not null);";

        try {
            mDataBase.execSQL(DB_CREATE2);
        } catch (Exception e) {
            if (e != null) Log.e(TAG, e.getMessage());
        }
    }

    public static long insert(String p, String n, String a) {
        ContentValues newValues = new ContentValues();

        if (n == null) {
            n = "";

        }
        if (a == null) {
            a = "";
        }
        newValues.put(KEY_NAME, n);
        newValues.put(KEY_NUM, a);

        // long nn = 0;
        return mDataBase.insert(p, null, newValues);
        // Log.e("!!", "num:" + a + ":" + nn);
        // n = mDataBase.insert(DB_TABLE, null, newValues);
        // return nn;
    }

    public static long insert(String p, String n, String a, int id) {
        ContentValues newValues = new ContentValues();

        if (n == null) {
            n = "";

        }
        if (a == null) {
            a = "";
        }
        newValues.put(KEY_ID, id);
        newValues.put(KEY_NAME, n);
        newValues.put(KEY_NUM, a);

        // long nn = 0;
        return mDataBase.insert(p, null, newValues);
        // Log.e("!!", "num:" + a + ":" + nn);
        // n = mDataBase.insert(DB_TABLE, null, newValues);
        // return nn;
    }

    public static void setFirstConnectNeedDown(String mac, boolean b) {

        clearPhoneBookData(mac, TAG_AUTO_DOWN);
        saveDataEx(mac, b ? "1" : "0", SaveData.TAG_AUTO_DOWN);
    }

    public static boolean isFirstConnectNeedDown(String mac) {

        mac = SaveData.TAG_AUTO_DOWN + mac;
        Cursor c = queryAllData(mac);

        if (c != null) {
            c.moveToFirst();
            String num = c.getString(c.getColumnIndex(KEY_NUM));
            if ("1".equals(num)) {
                return false;
            }
        }
        return true;
    }

    private static Cursor queryAllData(String table) {
        Cursor result = null;
        try {
            result = mDataBase.query(table, new String[]{KEY_ID, KEY_NAME, KEY_NUM}, null, null, null, null, null);
            if (result != null) {
                result.moveToFirst();
                // Log.e("count!!", "ffffffff"+result.getCount());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return result;
    }

    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "n";
    public static final String KEY_NUM = "m";

    // public static final String KEY_HEIGHT = "height";
    // find name

    public static String findName(ArrayList<PhoneBook> phonebook, String num) {

        if (phonebook != null && num != null) {
            // just for china

            for (PhoneBook pb : phonebook) {
                String num1 = pb.mNumber;
                if (num1.startsWith("+86")) {
                    num1 = num1.substring(3, num1.length() - 3);
                }

                if (num.startsWith("+86")) {
                    num = num.substring(3, num.length() - 3);
                }

                if (num1.equals(num)) {
                    return pb.mName;
                }
            }

        }
        return null;
    }

    public static String findName(ArrayList<PhoneBook> phonebook, ArrayList<PhoneBook> phonesimbook, String num) {
        String name = findName(phonebook, num);
        if (name == null) {
            name = findName(phonesimbook, num);
        }
        return name;
    }
}
