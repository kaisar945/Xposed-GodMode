package com.viewblocker.jrsen.injection.weiget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
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

    private static final int MARK_COLOR = Color.argb(150, 139, 195, 75);
    private static final int SELECT_COLOR = Color.argb(150, 255, 0, 0);
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

    public void updateBounds(int x, int y, int w, int h) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        layoutParams.width = w;
        layoutParams.height = h;
        layoutParams.leftMargin = x;
        layoutParams.topMargin = y;
        requestLayout();
    }

    public void updateBounds(Rect bounds) {
        updateBounds(bounds.left, bounds.top, bounds.width(), bounds.height());
    }

    private Rect bounds = new Rect();

    public Rect getRealBounds() {
        int[] out = new int[2];
        getLocationInWindow(out);
        int l = out[0];
        int t = out[1];
        int r = l + getWidth();
        int b = t + getHeight();
        bounds.set(l, t, r, b);
        return bounds;
    }

    public void setMarked(boolean enable) {
        if (isMarked != enable) {
            if (isMarked = enable) {
                getBackground().setColorFilter(MARK_COLOR, PorterDuff.Mode.SRC_ATOP);
            } else {
                getBackground().clearColorFilter();
            }
        }
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected != isSelected()) {
            super.setSelected(selected);
            if (selected) {
                setBackgroundColor(SELECT_COLOR);
            } else {
                setBackgroundColor(Color.TRANSPARENT);
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

    public void inflateView(View view) {
        Bitmap bitmap = ViewHelper.cloneViewAsBitmap(view);
        setBackground(new BitmapDrawable(getResources(), bitmap));
    }

    public static MaskView clone(View view) {
        MaskView maskView = new MaskView(view.getContext());
        maskView.inflateView(view);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(view.getWidth(), view.getHeight());
        int[] out = new int[2];
        view.getLocationOnScreen(out);
        layoutParams.leftMargin = out[0];
        layoutParams.topMargin = out[1];
        maskView.setLayoutParams(layoutParams);
        return maskView;
    }

    public static MaskView mask(View view) {
        MaskView maskView = new MaskView(view.getContext());
        maskView.setSelected(true);
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(view.getWidth(), view.getHeight());
        int[] out = new int[2];
        view.getLocationOnScreen(out);
        layoutParams.leftMargin = out[0];
        layoutParams.topMargin = out[1];
        maskView.setLayoutParams(layoutParams);
        return maskView;
    }

}
