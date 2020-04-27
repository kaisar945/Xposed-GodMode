package com.viewblocker.jrsen.util;

import android.os.Build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public final class DeviceCompat {

    public static boolean isXiaoMi() {
        return "Xiaomi".equalsIgnoreCase(Build.BRAND);
    }

    public static boolean isEnableSELinux() {
        try {
            Process proc = Runtime.getRuntime().exec("getenforce");
            if (proc.waitFor() == 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line = reader.readLine();
                return "Enforcing".equalsIgnoreCase(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void setSELinux(boolean enable) {
        try {
            Process su = Runtime.getRuntime().exec("su");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(su.getOutputStream()));
            writer.write("setenforce " + (enable ? "1" : "0"));
            writer.newLine();
            writer.write("exit");
            writer.newLine();
            writer.flush();
            su.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
