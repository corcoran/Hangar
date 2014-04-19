package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ca.mimic.apphangar.Tools.TaskInfo;

public class WatchfulService extends Service {

    TasksDataSource db;

    SharedPreferences prefs;
    PackageManager pkgm;
    PowerManager pm;

    // ArrayList<TaskInfo> taskList = new ArrayList<TaskInfo>();
    TaskInfo runningTask;
    String launcherPackage = null;

    int MAX_RUNNING_TASKS = 20;
    int TASKLIST_QUEUE_SIZE = 12;
    int TOTAL_CONTAINERS = 9;
    int LOOP_SECONDS = 3;

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

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    // private final IBinder mBinder = new WatchfulBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        if (db == null) {
            db = new TasksDataSource(this);
            db.open();
        }
        Tools.HangarLog("starting up.. ");

        prefs = getSharedPreferences(getPackageName(), MODE_MULTI_PROCESS);
        // prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        // prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        Tools.HangarLog("buildReorderAndLaunch: isToggled: " + isToggled);
        if (isToggled) {
            ArrayList<Tools.TaskInfo> taskList;
            taskList = Tools.buildTaskList(getApplicationContext(), db, TASKLIST_QUEUE_SIZE);
            reorderAndLaunch(taskList);
        }
    }

    protected void buildTasks() {
        // prefs = getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS);
        try {
            boolean isToggled = prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT);

            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(MAX_RUNNING_TASKS);

            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (recentTasks.size() > 0) {
                ComponentName task = recentTasks.get(0).baseActivity;
                String taskClass = task.getClassName();
                String taskPackage = task.getPackageName();

                if (launcherPackage != null && taskPackage.equals(launcherPackage)) {
                    if (runningTask == null || !runningTask.packageName.equals(taskPackage)) {
                        // First time in launcher?  Update the widget!
                        Tools.HangarLog("Found launcher -- Calling updateWidget!");
                        Tools.updateWidget(getApplicationContext());
                        runningTask = new TaskInfo(taskPackage);

                        buildReorderAndLaunch(isToggled);
                    }
                    return;
                }
                if (taskClass.equals("com.android.internal.app.ResolverActivity") ||
                        Tools.isBlacklistedOrBad(taskPackage, getApplicationContext(), db)) {
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
                runningTask = new TaskInfo(taskPackage);
                runningTask.className = taskClass;
                runningTask.packageName = taskPackage;
                try {
                    ApplicationInfo appInfo = pkgm.getApplicationInfo(taskPackage, 0);
                    runningTask.appName = appInfo.loadLabel(pkgm).toString();
                } catch (NullPointerException e) {
                    Tools.HangarLog("NPE taskPackage: " + taskPackage);
                    e.printStackTrace();
                }
                updateOrAdd(runningTask);
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
        // topPackage = null;
        // handler.removeCallbacks(scanApps);
        Tools.HangarLog("DESTROY");
        isNotificationRunning = false;
        stopForeground(true);
        // NotificationManager NotificationManager = (NotificationManager)
        //         getSystemService(Context.NOTIFICATION_SERVICE);
        // NotificationManager.cancel(1337);

    }
    protected void clearContainers(RemoteViews customNotifView, int start, Context mContext) {
        Tools.HangarLog("Clearing containers " + start + "-" + TOTAL_CONTAINERS);
        for (int i=start; i < TOTAL_CONTAINERS; i++) {
            int contID = getResources().getIdentifier("imageCont" + (i + 1), "id", mContext.getPackageName());
            customNotifView.setViewVisibility(contID, View.GONE);
        }

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
        // prefs = this.getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS);
        // Not a fun hack.  No way around it until they let you do getInt for setShowDividers!
        RemoteViews customNotifView;
        String taskPackage = this.getPackageName();

        int rootID = prefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT) ?
                getResources().getIdentifier("notification", "layout", taskPackage) :
                getResources().getIdentifier("notification_no_dividers", "layout", taskPackage);

        customNotifView = new RemoteViews(WatchfulService.this.getPackageName(),
                rootID);

        int maxButtons;
        int realMaxButtons = Integer.parseInt(prefs.getString(Settings.APPSNO_PREFERENCE, Integer.toString(Settings.APPSNO_DEFAULT)));
        int setPriority = Integer.parseInt(prefs.getString(Settings.PRIORITY_PREFERENCE, Integer.toString(Settings.PRIORITY_DEFAULT)));
        boolean isColorized = prefs.getBoolean(Settings.COLORIZE_PREFERENCE, Settings.COLORIZE_DEFAULT);
        int getColor = prefs.getInt(Settings.ICON_COLOR_PREFERENCE, Settings.ICON_COLOR_DEFAULT);

        Random r = new Random();
        int pendingNum = r.nextInt(99 - 1 + 1) + 1;

        if (taskList.size() < realMaxButtons) {
            maxButtons = taskList.size();
        } else {
            maxButtons = realMaxButtons;
        }
          
        if (maxButtons < TOTAL_CONTAINERS)
            clearContainers(customNotifView, maxButtons, getApplicationContext());

        Tools.HangarLog("taskList.size(): " + taskList.size() + " realmaxbuttons: " + realMaxButtons + " maxbuttons: " + maxButtons);
        int filledConts = 0;

        for (int i=0; i < taskList.size(); i++) {
            int resID = getResources().getIdentifier("imageButton" + (filledConts+1), "id", taskPackage);
            int contID = getResources().getIdentifier("imageCont" + (filledConts+1), "id", taskPackage);

            if (filledConts == maxButtons) {
                // Tools.HangarLog("filledConts [" + filledConts + "] == maxButtons [" + maxButtons + "]");
                break;
            }

            filledConts += 1;
            customNotifView.setViewVisibility(contID, View.VISIBLE);

            Drawable taskIcon, d;
            try {
                ApplicationInfo appInfo = pkgm.getApplicationInfo(taskList.get(i).packageName, 0);
                taskIcon = appInfo.loadIcon(pkgm);
            } catch (Exception e) {
                taskList.remove(i);
                continue;
            }

            if (isColorized) {
                d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskIcon, getColor));
            } else {
                d = taskIcon;
            }

            Bitmap bmpIcon = ((BitmapDrawable) d).getBitmap();
            customNotifView.setImageViewBitmap(resID, bmpIcon);

            Intent intent;
            PackageManager manager = getPackageManager();
            try {
                intent = manager.getLaunchIntentForPackage(taskList.get(i).packageName);
                if (intent == null) {
                    Tools.HangarLog("Couldn't get intent for ["+ taskList.get(i).packageName +"] className:" + taskList.get(i).className);
                    filledConts --;
                    customNotifView.setViewVisibility(contID, View.GONE);
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (i));
                PendingIntent activity = PendingIntent.getActivity(this, pendingNum, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                customNotifView.setOnClickPendingIntent(contID, activity);
            } catch (PackageManager.NameNotFoundException e) {

            }
        }

        String mIcon = prefs.getString(Settings.STATUSBAR_ICON_PREFERENCE, Settings.STATUSBAR_ICON_DEFAULT);
        int smallIcon = iconMap.get(Settings.STATUSBAR_ICON_WHITE_WARM);
        try {
            smallIcon = iconMap.get(mIcon);
        } catch (NullPointerException e) {
        }

        Notification notification = new Notification.Builder(WatchfulService.this).
                setContentTitle("Notification title")
                .setContentText("Notification content")
                .setSmallIcon(smallIcon)
                .setContent(customNotifView)
                .setOngoing(true)
                .setPriority(setPriority)
                .build();
        startForeground(1337, notification);
        isNotificationRunning = true;
        //NotificationManager NotificationManager = (NotificationManager)
        //        getSystemService(Context.NOTIFICATION_SERVICE);
        //NotificationManager.notify(0, notification);
    }
}
