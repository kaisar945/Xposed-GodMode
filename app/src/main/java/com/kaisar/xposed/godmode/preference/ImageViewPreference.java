package com.kaisar.xposed.godmode.preference;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.kaisar.xposed.godmode.R;

/**
 * Created by jrsen on 17-10-19.
 */

public final class ImageViewPreference extends androidx.preference.Preference {

    private Bitmap mBitmap;

    public ImageViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_widget_image);
    }

    public ImageViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageViewPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageViewPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView imageView = (ImageView) holder.itemView.findViewById(R.id.image);
        if (mBitmap != null) {
            imageView.setImageBitmap(mBitmap);
            mBitmap = null;
        }
    }

    public void setImageBitmap(Bitmap bm) {
        mBitmap = bm;
        notifyChanged();
    }

}
