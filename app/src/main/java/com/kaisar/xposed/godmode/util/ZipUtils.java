package com.kaisar.xposed.godmode.util;

import com.kaisar.xposed.godmode.injection.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by jrsen on 17-12-13.
 */

public final class ZipUtils {

    private static final ZipInterface CMD_IMPL = new CmdZip();
    private static final ZipInterface JAVA_IMPL = new JavaZip();

    public static boolean compress(String zipFilePath, String... compressFilePath) {
        return CMD_IMPL.compress(zipFilePath, compressFilePath)
                || JAVA_IMPL.compress(zipFilePath, compressFilePath);
    }

    public static boolean uncompress(String zipFilePath, String targetDir) {
        return CMD_IMPL.uncompress(zipFilePath, targetDir)
                || JAVA_IMPL.uncompress(zipFilePath, targetDir);
    }

    private static class CmdZip implements ZipInterface {

        @Override
        public boolean compress(String zipFilePath, String... compressFilePaths) {
            try {
                StringBuilder cmdBuilder = new StringBuilder();
                cmdBuilder.append("zip -r -j");
                cmdBuilder.append(" ").append(zipFilePath);
                for (String filePath : compressFilePaths) {
                    cmdBuilder.append(" ").append(filePath);
                }
                return Runtime.getRuntime().exec(cmdBuilder.toString()).waitFor() == 0;
            } catch (InterruptedException | IOException e) {
                return false;
            }
        }

        @Override
        public boolean uncompress(String zipFilePath, String targetDir) {
            try {
                String command = String.format("unzip -d %s %s", targetDir, zipFilePath);
                return Runtime.getRuntime().exec(command).waitFor() == 0;
            } catch (InterruptedException | IOException e) {
                return false;
            }
        }
    }

    private static class JavaZip implements ZipInterface {

        @Override
        public boolean compress(String zipFilePath, String... compressFilePaths) {
            try {
                FileOutputStream out = new FileOutputStream(zipFilePath);
                ZipOutputStream zipOut = new ZipOutputStream(out);
                for (String filePath : compressFilePaths) {
                    File file = new File(filePath);
                    ZipEntry e = new ZipEntry(file.getName());
                    zipOut.putNextEntry(e);
                    FileInputStream in = new FileInputStream(file);
                    FileUtils.copy(in, zipOut);
                    in.close();
                    zipOut.closeEntry();
                }
                zipOut.flush();
                zipOut.close();
                return true;
            } catch (IOException e) {
                File zipFile = new File(zipFilePath);
                if (zipFile.exists())
                    zipFile.delete();
                return false;
            }
        }

        @Override
        public boolean uncompress(String zipFilePath, String targetDir) {
            try {
                FileInputStream in = new FileInputStream(zipFilePath);
                ZipInputStream zipIn = new ZipInputStream(in);
                for (ZipEntry e; (e = zipIn.getNextEntry()) != null; ) {
                    File file = new File(targetDir, e.getName());
                    FileOutputStream out = new FileOutputStream(file);
                    FileUtils.copy(zipIn, out);
                    out.close();
                    zipIn.closeEntry();
                }
                zipIn.closeEntry();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private interface ZipInterface {
        boolean compress(String zipFilePath, String... compressFilePaths);

        boolean uncompress(String zipFilePath, String targetDir);
    }
}
