package com.kaisar.xposed.godmode.injection.hook;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.LinearLayout;

import com.kaisar.xposed.godmode.injection.ViewHelper;
import com.kaisar.xposed.godmode.injection.util.Property;

import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class DisplayPropertiesHook extends XC_MethodHook implements Property.OnPropertyChangeListener<Boolean> {
    boolean mDebugLayout;
    MainObserver mainObserver = new MainObserver();

    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        if (param.thisObject instanceof LinearLayout) return;
//        if (param.thisObject instanceof ViewGroup) return;
        if (ViewHelper.TAG_GM_CMP.equals(((View) param.thisObject).getTag())) return;

        View view = (View) param.thisObject;

        Method method;
        Class<?> clazz = param.thisObject.getClass();
        while (true) {
            method = XposedHelpers.findMethodExactIfExists(clazz, "onDraw", Canvas.class);
            clazz = clazz.getSuperclass();
            if (clazz == null) break;
            if (method != null) break;
        }
        mainObserver.addObserver(new UpdateObserver(view) {
            @Override
            public void onDataChanged(View view) {
                view.post(view::invalidate);
            }
        });
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (mDebugLayout) {
                    Canvas canvas = (Canvas) (param.getResult() == null ? param.args[0] : param.getResult());
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
        });
    }

    @Override
    public void onPropertyChange(Boolean debugLayout) {
        mDebugLayout = debugLayout;
        mainObserver.setDebugLayout(mDebugLayout);
    }

    abstract static class UpdateObserver implements Observer {

        View view;

        UpdateObserver(View view) {
            this.view = view;
        }

        @Override
        public void update(Observable o, Object arg) {
            onDataChanged(view);
        }

        public abstract void onDataChanged(View view);
    }

    static class MainObserver extends Observable {
        public void setDebugLayout(boolean bool) {
            setChanged();
            this.notifyObservers(bool);
        }
    }
}