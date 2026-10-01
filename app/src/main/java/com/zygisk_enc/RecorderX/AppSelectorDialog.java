package com.zygisk_enc.RecorderX;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppSelectorDialog {

    public interface OnAppSelectedCallback {
        void onAppSelected(AppInfo app);
    }

    private final Activity activity;
    private final int currentSelectedUid;
    private final int accentColor;
    private final OnAppSelectedCallback callback;

    private Dialog dialog;
    private AppSelectorAdapter adapter;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private RecyclerView rvApps;
    private EditText etSearch;
    private ImageView ivClear;
    private MaterialCheckBox cbNonSystemOnly;
    private ExecutorService executor;

    public AppSelectorDialog(@NonNull Activity activity, int currentSelectedUid, int accentColor, @NonNull OnAppSelectedCallback callback) {
        this.activity = activity;
        this.currentSelectedUid = currentSelectedUid;
        this.accentColor = accentColor;
        this.callback = callback;
    }

    public void show() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View contentView = LayoutInflater.from(activity).inflate(R.layout.dialog_app_selector, null);

        MaterialCardView card = contentView.findViewById(R.id.cardAppSelectorDialog);
        if (card != null) {
            float density = activity.getResources().getDisplayMetrics().density;
            card.setStrokeWidth((int) (2 * density));
            card.setStrokeColor(ColorStateList.valueOf(accentColor));
        }

        TextView title = contentView.findViewById(R.id.dialogAppSelectorTitle);
        if (title != null) {
            title.setTextColor(accentColor);
        }

        pbLoading = contentView.findViewById(R.id.pbLoadingApps);
        if (pbLoading != null) {
            pbLoading.setIndeterminateTintList(ColorStateList.valueOf(accentColor));
        }

        tvEmpty = contentView.findViewById(R.id.tvEmptyApps);
        rvApps = contentView.findViewById(R.id.rvAppList);
        etSearch = contentView.findViewById(R.id.etAppSearch);
        ivClear = contentView.findViewById(R.id.ivClearSearch);
        cbNonSystemOnly = contentView.findViewById(R.id.cbNonSystemOnly);

        if (cbNonSystemOnly != null) {
            cbNonSystemOnly.setButtonTintList(ColorStateList.valueOf(accentColor));
        }

        MaterialButton btnCancel = contentView.findViewById(R.id.btnCancelAppSelector);
        if (btnCancel != null) {
            btnCancel.setBackgroundTintList(ColorStateList.valueOf(accentColor));
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        ImageView btnClose = contentView.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        rvApps.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new AppSelectorAdapter(activity, currentSelectedUid, accentColor, app -> {
            if (callback != null) {
                callback.onAppSelected(app);
            }
            dialog.dismiss();
        });
        rvApps.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s != null ? s.toString() : "";
                ivClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                boolean nonSystem = cbNonSystemOnly.isChecked();
                adapter.filter(query, nonSystem);
                updateEmptyState();
            }
        });

        ivClear.setOnClickListener(v -> etSearch.setText(""));

        cbNonSystemOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
            adapter.filter(query, isChecked);
            updateEmptyState();
        });

        FrameLayout container = new FrameLayout(activity);
        int margin = (int) (20 * activity.getResources().getDisplayMetrics().density);
        container.setPadding(margin, margin, margin, margin);
        container.addView(contentView);
        dialog.setContentView(container);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.setBlurBehindRadius((int) (16 * activity.getResources().getDisplayMetrics().density));
                window.setAttributes(attrs);
            }
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }

        View rootView = activity.findViewById(android.R.id.content);
        if (rootView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rootView.setRenderEffect(RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP));
        }

        dialog.setOnDismissListener(d -> {
            if (rootView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootView.setRenderEffect(null);
            }
            if (executor != null) {
                executor.shutdownNow();
            }
        });

        dialog.show();
        loadApplicationsAsync();
    }

    private void updateEmptyState() {
        if (tvEmpty != null && adapter != null) {
            tvEmpty.setVisibility(adapter.getFilteredCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void loadApplicationsAsync() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager pm = activity.getPackageManager();
            List<ApplicationInfo> installedApps;
            try {
                installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            } catch (Exception e) {
                installedApps = new ArrayList<>();
            }

            String ourPackage = activity.getPackageName();
            List<AppInfo> appList = new ArrayList<>();

            // Option 1: Entire system audio (no UID filter)
            appList.add(new AppInfo(
                    activity.getString(R.string.target_audio_all_apps_option),
                    activity.getString(R.string.target_audio_all_apps_sub),
                    AppInfo.UID_ALL_APPS,
                    null,
                    false
            ));

            for (ApplicationInfo info : installedApps) {
                if (info == null || info.packageName == null) continue;
                // Exclude own app from target audio capture
                if (info.packageName.equals(ourPackage)) continue;

                boolean isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isUpdatedSystem = (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                boolean systemApp = isSystem && !isUpdatedSystem;

                String label;
                try {
                    CharSequence cs = info.loadLabel(pm);
                    label = (cs != null) ? cs.toString() : info.packageName;
                } catch (Exception e) {
                    label = info.packageName;
                }

                Drawable icon = null;
                try {
                    icon = info.loadIcon(pm);
                } catch (Exception ignored) {}

                appList.add(new AppInfo(label, info.packageName, info.uid, icon, systemApp));
            }

            Collections.sort(appList);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                if (adapter != null) {
                    adapter.setApps(appList);
                    boolean nonSystem = (cbNonSystemOnly != null) && cbNonSystemOnly.isChecked();
                    String query = (etSearch != null && etSearch.getText() != null) ? etSearch.getText().toString() : "";
                    adapter.filter(query, nonSystem);
                    updateEmptyState();
                }
            });
        });
    }
}
