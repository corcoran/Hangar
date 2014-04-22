package ca.mimic.apphangar;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;

import java.util.Random;

public class NotificationBar {
    Context mContext;
    RemoteViews customNotifView;

    boolean isColorized;
    int getColor;

    int mResID;
    int mContID;

    int pendingNum;
    String mTaskPackage;


    NotificationBar(String packageName, int rootID, int resID, int contID) {
        mTaskPackage = packageName;
        mResID = resID;
        mContID = contID;

        customNotifView = new RemoteViews(mTaskPackage, rootID);

        // Generate random number for pendingIntent
        Random r = new Random();
        pendingNum = r.nextInt(99 - 1 + 1) + 1;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    protected void setPrefs(SharedPreferences prefs) {
        // set Prefs for items
        isColorized = prefs.getBoolean(Settings.COLORIZE_PREFERENCE, Settings.COLORIZE_DEFAULT);
        getColor = prefs.getInt(Settings.ICON_COLOR_PREFERENCE, Settings.ICON_COLOR_DEFAULT);
    }

    protected boolean newItem(Tools.TaskInfo taskItem, int count) {
        RemoteViews item = new RemoteViews(mTaskPackage, R.layout.notification_item);

        Drawable taskIcon, d;
        PackageManager pkgm;
        try {
            pkgm = mContext.getPackageManager();
            ApplicationInfo appInfo = pkgm.getApplicationInfo(taskItem.packageName, 0);
            taskIcon = appInfo.loadIcon(pkgm);
        } catch (Exception e) {
            return false;
        }

        if (isColorized) {
            d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskIcon, getColor));
        } else {
            d = taskIcon;
        }

        Bitmap bmpIcon = ((BitmapDrawable) d).getBitmap();
        item.setImageViewBitmap(mResID, bmpIcon);

        Intent intent;
        try {
            intent = pkgm.getLaunchIntentForPackage(taskItem.packageName);
            if (intent == null) {
                Tools.HangarLog("Couldn't get intent for ["+ taskItem.packageName +"] className:" + taskItem.className);
                throw new PackageManager.NameNotFoundException();
            }
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction("action" + (count));
            PendingIntent activity = PendingIntent.getActivity(mContext, pendingNum, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            item.setOnClickPendingIntent(mContID, activity);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        customNotifView.addView(R.id.notifContainer, item);
        return true;
    }
    protected RemoteViews getView() {
        return customNotifView;
    }
}
