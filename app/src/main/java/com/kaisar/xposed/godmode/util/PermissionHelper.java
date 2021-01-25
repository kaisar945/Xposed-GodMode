package com.kaisar.xposed.godmode.util;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.kaisar.xposed.godmode.injection.util.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;

public class PermissionHelper {

    private final static int REQUEST_PERMISSION_CODE = 1;

    private final WeakReference<Activity> mActivityReference;

    public PermissionHelper(Activity activity) {
        mActivityReference = new WeakReference<>(activity);
    }

    public void applyPermissions(String... permissions) {
        try {
            Activity activity = Objects.requireNonNull(mActivityReference.get(), "Activity can't be null");
            ArrayList<String> unauthorizedPermissionList = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    unauthorizedPermissionList.add(permission);
                }
            }
            ActivityCompat.requestPermissions(activity, unauthorizedPermissionList.toArray(new String[0]), REQUEST_PERMISSION_CODE);
        } catch (Throwable e) {
            Logger.e(TAG, e.getMessage(), e);
        }
    }

    public boolean checkSelfPermission(String permission) {
        Activity activity = Objects.requireNonNull(mActivityReference.get(), "Activity can't be null");
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

}
