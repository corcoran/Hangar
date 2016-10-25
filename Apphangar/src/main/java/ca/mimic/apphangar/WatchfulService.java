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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.mimic.apphangar.Tools.TaskInfo;
import ca.mimic.apphangar.Tools.LollipopTaskInfo;

public class WatchfulService extends Service {

    TasksDataSource db;

    SharedPreferences prefs;
    SharedPreferences widgetPrefs;
    PackageManager pkgm;
    PowerManager pm;

    static int failCount;
    static TaskInfo runningTask;
    static ArrayList<TaskInfo> pinnedList;
    static ArrayList<TaskInfo> taskList;
    static ArrayList<String> notificationTasks;
    static LollipopTaskInfo lollipopTaskInfo;

    // createNotification variables

    String taskPackage;

    int contLayout;
    int rowLayout;
    int imageButtonLayout;
    int imageContLayout;

    int numOfApps;
    int setPriority;
    boolean secondRow;
    boolean moreApps;
    int moreAppsPage = 1;
    int moreAppsPages;
    int pinnedCount;
    int iconSize;
    int itemLayout;
    String mIcon;
    String notificationBg;

    String launcherPackage = null;
    boolean isNotificationRunning;

    final int MAX_RUNNING_TASKS = 20;
    final int LOOP_SECONDS = 3;

    final int ICON_SIZE_SMALL = 0;
    final int ICON_SIZE_LARGE = 2;

    protected static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";

    Map<String, Integer> iconMap;

    Handler handler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return new IWatchfulService.Stub() {
            @Override
            public void createNotification() {
                WatchfulService.this.createNotification();
            }
            @Override
            public void destroyNotification() {
                WatchfulService.this.destroyNotification();
            }
            @Override
            public void buildTasks() {
                WatchfulService.this.buildTasks();
            }
            @Override
            public void buildReorderAndLaunch() {
                pinnedList = null;
                notificationTasks = null;
                moreAppsPage = 1;
                synchronized (WatchfulService.this) {
                    WatchfulService.this.buildReorderAndLaunch(true);
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (db == null) {
            db = TasksDataSource.getInstance(this);
            db.open();
            failCount = 0;
        } else {
            return;
        }
        Tools.HangarLog("starting up.. ");

        prefs = getSharedPreferences(getPackageName(), MODE_MULTI_PROCESS);
        widgetPrefs = getSharedPreferences("AppsWidget", Context.MODE_MULTI_PROCESS);

        taskPackage = this.getPackageName();

        imageButtonLayout = getResources().getIdentifier("imageButton", "id", taskPackage);
        imageContLayout = getResources().getIdentifier("imageCont", "id", taskPackage);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        iconMap = new HashMap<String, Integer>();
        iconMap.put(Settings.STATUSBAR_ICON_WHITE_WARM, R.drawable.ic_apps_warm);
        iconMap.put(Settings.STATUSBAR_ICON_WHITE_COLD, R.drawable.ic_apps_cold);
        iconMap.put(Settings.STATUSBAR_ICON_WHITE_BLUE, R.drawable.ic_apps_blue);
        iconMap.put(Settings.STATUSBAR_ICON_WHITE, R.drawable.ic_apps_white);
        iconMap.put(Settings.STATUSBAR_ICON_BLACK_WARM, R.drawable.ic_apps_warm_black);
        iconMap.put(Settings.STATUSBAR_ICON_BLACK_COLD, R.drawable.ic_apps_cold_black);
        iconMap.put(Settings.STATUSBAR_ICON_BLACK_BLUE, R.drawable.ic_apps_blue_black);
        iconMap.put(Settings.STATUSBAR_ICON_TRANSPARENT, R.drawable.ic_apps_transparent);

        setRefreshAlarm(getApplicationContext());
    }

    public static void setRefreshAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, BootStart.class);
        intent.setAction(Tools.REFRESH_ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1339,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + Tools.AWAKE_REFRESH,
                Tools.AWAKE_REFRESH,
                pi);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if action is a Multiple page trigger
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Settings.MORE_APPS_ACTION)) {
            moreAppsPage = moreAppsPage + 1;
            createNotification();
            return START_STICKY;
        }

        prefs = getSharedPreferences(getPackageName(), MODE_MULTI_PROCESS);
        widgetPrefs = getSharedPreferences("AppsWidget", Context.MODE_MULTI_PROCESS);
        pkgm = getPackageManager();
        launcherPackage = Tools.getLauncher(getApplicationContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        handler.removeCallbacks(scanApps);
        handler.post(scanApps);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Tools.HangarLog("onDestroy service..");
        handler.removeCallbacks(scanApps);
        db.close();
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    protected void buildReorderAndLaunch(boolean isToggled) {
        if (isToggled) {
            Tools.HangarLog("buildReorderAndLaunch isToggled");
            // Grab new taskList and check if this is a new install
            taskList = Tools.buildTaskList(getApplicationContext(), db, Settings.TASKLIST_QUEUE_LIMIT);
            Tools.HangarLog("BaseTask 0: " + taskList.get(0).packageName);
            Tools.HangarLog("BaseTask size: " + taskList.size());
            // TODO: This needs to be more elegant for L...
            if (taskList.size() == 0 ||
                    (taskList.size() == 1 && (taskList.get(0).packageName.equals(getPackageName()) || taskList.get(0).packageName.equals("com.android.settings")))) {
                if (Tools.isLollipop(false)) {
                    buildLBaseTasks();
                } else {
                    buildBaseTasks();
                }
                taskList = Tools.buildTaskList(getApplicationContext(), db, Settings.TASKLIST_QUEUE_LIMIT);
            }
            reorderAndLaunch();
        }
    }

    protected String getClassName(Context context, String taskPackage) {
        String className = null;
        ResolveInfo resolveInfo;
        try {
            resolveInfo = new Tools().cachedImageResolveInfo(context, taskPackage);
            className = resolveInfo.activityInfo.name;
        } catch (Exception e) {
        }
        return className;
    }

    @TargetApi(21)
    protected void buildLBaseTasks() {
        Context context = getApplicationContext();
        List<UsageStats> stats = Tools.getUsageStats(context);

        for (UsageStats task : stats) {
            String taskPackage = task.getPackageName();
            String taskClass = getClassName(context, taskPackage);
            Tools.HangarLog("buildLBaseTask: package: " + taskPackage + " class: " + taskClass);
            buildTaskInfo(taskClass, taskPackage);
        }
    }
    protected void buildBaseTasks() {
        // taskList is blank!  Populating db from apps in memory.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(MAX_RUNNING_TASKS);
        if (recentTasks != null && recentTasks.size() > 0) {
            for (ActivityManager.RunningTaskInfo recentTask : recentTasks) {
                ComponentName task = recentTask.baseActivity;
                try {
                    String taskClass = task.getClassName();
                    String taskPackage = task.getPackageName();

                    buildTaskInfo(taskClass, taskPackage);
                } catch (NullPointerException e) {
                }
            }
        }
    }

    protected void buildTaskInfo(String className, String packageName) {
        boolean isTaskBlacklisted = Tools.isBlacklistedOrBad(packageName, getApplicationContext(), db);
        if (className == null || className.equals("com.android.internal.app.ResolverActivity") ||
                isTaskBlacklisted ||
                packageName.equals(launcherPackage)) {
            Tools.HangarLog("buildTaskInfo -- task [" + packageName + "] is bad?  Bailing!");
            return;
        }
        runningTask = new TaskInfo(packageName);
        runningTask.className = className;
        try {
            ApplicationInfo appInfo = pkgm.getApplicationInfo(packageName, 0);
            runningTask.appName = appInfo.loadLabel(pkgm).toString();
            if (runningTask.appName.isEmpty()) {
                Tools.HangarLog("Can't add task [" + packageName + "] to db -- appName is blank!");
                return;
            }
            updateOrAdd(runningTask);
        } catch (Exception e) {
            Tools.HangarLog("NPE taskPackage: " + packageName);
            e.printStackTrace();
        }

    }

    protected void buildTasks() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isLollipop;
                    boolean newActivity = false;
                    boolean recentTasksEmpty = true;

                    boolean isToggled = prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT);
                    boolean smartNotification = prefs.getBoolean(Settings.SMART_NOTIFICATION_PREFERENCE, Settings.SMART_NOTIFICATION_DEFAULT);

                    final Context context = getApplicationContext();

                    String taskClass = "";
                    String taskPackage = "";
                    String lTaskClass = "";
                    String lTaskPackage = "";

                    // TODO: This whole thing needs major refactoring.  Needs <=KK and =>L methods

                    isLollipop = Tools.isLollipop(false);

                    db = TasksDataSource.getInstance(context);
                    db.open();

                    pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

                    final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(MAX_RUNNING_TASKS);

                    if (recentTasks != null && recentTasks.size() > 0) {
                        recentTasksEmpty = false;
                        ComponentName task = recentTasks.get(0).baseActivity;
                        taskPackage = task.getPackageName();
                        taskClass = task.getClassName();
                    }

                    if (isLollipop) {
                        String oldPackage = "";
                        String newPackage;

                        if (lollipopTaskInfo != null)
                            oldPackage = lollipopTaskInfo.packageName;

                        List<UsageStats> listStats = Tools.getUsageStats(context);
                        if (listStats.size() == 0) {
                            // Either no permission or nothing new.  Move along
                            return;
                        } else {
                            if (lollipopTaskInfo == null) {
                                // listStats has been 0 up until now, force newActivity
                                Tools.HangarLog("newActivity being set to True");
                                newActivity = true;
                            }
                        }
                        lollipopTaskInfo = Tools.parseUsageStats(listStats, lollipopTaskInfo);

                        if (listStats.size() < 2 && listStats.size() > 0) {
                            newPackage = Tools.firstPackage(listStats);
                        } else {
                            newPackage = lollipopTaskInfo.packageName;
                        }

                        lTaskPackage = lollipopTaskInfo.lastPackageName;
                        boolean isTaskBlacklisted = Tools.isBlacklistedOrBad(lTaskPackage, context, db);
                        if (!isTaskBlacklisted) {
                            lTaskClass = getClassName(context, lTaskPackage);

                            newActivity = !oldPackage.equals(newPackage) && (lTaskClass != null);
                        }
                        if (!newActivity) {
                            return;
                        }

                    }

                    if (!recentTasksEmpty) {
                        if (isLollipop && taskPackage.equals(lollipopTaskInfo.lastRecentPackageName)) {
                            Tools.HangarLog("blanking taskPackage and taskClass (runningTask: " + taskPackage + ")");
                            taskPackage = "";
                            taskClass = "";
                        }

                        if (isLollipop)
                            lollipopTaskInfo.lastRecentPackageName = taskPackage;
                    }

                    if (launcherPackage != null && !taskPackage.isEmpty() &&
                            taskPackage.equals(launcherPackage)) {
                        boolean runningTaskLauncher = runningTask == null || !runningTask.packageName.equals(taskPackage);
                        boolean runningTaskLauncherL = false;

                        if (isLollipop)
                            runningTaskLauncherL = lollipopTaskInfo.lastRecentPackageName.equals(taskPackage);

                        if (runningTaskLauncher || (isLollipop && runningTaskLauncherL)) {
                            if (!isToggled && isAppsWidget()) {
                                taskList = Tools.buildTaskList(getApplicationContext(), db, Settings.TASKLIST_QUEUE_LIMIT);
                                reorderAndLaunch(true);
                            }

                            // First time in launcher?  Update the widget!
                            Tools.HangarLog("Found launcher -- Calling updateWidget!");
                            Tools.updateWidget(context);
                            runningTask = new TaskInfo(taskPackage);

                            buildReorderAndLaunch(isToggled & !isNotificationRunning);
                        }

                        if (!isLollipop) {
                            return;
                        }
                    }

                    boolean isTaskBlacklisted = Tools.isBlacklistedOrBad(taskPackage, context, db);
                    if (taskClass.equals("com.android.internal.app.ResolverActivity") ||
                            isTaskBlacklisted) {
                        buildReorderAndLaunch(isToggled & !isNotificationRunning);

                        if (!isLollipop) {
                            return;
                        }
                    }

                    if (runningTask != null && runningTask.packageName.equals(isLollipop ? lTaskPackage : taskPackage)) {
                        if (isLollipop) {
                            return;
                        }
                        if (pm.isScreenOn()) {
                            runningTask.seconds += LOOP_SECONDS;
                            if (runningTask.seconds >= LOOP_SECONDS * 5) {
                                db.addSeconds(taskPackage, runningTask.seconds);
                                runningTask.totalseconds += runningTask.seconds;
                                runningTask.seconds = 0;
                            }
                        }
                        return;
                    }

                    buildTaskInfo(isLollipop ? lTaskClass : taskClass, isLollipop ? lTaskPackage : taskPackage);

                    if (isLollipop) {
                        if (newActivity) {
                            int activityDelta = (int) Math.ceil(lollipopTaskInfo.timeInFGDelta / 1000);

                            Tools.HangarLog("New activity found, seconds in old: " + activityDelta);
                            if (activityDelta > 0) {
                                db.addSeconds(lollipopTaskInfo.lastPackageName, activityDelta);
                            }
                        }
                    }

                    if (taskClass.equals(getPackageName())) {
                        buildReorderAndLaunch(isToggled & !isNotificationRunning);
                        return;
                    }

                    // If task is showing we do not need to update the notification drawer.
                    if (smartNotification && isToggled) {
                        if (notificationTasks != null && new Tools().isInArray(notificationTasks, isLollipop ? lTaskPackage : taskPackage)) {
                            buildReorderAndLaunch(!isNotificationRunning);
                            return;
                        } else if (notificationTasks == null) {
                            moreAppsPage = 1;
                        }
                    }
                    if (new Tools().isPinned(context, isLollipop ? lTaskPackage : taskPackage)) {
                        buildReorderAndLaunch(isToggled & !isNotificationRunning);
                        return;
                    }
                    buildReorderAndLaunch(isToggled);
                } catch (Exception e) {
                    if (failCount >= 2) {
                        e.printStackTrace();
                        Tools.HangarLog("failCount reached limit.  Giving up!");
                    } else {
                        failCount++;
                        e.printStackTrace();
                        Tools.HangarLog("Exception hit!  Restarting buildTasks.. [" + failCount + "] exception: " + e);
                        WatchfulService.this.buildTasks();
                    }
                }
            }
        };
        new Thread(runnable).start();
    }

    protected final Runnable scanApps = new Runnable(){
        public void run(){
            // Tools.HangarLog("scanApps running..");
            buildTasks();
            handler.postDelayed(this, LOOP_SECONDS * 1000);
        }
    };

    public synchronized int updateOrAdd(TaskInfo newInfo) {
        int rows = db.updateTaskTimestamp(newInfo.packageName);
        if (rows > 0) {
            Tools.HangarLog("Updated task [" + newInfo.appName + "] with new Timestamp");

            return db.increaseLaunch(newInfo.packageName);
        } else {
            Date date = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Tools.HangarLog("Added task [" + newInfo.appName + "] to database date=[" + dateFormatter.format(date) + "]");
            db.createTask(newInfo.appName, newInfo.packageName, newInfo.className, dateFormatter.format(date));
            return 1;
        }
    }

    public void destroyNotification() {
        Tools.HangarLog("DESTROY");
        isNotificationRunning = false;
        runningTask = null;
        pinnedList = null;
        notificationTasks = null;
        moreAppsPage = 1;
        stopForeground(true);
    }

    protected boolean isAppsWidget() {
        try {
            int ids[] = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, AppsWidget.class));
            if (ids.length > 0)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void reorderAndLaunch(boolean isWidget) {
        Tools.HangarLog("reorderAndLaunch taskList.size(): " + taskList.size() + " isWidget: " + isWidget);
        boolean weightedRecents = prefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        boolean isToggled = prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT);
        int weightPriority = Integer.parseInt(prefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));
        if (weightedRecents) {
            taskList = new Tools().reorderTasks(taskList, db, weightPriority);
        }
        if (isToggled)
            createNotification();
        if (isWidget)
            Tools.reorderWidgetTasks(db, getApplicationContext());
    }

    protected void reorderAndLaunch() {
        reorderAndLaunch(false);
    }

    public void updatePrefs() {
        contLayout = prefs.getBoolean(Settings.ROW_DIVIDER_PREFERENCE, Settings.ROW_DIVIDER_DEFAULT) ?
                getResources().getIdentifier("notification", "layout", taskPackage) :
                getResources().getIdentifier("notification_no_dividers", "layout", taskPackage);
        rowLayout = prefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT) ?
                getResources().getIdentifier("notification_row", "layout", taskPackage) :
                getResources().getIdentifier("notification_row_no_dividers", "layout", taskPackage);

        numOfApps = Integer.parseInt(prefs.getString(Settings.APPSNO_PREFERENCE, Integer.toString(Settings.APPSNO_DEFAULT)));
        if (Tools.isLollipop(true)) {
            setPriority = Settings.PRIORITY_ON_L_DEFAULT;
        } else {
            setPriority = Integer.parseInt(prefs.getString(Settings.PRIORITY_PREFERENCE, Integer.toString(Settings.PRIORITY_DEFAULT)));
        }
        secondRow = prefs.getBoolean(Settings.SECOND_ROW_PREFERENCE, Settings.SECOND_ROW_DEFAULT);
        moreApps = prefs.getBoolean(Settings.MORE_APPS_PREFERENCE, Settings.MORE_APPS_DEFAULT);
        moreAppsPages = Integer.parseInt(prefs.getString(Settings.MORE_APPS_PAGES_PREFERENCE, Integer.toString(Settings.MORE_APPS_PAGES_DEFAULT)));
        iconSize = Integer.parseInt(prefs.getString(Settings.ICON_SIZE_PREFERENCE, Integer.toString(Settings.ICON_SIZE_DEFAULT)));
        notificationBg = prefs.getString(Settings.NOTIFICATION_BG_PREFERENCE, Settings.NOTIFICATION_BG_DEFAULT_VALUE);

        itemLayout = R.layout.notification_item;

        if (iconSize == ICON_SIZE_SMALL) {
            itemLayout = R.layout.notification_item_small;
        } else if (iconSize == ICON_SIZE_LARGE) {
            itemLayout = R.layout.notification_item_large;
        }

        mIcon = prefs.getString(Settings.STATUSBAR_ICON_PREFERENCE, Settings.STATUSBAR_ICON_DEFAULT);
        Tools.HangarLog("mIcon: " + mIcon);

    }

    public ArrayList<TaskInfo> getPageTasks(int pageNum, int count) {
        ArrayList<TaskInfo> copyList = new ArrayList<TaskInfo>();

        if (taskList != null)
            copyList = new ArrayList<TaskInfo>(taskList);

        ArrayList<TaskInfo> tmpList = new ArrayList<TaskInfo>();

        if (pageNum > moreAppsPages) return null;

        try {
            int start = (count * (pageNum - 1)) - pinnedCount - pageNum + 1;
            int end = (count * pageNum) - pinnedCount - pageNum + 1;

            Tools.HangarLog("getPageTasks i = " + start + " ; i < " + end);

            if (start < 0) {
                // This is if pinned brings us into the negative.  We always need the right amount!
                int diff = start * -1;
                start = 0;
                end = end + diff;
            }

            if (copyList.size() < start) {
                Tools.HangarLog("copyList.size() == " + copyList.size() + " < start(" + start + " )");
                return null;
//                return getPageTasks(moreAppsPage, count);
            }
            if (copyList.size() < end) {
                end = copyList.size();
            } else if (copyList.size() == end) {
                end = copyList.size() - 1;
            }

            for (int i = start; i < end; i++) {
                tmpList.add(copyList.get(i));
            }
            return tmpList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void createNotification() {
        int filledConts = 0;
        int maxButtons;
        boolean filledSecondRow = false;

        Context mContext = getApplicationContext();

        if (moreAppsPage == 1)
            updatePrefs();

        RemoteViews customNotifView = new RemoteViews(taskPackage, contLayout);
        RemoteViews customNotifBigView = customNotifView;

        // Create new AppDrawer row
        AppDrawer appDrawer = new AppDrawer(taskPackage);
        appDrawer.createRow(rowLayout, R.id.notifRow);
        appDrawer.setImageLayouts(imageButtonLayout, imageContLayout);
        appDrawer.setPrefs(prefs);
        appDrawer.setContext(mContext);

        maxButtons = numOfApps;

        int iconCacheCount = (maxButtons * (secondRow ? 2 : 1));
        appDrawer.setCount(iconCacheCount, Settings.CACHED_NOTIFICATION_ICON_LIMIT, secondRow);

        ArrayList<TaskInfo> pageList;

        if (moreAppsPage == 1) {
            if (pinnedList == null) {
                Tools.HangarLog("pinnedList is null");
                pinnedList = Tools.buildPinnedList(mContext, db);
                pinnedCount = pinnedList.size();
            }
            if (taskList != null) {
                pageList = new ArrayList<TaskInfo>(taskList);
            } else {
                pageList = new ArrayList<TaskInfo>();
            }
            pageList = new Tools().getPinnedTasks(mContext, pinnedList, pageList, iconCacheCount, moreApps);
        } else {
            if (pinnedCount > iconCacheCount)
                pinnedCount = iconCacheCount - 1;
            pageList = getPageTasks(moreAppsPage, iconCacheCount);
            if (pageList == null) {
                moreAppsPage = 1;
                createNotification();
                return;
            }
            pageList = new Tools().getPinnedTasks(mContext, null, pageList, iconCacheCount, moreApps);
            if (pageList.size() == 1) {
                moreAppsPage = 1;
                return;
            }
        }

//        Tools.HangarLog("taskList: " + taskList + " pageList: " + pageList + " realmaxbuttons: " + numOfApps + " maxbuttons: " + maxButtons + " moreAppsPage: " + moreAppsPage);
//        Tools.HangarLog("taskList.size(): " + taskList.size() + " pageList.size(): " + pageList.size() + " realmaxbuttons: " + numOfApps + " maxbuttons: " + maxButtons + " moreAppsPage: " + moreAppsPage);

        customNotifBigView.removeAllViews(R.id.notifContainer);
        notificationTasks = new ArrayList<String>();

        for (int i=0; i <= pageList.size(); i++) {
            boolean wrapItUp = false;
            if (i == pageList.size())
                wrapItUp = true;
            if (filledConts == maxButtons || wrapItUp) {
                if (filledSecondRow) {
                    filledSecondRow = false;
                    customNotifBigView = customNotifView;
                    customNotifBigView.addView(R.id.notifContainer, appDrawer.getRow());
                    break;
                } else {
                    customNotifView.addView(R.id.notifContainer, appDrawer.getRow());
                    filledSecondRow = true;
                    filledConts = 0;
                    appDrawer.createRow(rowLayout, R.id.notifRow);
                    appDrawer.setImageLayouts(imageButtonLayout, imageContLayout);
                }
            }

            if (!wrapItUp) {
                if (appDrawer.newItem(pageList.get(i), itemLayout)) {
                    appDrawer.addItem();
                    notificationTasks.add(pageList.get(i).packageName);
                    filledConts++;
                }
            }
        }

        if (!notificationBg.equals(Settings.NOTIFICATION_BG_DEFAULT_VALUE)) {
            customNotifView.setInt(R.id.notifContainer, "setBackgroundColor",
                    Color.parseColor(notificationBg));
        }

        if (filledSecondRow && secondRow) {
            Tools.HangarLog("Second row is not full -- adding expanded row anyway!");
            // Second row is not full :(
            customNotifBigView = customNotifView;
            customNotifBigView.addView(R.id.notifContainer, appDrawer.getRow());
        }

        // Set statusbar icon
        int smallIcon = iconMap.get(Settings.STATUSBAR_ICON_WHITE);
        try {
            smallIcon = iconMap.get(mIcon);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Notification notification;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(WatchfulService.this).
                setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_name))
                .setSmallIcon(smallIcon)
                .setContent(customNotifView)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setPriority(setPriority);

        if (Tools.isLollipop(false))
            lollipopNotificationSettings(builder);

        notification = builder.build();

        if (secondRow) {
            notification.bigContentView = customNotifBigView;
        }

        Tools.HangarLog("isNotificationRunning: " + isNotificationRunning);
        if (isNotificationRunning) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1337, notification);
        } else {
            startForeground(1337, notification);
        }

        if (moreAppsPage > 1) {
            notificationTasks = null;
            pinnedList = null;
        }

        isNotificationRunning = true;
    }

    @TargetApi(21)
    public void lollipopNotificationSettings(NotificationCompat.Builder builder) {
        if (Tools.isLollipop(false)) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(BCAST_CONFIGCHANGED) &&
                    runningTask != null && launcherPackage != null) {
                Tools.HangarLog("runningTask: " + runningTask.packageName + " launcherPackage: " + launcherPackage);
                if (runningTask.packageName.equals(launcherPackage)) {
                    Tools.updateWidget(context);
                }
            }
        }
    };
}
