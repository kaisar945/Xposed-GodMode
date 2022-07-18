package com.kaisar.xposed.godmode.util;

import android.os.Binder;
import android.os.Environment;

import com.kaisar.xposed.godmode.injection.util.Logger;

import java.io.File;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by fkj on 22-7-12.
 * System service get directory tool class
 */

public class SystemServerDirUtils {
    private static final String BASE_SERVER_DIR_NAME = "godmode";
    private final int BASE_SERVER_DIR_NAME_LENGTH = BASE_SERVER_DIR_NAME.length() + getGUID().length() + 1;
    private static File server_dir = null;

    private final Logger mLogger;

    public SystemServerDirUtils(Logger mLogger) {
        this.mLogger = mLogger;
    }

    private File systemDir() {
        File file;
        if (Binder.getCallingUid() == 2000) { // isShell
            file = new File(new File("/data/local/tmp"), "system");
            mLogger.i("Pick dir for Shell: " + file.getAbsolutePath());
            return file;
        }
        file = new File(Environment.getDataDirectory(), "system");
        mLogger.i("Pick dir for Root: " + file.getAbsolutePath());
        return file;
    }

    private File randomBaseServerDir() {
        return new File(systemDir(), BASE_SERVER_DIR_NAME + "_" + getGUID());
    }

    public File baseServerDir() {
        if (server_dir == null) {
            File findDir = findBaseServerDir();
            if (findDir == null) {
                server_dir = randomBaseServerDir();
                mLogger.i("Create random base server dir: " + server_dir.getAbsolutePath());
            } else {
                server_dir = findDir;
            }
        }
        return server_dir;
    }

    private File findBaseServerDir() {
        File[] files = systemDir().listFiles();
        if (files == null) {
            mLogger.e("Can't list files in system dir: " + systemDir().getAbsolutePath());
        }
        for (File file : files) {
            if (file.getName().startsWith(BASE_SERVER_DIR_NAME) && file.getName().length() == BASE_SERVER_DIR_NAME_LENGTH) {
                mLogger.i("Found base server dir: " + file.getAbsolutePath());
                return file;
            }
        }
        mLogger.i("No base server dir found");
        return null;
    }

    /**
     * Generate a 16 bit non repeating random number, including number + Case
     * @author https://blog.csdn.net/bingguang1993/article/details/103372383
     */
    private String getGUID() {
        StringBuilder uid = new StringBuilder();
        // Generate 16 bit strong random numbers
        Random rd = new SecureRandom();
        for (int i = 0; i < 16;) {
            // Generate a 3-bit random number of 0-2
            int type = rd.nextInt(3);
            switch (type){
                case 0:
                    // Random number of 0-9
                    uid.append(rd.nextInt(10));
                    break;
                case 1:
                    // ASCII is uppercase between 65-90, get uppercase random
                    uid.append((char)(rd.nextInt(25)+65));
                    break;
                case 2:
                    // ASCII is lowercase between 97-122, get lowercase random
                    uid.append((char)(rd.nextInt(25)+97));
                    break;
                default:
                    break;
            }
            i++;
        }
        return uid.toString();
    }

}
