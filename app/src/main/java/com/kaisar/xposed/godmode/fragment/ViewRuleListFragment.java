package com.kaisar.xposed.godmode.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.SettingsActivity;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.Preconditions;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.io.File;
import java.util.List;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleListFragment extends Fragment {

    private Drawable mIcon;
    private CharSequence mLabel;
    private CharSequence mPackageName;

    private RecyclerView mRecyclerView;

    private SharedViewModel mSharedViewModel;

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public void setLabel(CharSequence label) {
        mLabel = label;
    }

    public void setPackageName(CharSequence packageName) {
        mPackageName = packageName;
    }

    public ViewRuleListFragment() {
        super(R.layout.list_fragment_layout);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mSharedViewModel.selectedPackage.observe(this, packageName -> mSharedViewModel.updateViewRuleList(packageName));
        mSharedViewModel.actRules.observe(this, viewRules -> {
            if (viewRules.isEmpty()) {
                requireActivity().onBackPressed();
            } else {
                ListAdapter adapter = (ListAdapter) mRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View viewGroup = super.onCreateView(inflater, container, savedInstanceState);
        mRecyclerView = Preconditions.checkNotNull(viewGroup).findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(new ListAdapter());
        return viewGroup;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedViewModel.updateTitle(R.string.title_app_rule);
    }

    private final class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> implements View.OnClickListener {

        @LayoutRes
        private final int mLayoutResId = androidx.preference.R.layout.preference;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(requireContext()).inflate(mLayoutResId, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            List<ViewRule> viewRules = mSharedViewModel.actRules.getValue();
            ViewRule viewRule = Preconditions.checkNotNull(viewRules).get(position);

            Glide.with(ViewRuleListFragment.this).load(new File(viewRule.imagePath)).into(holder.mImageView);
            if (viewRule.activityClass != null && viewRule.activityClass.lastIndexOf('.') > -1) {
                String activityName = viewRule.activityClass.substring(viewRule.activityClass.lastIndexOf('.') + 1);
                holder.mTitleTextView.setText(getString(R.string.field_activity, activityName));
            }

            SpannableStringBuilder summaryBuilder = new SpannableStringBuilder();
            if (!TextUtils.isEmpty(viewRule.alias)) {
                SpannableString ss = new SpannableString(getString(R.string.field_rule_alias, viewRule.alias));
                ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.prefsAliasColor)), 0, ss.length(), 0);
                summaryBuilder.append(ss);
            }
            summaryBuilder.append(getString(R.string.field_view, viewRule.viewClass));
            holder.mSummaryTextView.setText(summaryBuilder);
            holder.itemView.setFocusable(true);
            holder.itemView.setClickable(true);
            holder.itemView.setTag(position);
            holder.itemView.setOnClickListener(this);
        }

        @Override
        public int getItemCount() {
            List<ViewRule> actRules = mSharedViewModel.actRules.getValue();
            return actRules != null ? actRules.size() : 0;
        }

        @Override
        public void onClick(View view) {
            ViewRuleDetailsContainerFragment fragment = new ViewRuleDetailsContainerFragment();
            int position = (Integer) view.getTag();
            fragment.setCurIndex(position);
            fragment.setIcon(mIcon);
            fragment.setLabel(mLabel);
            fragment.setPackageName(mPackageName);
            SettingsActivity activity = (SettingsActivity) requireActivity();
            activity.startPreferenceFragment(fragment);
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            final ImageView mImageView;
            final TextView mTitleTextView;
            final TextView mSummaryTextView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                mImageView = itemView.findViewById(android.R.id.icon);
                mTitleTextView = itemView.findViewById(android.R.id.title);
                mSummaryTextView = itemView.findViewById(android.R.id.summary);
            }

        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_app_rules, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_revoke_rules) {
            if (!mSharedViewModel.deleteAppRules(mPackageName.toString())) {
                Snackbar.make(requireActivity(), R.string.snack_bar_msg_revert_rule_fail, Snackbar.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.menu_export_rules) {

        }
        return super.onOptionsItemSelected(item);
    }

}
