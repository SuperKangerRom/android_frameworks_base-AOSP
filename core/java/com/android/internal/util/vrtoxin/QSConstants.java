/*
 * Copyright (C) 2015 The CyanogenMod Open Source Project
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
 * limitations under the License
 */

package com.android.internal.util.vrtoxin;

import java.util.ArrayList;

public class QSConstants {
    private QSConstants() {}

    public static final String TILE_WIFI = "wifi";
    public static final String TILE_BLUETOOTH = "bt";
    public static final String TILE_INVERSION = "inversion";
    public static final String TILE_CELLULAR = "cell";
    public static final String TILE_AIRPLANE = "airplane";
    public static final String TILE_ROTATION = "rotation";
    public static final String TILE_FLASHLIGHT = "flashlight";
    public static final String TILE_LOCATION = "location";
    public static final String TILE_CAST = "cast";
    public static final String TILE_HOTSPOT = "hotspot";
    public static final String TILE_NOTIFICATIONS = "notifications";
    public static final String TILE_DATA = "data";
    public static final String TILE_ROAMING = "roaming";
    public static final String TILE_DDS = "dds";
    public static final String TILE_APN = "apn";
    public static final String TILE_VRTOXIN = "vrtoxin_settings";
    public static final String TILE_SCREENSHOT = "screenshot";
    public static final String TILE_BRIGHTNESS = "brightness";
    public static final String TILE_SCREENOFF = "screen_off";
    public static final String TILE_VOLUME = "volume";
    public static final String TILE_EXPANDED_DESKTOP = "expanded_desktop";
    public static final String TILE_SCREEN_TIMEOUT = "screen_timeout";

    // Order matters
    protected static final ArrayList<String> TILES_DEFAULT = new ArrayList<String>();

    static {
        TILES_DEFAULT.add(TILE_WIFI);
        TILES_DEFAULT.add(TILE_BLUETOOTH);
        TILES_DEFAULT.add(TILE_CELLULAR);
        TILES_DEFAULT.add(TILE_AIRPLANE);
        TILES_DEFAULT.add(TILE_ROTATION);
        TILES_DEFAULT.add(TILE_FLASHLIGHT);
        TILES_DEFAULT.add(TILE_LOCATION);
        TILES_DEFAULT.add(TILE_CAST);
    }

    protected static final ArrayList<String> TILES_AVAILABLE = new ArrayList<String>();

    static {
        TILES_AVAILABLE.addAll(TILES_DEFAULT);
        TILES_AVAILABLE.add(TILE_INVERSION);
        TILES_AVAILABLE.add(TILE_HOTSPOT);
        TILES_AVAILABLE.add(TILE_NOTIFICATIONS);
        TILES_AVAILABLE.add(TILE_DATA);
        TILES_AVAILABLE.add(TILE_ROAMING);
        TILES_AVAILABLE.add(TILE_DDS);
        TILES_AVAILABLE.add(TILE_APN);
        TILES_AVAILABLE.add(TILE_VRTOXIN);
        TILES_AVAILABLE.add(TILE_SCREENOFF);
        TILES_AVAILABLE.add(TILE_SCREENSHOT);
        TILES_AVAILABLE.add(TILE_BRIGHTNESS);
        TILES_AVAILABLE.add(TILE_VOLUME);
        TILES_AVAILABLE.add(TILE_EXPANDED_DESKTOP);
        TILES_AVAILABLE.add(TILE_SCREEN_TIMEOUT);
    }
}
