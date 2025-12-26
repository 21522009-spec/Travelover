package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.List;

public class OwnerConversationsActivity extends AppCompatActivity {

    private OwnerConversationsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_conversations);

        adapter = new OwnerConversationsAdapter(this::openConversation);

        MaterialToolbar toolbar = findViewById(R.id.toolbarConversations);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup tab layout (Conversations/Contacts tabs)
        TabLayout tabLayout = findViewById(R.id.tabLayoutConversations);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.owner_conversation_tab_conversations));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.owner_conversation_tab_contacts));
        tabLayout.selectTab(tabLayout.getTabAt(0));

        // Setup RecyclerView for conversation list
        RecyclerView recyclerView = findViewById(R.id.recyclerOwnerConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup search input (no actual filtering implemented)
        EditText edtSearch = findViewById(R.id.edtSearchConversation);
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            v.clearFocus();
            return false;
        });

        // Load conversations from Firebase
        loadConversationsFromFirebase();
    }

    private void loadConversationsFromFirebase() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        if (currentUid.isEmpty()) {
            // No user logged in, nothing to load
            return;
        }
        DatabaseReference conversationsRef = FirebaseDatabase.getInstance().getReference("Conversations");
        Query query = conversationsRef.orderByChild("ownerId").equalTo(currentUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<OwnerConversation> conversationList = new ArrayList<>();
                for (DataSnapshot convSnapshot : snapshot.getChildren()) {
                    String convId = convSnapshot.getKey();
                    String title = convSnapshot.child("title").getValue(String.class);
                    String lastMessage = convSnapshot.child("lastMessage").getValue(String.class);
                    String lastTime = convSnapshot.child("lastTime").getValue(String.class);
                    Boolean hasNewMessage = convSnapshot.child("hasNewMessage").getValue(Boolean.class);
                    if (convId == null || title == null || lastMessage == null || lastTime == null) {
                        continue;
                    }
                    // If hasNewMessage is null, treat as false
                    boolean unread = hasNewMessage != null && hasNewMessage;
                    OwnerConversation conversation = new OwnerConversation(convId, title, lastMessage, lastTime, unread);
                    conversationList.add(conversation);
                }
                adapter.submitList(conversationList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read conversations
            }
        });
    }

    private void openConversation(@NonNull OwnerConversation conversation) {
        Intent intent = new Intent(this, OwnerMessagesActivity.class);
        intent.putExtra(OwnerMessagesActivity.EXTRA_CONVERSATION_ID, conversation.getId());
        intent.putExtra(OwnerMessagesActivity.EXTRA_CONVERSATION_TITLE, conversation.getTitle());
        startActivity(intent);
    }
}
