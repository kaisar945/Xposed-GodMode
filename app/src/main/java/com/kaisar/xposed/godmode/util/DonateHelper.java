package com.kaisar.xposed.godmode.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.kaisar.xposed.godmode.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class DonateHelper {

    public static void showDonateDialog(final Context context) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        startAliPayDonate(context);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        startWxPayDonate(context);
                        break;
                }
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.thanks)
                .setMessage(R.string.donate_thanks)
                .setPositiveButton(R.string.dialog_btn_alipay, listener)
                .setNegativeButton(R.string.dialog_btn_wxpay, listener)
                .create();
        dialog.show();
    }

    public static void startAliPayDonate(Context context) {
        try {
            String uri = "intent://platformapi/startapp?saId=10000007&" +
                    "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2FFKX06346HLWYNVPJ8VMW65%3F_s" +
                    "%3Dweb-other&_t=1472443966571#Intent;" +
                    "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
            Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
            context.startActivity(intent);
        } catch (Throwable ignored) {
            showQRCodeDialog(context, R.mipmap.qrcode_alipay, "com.eg.android.AlipayGphone");
        }
    }

    public static void startWxPayDonate(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage("com.tencent.mm");
            context.startActivity(intent);
            saveQrcodeImage2Gallery(context, R.mipmap.qrcode_wxpay);
            Toast.makeText(context, R.string.wechat_donate_tips, Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            showQRCodeDialog(context, R.mipmap.qrcode_wxpay, "com.tencent.mm");
        }
    }

    private static void saveQrcodeImage2Gallery(Context context, int imageResId) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageResId);
        File pictureDir = new File(Environment.getExternalStorageDirectory(), "Screenshots");
        File imageFile = new File(pictureDir, "donate_qrcode.jpg");
        if (pictureDir.exists() || pictureDir.mkdirs()) {
            FileOutputStream out = new FileOutputStream(imageFile);
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
            } finally {
                out.close();
            }
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)));
        }
    }

    private static void showQRCodeDialog(final Context context, int resId, final String targetPackageName) {
        try {
            saveQrcodeImage2Gallery(context, resId);
        } catch (Exception ignored) {
//            ignored.printStackTrace();
        }
        FrameLayout frameLayout = new FrameLayout(context);
        int padding = (int) context.getResources().getDimension(R.dimen.qrcode_layout_padding);
        frameLayout.setPadding(padding, padding, padding, padding);
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(resId);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager pm = context.getPackageManager();
                Intent intent = pm.getLaunchIntentForPackage(targetPackageName);
                try {
                    Preconditions.checkNotNull(intent);
                    context.startActivity(intent);
                } catch (Exception ignored) {
//                    ignored.printStackTrace();
                }
            }
        });
        frameLayout.addView(imageView);
        new AlertDialog.Builder(context)
                .setTitle(R.string.tips_qrcord)
                .setView(frameLayout)
                .setPositiveButton(R.string.thanks, null)
                .show();
    }

}
