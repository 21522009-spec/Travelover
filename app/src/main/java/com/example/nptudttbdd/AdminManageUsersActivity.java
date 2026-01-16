package com.example.nptudttbdd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class AdminManageUsersActivity extends AppCompatActivity {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, AdminManageUsersActivity.class);
    }

    private TravelDataRepository repository;
    private LinearLayout userListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);
        userListContainer = findViewById(R.id.userList);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        renderUsers();
    }

    private void renderUsers() {
        List<UserAccount> users = repository.getUsers();
        userListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (UserAccount user : users) {
            View itemView = inflater.inflate(R.layout.item_admin_user, userListContainer, false);
            bindUser(itemView, user);
            userListContainer.addView(itemView);
        }
    }

    private void bindUser(View itemView, UserAccount user) {
        TextView tvName = itemView.findViewById(R.id.tvName);
        TextView tvEmail = itemView.findViewById(R.id.tvEmail);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        Button btnLock = itemView.findViewById(R.id.btnLock);
        Button btnDelete = itemView.findViewById(R.id.btnDelete);
        EditText etReason = itemView.findViewById(R.id.etDeleteReason);
        Button btnConfirmDelete = itemView.findViewById(R.id.btnConfirmDelete);
        // Owner statistics views
        LinearLayout layoutOwnerStats = itemView.findViewById(R.id.layoutOwnerStats);
        TextView tvOwnerRevenue = itemView.findViewById(R.id.tvOwnerRevenue);
        TextView tvOwnerRooms = itemView.findViewById(R.id.tvOwnerRooms);
        TextView tvOwnerBookings = itemView.findViewById(R.id.tvOwnerBookings);

        tvName.setText(user.getName());
        tvEmail.setText(getString(R.string.admin_user_email_template, user.getEmail(), user.getPhone()));
        updateStatus(tvStatus, user);
        btnLock.setText(user.isLocked() ? R.string.admin_user_unlock : R.string.admin_user_lock);

        btnLock.setOnClickListener(v -> {
            repository.toggleUserLock(user.getId());
            updateStatus(tvStatus, user);
            btnLock.setText(user.isLocked() ? R.string.admin_user_unlock : R.string.admin_user_lock);
            // Cập nhật trạng thái phê duyệt trên Firebase khi khóa/mở khóa
            String uid = user.getId();
            if (!uid.contains("-")) {  // chỉ cập nhật nếu ID có vẻ là UID thật (bỏ qua user mẫu)
                FirebaseDatabase.getInstance().getReference("Users").child(uid)
                        .child("approved").setValue(!user.isLocked());
            }
            Toast.makeText(this,
                    user.isLocked() ? R.string.admin_user_locked : R.string.admin_user_unlocked,
                    Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v -> {
            if (etReason.getVisibility() == View.VISIBLE) {
                etReason.setVisibility(View.GONE);
                btnConfirmDelete.setVisibility(View.GONE);
            } else {
                etReason.setVisibility(View.VISIBLE);
                btnConfirmDelete.setVisibility(View.VISIBLE);
            }
        });

        btnConfirmDelete.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                etReason.setError(getString(R.string.admin_user_delete_reason_error));
                etReason.requestFocus();
                return;
            }
            repository.deleteUser(user.getId());
            Toast.makeText(this,
                    getString(R.string.admin_user_deleted, user.getName()),
                    Toast.LENGTH_SHORT).show();
            renderUsers();
        });

        // Display owner statistics for real accounts (UIDs without hyphens). Demo
        // data generated by TravelDataRepository uses random UUIDs that
        // contain hyphens, so these should not show stats. For real
        // Firebase users (no hyphens), we show the dashboard metrics.
        if (user.getId() != null && !user.getId().contains("-")) {
            layoutOwnerStats.setVisibility(View.VISIBLE);
            // Initialize default values before asynchronous queries
            tvOwnerRevenue.setText("0đ");
            tvOwnerRooms.setText("0");
            tvOwnerBookings.setText("0");
            loadOwnerStats(user.getId(), tvOwnerRevenue, tvOwnerRooms, tvOwnerBookings);
        } else {
            layoutOwnerStats.setVisibility(View.GONE);
        }
    }

    private void updateStatus(TextView tvStatus, UserAccount user) {
        if (user.isLocked()) {
            tvStatus.setText(R.string.admin_user_status_locked);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            tvStatus.setText(R.string.admin_user_status_active);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
        }
    }

    /**
     * Computes and populates owner statistics for the given user. This method
     * queries Firebase Realtime Database to calculate three metrics:
     *   1) Total number of rooms owned (from the Places node)
     *   2) Today's revenue (from the Payments node)
     *   3) Number of bookings today (from the Payments node, one payment per booking)
     *
     * The results are asynchronously written into the provided TextViews. If
     * any of the queries fail, the corresponding TextViews will remain with
     * their default values.
     *
     * @param ownerId    the UID of the owner account
     * @param tvRevenue  TextView to display the owner's revenue today
     * @param tvRooms    TextView to display the total number of rooms owned
     * @param tvBookings TextView to display the number of bookings today
     */
    private void loadOwnerStats(@NonNull String ownerId,
                               @NonNull TextView tvRevenue,
                               @NonNull TextView tvRooms,
                               @NonNull TextView tvBookings) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        // Count total rooms owned by this owner
        Query placesQuery = dbRef.child("Places").orderByChild("ownerId").equalTo(ownerId);
        placesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalRooms = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    totalRooms += 1;
                }
                tvRooms.setText(String.valueOf(totalRooms));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore errors for room count
            }
        });

        // Sum today's revenue and count today's bookings from payments
        Query paymentsQuery = dbRef.child("Payments").orderByChild("ownerId").equalTo(ownerId);
        paymentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long revenueToday = 0;
                int bookingsToday = 0;
                // Determine start and end of current day in milliseconds
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                long startOfDay = cal.getTimeInMillis();
                long endOfDay = startOfDay + 24L * 60L * 60L * 1000L - 1L;
                for (DataSnapshot paymentData : snapshot.getChildren()) {
                    Long ts = paymentData.child("timestamp").getValue(Long.class);
                    Long amount = paymentData.child("amount").getValue(Long.class);
                    if (ts != null && amount != null) {
                        if (ts >= startOfDay && ts <= endOfDay) {
                            revenueToday += amount;
                            bookingsToday += 1;
                        }
                    }
                }
                tvRevenue.setText(TravelDataRepository.formatCurrency(revenueToday));
                tvBookings.setText(String.valueOf(bookingsToday));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore errors for payments
            }
        });
    }
}