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
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OwnerMessagesActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_CONVERSATION_TITLE = "extra_conversation_title";

    private ChatMessageAdapter adapter;
    private String conversationId;
    private DatabaseReference conversationRef;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_messages);

        adapter = new ChatMessageAdapter();

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        if (conversationId == null || conversationId.isEmpty()) {
            // No conversation ID provided, close activity
            finish();
            return;
        }

        // Firebase references for conversation and messages
        conversationRef = FirebaseDatabase.getInstance().getReference("Conversations").child(conversationId);
        messagesRef = conversationRef.child("messages");

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnOwnerHome = findViewById(R.id.btnOwnerHome);
        btnBack.setOnClickListener(v -> finish());
        btnOwnerHome.setOnClickListener(v ->
                startActivity(new Intent(OwnerMessagesActivity.this, OwnerDashboardActivity.class))
        );

        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        String conversationTitle = getIntent().getStringExtra(EXTRA_CONVERSATION_TITLE);
        if (tvChatTitle != null) {
            tvChatTitle.setText(conversationTitle != null ? conversationTitle : getString(R.string.owner_portal_messages));
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        EditText edtMessage = findViewById(R.id.edtMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);

        // Mark conversation as read for OWNER when opening
        conversationRef.child("hasNewForOwner").setValue(false);
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
                            senderStr = "OWNER";
                        } else {
                            senderStr = "USER";
                        }
                    }
                    if (content == null || senderStr == null) {
                        continue;
                    }
                    ChatMessage.Sender sender;
                    try {
                        sender = ChatMessage.Sender.valueOf(senderStr.toUpperCase(Locale.getDefault()));
                    } catch (IllegalArgumentException e) {
                        // Unknown sender type, skip
                        continue;
                    }
                    long timestamp = time != null ? time : 0L;
                    String id = msgId != null ? msgId : UUID.randomUUID().toString();
                    ChatMessage chatMessage = new ChatMessage(id, sender, content, timestamp);
                    messageList.add(chatMessage);
                }
                adapter.submitList(messageList);
                // Scroll to the latest message
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to load messages
            }
        });

        btnSend.setOnClickListener(v -> {
            String content = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                edtMessage.setError(getString(R.string.message_error_empty));
                return;
            }
            // Ensure user is logged in
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(OwnerMessagesActivity.this, "Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Create a new message entry in Firebase
            DatabaseReference newMsgRef = messagesRef.push();
            String newMsgId = newMsgRef.getKey();
            if (newMsgId == null) {
                Toast.makeText(OwnerMessagesActivity.this, "Không thể gửi tin nhắn, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Prepare message data
            Map<String, Object> msgData = new HashMap<>();
            msgData.put("id", newMsgId);
            msgData.put("sender", "OWNER");
            msgData.put("message", content);
            msgData.put("timestamp", System.currentTimeMillis());
            // Optional fields for future use
            msgData.put("senderId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            msgData.put("text", content);
            // Send message
            newMsgRef.setValue(msgData);
            // Update conversation metadata
            // Format current time as "HH:mm"
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", content);
            updates.put("lastTime", timeStr);
            updates.put("lastTimestamp", System.currentTimeMillis());
            updates.put("hasNewForOwner", false);
            updates.put("hasNewForUser", true);
            updates.put("hasNewMessage", true);
            conversationRef.updateChildren(updates);
            // Clear input field
            edtMessage.setText(null);
        });
    }
}
