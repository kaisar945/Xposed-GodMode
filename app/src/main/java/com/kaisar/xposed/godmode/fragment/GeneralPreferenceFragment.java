package com.kaisar.xposed.godmode.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.QuickSettingsCompatService;
import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.SettingsActivity;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.util.DonateHelper;
import com.kaisar.xposed.godmode.util.PermissionHelper;
import com.kaisar.xposed.godmode.util.RuleHelper;
import com.kaisar.xposed.godmode.util.XposedEnvironment;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;


/**
 * Created by jrsen on 17-10-29.
 */

public final class GeneralPreferenceFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, LoaderManager.LoaderCallbacks<Void> {

    private static final int LIST_LOADER_ID = 0x01;
    private static final int IMPORT_LOADER_ID = 0x02;

    private static final String SETTING_PREFS = "settings";
    private static final String KEY_VERSION_CODE = "version_code";

    public static final String KEY_EDITOR_SWITCH = "editor_switch";
    public static final String KEY_QUICK_SETTING = "quick_setting";
    public static final String KEY_JOIN_GROUP = "join_group";
    public static final String KEY_DONATE = "donate";
    public static final String KEY_APP_RULES = "app_rules";


    private SwitchPreferenceCompat mEditorSwitchPreference;
    private CheckBoxPreference mQuickSettingPreference;
    private Preference mJoinGroupPreference;
    private Preference mDonatePreference;

    private ActivityResultLauncher<String> mPickFileLauncher;
    private SharedViewModel mSharedViewModel;

    private Logger mLogger;

    private final IObserver mRuleObserver = new IObserver.Stub() {
        @Override
        public void onEditModeChanged(boolean enable) {
            // Don't care
        }

        @Override
        public void onViewRuleChanged(String packageName, ActRules actRules) {
            mLogger.d("远端规则变化");
            mSharedViewModel.reloadAppRules();
            if (TextUtils.equals(packageName, mSharedViewModel.getSelectedPackage().getValue())) {
                mLogger.d("更新选中列表");
                mSharedViewModel.updateSelectedPackage(packageName);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mLogger = Logger.getLogger("流程");
        setHasOptionsMenu(true);
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mSharedViewModel.getAppRules().observe(this, new Observer<Map<String, ActRules>>() {
            @Override
            public void onChanged(Map<String, ActRules> result) {
//                mLogger.d("更新主页面列表:" + (result != null ? result.size() : result));
                if (result != null) {
                    Set<Map.Entry<String, ActRules>> entrySet = result.entrySet();
                    PreferenceCategory category = (PreferenceCategory) findPreference(KEY_APP_RULES);
                    category.removeAll();
                    PackageManager pm = requireContext().getPackageManager();
                    for (Map.Entry<String, ActRules> entry : entrySet) {
                        String packageName = entry.getKey();
                        Drawable icon;
                        CharSequence label;
                        try {
                            ApplicationInfo aInfo = pm.getApplicationInfo(packageName, 0);
                            icon = aInfo.loadIcon(pm);
                            label = aInfo.loadLabel(pm);
                        } catch (PackageManager.NameNotFoundException ignore) {
                            icon = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_god, requireContext().getTheme());
                            label = packageName;
                        }
                        Preference preference = new Preference(category.getContext());
                        preference.setIcon(icon);
                        preference.setTitle(label);
                        preference.setSummary(packageName);
                        preference.setKey(packageName);
                        preference.setOnPreferenceClickListener(GeneralPreferenceFragment.this);
                        category.addPreference(preference);
                    }
                }
            }
        });
        mPickFileLauncher = requireActivity().registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(final Uri result) {
                LoaderManager.getInstance(GeneralPreferenceFragment.this).initLoader(IMPORT_LOADER_ID, null, new LoaderManager.LoaderCallbacks<Boolean>() {
                    @NonNull
                    @Override
                    public Loader<Boolean> onCreateLoader(int id, @Nullable Bundle args) {
                        return new ImportTaskLoader(requireContext(), result);
                    }

                    @Override
                    public void onLoadFinished(@NonNull Loader<Boolean> loader, Boolean ok) {
                        if (ok) {
                            mSharedViewModel.reloadAppRules();
                            Snackbar.make(requireActivity(), R.string.import_success, Snackbar.LENGTH_LONG).show();
                        } else {
                            Snackbar.make(requireActivity(), R.string.import_failed, Snackbar.LENGTH_LONG).show();
                        }
                        LoaderManager.getInstance(GeneralPreferenceFragment.this).destroyLoader(IMPORT_LOADER_ID);
                    }

                    @Override
                    public void onLoaderReset(@NonNull Loader<Boolean> loader) {
                    }
                }).forceLoad();
            }
        });
        GodModeManager.getDefault().addObserver("*", mRuleObserver);
        LoaderManager.getInstance(this).initLoader(LIST_LOADER_ID, null, this).onContentChanged();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_general);
        mEditorSwitchPreference = (SwitchPreferenceCompat) findPreference(KEY_EDITOR_SWITCH);
        mEditorSwitchPreference.setChecked(GodModeManager.getDefault().isInEditMode());
        mEditorSwitchPreference.setOnPreferenceChangeListener(this);
        mQuickSettingPreference = (CheckBoxPreference) findPreference(KEY_QUICK_SETTING);
        mQuickSettingPreference.setOnPreferenceChangeListener(this);
        mJoinGroupPreference = findPreference(KEY_JOIN_GROUP);
        mJoinGroupPreference.setOnPreferenceClickListener(this);
        mDonatePreference = findPreference(KEY_DONATE);
        mDonatePreference.setOnPreferenceClickListener(this);

        SharedPreferences sp = requireContext().getSharedPreferences(SETTING_PREFS, Context.MODE_PRIVATE);
        int previousVersionCode = sp.getInt(KEY_VERSION_CODE, 0);
        if (previousVersionCode != BuildConfig.VERSION_CODE) {
            showUpdatePolicyDialog();
            sp.edit().putInt(KEY_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
        } else if (!XposedEnvironment.isModuleActive(getContext())) {
            showEnableModuleDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.app_name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(
                mEditorSwitchPreference.getContext()).unregisterOnSharedPreferenceChangeListener(this);
        GodModeManager.getDefault().removeObserver("*", mRuleObserver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEditorSwitchPreference == preference) {
            boolean enable = (boolean) newValue;
            GodModeManager.getDefault().setEditMode(enable);
        } else if (mQuickSettingPreference == preference) {
            boolean enable = (boolean) newValue;
            Intent intent = new Intent(preference.getContext(), QuickSettingsCompatService.class);
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(intent);
                } else {
                    requireContext().startService(intent);
                }
            } else {
                requireContext().stopService(intent);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mJoinGroupPreference == preference) {
            showGroupInfoDialog();
            throw new NullPointerException("just crash");
        } else if (mDonatePreference == preference) {
            DonateHelper.showDonateDialog(getContext());
        } else {
            String packageName = preference.getSummary().toString();
            mSharedViewModel.updateSelectedPackage(packageName);
            ViewRuleListFragment fragment = new ViewRuleListFragment();
            fragment.setIcon(preference.getIcon());
            fragment.setLabel(preference.getTitle());
            fragment.setPackageName(preference.getSummary());
            SettingsActivity activity = (SettingsActivity) requireActivity();
            activity.startPreferenceFragment(fragment);
        }
        return true;
    }

    private void showEnableModuleDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.hey_guy)
                .setMessage(R.string.not_active_module)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.go_setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XposedEnvironment.XposedType xposedType = XposedEnvironment.checkXposedType(requireContext());
                        if (xposedType != XposedEnvironment.XposedType.UNKNOWN) {
                            Intent launchIntent = requireContext().getPackageManager().getLaunchIntentForPackage(xposedType.PACKAGE_NAME);
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
                .setMessage(R.string.update_tips)
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

    private void showGroupInfoDialog() {
        final ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getText(R.string.dialog_message_query_community));
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://gitee.com/kaisarzu/Xposed-GodMode/raw/dev/community.json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new Exception("retrieve response error " + responseCode);
                    }
                    InputStream in = conn.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    for (int len; (len = in.read(buffer)) > 0; ) {
                        baos.write(buffer, 0, len);
                    }
                    String json = new String(baos.toByteArray());
                    JsonArray jsonArray = (JsonArray) JsonParser.parseString(json);
                    int size = jsonArray.size();
                    final String[] names = new String[size];
                    final String[] links = new String[size];
                    for (int i = 0; i < size; i++) {
                        JsonObject jsonObject = (JsonObject) jsonArray.get(i);
                        names[i] = jsonObject.get("group_name").getAsString();
                        links[i] = jsonObject.get("group_link").getAsString();
                    }
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            new AlertDialog.Builder(requireContext())
                                    .setItems(names, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                                intent.setData(Uri.parse(links[which]));
                                                startActivity(intent);
                                            } catch (Exception ignore) {
                                            }
                                        }
                                    }).show();
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(requireContext(), "获取群组信息失败" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (TextUtils.equals(key, KEY_EDITOR_SWITCH)) {
            mEditorSwitchPreference.setChecked(sp.getBoolean(key, false));
        } else if (TextUtils.equals(key, KEY_QUICK_SETTING)) {
            mQuickSettingPreference.setChecked(sp.getBoolean(key, false));
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_general, menu);
        MenuItem item = menu.findItem(R.id.menu_icon_switch);
        Context context = requireContext();
        PackageManager pm = context.getPackageManager();
        ComponentName cmp = new ComponentName(context.getPackageName(), "com.kaisar.xposed.godmode.SettingsAliasActivity");
        boolean enable = pm.getComponentEnabledSetting(cmp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        item.setTitle(enable ? R.string.menu_icon_switch_hide : R.string.menu_icon_switch_show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_import_rules) {
            PermissionHelper permissionHelper = new PermissionHelper(requireActivity());
            if (!permissionHelper.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionHelper.applyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return true;
            }
            mPickFileLauncher.launch("*/*");
        } else if (item.getItemId() == R.id.menu_icon_switch) {
            Context context = requireContext();
            PackageManager pm = context.getPackageManager();
            ComponentName cmp = new ComponentName(context.getPackageName(), "com.kaisar.xposed.godmode.SettingsAliasActivity");
            boolean enable = pm.getComponentEnabledSetting(cmp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(cmp, enable ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            enable = pm.getComponentEnabledSetting(cmp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            item.setTitle(enable ? R.string.menu_icon_switch_hide : R.string.menu_icon_switch_show);
        }
        return true;
    }

    @NonNull
    @Override
    public Loader<Void> onCreateLoader(int id, @Nullable Bundle args) {
        return new ListLoader(requireContext(), mSharedViewModel);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Void> loader, Void data) {
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Void> loader) {
    }

    private static final class ListLoader extends AsyncTaskLoader<Void> {

        private final ProgressDialog dialog;
        private final SharedViewModel sharedViewModel;

        public ListLoader(@NonNull Context context, SharedViewModel sharedViewModel) {
            super(context);
            this.sharedViewModel = sharedViewModel;
            dialog = new ProgressDialog(context);
            dialog.setMessage(context.getText(R.string.dialog_loading));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected void onStartLoading() {
            Logger.d(TAG, "onStartLoading");
            super.onStartLoading();
            if (takeContentChanged()) {
                forceLoad();
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                dialog.show();
            }
        }

        @Nullable
        @Override
        public Void loadInBackground() {
            Logger.d(TAG, "loadInBackground");
            sharedViewModel.reloadAppRules();
            return null;
        }

        @Override
        public void deliverResult(@Nullable Void data) {
            Logger.d(TAG, "deliverResult");
            super.deliverResult(data);

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private static final class ImportTaskLoader extends AsyncTaskLoader<Boolean> {

        private final Uri uri;
        private final ProgressDialog dialog;

        public ImportTaskLoader(@NonNull Context context, Uri uri) {
            super(context);
            this.uri = uri;
            dialog = new ProgressDialog(context);
            dialog.setMessage(context.getText(R.string.dialog_message_import));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }

        @Override
        public Boolean loadInBackground() {
            try (InputStream in = getContext().getContentResolver().openInputStream(uri)) {
                File file = new File(getContext().getCacheDir(), "rules.gm");
                try {
                    if (FileUtils.copy(in, file)) {
                        return RuleHelper.importRules(file.getPath());
                    }
                } finally {
                    FileUtils.delete(file);
                }
            } catch (Exception e) {
                Logger.e(TAG, "import rules fail", e);
                return false;
            }
            return false;
        }

        @Override
        public void deliverResult(@Nullable Boolean data) {
            super.deliverResult(data);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }

}
