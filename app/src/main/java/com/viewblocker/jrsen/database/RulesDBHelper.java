package com.viewblocker.jrsen.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jrsen on 17-10-17.
 */

public final class RulesDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "rules";
    private static final int DB_VERSION = 1;

    private static RulesDBHelper _instance;

    public static RulesDBHelper getInstance(Context context) {
        synchronized (RulesDBHelper.class) {
            if (_instance == null)
                _instance = new RulesDBHelper(context);
        }
        return _instance;
    }

    private RulesDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();
            String CREATE_VIEW_RULES_TABLE =
                    String.format("CREATE TABLE %s(%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, %s TEXT NOT NULL, %s INTEGER, %s INTEGER, %s INTEGER ,%s INTEGER, %s TEXT, %s TEXT, %s TEXT , %s TEXT, %s INTEGER);"
                            , ViewRulesTable.TABLE_NAME, ViewRulesTable.COLUMN_ID
                            , ViewRulesTable.COLUMN_PACKAGE_NAME, ViewRulesTable.COLUMN_ACTIVITY_CLASS_NAME
                            , ViewRulesTable.COLUMN_VIEW_X, ViewRulesTable.COLUMN_VIEW_Y
                            , ViewRulesTable.COLUMN_VIEW_WIDTH, ViewRulesTable.COLUMN_VIEW_HEIGHT
                            , ViewRulesTable.COLUMN_VIEW_THUMBNAIL, ViewRulesTable.COLUMN_VIEW_CLASS_NAME
                            , ViewRulesTable.COLUMN_VIEW_HIERARCHY_DEPTH, ViewRulesTable.COLUMN_VIEW_RESOURCE_NAME
                            , ViewRulesTable.COLUMN_VIEW_VISIBILITY);
            db.execSQL(CREATE_VIEW_RULES_TABLE);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
