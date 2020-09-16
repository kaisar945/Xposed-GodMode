package com.viewblocker.jrsen.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.viewblocker.jrsen.rule.ViewRule;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by jrsen on 17-11-22.
 */

public final class QRCodeFactory {

    public static Bitmap encode(ViewRule viewRule) {
        try {
            Objects.requireNonNull(viewRule.imagePath, "rule image path is null");
            ViewRule copy = (ViewRule) viewRule.clone();
            Bitmap image = BitmapFactory.decodeFile(copy.imagePath);
            try {
                copy.imagePath = null;
                String json = new Gson().toJson(copy);
                return generateQRImage(image, json);
            } finally {
                if (!image.isRecycled())
                    image.recycle();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Object[] decode(Bitmap bitmap) {
        try {
            Bitmap[] images = parseQRImage(bitmap);
            Bitmap image = images[0];
            Bitmap qrcode = images[1];
            try {
                String json = Objects.requireNonNull(QRCodeFactory.decodeQRBitmap(qrcode));
                ViewRule viewRule = new Gson().fromJson(json, ViewRule.class);
                return new Object[]{viewRule, image};
            } finally {
                if (qrcode != null) qrcode.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap generateQRImage(Bitmap image, String json) {
        try {
            int qrcodeSize = image.getWidth();
            Bitmap qrcode = encodeQRBitmap(json, qrcodeSize, qrcodeSize);
            Objects.requireNonNull(qrcode);
            try {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                int width = image.getWidth();
                int height = image.getHeight() + qrcode.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, image.getConfig());
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(image, 0, 0, paint);
                canvas.drawBitmap(qrcode, 0, canvas.getHeight(), paint);
                return bitmap;
            } finally {
                qrcode.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap[] parseQRImage(Bitmap mergedBitmap) {
        int qrcodeSize = mergedBitmap.getWidth();
        Bitmap image = Bitmap.createBitmap(mergedBitmap, 0, 0, mergedBitmap.getWidth(), mergedBitmap.getHeight() - mergedBitmap.getWidth());
        Bitmap qrcode = Bitmap.createBitmap(mergedBitmap, 0, mergedBitmap.getHeight() - mergedBitmap.getWidth(), qrcodeSize, qrcodeSize);
        return new Bitmap[]{image, qrcode};
    }

    private static Bitmap encodeQRBitmap(String data, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 0);
            int[] pixels = new int[width * height];
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints);
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException ignore) {
            return null;
        }
    }

    private static String decodeQRBitmap(Bitmap qrcode) {
        try {
            int width = qrcode.getWidth();
            int height = qrcode.getHeight();
            int[] pixels = new int[width * height];
            qrcode.getPixels(pixels, 0, width, 0, 0, width, height);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (NotFoundException ignore) {
            return "";
        }
    }
}
