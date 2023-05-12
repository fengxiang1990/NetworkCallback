package com.fxa.networkcallback;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.view.OrientationEventListener;

public class OrientationDetector extends OrientationEventListener {

    /**
     * Activity
     */
    private Activity mActivity;
    /**
     * 记录用户手机屏幕的位置
     */
    private int mOrientation = -1;

    /**
     * 构造方法
     *
     * @param activity 当前Activity
     */
    public OrientationDetector(Activity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        //记录用户手机上一次放置的位置
        int mLastOrientation = mOrientation;

        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            //手机平放时，检测不到有效的角度，重置为原始位置 -1
            mOrientation = -1;
            return;
        }
        //只检测是否有四个角度的改变
        if (orientation > 350 || orientation < 10) {
            //0度，竖直
            mOrientation = 0;
        } else if (orientation > 80 && orientation < 100) {
            //90度，右侧横屏
            mOrientation = 90;
        } else if (orientation > 170 && orientation < 190) {
            //180度，反向竖直
            mOrientation = 180;
        } else if (orientation > 260 && orientation < 280) {
            //270度，左侧横屏
            mOrientation=270;
        }
        //如果用户关闭了手机的屏幕旋转功能，不再开启代码自动旋转了，直接return
        try {
            //1 手机已开启屏幕旋转功能 0 手机未开启屏幕旋转功能
            if (Settings.System.getInt(mActivity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 0) {
                return;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        //当检测到用户手机位置距离上一次记录的手机位置发生了改变，开启屏幕自动旋转
        if (mLastOrientation != mOrientation) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    /**
     * 设置横竖屏
     * @param orientation 方向
     */
    public void setRequestedOrientation(int orientation){
        mActivity.setRequestedOrientation(orientation);
    }
}
