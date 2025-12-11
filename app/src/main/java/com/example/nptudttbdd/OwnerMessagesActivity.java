package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.UUID;

public class OwnerMessagesActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_CONVERSATION_TITLE = "extra_conversation_title";

    private TravelDataRepository repository;
    private ChatMessageAdapter adapter;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_messages);

        repository = TravelDataRepository.getInstance(this);
        adapter = new ChatMessageAdapter();

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = repository.getDefaultOwnerConversationId();
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnOwnerHome = findViewById(R.id.btnOwnerHome);
        btnBack.setOnClickListener(v -> finish());
        btnOwnerHome.setOnClickListener(v ->
                startActivity(new Intent(OwnerMessagesActivity.this, OwnerPortalActivity.class))
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

        renderMessages();
        recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));

        btnSend.setOnClickListener(v -> {
            String content = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                edtMessage.setError(getString(R.string.message_error_empty));
                return;
            }
            ChatMessage message = new ChatMessage(UUID.randomUUID().toString(),
                    ChatMessage.Sender.OWNER,
                    content);
            repository.appendOwnerMessage(conversationId, message);
            adapter.addMessage(message);
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            edtMessage.setText(null);
        });
    }

    private void renderMessages() {
        List<ChatMessage> messages = repository.getOwnerConversation(conversationId);
        adapter.submitList(messages);
    }
}