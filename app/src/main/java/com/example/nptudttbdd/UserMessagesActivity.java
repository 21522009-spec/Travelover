package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * USER chat screen (real-time).
 */
public class UserMessagesActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_CONVERSATION_TITLE = "extra_conversation_title";

    private UserChatMessageAdapter adapter;
    private String conversationId;
    private DatabaseReference conversationRef;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_messages);

        adapter = new UserChatMessageAdapter();

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        if (conversationId == null || conversationId.isEmpty()) {
            finish();
            return;
        }

        conversationRef = FirebaseDatabase.getInstance().getReference("Conversations").child(conversationId);
        messagesRef = conversationRef.child("messages");

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnUserHome = findViewById(R.id.btnUserHome);
        btnBack.setOnClickListener(v -> finish());
        btnUserHome.setOnClickListener(v ->
                startActivity(new Intent(UserMessagesActivity.this, MainActivity.class))
        );

        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        String conversationTitle = getIntent().getStringExtra(EXTRA_CONVERSATION_TITLE);
        if (tvChatTitle != null) {
            tvChatTitle.setText(conversationTitle != null ? conversationTitle : getString(R.string.user_conversation_title));
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        EditText edtMessage = findViewById(R.id.edtMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);

        // Mark read for user
        conversationRef.child("hasNewForUser").setValue(false);
        conversationRef.child("hasNewMessage").setValue(false);

        // Listen for conversation messages in real-time
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ChatMessage> messageList = new ArrayList<>();
                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    String msgId = msgSnapshot.child("id").getValue(String.class);
                    String senderStr = msgSnapshot.child("sender").getValue(String.class);
                    String content = msgSnapshot.child("message").getValue(String.class);
                    Long time = msgSnapshot.child("timestamp").getValue(Long.class);

                    // Backward compatibility: allow {text} field
                    if (content == null) {
                        content = msgSnapshot.child("text").getValue(String.class);
                    }
                    if (senderStr == null) {
                        // If only senderId exists, infer (best effort)
                        String senderId = msgSnapshot.child("senderId").getValue(String.class);
                        if (FirebaseAuth.getInstance().getCurrentUser() != null
                                && FirebaseAuth.getInstance().getCurrentUser().getUid().equals(senderId)) {
                            senderStr = "USER";
                        } else {
                            senderStr = "OWNER";
                        }
                    }
                    if (content == null || senderStr == null) {
                        continue;
                    }

                    ChatMessage.Sender sender;
                    try {
                        sender = ChatMessage.Sender.valueOf(senderStr.toUpperCase(Locale.getDefault()));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    long timestamp = time != null ? time : 0L;
                    String id = msgId != null ? msgId : UUID.randomUUID().toString();
                    messageList.add(new ChatMessage(id, sender, content, timestamp));
                }
                adapter.submitList(messageList);
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // ignore
            }
        });

        btnSend.setOnClickListener(v -> {
            String content = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                edtMessage.setError(getString(R.string.message_error_empty));
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(UserMessagesActivity.this, "Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference newMsgRef = messagesRef.push();
            String newMsgId = newMsgRef.getKey();
            if (newMsgId == null) {
                Toast.makeText(UserMessagesActivity.this, "Không thể gửi tin nhắn, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> msgData = new HashMap<>();
            msgData.put("id", newMsgId);
            msgData.put("sender", "USER");
            msgData.put("message", content);
            msgData.put("timestamp", System.currentTimeMillis());
            // Optional fields for future use
            msgData.put("senderId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            msgData.put("text", content);

            newMsgRef.setValue(msgData);

            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", content);
            updates.put("lastTime", timeStr);
            updates.put("lastTimestamp", System.currentTimeMillis());
            updates.put("hasNewForOwner", true);
            updates.put("hasNewForUser", false);
            updates.put("hasNewMessage", true);
            conversationRef.updateChildren(updates);

            edtMessage.setText(null);
        });
    }
}
