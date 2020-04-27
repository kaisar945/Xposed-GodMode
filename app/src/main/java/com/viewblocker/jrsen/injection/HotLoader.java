package com.viewblocker.jrsen.injection;

import android.os.Process;
import android.util.Log;

import com.viewblocker.jrsen.BuildConfig;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 开发更新模块无需重启 需要在xposed_init指定
 */
public final class HotLoader implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            String dexPath = Process.myUid() == Process.SYSTEM_UID ? getDexPathFromSystemFile() : getDexPathFromPackageService();
            Log.i("JRSEN", "hot load dex path:" + dexPath + " uid=" + Process.myUid());
            PathClassLoader classLoader = new PathClassLoader(dexPath, ClassLoader.getSystemClassLoader());
            Class<?> Clazz = classLoader.loadClass(BlockerInjector.class.getName());
            IXposedHookLoadPackage injector = (IXposedHookLoadPackage) Clazz.newInstance();
            injector.handleLoadPackage(lpparam);
        } catch (Throwable t) {
            Log.e("JRSEN", "loader exception", t);
        }
    }

    private String getDexPathFromSystemFile() {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new FileReader("/data/system/packages.xml"));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String nodeName = parser.getName();
                    if ("package".equals(nodeName) && BuildConfig.APPLICATION_ID.equals(parser.getAttributeValue(null, "name"))) {
                        String codePath = parser.getAttributeValue(null, "codePath");
                        File[] dexFiles = new File(codePath).listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return pathname.getName().endsWith(".apk");
                            }
                        });
                        return dexFiles[0].getAbsolutePath();
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Can't resolve %s dex path'", BuildConfig.APPLICATION_ID), e);
        }
        throw new RuntimeException(String.format("Can't resolve %s dex path'", BuildConfig.APPLICATION_ID));
    }

    private String getDexPathFromPackageService() throws IOException {
        java.lang.Process process = Runtime.getRuntime().exec("pm path " + BuildConfig.APPLICATION_ID);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = reader.readLine();
        if (output == null || output.split(":").length != 2) {
            throw new RuntimeException(String.format("Can't resolve %s dex path'", BuildConfig.APPLICATION_ID));
        }
        return output.trim().split(":")[1];
    }

}
