/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.vrtoxin.ActionConfig;
import com.android.internal.util.vrtoxin.ActionConstants;
import com.android.internal.util.vrtoxin.ActionHelper;
import com.android.internal.util.vrtoxin.DeviceUtils;
import com.android.internal.util.vrtoxin.ImageHelper;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.ArrayList;
import java.util.List;

public class NavigationBarView extends LinearLayout
        implements BaseStatusBar.NavigationBarCallback {

    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/NavigationBarView";

    // slippery nav bar when everything is disabled, e.g. during setup
    final static boolean SLIPPERY_WHEN_DISABLED = true;

    // Definitions for navbar menu button customization
    private final static int SHOW_RIGHT_MENU = 0;
    private final static int SHOW_LEFT_MENU = 1;
    private final static int SHOW_BOTH_MENU = 2;

    private final static int MENU_VISIBILITY_ALWAYS = 0;
    private final static int MENU_VISIBILITY_NEVER = 1;
    private final static int MENU_VISIBILITY_SYSTEM = 2;

    private static final int KEY_MENU_RIGHT = 0;
    private static final int KEY_MENU_LEFT = 1;
    private static final int KEY_IME_SWITCHER = 2;
    private static final int KEY_IME_LEFT = 3;
    private static final int KEY_IME_RIGHT = 4;
    private static final int KEY_EMPTY_LEFT = 5;
    private static final int KEY_EMPTY_RIGHT = 6;

    private final static int HIDE_IME_ARROW = 0;
    private final static int SHOW_IME_ARROW = 1;

    private int mMenuVisibility;
    private int mMenuSetting;
    private boolean mOverrideMenuKeys;
    private boolean mIsImeButtonVisible = false;

    private boolean mImeArrowVisibility;
    private boolean mIsImeArrowVisible = false;

    private boolean mIsHandlerCallbackActive = false;

    final Display mDisplay;
    View mCurrentView = null;
    View[] mRotatedViews = new View[4];

    int mBarSize;
    boolean mVertical;
    boolean mScreenOn;
    boolean mLeftInLandscape;

    boolean mShowMenu;
    int mDisabledFlags = 0;
    int mNavigationIconHints = 0;

    private BackButtonDrawable mBackIcon, mBackLandIcon;

    private int mRippleColor;

    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;

    private DeadZone mDeadZone;
    private final NavigationBarTransitions mBarTransitions;

    private int mNavBarButtonColor;
    private int mNavBarButtonColorMode;
    private boolean mAppIsBinded = false;

    private FrameLayout mRot0;
    private FrameLayout mRot90;

    private boolean mDimNavButtons;
    private int mDimNavButtonsTimeout;
    private float mDimNavButtonsAlpha = 0.5f;
    private float mOriginalAlpha = 1.0f;
    private boolean mIsDim = false;
    private boolean mIsAnimating = false;
    private boolean mDimNavButtonsAnimate;
    private int mDimNavButtonsAnimateDuration;
    private boolean mDimNavButtonsTouchAnywhere;
    private PowerManager mPowerManager;
    private boolean mIsPowerSaveMode = false;
    private ObjectAnimator mFadeOut;
    private boolean mIsExpandedDesktopOn;
    private KeyguardManager mKgm;

    private ArrayList<ActionConfig> mButtonsConfig;
    private List<Integer> mButtonIdList;

    private KeyButtonView.LongClickCallback mCallback;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    // performs manual animation in sync with layout transitions
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();

    private OnVerticalChangedListener mOnVerticalChangedListener;
    private boolean mIsLayoutRtl;
    private boolean mLayoutTransitionsEnabled = true;
    private boolean mWakeAndUnlocking;

    private GestureDetector mDoubleTapGesture;

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private boolean mHomeAppearing;
        private long mStartDelay;
        private long mDuration;
        private TimeInterpolator mInterpolator;

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = true;
                mStartDelay = transition.getStartDelay(transitionType);
                mDuration = transition.getDuration(transitionType);
                mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = false;
            }

            if (view.getId() == R.id.home)
                onNavButtonTouched();
        }

        public void onBackAltCleared() {
            // When dismissing ime during unlock, force the back button to run the same appearance
            // animation as home (if we catch this condition early enough).
            if (getBackButton() == null || getHomeButton() == null) return;
            if (!mBackTransitioning && getBackButton().getVisibility() == VISIBLE
                    && mHomeAppearing && getHomeButton().getAlpha() == 0) {
                getBackButton().setAlpha(0);
                ValueAnimator a = ObjectAnimator.ofFloat(getBackButton(), "alpha", 0, 1);
                a.setStartDelay(mStartDelay);
                a.setDuration(mDuration);
                a.setInterpolator(mInterpolator);
                a.start();
            }
        }
    }

    private final OnClickListener mImeSwitcherClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showInputMethodPicker(true /* showAuxiliarySubtypes */);
        }
    };

    // provides a listener for the empty space in the navbar
    private final OnTouchListener mNavButtonsTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mDimNavButtons) {
                onNavButtonTouched();
            }
            if (Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.DOUBLE_TAP_SLEEP_NAVBAR, 0, UserHandle.USER_CURRENT) == 1) {
                mDoubleTapGesture.onTouchEvent(event);
            }
            return true;
        }
    };

    public void onNavButtonTouched() {
        if (mIsHandlerCallbackActive) {
            mHandler.removeCallbacks(mNavButtonDimmer);
            mIsHandlerCallbackActive = false;
        }

        // power saving mode is on, do nothing
        if (mIsPowerSaveMode) return;

        final ViewGroup navButtons = getNavButtons();
        if (navButtons != null) {
            // restore alpha to previous state first
            if (mIsDim || mIsAnimating) {
                mIsAnimating = false;
                resetDim(navButtons);
            }
            if (mDimNavButtons && !mIsExpandedDesktopOn &&
                    !(mKgm != null ? mKgm.isDeviceLocked() : false)) {
                mHandler.postDelayed(mNavButtonDimmer, mDimNavButtonsTimeout);
                mIsHandlerCallbackActive = true;
            }
        }
    }

    private void resetDim(ViewGroup navButtons) {
        if (navButtons == null) {
            navButtons = getNavButtons();
        }
        if (navButtons != null) {
            if (mFadeOut != null) {
                mFadeOut.cancel();
            }
            mIsDim = false;
            navButtons.setAlpha(mOriginalAlpha);
        }
    }

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = mCurrentView.getWidth();
                    final int vh = mCurrentView.getHeight();

                    if (h != vh || w != vw) {
                        Log.w(TAG, String.format(
                            "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                            how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    // broadcast receiver for power saving mode
    private final BroadcastReceiver mBatteryDimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                Log.d(TAG, "Broadcast received: " + intent.getAction());
            }

            mIsPowerSaveMode = mPowerManager.isPowerSaveMode();
            if (mIsPowerSaveMode) {
                // battery is low, no dim until charged
                resetDim(null);
            }
            onNavButtonTouched();
        }
    };

    private final IntentFilter mBatteryFilter = new IntentFilter(
        PowerManager.ACTION_POWER_SAVE_MODE_CHANGING);

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDisplay = ((WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        final Resources res = getContext().getResources();
        mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);

        mBarTransitions = new NavigationBarTransitions(this);

        mDoubleTapGesture = new GestureDetector(mContext,
                new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                if (pm != null) pm.goToSleep(e.getEventTime());
                return true;
            }
        });

        mButtonsConfig = ActionHelper.getNavBarConfig(mContext);
        mButtonIdList = new ArrayList<Integer>();

        getIcons(res);

        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mIsPowerSaveMode = mPowerManager.isPowerSaveMode();

        mKgm = (KeyguardManager)
                mContext.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            root.setDrawDuringWindowsAnimating(true);
        }
        mContext.registerReceiver(mBatteryDimReceiver, mBatteryFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mContext.unregisterReceiver(mBatteryDimReceiver);
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setBar(PhoneStatusBar phoneStatusBar) {
        mTaskSwitchHelper.setBar(phoneStatusBar);
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(mVertical);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDimNavButtonsTouchAnywhere) {
            onNavButtonTouched();
        }
        if (mTaskSwitchHelper.onTouchEvent(event)) {
            return true;
        }
        if (mDeadZone != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mDeadZone.poke(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mTaskSwitchHelper.onInterceptTouchEvent(event);
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    private final H mHandler = new H();

    public List<Integer> getButtonIdList() {
        return mButtonIdList;
    }

    public View getCurrentView() {
        return mCurrentView;
    }

    public View getRecentsButton() {
        return mCurrentView.findViewById(R.id.recent_apps);
    }

    public View getLeftEmptyButton() {
        return mCurrentView.findViewById(R.id.empty_left);
    }

    public View getRightEmptyButton() {
        return mCurrentView.findViewById(R.id.empty_right);
    }

    public View getLeftMenuButton() {
        return mCurrentView.findViewById(R.id.menu_left);
    }

    public View getRightMenuButton() {
        return mCurrentView.findViewById(R.id.menu);
    }

    public View getCustomButton(int buttonId) {
        return mCurrentView.findViewById(buttonId);
    }

    public View getBackButton() {
        return mCurrentView.findViewById(R.id.back);
    }

    public KeyButtonView getHomeButton() {
        return (KeyButtonView) mCurrentView.findViewById(R.id.home);
    }

    public View getImeSwitchButton() {
        return mCurrentView.findViewById(R.id.ime_switcher);
    }

    public ViewGroup getNavButtons() {
        return (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
    }

    public View getLeftImeArrowButton() {
        return mCurrentView.findViewById(R.id.ime_left);
    }

    public View getRightImeArrowButton() {
        return mCurrentView.findViewById(R.id.ime_right);
    }

    public void setOverrideMenuKeys(boolean b) {
        mOverrideMenuKeys = b;
        setMenuVisibility(mShowMenu, true /* force */);
    }

    private void getIcons(Resources res) {
        Drawable backIcon, backIconLand;
        ActionConfig actionConfig;
        String backIconUri = ActionConstants.ICON_EMPTY;
        for (int j = 0; j < mButtonsConfig.size(); j++) {
            actionConfig = mButtonsConfig.get(j);
            final String action = actionConfig.getClickAction();
            if (action.equals(ActionConstants.ACTION_BACK)) {
                backIconUri = actionConfig.getIcon();
            }
        }

        backIcon = ActionHelper.getActionIconImage(mContext,
                ActionConstants.ACTION_BACK, backIconUri);
        backIconLand = backIcon;

        boolean shouldColor = true;
        if (backIconUri != null && !backIconUri.equals(ActionConstants.ICON_EMPTY)
                && !backIconUri.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)
                && mNavBarButtonColorMode == 1) {
            shouldColor = false;
        }

        // update back buttons color
        if (shouldColor && mNavBarButtonColorMode != 3) {
            backIcon.mutate();
            backIcon.setTintMode(PorterDuff.Mode.MULTIPLY);
            backIcon.setTint(mNavBarButtonColor);

            backIconLand.mutate();
            backIconLand.setTintMode(PorterDuff.Mode.MULTIPLY);
            backIconLand.setTint(mNavBarButtonColor);
        }

        mBackIcon     = new BackButtonDrawable(backIcon);
        mBackLandIcon = new BackButtonDrawable(backIconLand);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        updateSettings(true);

        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
        setDisabledFlags(mDisabledFlags, true);
    }

    public void setPinningCallback(KeyButtonView.LongClickCallback c) {
        mCallback = c;
    }

    private void makeBar() {
        if (mButtonsConfig.isEmpty() || mButtonsConfig == null) {
            return;
        }

        mButtonIdList.clear();

        mRippleColor = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_GLOW_TINT, -2, UserHandle.USER_CURRENT);

        ((LinearLayout) mRot0.findViewById(R.id.nav_buttons)).removeAllViews();
        ((LinearLayout) mRot0.findViewById(R.id.lights_out)).removeAllViews();
        ((LinearLayout) mRot90.findViewById(R.id.nav_buttons)).removeAllViews();
        ((LinearLayout) mRot90.findViewById(R.id.lights_out)).removeAllViews();

        for (int i = 0; i <= 1; i++) {
            boolean landscape = (i == 1);

            LinearLayout navButtonLayout = (LinearLayout) (landscape ? mRot90
                    .findViewById(R.id.nav_buttons) : mRot0
                    .findViewById(R.id.nav_buttons));

            LinearLayout lightsOut = (LinearLayout) (landscape ? mRot90
                    .findViewById(R.id.lights_out) : mRot0
                    .findViewById(R.id.lights_out));

            // add left menu
            KeyButtonView leftMenuKeyView = generateMenuKey(landscape, KEY_MENU_LEFT);
            leftMenuKeyView.setLongClickCallback(mCallback);
            addButton(navButtonLayout, leftMenuKeyView, landscape);
            addLightsOutButton(lightsOut, leftMenuKeyView, landscape, true);

            KeyButtonView leftImeArrow = generateMenuKey(landscape, KEY_IME_LEFT);
            addButton(navButtonLayout, leftImeArrow, landscape);
            addLightsOutButton(lightsOut, leftImeArrow, landscape, true);

            KeyButtonView leftEmpty = generateMenuKey(landscape, KEY_EMPTY_LEFT);
            addButton(navButtonLayout, leftEmpty, landscape);
            addLightsOutButton(lightsOut, leftEmpty, landscape, true);

            mAppIsBinded = false;
            ActionConfig actionConfig;

            for (int j = 0; j < mButtonsConfig.size(); j++) {
                actionConfig = mButtonsConfig.get(j);
                KeyButtonView v = generateKey(landscape,
                        actionConfig.getClickAction(),
                        actionConfig.getLongpressAction(),
                        actionConfig.getIcon());
                v.setTag((landscape ? "key_land_" : "key_") + j);

                addButton(navButtonLayout, v, landscape);
                addLightsOutButton(lightsOut, v, landscape, false);

                if (mButtonsConfig.size() == 3
                        && j != (mButtonsConfig.size() - 1)) {
                    // add separator view here
                    View separator = new View(mContext);
                    separator.setLayoutParams(getSeparatorLayoutParams(landscape));
                    addButton(navButtonLayout, separator, landscape);
                    addLightsOutButton(lightsOut, separator, landscape, true);
                }

            }

            KeyButtonView rightMenuKeyView = generateMenuKey(landscape, KEY_MENU_RIGHT);
            rightMenuKeyView.setLongClickCallback(mCallback);
            addButton(navButtonLayout, rightMenuKeyView, landscape);
            addLightsOutButton(lightsOut, rightMenuKeyView, landscape, true);

            KeyButtonView rightImeArrow = generateMenuKey(landscape, KEY_IME_RIGHT);
            addButton(navButtonLayout, rightImeArrow, landscape);
            addLightsOutButton(lightsOut, rightImeArrow, landscape, true);

            View imeSwitcher = generateMenuKey(landscape, KEY_IME_SWITCHER);
            addButton(navButtonLayout, imeSwitcher, landscape);
            addLightsOutButton(lightsOut, imeSwitcher, landscape, true);

            KeyButtonView rightEmpty = generateMenuKey(landscape, KEY_EMPTY_RIGHT);
            addButton(navButtonLayout, rightEmpty, landscape);
            addLightsOutButton(lightsOut, rightEmpty, landscape, true);
        }
        setMenuVisibility(mShowMenu, true);
    }

    public void recreateNavigationBar() {
        updateSettings(true);
    }

    public void updateNavigationBarSettings() {
        updateSettings(false);
    }

    private KeyButtonView generateKey(boolean landscape, String clickAction,
            String longpress,
            String iconUri) {

        KeyButtonView v = new KeyButtonView(mContext, null);
        v.setClickAction(clickAction);
        if (!clickAction.equals(ActionConstants.ACTION_HOME)) {
            // Don't touch long press listener for longpress Home button
            v.setLongpressAction(longpress);
        }
        int i = mContext.getResources().getDimensionPixelSize(R.dimen.navigation_key_width);
        v.setLayoutParams(getLayoutParams(landscape, i));

        if (clickAction.equals(ActionConstants.ACTION_BACK)) {
            v.setId(R.id.back);
        } else if (clickAction.equals(ActionConstants.ACTION_HOME)) {
            v.setId(R.id.home);
        } else if (clickAction.equals(ActionConstants.ACTION_RECENTS)) {
            v.setId(R.id.recent_apps);
        } else {
            int buttonId = v.generateViewId();
            v.setId(buttonId);
            mButtonIdList.add(buttonId);
        }

        if (clickAction.startsWith("**")) {
            v.setScaleType(KeyButtonView.ScaleType.CENTER_INSIDE);
        }

        boolean colorize = true;
        if (iconUri != null && !iconUri.equals(ActionConstants.ICON_EMPTY)
                && !iconUri.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)
                && mNavBarButtonColorMode == 1) {
            colorize = false;
        } else if (!clickAction.startsWith("**")) {
            final int[] appIconPadding = getAppIconPadding();
            if (landscape) {
                v.setPaddingRelative(appIconPadding[1], appIconPadding[0],
                        appIconPadding[3], appIconPadding[2]);
            } else {
                v.setPaddingRelative(appIconPadding[0], appIconPadding[1],
                        appIconPadding[2], appIconPadding[3]);
            }
            if (mNavBarButtonColorMode != 0) {
                colorize = false;
            }
            mAppIsBinded = true;
        }

        Drawable d = ActionHelper.getActionIconImage(mContext, clickAction, iconUri);

        if (d != null) {
            d.mutate();
            if (colorize && mNavBarButtonColorMode != 3) {
                d = ImageHelper.getColoredDrawable(d, mNavBarButtonColor);
            }
            v.setImageBitmap(ImageHelper.drawableToBitmap(d));
        }
        v.setRippleColor(mRippleColor);
        return v;
    }

    private KeyButtonView generateMenuKey(boolean landscape, int keyId) {
        Drawable d = null;
        KeyButtonView v = new KeyButtonView(mContext, null);
        int width = mContext.getResources().getDimensionPixelSize(R.dimen.navigation_extra_key_width);
        v.setLayoutParams(getLayoutParams(landscape, width));
        v.setScaleType(KeyButtonView.ScaleType.CENTER_INSIDE);
        if (keyId == KEY_MENU_LEFT || keyId == KEY_MENU_RIGHT) {
            v.setClickAction(ActionConstants.ACTION_MENU);
            v.setLongpressAction(ActionConstants.ACTION_NULL);
            if (keyId == KEY_MENU_LEFT) {
                v.setId(R.id.menu_left);
            } else {
                v.setId(R.id.menu);
            }
            v.setVisibility(View.GONE);
            v.setContentDescription(getResources().getString(R.string.accessibility_menu));
            d = mContext.getResources().getDrawable(R.drawable.ic_sysbar_menu);
        } else if (keyId == KEY_IME_LEFT) {
            v.setClickAction(ActionConstants.ACTION_IME_NAVIGATION_LEFT);
            v.setLongpressAction(ActionConstants.ACTION_IME_NAVIGATION_UP);
            v.setId(R.id.ime_left);
            v.setVisibility(View.GONE);
            d = mContext.getResources().getDrawable(R.drawable.ic_sysbar_ime_left);
        } else if (keyId == KEY_IME_RIGHT) {
            v.setClickAction(ActionConstants.ACTION_IME_NAVIGATION_RIGHT);
            v.setLongpressAction(ActionConstants.ACTION_IME_NAVIGATION_DOWN);
            v.setId(R.id.ime_right);
            v.setVisibility(View.GONE);
            d = mContext.getResources().getDrawable(R.drawable.ic_sysbar_ime_right);
        } else if (keyId == KEY_IME_SWITCHER) {
            v.setClickAction(ActionConstants.ACTION_IME);
            v.setId(R.id.ime_switcher);
            v.setVisibility(View.GONE);
            d = mContext.getResources().getDrawable(R.drawable.ic_ime_switcher_default);
        } else if (keyId == KEY_EMPTY_LEFT) {
            v.setId(R.id.empty_left);
            v.setVisibility(View.INVISIBLE);
        } else if (keyId == KEY_EMPTY_RIGHT) {
            v.setId(R.id.empty_right);
            v.setVisibility(View.INVISIBLE);
        }

        if (d != null) {
            d.mutate();
            if (mNavBarButtonColorMode != 3) {
                if (d instanceof VectorDrawable) {
                    d.setTint(mNavBarButtonColor);
                } else {
                    d = ImageHelper.getColoredDrawable(d, mNavBarButtonColor);
                }
            }
            v.setImageBitmap(ImageHelper.drawableToBitmap(d));
        }
        v.setRippleColor(mRippleColor);

        return v;
    }

    private int[] getAppIconPadding() {
        int[] padding = new int[4];
        // left
        padding[0] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
                .getDisplayMetrics());
        // top
        padding[1] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        // right
        padding[2] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
                .getDisplayMetrics());
        // bottom
        padding[3] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                getResources()
                        .getDisplayMetrics());
        return padding;
    }

    private LayoutParams getLayoutParams(boolean landscape, int dp) {
        float px = dp * getResources().getDisplayMetrics().density;
        return landscape ?
                new LayoutParams(LayoutParams.MATCH_PARENT, dp, 1f) :
                new LayoutParams(dp, LayoutParams.MATCH_PARENT, 1f);
    }

    private LayoutParams getSeparatorLayoutParams(boolean landscape) {
        float px = 25 * getResources().getDisplayMetrics().density;
        return landscape ?
                new LayoutParams(LayoutParams.MATCH_PARENT, (int) px) :
                new LayoutParams((int) px, LayoutParams.MATCH_PARENT);
    }

    private void addLightsOutButton(LinearLayout root, View v, boolean landscape, boolean empty) {
        ImageView addMe = new ImageView(mContext);
        addMe.setLayoutParams(v.getLayoutParams());
        addMe.setImageResource(empty ? R.drawable.ic_sysbar_lights_out_dot_large
                : R.drawable.ic_sysbar_lights_out_dot_small);
        addMe.setScaleType(ImageView.ScaleType.CENTER);
        addMe.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);

        if (landscape) {
            root.addView(addMe, 0);
        } else {
            root.addView(addMe);
        }
    }

    private void addButton(ViewGroup root, View addMe, boolean landscape) {
        if (landscape) {
            root.addView(addMe, 0);
        } else {
            root.addView(addMe);
        }
    }

    @Override
    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;
        final boolean backAlt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        if ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0 && !backAlt) {
            mTransitionListener.onBackAltCleared();
        }
        if (DEBUG) {
            android.widget.Toast.makeText(getContext(),
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        if (getBackButton() != null ) {
            ((ImageView) getBackButton()).setImageDrawable(null);
            ((ImageView) getBackButton()).setImageDrawable(mVertical ? mBackLandIcon : mBackIcon);
        }
        mBackLandIcon.setImeVisible(backAlt);
        mBackIcon.setImeVisible(backAlt);

        mIsImeButtonVisible = ((hints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) != 0
                    && !mImeArrowVisibility);
        mIsImeArrowVisible = (backAlt && mImeArrowVisibility);

        setDisabledFlags(mDisabledFlags, true);
    }

    @Override
    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0)
                && ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);
        final boolean keyguardProbablyEnabled =
                (mDisabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0;


        if (SLIPPERY_WHEN_DISABLED) {
            setSlippery(disableHome && disableRecent && disableBack);
        }

        final ViewGroup navButtons = getNavButtons();
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!lt.getTransitionListeners().contains(mTransitionListener)) {
                    lt.addTransitionListener(mTransitionListener);
                }
            }
        }
        if (inLockTask() && disableRecent && !disableHome) {
            // Don't hide recents when in lock task, it is used for exiting.
            // Unless home is hidden, then in DPM locked mode and no exit available.
            disableRecent = false;
        }

        if (mButtonsConfig != null && !mButtonsConfig.isEmpty()) {
            for (int j = 0; j < mButtonsConfig.size(); j++) {
                View v = (View) findViewWithTag((mVertical ? "key_land_" : "key_") + j);
                if (v != null) {
                    int vid = v.getId();
                    if (vid == R.id.back) {
                        v.setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
                    } else if (vid == R.id.recent_apps) {
                        v.setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);
                    } else { // treat all other buttons as same rule as home
                        v.setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
                    }
                }
            }
        }

        if (disableHome) {
            getLeftEmptyButton().setVisibility(View.INVISIBLE);
            getRightEmptyButton().setVisibility(View.INVISIBLE);
            getLeftMenuButton().setVisibility(View.GONE);
            getRightMenuButton().setVisibility(View.GONE);
            getLeftImeArrowButton().setVisibility(View.GONE);
            getRightImeArrowButton().setVisibility(View.GONE);
            getImeSwitchButton().setVisibility(View.GONE);
        } else {
            setMenuVisibility(mShowMenu, true);
        }

    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    private void setVisibleOrGone(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    public void setLayoutTransitionsEnabled(boolean enabled) {
        mLayoutTransitionsEnabled = enabled;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        mWakeAndUnlocking = wakeAndUnlocking;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean enabled = !mWakeAndUnlocking && mLayoutTransitionsEnabled;
        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        LayoutTransition lt = navButtons.getLayoutTransition();
        if (lt != null) {
            if (enabled) {
                lt.enableTransitionType(LayoutTransition.APPEARING);
                lt.enableTransitionType(LayoutTransition.DISAPPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            } else {
                lt.disableTransitionType(LayoutTransition.APPEARING);
                lt.disableTransitionType(LayoutTransition.DISAPPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            }
        }
    }

    private void setUseFadingAnimations(boolean useFadingAnimations) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean old = lp.windowAnimations != 0;
            if (!old && useFadingAnimations) {
                lp.windowAnimations = R.style.Animation_NavigationBarFadeIn;
            } else if (old && !useFadingAnimations) {
                lp.windowAnimations = 0;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & WindowManager.LayoutParams.FLAG_SLIPPERY) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    @Override
    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) {
            return;
        }

        View leftEmptyKeyView = getLeftEmptyButton();
        View rightEmptyKeyView = getRightEmptyButton();

        View leftMenuKeyView = getLeftMenuButton();
        View rightMenuKeyView = getRightMenuButton();

        View leftArrowKeyView = getLeftImeArrowButton();
        View rightArrowKeyView = getRightImeArrowButton();

        View imeSwitchKeyView = getImeSwitchButton();

        int leftEmptyKeyViewVisibility, rightEmptyKeyViewVisibility,
                leftMenuKeyViewVisibility, rightMenuKeyViewVisibility,
                leftArrowKeyViewVisibility, rightArrowKeyViewVisibility,
                imeSwitchKeyViewVisibility;

        // Only show Menu if IME switcher and IME arrowsnot shown.
        boolean showLeftMenuButton =
                (    (mMenuVisibility == MENU_VISIBILITY_ALWAYS || show)
                  && (mMenuSetting == SHOW_LEFT_MENU || mMenuSetting == SHOW_BOTH_MENU)
                  && (mMenuVisibility != MENU_VISIBILITY_NEVER)
                );

        boolean showRightMenuButton =
                (    (mMenuVisibility == MENU_VISIBILITY_ALWAYS || show)
                  && (mMenuSetting == SHOW_RIGHT_MENU || mMenuSetting == SHOW_BOTH_MENU)
                  && (mMenuVisibility != MENU_VISIBILITY_NEVER)
                );

        if(mIsImeArrowVisible) {
            leftEmptyKeyViewVisibility  = View.GONE;
            rightEmptyKeyViewVisibility = View.GONE;
            leftMenuKeyViewVisibility   = View.GONE;
            leftArrowKeyViewVisibility  = View.VISIBLE;
            rightMenuKeyViewVisibility  = View.GONE;
            rightArrowKeyViewVisibility = View.VISIBLE;
            imeSwitchKeyViewVisibility  = View.GONE;
        } else if (mOverrideMenuKeys) {
            leftEmptyKeyViewVisibility  = View.GONE;
            rightEmptyKeyViewVisibility = View.GONE;
            leftMenuKeyViewVisibility   = View.VISIBLE;
            leftArrowKeyViewVisibility  = View.GONE;
            rightMenuKeyViewVisibility  = View.VISIBLE;
            rightArrowKeyViewVisibility = View.GONE;
            imeSwitchKeyViewVisibility  = View.GONE;
        } else {
            if (showLeftMenuButton) {
                leftEmptyKeyViewVisibility  = View.GONE;
                leftMenuKeyViewVisibility   = View.VISIBLE;
                leftArrowKeyViewVisibility  = View.GONE;
            } else {
                leftEmptyKeyViewVisibility  = View.INVISIBLE;
                leftMenuKeyViewVisibility   = View.GONE;
                leftArrowKeyViewVisibility  = View.GONE;
            }
            if (showRightMenuButton) {
                rightEmptyKeyViewVisibility = View.GONE;
                rightMenuKeyViewVisibility  = View.VISIBLE;
                rightArrowKeyViewVisibility = View.GONE;
                imeSwitchKeyViewVisibility  = View.GONE;
            } else {
                if (mIsImeButtonVisible) {
                    rightEmptyKeyViewVisibility = View.GONE;
                    rightMenuKeyViewVisibility  = View.GONE;
                    rightArrowKeyViewVisibility = View.GONE;
                    imeSwitchKeyViewVisibility  = View.VISIBLE;
                } else {
                    rightEmptyKeyViewVisibility = View.INVISIBLE;
                    rightMenuKeyViewVisibility  = View.GONE;
                    rightArrowKeyViewVisibility = View.GONE;
                    imeSwitchKeyViewVisibility  = View.GONE;
                }
            }
        }

        // First remove what needs to be gone
        if (leftEmptyKeyViewVisibility == View.GONE) {
            leftEmptyKeyView.setVisibility(leftEmptyKeyViewVisibility);
        }
        if (rightEmptyKeyViewVisibility == View.GONE) {
            rightEmptyKeyView.setVisibility(rightEmptyKeyViewVisibility);
        }
        if (leftMenuKeyViewVisibility == View.GONE) {
            leftMenuKeyView.setVisibility(leftMenuKeyViewVisibility);
        }
        if (leftArrowKeyViewVisibility == View.GONE) {
            leftArrowKeyView.setVisibility(leftArrowKeyViewVisibility);
        }
        if (rightMenuKeyViewVisibility == View.GONE) {
            rightMenuKeyView.setVisibility(rightMenuKeyViewVisibility);
        }
        if (rightArrowKeyViewVisibility == View.GONE) {
            rightArrowKeyView.setVisibility(rightArrowKeyViewVisibility);
        }
        if (imeSwitchKeyViewVisibility == View.GONE) {
            imeSwitchKeyView.setVisibility(imeSwitchKeyViewVisibility);
        }

        // Second 'activate' what needs not to be gone
        if (leftEmptyKeyViewVisibility != View.GONE) {
            leftEmptyKeyView.setVisibility(leftEmptyKeyViewVisibility);
        }
        if (rightEmptyKeyViewVisibility != View.GONE) {
            rightEmptyKeyView.setVisibility(rightEmptyKeyViewVisibility);
        }
        if (leftMenuKeyViewVisibility != View.GONE) {
            leftMenuKeyView.setVisibility(leftMenuKeyViewVisibility);
        }
        if (leftArrowKeyViewVisibility != View.GONE) {
            leftArrowKeyView.setVisibility(leftArrowKeyViewVisibility);
        }
        if (rightMenuKeyViewVisibility != View.GONE) {
            rightMenuKeyView.setVisibility(rightMenuKeyViewVisibility);
        }
        if (rightArrowKeyViewVisibility != View.GONE) {
            rightArrowKeyView.setVisibility(rightArrowKeyViewVisibility);
        }
        if (imeSwitchKeyViewVisibility != View.GONE) {
            imeSwitchKeyView.setVisibility(imeSwitchKeyViewVisibility);
        }

        mShowMenu = show;
    }

    @Override
    public void onFinishInflate() {
        mRot0 = (FrameLayout) findViewById(R.id.rot0);
        mRot90 = (FrameLayout) findViewById(R.id.rot90);

        mRotatedViews[Surface.ROTATION_0] =
                mRotatedViews[Surface.ROTATION_180] = findViewById(R.id.rot0);
        mRotatedViews[Surface.ROTATION_90] = findViewById(R.id.rot90);

        mRotatedViews[Surface.ROTATION_270] = mRotatedViews[Surface.ROTATION_90];

        mCurrentView = mRotatedViews[Surface.ROTATION_0];
        updateSettings(true);

        if (getImeSwitchButton() != null)
            getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);

        final ViewGroup navButtons = getNavButtons();
        if (navButtons != null)
            navButtons.setOnTouchListener(mNavButtonsTouchListener);

        updateRTLOrder();
    }

    public boolean isVertical() {
        return mVertical;
    }

    public void setLeftInLandscape(boolean leftInLandscape) {
        mLeftInLandscape = leftInLandscape;
        mDeadZone.setStartFromRight(leftInLandscape);
    }

    public void reorient() {
        final int rot = mDisplay.getRotation();
        for (int i=0; i<4; i++) {
            mRotatedViews[i].setVisibility(View.GONE);
        }

        if (Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NAVIGATION_BAR_CAN_MOVE,
                DeviceUtils.isPhone(mContext) ? 1 : 0, UserHandle.USER_CURRENT) != 1) {
            mCurrentView = mRotatedViews[Surface.ROTATION_0];
        } else {
            mCurrentView = mRotatedViews[rot];
        }
        mCurrentView.setVisibility(View.VISIBLE);
        updateLayoutTransitionsEnabled();

        if (getImeSwitchButton() != null)
            getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);

        final ViewGroup navButtons = getNavButtons();
        if (navButtons != null)
            navButtons.setOnTouchListener(mNavButtonsTouchListener);

        mDeadZone = (DeadZone) mCurrentView.findViewById(R.id.deadzone);
        mDeadZone.setStartFromRight(mLeftInLandscape);

        // force the low profile & disabled states into compliance
        mBarTransitions.init();
        setDisabledFlags(mDisabledFlags, true /* force */);

        if (DEBUG) {
            Log.d(TAG, "reorient(): rot=" + mDisplay.getRotation());
        }

        updateTaskSwitchHelper();

        setNavigationIconHints(mNavigationIconHints, true);

        onNavButtonTouched();
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        mTaskSwitchHelper.setBarState(mVertical, isRtl);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        List<View> views = new ArrayList<View>();
        final View back = getBackButton();
        final View home = getHomeButton();
        final View recent = getRecentsButton();
        if (back != null) {
            views.add(back);
        }
        if (home != null) {
            views.add(home);
        }
        if (recent != null) {
            views.add(recent);
        }
        for (int i = 0; i < mButtonIdList.size(); i++) {
            final View customButton = getCustomButton(mButtonIdList.get(i));
            if (customButton != null) {
                views.add(customButton);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));

        final boolean newVertical = w > 0 && h > w;
        if (newVertical != mVertical) {
            mVertical = newVertical;
            //Log.v(TAG, String.format("onSizeChanged: h=%d, w=%d, vert=%s", h, w, mVertical?"y":"n"));
            reorient();
            notifyVerticalChangedListener(newVertical);
        }

        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (mOnVerticalChangedListener != null) {
            mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();
    }

    /**
     * In landscape, the LinearLayout is not auto mirrored since it is vertical. Therefore we
     * have to do it manually
     */
    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration()
                .getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mIsLayoutRtl != isLayoutRtl) {

            // We swap all children of the 90 and 270 degree layouts, since they are vertical
            View rotation90 = mRotatedViews[Surface.ROTATION_90];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));
            adjustExtraKeyGravity(rotation90, isLayoutRtl);

            View rotation270 = mRotatedViews[Surface.ROTATION_270];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
                adjustExtraKeyGravity(rotation270, isLayoutRtl);
            }
            mIsLayoutRtl = isLayoutRtl;
        }
    }

    private void adjustExtraKeyGravity(View navBar, boolean isLayoutRtl) {
        View menu = navBar.findViewById(R.id.menu);
        View imeSwitcher = navBar.findViewById(R.id.ime_switcher);

        /**
         * AOSP navbar places these views inside a FrameLayout, but slim's implementation
         * adds them to the LinearLayout, causing a ClassCastException for the parameters.
         * So, we need to determine which ViewGroup class the LayoutParams belongs to before
         * casting it to a subclass (FrameLayout.LayoutParams or LinearLayout.LayoutParams)
         */
        if (menu != null) {
            if (menu.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp =
                        (FrameLayout.LayoutParams) menu.getLayoutParams();
                lp.gravity = isLayoutRtl ? Gravity.BOTTOM : Gravity.TOP;
                menu.setLayoutParams(lp);
            } else {
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) menu.getLayoutParams();
                lp.gravity = isLayoutRtl ? Gravity.BOTTOM : Gravity.TOP;
                menu.setLayoutParams(lp);
            }
        }
        if (imeSwitcher != null) {
            if (imeSwitcher.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp =
                        (FrameLayout.LayoutParams) imeSwitcher.getLayoutParams();
                lp.gravity = isLayoutRtl ? Gravity.BOTTOM : Gravity.TOP;
                imeSwitcher.setLayoutParams(lp);
            } else {
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) imeSwitcher.getLayoutParams();
                lp.gravity = isLayoutRtl ? Gravity.BOTTOM : Gravity.TOP;
                imeSwitcher.setLayoutParams(lp);
            }
        }
    }

    /**
     * Swaps the children order of a LinearLayout if it's orientation is Vertical
     *
     * @param group The LinearLayout to swap the children from.
     */
    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) group;
            if (linearLayout.getOrientation() == VERTICAL) {
                int childCount = linearLayout.getChildCount();
                ArrayList<View> childList = new ArrayList<>(childCount);
                for (int i = 0; i < childCount; i++) {
                    childList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i = childCount - 1; i >= 0; i--) {
                    linearLayout.addView(childList.get(i));
                }
            }
        }
    }

    /*
    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onLayout: %s (%d,%d,%d,%d)",
                    changed?"changed":"notchanged", left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);
    }

    // uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT situations: if all else
    // fails, any touch on the display will fix the layout.
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: " + ev.toString());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            postCheckForInvalidLayout("touch");
        }
        return super.onInterceptTouchEvent(ev);
    }
    */


    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = getContext().getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    private void updateSettings(boolean recreate) {
        ContentResolver resolver = mContext.getContentResolver();

        mNavBarButtonColor = Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_BUTTON_TINT, -2, UserHandle.USER_CURRENT);

        if (mNavBarButtonColor == -2) {
            mNavBarButtonColor = mContext.getResources()
                    .getColor(R.color.navigationbar_button_default_color);
        }

        mNavBarButtonColorMode = Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0, UserHandle.USER_CURRENT);

        mButtonsConfig = ActionHelper.getNavBarConfig(mContext);

        mMenuSetting = Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_MENU_LOCATION, SHOW_RIGHT_MENU,
                UserHandle.USER_CURRENT);

        mMenuVisibility = Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_MENU_VISIBILITY, MENU_VISIBILITY_SYSTEM,
                UserHandle.USER_CURRENT);

        getIcons(getContext().getResources());

        mImeArrowVisibility = (Settings.System.getIntForUser(resolver,
                Settings.System.NAVIGATION_BAR_SHOW_IME_ARROWS, HIDE_IME_ARROW,
                UserHandle.USER_CURRENT) == SHOW_IME_ARROW);

        mDimNavButtons = (Settings.System.getIntForUser(resolver,
                Settings.System.DIM_NAV_BUTTONS, 0,
                UserHandle.USER_CURRENT) == 1);

        mDimNavButtonsTimeout = Settings.System.getIntForUser(resolver,
                Settings.System.DIM_NAV_BUTTONS_TIMEOUT, 3000,
                UserHandle.USER_CURRENT);

        mDimNavButtonsAlpha = (float) Settings.System.getIntForUser(resolver,
                Settings.System.DIM_NAV_BUTTONS_ALPHA, 50,
                UserHandle.USER_CURRENT) / 100.0f;

        mDimNavButtonsAnimate = (Settings.System.getIntForUser(resolver,
                Settings.System.DIM_NAV_BUTTONS_ANIMATE, 0,
                UserHandle.USER_CURRENT) == 1);

        mDimNavButtonsAnimateDuration = Settings.System.getIntForUser(resolver,
                Settings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION, 2000,
                UserHandle.USER_CURRENT);

        mDimNavButtonsTouchAnywhere = (Settings.System.getIntForUser(resolver,
                Settings.System.DIM_NAV_BUTTONS_TOUCH_ANYWHERE, 0,
                UserHandle.USER_CURRENT) == 1);

        String expDeskString = Settings.Global.getStringForUser(resolver,
                Settings.Global.POLICY_CONTROL, UserHandle.USER_CURRENT);
        mIsExpandedDesktopOn = (expDeskString != null ?
                expDeskString.equals("immersive.full=*") : false);

        setNavigationIconHints(mNavigationIconHints, true);

        // construct the navigationbar
        if (recreate) {
            makeBar();
        }
    }

    public void setForgroundColor(Drawable drawable) {
        if (mRot0 != null) {
            mRot0.setForeground(drawable);
        }
        if (mRot90 != null) {
            mRot90.setForeground(drawable);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        final Rect r = new Rect();
        final Point size = new Point();
        mDisplay.getRealSize(size);

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                        + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: "
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                        getResourceName(mCurrentView.getId()),
                        mCurrentView.getWidth(), mCurrentView.getHeight(),
                        visibilityToString(mCurrentView.getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        final View back = getBackButton();
        final View home = getHomeButton();
        final View recent = getRecentsButton();
        final View rightMenu = getRightMenuButton();
        final View leftMenu = getLeftMenuButton();

        if (back != null)
            dumpButton(pw, "back", back);

        if (home != null)
            dumpButton(pw, "home", home);

        if (recent != null)
            dumpButton(pw, "rcnt", recent);

        if (rightMenu != null)
            dumpButton(pw, "rightMenu", rightMenu);

        if (leftMenu != null)
            dumpButton(pw, "leftMenu", leftMenu);

        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(PhoneStatusBar.viewInfo(button)
                    + " " + visibilityToString(button.getVisibility())
                    + " alpha=" + button.getAlpha()
                    );
        }
        pw.println();
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean isVertical);
    }

    private Runnable mNavButtonDimmer = new Runnable() {
        @Override
        public void run() {
            mIsHandlerCallbackActive = false;
            final ViewGroup navButtons = getNavButtons();
            if (navButtons != null && !mIsDim) {
                mIsDim = true;
                if (mDimNavButtonsAnimate) {
                    mFadeOut = ObjectAnimator.ofFloat(
                            navButtons, "alpha", mOriginalAlpha, mDimNavButtonsAlpha);
                    mFadeOut.setInterpolator(new AccelerateInterpolator());
                    mFadeOut.setDuration(mDimNavButtonsAnimateDuration);
                    mFadeOut.setFrameDelay(100);
                    mFadeOut.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (mIsAnimating) {
                                mIsAnimating = false;
                            }
                            mFadeOut.removeAllListeners();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mFadeOut.removeAllListeners();
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }

                        @Override
                        public void onAnimationStart(Animator animation) {
                            mIsAnimating = true;
                        }
                    });
                    mFadeOut.start();
                } else {
                    navButtons.setAlpha(mDimNavButtonsAlpha);
                }
            }
        }
    };
}
