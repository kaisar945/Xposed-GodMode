package com.kaisar.xposed.godmode.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.util.Objects;

/**
 * Created by jrsen on 17-9-29.
 */

public final class Clipboard {

    public static boolean putContent(Context context, CharSequence text) {
        try {
            ClipboardManager clipboard = Objects.requireNonNull((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE));
            ClipData clip = ClipData.newPlainText(text, text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Throwable ignore) {
            //因为堆栈溢出trace信息很大无法set到剪切板
//            e.printStackTrace();
            return false;
        }
    }

}
