package com.viewblocker.jrsen.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.viewblocker.jrsen.rule.ActRules;
import com.viewblocker.jrsen.rule.ViewRule;
import com.viewblocker.jrsen.util.ViewRuleFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jrsen on 17-10-19.
 */

public final class ViewRulesTable {

    static final String TABLE_NAME = "view_rules";

    static final String COLUMN_ID = "_id";
    public static final String COLUMN_PACKAGE_NAME = "package_name";
    public static final String COLUMN_ALIAS = "alias";
    public static final String COLUMN_ACTIVITY_CLASS_NAME = "activity_class_name";
    public static final String COLUMN_VIEW_X = "x";
    public static final String COLUMN_VIEW_Y = "y";
    public static final String COLUMN_VIEW_WIDTH = "width";
    public static final String COLUMN_VIEW_HEIGHT = "height";
    public static final String COLUMN_VIEW_THUMBNAIL = "thumbnail";
    public static final String COLUMN_VIEW_CLASS_NAME = "class_name";
    public static final String COLUMN_VIEW_HIERARCHY_DEPTH = "hierarchy_depth";
    public static final String COLUMN_VIEW_RESOURCE_NAME = "resource_name";
    public static final String COLUMN_VIEW_VISIBILITY = "visibility";

    public static HashMap<String, ActRules> getAppRules(Context context) {
        RulesDBHelper dbHelper = RulesDBHelper.getInstance(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        HashMap<String, ActRules> appRules = new HashMap<>();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        cursor.moveToFirst();
        cursor.moveToPrevious();
        while (cursor.moveToNext()) {
            int _id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            String packageName = cursor.getString(cursor.getColumnIndex(COLUMN_PACKAGE_NAME));
            String activityClassName = cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_CLASS_NAME));
            int x = cursor.getInt(cursor.getColumnIndex(COLUMN_VIEW_X));
            int y = cursor.getInt(cursor.getColumnIndex(COLUMN_VIEW_Y));
            int width = cursor.getInt(cursor.getColumnIndex(COLUMN_VIEW_WIDTH));
            int height = cursor.getInt(cursor.getColumnIndex(COLUMN_VIEW_HEIGHT));
            String viewThumbnailFilePath = cursor.getString(cursor.getColumnIndex(COLUMN_VIEW_THUMBNAIL));
            String viewClassName = cursor.getString(cursor.getColumnIndex(COLUMN_VIEW_CLASS_NAME));
            String viewHierarchyDepthText = cursor.getString(cursor.getColumnIndex(COLUMN_VIEW_HIERARCHY_DEPTH));
            int[] viewHierarchyDepth = ViewRuleFactory.str2IntArray(viewHierarchyDepthText);
            String resourceName = cursor.getString(cursor.getColumnIndex(COLUMN_VIEW_RESOURCE_NAME));
            int visibility = cursor.getInt(cursor.getColumnIndex(COLUMN_VIEW_VISIBILITY));

            List<ViewRule> viewRules;
            ActRules actRules = appRules.get(packageName);
            if (actRules != null) {
                viewRules = actRules.get(activityClassName);
                if (viewRules == null) {
                    viewRules = new ArrayList<>();
                    actRules.put(activityClassName, viewRules);
                }
            } else {
                viewRules = new ArrayList<>();
                actRules = new ActRules();
                actRules.put(activityClassName, viewRules);
                appRules.put(packageName, actRules);
            }
            viewRules.add(new ViewRule(viewThumbnailFilePath, null, x, y, width, height, activityClassName
                    , viewClassName, viewHierarchyDepth, resourceName, visibility, System.currentTimeMillis()));
        }
        cursor.close();
        db.close();
        return appRules;
    }

}
