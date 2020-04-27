package com.viewblocker.jrsen.injection.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import com.viewblocker.jrsen.injection.ViewHelper;
import com.viewblocker.jrsen.injection.annotation.DisableHook;

/**
 * Created by jrsen on 17-10-13.
 */

@DisableHook
@SuppressLint("AppCompatCustomView")
public final class MirrorView extends View {

    private int x, y;
    private boolean isMarked;
    private final Drawable drawable;

    private MirrorView(Context context, Drawable drawable) {
        super(context);
        this.drawable = drawable;
    }

    public void updatePosition(int newX, int newY) {
        x = newX;
        y = newY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(x, y);
        drawable.draw(canvas);
        canvas.restore();
    }

    public Rect getRealBounds() {
        Rect rect = new Rect(drawable.getBounds());
        rect.offsetTo(x, y);
        return rect;
    }

    public void setMarked(boolean enable) {
        isMarked = enable;
        if (enable) {
            int color = Color.argb(150, 139, 195, 75);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        } else {
            drawable.clearColorFilter();
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
        parent.removeView(this);
    }

    public static MirrorView clone(View view) {
        Bitmap bitmap = ViewHelper.cloneViewAsBitmap(view);
        Drawable drawable = new BitmapDrawable(bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());

        MirrorView mirrorView = new MirrorView(view.getContext(), drawable);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT);
        mirrorView.setLayoutParams(layoutParams);

        int[] out = new int[2];
        view.getLocationOnScreen(out);
        mirrorView.updatePosition(out[0], out[1]);
        return mirrorView;
    }

}
