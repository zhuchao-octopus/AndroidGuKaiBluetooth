package com.zhuchao.android.bt.bt;

public class PhoneBook {
    public String mNumber;
    public String mName;
    public String mPinyin;
    public int mIndex;

    public PhoneBook() {
    }

    public PhoneBook(String number, String name, String py) {
        mNumber = number;
        mName = name;
        mPinyin = py;
    }

    public PhoneBook(String number, String name, String py, int index) {
        mNumber = number;
        mName = name;
        mPinyin = py;
        mIndex = index;
    }

    public void setIndex(int i) {
        mIndex = i;
    }


}
