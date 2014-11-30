/*
 * Copyright © 2014 Jeff Corcoran
 *
 * This file is part of Hangar.
 *
 * Hangar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hangar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hangar.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package ca.mimic.apphangar;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TasksDataSource {

    // Database fields
    private static SQLiteDatabase database;
    private static Tasks dbHelper;
    private static TasksDataSource sInstance;
    private String[] allColumns = { Tasks.COLUMN_ID,
            Tasks.COLUMN_NAME, Tasks.COLUMN_PACKAGENAME,
            Tasks.COLUMN_CLASSNAME, Tasks.COLUMN_SECONDS, Tasks.COLUMN_TIMESTAMP, Tasks.COLUMN_BLACKLISTED, Tasks.COLUMN_LAUNCHES, Tasks.COLUMN_ORDER, Tasks.COLUMN_WIDGET_ORDER };

    private TasksDataSource(Context context) {
        if (dbHelper == null) {
            dbHelper = new Tasks(context);
        }
    }

    public static synchronized TasksDataSource getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TasksDataSource(context.getApplicationContext());
        }
        return sInstance;
    }

    public void open() throws SQLException {
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
            database = null;
        }
    }

    public TasksModel createTask(String name, String packagename, String classname,
            String timestamp) {
        synchronized (this) {
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
    }

    public int addSeconds(String name, int seconds) {
        synchronized (this) {
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
    }

//    public int getSeconds(String name) {
//        TasksModel task = getTask(name);
//        if (task != null) {
//            return task.getSeconds();
//        }
//        return 0;
//    }

    public int increaseLaunch(String name) {
        synchronized (this) {
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
    }

    public int getHighestSeconds() {
        synchronized (this) {
            //Cursor cursor = database.query(Tasks.TABLE_TASKS,
            //        allColumns, Tasks.COLUMN_SECONDS + " = (SELECT MAX(seconds))",
            //        null, null, null, null);
            Cursor cursor = database.query(Tasks.TABLE_TASKS, new String[]{"MAX(" + Tasks.COLUMN_SECONDS + ")"}, Tasks.COLUMN_BLACKLISTED + " = " + 0, null, null, null, null);
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
    }

    public int getHighestLaunch() {
        synchronized (this) {
            Cursor cursor = database.query(Tasks.TABLE_TASKS, new String[]{"MAX(" + Tasks.COLUMN_LAUNCHES + ")"}, Tasks.COLUMN_BLACKLISTED + " = " + 0, null, null, null, null);
            try {
                while (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
            return 0;
        }
    }

    public void deleteTask(TasksModel task) {
        synchronized (this) {
            long id = task.getId();
            System.out.println("Tasks deleted with id: " + id);
            database.delete(Tasks.TABLE_TASKS, Tasks.COLUMN_ID
                    + " = " + id, null);
        }
    }

    public void deletePackageName(String packageName) {
        synchronized (this) {
            System.out.println("Tasks deleted with id: " + packageName);
            database.delete(Tasks.TABLE_TASKS, Tasks.COLUMN_PACKAGENAME
                    + " = \"" + packageName + "\"", null);
        }
    }

    public TasksModel getTask(String name) {
        synchronized (this) {
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
    }

    public void resetTaskStats(TasksModel task) {
        synchronized (this) {
            ContentValues args = new ContentValues();
            args.put(Tasks.COLUMN_LAUNCHES, 0);
            args.put(Tasks.COLUMN_SECONDS, 0);
            database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_ID
                    + " = " + task.getId(), null);
        }
    }

    public void blacklistTask(TasksModel task, boolean blacklisted) {
        synchronized (this) {
            ContentValues args = new ContentValues();
            args.put(Tasks.COLUMN_BLACKLISTED, blacklisted ? 1 : 0);
            database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_ID
                    + " = " + task.getId(), null);
        }
    }

    public List<TasksModel> getBlacklisted() {
        synchronized (this) {
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
    }

    public int updateTaskTimestamp(String name) {
        synchronized (this) {
            ContentValues args = new ContentValues();
            Date date = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            args.put(Tasks.COLUMN_TIMESTAMP, dateFormatter.format(date));
            return database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_PACKAGENAME
                    + " = '" + name + "'", null);
        }
    }

    public int setOrder(String name, int order, boolean widget) {
        synchronized (this) {
            ContentValues args = new ContentValues();
            if (widget) {
                args.put(Tasks.COLUMN_WIDGET_ORDER, order);
            } else {
                args.put(Tasks.COLUMN_ORDER, order);
            }
            return database.update(Tasks.TABLE_TASKS, args, Tasks.COLUMN_PACKAGENAME
                    + " = '" + name + "'", null);
        }
    }

//    public int setOrder(String name, int order) {
//        return setOrder(name, order, false);
//    }

    public void blankOrder(boolean widget) {
        synchronized (this) {
            ContentValues args = new ContentValues();
            if (widget) {
                args.put(Tasks.COLUMN_WIDGET_ORDER, 0);
            } else {
                args.put(Tasks.COLUMN_ORDER, 0);
            }
            database.update(Tasks.TABLE_TASKS, args, null, null);
        }
    }

    public List<TasksModel> getPinnedTasks(ArrayList<String> pinnedApps, int sortType) {
        synchronized (this) {
            if (pinnedApps == null)
                return null;
            List<TasksModel> tasks = new ArrayList<TasksModel>();

            String sortString = null;
            switch (sortType) {
                case 0:
                    sortString = Tasks.COLUMN_SECONDS + " DESC";
                    break;
                case 1:
                    sortString = "lower(" + Tasks.COLUMN_NAME + ")";
                    break;
                case 2:
                    sortString = Tasks.COLUMN_TIMESTAMP + " DESC";
                    break;
            }

            Cursor cursor = database.query(Tasks.TABLE_TASKS,
                    allColumns, Tasks.COLUMN_BLACKLISTED + " = " + 0 + filterPinned(pinnedApps, false),
                    null, null, null, sortString, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                try {
                    TasksModel task = cursorToTasks(cursor);
                    tasks.add(task);
                    cursor.moveToNext();
                } catch (ParseException e) {
                    Tools.HangarLog("getPinnedTasks parse error [" + e + "]");
                }
            }
            // make sure to close the cursor
            cursor.close();
            if (sortType == 3) {
                List<TasksModel> addedPins = new ArrayList<TasksModel>();

                for (int i = pinnedApps.size() - 1; i >= 0; i--) {
                    String packageName = pinnedApps.get(i);
                    for (TasksModel task : tasks) {
                        if (task.getPackageName().equals(packageName)) {
                            addedPins.add(task);
                        }
                    }
                }
                return addedPins;
            }
            return tasks;
        }
    }

    public List<TasksModel> getAllTasks(int limit, ArrayList<String> pinnedApps) {
        synchronized (this) {
            List<TasksModel> tasks = new ArrayList<TasksModel>();

            Cursor cursor = database.query(Tasks.TABLE_TASKS,
                    allColumns, limit == 0 ? null : Tasks.COLUMN_BLACKLISTED + " = " + 0 + filterPinned(pinnedApps, true),
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
    }

    public List<TasksModel> getAllTasks(int limit) {
        return getAllTasks(limit, null);
    }

    public String filterPinned(ArrayList<String> appList, boolean omit) {
        String pinnedApps = "";
        if (appList == null) {
            return pinnedApps;
        }
        for (String app : appList) {
            pinnedApps += Tasks.COLUMN_PACKAGENAME + " " + (omit ? "!" : "") +
                    "= \"" + app + (omit ? "\" and " : "\" or ");
        }
        return " and (" + pinnedApps.replaceAll(omit ? " and $" : " or $", "") + ")";
    }

    public List<TasksModel> getOrderedTasks(int limit, boolean widget, ArrayList<String> pinnedApps) {
        synchronized (this) {
            List<TasksModel> tasks = new ArrayList<TasksModel>();

            Cursor cursor;
            if (widget) {
                cursor = database.query(Tasks.TABLE_TASKS,
                        allColumns, Tasks.COLUMN_WIDGET_ORDER + " > 0 or (" +
                                Tasks.COLUMN_WIDGET_ORDER + " = 0 and " +
                                Tasks.COLUMN_ORDER + " > 0)" +
                                (limit == 0 ? null : " and " + Tasks.COLUMN_BLACKLISTED + " = " + 0 + filterPinned(pinnedApps, true)),
                        null, null, null, Tasks.COLUMN_WIDGET_ORDER + " desc, " + Tasks.COLUMN_ORDER + " desc",
                        limit == 0 ? null : Integer.toString(limit));
            } else {
                cursor = database.query(Tasks.TABLE_TASKS,
                        allColumns, Tasks.COLUMN_ORDER + " > 0" + (limit == 0 ? null : " and " + Tasks.COLUMN_BLACKLISTED + " = " + 0 + filterPinned(pinnedApps, true)),
                        null, null, null, Tasks.COLUMN_ORDER + " desc",
                        limit == 0 ? null : Integer.toString(limit));
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                try {
                    TasksModel task = cursorToTasks(cursor);
                    tasks.add(task);
                    cursor.moveToNext();
                } catch (ParseException e) {
                    Tools.HangarLog("getOrderedTasks parse error [" + e + "]");
                }
            }
            // make sure to close the cursor
            cursor.close();
            return tasks;
        }
    }

//    public List<TasksModel> getOrderedTasks(int limit, boolean widget) {
//        return getOrderedTasks(limit, widget, null);
//    }

//    public List<TasksModel> getOrderedTasks(int limit) {
//        return getOrderedTasks(limit, false);
//    }

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
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Timestamp ts = new Timestamp(dateFormatter.parse(timeStamp).getTime());
        task.setTimestamp(ts);
        task.setBlacklisted(cursor.getInt(6) == 1);
        task.setLaunches(cursor.getInt(7));
        task.setOrder(cursor.getInt(8));
        task.setWidgetOrder(cursor.getInt(9));
        return task;
    }
}