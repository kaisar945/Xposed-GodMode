package com.viewblocker.jrsen.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.R;
import com.viewblocker.jrsen.adapter.AdapterDataObserver;
import com.viewblocker.jrsen.injection.bridge.GodModeManager;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.QRCodeFactory;
import com.viewblocker.jrsen.widget.Snackbar;

import java.util.Objects;

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
        } else if (item.getItemId() == R.id.menu_share) {
            try {
                Bitmap image = Objects.requireNonNull(QRCodeFactory.encode(viewRule));
                final String imagePath = Objects.requireNonNull(MediaStore.Images.Media.insertImage(requireContext().getContentResolver(), image, "rule_image", "god mode rule file"));
                Snackbar.make(requireActivity(), R.string.export_view_rule_success, Snackbar.LENGTH_LONG)
                        .setAction(R.string.menu_share, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Uri imgUri = Uri.parse(imagePath);
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, imgUri);
                                shareIntent.setType("image/jpeg");
                                startActivity(shareIntent);
                            }
                        })
                        .show();
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(requireActivity(), R.string.export_view_rule_failed, Snackbar.LENGTH_LONG).show();
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
