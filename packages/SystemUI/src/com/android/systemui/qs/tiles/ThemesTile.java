/*
 * Copyright (C) 2015 VRToxin Project
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThemesTile extends QSTile<QSTile.BooleanState> {
    private boolean mListening;
    private ThemesObserver mObserver;

    public ThemesTile(Host host) {
        super(host);
        mObserver = new ThemesObserver(mHandler);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$ThemeSettingsSettingsActivity");
        mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$DashboardSettingsActivity");
        mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        state.icon = ResourceIcon.get(R.drawable.ic_settings_themes_alpha);
        state.label = mContext.getString(R.string.quick_settings_themes_label);

	}

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

    private class ThemesObserver extends ContentObserver {
        public ThemesObserver(Handler handler) {
            super(handler);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.VRTOXIN_QS_CONSTANTS;
    }
}

