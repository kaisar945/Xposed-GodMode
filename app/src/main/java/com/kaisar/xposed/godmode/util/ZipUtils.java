package com.kaisar.xposed.godmode.util;

import com.kaisar.xposed.godmode.injection.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by jrsen on 17-12-13.
 */

public final class ZipUtils {

    public static void compress(OutputStream out, String... filePaths) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (String filePath : filePaths) {
                File file = new File(filePath);
                ZipEntry e = new ZipEntry(file.getName());
                zipOut.putNextEntry(e);
                try (FileInputStream in = new FileInputStream(file)) {
                    FileUtils.copy(in, zipOut);
                }
                zipOut.closeEntry();
            }
            zipOut.flush();
        }
    }

    public static void uncompress(InputStream in, String destPath) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(in)) {
            for (ZipEntry e; (e = zipIn.getNextEntry()) != null; ) {
                File file = new File(destPath, e.getName());
                try (FileOutputStream out = new FileOutputStream(file)) {
                    FileUtils.copy(zipIn, out);
                }
                zipIn.closeEntry();
            }
        }
    }

}
