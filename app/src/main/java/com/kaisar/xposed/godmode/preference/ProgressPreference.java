package com.kaisar.xposed.godmode.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.kaisar.xposed.godmode.R;


public final class ProgressPreference extends androidx.preference.Preference {

    private LinearProgressIndicator mLinearProgressIndicator;

    public ProgressPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_progress);
    }

    public ProgressPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_progress);
    }

    public ProgressPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mLinearProgressIndicator = (LinearProgressIndicator) holder.findViewById(R.id.progress_indicator);
    }

}
