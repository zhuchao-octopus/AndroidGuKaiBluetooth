package com.zhuchao.android.bt.bt;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.zhuchao.android.bt.hw.ATBluetooth;

public class BtPhoneBookProvider extends ContentProvider {
    private SQLiteDatabase sqlDB;
    private DatabaseHelper dbHelper;
    private static final String DATABASE_NAME = "Users.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE = "_TABLE";
    private static final String TAG = "MyContentProvider";


    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("Create table " + TABLE + "( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, mac TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        sqlDB = dbHelper.getWritableDatabase();
        sqlDB.delete(TABLE, s, as);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        //		sqlDB = dbHelper.getWritableDatabase();
        //		long rowId = sqlDB.insert(TABLE, "", contentvalues);
        //		if (rowId > 0) {
        //			Uri rowUri = ContentUris.appendId(
        //					BTPhoneBook.User.CONTENT_URI.buildUpon(), rowId).build();
        //			getContext().getContentResolver().notifyChange(rowUri, null);
        //			return rowUri;
        //		}
        ////		throw new SQLException("Failed to insert row into " + uri);
        //		Log.e(TAG, "Failed to insert row into " + uri);
        return null;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return (dbHelper == null) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d("abc", "!!!!!!!!!!bt: queryqueryqueryqueryquery");
        Cursor c = null;
        if (ATBluetooth.mCurrentMac != null) {
            String table = SaveData.TAG_PHONE_BOOK + ATBluetooth.mCurrentMac;
            if (SaveData.mDataBase != null) {
                try {
                    c = SaveData.mDataBase.query(table, new String[]{
                            SaveData.KEY_ID, SaveData.KEY_NAME, SaveData.KEY_NUM
                    }, null, null, null, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "query failed: " + e);
                }
            }
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
        sqlDB = dbHelper.getWritableDatabase();
        int n = sqlDB.update(TABLE, contentvalues, null, null);

        return n;
    }

}