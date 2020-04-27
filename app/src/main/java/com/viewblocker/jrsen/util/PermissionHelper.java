package com.viewblocker.jrsen.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

/**
 * Android 6.0 上权限分为<b>正常</b>和<b>危险</b>级别
 * <ul>
 * <li>正常级别权限：开发者仅仅需要在AndroidManifext.xml上声明，那么应用就会被允许拥有该权限，如：android.permission.INTERNET</li>
 * <li>危险级别权限：开发者需要在AndroidManifext.xml上声明，并且在运行时进行申请，而且用户允许了，应用才会被允许拥有该权限，如：android.permission.WRITE_EXTERNAL_STORAGE</li>
 * </ul>
 * 有米的以下权限需要在Android6.0上被允许，有米广告sdk才能正常工作，开发者需要在调用有米的任何代码之前，提前让用户允许权限
 * <ul>
 * <li>必须申请的权限
 * <ul>
 * <li>android.permission.READ_PHONE_STATE</li>
 * <li>android.permission.WRITE_EXTERNAL_STORAGE</li>
 * </ul>
 * </li>
 * <li>可选申请的权限
 * <ul>
 * <li>android.permission.ACCESS_FINE_LOCATION</li>
 * </ul>
 * </li>
 * </ul>
 * Android 6.0+ 权限申请助手
 *
 * @since 2016-01-12
 */
public class PermissionHelper {

    private static final String TAG = "PermissionHelper";

    private final static int REQUEST_PERMISSION_CODE = 1;
    private final static int REQUEST_OPEN_APPLICATION_SETTINGS_CODE = 12345;

    /**
     * 有米 Android SDK 所需要向用户申请的权限列表
     */
    private PermissionModel[] mPermissionModels = new PermissionModel[]{
            new PermissionModel("存储空间", Manifest.permission.WRITE_EXTERNAL_STORAGE, "我们需要您允许我们读写你的存储卡，以方便我们临时保存一些数据"),
    };

    private SoftReference<Activity> mActivity;


    public PermissionHelper(Activity activity) {
        mActivity = new SoftReference<>(activity);
    }

    /**
     * 这里我们演示如何在Android 6.0+上运行时申请权限
     */
    public void applyPermissions() {
        Activity activity = mActivity.get();
        if (activity == null) return;
        try {
            ArrayList<String> permissions = new ArrayList<>();
            for (final PermissionModel model : mPermissionModels) {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, model.permission))
                    permissions.add(model.permission);
            }
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSION_CODE);
        } catch (Throwable e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * 对应Activity的 {@code onRequestPermissionsResult(...)} 方法
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        final Activity activity = mActivity.get();
        if (activity == null) return;
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                // 如果用户不允许，我们视情况发起二次请求或者引导用户到应用页面手动打开
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // 二次请求，表现为：以前请求过这个权限，但是用户拒接了
                    // 在二次请求的时候，会有一个“不再提示的”checkbox
                    // 因此这里需要给用户解释一下我们为什么需要这个权限，否则用户可能会永久不在激活这个申请
                    // 方便用户理解我们为什么需要这个权限
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                                .setTitle("权限申请")
                                .setMessage(findPermissionExplain(permissions[i]))
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                applyPermissions();
                                            }

                                        }
                                );
                        builder.setCancelable(false);
                        builder.show();
                    }
                    // 到这里就表示已经是第3+次请求，而且此时用户已经永久拒绝了，这个时候，我们引导用户到应用权限页面，让用户自己手动打开
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("权限申请")
                                .setMessage("请在打开的窗口的权限中开启" + findPermissionName(permissions[i]) + "权限，以正常使用本应用")
                                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                openApplicationSettings(REQUEST_OPEN_APPLICATION_SETTINGS_CODE);
                                            }
                                        }
                                ).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                activity.finish();
                                            }
                                        }
                                );
                        builder.setCancelable(false);
                        builder.show();
                    }
                    return;
                }
            }
        }
    }

    /**
     * 对应Activity的 {@code onActivityResult(...)} 方法
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OPEN_APPLICATION_SETTINGS_CODE
                && !isAllRequestedPermissionGranted()) {
            Activity activity = mActivity.get();
            if (activity != null) activity.finish();
        }
    }

    /**
     * 判断是否所有的权限都被授权了
     *
     * @return
     */
    public boolean isAllRequestedPermissionGranted() {
        Activity activity = mActivity.get();
        if (activity == null) return false;
        for (PermissionModel model : mPermissionModels) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, model.permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 打开应用设置界面
     *
     * @param requestCode 请求码
     * @return
     */
    private boolean openApplicationSettings(int requestCode) {
        Activity activity = mActivity.get();
        if (activity == null) return false;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()));
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            // Android L 之后Activity的启动模式发生了一些变化
            // 如果用了下面的 Intent.FLAG_ACTIVITY_NEW_TASK ，并且是 startActivityForResult
            // 那么会在打开新的activity的时候就会立即回调 onActivityResult
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivityForResult(intent, requestCode);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    /**
     * 查找申请权限的解释短语
     *
     * @param permission 权限
     * @return
     */
    private String findPermissionExplain(String permission) {
        if (mPermissionModels != null) {
            for (PermissionModel model : mPermissionModels) {
                if (model != null && model.permission != null && model.permission.equals(permission)) {
                    return model.explain;
                }
            }
        }
        return null;
    }

    /**
     * 查找申请权限的名称
     *
     * @param permission 权限
     * @return
     */
    private String findPermissionName(String permission) {
        if (mPermissionModels != null) {
            for (PermissionModel model : mPermissionModels) {
                if (model != null && model.permission != null && model.permission.equals(permission)) {
                    return model.name;
                }
            }
        }
        return null;
    }

    private static class PermissionModel {

        /**
         * 权限名称
         */
        public String name;

        /**
         * 请求的权限
         */
        public String permission;

        /**
         * 解析为什么请求这个权限
         */
        public String explain;

        public PermissionModel(String name, String permission, String explain) {
            this.name = name;
            this.permission = permission;
            this.explain = explain;
        }
    }

}
