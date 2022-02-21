package com.kaisar.xposed.godmode.fragment;

import static com.kaisar.xposed.godmode.fragment.GeneralPreferenceFragmentDirections.actionGeneralPreferenceFragmentToViewRuleListFragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.CrashHandler;
import com.kaisar.xposed.godmode.GodModeApplication;
import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.preference.ProgressPreference;
import com.kaisar.xposed.godmode.rule.ActRules;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.util.Clipboard;
import com.kaisar.xposed.godmode.util.DonateHelper;
import com.kaisar.xposed.godmode.util.PermissionHelper;
import com.kaisar.xposed.godmode.util.Preconditions;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by jrsen on 17-10-29.
 */

public final class GeneralPreferenceFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String SETTING_PREFS = "settings";
    private static final String KEY_VERSION_CODE = "version_code";

    private ProgressPreference mProgressPreference;
    private SwitchPreferenceCompat mEditorSwitchPreference;
    private Preference mJoinGroupPreference;
    private Preference mDonatePreference;

    private ActivityResultLauncher<String> mFileLauncher;
    private SharedViewModel mSharedViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mFileLauncher = requireActivity().registerForActivityResult(new ActivityResultContracts.GetContent(), this::onActivityResult);
        mSharedViewModel.appRules.observe(this, this::onAppRuleChange);
        if (!checkCrash()) {
            mProgressPreference.setVisible(true);
            mSharedViewModel.loadAppRules();
        }
    }

    private boolean checkCrash() {
        String crashInfo = CrashHandler.getLastCrashInfo(GodModeApplication.getApplication());
        if (crashInfo != null) {
            SpannableString text = new SpannableString(getString(R.string.crash_tip));
            SpannableString st = new SpannableString(crashInfo);
            st.setSpan(new RelativeSizeSpan(0.7f), 0, st.length(), 0);
            CharSequence message = TextUtils.concat(text, st);
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.hey_guy)
                    .setMessage(message)
                    .setPositiveButton(R.string.dialog_btn_copy, (dialog, which) -> Clipboard.putContent(requireContext(), crashInfo))
                    .show();
            return true;
        }
        return false;
    }

    private void onActivityResult(Uri uri) {
        if (uri == null) return;
        mProgressPreference.setVisible(true);
        mSharedViewModel.importExternalRules(requireContext(), uri, new SharedViewModel.ImportCallback() {
            @Override
            public void onSuccess() {
                Snackbar.make(requireActivity(), R.string.import_success, Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Throwable t) {
                Snackbar.make(requireActivity(), R.string.import_failed, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void onAppRuleChange(AppRules appRules) {
        mProgressPreference.setVisible(false);
        appRules = appRules != null ? appRules : new AppRules();
        Set<Map.Entry<String, ActRules>> entries = appRules.entrySet();
        PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.pref_key_app_rules));
        category.removeAll();
        PackageManager pm = requireContext().getPackageManager();
        for (Map.Entry<String, ActRules> entry : entries) {
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
            preference.setOnPreferenceClickListener(this);
            category.addPreference(preference);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, rootKey);
        mProgressPreference = (ProgressPreference) findPreference(getString(R.string.pref_key_progress_indicator));
        mProgressPreference.setVisible(false);
        mEditorSwitchPreference = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_key_editor));
        mEditorSwitchPreference.setChecked(GodModeManager.getDefault().isInEditMode());
        mEditorSwitchPreference.setOnPreferenceClickListener(this);
        mEditorSwitchPreference.setOnPreferenceChangeListener(this);
        mJoinGroupPreference = findPreference(getString(R.string.pref_key_join_group));
        mJoinGroupPreference.setOnPreferenceClickListener(this);
        mDonatePreference = findPreference(getString(R.string.pref_key_donate));
        mDonatePreference.setOnPreferenceClickListener(this);

        SharedPreferences sp = requireContext().getSharedPreferences(SETTING_PREFS, Context.MODE_PRIVATE);
        int previousVersionCode = sp.getInt(KEY_VERSION_CODE, 0);
        if (previousVersionCode != BuildConfig.VERSION_CODE) {
            sp.edit().putInt(KEY_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
            showUpdatePolicyDialog();
        } else if (!GodModeManager.getDefault().hasLight()) {
            showEnableModuleDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedViewModel.updateTitle(R.string.app_name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(
                mEditorSwitchPreference.getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return GodModeManager.getDefault().hasLight();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mEditorSwitchPreference == preference) {
            if (!GodModeManager.getDefault().hasLight()) {
                Toast.makeText(requireContext(), R.string.not_active_module, Toast.LENGTH_SHORT).show();
                return true;
            }
            GodModeManager.getDefault().setEditMode(mEditorSwitchPreference.isChecked());
        } else if (mJoinGroupPreference == preference) {
            showGroupInfoDialog();
        } else if (mDonatePreference == preference) {
            DonateHelper.showDonateDialog(requireContext());
        } else {
            String packageName = preference.getSummary().toString();
            mSharedViewModel.updateSelectedPackage(packageName);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(actionGeneralPreferenceFragmentToViewRuleListFragment());
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (TextUtils.equals(key, getString(R.string.pref_key_editor))) {
            mEditorSwitchPreference.setChecked(sp.getBoolean(key, false));
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_general, menu);
        MenuItem item = menu.findItem(R.id.menu_icon_switch);
        boolean hidden = mSharedViewModel.isIconHidden(requireContext());
        item.setTitle(!hidden ? R.string.menu_icon_switch_hide : R.string.menu_icon_switch_show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_import_rules) {
            PermissionHelper permissionHelper = new PermissionHelper(requireActivity());
            if (!permissionHelper.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionHelper.applyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return true;
            }
            mFileLauncher.launch("*/*");
        } else if (item.getItemId() == R.id.menu_icon_switch) {
            boolean hidden = mSharedViewModel.isIconHidden(requireContext());
            mSharedViewModel.setIconHidden(requireContext(), hidden = !hidden);
            item.setTitle(hidden ? R.string.menu_icon_switch_show : R.string.menu_icon_switch_hide);
        }
        return true;
    }

    private void showEnableModuleDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.hey_guy)
                .setMessage(R.string.not_active_module)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showUpdatePolicyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.welcome_title)
                .setMessage(R.string.update_tips)
                .setPositiveButton(R.string.dialog_btn_alipay, (dialog1, which) -> DonateHelper.startAliPayDonate(requireContext()))
                .setNegativeButton(R.string.dialog_btn_wxpay, (dialog12, which) -> DonateHelper.startWxPayDonate(requireContext()))
                .show();
    }

    private void showGroupInfoDialog() {
        final ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getText(R.string.dialog_message_query_community));
        progressDialog.show();
        mSharedViewModel.getGroupInfo(new Callback<Map<String, String>[]>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>[]> call, @NonNull Response<Map<String, String>[]> response) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                try {
                    if (!response.isSuccessful()) throw new Exception("not successful");
                    Map<String, String>[] body = Preconditions.checkNotNull(response.body());
                    final int N = body.length;
                    String[] names = new String[N];
                    String[] links = new String[N];
                    for (int i = 0; i < N; i++) {
                        Map<String, String> map = body[i];
                        names[i] = map.get("group_name");
                        links[i] = map.get("group_link");
                    }
                    new AlertDialog.Builder(requireContext())
                            .setItems(names, (dialog, which) -> {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(links[which]));
                                    startActivity(intent);
                                } catch (Exception ignore) {
                                }
                            }).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "获取群组信息失败:" + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>[]> call, @NonNull Throwable t) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(requireContext(), "获取群组信息失败" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}