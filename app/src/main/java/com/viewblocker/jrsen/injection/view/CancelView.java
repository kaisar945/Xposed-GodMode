package com.viewblocker.jrsen.injection.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.viewblocker.jrsen.injection.annotation.DisableHook;

/**
 * Created by jrsen on 17-11-4.
 */

@DisableHook
@SuppressLint("AppCompatCustomView")
public final class CancelView extends View {

    private final Paint rectPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private CharSequence text = "噢 上帝，原谅这只愚蠢的土拨鼠吧！";
    private Rect statusBarBounds = new Rect();
    private Rect textLayoutBounds = new Rect();
    private Rect textBounds = new Rect();

    public CancelView(Context context) {
        super(context);
        initView();
    }

    public CancelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CancelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }


    private void initView() {
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(Color.argb(230, 139, 195, 75));
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, getResources().getDisplayMetrics()));
        textPaint.setColor(Color.WHITE);
        textPaint.getTextBounds(text.toString(), 0, text.length(), textBounds);
        textBounds.offsetTo(0, 0);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(lp);
    }

    public int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public void attachToContainer(ViewGroup container) {
        container.addView(this);
    }

    public void detachFromContainer() {
        ViewGroup parent = (ViewGroup) getParent();
        parent.removeView(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        //draw status bar rect
        canvas.drawRect(getStatusBarBounds(), rectPaint);
        Rect textLayoutBounds = getTextLayoutBounds();
        canvas.drawRect(textLayoutBounds, rectPaint);

        //draw text
        float x = textLayoutBounds.centerX() - textBounds.centerX();
        float y = textLayoutBounds.centerY() + textBounds.centerY();
        canvas.drawText(text, 0, text.length(), x, y, textPaint);
        canvas.restore();
    }

    private Rect getStatusBarBounds() {
        if (statusBarBounds.isEmpty())
            statusBarBounds.set(getLeft(), 0, getRight(), getStatusBarHeight());
        return statusBarBounds;
    }

    private Rect getTextLayoutBounds() {
        if (textLayoutBounds.isEmpty()) {
            TypedValue tv = new TypedValue();
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
                textLayoutBounds.set(getLeft(), getStatusBarHeight(), getRight(), getStatusBarHeight() + actionBarHeight);
            }
        }
        return textLayoutBounds;
    }

    public Rect getRealBounds() {
        return new Rect(statusBarBounds.left, statusBarBounds.top, statusBarBounds.right, textLayoutBounds.bottom);
    }
}
