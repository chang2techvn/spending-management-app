package com.example.spending_management_app.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.spending_management_app.R;

public class ToastHelper {
    // Helper method để hiển thị toast ở top
    public static void showTopToast(Activity activity, String message, int duration) {
        try {
            if (activity != null) {
                Toast toast = Toast.makeText(activity.getApplicationContext(), message, duration);
                // Đặt toast ở TOP của màn hình với margin lớn
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 250);
                toast.show();

                // Log để debug
                android.util.Log.d("ToastHelper", "Top toast shown: " + message);
            }
        } catch (Exception e) {
            android.util.Log.e("ToastHelper", "Error showing top toast", e);
        }
    }

    // Method tạo custom view toast ở TOP với UI đẹp và animation
    public static void showCustomTopToast(Activity activity, String message) {
        showCustomToastWithType(activity, message, "success");
    }

    // Method tổng quát cho các loại toast
    public static void showCustomToastWithType(Activity activity, String message, String type) {
        try {
            // Tạo custom toast view với layout đẹp
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(activity);
            android.view.View layout = inflater.inflate(R.layout.custom_toast_layout, null);

            // Set background dựa vào type
            switch (type) {
                case "success":
                    layout.setBackgroundResource(R.drawable.toast_success_background);
                    break;
                case "error":
                    layout.setBackgroundResource(R.drawable.toast_error_background);
                    break;
                default:
                    layout.setBackgroundResource(R.drawable.toast_background);
                    break;
            }

            android.widget.TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);

            Toast toast = new Toast(activity);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);

            // Animation dựa vào type
            layout.setAlpha(0f);
            if ("error".equals(type)) {
                // Animation cho error với shake effect
                layout.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        // Hiệu ứng rung nhẹ
                        layout.animate().translationX(-8).setDuration(80)
                            .withEndAction(() -> layout.animate().translationX(8).setDuration(80)
                                .withEndAction(() -> layout.animate().translationX(0).setDuration(80).start()).start()).start();
                    }).start();
            } else {
                // Animation bình thường cho success/info
                layout.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            }

            toast.show();

            // Animation slide out với timing khác nhau
            int delay = "error".equals(type) ? 4500 : 4000;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (layout.getParent() != null) {
                    layout.animate()
                        .translationX(layout.getWidth() + 100)
                        .alpha(0.2f)
                        .setDuration(600)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> android.util.Log.d("ToastHelper", "Toast slide out completed"))
                        .start();
                }
            }, delay);

            android.util.Log.d("ToastHelper", "Beautiful " + type + " toast shown: " + message);

        } catch (Exception e) {
            android.util.Log.e("ToastHelper", "Custom toast failed", e);
            // Fallback đơn giản
            Toast simpleToast = Toast.makeText(activity, message, Toast.LENGTH_LONG);
            simpleToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
            simpleToast.show();
        }
    }

    // Method để hiển thị toast ở layer cao nhất (trên cùng màn hình)
    public static void showToastOnTop(Activity activity, String message) {
        try {
            // Chỉ hiển thị 1 custom toast duy nhất ở TOP với UI đẹp
            showCustomTopToast(activity, message);
            android.util.Log.d("ToastHelper", "Single top toast shown: " + message);

        } catch (Exception e) {
            android.util.Log.e("ToastHelper", "Error showing top toast", e);
            // Simple fallback nếu custom toast fail
            try {
                Toast simpleToast = Toast.makeText(activity, message, Toast.LENGTH_LONG);
                simpleToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 200);
                simpleToast.show();
            } catch (Exception ex) {
                android.util.Log.e("ToastHelper", "Fallback toast failed", ex);
            }
        }
    }

    // Method riêng cho error toast
    public static void showErrorToast(Activity activity, String message) {
        showCustomToastWithType(activity, message, "error");
    }

}
