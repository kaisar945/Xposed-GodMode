package com.kaisar.xposed.godmode.fragment;

import android.content.pm.PackageManager;
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
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.model.SharedViewModel;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleListFragment extends Fragment {

    private Drawable mIcon;
    private String mPackageName;
    private RecyclerView mRecyclerView;
    private SharedViewModel mSharedViewModel;

    public ViewRuleListFragment() {
        super(R.layout.list_fragment_layout);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mPackageName = mSharedViewModel.mSelectedPackage.getValue();
        Objects.requireNonNull(mPackageName, "mSelectedPackage should not be null.");
        try {
            PackageManager packageManager = requireContext().getPackageManager();
            mIcon = packageManager.getApplicationIcon(mPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            mIcon = ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_god, requireContext().getTheme());
        }
        mSharedViewModel.mSelectedPackage.observe(this, packageName -> mSharedViewModel.updateViewRuleList(packageName));
        mSharedViewModel.mActRules.observe(this, newData -> {
            if (newData.isEmpty()) {
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                ListAdapter adapter = (ListAdapter) mRecyclerView.getAdapter();
                if (adapter != null) {
                    List<ViewRule> oldData = adapter.getData();
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new Callback(oldData, newData));
                    adapter.setData(newData);
                    diffResult.dispatchUpdatesTo(adapter);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRecyclerView == null) {
            mRecyclerView = (RecyclerView) super.onCreateView(inflater, container, savedInstanceState);
        }
        return mRecyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) view;
        ListAdapter adapter = (ListAdapter) recyclerView.getAdapter();
        if (adapter == null) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(new ListAdapter());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedViewModel.updateTitle(R.string.title_app_rule);
    }

    private static final class Callback extends DiffUtil.Callback {

        final List<ViewRule> mOldData, mNewData;

        private Callback(List<ViewRule> oldData, List<ViewRule> newData) {
            mOldData = oldData;
            mNewData = newData;
        }

        @Override
        public int getOldListSize() {
            return mOldData.size();
        }

        @Override
        public int getNewListSize() {
            return mNewData.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldData.get(oldItemPosition).hashCode() == mNewData.get(newItemPosition).hashCode();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return true;
        }
    }

    private final class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> implements View.OnClickListener {

        @LayoutRes
        private final int mLayoutResId = androidx.preference.R.layout.preference;
        private final List<ViewRule> mData = new ArrayList<>();

        public void setData(List<ViewRule> newData) {
            mData.clear();
            mData.addAll(newData);
        }

        public List<ViewRule> getData() {
            return mData;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(mLayoutResId, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ViewRule viewRule = mData.get(position);
            Glide.with(ViewRuleListFragment.this).load(viewRule).error(mIcon).diskCacheStrategy(DiskCacheStrategy.NONE).into(holder.mImageView);
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
            return mData.size();
        }

        @Override
        public void onClick(View view) {
            final int position = (Integer) view.getTag();
            NavHostFragment.findNavController(ViewRuleListFragment.this).navigate(ViewRuleListFragmentDirections.actionViewRuleListFragmentToViewRuleDetailsContainerFragment(position));
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
