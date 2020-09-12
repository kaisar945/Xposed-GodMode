package com.viewblocker.jrsen.util;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.Optional;

/**
 * Simple static methods to be called at the start of your own methods to verify
 * correct arguments and state.
 */
public final class Preconditions {

    public static boolean checkBitmap(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }

    public static boolean checkBitmapOrThrow(Bitmap bitmap) {
        if (!checkBitmap(bitmap)) {
            throw new NullPointerException("bitmap is null or recycled!");
        }
        return true;
    }

    /**
     * Ensures that an string reference passed as a parameter to the calling
     * method is not empty.
     *
     * @param string an string reference
     * @return the string reference that was validated
     * @throws IllegalArgumentException if {@code string} is empty
     */
    public static @NonNull
    <T extends CharSequence> T checkStringNotEmpty(final T string) {
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
    public static @NonNull
    <T> T checkNotNull(final T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * Checks that the specified object reference is not {@code null} and
     * throws a customized {@link NullPointerException} if it is. This method
     * is designed primarily for doing parameter validation in methods and
     * constructors with multiple parameters, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar, Baz baz) {
     *     this.bar = Objects.requireNonNull(bar, "bar must not be null");
     *     this.baz = Objects.requireNonNull(baz, "baz must not be null");
     * }
     * </pre></blockquote>
     *
     * @param reference     the object reference to check for nullity
     * @param message detail message to be used in the event that a {@code
     *                NullPointerException} is thrown
     * @param <T> the type of the reference
     * @return {@code reference} if not {@code null}
     * @throws NullPointerException if {@code reference} is {@code null}
     */
    public static <T> T checkNotNull(T reference, String message) {
        if (reference == null)
            throw new NullPointerException(message);
        return reference;
    }

    public static <T> T optionDefault(T reference, T defaultValue){
        if (reference == null)
            return defaultValue;
        return reference;
    }

    public static CharSequence optionDefault(CharSequence reference, CharSequence defaultValue){
        if (TextUtils.isEmpty(reference))
            return defaultValue;
        return reference;
    }

}
