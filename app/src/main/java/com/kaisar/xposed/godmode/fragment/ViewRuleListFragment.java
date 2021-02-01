package com.kaisar.xposed.godmode.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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
import androidx.core.content.res.TypedArrayUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kaisar.xposed.godmode.R;
import com.kaisar.xposed.godmode.SettingsActivity;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.PermissionHelper;
import com.kaisar.xposed.godmode.util.Preconditions;
import com.kaisar.xposed.godmode.util.RuleHelper;
import com.kaisar.xposed.godmode.widget.Snackbar;

import java.util.List;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;
import static com.kaisar.xposed.godmode.injection.util.CommonUtils.recycleNullableBitmap;

/**
 * Created by jrsen on 17-10-29.
 */

public final class ViewRuleListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final int THUMBNAIL_LOADER_ID = 0x01;
    private static final int EXPORT_TASK_LOADER_ID = 0x02;

    private Logger mLogger;
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
//        mLogger = Logger.getLogger("流程");
        setHasOptionsMenu(true);
        mSharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mSharedViewModel.getSelectedPackage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String packageName) {
                mSharedViewModel.updateViewRuleList(packageName);
            }
        });
        mSharedViewModel.getActRules().observe(this, new Observer<List<ViewRule>>() {
            @Override
            public void onChanged(List<ViewRule> viewRules) {
                if (viewRules.isEmpty()) {
//                    mLogger.d("規則列表頁爲空返回上一個頁面");
                    requireActivity().onBackPressed();
                } else {
//                    mLogger.d("重新加載規則列表");
                    LoaderManager.getInstance(ViewRuleListFragment.this).restartLoader(THUMBNAIL_LOADER_ID, null, ViewRuleListFragment.this).onContentChanged();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View viewGroup = super.onCreateView(inflater, container, savedInstanceState);
        Preconditions.checkNotNull(viewGroup, "view group should not be null");
        mRecyclerView = viewGroup.findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(new DataAdapter());
        return viewGroup;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.title_app_rule);
    }

    final class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

        final @LayoutRes
        int mResourceId;

        @SuppressLint("RestrictedApi")
        public DataAdapter() {
            int attr = TypedArrayUtils.getAttr(requireContext(), R.attr.preferenceStyle, android.R.attr.preferenceStyle);
            TypedArray a = requireContext().obtainStyledAttributes(null, R.styleable.Preference, attr, 0);
            mResourceId = TypedArrayUtils.getResourceId(a, R.styleable.Preference_layout,
                    R.styleable.Preference_android_layout, R.layout.preference);
            a.recycle();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(requireContext()).inflate(mResourceId, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            List<ViewRule> viewRules = mSharedViewModel.getActRules().getValue();
            ViewRule viewRule = viewRules.get(position);

            if (Preconditions.checkBitmap(viewRule.thumbnail)) {
                holder.mImageView.setImageBitmap(viewRule.thumbnail);
            }
            if (viewRule.activityClass != null) {
                String activityName = viewRule.activityClass.substring(viewRule.activityClass.lastIndexOf('.') + 1);
                holder.mTitleTextView.setText(getString(R.string.field_activity, activityName));
            } else {
                holder.mTitleTextView.setText(getString(R.string.unknown_activity));
            }

            SpannableStringBuilder summaryBuilder = new SpannableStringBuilder();
            if (!TextUtils.isEmpty(viewRule.alias)) {
                SpannableString ss = new SpannableString(getString(R.string.field_rule_alias, viewRule.alias));
                ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.alias)), 0, ss.length(), 0);
                summaryBuilder.append(ss);
            }
            summaryBuilder.append(getString(R.string.field_view, viewRule.viewClass));
            holder.mSummaryTextView.setText(summaryBuilder);

            setEnabledStateOnViews(holder.itemView, true);
            holder.itemView.setFocusable(true);
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewRuleDetailsContainerFragment fragment = new ViewRuleDetailsContainerFragment();
                    fragment.setCurIndex(position);
                    fragment.setIcon(mIcon);
                    fragment.setLabel(mLabel);
                    fragment.setPackageName(mPackageName);
                    SettingsActivity activity = (SettingsActivity) requireActivity();
                    activity.startPreferenceFragment(fragment);
                }
            });
        }

        private void setEnabledStateOnViews(View v, boolean enabled) {
            v.setEnabled(enabled);

            if (v instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) v;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    setEnabledStateOnViews(vg.getChildAt(i), enabled);
                }
            }
        }

        @Override
        public int getItemCount() {
            List<ViewRule> viewRules = mSharedViewModel.getActRules().getValue();
            return viewRules.size();
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
    public void onDestroy() {
        super.onDestroy();
        List<ViewRule> viewRules = mSharedViewModel.getActRules().getValue();
        for (ViewRule viewRule : viewRules) {
            recycleNullableBitmap(viewRule.snapshot);
            viewRule.snapshot = null;
            recycleNullableBitmap(viewRule.thumbnail);
            viewRule.thumbnail = null;
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
            PermissionHelper permissionHelper = new PermissionHelper(requireActivity());
            if (!permissionHelper.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionHelper.applyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return true;
            }
            LoaderManager.getInstance(this).initLoader(EXPORT_TASK_LOADER_ID, null, new LoaderManager.LoaderCallbacks<String>() {
                @NonNull
                @Override
                public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
                    return new ExportTaskLoader(requireContext());
                }

                @Override
                public void onLoadFinished(@NonNull Loader<String> loader, String filepath) {
                    Snackbar.make(requireActivity(), FileUtils.exists(filepath)
                            ? getString(R.string.export_successful, filepath)
                            : getString(R.string.export_failed), Snackbar.LENGTH_LONG).show();
                    LoaderManager.getInstance(ViewRuleListFragment.this).destroyLoader(EXPORT_TASK_LOADER_ID);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<String> loader) {

                }
            }).forceLoad();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        return new ThumbnailLoader(requireContext(), mSharedViewModel);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Boolean> loader, Boolean data) {
        if (data) {
            @SuppressWarnings("rawtypes") RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Boolean> loader) {
    }

    private static final class ThumbnailLoader extends AsyncTaskLoader<Boolean> {

        private final ProgressDialog mDialog;
        private final SharedViewModel mSharedViewModel;

        public ThumbnailLoader(Context context, SharedViewModel sharedViewModel) {
            super(context);
            mSharedViewModel = sharedViewModel;
            mDialog = new ProgressDialog(context);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setMessage(getContext().getString(R.string.dialog_loading));
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            if (takeContentChanged()) {
                forceLoad();
                if (mDialog.isShowing()) {
                    mDialog.dismiss();
                }
                mDialog.show();
            }
        }

        @Override
        public Boolean loadInBackground() {
            GodModeManager gmm = GodModeManager.getDefault();
            List<ViewRule> viewRules = mSharedViewModel.getActRules().getValue();
            for (ViewRule viewRule : viewRules) {
                if (viewRule.thumbnail == null && !TextUtils.isEmpty(viewRule.imagePath)) {
                    try {
                        ParcelFileDescriptor parcelFileDescriptor = gmm.openImageFileDescriptor(viewRule.imagePath);
                        Preconditions.checkNotNull(parcelFileDescriptor);
                        try {
                            viewRule.snapshot = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor.getFileDescriptor());
                            viewRule.thumbnail = Bitmap.createBitmap(viewRule.snapshot, viewRule.x, viewRule.y, viewRule.width, viewRule.height);
                        } finally {
                            parcelFileDescriptor.close();
                        }
                    } catch (Exception e) {
                        Logger.w(TAG, "load image failed", e);
                    }
                }
            }
            return true;
        }

        @Override
        public void deliverResult(@Nullable Boolean data) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
            if (isReset()) {
                releaseResources();
            }
            super.deliverResult(data);
        }

        private void releaseResources() {
            List<ViewRule> viewRules = mSharedViewModel.getActRules().getValue();
            for (ViewRule rule : viewRules) {
                recycleNullableBitmap(rule.snapshot);
                recycleNullableBitmap(rule.thumbnail);
            }
        }
    }

    private final class ExportTaskLoader extends AsyncTaskLoader<String> {

        private final ProgressDialog dialog;


        public ExportTaskLoader(@NonNull Context context) {
            super(context);
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(context.getResources().getString(R.string.dialog_message_export));
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }

        @Nullable
        @Override
        public String loadInBackground() {
            List<ViewRule> viewRules = mSharedViewModel.getActRules().getValue();
            return RuleHelper.exportRules(viewRules.toArray(new ViewRule[0]));
        }

        @Override
        public void deliverResult(String filepath) {
            super.deliverResult(filepath);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }

}
