package com.viewblocker.jrsen.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.view.View;

import com.viewblocker.jrsen.BlockerApplication;
import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.adapter.AdapterDataObserver;
import com.viewblocker.jrsen.preference.ImagePreviewPreference;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.service.InjectBridgeService;
import com.viewblocker.jrsen.util.Preconditions;

import java.util.Arrays;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleDetailsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ALIAS = "alias";
    private static final String KEY_VISIBILITY = "visibility";

    private int index;
    private Drawable icon;
    private CharSequence label;
    private CharSequence packageName;
    private ViewRule viewRule;
    private Bitmap snapshot;
    private AdapterDataObserver<ViewRule> dataObserver;

    public void setIndex(int index) {
        this.index = index;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setLabel(CharSequence label) {
        this.label = label;
    }

    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    public void setViewRule(ViewRule viewRule) {
        this.viewRule = viewRule;
    }

    public void setDataObserver(AdapterDataObserver<ViewRule> dataObserver) {
        this.dataObserver = dataObserver;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_empty);
        android.support.v7.preference.PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = preferenceScreen.getContext();
        Preference headerPreference = new Preference(context);
        headerPreference.setSelectable(false);
        headerPreference.setIcon(icon);
        headerPreference.setTitle(label);
        headerPreference.setSummary(packageName);
        preferenceScreen.addPreference(headerPreference);

        Preference preference = new Preference(context);
        preference.setSummary("活动:" + (viewRule.activityClassName != null ? viewRule.activityClassName : "Unknown"));
        preferenceScreen.addPreference(preference);

        preference = new EditTextPreference(context);
        preference.setKey(KEY_ALIAS);
        preference.setSummary("控件别名:" + (viewRule.alias != null ? viewRule.alias : "未命名"));
        preference.setOnPreferenceChangeListener(this);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((EditTextPreference) preference).setText(viewRule.alias);
                return false;
            }
        });
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        Rect bounds = new Rect(viewRule.x, viewRule.y, viewRule.x + viewRule.width, viewRule.y + viewRule.height);
        preference.setSummary("控件边界:" + bounds.toShortString());
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setSummary("控件类型:" + viewRule.viewClassName);
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setSummary("控件布局深度:" + Arrays.toString(viewRule.viewHierarchyDepth));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setSummary("控件资源名称:" + viewRule.resourceName);
        preferenceScreen.addPreference(preference);

        ListPreference listPreference = new ListPreference(context);
        listPreference.setKey(KEY_VISIBILITY);
        listPreference.setOnPreferenceChangeListener(this);
        preference = listPreference;
        listPreference.setDialogTitle("控件可见性");
        CharSequence[] entries = {"占位", "不占位"};
        listPreference.setEntries(entries);
        listPreference.setEntryValues(new CharSequence[]{String.valueOf(View.INVISIBLE), String.valueOf(View.GONE)});
        listPreference.setDefaultValue(String.valueOf(viewRule.visibility));
        listPreference.setSummary("控件可见性:" + (viewRule.visibility == View.INVISIBLE ? entries[0] : entries[1]));
        preferenceScreen.addPreference(preference);

        boolean hasSnapshot = !TextUtils.isEmpty(viewRule.snapshotFilePath);
        preference = new Preference(context);
        preference.setSummary(hasSnapshot ? "控件预览:" : "该控件没有预览图");
        preferenceScreen.addPreference(preference);

        if (hasSnapshot) {
            preference = new ImagePreviewPreference(context);
            snapshot = BitmapFactory.decodeFile(viewRule.snapshotFilePath);
            preference.setDefaultValue(snapshot);
            preferenceScreen.addPreference(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.title_rule_details);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Preconditions.checkBitmap(snapshot)) {
            snapshot.recycle();
        }
        dataObserver = null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String s = preference.getKey();
        InjectBridgeService service = InjectBridgeService.getBridge(BlockerApplication.getApplication());
        if (KEY_ALIAS.equals(s)) {
            preference.setSummary("控件别名:" + newValue);
            viewRule.alias = (String) newValue;
            service.update(preference.getContext(), packageName.toString(), viewRule);
            dataObserver.onItemChanged(index);
            return false;
        } else if (KEY_VISIBILITY.equals(s)) {
            ListPreference listPreference = (ListPreference) preference;
            CharSequence[] entries = listPreference.getEntries();
            int origVisibility = viewRule.visibility;
            viewRule.visibility = Integer.valueOf((String) newValue);
            if (!service.update(preference.getContext(), packageName.toString(), viewRule)) {
                viewRule.visibility = origVisibility;
            }
            listPreference.setValue(String.valueOf(viewRule.visibility));
            listPreference.setSummary("控件可见性:" + (viewRule.visibility == View.INVISIBLE ? entries[0] : entries[1]));
            dataObserver.onItemChanged(index);
        }
        return true;
    }

}
