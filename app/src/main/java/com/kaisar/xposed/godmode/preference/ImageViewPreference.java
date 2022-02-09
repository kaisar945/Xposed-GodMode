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

    private ImageView mImageView;
    private Bitmap mBitmap;

    public ImageViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_image_preview);
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
        mImageView = (ImageView) holder.findViewById(R.id.image);
        if (mBitmap != null) {
            mImageView.setImageBitmap(mBitmap);
            mBitmap = null;
        }
    }

    public void setImageBitmap(Bitmap bm) {
        if (mImageView != null) {
            mImageView.setImageBitmap(mBitmap);
        } else {
            mBitmap = bm;
        }
    }

}
