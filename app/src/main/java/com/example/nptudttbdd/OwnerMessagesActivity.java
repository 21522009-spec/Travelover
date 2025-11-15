package com.example.nptudttbdd;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.UUID;

public class OwnerMessagesActivity extends AppCompatActivity {

    private TravelDataRepository repository;
    private ChatMessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_messages);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);
        adapter = new ChatMessageAdapter();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

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
            repository.appendOwnerMessage(message);
            adapter.addMessage(message);
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            edtMessage.setText(null);
        });
    }

    private void renderMessages() {
        List<ChatMessage> messages = repository.getOwnerConversation();
        adapter.submitList(messages);
    }
}