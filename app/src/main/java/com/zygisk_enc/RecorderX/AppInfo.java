package com.zygisk_enc.RecorderX;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AppInfo implements Comparable<AppInfo> {
    public static final int UID_ALL_APPS = -1;

    private final String appName;
    private final String packageName;
    private final int uid;
    @Nullable
    private final Drawable icon;
    private final boolean isSystemApp;

    public AppInfo(@NonNull String appName, @NonNull String packageName, int uid, @Nullable Drawable icon, boolean isSystemApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.uid = uid;
        this.icon = icon;
        this.isSystemApp = isSystemApp;
    }

    @NonNull
    public String getAppName() {
        return appName;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public int getUid() {
        return uid;
    }

    @Nullable
    public Drawable getIcon() {
        return icon;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public boolean isAllAppsOption() {
        return uid == UID_ALL_APPS;
    }

    @Override
    public int compareTo(@NonNull AppInfo other) {
        // "All Apps" option always comes first
        if (this.isAllAppsOption() && !other.isAllAppsOption()) return -1;
        if (!this.isAllAppsOption() && other.isAllAppsOption()) return 1;
        return this.appName.compareToIgnoreCase(other.appName);
    }
}
