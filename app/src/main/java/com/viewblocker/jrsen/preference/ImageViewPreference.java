package com.viewblocker.jrsen.preference;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.viewblocker.jrsen.R;

/**
 * Created by jrsen on 17-10-19.
 */

public final class ImageViewPreference extends androidx.preference.Preference {

    private Bitmap bitmap;

    public ImageViewPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_image_preview);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView imageView = (ImageView) holder.findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);
    }

    //    @Override
//    protected View onCreateView(ViewGroup parent) {
//        View view = super.onCreateView(parent);
//        if (bitmap != null && !bitmap.isRecycled()) {
//            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
//            layoutParams.height = bitmap.getHeight();
//        }
//        return view;
//    }

//    @Override
//    protected void onBindView(View view) {
//        super.onBindView(view);
//        if (bitmap != null && !bitmap.isRecycled()) {
//            ImageView imageView = (ImageView) view.findViewById(R.id.image);
//            imageView.setImageBitmap(bitmap);
//        }
//    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        bitmap = (Bitmap) defaultValue;
    }


}
