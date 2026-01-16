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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public final class ChatButtonManager {

    private static final int DRAG_THRESHOLD = 10;

    private ChatButtonManager() {
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
            if (activity instanceof OwnerMessagesActivity
                    || activity instanceof OwnerConversationsActivity
                    || activity instanceof UserMessagesActivity
                    || activity instanceof UserConversationsActivity) {
                return;
            }

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                // Not logged in
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(uid)
                    .child("role");
            // Decide navigation based on role stored in Users/{uid}/role
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String role = snapshot.getValue(String.class);
                    if (role == null) role = "user";
                    Intent intent;
                    if ("owner".equalsIgnoreCase(role)) {
                        intent = new Intent(activity, OwnerConversationsActivity.class);
                    } else {
                        intent = new Intent(activity, UserConversationsActivity.class);
                    }
                    activity.startActivity(intent);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Fallback: open user conversations
                    Intent intent = new Intent(activity, UserConversationsActivity.class);
                    activity.startActivity(intent);
                }
            });
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