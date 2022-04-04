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

import android.car.hardware.hvac.CarHvacManager;
import android.util.Log;
import android.util.SparseIntArray;

import com.android.car.hvac.HvacController;
import com.android.car.hvac.ui.FanDirectionButtons;

/**
 * 用于处理风扇方向变化的控制器。
 * 还将{@link FanDirectionButtons}中指定的风扇方向映射到车辆硬件中的{@link CarHvacManager}{@code#fan_DIRECTION_*}常量。
 */
public class FanDirectionButtonsController {
    private static final String TAG = "FanDirectionButtonsController";
    private final FanDirectionButtons mFanDirectionButtons;
    private final HvacController mHvacController;
    private final SparseIntArray mFanDirectionMap =
            new SparseIntArray(FanDirectionButtons.FAN_DIRECTION_COUNT);

    public FanDirectionButtonsController(FanDirectionButtons speedBar, HvacController controller) {
        mFanDirectionButtons = speedBar;
        mHvacController = controller;
        initialize();
    }

    private void initialize() {
        // 注：此处使用的是特定于车辆的值，因为并非所有车辆都有地板和除霜器风扇方向。
        mFanDirectionMap.put(FanDirectionButtons.FAN_DIRECTION_FACE,
                CarHvacManager.FAN_DIRECTION_FACE);
        mFanDirectionMap.put(FanDirectionButtons.FAN_DIRECTION_FACE_FLOOR,
                (CarHvacManager.FAN_DIRECTION_FACE | CarHvacManager.FAN_DIRECTION_FLOOR));
        mFanDirectionMap.put(FanDirectionButtons.FAN_DIRECTION_FLOOR,
                CarHvacManager.FAN_DIRECTION_FLOOR);
        mFanDirectionMap.put(FanDirectionButtons.FAN_DIRECTION_FLOOR_DEFROSTER,
                (CarHvacManager.FAN_DIRECTION_DEFROST | CarHvacManager.FAN_DIRECTION_FLOOR));
        mFanDirectionButtons.setFanDirectionClickListener(mListener);
        mHvacController.registerCallback(mCallback);
    }

    private final FanDirectionButtons.FanDirectionClickListener mListener
            = new FanDirectionButtons.FanDirectionClickListener() {
        @Override
        public void onFanDirectionClicked(@FanDirectionButtons.FanDirection int direction) {
            mHvacController.setFanDirection(mFanDirectionMap.get(direction));
        }
    };

    private HvacController.Callback mCallback = new HvacController.Callback() {
        @Override
        public void onFanDirectionChange(int direction) {
            int index = mFanDirectionMap.indexOfValue(direction);
            if (index == -1) {
                Log.w(TAG, "Unexpected fan direction: " + direction);
                return;
            }
            mFanDirectionButtons.setFanDirection(mFanDirectionMap.keyAt(index));
        }
    };
}
