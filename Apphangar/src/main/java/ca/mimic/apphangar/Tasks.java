package ca.mimic.apphangar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Tasks extends SQLiteOpenHelper {

    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CLASSNAME = "classname";
    public static final String COLUMN_PACKAGENAME = "packagename";
    public static final String COLUMN_SECONDS = "seconds";
    public static final String COLUMN_LAUNCHES = "launches";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_BLACKLISTED = "blacklisted";
    public static final String COLUMN_ORDER = "order";

    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 5;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_TASKS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null, " + COLUMN_CLASSNAME
            + " text not null, " + COLUMN_PACKAGENAME
            + " text not null, " + COLUMN_SECONDS
            + " integer not null default 0, " + COLUMN_LAUNCHES
            + " integer not null default 1, " + COLUMN_TIMESTAMP
            + " datetime not null default current_timestamp, " + COLUMN_BLACKLISTED
            + " bool not null default 0);";

    public Tasks(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(Tasks.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

}