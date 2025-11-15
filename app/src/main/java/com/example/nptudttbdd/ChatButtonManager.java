package com.example.nptudttbdd;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public final class ChatButtonManager {

    private static final int DRAG_THRESHOLD = 10;

    private ChatButtonManager() {
        // Utility class
    }

    public static void attach(@NonNull AppCompatActivity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null || root.findViewById(R.id.chatFloatingButton) != null) {
            return;
        }

        FloatingActionButton chatButton = (FloatingActionButton) LayoutInflater.from(activity)
                .inflate(R.layout.view_draggable_chat_button, root, false);
        root.addView(chatButton);

        setupDragBehavior(chatButton);
        setupNavigation(activity, chatButton);
    }

    private static void setupNavigation(@NonNull AppCompatActivity activity,
                                        @NonNull FloatingActionButton chatButton) {
        chatButton.setOnClickListener(v -> {
            if (activity instanceof OwnerMessagesActivity) {
                return;
            }
            Intent intent = new Intent(activity, OwnerMessagesActivity.class);
            activity.startActivity(intent);
        });
    }

    private static void setupDragBehavior(@NonNull View chatButton) {
        final boolean[] isDragging = {false};
        final float[] downPosition = new float[2];
        final float[] offset = new float[2];

        chatButton.setOnTouchListener((v, event) -> {
            ViewGroup parent = (ViewGroup) v.getParent();
            if (!(parent instanceof FrameLayout)) {
                return false;
            }
            FrameLayout frameLayout = (FrameLayout) parent;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging[0] = false;
                    downPosition[0] = event.getRawX();
                    downPosition[1] = event.getRawY();
                    offset[0] = v.getX() - event.getRawX();
                    offset[1] = v.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - downPosition[0];
                    float deltaY = event.getRawY() - downPosition[1];

                    if (!isDragging[0] && (Math.abs(deltaX) > DRAG_THRESHOLD
                            || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                        isDragging[0] = true;
                    }

                    if (isDragging[0]) {
                        float newX = event.getRawX() + offset[0];
                        float newY = event.getRawY() + offset[1];

                        int maxX = frameLayout.getWidth() - v.getWidth();
                        int maxY = frameLayout.getHeight() - v.getHeight();

                        newX = clamp(newX, 0, maxX);
                        newY = clamp(newY, 0, maxY);

                        v.setX(newX);
                        v.setY(newY);
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging[0]) {
                        isDragging[0] = false;
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}