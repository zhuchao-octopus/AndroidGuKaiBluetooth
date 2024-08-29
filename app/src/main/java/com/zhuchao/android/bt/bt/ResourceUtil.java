package com.zhuchao.android.bt.bt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.common.util.MachineConfig;
import com.common.util.SystemConfig;

public class ResourceUtil {

    public static String mSystemUI = null;
    public static int mScreenWidth = 0;
    public static int mScreenHeight = 0;

    @SuppressLint("DiscouragedApi")
    public static int getLayoutId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "layout", paramContext.getPackageName());
    }

    @SuppressLint("DiscouragedApi")
    public static int getStringId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "string", paramContext.getPackageName());
    }

    @SuppressLint("DiscouragedApi")
    public static int getDrawableId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "drawable", paramContext.getPackageName());
    }

    @SuppressLint("DiscouragedApi")
    public static int getStyleId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "style", paramContext.getPackageName());
    }

    @SuppressLint("DiscouragedApi")
    public static int getId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "id", paramContext.getPackageName());
    }

    @SuppressLint("DiscouragedApi")
    public static int getColorId(Context paramContext, String paramString) {
        return paramContext.getResources().getIdentifier(paramString, "color", paramContext.getPackageName());
    }

    public static String updateAppUi(Context context) { // app used except
        // launcher

        String value = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);


        if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(value) || MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(value)) {

            String s = SystemConfig.getProperty(context, SystemConfig.KEY_LAUNCHER_UI_RM10);
            if (s != null) {
                if ("1".equals(s)) {
                    value = MachineConfig.VALUE_SYSTEM_UI21_RM10_2;
                } else { //0
                    value = MachineConfig.VALUE_SYSTEM_UI20_RM10_1;
                }

            }

        }
        //		else if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(value)) {
        //
        //			String s = SystemConfig.getProperty(context,
        //					SystemConfig.KEY_LAUNCHER_UI_RM10);
        //			if(s!=null){
        //				if ("1".equals(s)){
        //					value = MachineConfig.VALUE_SYSTEM_UI21_RM10_2;
        //				} else { //0
        //					value = MachineConfig.VALUE_SYSTEM_UI21_RM12;
        //				}
        //			}
        //		}
        //		value = MachineConfig.VALUE_SYSTEM_UI44_KLD007;//

        mSystemUI = value;
        // if (value != null) {
        int sw = 0;
        int w = 0;
        int h = 0;
        int type = 0; // deault 800X480

        //		DisplayMetrics dm = context.getResources().getDisplayMetrics();
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays();

        ///DisplayInfo outDisplayInfo = new DisplayInfo();
        ///display[0].getDisplayInfo(outDisplayInfo);
        //		Rect outRect = new Rect();
        //		display[0].getOverscanInsets(outRect);
        ///mScreenWidth = outDisplayInfo.appWidth;
        ///mScreenHeight = outDisplayInfo.appHeight;
        //		if (dm.widthPixels == 1024 && dm.heightPixels == 600) {
        //			type = 1;
        //			sw = 321;
        //		} else

        boolean multiWindow = false;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm.widthPixels != mScreenWidth) {
            multiWindow = true;
        } else {
            multiWindow = false;
        }


        if (mScreenWidth == 1280 && mScreenHeight == 480) {//1280X480?
            type = 2;
            sw = 322;
        } else if (mScreenWidth < 1024 && mScreenHeight == 480) {//1280X480?
            type = 0;
            sw = 320;
        } else if (mScreenWidth == 1280 && (mScreenHeight >= 720 && mScreenHeight <= 800)) {
            type = 3;
            sw = 323;
        } else if (mScreenWidth == 1920 && mScreenHeight == 1080) {//1280X480?
            type = 4;
            sw = 324;
        } else {
            //			if (outRect.left != 0 /*&& outRect.right == 20 && outRect.top == 60
            //					&& outRect.bottom == 60*/) { // for mini
            //				type = 0;
            //				sw = 320;
            //				if (context instanceof Activity) {
            //					Activity new_name = (Activity) context;
            //					new_name.setTheme(R.style.TranslucentTheme2);
            //				}
            //				w = 600;
            //
            //			} else {
            type = 1;
            sw = 321;
            //			}
        }


        if (MachineConfig.VALUE_SYSTEM_UI_KLD12_80.equals(value) || MachineConfig.VALUE_SYSTEM_UI19_KLD1.equals(value) || MachineConfig.VALUE_SYSTEM_UI28_7451.equals(value) || MachineConfig.VALUE_SYSTEM_UI34_KLD9.equals(value) || MachineConfig.VALUE_SYSTEM_UI35_KLD813_2.equals(value)) {
            if (type == 0) {
                sw = 326;
            } else if (type == 3) {
                sw = 329;
            } else if (type == 2) {
                sw = 328;
            } else if (type == 4) {
                sw = 330;
            } else {
                sw = 327;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD3_8702.equals(value) || MachineConfig.VALUE_SYSTEM_UI_KLD15_6413.equals(value) || MachineConfig.VALUE_SYSTEM_UI16_7099.equals(value) || MachineConfig.VALUE_SYSTEM_UI32_KLD8.equals(value) || MachineConfig.VALUE_SYSTEM_UI36_664.equals(value) || MachineConfig.VALUE_SYSTEM_UI37_KLD10.equals(value)) {
            if (type == 0) {
                sw = 332;
            } else if (type == 3) {
                sw = 335;
            } else if (type == 2) {
                sw = 334;
            } else {
                sw = 333;
            }
        } else if (MachineConfig.VALUE_SYSTEM_UI_KLD10_887.equals(value)) {
            sw = 320 + type;
            //			if (type == 0) {
            //				sw = 320;
            //			} else if (type == 2) {
            //				sw = 322;
            //			} else {
            //				sw = 321;
            //			}
        } else if (MachineConfig.VALUE_SYSTEM_UI20_RM10_1.equals(value)) {
            sw = 340 + type;
            //			if (type == 0) {
            //				sw = 340;
            //			} else if (type == 1) {
            //				sw = 341;
            //			} else if (type == 2) {
            //				sw = 342;
            //			} else {
            //				sw = 341;
            //			}
        } else if (MachineConfig.VALUE_SYSTEM_UI21_RM10_2.equals(value)) {
            sw = 350 + type;

            //			if (type == 0) {
            //				sw = 350;
            //			} else if (type == 1) {
            //				sw = 351;
            //			} else if (type == 2) {
            //				sw = 352;
            //			} else {
            //				sw = 351;
            //			}
        } else if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(value) || MachineConfig.VALUE_SYSTEM_UI31_KLD7.equals(value) || MachineConfig.VALUE_SYSTEM_UI_PX30_1.equals(value)) {
            sw = 360 + type;
        }
        //wrong 813 no use
        else if (MachineConfig.VALUE_SYSTEM_UI35_KLD813.equals(value)) {
            sw = 370 + type;
        } else if (MachineConfig.VALUE_SYSTEM_UI21_RM12.equals(value)) {
            sw = 380 + type;
            //			if (type == 0) {
            //				sw = 380;
            //			} else if (type == 1) {
            //				sw = 381;
            //			}
        } else if (MachineConfig.VALUE_SYSTEM_UI45_8702_2.equals(value)) {
            sw = 390 + type;
            //			if (type == 0) {
            //				sw = 390;
            //			}  else if (type == 3) {
            //				sw = 393;
            //			} else if (type == 2) {
            //				sw = 392;
            //			}else {
            //				sw = 391;
            //			}
        } else if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(value)) {
            sw = 400 + type;
        } else if (MachineConfig.VALUE_SYSTEM_UI43_3300_1.equals(value)) {
            //from VALUE_SYSTEM_UI_KLD3_8702 332
            sw = 410 + type;
            if (multiWindow) {
                w = 1080;
            }
        }


        Configuration c = context.getResources().getConfiguration();
        Log.d("ddd", c + ":sw:" + sw + ":" + w);
        if (sw != 0) {
            c.smallestScreenWidthDp = sw;
        }
        if (w != 0) {
            c.screenWidthDp = w;
        }
        if (h != 0) {
            c.screenHeightDp = h;
        }
        context.getResources().updateConfiguration(c, null);

        // }


        return value;
    }

}
