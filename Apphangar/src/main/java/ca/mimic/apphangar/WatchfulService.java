package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
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
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchfulService extends Service {
    String TAG = "Apphangar";

    TasksDataSource db;

    SharedPreferences prefs;
    PowerManager pm;

    ArrayList<TaskInfo> taskList = new ArrayList<TaskInfo>();
    String topPackage;

    int MAX_RUNNING_TASKS = 20;
    int TASKLIST_QUEUE_SIZE = 12;
    int TOTAL_CONTAINERS = 9;
    int LOOP_SECONDS = 3;

    Map<String, Integer> iconMap;

    Handler handler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return new IWatchfulService.Stub() {
            @Override
            public void clearTasks() {
                topPackage = null;
                taskList.clear();
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
        Log.d(TAG, "starting up..");
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
        Log.d(TAG, "Getting prefs");
        prefs = getSharedPreferences(getPackageName(), MODE_MULTI_PROCESS);
        // prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        handler.removeCallbacks(scanApps);
        handler.post(scanApps);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy service..");
        handler.removeCallbacks(scanApps);
        db.close();
        super.onDestroy();
    }

    public static class TaskInfo {
        private String appName = "";
        private String packageName = "";
        private String className = "";
        private int launches = 0;
        private float order = 0;
        private int seconds = 0;
        private int totalseconds = 0;

    }

    public TaskInfo getTask(String packageName) {
        for (TaskInfo task : taskList) {
            if (task.packageName.equals(packageName)) {
                return task;
            }
        }
        return null;
    }

    protected boolean isBlacklisted(String packageName) {
        for (String blTask : getBlacklisted()) {
            if (packageName.equals(blTask)) {
                return true;
            }
        }
        return false;
    }

    protected void updateWidget(Context mContext) {
        Intent i = new Intent(mContext, StatsWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        mContext.sendBroadcast(i);
    }

    protected void buildTasks() {
        // prefs = getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS);
        try {
            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(MAX_RUNNING_TASKS);

            PackageManager pkgm = getApplicationContext().getPackageManager();
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (recentTasks.size() > 0) {
                ComponentName task = recentTasks.get(0).baseActivity;
                String taskClass = task.getClassName();
                String taskPackage = task.getPackageName();

                if (taskClass.equals("com.android.launcher2.Launcher")) {
                    if (!topPackage.equals(taskPackage)) {
                        // First time in launcher?  Update the widget!
                        Log.d(TAG, "Calling updateWidget!");
                        updateWidget(getApplicationContext());
                    }
                    topPackage = taskPackage;
                    return;
                }
                if (taskClass.equals("com.android.internal.app.ResolverActivity")) {
                    return;
                }

                if (topPackage != null && topPackage.equals(taskPackage)) {
                    TaskInfo runningTask = getTask(taskPackage);
                    if (runningTask != null && pm.isScreenOn()) {
                        runningTask.seconds += LOOP_SECONDS;
                        Log.d(TAG, "Task [" + runningTask.appName + "] in fg [" + runningTask.seconds + "]s");
                        if (runningTask.seconds >= LOOP_SECONDS * 5) {
                            Log.d(TAG, "Dumping task [" + runningTask.appName + "] to DB [" + runningTask.seconds + "]s");
                            db.addSeconds(taskPackage, runningTask.seconds);
                            runningTask.totalseconds += runningTask.seconds;
                            runningTask.seconds = 0;
                        }
                    }
                    return;
                }
                topPackage = taskPackage;
            }

            if (taskList.size() < TASKLIST_QUEUE_SIZE) {
                List<TasksModel> tasks = db.getAllTasks(TASKLIST_QUEUE_SIZE);

                for (TasksModel taskM : tasks) {
                    String taskPackage = taskM.getPackageName();
                    if (taskPackage.equals(topPackage))
                        continue;

                    if (isBlacklisted(taskPackage))
                        continue;

                    boolean skipTask = false;
                    for (TaskInfo taskL : taskList) {
                        if (taskPackage.equals(taskL.packageName))
                            skipTask = true;
                    }
                    if (skipTask)
                        continue;

                    TaskInfo dbTask = new TaskInfo();
                    dbTask.appName = taskM.getName();
                    dbTask.packageName = taskPackage;
                    dbTask.className = taskM.getClassName();
                    dbTask.launches = taskM.getLaunches();
                    dbTask.totalseconds = taskM.getSeconds();

                    try {
                        pkgm.getApplicationInfo(dbTask.packageName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        db.deleteTask(taskM);
                        continue;
                    }

                    Log.d(TAG, "Adding to taskList [" + dbTask.appName + "] [" + dbTask.launches + "] [" + dbTask.totalseconds + "]s");
                    taskList.add(dbTask);
                }
            }

            int origSize = taskList.size();

            for (int i = 0; i < recentTasks.size(); i++) {
                ComponentName task = recentTasks.get(i).baseActivity;
                String taskPackage = task.getPackageName();

                ApplicationInfo appInfo = pkgm.getApplicationInfo(taskPackage, 0);
                if (isBlacklisted(taskPackage))
                    continue;

                TaskInfo newInfo = new TaskInfo();
                newInfo.appName = appInfo.loadLabel(pkgm).toString();
                newInfo.packageName = taskPackage;
                newInfo.className = task.getClassName();

                if (i == 0 && origSize > 0) {
                    int launches = updateOrAdd(newInfo);
                    newInfo.launches = launches;
                    newInfo.totalseconds = db.getSeconds(newInfo.packageName);

                    Log.d(TAG, "Launches [" + newInfo.appName + "] [" + launches + "]");
                    taskList.add(0, newInfo);
                    Log.d(TAG, "Added [" + newInfo.appName + "] to taskList.  Size=[" + taskList.size() + "]");
                    Log.d(TAG, "Just adding latest item to taskList");
                    for (int j=1; j < taskList.size(); j++) {
                        if (taskList.get(j).packageName.equals(taskList.get(0).packageName)) {
                            // Log.d(TAG, "Duplicate task found at [" + j + "] -- removing..");
                            taskList.remove(j);
                        }
                    }
                    break;
                } else if (origSize == 0) {
                    if (isBlacklisted(newInfo.packageName)) {
                        continue;
                    }

                    if (newInfo.className.equals("com.android.launcher2.Launcher") ||
                            newInfo.className.equals("com.android.internal.app.ResolverActivity")) {
                        continue;
                    }
                    // No database?  Let's create from memory..
                    updateOrAdd(newInfo);
                    taskList.add(newInfo);
                }
            }
            if (taskList.size() > 0) {
                boolean weightedRecents = prefs.getBoolean(Settings.WEIGHTED_RECENTS_PREFERENCE,
                        Settings.WEIGHTED_RECENTS_DEFAULT);
                boolean isToggled = prefs.getBoolean(Settings.TOGGLE_PREFERENCE, Settings.TOGGLE_DEFAULT);
                if (isToggled) {
                    if (weightedRecents)
                        reorderTasks();
                    createNotification();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    class TaskInfoOrder {
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

    class TaskComparator implements Comparator<TaskInfoOrder>
    {
        private String mType;

        int weightPriority = Integer.parseInt(prefs.getString(Settings.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(Settings.WEIGHT_PRIORITY_DEFAULT)));

        public TaskComparator (String type){
            this.mType = type;
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

    private void reorderTasks() {
        int highestSeconds = db.getHighestSeconds();
        int highestLaunch = db.getHighestLaunch();
        Log.d(TAG, "highest Launch [" + highestLaunch + "] Seconds [" + highestSeconds + "]");
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
        Collections.sort(taskListE, new TaskComparator("launch"));
        int c = 0;
        for (int i=taskListE.size()-1; i >= 0; i--) {
            taskListE.get(c).launchOrder = (i + 1);
            c++;
        }

        c = 0;
        Collections.sort(taskListE, new TaskComparator("seconds"));
        for (int i=taskListE.size()-1; i >= 0; i--) {
            taskListE.get(c).secondsOrder = (i + 1);
            c++;
        }

        Collections.sort(taskListE, new TaskComparator("final"));
        taskList.clear();
        for (TaskInfoOrder taskE : taskListE) {
            Log.d(TAG, "task[" + taskE.getOrig().appName + "] l[" + taskE.launchOrder + "] p[" + taskE.placeOrder + "] s[" + taskE.secondsOrder + "]");
            taskList.add(taskE.getOrig());
        }
    }

    protected final Runnable scanApps = new Runnable(){
        public void run(){
            // Log.d(TAG, "scanApps running..");
            buildTasks();
            handler.postDelayed(this, LOOP_SECONDS * 1000);
        }
    };
    public int updateOrAdd(TaskInfo newInfo) {
        int rows = db.updateTaskTimestamp(newInfo.packageName);
        if (rows > 0) {
            Log.d(TAG, "Updated task [" + newInfo.appName + "] with new Timestamp");

            return db.increaseLaunch(newInfo.packageName);
        } else {
            Date date = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.d(TAG, "Added task [" + newInfo.appName + "] to database date=[" + dateFormatter.format(date) + "]");
            db.createTask(newInfo.appName, newInfo.packageName, newInfo.className, dateFormatter.format(date));
            return 1;
        }
    }
    public ArrayList<String> getBlacklisted() {
        ArrayList<String> blPNames = new ArrayList<String>();
        List<TasksModel> blTasks = db.getBlacklisted();
        for (TasksModel task : blTasks) {
            blPNames.add(task.getPackageName());
        }
        blPNames.add("com.android.systemui");
        blPNames.add("com.android.phone");
        blPNames.add(getPackageName());
        blPNames.add("com.android.settings");
        blPNames.add("com.android.packageinstaller");
        return blPNames;
    }
    public void destroyNotification() {
        // topPackage = null;
        // handler.removeCallbacks(scanApps);
        Log.d(TAG, "DESTROY");
        stopForeground(true);
        // NotificationManager NotificationManager = (NotificationManager)
        //         getSystemService(Context.NOTIFICATION_SERVICE);
        // NotificationManager.cancel(1337);

    }
    protected void clearContainers(RemoteViews customNotifView, int start, Context mContext) {
        Log.d(TAG, "Clearing containers " + start + "-" + TOTAL_CONTAINERS);
        for (int i=start; i < TOTAL_CONTAINERS; i++) {
            int contID = getResources().getIdentifier("imageCont" + (i + 1), "id", mContext.getPackageName());
            customNotifView.setViewVisibility(contID, View.GONE);
        }

    }

    public void createNotification() {
        // prefs = this.getSharedPreferences(getPackageName(), Context.MODE_MULTI_PROCESS);
        // Not a fun hack.  No way around it until they let you do getInt for setShowDividers!
        PackageManager pkgm = getApplicationContext().getPackageManager();
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

        if (taskList.size() < realMaxButtons) {
            maxButtons = taskList.size();
        } else {
            maxButtons = realMaxButtons;
        }
          
        if (maxButtons < TOTAL_CONTAINERS)
            clearContainers(customNotifView, maxButtons, getApplicationContext());

        Log.d(TAG, "taskList.size(): " + taskList.size() + " realmaxbuttons: " + realMaxButtons + " maxbuttons: " + maxButtons);
        int filledConts = 0;

        for (int i=0; i < taskList.size(); i++) {
            int resID = getResources().getIdentifier("imageButton" + (filledConts+1), "id", taskPackage);
            int contID = getResources().getIdentifier("imageCont" + (filledConts+1), "id", taskPackage);

            if (filledConts == maxButtons) {
                // Log.d(TAG, "filledConts [" + filledConts + "] == maxButtons [" + maxButtons + "]");
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
                    Log.d(TAG, "Couldn't get intent for ["+ taskList.get(i).packageName +"] className:" + taskList.get(i).className);
                    filledConts --;
                    customNotifView.setViewVisibility(contID, View.GONE);
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (i));
                PendingIntent activity = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        //NotificationManager NotificationManager = (NotificationManager)
        //        getSystemService(Context.NOTIFICATION_SERVICE);
        //NotificationManager.notify(0, notification);
    }
}
