package com.viewblocker.jrsen.injection.util;

import android.graphics.Bitmap;

public final class CommonUtils {
    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
