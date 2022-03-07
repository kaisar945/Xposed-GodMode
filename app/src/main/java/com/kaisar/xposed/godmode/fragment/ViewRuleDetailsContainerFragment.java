package com.kaisar.xposed.godmode.fragment;

import android.content.ActivityNotFoundException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.google.android.material.snackbar.Snackbar;
import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.BackupUtils;
import com.kaisar.xposed.godmode.util.Preconditions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ViewRuleDetailsContainerFragment extends PreferenceFragmentCompat {

    private int mCurIndex;

    private ViewPager2 mViewPager;
    private SharedViewModel mSharedViewModel;

    private ActivityResultLauncher<String> mBackupLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ViewRuleDetailsContainerFragmentArgs args = ViewRuleDetailsContainerFragmentArgs.fromBundle(requireArguments());
        mCurIndex = args.getCurIndex();
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mBackupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(), this::onBackupFileSelected);
    }

    private void onBackupFileSelected(Uri uri) {
        if (uri == null) return;
        List<ViewRule> rules = mSharedViewModel.actRules.getValue();
        if (rules != null && !rules.isEmpty()) {
            ViewRule viewRule = rules.get(mCurIndex);
            List<ViewRule> viewRules = rules.subList(mCurIndex, mCurIndex + 1);
            mSharedViewModel.backupRules(uri, viewRule.packageName, viewRules, new SharedViewModel.ResultCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(Exception e) {
                    Snackbar.make(requireView(), R.string.snack_bar_msg_backup_rule_fail, Snackbar.LENGTH_SHORT).show();
                }
            });
        } else {
            Snackbar.make(requireView(), R.string.snack_bar_msg_backup_rule_fail, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    private final OnPageChangeCallback mCallback = new OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            mCurIndex = position;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mViewPager = (ViewPager2) inflater.inflate(R.layout.fragment_rule_details_container, container, false);
        mViewPager.setAdapter(new DetailFragmentStateAdapter(this));
        mViewPager.registerOnPageChangeCallback(mCallback);
        mViewPager.setCurrentItem(mCurIndex);
        return mViewPager;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_app_rule, menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        mSharedViewModel.actRules.observe(this, newData -> {
            if (newData.isEmpty()) {
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                DetailFragmentStateAdapter adapter = (DetailFragmentStateAdapter) mViewPager.getAdapter();
                Preconditions.checkNotNull(adapter, "This object should not be null.");
                List<ViewRule> oldData = adapter.getData();
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return oldData.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return newData.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return oldData.get(oldItemPosition).hashCode() == newData.get(newItemPosition).hashCode();
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return true;
                    }
                });
                adapter.setData(newData);
                diffResult.dispatchUpdatesTo(adapter);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        List<ViewRule> viewRules = mSharedViewModel.actRules.getValue();
        if (viewRules != null) {
            ViewRule viewRule = viewRules.get(mCurIndex);
            if (item.getItemId() == R.id.menu_delete_rule) {
                mSharedViewModel.deleteRule(viewRule);
                NavHostFragment.findNavController(this).popBackStack();
            } else if (item.getItemId() == R.id.menu_backup_rule) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault());
                    PackageManager packageManager = requireContext().getPackageManager();
                    String packageName = mSharedViewModel.selectedPackage.getValue();
                    if (packageName == null)
                        throw new BackupUtils.BackupException("packageName should not be null.");
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                    String label = applicationInfo.loadLabel(packageManager).toString();
                    String filename = String.format(Locale.getDefault(), "%s_%s.gzip", label, sdf.format(new Date()));
                    mBackupLauncher.launch(filename);
                    return true;
                } catch (ActivityNotFoundException | PackageManager.NameNotFoundException | BackupUtils.BackupException e) {
                    Snackbar.make(requireView(), R.string.snack_bar_msg_backup_rule_fail, Snackbar.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    static final class DetailFragmentStateAdapter extends FragmentStateAdapter {

        final List<ViewRule> mData = new ArrayList<>();

        public DetailFragmentStateAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        public List<ViewRule> getData() {
            return mData;
        }

        public void setData(List<ViewRule> data) {
            mData.clear();
            mData.addAll(data);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            ViewRule viewRule = mData.get(position);
            ViewRuleDetailsFragment fragment = new ViewRuleDetailsFragment();
            fragment.setViewRule(viewRule);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }


}
