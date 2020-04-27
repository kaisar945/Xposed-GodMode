package com.viewblocker.jrsen.util;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Simple static methods to be called at the start of your own methods to verify
 * correct arguments and state.
 */
public final class Preconditions {

    public static boolean checkBitmap(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }

    /**
     * Ensures that an string reference passed as a parameter to the calling
     * method is not empty.
     *
     * @param string an string reference
     * @return the string reference that was validated
     * @throws IllegalArgumentException if {@code string} is empty
     */
    public static @NonNull <T extends CharSequence> T checkStringNotEmpty(final T string) {
        if (TextUtils.isEmpty(string)) {
            throw new IllegalArgumentException();
        }
        return string;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static @NonNull <T> T checkNotNull(final T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

}
