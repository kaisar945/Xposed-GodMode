package com.kaisar.xposed.godmode.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.preference.ImageViewPreference;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;
import static com.kaisar.xposed.godmode.injection.util.CommonUtils.recycleNullableBitmap;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleDetailsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, LoaderManager.LoaderCallbacks<Bitmap> {

    private static final String KEY_ALIAS = "alias";
    private static final String KEY_VISIBILITY = "visibility";

    private Drawable mIcon;
    private CharSequence mLabel;
    private CharSequence mPackageName;
    private ViewRule mViewRule;
    private Bitmap mImageBitmap;

    private SharedViewModel mSharedViewModel;

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public void setLabel(CharSequence label) {
        mLabel = label;
    }

    public void setPackageName(CharSequence packageName) {
        mPackageName = packageName;
    }

    public void setViewRule(ViewRule viewRule) {
        mViewRule = viewRule;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_empty);
        androidx.preference.PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = preferenceScreen.getContext();
        Preference headerPreference = new Preference(context);
        headerPreference.setSelectable(false);
        headerPreference.setIcon(mIcon);
        headerPreference.setTitle(mLabel);
        headerPreference.setSummary(mPackageName);
        preferenceScreen.addPreference(headerPreference);

        Preference preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_create_time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        preference.setSummary(dateFormat.format(new Date(mViewRule.timestamp)));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_generate_version);
        preference.setSummary(String.format(Locale.getDefault(), "%s %s", mLabel, mViewRule.matchVersionName));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_activity);
        preference.setSummary(Preconditions.optionDefault(mViewRule.activityClass, "None"));
        preferenceScreen.addPreference(preference);

        EditTextPreference aliasEditTextPreference = new EditTextPreference(context);
        aliasEditTextPreference.setKey(KEY_ALIAS);
        aliasEditTextPreference.setTitle(R.string.rule_details_field_alias);
        aliasEditTextPreference.setDialogTitle(R.string.rule_details_set_alias);
        aliasEditTextPreference.setSummary(Preconditions.optionDefault(mViewRule.alias, getString(R.string.rule_details_set_alias)));
        aliasEditTextPreference.setPersistent(false);
        aliasEditTextPreference.setOnPreferenceChangeListener(this);
        aliasEditTextPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((EditTextPreference) preference).setText(mViewRule.alias);
                return false;
            }
        });
        preference = aliasEditTextPreference;
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        Rect bounds = new Rect(mViewRule.x, mViewRule.y, mViewRule.x + mViewRule.width, mViewRule.y + mViewRule.height);
        preference.setTitle(R.string.rule_details_field_view_bounds);
        preference.setSummary(bounds.toShortString());
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_view_type);
        preference.setSummary(mViewRule.viewClass);
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_view_depth);
        preference.setSummary(Arrays.toString(mViewRule.depth));
        preferenceScreen.addPreference(preference);

        preference = new Preference(context);
        preference.setTitle(R.string.rule_details_field_res_name);
        preference.setSummary(mViewRule.resourceName);
        preferenceScreen.addPreference(preference);

        if (!TextUtils.isEmpty(mViewRule.text)) {
            preference = new Preference(context);
            preference.setTitle(R.string.rule_details_field_text);
            preference.setSummary(mViewRule.text);
            preferenceScreen.addPreference(preference);
        }
        if (!TextUtils.isEmpty(mViewRule.description)) {
            preference = new Preference(context);
            preference.setTitle(R.string.rule_details_field_description);
            preference.setSummary(mViewRule.description);
            preferenceScreen.addPreference(preference);
        }

        DropDownPreference dropDownPreference = new DropDownPreference(context);
        dropDownPreference.setPersistent(false);
        dropDownPreference.setKey(KEY_VISIBILITY);
        dropDownPreference.setOnPreferenceChangeListener(this);
        dropDownPreference.setTitle(R.string.rule_details_field_visibility);
        CharSequence[] entries = {getString(R.string.rule_details_invisible), getString(R.string.rule_details_gone)};
        dropDownPreference.setSummary("%s");
        dropDownPreference.setEntries(entries);
        dropDownPreference.setEntryValues(new CharSequence[]{String.valueOf(View.INVISIBLE), String.valueOf(View.GONE)});
        dropDownPreference.setValue(String.valueOf(mViewRule.visibility));
        preference = dropDownPreference;
        preferenceScreen.addPreference(preference);

        if (!TextUtils.isEmpty(mViewRule.imagePath)) {
            LoaderManager.getInstance(this).initLoader(0, null, this).forceLoad();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewRule = null;
        recycleNullableBitmap(mImageBitmap);
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_ALIAS.equals(key)) {
            mViewRule.alias = (String) newValue;
            preference.setSummary(mViewRule.alias);
            mSharedViewModel.updateRule(mViewRule);
        } else if (KEY_VISIBILITY.equals(key)) {
            int newVisibility = Integer.parseInt((String) newValue);
            if (newVisibility != mViewRule.visibility) {
                mViewRule.visibility = newVisibility;
                mSharedViewModel.updateRule(mViewRule);
            }
        }
        return true;
    }

    @NonNull
    @Override
    public Loader<Bitmap> onCreateLoader(int id, @Nullable Bundle args) {
        return new ImageLoader(requireContext(), mViewRule);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Bitmap> loader, Bitmap data) {
        if (data != null) {
            mImageBitmap = data;
            ImageViewPreference imageViewPreference = new ImageViewPreference(getContext());
            imageViewPreference.setImageBitmap(data);
            getPreferenceScreen().addPreference(imageViewPreference);
        }
        LoaderManager.getInstance(this).destroyLoader(0);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Bitmap> loader) {
    }

    static final class ImageLoader extends AsyncTaskLoader<Bitmap> {

        final ViewRule viewRule;

        public ImageLoader(@NonNull Context context, ViewRule viewRule) {
            super(context);
            this.viewRule = viewRule;
        }

        @Nullable
        @Override
        public Bitmap loadInBackground() {
            Bitmap snapshot = viewRule.snapshot;
            if (snapshot == null && !TextUtils.isEmpty(viewRule.imagePath)) {
                ParcelFileDescriptor parcelFileDescriptor = GodModeManager.getDefault().openImageFileDescriptor(viewRule.imagePath);
                if (parcelFileDescriptor != null) {
                    try {
                        try {
                            snapshot = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor.getFileDescriptor()).copy(Bitmap.Config.ARGB_8888, true);
                        } finally {
                            parcelFileDescriptor.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (snapshot != null) {
                Bitmap copySnapshot = snapshot.copy(snapshot.getConfig(), true);
                Paint markPaint = new Paint();
                markPaint.setColor(Color.RED);
                markPaint.setAlpha(100);
                Canvas canvas = new Canvas(copySnapshot);
                canvas.drawRect(viewRule.x, viewRule.y, viewRule.x + viewRule.width, viewRule.y + viewRule.height, markPaint);
                return copySnapshot;
            }
            return null;
        }
    }
}
