package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.mimic.apphangar.Tools.TaskInfo;

public class WatchfulService extends Service {

    TasksDataSource db;

    SharedPreferences prefs;
    PackageManager pkgm;
    PowerManager pm;

    TaskInfo runningTask;
    String launcherPackage = null;

    final int MAX_RUNNING_TASKS = 20;
    final int TASKLIST_QUEUE_SIZE = 12;
    final int TOTAL_CONTAINERS = 9;
    final int LOOP_SECONDS = 3;

    boolean isNotificationRunning;

    Map<String, Integer> iconMap;

    Handler handler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return new IWatchfulService.Stub() {
            @Override
            public void clearTasks() {
                runningTask = null;
            }
            @Override
            public void runScan() {
                WatchfulService.this.runScan();
            }
            @Override
            public void destroyNotification() {
                WatchfulService.this.destroyNotification();
            }
            @Override
            public void buildTasks() {
                WatchfulService.this.buildTasks();
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (db == null) {
            db = new TasksDataSource(this);
            db.open();
        } else {
            return;
        }
        Tools.HangarLog("starting up.. ");

        prefs = getSharedPreferences(getPackageName(), MODE_MULTI_PROCESS);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        iconMap = new HashMap<String, Integer>();
        iconMap.put(Settings.STATUSBAR_ICON_WHITE_WARM, R.drawable.ic_apps_warm);
        iconMap.put(Settings.STATUSBAR_ICON_WHITE_COLD, R.drawable.ic_apps_cold);
        iconMap.put(Settings.STATUSBAR_ICON_WHITE_BLUE, R.drawable.ic_apps_blue);
        iconMap.put(Settings.STATUSBAR_ICON_BLACK_WARM, R.drawable.ic_apps_warm_black);
        iconMap.put(Settings.STATUSBAR_ICON_BLACK_COLD, R.drawable.ic_apps_cold_black);
        iconMap.put(Settings.STATUSBAR_ICON_BLACK_BLUE, R.drawable.ic_apps_blue_black);
        iconMap.put(Settings.STATUSBAR_ICON_TRANSPARENT, R.drawable.ic_apps_transparent);
    }

    protected void runScan() {
        handler.removeCallbacks(scanApps);
        handler.post(scanApps);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Tools.HangarLog("Getting prefs");
        prefs = getSharedPreferences(getPackageName(), MODE_MULTI_PROCESS);
        pkgm = getApplicationContext().getPackageManager();
        launcherPackage = Tools.getLauncher(getApplicationContext());
        handler.removeCallbacks(scanApps);
        handler.post(scanApps);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Tools.HangarLog("onDestroy service..");
        handler.removeCallbacks(scanApps);
        db.close();
        super.onDestroy();
    }

    protected void buildReorderAndLaunch(boolean isToggled) {
        if (isToggled) {
            ArrayList<Tools.TaskInfo> taskList;
            taskList = Tools.buildTaskList(getApplicationContext(), db, TASKLIST_QUEUE_SIZE);
            if (taskList.size() == 0) {
                buildBaseTasks();
                taskList = Tools.buildTaskList(getApplicationContext(), db, TASKLIST_QUEUE_SIZE);
            }
            reorderAndLaunch(taskList);
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
                } catch (NullPointerException e) { }
            }
        }
    }

    protected void buildTaskInfo(String className, String packageName) {
        if (className.equals("com.android.internal.app.ResolverActivity") ||
                Tools.isBlacklistedOrBad(packageName, getApplicationContext(), db) ||
                packageName.equals(launcherPackage)) {
            return;
        }
        runningTask = new TaskInfo(packageName);
        runningTask.className = className;
        try {
            ApplicationInfo appInfo = pkgm.getApplicationInfo(packageName, 0);
            runningTask.appName = appInfo.loadLabel(pkgm).toString();
            updateOrAdd(runningTask);
        } catch (Exception e) {
            Tools.HangarLog("NPE taskPackage: " + packageName);
            e.printStackTrace();
        }

    }

    protected void buildTasks() {
        try {
            boolean isToggled = prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT);

            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(MAX_RUNNING_TASKS);
            final Context mContext = getApplicationContext();

            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (recentTasks != null && recentTasks.size() > 0) {
                ComponentName task = recentTasks.get(0).baseActivity;
                String taskClass = task.getClassName();
                String taskPackage = task.getPackageName();

                if (launcherPackage != null && taskPackage != null &&
                        taskPackage.equals(launcherPackage)) {
                    if (runningTask == null || !runningTask.packageName.equals(taskPackage)) {
                        // First time in launcher?  Update the widget!
                        Tools.HangarLog("Found launcher -- Calling updateWidget!");
                        Tools.updateWidget(mContext);
                        runningTask = new TaskInfo(taskPackage);

                        buildReorderAndLaunch(isToggled);
                    }
                    return;
                }
                if (taskClass.equals("com.android.internal.app.ResolverActivity") ||
                        Tools.isBlacklistedOrBad(taskPackage, mContext, db)) {
                    buildReorderAndLaunch(isToggled & !isNotificationRunning);
                    return;
                }

                if (runningTask != null && runningTask.packageName.equals(taskPackage)) {
                    if (pm.isScreenOn()) {
                        runningTask.seconds += LOOP_SECONDS;
                        Tools.HangarLog("Task [" + runningTask.packageName + "] in fg [" + runningTask.seconds + "]s");
                        if (runningTask.seconds >= LOOP_SECONDS * 5) {
                            Tools.HangarLog("Dumping task [" + runningTask.packageName + "] to DB [" + runningTask.seconds + "]s");
                            db.addSeconds(taskPackage, runningTask.seconds);
                            runningTask.totalseconds += runningTask.seconds;
                            runningTask.seconds = 0;
                        }
                    }
                    return;
                }
                buildTaskInfo(taskClass, taskPackage);
            }
            buildReorderAndLaunch(isToggled);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected final Runnable scanApps = new Runnable(){
        public void run(){
            // Tools.HangarLog("scanApps running..");
            buildTasks();
            handler.postDelayed(this, LOOP_SECONDS * 1000);
        }
    };
    public int updateOrAdd(TaskInfo newInfo) {
        int rows = db.updateTaskTimestamp(newInfo.packageName);
        if (rows > 0) {
            Tools.HangarLog("Updated task [" + newInfo.appName + "] with new Timestamp");

            return db.increaseLaunch(newInfo.packageName);
        } else {
            Date date = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Tools.HangarLog("Added task [" + newInfo.appName + "] to database date=[" + dateFormatter.format(date) + "]");
            db.createTask(newInfo.appName, newInfo.packageName, newInfo.className, dateFormatter.format(date));
            return 1;
        }
    }

    public void destroyNotification() {
        Tools.HangarLog("DESTROY");
        isNotificationRunning = false;
        stopForeground(true);
    }

    protected void reorderAndLaunch(ArrayList<Tools.TaskInfo> taskList) {
        boolean weightedRecents = prefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                Settings.WEIGHTED_RECENTS_DEFAULT);
        boolean isToggled = prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT);
        int weightPriority = Integer.parseInt(prefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));
        if (isToggled) {
            if (weightedRecents)
                taskList = Tools.reorderTasks(taskList, db, weightPriority);
            createNotification(taskList);
        }
    }

    public void createNotification(ArrayList<Tools.TaskInfo> taskList) {
        // Not a fun hack.  No way around it until they let you do getInt for setShowDividers!
        String taskPackage = this.getPackageName();
        Context mContext = getApplicationContext();

        int rowLayout = prefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT) ?
                getResources().getIdentifier("notification", "layout", taskPackage) :
                getResources().getIdentifier("notification_no_dividers", "layout", taskPackage);
        int imageButtonLayout = getResources().getIdentifier("imageButton", "id", taskPackage);
        int imageContLayout = getResources().getIdentifier("imageCont", "id", taskPackage);

        // Create new AppDrawer row
        AppDrawer appDrawer = new AppDrawer(taskPackage);
        appDrawer.createRow(rowLayout, R.id.notifContainer);
        appDrawer.setImageLayouts(imageButtonLayout, imageContLayout);
        appDrawer.setPrefs(prefs);
        appDrawer.setContext(mContext);

        int maxButtons;
        int userMaxButtons = Integer.parseInt(prefs.getString(Settings.APPSNO_PREFERENCE, Integer.toString(Settings.APPSNO_DEFAULT)));
        int setPriority = Integer.parseInt(prefs.getString(Settings.PRIORITY_PREFERENCE, Integer.toString(Settings.PRIORITY_DEFAULT)));

        if (taskList.size() < userMaxButtons) {
            maxButtons = taskList.size();
        } else {
            maxButtons = userMaxButtons;
        }
          
        Tools.HangarLog("taskList.size(): " + taskList.size() + " realmaxbuttons: " + userMaxButtons + " maxbuttons: " + maxButtons);
        int filledConts = 0;

        for (int i=0; i < taskList.size(); i++) {
            if (filledConts == maxButtons) {
                break;
            }

            if (appDrawer.newItem(taskList.get(i), R.layout.notification_item, i)) {
                appDrawer.addItem();
                filledConts++;
            }
        }

        // get appDrawer view
        RemoteViews customNotifView = appDrawer.getRow();

        // Set statusbar icon
        String mIcon = prefs.getString(Settings.STATUSBAR_ICON_PREFERENCE, Settings.STATUSBAR_ICON_DEFAULT);
        int smallIcon = iconMap.get(Settings.STATUSBAR_ICON_WHITE_WARM);
        try {
            smallIcon = iconMap.get(mIcon);
        } catch (NullPointerException e) {
        }

        Notification notification = new Notification.Builder(WatchfulService.this).
                setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_name))
                .setSmallIcon(smallIcon)
                .setContent(customNotifView)
                .setOngoing(true)
                .setPriority(setPriority)
                .build();
        startForeground(1337, notification);
        isNotificationRunning = true;
    }
}
