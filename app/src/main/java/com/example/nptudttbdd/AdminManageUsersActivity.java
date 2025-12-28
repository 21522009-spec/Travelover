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
}