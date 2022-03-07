
package com.kaisar.xposed.godmode.util;

import static com.kaisar.xposed.godmode.injection.util.CommonUtils.recycleNullableBitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaisar.xposed.godmode.GodModeApplication;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.rule.ViewRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule backup util
 */
public final class BackupUtils {

    private static final int VERSION = 1;
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String PACK_SUFFIX = ".gzip";

    //Don't instantiate this class
    private BackupUtils() {
    }

    public static class BackupException extends Exception {
        public BackupException() {
        }

        public BackupException(String message) {
            super(message);
        }

        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }

        public BackupException(Throwable cause) {
            super(cause);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public BackupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    public static class RestoreException extends Exception {
        public RestoreException() {
        }

        public RestoreException(String message) {
            super(message);
        }

        public RestoreException(String message, Throwable cause) {
            super(message, cause);
        }

        public RestoreException(Throwable cause) {
            super(cause);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public RestoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    public static void backupRules(Uri toUri, String packageName, List<ViewRule> viewRules) throws BackupException {
        ArrayList<String> backupFilePathList = new ArrayList<>();
        ArrayList<ViewRule> backupViewRuleList = new ArrayList<>(viewRules.size());
        File backupDir = new File(GodModeApplication.getApplication().getCacheDir(), "backup");
        if (!backupDir.exists() || FileUtils.rmdir(backupDir.getPath())) {
            boolean ok = backupDir.mkdirs();
            if (!ok) throw new BackupException("Create backup directory failed.");
            try {
                for (ViewRule viewRule : viewRules) {
                    ViewRule viewRuleCopy = viewRule.clone();
                    ParcelFileDescriptor parcelFileDescriptor = GodModeManager.getDefault().openImageFileDescriptor(viewRule.imagePath);
                    if (parcelFileDescriptor != null) {
                        try (FileChannel inChannel = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel()) {
                            File file = new File(backupDir, System.currentTimeMillis() + ".webp");
                            try (FileChannel outChannel = new FileOutputStream(file).getChannel()) {
                                inChannel.transferTo(0, inChannel.size(), outChannel);
                                viewRuleCopy.imagePath = file.getName();
                                backupFilePathList.add(file.getPath());
                            }
                        }
                    }
                    backupViewRuleList.add(viewRuleCopy);
                }
                File manifestFile = new File(backupDir, MANIFEST_FILE);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("version", VERSION);
                jsonObject.addProperty("packageName", packageName);
                Gson gson = new GsonBuilder().create();
                JsonElement jsonElement = gson.toJsonTree(backupViewRuleList);
                jsonObject.add("rules", jsonElement);
                FileUtils.stringToFile(manifestFile, jsonObject.toString());
                backupFilePathList.add(manifestFile.getPath());
                OutputStream out = GodModeApplication.getApplication().getContentResolver().openOutputStream(toUri);
                ZipUtils.compress(out, backupFilePathList.toArray(new String[0]));
            } catch (IOException e) {
                throw new BackupException(e);
            } finally {
                FileUtils.rmdir(backupDir.getPath());
            }
        }
    }

    public static void restoreRules(Uri fromUri) throws RestoreException {
        File restoreDir = new File(GodModeApplication.getApplication().getCacheDir(), "restore");
        if (!restoreDir.exists() || FileUtils.rmdir(restoreDir.getPath())) {
            boolean ok = restoreDir.mkdirs();
            if (!ok) throw new RestoreException("Create restore directory failed.");
            try {
                InputStream in = GodModeApplication.getApplication().getContentResolver().openInputStream(fromUri);
                ZipUtils.uncompress(in, restoreDir.getPath());
                File manifestFile = new File(restoreDir, MANIFEST_FILE);
                if (!manifestFile.exists()) throw new RestoreException("Miss manifest.json file.");
                String json = FileUtils.readTextFile(manifestFile, 0, null);
                Gson gson = new GsonBuilder().create();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                int version = jsonObject.get("version").getAsInt();
                String packageName = jsonObject.get("packageName").getAsString();
                JsonArray jsonArray = jsonObject.getAsJsonArray("rules");
                for (int i = 0; i < jsonArray.size(); i++) {
                    String ruleJson = jsonArray.get(i).toString();
                    ViewRule viewRule = gson.fromJson(ruleJson, ViewRule.class);
                    String imagePath = new File(restoreDir, viewRule.imagePath).getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    GodModeManager.getDefault().writeRule(viewRule.packageName, viewRule, bitmap);
                    recycleNullableBitmap(bitmap);
                }
            } catch (IOException e) {
                throw new RestoreException(e);
            } finally {
                FileUtils.rmdir(restoreDir.getPath());
            }
        }
    }
}
