package com.viewblocker.jrsen.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Pair;
import android.util.TypedValue;

import com.viewblocker.jrsen.BlockerApplication;
import com.viewblocker.jrsen.database.ViewRulesTable;
import com.viewblocker.jrsen.rule.ViewRule;

import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by jrsen on 17-11-22.`
 */

public final class ViewRuleFactory {

    public static Bitmap encodeViewRuleAsQRImage(String packageName, ViewRule rule) {
        try {
            String snapshotFilePath = Preconditions.checkStringNotEmpty(rule.snapshotFilePath);
            Bitmap thumbnail = BitmapFactory.decodeFile(snapshotFilePath);
            JSONObject jobj = new JSONObject();
            jobj.put(ViewRulesTable.COLUMN_PACKAGE_NAME, packageName);
            String alias = rule.alias != null ? new String(Base64.encode(rule.alias.getBytes(), Base64.DEFAULT)) : "";
            jobj.put(ViewRulesTable.COLUMN_ALIAS, alias);
            jobj.put(ViewRulesTable.COLUMN_ACTIVITY_CLASS_NAME, rule.activityClassName);
            jobj.put(ViewRulesTable.COLUMN_VIEW_X, rule.x);
            jobj.put(ViewRulesTable.COLUMN_VIEW_Y, rule.y);
            jobj.put(ViewRulesTable.COLUMN_VIEW_WIDTH, rule.width);
            jobj.put(ViewRulesTable.COLUMN_VIEW_HEIGHT, rule.height);
            jobj.put(ViewRulesTable.COLUMN_VIEW_CLASS_NAME, rule.viewClassName);
            jobj.put(ViewRulesTable.COLUMN_VIEW_HIERARCHY_DEPTH, Arrays.toString(rule.viewHierarchyDepth));
            jobj.put(ViewRulesTable.COLUMN_VIEW_RESOURCE_NAME, rule.resourceName);
            jobj.put(ViewRulesTable.COLUMN_VIEW_VISIBILITY, rule.visibility);
            int length = getQRCodeImageLength();
            Bitmap qrcode = QRCodeFactory.encodeQRBitmap(jobj.toString(), length, length);
            Bitmap image = mergeQRCodePreviewImage(thumbnail, qrcode);
            if (Preconditions.checkBitmap(thumbnail)) {
                thumbnail.recycle();
            }
            if (Preconditions.checkBitmap(qrcode)) {
                qrcode.recycle();
            }
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    public static Pair<String, ViewRule> decodeViewRuleFromQRImage(Bitmap bitmap) {
        try {
            int length = getQRCodeImageLength();
            if (Preconditions.checkBitmap(bitmap) && bitmap.getWidth() > length && bitmap.getHeight() > length) {
                Bitmap qrcode = Bitmap.createBitmap(bitmap, bitmap.getWidth() - length, bitmap.getHeight() - length, length, length);
                String json = Preconditions.checkStringNotEmpty(QRCodeFactory.decodeQRBitmap(qrcode));
                JSONObject obj = new JSONObject(json);
                String packageName = obj.optString(ViewRulesTable.COLUMN_PACKAGE_NAME);
                String alias = obj.optString(ViewRulesTable.COLUMN_ALIAS);
                alias = new String(Base64.decode(alias, Base64.DEFAULT));
                String actClassName = obj.optString(ViewRulesTable.COLUMN_ACTIVITY_CLASS_NAME);
                int x = obj.optInt(ViewRulesTable.COLUMN_VIEW_X);
                int y = obj.optInt(ViewRulesTable.COLUMN_VIEW_Y);
                int width = obj.optInt(ViewRulesTable.COLUMN_VIEW_WIDTH);
                int height = obj.optInt(ViewRulesTable.COLUMN_VIEW_HEIGHT);
                String viewClassName = obj.optString(ViewRulesTable.COLUMN_VIEW_CLASS_NAME);
                String viewHierarchyDepthStr = obj.optString(ViewRulesTable.COLUMN_VIEW_HIERARCHY_DEPTH);
                int[] viewHierarchyDepth = str2IntArray(viewHierarchyDepthStr);
                String resourceName = obj.optString(ViewRulesTable.COLUMN_VIEW_RESOURCE_NAME);
                int visibility = obj.optInt(ViewRulesTable.COLUMN_VIEW_VISIBILITY);
                long recordTimeStamp = System.currentTimeMillis();/*导入的规则以最后导入的时间计算*/
                return Pair.create(packageName, new ViewRule(null, alias, x, y, width
                        , height, actClassName, viewClassName, viewHierarchyDepth, resourceName, visibility, recordTimeStamp));
            }
        } catch (Exception ignore) {
//            ignore.printStackTrace();
        }
        return null;
    }

    private static int getQRCodeImageLength() {
        Resources res = BlockerApplication.getApplication().getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, res.getDisplayMetrics());
    }

    private static Bitmap mergeQRCodePreviewImage(Bitmap thumbnail, Bitmap qrcode) {
        if (Preconditions.checkBitmap(thumbnail) && Preconditions.checkBitmap(qrcode)) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Bitmap bitmap = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), thumbnail.getConfig());
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(thumbnail, 0, 0, paint);
            canvas.drawBitmap(qrcode, canvas.getWidth() - qrcode.getWidth(), canvas.getHeight() - qrcode.getHeight(), paint);
            return bitmap;
        }
        return null;
    }

    public static int[] str2IntArray(String str) {
        str = str.replace("[", "");
        str = str.replace("]", "");
        String[] split = str.split(", ");
        int[] array = new int[split.length];
        for (int i = 0; i < split.length; i++)
            array[i] = Integer.parseInt(split[i]);
        return array;
    }

}
