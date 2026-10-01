package com.zygisk_enc.RecorderX;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppSelectorAdapter extends RecyclerView.Adapter<AppSelectorAdapter.AppViewHolder> {

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo app);
    }

    private final Context context;
    private final List<AppInfo> masterList = new ArrayList<>();
    private final List<AppInfo> displayList = new ArrayList<>();
    private final OnAppSelectedListener listener;
    private int selectedUid;
    private int accentColor;
    private String currentQuery = "";
    private boolean nonSystemOnly = true;

    public AppSelectorAdapter(Context context, int selectedUid, int accentColor, OnAppSelectedListener listener) {
        this.context = context;
        this.selectedUid = selectedUid;
        this.accentColor = accentColor;
        this.listener = listener;
    }

    public void setApps(List<AppInfo> apps) {
        masterList.clear();
        if (apps != null) {
            masterList.addAll(apps);
        }
        applyFilters();
    }

    public void setSelectedUid(int uid) {
        this.selectedUid = uid;
        notifyDataSetChanged();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        notifyDataSetChanged();
    }

    public void filter(String query, boolean nonSystemOnly) {
        this.currentQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        this.nonSystemOnly = nonSystemOnly;
        applyFilters();
    }

    private void applyFilters() {
        displayList.clear();
        for (AppInfo app : masterList) {
            // Always keep the "All Apps" system option visible
            if (app.isAllAppsOption()) {
                if (currentQuery.isEmpty() || app.getAppName().toLowerCase(Locale.getDefault()).contains(currentQuery)) {
                    displayList.add(app);
                }
                continue;
            }

            // Check non-system filter
            if (nonSystemOnly && app.isSystemApp()) {
                continue;
            }

            // Check search text filter
            if (!currentQuery.isEmpty()) {
                boolean matchesName = app.getAppName().toLowerCase(Locale.getDefault()).contains(currentQuery);
                boolean matchesPkg = app.getPackageName().toLowerCase(Locale.getDefault()).contains(currentQuery);
                boolean matchesUid = String.valueOf(app.getUid()).contains(currentQuery);
                if (!matchesName && !matchesPkg && !matchesUid) {
                    continue;
                }
            }

            displayList.add(app);
        }
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return displayList.size();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_selector, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = displayList.get(position);

        holder.appName.setText(app.getAppName());
        holder.appPackage.setText(app.getPackageName());

        if (app.isAllAppsOption()) {
            holder.appIcon.setImageResource(R.drawable.ic_speaker);
            holder.appIcon.setImageTintList(ColorStateList.valueOf(accentColor));
            holder.appUidBadge.setText("SYSTEM");
        } else {
            if (app.getIcon() != null) {
                holder.appIcon.setImageDrawable(app.getIcon());
                holder.appIcon.setImageTintList(null);
            } else {
                holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
                holder.appIcon.setImageTintList(null);
            }
            holder.appUidBadge.setText(context.getString(R.string.target_audio_uid_format, app.getUid()));
        }

        boolean isSelected = (app.getUid() == selectedUid);
        holder.ivSelectedCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.ivSelectedCheck.setImageTintList(ColorStateList.valueOf(accentColor));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppSelected(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView appIcon;
        final TextView appName;
        final TextView appPackage;
        final TextView appUidBadge;
        final ImageView ivSelectedCheck;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appPackage = itemView.findViewById(R.id.appPackage);
            appUidBadge = itemView.findViewById(R.id.appUidBadge);
            ivSelectedCheck = itemView.findViewById(R.id.ivSelectedCheck);
        }
    }
}
