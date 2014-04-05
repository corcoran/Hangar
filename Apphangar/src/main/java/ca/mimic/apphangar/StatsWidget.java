package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Collections;
import java.util.List;

import ca.mimic.apphangar.Settings.PrefsGet;

/**
 * Implementation of App Widget functionality.
 */
public class StatsWidget extends AppWidgetProvider {

    protected static TasksDataSource db;
    protected static Context mContext;
    protected static PrefsGet prefs;

    protected static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("Apphangar", "onUpdate");
        // There may be multiple widgets active, so update all of them
        mContext = context;
        // final int N = appWidgetIds.length;
        // for (int i=0; i<N; i++) {
        //     Bundle options=appWidgetManager.getAppWidgetOptions(appWidgetIds[i]);
        //     updateAppWidget(context, appWidgetManager, appWidgetIds[i], options);
        // }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        context.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Apphangar", "onReceive");
        if (mContext == null) {
            mContext = context;
            IntentFilter filter = new IntentFilter();
            filter.addAction(BCAST_CONFIGCHANGED);
            context.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
        }

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, StatsWidget.class));

        for(int id : ids) {
            Log.d("Apphangar", "per id: " + id);
            try {
                Bundle options=mgr.getAppWidgetOptions(id);
                updateAppWidget(context, mgr, id, options);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.d("Apphangar", "NPE onReceive");
            }
            // mgr.notifyAppWidgetViewDataChanged(id, R.id.taskRoot);
        }
        super.onReceive(context, intent);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(Tools.dpToPx(mContext, 100), Tools.dpToPx(mContext, 5), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle options) {

        Log.d("Apphangar", "updateAppWidget");
        prefs = new PrefsGet(context.getSharedPreferences("StatsWidget", Context.MODE_PRIVATE));

        SharedPreferences mPrefs = prefs.prefsGet();

        int statsLayout;
        if (mPrefs.getBoolean(Settings.DIVIDER_PREFERENCE, Settings.DIVIDER_DEFAULT)) {
            statsLayout = R.layout.stats_widget;
        } else {
            statsLayout = R.layout.stats_widget_no_dividers;
        }
        boolean appsNoByWidgetSize = mPrefs.getBoolean(Settings.APPS_BY_WIDGET_SIZE_PREFERENCE, Settings.APPS_BY_WIDGET_SIZE_DEFAULT);
        int appsNo;
        int appsNoLs;

        Log.d("Apphangar", "minHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        Log.d("Apphangar", "maxHeight: " + options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        appsNo = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) - 15) / 35);
        appsNoLs = (int) Math.floor((options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) - 15) / 35);

        if (appsNoByWidgetSize && appsNo > 0) {
            Log.d("Apphangar", "appsNoByWidgetSize=true, appsNo=" + appsNo);
        } else {
            appsNo = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_DEFAULT)));
        }

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            if (appsNoByWidgetSize && appsNoLs > 0) {
                appsNo = appsNoLs;
                Log.d("Apphangar", "Landscape! appsNoByWidgetSize=true, appsNo=" + appsNo);
            } else {
                appsNo = Integer.parseInt(mPrefs.getString(Settings.STATS_WIDGET_APPSNO_LS_PREFERENCE, Integer.toString(Settings.STATS_WIDGET_APPSNO_LS_DEFAULT)));
            }
            Log.d("Apphangar", "LANDSCAPE");
        }

        int getColor = mPrefs.getInt(Settings.BACKGROUND_COLOR_PREFERENCE, Settings.BACKGROUND_COLOR_DEFAULT);

        RemoteViews views = new RemoteViews(context.getPackageName(), statsLayout);
        PackageManager pkgm = context.getPackageManager();
        String packageName = context.getPackageName();
        Intent intent;

        views.setInt(R.id.taskRoot, "setBackgroundColor", getColor);

        if (db == null) {
            db = new TasksDataSource(context);
            db.open();
        }
        int highestSeconds = db.getHighestSeconds();
        List<TasksModel> tasks = db.getAllTasks();
        Collections.sort(tasks, new Settings().new TasksComparator("seconds"));

        int count = 0;
        for (TasksModel task : tasks) {
            int appID = context.getResources().getIdentifier("appCont" + (count + 1), "id",
                    packageName);
            int iconID = context.getResources().getIdentifier("iconCont" + (count + 1), "id",
                    packageName);
            int labelID = context.getResources().getIdentifier("appName" + (count + 1), "id",
                    packageName);
            int imgID = context.getResources().getIdentifier("barImg" + (count + 1), "id",
                    packageName);
            int statsID = context.getResources().getIdentifier("statsCont" + (count + 1), "id",
                    packageName);

            if (task.getBlacklisted()) { continue; }
            if (count >= appsNo) {
                if (count == 12) { break; }
                views.setViewVisibility(appID, View.GONE);
                count++;
                continue;
            }

            Drawable taskIcon;
            try {
                ApplicationInfo appInfo = pkgm.getApplicationInfo(task.getPackageName(), 0);
                taskIcon = appInfo.loadIcon(pkgm);
            } catch (Exception e) {
                continue;
            }

            count++;

            Bitmap bmpIcon = ((BitmapDrawable) taskIcon).getBitmap();
            views.setImageViewBitmap(iconID, bmpIcon);
            views.setTextViewText(labelID, task.getName());

            // int maxWidth = dpToPx(250) - dpToPx(32+14+14); // ImageView + Margin? + Stats text?
            float secondsRatio = (float) task.getSeconds() / highestSeconds;
            int barColor;
            int secondsColor = (Math.round(secondsRatio * 100));
            if (secondsColor >= 80 ) {
                barColor = 0xFF34B5E2;
            } else if (secondsColor >= 60) {
                barColor = 0xFFAA66CC;
            } else if (secondsColor >= 40) {
                barColor = 0xFF74C353;
            } else if (secondsColor >= 20) {
                barColor = 0xFFFFBB33;
            } else {
                barColor = 0xFFFF4444;
            }
            int[] colors = new int[]{barColor, Tools.dpToPx(context, secondsColor-1), 0x00000000, Tools.dpToPx(mContext, 100-secondsColor)};
            Log.d("Apphangar", "BarDrawable: " + colors[0] + ", " + colors[1] + ", " + colors[2] + ", " + colors[3]);
            Log.d("Apphangar", "bar1 dp: " + Tools.pxToDp(context, secondsColor * 2.55f));
            Drawable sd = new BarDrawable(colors);
            Bitmap bmpIcon2 = drawableToBitmap(sd);
            views.setImageViewBitmap(imgID, bmpIcon2);

            int[] statsTime = new Settings().splitToComponentTimes(task.getSeconds());
            String statsString = ((statsTime[0] > 0) ? statsTime[0] + "h " : "") + ((statsTime[1] > 0) ? statsTime[1] + "m " : "") + ((statsTime[2] > 0) ? statsTime[2] + "s " : "");
            views.setTextViewText(statsID, statsString);

            try {
                intent = pkgm.getLaunchIntentForPackage(task.getPackageName());
                if (intent == null) {
                    count --;
                    throw new PackageManager.NameNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setAction("action" + (count));
                PendingIntent activity = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                views.setOnClickPendingIntent(appID, activity);
                views.setViewVisibility(appID, View.VISIBLE);
            } catch (PackageManager.NameNotFoundException e) {

            }
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {

            if ( myIntent.getAction().equals( BCAST_CONFIGCHANGED ) ) {
                final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(1);
                if (recentTasks.size() > 0) {
                    ComponentName task = recentTasks.get(0).baseActivity;
                    String taskPackage = task.getPackageName();
                    if (taskPackage.equals(Tools.getLauncher(context))) {
                        Log.d("Apphangar", "We're in the launcher changing orientation!");
                        StatsWidget.this.onReceive(context, new Intent());
                    }
                }

            }
        }
    };
}


