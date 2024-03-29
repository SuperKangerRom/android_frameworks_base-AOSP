/*
* Copyright (C) 2015 DarkKat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.internal.util.vrtoxin;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;

public class StatusBarColorHelper {

    private static final int WHITE = 0xffffffff;
    private static final int BLACK = 0xff000000;
    private static final int TRANSLUCENT_BLACK = 0x7a000000;

    public static int getGreetingColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_GREETING_COLOR, WHITE);
    }

    public static int getGreetingColorDark(Context context) {
        final int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_GREETING_COLOR_DARK_MODE,
                TRANSLUCENT_BLACK);
        return (153 << 24) | (color & 0x00ffffff);
    }

    public static int getBatteryColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_BATTERY_COLOR, WHITE);
    }

    public static int getBatteryColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_BATTERY_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getBatteryTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR, WHITE);
    }

    public static int getBatteryTextColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNetworkSignalColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_SIGNAL_COLOR, WHITE);
    }

    public static int getNetworkSignalColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_SIGNAL_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNoSimColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_NO_SIM_COLOR, WHITE);
    }

    public static int getNoSimColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_NO_SIM_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getAirplaneModeColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_AIRPLANE_MODE_COLOR, WHITE);
    }

    public static int getAirplaneModeColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_ICONS_AIRPLANE_MODE_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getStatusIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_STATUS_ICONS_COLOR, WHITE);
    }

    public static int getStatusIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_STATUS_ICONS_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getNotificationIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIFICATION_ICONS_COLOR, WHITE);
    }

    public static int getNotificationIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIFICATION_ICONS_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getTickerTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_TICKER_TEXT_COLOR, WHITE);
    }

    public static int getTickerTextColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_TICKER_TEXT_COLOR_DARK_MODE,
                BLACK);
    }

    public static int getTickerIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_TICKER_ICON_COLOR, WHITE);
    }

    public static int getTickerIconColorDark(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_TICKER_ICON_COLOR_DARK_MODE,
                BLACK);
    }

    /*public static int getNotifCountIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT_ICON_COLOR, WHITE);
    }

    public static int getNotifCountIconDarkColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT_ICON_COLOR_DARK_MODE, BLACK);
    }

    public static int getNotifCountTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT_TEXT_COLOR,
                BLACK);
    }

    public static int getNotifCountTextDarkColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT_TEXT_COLOR_DARK_MODE,
                WHITE);
    }*/
}
