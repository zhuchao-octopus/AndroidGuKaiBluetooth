package com.zhuchao.android.bt.view;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.Scroller;

import java.lang.reflect.Method;

public class SplitHScrollView extends FrameLayout {
    private static final String LOGTAG = "SplitHScrollView";
    private float lastX;
    private float lastY;
    //	private VelocityTracker mVelocityTracker;
    private Scroller mScroller = null;
    public int LEFT_LIMIT = 0;
    public int TOP_LIMIT = 0;
    public int WIDTH = 1024;
    public int WIDTH2 = WIDTH / 2;
    public int HEIGHT = 600;
    private Context mContext;

    private boolean mDisallowIntercept = false;
    private boolean mIsSplitScreen = false;
    private boolean mIsBeingDragged = false;

    private void initScroll(Context context) {
        mContext = context;
        if (mScroller == null) {
            mScroller = new Scroller(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setKeepScreenOn(true);

			/*WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		    if (windowManager != null) {
		        DisplayMetrics dm = new DisplayMetrics();
		        windowManager.getDefaultDisplay().getRealMetrics(dm);
		        WIDTH = dm.widthPixels;
		        HEIGHT = dm.heightPixels;
		        WIDTH2 = WIDTH / 2;
		        Log.d(LOGTAG, "initScroll " + WIDTH + "," + HEIGHT);
		    }*/

            DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display[] display = displayManager.getDisplays();

            //DisplayInfo outDisplayInfo = new DisplayInfo();
            //display[0].getDisplayInfo(outDisplayInfo);
            //WIDTH = outDisplayInfo.appWidth;
            //HEIGHT = outDisplayInfo.appHeight;
            WIDTH2 = WIDTH / 2;

            Log.d(LOGTAG, "initScroll " + WIDTH + "," + HEIGHT);
        }
    }


    public SplitHScrollView(Context context) {
        super(context);
        initScroll(context);
    }

    public SplitHScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SplitHScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SplitHScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initScroll(context);
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
    }

    private final int mTouchSlop = 16;
    private int mLastMotionX = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mIsSplitScreen || mDisallowIntercept) return false;

        final int action = ev.getActionMasked();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true; //don't dispatch ev to child view
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                final int x = (int) ev.getX();
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                if (xDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionX = x;
                } else {
                    mIsBeingDragged = false;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                setLastPosition(ev.getX(), ev.getY());
                mIsBeingDragged = false;
                mLastMotionX = (int) ev.getX();
                // Log.d(LOGTAG, "ACTION_DOWNP2 " + mLastMotionX);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Log.d(LOGTAG, "ACTION_UP2 " + mLastMotionX);
                mIsBeingDragged = false;
                break;
        }
        // Log.d(LOGTAG, "mIsBeingDragged " + mIsBeingDragged);
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //		super.onTouchEvent(ev);
        if (!mIsSplitScreen || mDisallowIntercept) return false;
        //		if (mVelocityTracker == null) {
        //			mVelocityTracker = VelocityTracker.obtain();
        //		}
        //		mVelocityTracker.addMovement(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopScroller();
                setLastPosition(ev.getX(), ev.getY()); //maybe havn't ACTION_DOWN, get from onInterceptTouchEvent
                //			Log.d(LOGTAG, "ACTION_DOWN " + lastX);
                break;
            case MotionEvent.ACTION_MOVE:
                int scroll_x = getScrollX();
                int move_x = (int) (lastX - ev.getX());
                //			Log.d(LOGTAG, "ACTION_MOVE: scrollx=" + scroll_x + ", movex=" + move_x + ", lastX=" + lastX + ", curx=" + ev.getX());
                if (move_x < 0) {
                    if (scroll_x > 0) {    //disable scroll exceed leftlimit
                        if (-move_x > scroll_x) scrollBy(-scroll_x, 0);
                        else scrollBy(move_x, 0);
                    }
                } else {
                    scrollBy(move_x, 0);
                }
                setLastPosition(ev.getX(), ev.getY());
                break;
            case MotionEvent.ACTION_UP:
                //			mVelocityTracker.computeCurrentVelocity(1000);
                //			if (mScroller != null) {
                //				mScroller.fling(getScrollX(), 0/*getScrollY()*/,
                //						(int) -mVelocityTracker.getXVelocity(),
                //						0/*(int) -mVelocityTracker.getYVelocity()*/,
                //						LEFT_LIMIT,	WIDTH / 2, 0/*TOP_LIMIT*/, 0/*BOTTOM_LIMIT*/);
                //			}
                //			mVelocityTracker.recycle();
                //			mVelocityTracker = null;
                break;
        }
        return true;
    }

    private void setLastPosition(float x, float y) {
        lastX = x;
        lastY = y;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        //		Log.d(LOGTAG, "scrollTo1 l=" + l + " ,t=" + t + "oldl" + oldl + " ,oldt=" + oldt);
    }

    @Override
    public void scrollTo(int x, int y) {
        //		Log.d(LOGTAG, "scrollTo1 x=" + x + " ,y=" + y);
        x = Math.min(Math.max(x, LEFT_LIMIT), WIDTH2);
        y = Math.min(Math.max(y, TOP_LIMIT), HEIGHT);
        //		Log.d(LOGTAG, "scrollTo2 x=" + x + " ,y=" + y);
        super.scrollTo(x, y);
    }

    @Override
    public void computeScroll() {
        if (mScroller != null && mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
        }
    }

    private void stopScroller() {
        if (mScroller != null && !mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //		Log.d(LOGTAG, "onSizeChanged " + w + "," + h + "," + oldw + "," + oldh);
        super.onSizeChanged(w, h, oldw, oldh);
        HEIGHT = h;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        HEIGHT = bottom;
        //		Log.d(LOGTAG, "onLayout " + changed + "," + left + "," + top + "," + right + "," + bottom);
        if (changed) {
            mIsSplitScreen = isInMultiWindowMode();
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    boolean isInMultiWindowMode() {
        boolean ret = false;
        try {
            Class<?> clazz_activity = Class.forName("android.app.Activity");
            Method method_isInMultiWindowMode = clazz_activity.getMethod("isInMultiWindowMode");
            if (method_isInMultiWindowMode != null) {
                ret = (Boolean) method_isInMultiWindowMode.invoke(getContext());
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.toString());
        }
        //		Log.d(LOGTAG, "isInMultiWindowMode " + ret);

        return ret;
    }
}
