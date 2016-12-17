package com.ankurmittal.translateposts;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by AnkurMittal2 on 13-12-2016.
 */


public class DBHelper  extends SQLiteOpenHelper  {
    //version number to upgrade database version
    //each time if you Add, Edit table, you need to change the
    //version number.
    private static final int DATABASE_VERSION = 2;

    // Database Name
    public static final String DATABASE_NAME = "translateposts.db";
    public static final String TABLE = "Posts";
    public static final String KEY_ID = "ID";
    public static final String KEY_message = "message";
    public static final String KEY_translated_message = "translated_message";
    public static final String KEY_datetime = "datetime";


    public DBHelper(Context context ) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //All necessary tables you like to create will create here

        String CREATE_TABLE_STUDENT = "CREATE TABLE " + TABLE  + "("
                + KEY_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + KEY_message + " TEXT, "
                + KEY_translated_message + " TEXT, "
                + KEY_datetime + " TEXT )";

        db.execSQL(CREATE_TABLE_STUDENT);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed, all data will be gone!!!
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);

        // Create tables again
        onCreate(db);

    }


}