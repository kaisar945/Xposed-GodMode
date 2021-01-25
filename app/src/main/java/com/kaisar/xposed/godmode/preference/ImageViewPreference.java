package com.kaisar.xposed.godmode.preference;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.kaisar.xposed.godmode.R;

/**
 * Created by jrsen on 17-10-19.
 */

public final class ImageViewPreference extends androidx.preference.Preference {

    private ImageView mImageView;
    private Bitmap mBitmap;

    public ImageViewPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_image_preview);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mImageView = (ImageView) holder.findViewById(R.id.image);
        if (mBitmap != null) {
            mImageView.setImageBitmap(mBitmap);
        }
    }

    public void setImageBitmap(Bitmap bm) {
        mBitmap = bm;
        if (mImageView != null) {
            mImageView.setImageBitmap(bm);
        }
    }

    public void setImageResource(int resId) {
        if (mImageView != null) {
            mImageView.setImageResource(resId);
        }
    }

}
