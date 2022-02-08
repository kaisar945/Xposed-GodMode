package com.kaisar.xposed.godmode.injection.hook;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.kaisar.xposed.godmode.injection.ViewHelper;
import com.kaisar.xposed.godmode.injection.util.Property;

import de.robv.android.xposed.XC_MethodHook;

public final class DisplayPropertiesHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {
    boolean mDebugLayout;

    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        if (mDebugLayout) {
            Canvas canvas = (Canvas) param.args[0];
            View view = (View) param.thisObject;
            if (!ViewHelper.TAG_GM_CMP.equals(view.getTag())) {
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                canvas.drawRect(new Rect(0, 0, view.getWidth(), view.getHeight()), paint);
                param.setResult(canvas);
            }
        }
    }

    @Override
    public void onPropertyChange(Boolean debugLayout) {
        mDebugLayout = debugLayout;
    }
}