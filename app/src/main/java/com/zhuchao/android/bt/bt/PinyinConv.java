package com.zhuchao.android.bt.bt;

/**
 * @date 2010-1-22
 */

public class PinyinConv {

    private static int BEGIN = 45217;

    private static int END = 63486;

    private static char[] chartable = {
            0x554a, 0x82ad, 0x64e6, 0x642d, 0x86fe, 0x53d1, 0x5676, 0x54c8, 0x54c8, 0x51fb, 0x5580, 0x5783, 0x5988, 0x62ff, 0x54e6, 0x556a, 0x671f, 0x7136, 0x6492, 0x584c, 0x584c, 0x584c, 0x6316,
            0x6614, 0x538b, 0x531d,
    };

    private static int[] table = new int[27];

    private static char[] initialtable = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g',

            'h', 'h', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',

            't', 't', 'w', 'x', 'y', 'z',
    };

    static {

        for (int i = 0; i < 26; i++) {

            table[i] = gbValue(chartable[i]);

        }

        table[26] = END;

    }

    public static String cn2py(String SourceStr) {

        if (SourceStr == null) {
            return null;
        }
        String Result = "";

        int StrLength = SourceStr.length();

        int i;


        try {

            //jump 空格
            for (i = 0; i < StrLength; i++) {

                if (SourceStr.charAt(i) != ' ') {
                    break;
                }


            }

            for (; i < StrLength; i++) {

                Result += Char2Initial(SourceStr.charAt(i));

            }

        } catch (Exception e) {

            Result = "";

        }

        return Result;

    }

    public static char cn2pyHead(String SourceStr) {

        char Result = 'a';
        if (SourceStr != null && SourceStr.length() > 0) {
            try {
                Result = Char2Initial(SourceStr.charAt(0));
            } catch (Exception e) {
                Result = 'a';
            }
        } else {
            Result = 'a';
        }
        return Result;

    }

    private static char Char2Initial(char ch) {

        if (ch >= 'a' && ch <= 'z')

            return (char) (ch - 'a' + 'A');

        if (ch >= 'A' && ch <= 'Z')

            return ch;

        int gb = gbValue(ch);

        if ((gb < BEGIN) || (gb > END))

            return ch;

        int i;

        for (i = 0; i < 26; i++) {

            if ((gb >= table[i]) && (gb < table[i + 1]))

                break;

        }

        if (gb == END) {

            i = 25;

        }
        char initial = initialtable[i];
        if (i == 25) {
            switch (gb) {
                case 0xe6c3:
                    initial = 't';
                    break;
            }
        }
        return (char) (initial - 'a' + 'A');

    }

    private static int gbValue(char ch) {

        String str = new String();

        str += ch;

        try {

            byte[] bytes = str.getBytes("GB2312");

            if (bytes.length < 2)

                return 0;

            return (bytes[0] << 8 & 0xff00) + (bytes[1] & 0xff);

        } catch (Exception e) {

            return 0;

        }

    }

}
