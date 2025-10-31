package com.example.nptudttbdd;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.UUID;

public class AdminMessagesActivity extends AppCompatActivity {

    private TravelDataRepository repository;
    private LinearLayout messageContainer;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_messages);

        repository = TravelDataRepository.getInstance(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        scrollView = findViewById(R.id.messageScrollView);
        messageContainer = findViewById(R.id.messageContainer);
        EditText etMessage = findViewById(R.id.etMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSend);

        renderMessages();

        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, R.string.message_error_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            ChatMessage newMessage = new ChatMessage(UUID.randomUUID().toString(),
                    ChatMessage.Sender.ADMIN,
                    content);
            repository.appendAdminMessage(newMessage);
            addMessageView(newMessage);
            etMessage.setText(null);
            scrollToBottom();
        });
    }

    private void renderMessages() {
        messageContainer.removeAllViews();
        List<ChatMessage> messages = repository.getAdminConversation();
        for (ChatMessage message : messages) {
            addMessageView(message);
        }
        scrollToBottom();
    }

    private void addMessageView(ChatMessage message) {
        int layoutId = message.getSender() == ChatMessage.Sender.ADMIN
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View view = LayoutInflater.from(this).inflate(layoutId, messageContainer, false);
        TextView tvMessage;
        if (message.getSender() == ChatMessage.Sender.ADMIN) {
            tvMessage = view.findViewById(R.id.tvMessageTextSent);
        } else {
            tvMessage = view.findViewById(R.id.tvMessageTextReceived);
        }
        tvMessage.setText(message.getMessage());
        messageContainer.addView(view);
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}