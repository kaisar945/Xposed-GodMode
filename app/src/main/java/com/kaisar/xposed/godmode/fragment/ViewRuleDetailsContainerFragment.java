package com.kaisar.xposed.godmode.fragment;

import android.graphics.drawable.Drawable;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceFragmentCompat;
import androidx.viewpager.widget.ViewPager;

import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.adapter.AdapterDataObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.RuleHelper;
import com.kaisar.xposed.godmode.widget.Snackbar;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

public final class ViewRuleDetailsContainerFragment extends PreferenceFragmentCompat implements ViewPager.OnPageChangeListener {

    private int curIndex;
    private Drawable icon;
    private CharSequence label;
    private CharSequence packageName;
    private AdapterDataObserver<ViewRule> adapterDataObserver;

    public void setCurIndex(int curIndex) {
        this.curIndex = curIndex;
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

    public void setAdapterDataObserver(AdapterDataObserver<ViewRule> adapterDataObserver) {
        this.adapterDataObserver = adapterDataObserver;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewPager viewPager = (ViewPager) inflater.inflate(R.layout.preference_view_pager, container, false);
        viewPager.addOnPageChangeListener(this);
        viewPager.setAdapter(new PanelFragmentPagerAdapter(requireActivity().getSupportFragmentManager()));
        viewPager.setCurrentItem(curIndex);
        return viewPager;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_app_rule, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ViewRule viewRule = adapterDataObserver.getItem(curIndex);
        if (item.getItemId() == R.id.menu_revert) {
            GodModeManager.getDefault().deleteRule(packageName.toString(), viewRule);
            adapterDataObserver.onItemRemoved(curIndex);
            requireActivity().onBackPressed();
        } else if (item.getItemId() == R.id.menu_export) {
            try {
                String filepath = RuleHelper.exportRules(viewRule);
                Snackbar.make(requireActivity(), getString(R.string.export_successful, filepath), Snackbar.LENGTH_LONG).show();
            } catch (Exception e) {
                Logger.e(TAG, "export single rule fail", e);
                Snackbar.make(requireActivity(), R.string.export_failed, Snackbar.LENGTH_LONG).show();
            }
        }
        return true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        curIndex = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    final class PanelFragmentPagerAdapter extends FragmentStatePagerAdapter {

        PanelFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ViewRuleDetailsFragment fragment = new ViewRuleDetailsFragment();
            fragment.setIndex(position);
            fragment.setIcon(icon);
            fragment.setLabel(label);
            fragment.setPackageName(packageName);
            fragment.setViewRule(adapterDataObserver.getItem(position));
            fragment.setDataObserver(adapterDataObserver);
            return fragment;
        }

        @Override
        public int getCount() {
            return adapterDataObserver.getSize();
        }
    }


}
