package com.viewblocker.jrsen.injection.weiget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.viewblocker.jrsen.injection.ViewHelper;

import static com.viewblocker.jrsen.injection.ViewHelper.TAG_GM_CMP;

/**
 * Created by jrsen on 17-10-13.
 */

public final class MaskView extends View {

    private Drawable mMaskDrawable;

    private int mMarkColor = Color.TRANSPARENT;
    private boolean isMarked;

    public MaskView(Context context) {
        this(context, null);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setTag(TAG_GM_CMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mMaskDrawable != null) {
            mMaskDrawable.draw(canvas);
        }
    }

    public void updateOverlayBounds(int x, int y, int w, int h) {
        updateOverlayBounds(new Rect(x, y, x + w, y + h));
    }

    public void updateOverlayBounds(Rect bounds) {
        mMaskDrawable.setBounds(bounds);
        invalidate();
    }

    public Rect getRealBounds() {
        return mMaskDrawable.getBounds();
    }

    private void setMaskDrawable(Drawable drawable) {
        mMaskDrawable = drawable;
    }

    private Drawable getMaskDrawable() {
        return mMaskDrawable;
    }

    public void setMarkColor(int color) {
        mMarkColor = color;
    }

    public void setMarked(boolean enable) {
        if (isMarked != enable) {
            if (isMarked = enable) {
                mMaskDrawable.setColorFilter(mMarkColor, PorterDuff.Mode.SRC_ATOP);
            } else {
                mMaskDrawable.clearColorFilter();
            }
        }
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void attachToContainer(ViewGroup container) {
        container.addView(this);
    }

    public void detachFromContainer() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
    }

    public void setMaskOverlay(View view) {
        Bitmap bitmap = ViewHelper.cloneViewAsBitmap(view);
        setMaskDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    public void setMaskOverlay(int color) {
        setMaskDrawable(new ColorDrawable(color));
    }

    public static MaskView makeMaskView(Context context) {
        MaskView maskView = new MaskView(context);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, ViewGroup.MarginLayoutParams.MATCH_PARENT);
        maskView.setLayoutParams(layoutParams);
        return maskView;
    }

}
