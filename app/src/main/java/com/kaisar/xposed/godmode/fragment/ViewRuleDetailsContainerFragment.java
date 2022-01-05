package com.kaisar.xposed.godmode.fragment;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

import android.Manifest;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.repository.LocalRepository;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.PermissionHelper;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ViewRuleDetailsContainerFragment extends PreferenceFragmentCompat {

    private int mCurIndex;
    private Drawable mIcon;
    private CharSequence mLabel;
    private CharSequence mPackageName;

    private SharedViewModel mSharedViewModel;

    public void setCurIndex(int curIndex) {
        mCurIndex = curIndex;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public void setLabel(CharSequence label) {
        mLabel = label;
    }

    public void setPackageName(CharSequence packageName) {
        mPackageName = packageName;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mSharedViewModel.updateTitle(R.string.title_rule_details);
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
        ViewPager2 viewPager2 = (ViewPager2) inflater.inflate(R.layout.preference_view_pager, container, false);
        viewPager2.setAdapter(new DetailFragmentStateAdapter(this));
        viewPager2.registerOnPageChangeCallback(mCallback);
        viewPager2.setCurrentItem(mCurIndex);
        return viewPager2;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_app_rule, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        List<ViewRule> viewRules = mSharedViewModel.actRules.getValue();
        ViewRule viewRule = viewRules.get(mCurIndex);
        if (item.getItemId() == R.id.menu_revert) {
            mSharedViewModel.deleteRule(viewRule);
            requireActivity().onBackPressed();
        }
//        else if (item.getItemId() == R.id.menu_export) {
//            PermissionHelper permissionHelper = new PermissionHelper(requireActivity());
//            if (!permissionHelper.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                permissionHelper.applyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                return true;
//            }
//            File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
//            String date = simpleDateFormat.format(new Date());
//            File file = new File(externalStoragePublicDirectory, String.format("GodMode-Rules-%s.gzip", date));
//            boolean ok = LocalRepository.exportRules(file.getPath(), viewRules);
//            Snackbar.make(requireActivity(), ok ? getString(R.string.export_successful, file.getPath()) : getString(R.string.export_failed), Snackbar.LENGTH_LONG).show();
//        }
        return true;
    }

    final class DetailFragmentStateAdapter extends FragmentStateAdapter {

        public DetailFragmentStateAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            List<ViewRule> viewRules = mSharedViewModel.actRules.getValue();
            ViewRule viewRule = viewRules.get(position);
            ViewRuleDetailsFragment fragment = new ViewRuleDetailsFragment();
            fragment.setIcon(mIcon);
            fragment.setLabel(mLabel);
            fragment.setPackageName(mPackageName);
            fragment.setViewRule(viewRule);
            return fragment;
        }

        @Override
        public int getItemCount() {
            List<ViewRule> viewRules = mSharedViewModel.actRules.getValue();
            return viewRules.size();
        }
    }


}
