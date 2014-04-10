package ca.mimic.apphangar;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Tools {
    protected static String getLauncher(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo == null) {
            // should not happen. A home is always installed, isn't it?
        }
        if ("android".equals(res.activityInfo.packageName)) {
            // No default selected
        } else {
            return res.activityInfo.packageName;
        }
        return null;
    }
    public static float pxToDp(Context context, float px){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
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
        protected float order = 0;
        protected int seconds = 0;
        protected int totalseconds = 0;

    }


    protected static TaskInfo getTask(String packageName, ArrayList<TaskInfo> taskList) {
        for (TaskInfo task : taskList) {
            if (task.packageName.equals(packageName)) {
                return task;
            }
        }
        return null;
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

        public TaskComparator (String type, int weight){
            this.mType = type;
            weightPriority = weight;
        }
        public int compare(TaskInfoOrder c1, TaskInfoOrder c2)
        {
            Float a1;
            Float a2;
            if (mType.equals("launch")) {
                a1 = c1.launchScore;
                a2 = c2.launchScore;
            } else if (mType.equals("seconds")) {
                a1 = c1.secondsScore;
                a2 = c2.secondsScore;
            } else {
                switch (weightPriority) {
                    case 1:
                        a1 = (float) c1.secondsOrder + c1.launchOrder + (c1.placeOrder * 2);
                        a2 = (float) c2.secondsOrder + c2.launchOrder + (c2.placeOrder * 2);
                        break;
                    case 2:
                        a1 = (float) c1.secondsOrder + (c1.launchOrder * 2) + c1.placeOrder;
                        a2 = (float) c2.secondsOrder + (c2.launchOrder * 2) + c2.placeOrder;
                        break;
                    case 3:
                        a1 = (float) (c1.secondsOrder * 2) + c1.launchOrder + c1.placeOrder;
                        a2 = (float) (c2.secondsOrder * 2) + c2.launchOrder + c2.placeOrder;
                        break;
                    default:
                        a1 = (float) c1.secondsOrder + c1.launchOrder + c1.placeOrder;
                        a2 = (float) c2.secondsOrder + c2.launchOrder + c2.placeOrder;
                }
            }

            return a2.compareTo(a1);
        }
    }


    protected static ArrayList<TaskInfo> reorderTasks(ArrayList<TaskInfo> taskList, TasksDataSource db, int weightPriority) {
        int highestSeconds = db.getHighestSeconds();
        int highestLaunch = db.getHighestLaunch();
        Log.d(Settings.TAG, "highest Launch [" + highestLaunch + "] Seconds [" + highestSeconds + "]");
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

        Collections.sort(taskListE, new TaskComparator("launch", weightPriority));
        int c = 0;
        for (int i=taskListE.size()-1; i >= 0; i--) {
            taskListE.get(c).launchOrder = (i + 1);
            c++;
        }

        c = 0;
        Collections.sort(taskListE, new TaskComparator("seconds", weightPriority));
        for (int i=taskListE.size()-1; i >= 0; i--) {
            taskListE.get(c).secondsOrder = (i + 1);
            c++;
        }

        Collections.sort(taskListE, new TaskComparator("final", weightPriority));
        taskList.clear();
        for (TaskInfoOrder taskE : taskListE) {
            Log.d(Settings.TAG, "task[" + taskE.getOrig().appName + "] l[" + taskE.launchOrder + "] p[" + taskE.placeOrder + "] s[" + taskE.secondsOrder + "]");
            taskList.add(taskE.getOrig());
        }
        return taskList;
    }

    protected static ArrayList<String> getBlacklisted(Context context, TasksDataSource db) {
        ArrayList<String> blPNames = new ArrayList<String>();
        List<TasksModel> blTasks = db.getBlacklisted();
        for (TasksModel task : blTasks) {
            blPNames.add(task.getPackageName());
        }
        blPNames.add("com.android.systemui");
        blPNames.add("com.android.phone");
        blPNames.add(context.getPackageName());
        blPNames.add("com.android.settings");
        blPNames.add("com.android.packageinstaller");
        return blPNames;
    }
}
