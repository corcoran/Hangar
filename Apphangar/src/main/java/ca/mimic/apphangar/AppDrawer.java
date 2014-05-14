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
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.Random;

public class AppDrawer {
    Context mContext;
    RemoteViews mRowView;
    RemoteViews mLastItem;

    boolean isColorized;
    int getColor;

    int mImageButtonLayout;
    int mImageContLayout;
    int mRowId;

    int pendingNum;
    String mTaskPackage;


    AppDrawer(String packageName) {
        mTaskPackage = packageName;
    }

    protected void createRow(int rowLayout, int rowId) {
        mRowId = rowId;

        mRowView = new RemoteViews(mTaskPackage, rowLayout);
        mRowView.removeAllViews(mRowId);

        // Generate random number for pendingIntent
        Random r = new Random();
        pendingNum = r.nextInt(99 - 1 + 1) + 1;
    }

    protected void setImageLayouts(int imageButtonLayout, int imageContLayout) {
        mImageButtonLayout = imageButtonLayout;
        mImageContLayout = imageContLayout;
    }

    public void setContext(Context context) {
        mContext = context;
    }
    protected void setRowBackgroundColor(int color) {
        mRowView.setInt(mRowId, "setBackgroundColor", color);
    }

    protected void setPrefs(SharedPreferences prefs) {
        // set Prefs for mLastItems
        isColorized = prefs.getBoolean(Settings.COLORIZE_PREFERENCE, Settings.COLORIZE_DEFAULT);
        getColor = prefs.getInt(Settings.ICON_COLOR_PREFERENCE, Settings.ICON_COLOR_DEFAULT);
    }

    protected boolean newItem(Tools.TaskInfo taskItem, int mLastItemLayout, int count) {
        mLastItem = new RemoteViews(mTaskPackage, mLastItemLayout);
        if (taskItem.packageName == null) {
            // Dummy invisible item
            return true;
        }

        Drawable taskIcon, d;
        PackageManager pkgm;
        Uri uri = null;
        try {
            pkgm = mContext.getPackageManager();
            ApplicationInfo appInfo = pkgm.getApplicationInfo(taskItem.packageName, 0);
            taskIcon = appInfo.loadIcon(pkgm);
            if(appInfo.icon != 0) {
                uri = Uri.parse("android.resource://" + taskItem.packageName + "/" + appInfo.icon);
            }
        } catch (Exception e) {
            return false;
        }

        if (isColorized) {
            d = new BitmapDrawable(ColorHelper.getColoredBitmap(taskIcon, getColor));
            Bitmap bmpIcon = ((BitmapDrawable) d).getBitmap();
            int size = Tools.dpToPx(mContext, 128);
            mLastItem.setImageViewBitmap(mImageButtonLayout, Bitmap.createScaledBitmap(bmpIcon, size, size, false));
        } else {
            mLastItem.setImageViewUri(mImageButtonLayout, uri);
        }

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
            mLastItem.setOnClickPendingIntent(mImageContLayout, activity);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return true;
    }
    protected void addItem() {
        mRowView.addView(mRowId, mLastItem);
    }
    protected void setItemVisibility(int visibility) {
        mLastItem.setViewVisibility(mImageContLayout, visibility);
    }
    protected RemoteViews getRow() {
        return mRowView;
    }
}
