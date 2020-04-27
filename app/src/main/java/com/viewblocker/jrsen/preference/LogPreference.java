package com.viewblocker.jrsen.preference;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.TextView;

/**
 * Created by jrsen on 17-10-29.
 */

public final class LogPreference extends android.support.v7.preference.Preference {

    public LogPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textView = (TextView) holder.findViewById(android.R.id.summary);
        textView.setMaxLines(Integer.MAX_VALUE);
    }
}
