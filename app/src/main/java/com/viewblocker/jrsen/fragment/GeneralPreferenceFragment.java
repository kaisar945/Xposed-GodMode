package com.viewblocker.jrsen.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.viewblocker.jrsen.BuildConfig;
import com.viewblocker.jrsen.QuickSettingsCompatService;
import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.SettingsActivity;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.injection.util.FileUtils;
import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.DonateHelper;
import com.viewblocker.jrsen.util.QRCodeFactory;
import com.viewblocker.jrsen.util.XposedEnvironment;
import com.viewblocker.jrsen.util.ZipUtils;
import com.viewblocker.jrsen.widget.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;


/**
 * Created by jrsen on 17-10-29.
 */

public final class GeneralPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, LoaderManager.LoaderCallbacks<HashMap<String, ActRules>> {

    private static final String SETTING_PREFS = "settings";
    private static final String KEY_VERSION_CODE = "version_code";

    public static final String KEY_EDITOR_SWITCH = "editor_switch";
    public static final String KEY_QUICK_SETTING = "quick_setting";
    public static final String KEY_ADD_QQ_GROUP = "group_qrcode";
    public static final String KEY_DONATE = "donate";
    public static final String KEY_APP_RULES = "app_rules";


    private static final int REQUEST_PICK_FILE = 1;
    private SwitchPreferenceCompat mEditorSwitchPreference;
    private CheckBoxPreference mQuickSettingPreference;
    private Preference mJoinQQGroupPreference;
    private Preference mDonatePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.pref_general);
        mEditorSwitchPreference = (SwitchPreferenceCompat) findPreference(KEY_EDITOR_SWITCH);
        mEditorSwitchPreference.setOnPreferenceChangeListener(this);
        mQuickSettingPreference = (CheckBoxPreference) findPreference(KEY_QUICK_SETTING);
        mQuickSettingPreference.setOnPreferenceChangeListener(this);
        mJoinQQGroupPreference = findPreference(KEY_ADD_QQ_GROUP);
        mJoinQQGroupPreference.setOnPreferenceClickListener(this);
        mDonatePreference = findPreference(KEY_DONATE);
        mDonatePreference.setOnPreferenceClickListener(this);
        PreferenceManager.getDefaultSharedPreferences(
                mEditorSwitchPreference.getContext()).registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sp = requireActivity().getSharedPreferences(SETTING_PREFS, Context.MODE_PRIVATE);
        int previousVersionCode = sp.getInt(KEY_VERSION_CODE, 0);
        if (previousVersionCode != BuildConfig.VERSION_CODE) {
            showUpdatePolicyDialog();
            sp.edit().putInt(KEY_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
        } else if (!XposedEnvironment.isModuleActive(getContext())) {
            requestEnableModuleDialog();
        }
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.app_name);
        LoaderManager.getInstance(this).getLoader(0).onContentChanged();
    }

    @Override
    public Loader<HashMap<String, ActRules>> onCreateLoader(int id, Bundle args) {
        return new DataLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<HashMap<String, ActRules>> loader, HashMap<String, ActRules> rules) {
        PreferenceCategory category = (PreferenceCategory) findPreference(KEY_APP_RULES);
        category.removeAll();
        if (rules != null) {
            PackageManager pm = requireContext().getPackageManager();
            for (String packageName : rules.keySet()) {
                Drawable icon;
                CharSequence label;
                try {
                    ApplicationInfo aInfo = pm.getApplicationInfo(packageName, 0);
                    icon = aInfo.loadIcon(pm);
                    label = aInfo.loadLabel(pm);
                } catch (PackageManager.NameNotFoundException ignore) {
                    icon = getResources().getDrawable(R.mipmap.ic_god);
                    label = packageName;
                }
                Preference preference = new Preference(category.getContext());
                preference.setIcon(icon);
                preference.setTitle(label);
                preference.setSummary(packageName);
                preference.setKey(packageName);
                preference.setOnPreferenceClickListener(this);
                category.addPreference(preference);
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<HashMap<String, ActRules>> loader) {
    }

    private static final class DataLoader extends AsyncTaskLoader<HashMap<String, ActRules>> {

        public DataLoader(Context context) {
            super(context);
        }

        @Override
        public HashMap<String, ActRules> loadInBackground() {
            return (HashMap<String, ActRules>) GodModeManager.getDefault().getAllRules();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(
                mEditorSwitchPreference.getContext()).unregisterOnSharedPreferenceChangeListener(this);
        LoaderManager.getInstance(this).destroyLoader(0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEditorSwitchPreference == preference) {
            boolean enable = (boolean) newValue;
            GodModeManager.getDefault().setEditMode(enable);
//            Snackbar.make(getActivity(), R.string.toast_tip_force_stop, Snackbar.LENGTH_LONG).show();
        } else if (mQuickSettingPreference == preference) {
            boolean enable = (boolean) newValue;
            QuickSettingsCompatService.setComponentState(preference.getContext(), enable);
            Intent intent = new Intent(preference.getContext(), QuickSettingsCompatService.class);
            if (enable) {
                preference.getContext().startService(intent);
            } else {
                preference.getContext().stopService(intent);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mJoinQQGroupPreference == preference) {
            showJoinGroupDialog();
        } else if (mDonatePreference == preference) {
            DonateHelper.showDonateDialog(getContext());
        } else {
            ViewRuleListFragment fragment = new ViewRuleListFragment();
            fragment.setIcon(preference.getIcon());
            fragment.setLabel(preference.getTitle());
            fragment.setPackageName(preference.getSummary());
            SettingsActivity activity = (SettingsActivity) requireActivity();
            activity.startPreferenceFragment(fragment, true);
        }
        return true;
    }

    private void requestEnableModuleDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.hey_guy)
                .setMessage(R.string.not_active_module)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.go_setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XposedEnvironment.XposedType xposedType = XposedEnvironment.checkXposedType(requireContext());
                        if (xposedType != XposedEnvironment.XposedType.UNKNOWN) {
                            Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage(xposedType.PACKAGE_NAME);
                            startActivity(launchIntent);
                        } else {
                            Snackbar.make(requireActivity(), R.string.not_found_xp_installer, Snackbar.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void showUpdatePolicyDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.welcome_title)
                .setMessage("\t我想你可以试试用上帝模式屏蔽一些令人烦恼的按钮。噢！没错这听起来很有趣。不过这个小家伙有时会像汤姆家那只该死的倔驴一样不听话，别担心伙计们，我会尽快修理好的！\n\n" +
                        "\t伙计们玩的开心记得支持一下你们的老伙计 非常感谢！" +
                        "噢 上帝啊 看看我到底在说些什么我想我应该安静一下...")
                .setPositiveButton(R.string.dialog_btn_alipay, null)
                .setNegativeButton(R.string.dialog_btn_wxpay, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface di) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DonateHelper.startAliPayDonate(v.getContext());
                    }
                });
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DonateHelper.startWxPayDonate(v.getContext());
                    }
                });
            }
        });
        dialog.show();
    }

    private void showJoinGroupDialog() {
        new AlertDialog.Builder(requireContext())
                .setItems(R.array.qq_groups_name, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String[] links = getResources().getStringArray(R.array.qq_groups_link);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(links[which]));
                            startActivity(intent);
                        } catch (Exception ignore) {
                        }
                    }
                }).show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (TextUtils.equals(key, KEY_EDITOR_SWITCH)) {
            mEditorSwitchPreference.setChecked(sp.getBoolean(key, false));
            sp.getBoolean(key, false);
        } else if (TextUtils.equals(key, KEY_QUICK_SETTING)) {
            mQuickSettingPreference.setChecked(sp.getBoolean(key, false));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_general, menu);
        MenuItem item = menu.findItem(R.id.menu_icon_switch);
        Context context = requireContext();
        PackageManager pm = context.getPackageManager();
        ComponentName cmp = new ComponentName(context.getPackageName(), "com.viewblocker.jrsen.SettingsAliasActivity");
        boolean enable = pm.getComponentEnabledSetting(cmp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        item.setTitle(enable ? R.string.menu_icon_switch_hide : R.string.menu_icon_switch_show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_import_view_rule) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_PICK_FILE);
        } else if (item.getItemId() == R.id.menu_icon_switch) {
            Context context = requireContext();
            PackageManager pm = context.getPackageManager();
            ComponentName cmp = new ComponentName(context.getPackageName(), "com.viewblocker.jrsen.SettingsAliasActivity");
            boolean enable = pm.getComponentEnabledSetting(cmp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(cmp, enable ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            enable = pm.getComponentEnabledSetting(cmp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            item.setTitle(enable ? R.string.menu_icon_switch_hide : R.string.menu_icon_switch_show);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK) {
            handleIncomingIntent(data.getData());
        }
    }

    public void dispatchOnNewIntent(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        handleIncomingIntent(uri);
    }

    private void handleIncomingIntent(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")
                || fileName.endsWith(".webp")) {
            handleImportViewRule(uri, false);
        } else if (fileName.endsWith(".gm")) {
            handleImportViewRule(uri, true);
        } else {
            Snackbar.make(requireActivity(), R.string.import_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleImportViewRule(Uri uri, boolean batchOperation) {
        new ImportAsyncTask().execute(uri, batchOperation);
    }

    private String getFileName(Uri uri) {
        if (uri != null) {
            String uriStr = uri.toString();
            if (uriStr.startsWith("file:")) {
                return new File(uri.getPath()).getName();
            } else if (uriStr.startsWith("content:")) {
                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    String fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    cursor.close();
                    return fileName;
                }
            }
        }
        return "";
    }

    private final class ImportAsyncTask extends AsyncTask<Object, Integer, Boolean> {

        ProgressDialog mProgressDialog;

        @Override
        protected Boolean doInBackground(Object[] args) {
            Uri uri = (Uri) args[0];
            boolean batchOperation = (boolean) args[1];
            if (batchOperation) {
                File cacheDir = null;
                try {
                    Context context = getActivity();
                    cacheDir = new File(context.getExternalCacheDir(), "import-batch-cache");
                    if (cacheDir.exists()) return null;//正在导入规则
                    InputStream in = context.getContentResolver().openInputStream(uri);
                    File dst = new File(cacheDir, System.currentTimeMillis() + "");
                    if (!dst.exists()) dst.getParentFile().mkdirs();
                    if (FileUtils.copy(in, dst)) {
                        boolean successful = ZipUtils.uncompress(dst.getAbsolutePath(), dst.getParent());
                        if (successful) {
                            File[] files = cacheDir.listFiles(new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.endsWith(".webp");
                                }
                            });
                            publishProgress(files.length);
                            for (int i = 0; i < files.length; i++) {
                                processImportViewRule(Uri.fromFile(files[i]));
                                publishProgress(i + 1);
                            }
                            return true;
                        }
                    }
                } catch (FileNotFoundException ignore) {
                    return false;
                } finally {
                    if (cacheDir != null)
                        FileUtils.delete(cacheDir);
                }
            } else {
                publishProgress(1);
                boolean successful = processImportViewRule(uri);
                publishProgress(1);
                return successful;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setTitle(R.string.dialog_title_import);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setMax(values[0]);
                mProgressDialog.setProgress(0);
                mProgressDialog.show();
            } else {
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean successful) {
            super.onPostExecute(successful);
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            if (successful) {
                Snackbar.make(getActivity(), R.string.import_success, Snackbar.LENGTH_LONG).show();
                getActivity().getSupportLoaderManager().getLoader(0).onContentChanged();
            } else {
                Snackbar.make(getActivity(), R.string.import_failed, Snackbar.LENGTH_LONG).show();
            }
        }

        private boolean processImportViewRule(Uri imgUri) {
            try {
                Bitmap fullImage = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imgUri);
                Bitmap splitImage = null;
                try {
                    Object[] array = Objects.requireNonNull(QRCodeFactory.decode(fullImage));
                    ViewRule viewRule = (ViewRule) array[0];
                    splitImage = (Bitmap) array[1];
                    GodModeManager.getDefault().writeRule(viewRule.packageName, viewRule, splitImage);
                    return true;
                } finally {
                    fullImage.recycle();
                    if (splitImage != null)
                        splitImage.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

}
