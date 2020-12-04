package com.viewblocker.jrsen.injection;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XposedHelpers;

public final class ViewCompat {

    public static void setVisibility(View view, int visibility) {
        try {
            view.setVisibility(visibility);
        } catch (Exception e) {
            // 已知有些控件重写该方法禁止设置控件的显示
            // eg:https://cs.android.com/android/platform/superproject/+/master:packages/apps/Dialer/java/com/android/dialer/widget/DialerFloatingActionButton.java;l=74?q=DialerFloatingActionButton
            XposedHelpers.callMethod(view, "setFlags", visibility, 0x0000000C);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Drawable background = view.getBackground();
                if (background != null) background.setVisible(visibility == View.VISIBLE, false);
            }
        }
    }
}
