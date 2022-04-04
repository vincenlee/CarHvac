/*
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.hvac;

import android.app.Service;
import android.car.Car;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.android.car.hvac.controllers.HvacPanelController;
import com.android.car.hvac.ui.TemperatureBarOverlay;

import java.util.ArrayList;
import java.util.List;


/**
 * 为HVAC控件创建滑动面板，并将其添加到SystemUI上方的窗口管理器中。
 */
public class HvacUiService extends Service {
    public static final String CAR_INTENT_ACTION_TOGGLE_HVAC_CONTROLS =
            "android.car.intent.action.TOGGLE_HVAC_CONTROLS";
    private static final String TAG = "HvacUiService";

    private final List<View> mAddedViews = new ArrayList<>();

    private WindowManager mWindowManager;

    private View mContainer;

    private int mNavBarHeight;
    private int mPanelCollapsedHeight;
    private int mPanelFullExpandedHeight;
    private int mScreenBottom;
    private int mScreenWidth;
    // 这是为了补偿y坐标原点与屏幕实际底部之间的差异。
    private int mInitialYOffset = 0;
    private DisplayMetrics mDisplayMetrics;

    private int mTemperatureSideMargin;
    private int mTemperatureOverlayWidth;
    private int mTemperatureOverlayHeight;

    private HvacPanelController mHvacPanelController;
    private HvacController mHvacController;

    // 我们需要一个扩展的和折叠的版本，因为在调整窗口大小的过程中出现了一个渲染错误，因此我们需要在折叠的窗口和扩展的窗口之间进行切换。
    private TemperatureBarOverlay mDriverTemperatureBar;
    private TemperatureBarOverlay mPassengerTemperatureBar;
    private TemperatureBarOverlay mDriverTemperatureBarCollapsed;
    private TemperatureBarOverlay mPassengerTemperatureBarCollapsed;


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void onCreate() {
        Resources res = getResources();
        boolean showCollapsed = res.getBoolean(R.bool.config_showCollapsedBars);
        mPanelCollapsedHeight = res.getDimensionPixelSize(R.dimen.car_hvac_panel_collapsed_height);
        mPanelFullExpandedHeight = res.getDimensionPixelSize(R.dimen.car_hvac_panel_full_expanded_height);

        mTemperatureSideMargin = res.getDimensionPixelSize(R.dimen.temperature_side_margin);
        mTemperatureOverlayWidth = res.getDimensionPixelSize(R.dimen.temperature_bar_width_expanded);
        mTemperatureOverlayHeight = res.getDimensionPixelSize(R.dimen.car_hvac_panel_full_expanded_height);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mDisplayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(mDisplayMetrics);
        mScreenBottom = mDisplayMetrics.heightPixels;
        mScreenWidth = mDisplayMetrics.widthPixels;

        int identifier = res.getIdentifier("navigation_bar_height_car_mode", "dimen", "android");
        mNavBarHeight = (identifier > 0 && showCollapsed) ? res.getDimensionPixelSize(identifier) : 0;

        WindowManager.LayoutParams testparams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        // 不存在从服务内部获取系统ui可见性的当前状态的方法，因此我们将全屏放置一些东西，并检查其最终测量结果，作为获取该信息的黑客手段。
        // 一旦我们有了初始状态，我们就可以安全地从那时开始注册更改事件。
        View windowSizeTest = new View(this) {
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                Log.i(TAG, "onLayout: changed：" + changed + ";left:" + left + ";top:" + top + ";right:" + right + ";bottom" + bottom);
                boolean sysUIShowing = (mDisplayMetrics.heightPixels != bottom);
                mInitialYOffset = (sysUIShowing) ? -mNavBarHeight : 0;
                Log.i(TAG, "onLayout: mNavBarHeight:" + mNavBarHeight);
                Log.i(TAG, "onLayout: sysUIShowing:" + sysUIShowing + ";mInitialYOffset" + mInitialYOffset);
                layoutHvacUi();
                // 我们现在有了初始状态，因此不再需要这个空视图。
                mWindowManager.removeView(this);
                mAddedViews.remove(this);
            }
        };

        addViewToWindowManagerAndTrack(windowSizeTest, testparams);

        // 接收事件的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(CAR_INTENT_ACTION_TOGGLE_HVAC_CONTROLS);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        // 注册接收器，以便任何具有CONTROL_CAR_CLIMATE权限的用户都可以调用它。
        registerReceiverAsUser(mBroadcastReceiver, UserHandle.ALL, filter,
                Car.PERMISSION_CONTROL_CAR_CLIMATE, null);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: " + action);
            if (action.equals(CAR_INTENT_ACTION_TOGGLE_HVAC_CONTROLS)) {
                mHvacPanelController.toggleHvacUi();
            } else if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                mHvacPanelController.collapseHvacUi();
            }
        }
    };


    /**
     * 在确定最小偏移量后调用。
     * 这将生成HVAC UI所需的所有组件的布局。
     * 启动时，折叠视图所需的所有窗口都可见，而展开视图的窗口已创建并调整大小，但不可见。
     */
    private void layoutHvacUi() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        & ~WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.packageName = this.getPackageName();
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.x = 0;
        params.y = mInitialYOffset;
        params.width = mScreenWidth;
        params.height = mScreenBottom;
        params.setTitle("HVAC Container");
        disableAnimations(params);
        // required of the sysui visiblity listener is not triggered.
        params.hasSystemUiListeners = true;

        mContainer = inflater.inflate(R.layout.hvac_panel, null);
        mContainer.setLayoutParams(params);
        mContainer.setOnSystemUiVisibilityChangeListener(visibility -> {
            Log.i(TAG, "layoutHvacUi: visibility:" + visibility);
            boolean systemUiVisible = (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0;
            int y = 0;
            if (systemUiVisible) {
                // 当systemUi可见时，窗口系统坐标从系统导航栏上方的0开始。
                // 因此，如果我们想获得屏幕底部的实际高度，我们需要将y值设置为导航栏高度的负值。
                y = -mNavBarHeight;
            }
            setYPosition(mDriverTemperatureBar, y);
            setYPosition(mPassengerTemperatureBar, y);
            setYPosition(mDriverTemperatureBarCollapsed, y);
            setYPosition(mPassengerTemperatureBarCollapsed, y);
            setYPosition(mContainer, y);
        });

        // 顶部填充应根据屏幕高度和扩展hvac面板的高度进行计算。由填充物定义的空间意味着可以单击以关闭hvac面板。
        int topPadding = mScreenBottom - mPanelFullExpandedHeight;
        mContainer.setPadding(0, topPadding, 0, 0);

        mContainer.setFocusable(false);
        mContainer.setFocusableInTouchMode(false);

        View panel = mContainer.findViewById(R.id.hvac_center_panel);
        panel.getLayoutParams().height = mPanelCollapsedHeight;

        addViewToWindowManagerAndTrack(mContainer, params);

        createTemperatureBars(inflater);

        // UI状态控制器，用来控制展开/收起时UI的各种状态并执行动画
        mHvacPanelController = new HvacPanelController(this /* context */, mContainer,
                mWindowManager, mDriverTemperatureBar, mPassengerTemperatureBar,
                mDriverTemperatureBarCollapsed, mPassengerTemperatureBarCollapsed
        );
        // 绑定 HvacController Service
        Intent bindIntent = new Intent(this /* context */, HvacController.class);
        if (!bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Failed to connect to HvacController.");
        }
    }

    // 添加View到WindowManager中
    private void addViewToWindowManagerAndTrack(View view, WindowManager.LayoutParams params) {
        mWindowManager.addView(view, params);
        mAddedViews.add(view);
    }

    private void setYPosition(View v, int y) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) v.getLayoutParams();
        lp.y = y;
        mWindowManager.updateViewLayout(v, lp);
    }

    private void createTemperatureBars(LayoutInflater inflater) {
        mDriverTemperatureBarCollapsed = createTemperatureBarOverlay(inflater,
                "HVAC Driver Temp collapsed",
                mNavBarHeight,
                Gravity.BOTTOM | Gravity.LEFT);

        mPassengerTemperatureBarCollapsed = createTemperatureBarOverlay(inflater,
                "HVAC Passenger Temp collapsed",
                mNavBarHeight,
                Gravity.BOTTOM | Gravity.RIGHT);

        mDriverTemperatureBar = createTemperatureBarOverlay(inflater,
                "HVAC Driver Temp",
                mTemperatureOverlayHeight,
                Gravity.BOTTOM | Gravity.LEFT);

        mPassengerTemperatureBar = createTemperatureBarOverlay(inflater,
                "HVAC Passenger Temp",
                mTemperatureOverlayHeight,
                Gravity.BOTTOM | Gravity.RIGHT);
    }

    private TemperatureBarOverlay createTemperatureBarOverlay(LayoutInflater inflater,
                                                              String windowTitle, int windowHeight, int gravity) {
        WindowManager.LayoutParams params = createTemperatureBarLayoutParams(
                windowTitle, windowHeight, gravity);
        TemperatureBarOverlay button = (TemperatureBarOverlay) inflater
                .inflate(R.layout.hvac_temperature_bar_overlay, null);
        button.setLayoutParams(params);
        addViewToWindowManagerAndTrack(button, params);
        return button;
    }

    // 注意：窗口管理器不复制布局参数，而是使用提供的对象，因此每个窗口都需要一个新的副本，否则更改1会影响其他窗口
    private WindowManager.LayoutParams createTemperatureBarLayoutParams(String windowTitle,
                                                                        int windowHeight, int gravity) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.x = mTemperatureSideMargin;
        lp.y = mInitialYOffset;
        lp.width = mTemperatureOverlayWidth;
        disableAnimations(lp);
        lp.setTitle(windowTitle);
        lp.height = windowHeight;
        lp.gravity = gravity;
        return lp;
    }

    /**
     * 当窗口管理器更新子视图时禁用动画。
     */
    private void disableAnimations(WindowManager.LayoutParams params) {
        try {
            int currentFlags = (Integer) params.getClass().getField("privateFlags").get(params);
            params.getClass().getField("privateFlags").set(params, currentFlags | 0x00000040);
        } catch (Exception e) {
            Log.e(TAG, "Error disabling animation");
        }
    }

    @Override
    public void onDestroy() {
        for (View view : mAddedViews) {
            mWindowManager.removeView(view);
        }
        mAddedViews.clear();
        if (mHvacController != null) {
            unbindService(mServiceConnection);
        }
        unregisterReceiver(mBroadcastReceiver);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mHvacController = ((HvacController.LocalBinder) service).getService();
            final Context context = HvacUiService.this;

            final Runnable r = () -> {
                // hvac控制器从车辆刷新其值后，绑定所有值。
                mHvacPanelController.updateHvacController(mHvacController);
            };

            if (mHvacController != null) {
                mHvacController.requestRefresh(r, new Handler(context.getMainLooper()));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mHvacController = null;
            mHvacPanelController.updateHvacController(null);
            //TODO:b/29126575重新启动后重新连接控制器
        }
    };
}
