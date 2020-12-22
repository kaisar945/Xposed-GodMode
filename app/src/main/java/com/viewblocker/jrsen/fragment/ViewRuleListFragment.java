package com.viewblocker.jrsen.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.SettingsActivity;
import com.viewblocker.jrsen.adapter.AdapterDataObserver;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.util.FileUtils;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.Preconditions;
import com.viewblocker.jrsen.util.RuleHelper;
import com.viewblocker.jrsen.widget.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.viewblocker.jrsen.injection.util.CommonUtils.recycleBitmap;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleListFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, AdapterDataObserver<ViewRule>, LoaderManager.LoaderCallbacks<List<ViewRule>> {

    private static final int RULE_LIST_LOADER_ID = 0x01;
    private static final int EXPORT_TASK_LOADER_ID = 0x02;

    private static final String EXTRA_RULE = "extra_rule";
    private final List<ViewRule> viewRules = new ArrayList<>();

    private Drawable icon;
    private CharSequence label;
    private CharSequence packageName;

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setLabel(CharSequence label) {
        this.label = label;
    }

    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.pref_empty);
        LoaderManager.getInstance(this).initLoader(RULE_LIST_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_app_rule);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        for (ViewRule viewRule : viewRules) {
            if (Preconditions.checkBitmap(viewRule.thumbnail)) {
                recycleBitmap(viewRule.thumbnail);
            }
            if (Preconditions.checkBitmap(viewRule.snapshot)) {
                recycleBitmap(viewRule.snapshot);
            }
        }
        LoaderManager.getInstance(this).destroyLoader(RULE_LIST_LOADER_ID);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        ViewRule viewRule = preference.getExtras().getParcelable(EXTRA_RULE);
        ViewRuleDetailsContainerFragment fragment = new ViewRuleDetailsContainerFragment();
        fragment.setAdapterDataObserver(this);
        fragment.setCurIndex(viewRules.indexOf(viewRule));
        fragment.setIcon(icon);
        fragment.setLabel(label);
        fragment.setPackageName(packageName);
        SettingsActivity activity = (SettingsActivity) requireActivity();
        activity.startPreferenceFragment(fragment, true);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_app_rules, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_purge_all_rules) {
            if (GodModeManager.getDefault().deleteRules(packageName.toString())) {
                getPreferenceScreen().removeAll();
                requireActivity().onBackPressed();
            } else {
                Snackbar.make(requireActivity(), R.string.snack_bar_msg_revert_rule_fail, Snackbar.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.menu_export_all_rules) {
            LoaderManager.getInstance(this).initLoader(EXPORT_TASK_LOADER_ID, null, new LoaderManager.LoaderCallbacks<String>() {
                @NonNull
                @Override
                public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
                    return new ExportTaskLoader(requireContext(), viewRules);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<String> loader, String filepath) {
                    Snackbar.make(requireActivity(), FileUtils.exists(filepath)
                            ? getString(R.string.export_successful, filepath)
                            : getString(R.string.export_failed), Snackbar.LENGTH_LONG).show();
                    LoaderManager.getInstance(ViewRuleListFragment.this).destroyLoader(EXPORT_TASK_LOADER_ID);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<String> loader) {

                }
            }).forceLoad();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getSize() {
        return viewRules.size();
    }

    @Override
    public ViewRule getItem(int position) {
        return viewRules.get(position);
    }

    @Override
    public void onItemRemoved(int position) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference preference = preferenceScreen.getPreference(position);
        preferenceScreen.removePreference(preference);
    }

    @Override
    public void onItemChanged(int position) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference preference = preferenceScreen.getPreference(position);
        if (position < viewRules.size()) {
            updatePreference(preference, viewRules.get(position));
        }
    }

    private void updatePreference(Preference preference, ViewRule viewRule) {
        if (Preconditions.checkBitmap(viewRule.thumbnail)) {
            preference.setIcon(new BitmapDrawable(getResources(), viewRule.thumbnail));
        } else {
            preference.setIcon(icon);
        }
        String activitySimpleName = viewRule.activityClass != null
                ? viewRule.activityClass.substring(viewRule.activityClass.lastIndexOf('.') + 1) : "Unknown";
        preference.setTitle(getString(R.string.field_activity, activitySimpleName));
        SpannableStringBuilder summaryBuilder = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(viewRule.alias)) {
            SpannableString ss = new SpannableString(getString(R.string.field_rule_alias, viewRule.alias));
            ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.alias)), 0, ss.length(), 0);
            summaryBuilder.append(ss);
        }
        summaryBuilder.append(getString(R.string.field_view, viewRule.viewClass));
        preference.setSummary(summaryBuilder);
    }

    @Override
    public Loader<List<ViewRule>> onCreateLoader(int id, Bundle args) {
        return new RuleListLoader(getContext(), packageName);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<ViewRule>> loader, List<ViewRule> data) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        viewRules.clear();
        preferenceScreen.removeAll();
        viewRules.addAll(data);
        for (ViewRule viewRule : data) {
            Preference preference = new Preference(preferenceScreen.getContext());
            updatePreference(preference, viewRule);
            Bundle extras = preference.getExtras();
            extras.putParcelable(EXTRA_RULE, viewRule);
            preference.setOnPreferenceClickListener(this);
            preferenceScreen.addPreference(preference);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<ViewRule>> loader) {

    }

    private static final class RuleListLoader extends AsyncTaskLoader<List<ViewRule>> {

        private final CharSequence packageName;
        private final ProgressDialog dialog;

        public RuleListLoader(Context context, CharSequence packageName) {
            super(context);
            this.packageName = packageName;
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(getContext().getString(R.string.dialog_loading));
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog.show();
        }

        @Override
        public List<ViewRule> loadInBackground() {
            GodModeManager manager = GodModeManager.getDefault();
            ActRules actRules = manager.getRules(packageName.toString());
            ArrayList<ViewRule> viewRules = new ArrayList<>();
            for (List<ViewRule> values : actRules.values()) {
                viewRules.addAll(values);
            }
            Collections.sort(viewRules, Collections.reverseOrder(new Comparator<ViewRule>() {
                @Override
                public int compare(ViewRule o1, ViewRule o2) {
                    return Long.compare(o1.timestamp, o2.timestamp);
                }
            }));
            for (ViewRule viewRule : viewRules) {
                try {
                    ParcelFileDescriptor parcelFileDescriptor = manager.openFile(viewRule.imagePath, ParcelFileDescriptor.MODE_READ_ONLY);
                    Preconditions.checkNotNull(parcelFileDescriptor);
                    try {
                        Bitmap snapshot = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor.getFileDescriptor());
                        Bitmap thumbnail = Bitmap.createBitmap(snapshot, viewRule.x, viewRule.y, viewRule.width, viewRule.height);
                        viewRule.snapshot = snapshot;
                        viewRule.thumbnail = thumbnail;
                    } finally {
                        parcelFileDescriptor.close();
                    }
                } catch (Exception ignored) {
                }
            }
            return viewRules;
        }

        @Override
        public void deliverResult(List<ViewRule> data) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (isReset()) {
                releaseResources(data);
            }
            super.deliverResult(data);
        }

        private void releaseResources(List<ViewRule> data) {
            for (ViewRule rule : data) {
                recycleBitmap(rule.snapshot);
                recycleBitmap(rule.thumbnail);
            }
        }
    }

    private static final class ExportTaskLoader extends AsyncTaskLoader<String> {

        private final List<ViewRule> viewRules;
        private final ProgressDialog dialog;


        public ExportTaskLoader(@NonNull Context context, List<ViewRule> viewRules) {
            super(context);
            this.viewRules = viewRules;
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(context.getResources().getString(R.string.dialog_message_export));
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog.show();
        }

        @Nullable
        @Override
        public String loadInBackground() {
            return RuleHelper.exportRules(viewRules.toArray(new ViewRule[0]));
        }

        @Override
        public void deliverResult(String filepath) {
            super.deliverResult(filepath);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }

}
