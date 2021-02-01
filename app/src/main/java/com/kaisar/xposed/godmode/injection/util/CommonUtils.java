package com.kaisar.xposed.godmode.injection.util;

import android.graphics.Bitmap;

public final class CommonUtils {
    public static void recycleNullableBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
