/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Modifications to original by Jeff Corcoran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.mimic.apphangar;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class IconCacheHelper {
    private static final String RESOURCE_FILE_PREFIX = "icon_";

    private IconPackHelper mIconPackHelper;

    private final Context mContext;
    private final PackageManager mPackageManager;

    private int mIconDpi;

    public IconCacheHelper(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mIconDpi = activityManager.getLauncherLargeIconDensity();
        // need to set mIconDpi before getting default icon

        mIconPackHelper = new IconPackHelper(context);
        loadIconPack();
    }

    private void loadIconPack() {
        mIconPackHelper.unloadIconPack();
        Settings.PrefsGet prefs = new Settings.PrefsGet(mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_MULTI_PROCESS));
        SharedPreferences mPrefs = prefs.prefsGet();
        String iconPack = mPrefs.getString(Settings.ICON_PACK_PREFERENCE, "");
        if (!TextUtils.isEmpty(iconPack) && !mIconPackHelper.loadIconPack(iconPack)) {
            SharedPreferences.Editor mEditor = prefs.editorGet();
            mEditor.putString(Settings.ICON_PACK_PREFERENCE, "");
            mEditor.apply();
        }
    }


    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }
        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public Drawable getFullResIcon(ActivityInfo info) {

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId;
            if (mIconPackHelper != null && mIconPackHelper.isIconPackLoaded()) {
                iconId = mIconPackHelper.getResourceIdForActivityIcon(info);
                if (iconId != 0) {
                    return getFullResIcon(mIconPackHelper.getIconPackResources(), iconId);
                }
            }
            iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }

        return getFullResDefaultActivityIcon();
    }

    public static int chmod(File path, int mode) {
        try {
            Class fileUtils = Class.forName("android.os.FileUtils");
            Method setPermissions = fileUtils.getMethod("setPermissions",
                    String.class, int.class, int.class, int.class);
            return (Integer) setPermissions.invoke(null, path.getAbsolutePath(),
                    mode, -1, -1);
        } catch (Exception e) {
            Tools.HangarLog("chmod Exception: " + e);
            return 0;
        }
    }

    public static String preloadIcon(Context context, ComponentName componentName, Bitmap icon, int size) {
        final String key = componentName.flattenToString();
        FileOutputStream resourceFile = null;
        File file = null;
        try {
            file = new File(context.getCacheDir(), getResourceFilename(componentName));
            resourceFile = new FileOutputStream(file);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (Bitmap.createScaledBitmap(icon, size, size, true).compress(android.graphics.Bitmap.CompressFormat.PNG, 75, os)) {
                byte[] buffer = os.toByteArray();
                resourceFile.write(buffer, 0, buffer.length);
            } else {
                Tools.HangarLog("failed to encode cache for " + key);
                return null;
            }
        } catch (FileNotFoundException e) {
            Tools.HangarLog("failed to pre-load cache for " + key);
        } catch (IOException e) {
            Tools.HangarLog("failed to pre-load cache for " + key);
        } finally {
            if (resourceFile != null) {
                try {
                    resourceFile.flush();
                    resourceFile.close();
                    chmod(file, 0664);
                } catch (IOException e) {
                    Tools.HangarLog("failed to save restored icon for: " + key);
                }
            }
        }
        return file.getPath();
    }

    protected static String getPreloadedIconUri(Context context, ComponentName componentName) {
        File file = new File(context.getCacheDir(), getResourceFilename(componentName));
        if (file.exists()) {
            return file.getPath();
        } else {
            return null;
        }
    }

    protected static Bitmap getPreloadedIcon(Context context, ComponentName componentName) {
        final String key = componentName.flattenToShortString();

        Bitmap icon = null;
        FileInputStream resourceFile = null;
        try {
            File file = new File(context.getCacheDir(), getResourceFilename(componentName));
            resourceFile = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int bytesRead = 0;
            while(bytesRead >= 0) {
                bytes.write(buffer, 0, bytesRead);
                bytesRead = resourceFile.read(buffer, 0, buffer.length);
            }
            icon = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size());
            if (icon == null) {
                Tools.HangarLog("failed to decode pre-load icon for " + key);
            }
        } catch (FileNotFoundException e) {
            Tools.HangarLog("there is no restored icon for: " + key);
        } catch (IOException e) {
            Tools.HangarLog("failed to read pre-load icon for: " + key);
        } finally {
            if(resourceFile != null) {
                try {
                    resourceFile.close();
                } catch (IOException e) {
                    Tools.HangarLog("failed to manage pre-load icon file: " + key);
                }
            }
        }

        if (icon != null) {
            // TODO: handle alpha mask in the view layer
            Bitmap b = Bitmap.createBitmap(Math.max(icon.getWidth(), 1),
                    Math.max(icon.getHeight(), 1),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint paint = new Paint();
            // paint.setAlpha(127);
            c.drawBitmap(icon, 0, 0, paint);
            c.setBitmap(null);
            icon.recycle();
            icon = b;
        }

        return  icon;
    }

    protected static String getResourceFilename(ComponentName component) {
        String resourceName = component.flattenToShortString();
        String filename = resourceName.replace(File.separatorChar, '_');
        return RESOURCE_FILE_PREFIX + filename;
    }
}
