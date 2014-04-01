package ca.mimic.apphangar;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Collections;
import java.util.List;


/**
 * Implementation of App Widget functionality.
 */
public class StatsWidget extends AppWidgetProvider {

    private static TasksDataSource db;
    private static Context mContext;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("Apphangar", "onUpdate");
        // There may be multiple widgets active, so update all of them
        mContext = context;
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Apphangar", "onReceive");
        mContext = context;
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, StatsWidget.class));

        for(int id : ids) {
            Log.d("Apphangar", "per id: " + id);
            try {
                updateAppWidget(context, mgr, id);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.d("Apphangar", "NPE onReceive");
            }
            // mgr.notifyAppWidgetViewDataChanged(id, R.id.taskRoot);
        }
        super.onReceive(context, intent);
    }

    public static int dpToPx(int dp) {
        Resources r = mContext.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(dpToPx(100), dpToPx(5), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {

        Log.d("Apphangar", "updateAppWidget");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stats_widget);
        PackageManager pkgm = context.getPackageManager();
        String packageName = context.getPackageName();
        Intent intent;

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
            if (count == 8) {
                break;
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
            int[] colors = new int[]{barColor, Math.round(secondsColor * 2.5f), 0x00000000, Math.round((100-secondsColor) * 2.5f)};
            Log.d("Apphangar", "BarDrawable: " + colors[0] + ", " + colors[1] + ", " + colors[2] + ", " + colors[3]);
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
}


