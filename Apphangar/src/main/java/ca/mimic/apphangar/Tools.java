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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Tools {
    final static String TAG = "Apphangar";

    protected static void HangarLog(String message) {
        if (BuildConfig.BUILD_TYPE.equals("debug"))
            Log.d(TAG, message);
    }

    protected boolean isPinned(Context context, String packageName) {
        ArrayList<String> appList = getPinned(context);
        for (String app : appList) {
            if (app.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    protected ArrayList<String> getPinned(Context context) {
        SharedPreferences settingsPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_MULTI_PROCESS);
        String pinnedApps = settingsPrefs.getString(Settings.PINNED_APPS, "");

        return new ArrayList<String>(Arrays.asList(pinnedApps.split(" ")));
    }

    protected boolean togglePinned(Context context, String packageName) {
        SharedPreferences settingsPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor settingsEditor = settingsPrefs.edit();

        ArrayList<String> appList = getPinned(context);

        Boolean removed = false;
        String pinnedApps = "";

        for (String app : appList) {
            if (app.equals(packageName)) {
                removed = true;
                continue;
            }
            pinnedApps += app + " ";
        }
        if (!removed) {
            pinnedApps += packageName;
        }

        settingsEditor.putString(Settings.PINNED_APPS, pinnedApps.trim());
        settingsEditor.apply();

        return !removed;
    }


    protected static String getLauncher(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        if (!"android".equals(res.activityInfo.packageName)) {
            return res.activityInfo.packageName;
        }
        return null;
    }

    public static int dpToPx(Context context, int dp) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    protected static void updateWidget(Context mContext) {
        Intent i = new Intent(mContext, StatsWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        mContext.sendBroadcast(i);

        i = new Intent(mContext, AppsWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        mContext.sendBroadcast(i);
    }

    protected static class TaskInfoOrder {
        int launchOrder;
        float launchScore;
        int secondsOrder;
        float secondsScore;
        int placeOrder;

        TaskInfo origTask;
        TaskInfoOrder(TaskInfo task) {
            origTask = task;
        }
        TaskInfo getOrig() {
            return origTask;
        }
    }

    protected static class TaskInfo {
        protected String appName = "";
        protected String packageName = "";
        protected String className = "";
        protected int launches = 0;
        protected int seconds = 0;
        protected int totalseconds = 0;

        TaskInfo (String string) {
            packageName = string;
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    protected static class AppRowComparator implements Comparator<AppsRowItem> {
        final int TIME_SPENT = 0;
        final int ALPHABETICAL = 1;

        final int PINNED = 0;
        final int BLACKLISTED = 1;
        final int NONE = 2;

        int mTopType;
        int mSortType;

        AppRowComparator (int topType, int sortType){
            mTopType = topType;
            mSortType = sortType;
        }
        @Override
        public int compare(AppsRowItem r1, AppsRowItem r2) {
            int firstCompare = 0;

            switch (mTopType) {
                case PINNED:
                    firstCompare = r2.getPinned().compareTo(r1.getPinned());
                    break;
                case BLACKLISTED:
                    firstCompare = r2.getBlacklisted().compareTo(r1.getBlacklisted());
                    break;
                case NONE:
                    break;
            }
            if (firstCompare == 0) {
                switch (mSortType) {
                    case ALPHABETICAL:
                        return r1.getName().compareToIgnoreCase(r2.getName());
                    case TIME_SPENT:
                        Integer o1 = r1.getSeconds();
                        Integer o2 = r2.getSeconds();
                        return o2.compareTo(o1);

                }
            }
            return firstCompare;
        }
    }

    protected static class TasksModelComparator implements Comparator<TasksModel> {
        String mType = "seconds";
        TasksModelComparator(String type) {
            mType = type;
        }
        @Override
        public int compare(TasksModel t1, TasksModel t2) {
            Integer o1 = 0;
            Integer o2 = 0;
            if (mType.equals("seconds")) {
                o1 = t1.getSeconds();
                o2 = t2.getSeconds();
            }
            int firstCompare = o2.compareTo(o1);
            if (firstCompare == 0) {
                return t1.getBlacklisted().compareTo(t2.getBlacklisted());
            }
            return firstCompare;
        }
    }
    protected static class TaskComparator implements Comparator<TaskInfoOrder>
    {
        private String mType;
        private int weightPriority;
        private Float numToCompare;
        private Float baseRecency;

        public TaskComparator (String type, int weight, int num){
            mType = type;
            weightPriority = weight;
            Float calNum = num / 10f;
            baseRecency = (calNum < 2.5) ? 2.5f : calNum;
            numToCompare = baseRecency + 1.5f;
            HangarLog("num: " + num + " calNum: " + calNum + " baseRecency: " + baseRecency + " numToCompare: " + numToCompare);

        }
        public int compare(TaskInfoOrder c1, TaskInfoOrder c2)
        {
            Float a1;
            Float a2;
            Float c1p = c1.placeOrder * baseRecency;
            Float c2p = c2.placeOrder * baseRecency;
            if (mType.equals("launch")) {
                a1 = c1.launchScore;
                a2 = c2.launchScore;
            } else if (mType.equals("seconds")) {
                a1 = c1.secondsScore;
                a2 = c2.secondsScore;
            } else {
                switch (weightPriority) {
                    case 1:
                        a1 = (float) c1.secondsOrder + c1.launchOrder + c1.placeOrder * numToCompare;
                        a2 = (float) c2.secondsOrder + c2.launchOrder + c2.placeOrder * numToCompare;
                        break;
                    case 2:
                        a1 = (float) c1.secondsOrder + (c1.launchOrder * numToCompare) + c1p;
                        a2 = (float) c2.secondsOrder + (c2.launchOrder * numToCompare) + c2p;
                        break;
                    case 3:
                        a1 = (c1.secondsOrder * numToCompare) + c1.launchOrder + c1p;
                        a2 = (c2.secondsOrder * numToCompare) + c2.launchOrder + c2p;
                        break;
                    default:
                        a1 = (float) c1.secondsOrder + c1.launchOrder + c1p;
                        a2 = (float) c2.secondsOrder + c2.launchOrder + c2p;
                }
            }

            return a2.compareTo(a1);
        }
    }

    public Boolean isInArray(ArrayList<String> list, String str) {
        for (String curVal : list){
            if (curVal.equals(str)) {
                return true;
            }
        }
        return false;
    }

    protected synchronized ArrayList<TaskInfo> reorderTasks(ArrayList<TaskInfo> taskList, TasksDataSource db, int weightPriority, boolean widget) {
        Tools.HangarLog("reorderTasks: " + taskList.size() + " widget? " + widget);
        int highestSeconds = db.getHighestSeconds();
        int highestLaunch = db.getHighestLaunch();
        int count = 1;
        int subtractor = taskList.size() + 1;

        ArrayList<TaskInfoOrder> taskListE = new ArrayList<TaskInfoOrder>();

        for (TaskInfo task : taskList) {
            TaskInfoOrder newTask = new TaskInfoOrder(task);

            int taskSeconds = task.totalseconds > 0 ? task.totalseconds : 1;

            newTask.launchScore = (float) task.launches / highestLaunch * 10;
            newTask.placeOrder = subtractor - count;
            newTask.secondsScore = (float) taskSeconds / highestSeconds * 10;

            taskListE.add(newTask);

            count ++;
        }

        Collections.sort(taskListE, new TaskComparator("launch", weightPriority, taskList.size()));
        int c = 0;
        for (int i=taskListE.size()-1; i >= 0; i--) {
            taskListE.get(c).launchOrder = (i + 1);
            c++;
        }

        c = 0;
        Collections.sort(taskListE, new TaskComparator("seconds", weightPriority, taskList.size()));
        for (int i=taskListE.size()-1; i >= 0; i--) {
            taskListE.get(c).secondsOrder = (i + 1);
            c++;
        }

        Collections.sort(taskListE, new TaskComparator("final", weightPriority, taskList.size()));
        taskList.clear();
        int order = taskListE.size();
        db.blankOrder(widget);
        for (TaskInfoOrder taskE : taskListE) {
            taskList.add(taskE.getOrig());
            db.setOrder(taskE.getOrig().packageName, order, widget);
            order--;
        }
        return taskList;
    }

    protected ArrayList<TaskInfo> reorderTasks(ArrayList<TaskInfo> taskList, TasksDataSource db, int weightPriority) {
        return reorderTasks(taskList, db, weightPriority, false);
    }

    protected synchronized static void reorderWidgetTasks(TasksDataSource db, Context context) {
        SharedPreferences settingsPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_MULTI_PROCESS);
        SharedPreferences widgetPrefs = context.getSharedPreferences("AppsWidget", Context.MODE_MULTI_PROCESS);

        boolean weightedRecents = widgetPrefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        int weightPriority = Integer.parseInt(widgetPrefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));

        boolean wR = settingsPrefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        int wP = Integer.parseInt(settingsPrefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));

        HangarLog("reorderWidgetTasks wR: " + wR + " wP: " + wP + " weightPriority: " + weightPriority + " weightedRecents: " + weightedRecents);
        if ((weightedRecents && !wR) || (weightedRecents && wP != weightPriority)) {
            ArrayList<TaskInfo> appList = buildTaskList(context, db, Settings.TASKLIST_QUEUE_LIMIT);
            new Tools().reorderTasks(appList, db, weightPriority, true);
        } else {
            db.blankOrder(true);
        }
    }

    protected static ArrayList<String> getBlacklisted(TasksDataSource db) {
        ArrayList<String> blPNames = new ArrayList<String>();
        List<TasksModel> blTasks = db.getBlacklisted();
        for (TasksModel task : blTasks) {
            blPNames.add(task.getPackageName());
        }
        blPNames.add("com.android.systemui");
        blPNames.add("com.android.phone");
        blPNames.add("com.android.packageinstaller");
        return blPNames;
    }

    protected static boolean isBlacklistedOrBad(String packageName, Context context, TasksDataSource db) {
        try {
            PackageManager pkgm = context.getPackageManager();
            Intent intent = pkgm.getLaunchIntentForPackage(packageName);
            if (intent == null)
                throw new PackageManager.NameNotFoundException();

        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
        for (String blTask : getBlacklisted(db)) {
            if (packageName.equals(blTask)) {
                return true;
            }
        }
        return false;
    }

    protected synchronized static ArrayList<Tools.TaskInfo> buildTaskList(Context context, TasksDataSource db,
                                                             int queueSize, boolean weighted,
                                                             boolean widget) {
        ArrayList<Tools.TaskInfo> taskList = new ArrayList<Tools.TaskInfo>();
        List<TasksModel> tasks;
        ArrayList<String> pinnedApps = new ArrayList<String>();

        SharedPreferences settingsPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_MULTI_PROCESS);
        int pinnedSort = Integer.parseInt(settingsPrefs.getString(Settings.PINNED_SORT_PREFERENCE, Integer.toString(Settings.PINNED_SORT_DEFAULT)));
        boolean ignorePinned = settingsPrefs.getBoolean(Settings.IGNORE_PINNED_PREFERENCE, Settings.IGNORE_PINNED_DEFAULT);

        if (!ignorePinned)
            pinnedApps = new Tools().getPinned(context);

        Tools.HangarLog("buildTaskList queueSize: " + queueSize + " weighted: " + weighted);

        if (queueSize == 0) {
            // queueSize 0 gets pinnedTasks.
            if (pinnedApps.size() == 0) {
                return null;
            }
            tasks = db.getPinnedTasks(pinnedApps, pinnedSort);
        } else if (weighted) {
            tasks = db.getOrderedTasks(queueSize, widget, pinnedApps);
        } else {
            tasks = db.getAllTasks(queueSize, pinnedApps);
        }

        for (TasksModel taskM : tasks) {
            String taskPackage = taskM.getPackageName();

            if (isBlacklistedOrBad(taskPackage, context, db))
                continue;

            Tools.TaskInfo dbTask = new Tools.TaskInfo(taskPackage);
            dbTask.appName = taskM.getName();
            dbTask.className = taskM.getClassName();
            dbTask.launches = taskM.getLaunches();
            dbTask.totalseconds = taskM.getSeconds();

            try {
                PackageManager pkgm = context.getPackageManager();
                pkgm.getApplicationInfo(taskPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                db.deleteTask(taskM);
                continue;
            }

            taskList.add(dbTask);
        }
        return taskList;
    }
    protected static ArrayList<Tools.TaskInfo> buildTaskList(Context context, TasksDataSource db, int queueSize) {
        return buildTaskList(context, db, queueSize, false, false);
    }

    protected static ArrayList<Tools.TaskInfo> buildPinnedList(Context context, TasksDataSource db) {
        return buildTaskList(context, db, 0, false, false);
    }

    protected ArrayList<Tools.TaskInfo> getPinnedTasks (Context context, ArrayList<Tools.TaskInfo> pinnedList, ArrayList<Tools.TaskInfo> taskList, int count) {
        SharedPreferences settingsPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_MULTI_PROCESS);
        int pinnedPlacement = Integer.parseInt(settingsPrefs.getString(Settings.PINNED_PLACEMENT_PREFERENCE, Integer.toString(Settings.PINNED_PLACEMENT_DEFAULT)));
        Tools.HangarLog("getPinnedTasks");

        if (pinnedList.size() > 0) {
            if (pinnedPlacement == Settings.PINNED_PLACEMENT_LEFT) {
                pinnedList.addAll(taskList);
                for (int i = pinnedList.size()-1;i >= count; i--) {
                    pinnedList.remove(i);
                }

                return pinnedList;
            } else {
                int index = count - pinnedList.size();
                if (index < 0) index = 0;
                try {
                    taskList.addAll(index, pinnedList);
                    for (int i = taskList.size()-1;i >= count; i--) {
                        taskList.remove(i);
                    }
                } catch (IndexOutOfBoundsException e) {
                    if (index > taskList.size()) {
                        if (pinnedList.size() >= taskList.size()) {
                            return pinnedList;
                        }
                        taskList.addAll(pinnedList);
                    }
                }
                return taskList;
            }
        }
        return taskList;
    }

}
