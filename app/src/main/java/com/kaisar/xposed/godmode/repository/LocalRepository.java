package com.kaisar.xposed.godmode.repository;

import static com.kaisar.xposed.godmode.GodModeApplication.TAG;
import static com.kaisar.xposed.godmode.injection.util.CommonUtils.recycleNullableBitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;

import com.google.gson.Gson;
import com.kaisar.xposed.godmode.GodModeApplication;
import com.kaisar.xposed.godmode.IObserver;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.injection.util.FileUtils;
import com.kaisar.xposed.godmode.injection.util.Logger;
import com.kaisar.xposed.godmode.rule.AppRules;
import com.kaisar.xposed.godmode.rule.ViewRule;
import com.kaisar.xposed.godmode.util.ZipUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LocalRepository {

    private static final String MANIFEST = "manifest.json";
    private static final String PACK_SUFFIX = ".gzip";

    public static void addObserver(String packageName, IObserver observer) {
        GodModeManager.getDefault().addObserver(packageName, observer);
    }

    public static AppRules loadAppRules() {
        return GodModeManager.getDefault().getAllRules();
    }

    public static boolean exportRules(String savePath, List<ViewRule> viewRules) {
        ArrayList<ViewRule> viewRuleList = new ArrayList<>(viewRules.size());
        ArrayList<String> filePathList = new ArrayList<>();
        // Save rule preview image
        for (ViewRule viewRule : viewRules) {
            ViewRule viewRuleCopy = viewRule.clone();
            Bitmap bitmap = GodModeManager.getDefault().openImageFileBitmap(viewRule.imagePath);
            if (bitmap != null) {
                try {
                    File file = new File(GodModeApplication.getApplication().getCacheDir(), System.currentTimeMillis() + ".webp");
                    try (FileChannel outChannel = new FileOutputStream(file).getChannel()) {
                        InputStream inputStream = Bitmap2IS(bitmap);
                        byte[] buffer = new byte[inputStream.available()];
                        inputStream.read(buffer);
                        outChannel.write(byte2Byffer(buffer));
                        filePathList.add(file.getPath());
                        viewRuleCopy.imagePath = file.getName();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            viewRuleList.add(viewRuleCopy);
        }
        // Write manifest config
        try {
            File manifestFile = new File(savePath, MANIFEST);
            FileUtils.stringToFile(manifestFile, new JSONArray(viewRuleList).toString());
            filePathList.add(manifestFile.getPath());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            ViewRule viewRule = viewRules.get(0);
            String filename = String.format("%s(%s)-%s%s", viewRule.label, viewRule.matchVersionName, sdf.format(new Date()), PACK_SUFFIX);
            String zipFile = new File(savePath, filename).getAbsolutePath();
            return ZipUtils.compress(zipFile, filePathList.toArray(new String[0]));
        } catch (IOException e) {
            Logger.e(TAG, "Write manifest file fail", e);
            return false;
        } finally {
            for (String filepath : filePathList) {
                FileUtils.delete(filepath);
            }
        }
    }

    public static ByteBuffer byte2Byffer(byte[] byteArray) {
        ByteBuffer buffer=ByteBuffer.allocate(byteArray.length);
        buffer.put(byteArray);
        buffer.flip();
        return buffer;
    }

    private static InputStream Bitmap2IS(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream sbs = new ByteArrayInputStream(baos.toByteArray());
        return sbs;
    }

    public static boolean importRules(String rulesFile) throws IOException {
        File cacheDir = GodModeApplication.getApplication().getCacheDir();
        ArrayList<String> filePathList = new ArrayList<>();
        if (ZipUtils.uncompress(rulesFile, cacheDir.getAbsolutePath())) {
            try {
                File manifestFile = new File(cacheDir, MANIFEST);
                filePathList.add(manifestFile.getPath());
                if (manifestFile.exists()) {
                    String json = FileUtils.readTextFile(manifestFile, 0, null);
                    try {
                        JSONArray jsonArray = new JSONArray(json);
                        Gson gson = new Gson();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            String itemJson = jsonArray.optString(i);
                            ViewRule viewRule = gson.fromJson(itemJson, ViewRule.class);
                            String imagePath = new File(cacheDir, viewRule.imagePath).getPath();
                            filePathList.add(imagePath);
                            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                            GodModeManager.getDefault().writeRule(viewRule.packageName, viewRule, bitmap);
                            recycleNullableBitmap(bitmap);
                        }
                        return true;
                    } catch (JSONException e) {
                        return false;
                    }
                }
            } finally {
                for (String filepath : filePathList) {
                    FileUtils.delete(filepath);
                }
            }
        }
        return false;
    }
}
