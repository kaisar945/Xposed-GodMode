package com.kaisar.xposed.godmode.util;

import android.app.Activity;
import android.content.Intent;

public class SafHelper {
    public static void saveFile(Activity activity, String fileName, int code){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        activity.startActivityForResult(intent, code);
    }

    public static void openFile(Activity activity, int code) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        activity.startActivityForResult(intent, code);
    }
}
