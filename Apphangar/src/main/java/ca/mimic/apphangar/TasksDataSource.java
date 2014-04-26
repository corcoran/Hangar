package ca.mimic.apphangar;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TasksDataSource {

    // Database fields
    private SQLiteDatabase database;
    private Tasks dbHelper;
    private String[] allColumns = { Tasks.COLUMN_ID,
            Tasks.COLUMN_NAME, Tasks.COLUMN_PACKAGENAME,
            Tasks.COLUMN_CLASSNAME, Tasks.COLUMN_SECONDS, Tasks.COLUMN_TIMESTAMP, Tasks.COLUMN_BLACKLISTED, Tasks.COLUMN_LAUNCHES };

    public TasksDataSource(Context context) {
        dbHelper = new Tasks(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public TasksModel createTask(String name, String packagename, String classname,
            String timestamp) {
        ContentValues values = new ContentValues();
        values.put(Tasks.COLUMN_NAME, name);
        values.put(Tasks.COLUMN_PACKAGENAME, packagename);
        values.put(Tasks.COLUMN_CLASSNAME, classname);
        values.put(Tasks.COLUMN_TIMESTAMP, timestamp);
        long insertId = database.insert(Tasks.TABLE_TASKS, null,
                values);
        Cursor cursor = database.query(Tasks.TABLE_TASKS,
                allColumns, Tasks.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        TasksModel newTasks;
        try {
            newTasks = cursorToTasks(cursor);
            cursor.close();
            return newTasks;
        } catch (ParseException e) {
            Tools.HangarLog("createTask timestamp parse error [" + e + "]");
        }

        return null;
    }

    public int addSeconds(String name, int seconds) {
        try {
            TasksModel task = getTask(name);
            long id = task.getId();
            database.execSQL("UPDATE " + Tasks.TABLE_TASKS + " SET "
                    + Tasks.COLUMN_SECONDS + " = " + Tasks.COLUMN_SECONDS + " +" + seconds + " WHERE "
                    + Tasks.COLUMN_ID + " = " + id);
            return task.getSeconds() + seconds;
        } catch (Exception e) {
            Tools.HangarLog("Exception for addSeconds [" + name + "]");
            e.printStackTrace();
        }
        return 0;
    }

    public int getSeconds(String name) {
        TasksModel task = getTask(name);
        if (task != null) {
            return task.getSeconds();
        }
        return 0;
    }

    public int increaseLaunch(String name) {
        try {
            TasksModel task = getTask(name);
            long id = task.getId();
            database.execSQL("UPDATE " + Tasks.TABLE_TASKS + " SET "
                    + Tasks.COLUMN_LAUNCHES + " = " + Tasks.COLUMN_LAUNCHES + " +1 WHERE "
                    + Tasks.COLUMN_ID + " = " + id);
            return task.getLaunches() + 1;
        } catch (Exception e) {
            Tools.HangarLog("Exception for increaseLaunch [" + name + "]");
        }
        return 0;
    }

    public int getHighestSeconds() {
        //Cursor cursor = database.query(Tasks.TABLE_TASKS,
        //        allColumns, Tasks.COLUMN_SECONDS + " = (SELECT MAX(seconds))",
        //        null, null, null, null);
        Cursor cursor = database.query(Tasks.TABLE_TASKS, new String [] {"MAX(" + Tasks.COLUMN_SECONDS + ")"}, Tasks.COLUMN_BLACKLISTED + " = " + 0, null, null, null, null);
        // Tools.HangarLog("getHighestSeconds cursor:" + cursor.getInt(0));
        try {
            while (cursor.moveToFirst()) {
                return cursor.getInt(0);
                //TasksModel task = cursorToTasks(cursor);
                //Tools.HangarLog("highest Seconds [" + task.getName() + "] s[" + task.getSeconds() + "]");
                //return task.getSeconds();
            }
        //} catch (ParseException e) {
        //    Tools.HangarLog("getHighestSeconds parse error [" + e + "]");
        } finally {
            cursor.close();
        }
        return 0;
    }

    public int getHighestLaunch() {
        Cursor cursor = database.query(Tasks.TABLE_TASKS, new String [] {"MAX(" + Tasks.COLUMN_LAUNCHES + ")"}, Tasks.COLUMN_BLACKLISTED + " = " + 0, null, null, null, null);
        try {
            while (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        return 0;
    }

    public void deleteTask(TasksModel task) {
        long id = task.getId();
        System.out.println("Tasks deleted with id: " + id);
        database.delete(Tasks.TABLE_TASKS, Tasks.COLUMN_ID
                + " = " + id, null);
    }

    public TasksModel getTask(String name) {
        Cursor cursor = database.query(Tasks.TABLE_TASKS,
                allColumns, Tasks.COLUMN_PACKAGENAME + " = '" + name + "'", null, null, null, null);
        try {
            while (cursor.moveToFirst()) {
                TasksModel task = cursorToTasks(cursor);
                return task;
            }
        } catch (ParseException e) {
            Tools.HangarLog("getTask parse error [" + e + "]");
        } finally {
            cursor.close();
        }
        return null;
    }

    public void resetTaskStats(TasksModel task) {
        ContentValues args = new ContentValues();
        args.put(Tasks.COLUMN_LAUNCHES, 0);
        args.put(Tasks.COLUMN_SECONDS, 0);
        database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_ID
                + " = " + task.getId(), null);
    }

    public void blacklistTask(TasksModel task, boolean blacklisted) {
        ContentValues args = new ContentValues();
        args.put(Tasks.COLUMN_BLACKLISTED, blacklisted ? 1 : 0);
        database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_ID
                + " = " + task.getId(), null);
    }

    public List<TasksModel> getBlacklisted() {
        List<TasksModel> tasks = new ArrayList<TasksModel>();

        Cursor cursor = database.query(Tasks.TABLE_TASKS,
                allColumns, Tasks.COLUMN_BLACKLISTED + " = " + 1,
                null, null, null, Tasks.COLUMN_TIMESTAMP + " DESC");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            try {
                TasksModel task = cursorToTasks(cursor);
                tasks.add(task);
                cursor.moveToNext();
            } catch (ParseException e) {
                Tools.HangarLog("blacklistTask parse error [" + e + "]");
            }
        }
        // make sure to close the cursor
        cursor.close();
        return tasks;
    }

    public int updateTaskTimestamp(String name) {
        ContentValues args = new ContentValues();
        Date date = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        args.put("timestamp", dateFormatter.format(date));
        return database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_PACKAGENAME
                + " = '" + name + "'", null);
    }

    public int updateOrder(String name, int order) {
        ContentValues args = new ContentValues();
        args.put("timestamp", dateFormatter.format(date));
        return database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_PACKAGENAME
                + " = '" + name + "'", null);
    }

    public List<TasksModel> getAllTasks(int limit) {
        List<TasksModel> tasks = new ArrayList<TasksModel>();

        Cursor cursor = database.query(Tasks.TABLE_TASKS,
                allColumns, limit == 0 ? null : Tasks.COLUMN_BLACKLISTED + " = " + 0,
                null, null, null, Tasks.COLUMN_TIMESTAMP + " DESC",
                limit == 0 ? null : Integer.toString(limit));

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            try {
                TasksModel task = cursorToTasks(cursor);
                tasks.add(task);
                cursor.moveToNext();
            } catch (ParseException e) {
                Tools.HangarLog("getAllTasks parse error [" + e + "]");
            }
        }
        // make sure to close the cursor
        cursor.close();
        return tasks;
    }

    public List<TasksModel> getAllTasks() {
        return getAllTasks(0);
    }
    private TasksModel cursorToTasks(Cursor cursor) throws ParseException {
        TasksModel task = new TasksModel();
        task.setId(cursor.getLong(0));
        task.setName(cursor.getString(1));
        task.setPackageName(cursor.getString(2));
        task.setClassName(cursor.getString(3));
        task.setSeconds(cursor.getInt(4));
        String timeStamp = cursor.getString(5);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timestamp ts = new Timestamp(dateFormatter.parse(timeStamp).getTime());
        task.setTimestamp(ts);
        task.setBlacklisted(cursor.getInt(6) == 1);
        task.setLaunches(cursor.getInt(7));
        return task;
    }
}