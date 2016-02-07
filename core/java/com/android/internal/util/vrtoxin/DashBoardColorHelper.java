/*
* Copyright (C) 2016 The VRToxin Project
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
import android.content.res.ColorStateList;
import android.provider.Settings;

public class DashBoardColorHelper {

    private static final int WHITE = 0xffffffff;
    private static final int BLACK = 0xff000000;
    private static final int TRANSLUCENT_BLACK = 0x7a000000;
    private static final int VRTOXIN_BLUE = 0xff1976D2;

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SETTINGS_BG_COLOR, WHITE);
    }

    public static int getTitleTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SETTINGS_TITLE_TEXT_COLOR,
                BLACK);
    }

    public static int getCategoryTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SETTINGS_CATEGORY_TEXT_COLOR,
                BLACK);
    }

    public static int getIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SETTINGS_ICON_COLOR,
                WHITE);
    }
}
