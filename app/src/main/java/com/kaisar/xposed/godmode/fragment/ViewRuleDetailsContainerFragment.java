package com.kaisar.xposed.godmode.fragment;

import android.os.Bundle;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.repository.LocalRepository;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;
import com.kaisar.xposed.godmode.util.SafHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ViewRuleDetailsContainerFragment extends PreferenceFragmentCompat {

    private int mCurIndex;

    private ViewPager2 mViewPager;
    private SharedViewModel mSharedViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
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
            if (item.getItemId() == R.id.menu_revert) {
                mSharedViewModel.deleteRule(viewRule);
                NavHostFragment.findNavController(this).popBackStack();
            } else if (item.getItemId() == R.id.menu_export) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                String date = simpleDateFormat.format(new Date());
                ViewRule mViewRule = viewRules.get(0);
                SafHelper.saveFile(requireActivity(), String.format("GodMode-Rules-%s", String.format("%s(%s)-%s", mViewRule.label, mViewRule.matchVersionName, date)), 114514);
                File cacheDir = requireActivity().getExternalCacheDir();
                LocalRepository.exportRules(cacheDir.getPath(), viewRules);
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
