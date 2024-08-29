package com.zhuchao.android.bt.bt;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class ContactsUtils {
    private final static String TAG = "ContactsUtils";
    private Context mContext;
    private static ContactsUtils mContactsUtils = null;

    public static ContactsUtils getInstanse(Context context) {
        if (mContactsUtils == null) {
            mContactsUtils = new ContactsUtils(context);
        }
        return mContactsUtils;
    }

    ContactsUtils(Context context) {
        mContext = context;
    }

    public void getAll(String mac) {
        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();
        if (mac != null) cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"data1", "display_name", "data2"}, "data2=?", new String[]{
                mac
        }, "display_name ASC");
        else cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"data1", "display_name", "data2"}, null, null, "display_name ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String telnum = cursor.getString(cursor.getColumnIndex("data1")).replaceAll("\\s", "");
                String name = cursor.getString(cursor.getColumnIndex("display_name"));
                Log.i(TAG, name + ":" + telnum);
            }
            cursor.close();
        }
    }

    /**
     * add one contact
     */
    public boolean add(String name, String phone, String mac) {
        if (name == null || phone == null || mac == null || name.isEmpty() || phone.isEmpty() || mac.isEmpty()) return false;
        // 向RawContacts.CONTENT_URI空值插入，
        // 先获取Android系统返回的rawContactId  
        // 后面要基于此id插入值  
        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, mac);        //we use custom_ringtone to save mac for delete
        Uri rawContactUri = mContext.getContentResolver().insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        values.clear();

        // add Name
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        // 内容类型  
        values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        // 联系人名字  
        values.put(StructuredName.GIVEN_NAME, name);
        values.put("data7", mac);                        //we use data7 field save mac for delete
        // 向联系人URI添加联系人名字  
        mContext.getContentResolver().insert(Data.CONTENT_URI, values);
        values.clear();

        // add Phone
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        // 联系人的电话号码  
        values.put(Phone.NUMBER, phone);
        // 电话类型  
        values.put(Phone.TYPE, mac/*Phone.TYPE_MOBILE*/);    //we use data2 field save mac for wactalk phonetype
        values.put("data7", mac);                            //we use data7 field save mac for delete
        // 向联系人电话号码URI添加电话号码  
        mContext.getContentResolver().insert(Data.CONTENT_URI, values);
        values.clear();

        // add email
		/*if (email != null && !email.isEmpty()) {
			values.put(Data.RAW_CONTACT_ID, rawContactId);  
	        values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);  
	        // 联系人的Email地址  
	        values.put(Email.DATA, email);  
	        // 电子邮件的类型  
	        values.put(Email.TYPE, Email.TYPE_WORK);  
	        // 向联系人Email URI添加Email数据  
	        mContext.getContentResolver().insert(Data.CONTENT_URI, values); 
		}*/

        //        Log.d(TAG, "add ok");

        return true;
    }

    /**
     * clear all contacts
     */
    public void clear(String mac) {
        // Uri.parse("content://com.android.contacts/raw_contacts");
        if (mac == null || mac.isEmpty()) return;
        try {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.delete(RawContacts.CONTENT_URI, RawContacts.CUSTOM_RINGTONE + "=?", new String[]{mac});
            resolver.delete(Data.CONTENT_URI, "data7=?", new String[]{mac});
        } catch (Exception ignored) {
        }
    }

    public void notifyWacTalk(String mac) {
        if (mac == null || mac.isEmpty()) return;
        try {
            Intent intent = new Intent("com.tw.intent.action.SYNC_CONTACTS");
            intent.putExtra("phonetype", mac);
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
        }
    }
}