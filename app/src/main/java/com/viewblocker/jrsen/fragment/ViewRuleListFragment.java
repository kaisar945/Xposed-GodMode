package com.viewblocker.jrsen.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.viewblocker.jrsen.BlockerApplication;
import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.SettingsActivity;
import com.viewblocker.jrsen.adapter.AdapterDataObserver;
import com.viewblocker.jrsen.injection.util.FileUtils;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.service.InjectBridgeService;
import com.viewblocker.jrsen.util.Preconditions;
import com.viewblocker.jrsen.util.ViewRuleFactory;
import com.viewblocker.jrsen.util.ZipUtils;
import com.viewblocker.jrsen.widget.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleListFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, AdapterDataObserver<ViewRule>, LoaderManager.LoaderCallbacks<List<ViewRule>> {

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
        getActivity().getSupportLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.title_app_rule);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getSupportLoaderManager().destroyLoader(1);
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
        SettingsActivity activity = (SettingsActivity) getActivity();
        activity.startPreferenceFragment(fragment, true);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_app_rules, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_purge_all_rules) {
            InjectBridgeService service = InjectBridgeService.getBridge(BlockerApplication.getApplication());
            if (service.purgeRulesByPackage(packageName.toString())) {
                getPreferenceScreen().removeAll();
                getActivity().onBackPressed();
            } else {
                Snackbar.make(getActivity(), R.string.snack_bar_msg_revert_rule_fail, Snackbar.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.menu_export_all_rules) {
            new ExportAsyncTask().execute();
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
        String activitySimpleName = viewRule.activityClassName != null
                ? viewRule.activityClassName.substring(viewRule.activityClassName.lastIndexOf('.') + 1) : "Unknown";
        preference.setTitle(getString(R.string.field_activity, activitySimpleName));
        SpannableStringBuilder summaryBuilder = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(viewRule.alias)) {
            SpannableString ss = new SpannableString(getString(R.string.field_rule_alias, viewRule.alias));
            ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.alias)), 0, ss.length(), 0);
            summaryBuilder.append(ss);
        }
        summaryBuilder.append(getString(R.string.field_view, viewRule.viewClassName));
        preference.setSummary(summaryBuilder);
    }

    @Override
    public Loader<List<ViewRule>> onCreateLoader(int id, Bundle args) {
        return new RuleListLoader(getContext(), packageName.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<ViewRule>> loader, List<ViewRule> data) {
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
    public void onLoaderReset(Loader<List<ViewRule>> loader) {

    }

    private static final class RuleListLoader extends AsyncTaskLoader<List<ViewRule>> {

        private String packageName;
        private ProgressDialog dialog;

        public RuleListLoader(Context context, String packageName) {
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
            InjectBridgeService bridge = InjectBridgeService.getBridge(getContext());
            ActRules actRules = bridge.getRules(packageName);
            ArrayList<ViewRule> viewRules = new ArrayList<>();
            for (List<ViewRule> values : actRules.values()) {
                viewRules.addAll(values);
            }
            Collections.sort(viewRules, Collections.reverseOrder(new Comparator<ViewRule>() {
                @Override
                public int compare(ViewRule o1, ViewRule o2) {
                    return Long.compare(o1.recordTimeStamp, o2.recordTimeStamp);
                }
            }));
            for (ViewRule viewRule : viewRules) {
                try {
                    String snapshotFilePath = Preconditions.checkStringNotEmpty(viewRule.snapshotFilePath);
                    Bitmap snapshot = BitmapFactory.decodeFile(snapshotFilePath);
                    Bitmap thumbnail = Bitmap.createBitmap(snapshot, viewRule.x, viewRule.y, viewRule.width, viewRule.height);
                    if (thumbnail != snapshot) {
                        snapshot.recycle();
                    }
                    viewRule.thumbnail = thumbnail;
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
                if (Preconditions.checkBitmap(rule.thumbnail)) {
                    rule.thumbnail.recycle();
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ExportAsyncTask extends AsyncTask<Void, Integer, Object[]> {

        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle(getResources().getString(R.string.dialog_title_export, label));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setMax(viewRules.size());
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
        }

        @Override
        protected Object[] doInBackground(Void... voids) {
            File godModeDir = Environment.getExternalStoragePublicDirectory("GodMode");
            File cacheDir = new File(godModeDir, ".cache");
            FileUtils.delete(cacheDir);
            if (!cacheDir.exists() && cacheDir.mkdirs()) {
                final int N = viewRules.size();
                int exportCount = 0;
                for (int i = 0; i < N; i++) {
                    ViewRule viewRule = viewRules.get(i);
                    Bitmap viewRuleImage = ViewRuleFactory.encodeViewRuleAsQRImage(packageName.toString(), viewRule);
                    if (viewRuleImage != null) {
                        try {
                            FileOutputStream out = new FileOutputStream(new File(cacheDir,
                                    System.currentTimeMillis() + File.separatorChar + ".webp"));
                            if (viewRuleImage.compress(Bitmap.CompressFormat.WEBP, 50, out)) {
                                exportCount++;
                            }
                            out.close();
                            viewRuleImage.recycle();
                        } catch (IOException e) {
                        }
                    }
                    publishProgress(i + 1);
                }
                String zipFile = new File(godModeDir, packageName + ".gm").getAbsolutePath();
                if (FileUtils.exists(zipFile)) FileUtils.delete(zipFile);
                File[] files = cacheDir.listFiles();
                String[] filepaths = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    filepaths[i] = files[i].getAbsolutePath();
                }
                boolean successful = ZipUtils.compress(zipFile, filepaths);
                FileUtils.delete(cacheDir);
                return new Object[]{successful, exportCount, N - exportCount, zipFile};
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Object[] result) {
            super.onPostExecute(result);
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            boolean successful = (boolean) result[0];
            int exportCount = (int) result[1];
            int unexportCount = (int) result[2];
            String filePath = (String) result[3];
            Snackbar.make(getActivity(), successful
                    ? getString(R.string.export_successful, exportCount, unexportCount, filePath)
                    : getString(R.string.export_failed), Snackbar.LENGTH_LONG).show();
        }
    }

}
