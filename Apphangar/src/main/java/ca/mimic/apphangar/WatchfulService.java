package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

// TODO Run service to loop and poll running apps
// TODO Report list of app names by most recent
// TODO Create array of items
// TODO Update array when new item is added


public class WatchfulService extends Service {
    String TAG = "Apphangar";

    TasksDataSource db;

    SharedPreferences prefs;
    Context mContext;
    PowerManager pm;

    ArrayList<TaskInfo> taskList = new ArrayList<TaskInfo>();
    String topPackage;
    String topClass;
    int realMaxButtons;

    int MAX_RUNNING_TASKS = 20;
    int TASKLIST_QUEUE_SIZE = 12;
    int TOTAL_CONTAINERS = 9;
    int LOOP_SECONDS = 3;

    RemoteViews customNotifView;

    Handler handler = new Handler();

    public class WatchfulBinder extends Binder {
        WatchfulService getService() {
            return WatchfulService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new WatchfulBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = this.getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        mContext = this;
        db = new TasksDataSource(this);
        db.open();
        realMaxButtons = Integer.parseInt(prefs.getString(SettingsActivity.APPSNO_PREFERENCE, Integer.toString(SettingsActivity.APPSNO_DEFAULT)));
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    protected void runScan() {
        handler.removeCallbacks(scanApps);
        handler.post(scanApps);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(scanApps);
        handler.post(scanApps);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scanApps);
        db.close();
        super.onDestroy();
    }

    public static class TaskInfo {
        private String appName = "";
        private String packageName = "";
        private String className = "";
        private Drawable icon;
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

    protected void buildTasks() {
        try {
            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(MAX_RUNNING_TASKS);

            PackageManager pkgm = getApplicationContext().getPackageManager();
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (recentTasks.size() > 0) {
                ComponentName task = recentTasks.get(0).baseActivity;

                if (task.getClassName().equals("com.android.launcher2.Launcher") ||
                        task.getClassName().equals("com.android.internal.app.ResolverActivity")) {
                    return;
                }

                if (topPackage != null && topPackage.equals(task.getPackageName())) {
                    TaskInfo runningTask = getTask(task.getPackageName());
                    if (runningTask != null && pm.isScreenOn()) {
                        runningTask.seconds += LOOP_SECONDS;
                        Log.d(TAG, "Task [" + runningTask.appName + "] in fg [" + runningTask.seconds + "]s");
                        if (runningTask.seconds >= LOOP_SECONDS * 5) {
                            Log.d(TAG, "Dumping task [" + runningTask.appName + "] to DB [" + runningTask.seconds + "]s");
                            db.addSeconds(task.getPackageName(), runningTask.seconds);
                            runningTask.totalseconds += runningTask.seconds;
                            runningTask.seconds = 0;
                        }
                    }
                    return;
                }
                topPackage = task.getPackageName();
                topClass = task.getClassName();
            }

            if (taskList.size() < TASKLIST_QUEUE_SIZE) {
                List<TasksModel> tasks = db.getAllTasks(TASKLIST_QUEUE_SIZE);

                for (TasksModel taskM : tasks) {
                    if (taskM.getPackageName().equals(topPackage))
                        continue;

                    if (isBlacklisted(taskM.getPackageName()))
                        continue;

                    boolean skipTask = false;
                    for (TaskInfo taskL : taskList) {
                        if (taskM.getPackageName().equals(taskL.packageName))
                            skipTask = true;
                    }
                    if (skipTask)
                        continue;

                    TaskInfo dbTask = new TaskInfo();
                    dbTask.appName = taskM.getName();
                    dbTask.packageName = taskM.getPackageName();
                    dbTask.className = taskM.getClassName();
                    dbTask.launches = taskM.getLaunches();
                    dbTask.totalseconds = taskM.getSeconds();

                    ApplicationInfo appInfo = pkgm.getApplicationInfo(dbTask.packageName, 0);
                    dbTask.icon = appInfo.loadIcon(pkgm);

                    Log.d(TAG, "Adding to taskList [" + dbTask.appName + "] [" + dbTask.launches + "] [" + dbTask.totalseconds + "]s");
                    taskList.add(dbTask);
                }
            }

            int origSize = taskList.size();

            for (int i = 0; i < recentTasks.size(); i++) {
                ComponentName task = recentTasks.get(i).baseActivity;

                ApplicationInfo appInfo = pkgm.getApplicationInfo(task.getPackageName(), 0);
                if (isBlacklisted(task.getPackageName()))
                    continue;

                TaskInfo newInfo = new TaskInfo();
                newInfo.appName = appInfo.loadLabel(pkgm).toString();
                newInfo.icon = appInfo.loadIcon(pkgm);
                newInfo.packageName = task.getPackageName();
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
                boolean weightedRecents = prefs.getBoolean(SettingsActivity.WEIGHTED_RECENTS_PREFERENCE,
                        SettingsActivity.WEIGHTED_RECENTS_DEFAULT);
                boolean isToggled = prefs.getBoolean(SettingsActivity.TOGGLE_PREFERENCE, SettingsActivity.TOGGLE_DEFAULT);
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

        int weightPriority = Integer.parseInt(prefs.getString(SettingsActivity.WEIGHT_PRIORITY_PREFERENCE,
                Integer.toString(SettingsActivity.WEIGHT_PRIORITY_DEFAULT)));

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
        topPackage = null;
        // handler.removeCallbacks(scanApps);
        stopForeground(true);
        // NotificationManager NotificationManager = (NotificationManager)
        //         getSystemService(Context.NOTIFICATION_SERVICE);
        // NotificationManager.cancel(1337);
    }
    protected void clearContainers(int start) {
        Log.d(TAG, "Clearing containers " + start + "-" + TOTAL_CONTAINERS);
        for (int i=start; i < TOTAL_CONTAINERS; i++) {
            int contID = getResources().getIdentifier("imageCont" + (i + 1), "id", mContext.getPackageName());
            customNotifView.setViewVisibility(contID, View.GONE);
        }

    }

    public int dpToPx(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }
    public void createNotification() {
        // Not a fun hack.  No way around it until they let you do getInt for setShowDividers!
        int rootID = prefs.getBoolean(SettingsActivity.DIVIDER_PREFERENCE, SettingsActivity.DIVIDER_DEFAULT) ?
                getResources().getIdentifier("notification", "layout", this.getPackageName()) :
                getResources().getIdentifier("notification_no_dividers", "layout", this.getPackageName());

        customNotifView = new RemoteViews(WatchfulService.this.getPackageName(),
                rootID);

        int maxButtons = 0;
        realMaxButtons = Integer.parseInt(prefs.getString(SettingsActivity.APPSNO_PREFERENCE, Integer.toString(SettingsActivity.APPSNO_DEFAULT)));
        int setPriority = Integer.parseInt(prefs.getString(SettingsActivity.PRIORITY_PREFERENCE, Integer.toString(SettingsActivity.PRIORITY_DEFAULT)));
        boolean isColorized = prefs.getBoolean(SettingsActivity.COLORIZE_PREFERENCE, SettingsActivity.COLORIZE_DEFAULT);
        int getColor = prefs.getInt(SettingsActivity.ICON_COLOR_PREFERENCE, SettingsActivity.ICON_COLOR_DEFAULT);

        if (taskList.size() < realMaxButtons) {
            maxButtons = taskList.size();
        } else {
            maxButtons = realMaxButtons;
        }
          
        if (maxButtons < TOTAL_CONTAINERS)
            clearContainers(maxButtons);

        Log.d(TAG, "taskList.size(): " + taskList.size() + " realmaxbuttons: " + realMaxButtons + " maxbuttons: " + maxButtons);
        int filledConts = 0;

        for (int i=0; i < taskList.size(); i++) {
            int resID = getResources().getIdentifier("imageButton" + (filledConts+1), "id", this.getPackageName());
            int contID = getResources().getIdentifier("imageCont" + (filledConts+1), "id", this.getPackageName());

            if (filledConts == maxButtons) {
                // Log.d(TAG, "filledConts [" + filledConts + "] == maxButtons [" + maxButtons + "]");
                break;
            }

            filledConts += 1;
            customNotifView.setViewVisibility(contID, View.VISIBLE);

            Drawable d;
            if (isColorized) {
                d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskList.get(i).icon, getColor));
            } else {
                d = taskList.get(i).icon;
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

        Notification notification = new Notification.Builder(WatchfulService.this).
                setContentTitle("Notification title")
                .setContentText("Notification content")
                .setSmallIcon(R.drawable.ic_apps_white)
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
