package com.viewblocker.jrsen.util;

import android.graphics.Bitmap;
import android.graphics.Color;

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

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by jrsen on 17-11-22.
 */

public final class QRCodeFactory {

    public static Bitmap encodeQRBitmap(String data, int width, int height) {
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
        }
        return null;
    }

    public static String decodeQRBitmap(Bitmap qrcode) {
        try {
            int width = qrcode.getWidth();
            int height = qrcode.getHeight();
            int[] pixels = new int[width * height];
            qrcode.getPixels(pixels, 0, width, 0, 0, width, height);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (NotFoundException ignore) {
        }
        return "";
    }
}
