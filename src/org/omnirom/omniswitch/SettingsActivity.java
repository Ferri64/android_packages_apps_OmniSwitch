/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omniswitch;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.omnirom.omniswitch.ui.CheckboxListDialog;
import org.omnirom.omniswitch.ui.DragHandleColorPreference;
import org.omnirom.omniswitch.ui.FavoriteDialog;
import org.omnirom.omniswitch.ui.SeekBarPreference;
import org.omnirom.omniswitch.ui.SettingsGestureView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener  {
    private static final String TAG = "SettingsActivity";

    public static final String PREF_OPACITY = "opacity";
    public static final String PREF_ANIMATE = "animate";
    public static final String PREF_START_ON_BOOT = "start_on_boot";
    public static final String PREF_ICON_SIZE = "icon_size";
    public static final String PREF_DRAG_HANDLE_LOCATION = "drag_handle_location_new";
    private static final String PREF_ADJUST_HANDLE = "adjust_handle";
    public static final String PREF_DRAG_HANDLE_COLOR = "drag_handle_color";
    public static final String PREF_DRAG_HANDLE_OPACITY = "drag_handle_opacity";
    public static final String PREF_SHOW_RAMBAR = "show_rambar";
    public static final String PREF_SHOW_LABELS = "show_labels";
    public static final String PREF_FAVORITE_APPS_CONFIG = "favorite_apps_config";
    public static final String PREF_FAVORITE_APPS = "favorite_apps";
    public static final String PREF_HANDLE_POS_START_RELATIVE = "handle_pos_start_relative";
    public static final String PREF_HANDLE_HEIGHT = "handle_height";
    public static final String PREF_BUTTON_CONFIG = "button_config";
    public static final String PREF_BUTTONS = "buttons";
    public static final String PREF_BUTTON_DEFAULT = "1,1,1,1,1";
    public static final String PREF_AUTO_HIDE_HANDLE = "auto_hide_handle";
    public static final String PREF_DRAG_HANDLE_ENABLE = "drag_handle_enable";
    public static final String PREF_ENABLE = "enable";
    
    public static int BUTTON_KILL_ALL = 0;
    public static int BUTTON_KILL_OTHER = 1;
    public static int BUTTON_TOGGLE_APP = 2;
    public static int BUTTON_HOME = 3;
    public static int BUTTON_SETTINGS = 4;

    private ListPreference mIconSize;
    private SeekBarPreference mOpacity;
    private Preference mFavoriteAppsConfig;
    private Preference mAdjustHandle;
    private static List<String> sFavoriteList = new ArrayList<String>();
    private static SharedPreferences sPrefs;
    private SettingsGestureView mGestureView;
    private FavoriteDialog mManageAppDialog;
    private Preference mButtonConfig;
    private String[] mButtonEntries;
    private Drawable[] mButtonImages;
    private String mButtons;
    private SeekBarPreference mDragHandleOpacity;
    private SwitchPreference mDragHandleEnable;
    private CheckBoxPreference mDragHandleAutoHide;
    private DragHandleColorPreference mDragHandleColor;

    private Switch mToggleServiceSwitch;

    @Override
    public void onPause() {
        if (mGestureView != null) {
            mGestureView.hide();
        }
        if (mManageAppDialog != null) {
            mManageAppDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.recents_settings);

        mIconSize = (ListPreference) findPreference(PREF_ICON_SIZE);
        mIconSize.setOnPreferenceChangeListener(this);
        List<CharSequence> values = Arrays.asList(mIconSize.getEntryValues());
        int idx = values.indexOf(sPrefs.getString(PREF_ICON_SIZE,
                mIconSize.getEntryValues()[1].toString()));
        if(idx == -1){
            idx = 1;
        }
        mIconSize.setValueIndex(idx);
        mIconSize.setSummary(mIconSize.getEntries()[idx]);

        mOpacity = (SeekBarPreference) findPreference(PREF_OPACITY);
        mOpacity.setInitValue(sPrefs.getInt(PREF_OPACITY, 60));
        mOpacity.setOnPreferenceChangeListener(this);

        mDragHandleOpacity = (SeekBarPreference) findPreference(PREF_DRAG_HANDLE_OPACITY);
        mDragHandleOpacity.setInitValue(sPrefs.getInt(PREF_DRAG_HANDLE_OPACITY, 100));
        mDragHandleOpacity.setOnPreferenceChangeListener(this);

        mAdjustHandle = (Preference) findPreference(PREF_ADJUST_HANDLE);
        mButtonConfig = (Preference) findPreference(PREF_BUTTON_CONFIG);
        initButtons();
        mButtons = sPrefs.getString(PREF_BUTTONS, PREF_BUTTON_DEFAULT);
        
        mFavoriteAppsConfig = (Preference) findPreference(PREF_FAVORITE_APPS_CONFIG);
        String favoriteListString = sPrefs.getString(PREF_FAVORITE_APPS, "");
        sFavoriteList.clear();
        Utils.parseFavorites(favoriteListString, sFavoriteList);
        removeUninstalledFavorites(this);
        
        mDragHandleAutoHide = (CheckBoxPreference) findPreference(PREF_AUTO_HIDE_HANDLE);
        mDragHandleEnable = (SwitchPreference) findPreference(PREF_DRAG_HANDLE_ENABLE);
        mDragHandleEnable.setOnPreferenceChangeListener(this);
        mDragHandleColor = (DragHandleColorPreference) findPreference(PREF_DRAG_HANDLE_COLOR);
        
        updateDragHandleEnablement(mDragHandleEnable.isChecked());
    }
    
    private void updateDragHandleEnablement(Boolean value) {
        boolean dragHandleEnable = value.booleanValue();
        mAdjustHandle.setEnabled(dragHandleEnable);
        mDragHandleOpacity.setEnabled(dragHandleEnable);
        mDragHandleAutoHide.setEnabled(dragHandleEnable);
        mDragHandleColor.setEnabled(dragHandleEnable);
    }

    private class ButtonsApplyRunnable implements CheckboxListDialog.ApplyRunnable {
        public void apply(boolean[] buttons) {
            mButtons = Utils.buttonArrayToString(buttons);
            sPrefs.edit().putString(PREF_BUTTONS, mButtons).commit();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mAdjustHandle) {
            mGestureView = new SettingsGestureView(this);
            mGestureView.show();
            return true;
        } else if (preference == mFavoriteAppsConfig) {
            showManageAppDialog();
            return true;
        } else if (preference == mButtonConfig){
            boolean[] buttons = Utils.buttonStringToArry(mButtons);
            CheckboxListDialog dialog = new CheckboxListDialog(this,
                    mButtonEntries, mButtonImages, buttons, new ButtonsApplyRunnable(),
                    getResources().getString(R.string.buttons_title));
            dialog.show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mIconSize) {
            String value = (String) newValue;
            List<CharSequence> values = Arrays.asList(mIconSize
                    .getEntryValues());
            int idx = values.indexOf(value);
            mIconSize.setSummary(mIconSize.getEntries()[idx]);
            mIconSize.setValueIndex(idx);
            return true;
        } else if (preference == mOpacity) {
            float val = Float.parseFloat((String) newValue);
            sPrefs.edit().putInt(PREF_OPACITY, (int) val).commit();
            return true;
        } else if (preference == mDragHandleOpacity) {
            float val = Float.parseFloat((String) newValue);
            sPrefs.edit().putInt(PREF_DRAG_HANDLE_OPACITY, (int) val).commit();
            return true;
        } else if (preference == mDragHandleEnable) {
            updateDragHandleEnablement((Boolean) newValue);
            return true;
        }

        return false;
    }

    private void showManageAppDialog() {
        if (mManageAppDialog != null && mManageAppDialog.isShowing()) {
            return;
        }

        List<String> favoriteList = new ArrayList<String>();
        favoriteList.addAll(sFavoriteList);
        mManageAppDialog = new FavoriteDialog(this, favoriteList);
        mManageAppDialog.show();
    }

    public static void removeUninstalledFavorites(final Context context) {
        Log.d(TAG, "" + sFavoriteList);
        final PackageManager pm = context.getPackageManager();
        boolean changed = false;
        List<String> newFavoriteList = new ArrayList<String>();
        Iterator<String> nextFavorite = sFavoriteList.iterator();
        while (nextFavorite.hasNext()) {
            String favorite = nextFavorite.next();
            Intent intent = null;
            try {
                intent = Intent.parseUri(favorite, 0);
                pm.getActivityIcon(intent);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "NameNotFoundException: [" + favorite + "]");
                changed = true;
                continue;
            } catch (URISyntaxException e) {
                Log.e(TAG, "URISyntaxException: [" + favorite + "]");
                changed = true;
                continue;
            }
            newFavoriteList.add(favorite);
        }
        if (changed) {
            sFavoriteList.clear();
            sFavoriteList.addAll(newFavoriteList);
            sPrefs.edit()
                    .putString(PREF_FAVORITE_APPS, Utils.flattenFavorites(sFavoriteList))
                    .commit();
        }
    }
    
    public void applyChanges(List<String> favoriteList){
        sFavoriteList.clear();
        sFavoriteList.addAll(favoriteList);
        sPrefs.edit()
                .putString(PREF_FAVORITE_APPS,
                        Utils.flattenFavorites(sFavoriteList))
                .commit();
    }
    
    private void initButtons(){
        mButtonEntries = getResources().getStringArray(R.array.button_entries);
        mButtonImages = new Drawable[mButtonEntries.length];
        mButtonImages[0]=Utils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.kill_all));
        mButtonImages[1]=Utils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.kill_other));
        mButtonImages[2]=Utils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.lastapp));
        mButtonImages[3]=Utils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.home));
        mButtonImages[4]=Utils.colorize(getResources(), Color.GRAY, getResources().getDrawable(R.drawable.settings));
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // dont restart activity on orientation changes
        if (mGestureView != null && mGestureView.isShowing()){
            mGestureView.handleRotation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        mToggleServiceSwitch = (Switch) menu.findItem(R.id.toggle_service).getActionView().findViewById(R.id.switch_item);
        mToggleServiceSwitch.setChecked(SwitchService.isRunning());
        mToggleServiceSwitch.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                boolean value = ((Switch)v).isChecked();
                Intent svc = new Intent(SettingsActivity.this, SwitchService.class);
                if (value) {
                    Intent killRecent = new Intent(
                            SwitchService.RecentsReceiver.ACTION_KILL_ACTIVITY);
                    sendBroadcast(killRecent);

                    startService(svc);
                } else {
                    Intent killRecent = new Intent(
                            SwitchService.RecentsReceiver.ACTION_KILL_ACTIVITY);
                    sendBroadcast(killRecent);
                }
                sPrefs.edit().putBoolean(PREF_ENABLE, value).commit();
            }});
        return true;
    }
}
