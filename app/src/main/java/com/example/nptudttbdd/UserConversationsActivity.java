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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * USER side conversation list (chat with owners).
 */
public class UserConversationsActivity extends AppCompatActivity {

    private UserConversationsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_conversations);

        adapter = new UserConversationsAdapter(this::openConversation);

        MaterialToolbar toolbar = findViewById(R.id.toolbarConversations);
        toolbar.setNavigationOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.tabLayoutConversations);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.user_conversation_tab_conversations));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.user_conversation_tab_contacts));
        tabLayout.selectTab(tabLayout.getTabAt(0));

        RecyclerView recyclerView = findViewById(R.id.recyclerUserConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        EditText edtSearch = findViewById(R.id.edtSearchConversation);
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            v.clearFocus();
            return false;
        });

        loadConversationsFromFirebase();
    }

    private void loadConversationsFromFirebase() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        if (currentUid.isEmpty()) {
            return;
        }

        DatabaseReference conversationsRef = FirebaseDatabase.getInstance().getReference("Conversations");
        Query query = conversationsRef.orderByChild("userId").equalTo(currentUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserConversation> conversationList = new ArrayList<>();
                for (DataSnapshot convSnapshot : snapshot.getChildren()) {
                    String convId = convSnapshot.getKey();
                    String title = convSnapshot.child("title").getValue(String.class);
                    String lastMessage = convSnapshot.child("lastMessage").getValue(String.class);
                    String lastTime = convSnapshot.child("lastTime").getValue(String.class);

                    // New schema (recommended)
                    Boolean hasNewForUser = convSnapshot.child("hasNewForUser").getValue(Boolean.class);
                    // Backward compatibility
                    Boolean hasNewLegacy = convSnapshot.child("hasNewMessage").getValue(Boolean.class);

                    if (convId == null || title == null) {
                        continue;
                    }

                    if (lastMessage == null) lastMessage = "";
                    if (lastTime == null) lastTime = "";

                    boolean unread = (hasNewForUser != null && hasNewForUser)
                            || (hasNewLegacy != null && hasNewLegacy);

                    conversationList.add(new UserConversation(convId, title, lastMessage, lastTime, unread));
                }
                adapter.submitList(conversationList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ignore
            }
        });
    }

    private void openConversation(@NonNull UserConversation conversation) {
        Intent intent = new Intent(this, UserMessagesActivity.class);
        intent.putExtra(UserMessagesActivity.EXTRA_CONVERSATION_ID, conversation.getId());
        intent.putExtra(UserMessagesActivity.EXTRA_CONVERSATION_TITLE, conversation.getTitle());
        startActivity(intent);
    }
}
