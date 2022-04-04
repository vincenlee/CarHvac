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
package com.android.car.hvac.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.car.hvac.R;

import java.util.ArrayList;

/**
 * 表示风扇速度栏。该栏由风扇速度按钮列表组成。当选择速度时，所有较低级别将打开，所有较高级别将关闭。还可以打开和关闭当前选择的速度。
 */
public class FanSpeedBar extends RelativeLayout {
    /**
     * 单击风扇速度栏中的按钮时会收到通知的侦听器。
     */
    public interface FanSpeedButtonClickListener {
        void onMaxButtonClicked();

        void onOffButtonClicked();

        void onFanSpeedSegmentClicked(int position);
    }

    private static final int BAR_SEGMENT_ANIMATION_DELAY_MS = 50;
    private static final int BAR_SEGMENT_ANIMATION_MS = 100;
    private static final int NUM_FAN_SPEED = 34;

    private int mButtonEnabledTextColor;
    private int mButtonDisabledTextColor;

    private int mFanOffEnabledBgColor;
    private int mFanMaxEnabledBgColor;

    private float mCornerRadius;

    private TextView mMaxButton;
    private TextView mOffButton;

    private FanSpeedBarSegment mFanSpeed1;
    private FanSpeedBarSegment mFanSpeed2;
    private FanSpeedBarSegment mFanSpeed3;
    private FanSpeedBarSegment mFanSpeed4;

    private FanSpeedButtonClickListener mListener;

    private final ArrayList<FanSpeedBarSegment> mFanSpeedButtons = new ArrayList<>(NUM_FAN_SPEED);

    public FanSpeedBar(Context context) {
        super(context);
        init();
    }

    public FanSpeedBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FanSpeedBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.fan_speed, this);

        Resources res = getContext().getResources();
        // 风扇速度栏设置为高度72dp，以匹配最小抽头目标尺寸。然而，它是插入的，以使它看起来更薄。
        int barHeight = res.getDimensionPixelSize(R.dimen.hvac_fan_speed_bar_height);
        int insetHeight = res.getDimensionPixelSize(R.dimen.hvac_fan_speed_bar_vertical_inset);
        mCornerRadius = (barHeight - 2 * insetHeight) / 2;

        mFanOffEnabledBgColor = res.getColor(R.color.hvac_fanspeed_off_enabled_bg);

        mButtonEnabledTextColor = res.getColor(R.color.hvac_fanspeed_off_enabled_text_color);
        mButtonDisabledTextColor = res.getColor(R.color.hvac_fanspeed_off_disabled_text_color);
        mFanMaxEnabledBgColor = res.getColor(R.color.hvac_fanspeed_segment_color);
    }

    public void setFanspeedButtonClickListener(FanSpeedButtonClickListener clickListener) {
        mListener = clickListener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFanSpeed1 = (FanSpeedBarSegment) findViewById(R.id.fan_speed_1);
        mFanSpeed2 = (FanSpeedBarSegment) findViewById(R.id.fan_speed_2);
        mFanSpeed3 = (FanSpeedBarSegment) findViewById(R.id.fan_speed_3);
        mFanSpeed4 = (FanSpeedBarSegment) findViewById(R.id.fan_speed_4);

        mFanSpeed1.setTag(R.id.TAG_FAN_SPEED_LEVEL, 1);
        mFanSpeed2.setTag(R.id.TAG_FAN_SPEED_LEVEL, 2);
        mFanSpeed3.setTag(R.id.TAG_FAN_SPEED_LEVEL, 3);
        mFanSpeed4.setTag(R.id.TAG_FAN_SPEED_LEVEL, 4);

        mFanSpeedButtons.add(mFanSpeed1);
        mFanSpeedButtons.add(mFanSpeed2);
        mFanSpeedButtons.add(mFanSpeed3);
        mFanSpeedButtons.add(mFanSpeed4);

        for (View view : mFanSpeedButtons) {
            view.setOnClickListener(mFanSpeedBarClickListener);
        }

        mMaxButton = (TextView) findViewById(R.id.fan_max);
        mMaxButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setMax();
                if (mListener != null) {
                  mListener.onMaxButtonClicked();
                }
            }
        });

        mOffButton = (TextView) findViewById(R.id.fan_off);
        mOffButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setOff();
                if (mListener != null) {
                    mListener.onOffButtonClicked();
                }
            }
        });

        // 根据条的高度设置“关闭/最大”按钮的角半径，以获得半圆形边框。
        GradientDrawable offButtonBg = new GradientDrawable();
        offButtonBg.setCornerRadii(new float[]{mCornerRadius, mCornerRadius, 0, 0,
                0, 0, mCornerRadius, mCornerRadius});
        mOffButton.setBackground(offButtonBg);
        mOffButton.setTextColor(mButtonDisabledTextColor);

        GradientDrawable maxButtonBg = new GradientDrawable();
        maxButtonBg.setCornerRadii(new float[]{0, 0, mCornerRadius, mCornerRadius,
                mCornerRadius, mCornerRadius, 0, 0});
        mMaxButton.setBackground(maxButtonBg);
        mMaxButton.setTextColor(mButtonDisabledTextColor);
    }

    public void setMax() {
        int numFanSpeed = mFanSpeedButtons.size();
        int delay = 0;

        for (int i = 0; i < numFanSpeed; i++) {
            if (!mFanSpeedButtons.get(i).isTurnedOn()) {
                mFanSpeedButtons.get(i).playTurnOnAnimation(BAR_SEGMENT_ANIMATION_MS, delay);
                delay += BAR_SEGMENT_ANIMATION_DELAY_MS;
            }
        }
        setOffButtonEnabled(false);
        setMaxButtonEnabled(true);
    }

    private void setMaxButtonEnabled(boolean enabled) {
        GradientDrawable background = (GradientDrawable) mMaxButton.getBackground();
        if (enabled) {
            background.setColor(mFanMaxEnabledBgColor);
            mMaxButton.setTextColor(mButtonEnabledTextColor);
        } else {
            background.setColor(Color.TRANSPARENT);
            mMaxButton.setTextColor(mButtonDisabledTextColor);
        }
    }


    private void setOffButtonEnabled(boolean enabled) {
        GradientDrawable background = (GradientDrawable) mOffButton.getBackground();
        if (enabled) {
            background.setColor(mFanOffEnabledBgColor);
            mOffButton.setTextColor(mButtonEnabledTextColor);
        } else {
            background.setColor(Color.TRANSPARENT);
            mOffButton.setTextColor(mButtonDisabledTextColor);
        }
    }

    public void setOff() {
        setOffButtonEnabled(true);
        setMaxButtonEnabled(false);

        int numFanSpeed = mFanSpeedButtons.size();
        int delay = 0;
        for (int i = numFanSpeed - 1; i >= 0; i--) {
            if (mFanSpeedButtons.get(i).isTurnedOn()) {
                mFanSpeedButtons.get(i).playTurnOffAnimation(BAR_SEGMENT_ANIMATION_MS, delay);
                delay += BAR_SEGMENT_ANIMATION_DELAY_MS;
            }
        }
    }

    /**
     * 根据位置将风扇速度段设置为开-关。请注意，如果需要动画，则更改不会动画化，请使用{@link FanSpeedBar#animateToSpeedSegment（int）}
     */
    public void setSpeedSegment(int position) {
        for (int i = 0; i < mFanSpeedButtons.size(); i++) {
            // 对于低于该位置的线段，应将其启用。
            mFanSpeedButtons.get(i).setTurnedOn(i < position ? true : false);
        }
    }

    /**
     * 将风扇速度栏设置为特定位置的动画。打开之前的所有位置，关闭之后的所有位置。
     */
    public void animateToSpeedSegment(int position) {
        setOffButtonEnabled(false);
        setMaxButtonEnabled(false);

        int fanSpeedCount = mFanSpeedButtons.size();
        int fanSpeedIndex = position - 1;

        if (fanSpeedIndex < 0) {
            fanSpeedIndex = 0;
        } else if (fanSpeedIndex > fanSpeedCount) {
            fanSpeedIndex = fanSpeedCount - 1;
        }

        int delay = 0;
        if (mFanSpeedButtons.get(fanSpeedIndex).isTurnedOn()) {
            // 如果选定位置已打开，则确保关闭后的每个段。
            for (int i = fanSpeedCount - 1; i > fanSpeedIndex; i--) {
                if (mFanSpeedButtons.get(i).isTurnedOn()) {
                    mFanSpeedButtons.get(i).playTurnOffAnimation(BAR_SEGMENT_ANIMATION_MS, delay);
                    delay += BAR_SEGMENT_ANIMATION_DELAY_MS;
                }
            }
        } else {
            // 如果所选位置已关闭，则打开其之前的所有位置，并打开其自身。
            for (int i = 0; i <= fanSpeedIndex; i++) {
                if (!mFanSpeedButtons.get(i).isTurnedOn()) {
                    mFanSpeedButtons.get(i).playTurnOnAnimation(BAR_SEGMENT_ANIMATION_MS, delay);
                    delay += BAR_SEGMENT_ANIMATION_DELAY_MS;
                }
            }
        }
    }

    private final OnClickListener mFanSpeedBarClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int level = (int) v.getTag(R.id.TAG_FAN_SPEED_LEVEL);

            setOffButtonEnabled(false);
            setMaxButtonEnabled(false);

            int fanSpeedCount = mFanSpeedButtons.size();
            int fanSpeedIndex = level - 1;

            // 如果“选定速度”是栏中的最后一段，请将其禁用，因为它当前处于启用状态。
            if (fanSpeedIndex == fanSpeedCount - 1
                    && mFanSpeedButtons.get(fanSpeedIndex).isTurnedOn()) {
                mFanSpeedButtons.get(fanSpeedIndex)
                        .playTurnOffAnimation(BAR_SEGMENT_ANIMATION_MS, 0);
                return;
            }

            // 如果选定的速度为on，而下一个风扇速度未为on
            //然后关闭选定的速度。
            if (fanSpeedIndex < fanSpeedCount - 1
                    && mFanSpeedButtons.get(fanSpeedIndex).isTurnedOn()
                    && !mFanSpeedButtons.get(fanSpeedIndex + 1).isTurnedOn()) {
                mFanSpeedButtons.get(fanSpeedIndex)
                        .playTurnOffAnimation(BAR_SEGMENT_ANIMATION_MS, 0);
                return;
            }

            int delay = 0;
            for (int i = 0; i < level; i++) {
                if (!mFanSpeedButtons.get(i).isTurnedOn()) {
                    mFanSpeedButtons.get(i).playTurnOnAnimation(BAR_SEGMENT_ANIMATION_MS, delay);
                    delay += BAR_SEGMENT_ANIMATION_DELAY_MS;
                }
            }

            delay = 0;
            for (int i = fanSpeedCount - 1; i >= level; i--) {
                if (mFanSpeedButtons.get(i).isTurnedOn()) {
                    mFanSpeedButtons.get(i).playTurnOffAnimation(BAR_SEGMENT_ANIMATION_MS, delay);
                    delay += BAR_SEGMENT_ANIMATION_DELAY_MS;
                }
            }

            if (mListener != null) {
                mListener.onFanSpeedSegmentClicked(level);
            }
        }
    };
}
