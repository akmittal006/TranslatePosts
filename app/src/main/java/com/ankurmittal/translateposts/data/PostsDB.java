package com.ankurmittal.translateposts.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ankurmittal.translateposts.DBHelper;

import java.util.ArrayList;

import rx.functions.Action1;

/**
 * Created by AnkurMittal2 on 13-12-2016.
 */

public class PostsDB implements Action1<Post>{

    private DBHelper dbHelper;
    private SQLiteDatabase db;

    public PostsDB(Context context) {
        dbHelper  = new DBHelper(context);

    }

    public int insert(Post post) {

        ContentValues values = new ContentValues();
        values.put(dbHelper.KEY_message, post.getMessage());
        values.put(dbHelper.KEY_translated_message, post.getTranslatedMessage());
        values.put(dbHelper.KEY_datetime, post.getDateTime());
        return (int)db.insert(dbHelper.TABLE, null, values);
    }

    public void deleteAll() {

        db.delete(dbHelper.TABLE,null,null);
    }

    public ArrayList<Post> getList() {
        String selectQuery =  "SELECT  *" +
                " FROM " + dbHelper.TABLE + " ORDER BY " + DBHelper.KEY_ID+ " DESC";

        ArrayList<Post> postsList = new ArrayList<Post>();

        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list

        if (cursor.moveToFirst()) {
            do {
                Post post = new Post();
                post.setId(cursor.getInt(cursor.getColumnIndex(dbHelper.KEY_ID)));
                post.setMessage(cursor.getString(cursor.getColumnIndex(dbHelper.KEY_message)));
                post.setTranslatedMessage(cursor.getString(cursor.getColumnIndex(dbHelper.KEY_translated_message)));
                post.setDateTime(cursor.getString(cursor.getColumnIndex(dbHelper.KEY_datetime)));
                postsList.add(post);

            } while (cursor.moveToNext());
        }

        cursor.close();
        return postsList;

    }

    public void open() {
        db = dbHelper.getWritableDatabase();

    }

    public void close() {
        db.close(); // Closing database connection

    }

    @Override
    public void call(Post post) {
        //update following post
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_translated_message,post.getTranslatedMessage());
        db.update(DBHelper.TABLE,values,DBHelper.KEY_ID + " = ?",new String[] {post.getId() + ""});
    }

    public boolean isOpen() {
        return db.isOpen();
    }

}
