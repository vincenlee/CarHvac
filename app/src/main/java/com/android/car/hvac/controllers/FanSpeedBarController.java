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
package com.android.car.hvac.controllers;

import android.util.Log;
import com.android.car.hvac.HvacController;
import com.android.car.hvac.ui.FanSpeedBar;

/**
 * 用于调节风扇速度的风扇速度条控制器。
 */
public class FanSpeedBarController {
    private final static String TAG = "FanSpeedBarCtrl";

    private final FanSpeedBar mFanSpeedBar;
    private final HvacController mHvacController;
    private int mCurrentFanSpeed;

    // 注：以下是特定于汽车的数值。
    private static final int MAX_FAN_SPEED = 6;
    private static final int MIN_FAN_SPEED = 1;

    public FanSpeedBarController(FanSpeedBar speedBar, HvacController controller) {
        mFanSpeedBar = speedBar;
        mHvacController = controller;
        initialize();
    }

    private void initialize() {
        mFanSpeedBar.setFanspeedButtonClickListener(mClickListener);
        mHvacController.registerCallback(mCallback);
        // 在初始化期间，我们不需要设置更改的动画。
        handleFanSpeedUpdate(mHvacController.getFanSpeed(), false /* animateUpdate */);
    }

    private void handleFanSpeedUpdate(int speed, boolean animateUpdate) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Fan speed bar being set to value: " + speed);
        }

        mCurrentFanSpeed = speed;
        if (mCurrentFanSpeed == MIN_FAN_SPEED) {
            mFanSpeedBar.setOff();
        } else if (mCurrentFanSpeed >= MAX_FAN_SPEED) {
            mFanSpeedBar.setMax();
        } else if (mCurrentFanSpeed < MAX_FAN_SPEED && mCurrentFanSpeed > MIN_FAN_SPEED) {
            // 注意所使用的特定于车辆的值：最低风扇转速由off按钮表示，第一段实际上代表第二个风扇转速设置。
            if (animateUpdate) {
                mFanSpeedBar.animateToSpeedSegment(mCurrentFanSpeed - 1);
            } else {
                mFanSpeedBar.setSpeedSegment(mCurrentFanSpeed - 1);
            }
        }
    }

    private FanSpeedBar.FanSpeedButtonClickListener mClickListener
            = new FanSpeedBar.FanSpeedButtonClickListener() {
        @Override
        public void onMaxButtonClicked() {
            mHvacController.setFanSpeed(MAX_FAN_SPEED);
        }

        @Override
        public void onOffButtonClicked() {
            mHvacController.setFanSpeed(MIN_FAN_SPEED);
        }

        @Override
        public void onFanSpeedSegmentClicked(int position) {
            // 注意所使用的特定于车辆的值：最低风扇转速由off按钮表示，第一段实际上代表第二个风扇转速设置。
            mHvacController.setFanSpeed(position + 1);
        }
    };

    private HvacController.Callback mCallback = new HvacController.Callback() {
        @Override
        public void onFanSpeedChange(int speed) {
            handleFanSpeedUpdate(speed, true /* animateUpdate */);
        }
    };
}
