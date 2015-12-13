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

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class QSUtils {
    private static boolean sAvailableTilesFiltered;
    private static final SparseArray<Context> sSystemUiContextForUser = new SparseArray<>();

    public interface OnQSChanged {
        void onQSChanged();
    }

    private QSUtils() {}

    public static List<String> getAvailableTiles(Context context) {
        filterTiles(context);
        return QSConstants.TILES_AVAILABLE;
    }

    public static List<String> getDefaultTiles(Context context) {
        final List<String> tiles = new ArrayList<>();
        final String defaults = context.getString(
                com.android.internal.R.string.config_defaultQuickSettingsTiles);
        if (!TextUtils.isEmpty(defaults)) {
            final String[] array = TextUtils.split(defaults, Pattern.quote(","));
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                tiles.add(item);
            }
            filterTiles(context, tiles);
        }
        return tiles;
    }

    public static String getDefaultTilesAsString(Context context) {
        List<String> list = getDefaultTiles(context);
        return TextUtils.join(",", list);
    }

    private static void filterTiles(Context context, List<String> tiles) {
        boolean deviceSupportsMobile = deviceSupportsMobileData(context);

        // Tiles that need conditional filtering
        Iterator<String> iterator = tiles.iterator();
        while (iterator.hasNext()) {
            String tileKey = iterator.next();
            boolean removeTile = false;
            switch (tileKey) {
                case QSConstants.TILE_CELLULAR:
                case QSConstants.TILE_HOTSPOT:
                case QSConstants.TILE_ROAMING:
                case QSConstants.TILE_APN:
                    removeTile = !deviceSupportsMobile;
                    break;
                case QSConstants.TILE_CAST:
                    removeTile = !QsDeviceUtils.deviceSupportsRemoteDisplay(context);
                    break;
                case QSConstants.TILE_DATA:
                    removeTile = !QsDeviceUtils.deviceSupportsMobileData(context);
                    break;
                case QSConstants.TILE_FLASHLIGHT:
                    removeTile = !QsDeviceUtils.deviceSupportsFlashLight(context);
                    break;
                case QSConstants.TILE_BLUETOOTH:
                    removeTile = !QsDeviceUtils.deviceSupportsBluetooth();
                    break;
                case QSConstants.TILE_NFC:
                    removeTile = !QsDeviceUtils.deviceSupportsNfc(context);
                    break;
                case QSConstants.TILE_AMBIENT_DISPLAY:
                    removeTile = !QsDeviceUtils.isDozeAvailable(context);
                    break;
            }
            if (removeTile) {
                iterator.remove();
            }
        }
    }

    private static void filterTiles(Context context) {
        if (!sAvailableTilesFiltered) {
            filterTiles(context, QSConstants.TILES_AVAILABLE);
            sAvailableTilesFiltered = true;
        }
    }

    private static boolean deviceSupportsDdsSupported(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isMultiSimEnabled()
                && tm.getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDA;
    }

    public static boolean deviceSupportsMobileData(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean deviceSupportsBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public static boolean deviceSupportsFlashLight(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null
                        && flashAvailable
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            // Ignore
        }
        return false;
    }

    public static Context getQSTileContext(Context context, int userId) {
        Context ctx = sSystemUiContextForUser.get(userId);
        if (ctx == null) {
            try {
                ctx = context.createPackageContextAsUser(
                        "com.android.systemui", 0, new UserHandle(userId));
                sSystemUiContextForUser.put(userId, ctx);
            } catch (NameNotFoundException ex) {
                // We can safely ignore this
            }
        }
        return ctx;
    }

    public static boolean isQSTileEnabledForUser(
            Context context, String tileSpec, int userId) {
        final ContentResolver resolver = context.getContentResolver();
        String order = Settings.System.getStringForUser(resolver,
                Settings.System.QS_TILES, userId);
        return !TextUtils.isEmpty(order) && Arrays.asList(order.split(",")).contains(tileSpec);
    }

    public static ContentObserver registerObserverForQSChanges(Context ctx, final OnQSChanged cb) {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                cb.onQSChanged();
            }
        };

        ctx.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.QS_TILES),
                false, observer, UserHandle.USER_ALL);
        return observer;
    }

    public static void unregisterObserverForQSChanges(Context ctx, ContentObserver observer) {
        ctx.getContentResolver().unregisterContentObserver(observer);
    }

}
