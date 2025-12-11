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

import java.util.List;

public class OwnerConversationsActivity extends AppCompatActivity {

    private TravelDataRepository repository;
    private OwnerConversationsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_conversations);

        repository = TravelDataRepository.getInstance(this);
        adapter = new OwnerConversationsAdapter(this::openConversation);

        MaterialToolbar toolbar = findViewById(R.id.toolbarConversations);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupTabs();
        setupList();
        setupSearch();
    }

    private void setupList() {
        RecyclerView recyclerView = findViewById(R.id.recyclerOwnerConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        List<OwnerConversation> conversations = repository.getOwnerConversations();
        adapter.submitList(conversations);
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayoutConversations);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.owner_conversation_tab_conversations));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.owner_conversation_tab_contacts));
        tabLayout.selectTab(tabLayout.getTabAt(0));
    }

    private void setupSearch() {
        EditText edtSearch = findViewById(R.id.edtSearchConversation);
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            v.clearFocus();
            return false;
        });
    }

    private void openConversation(@NonNull OwnerConversation conversation) {
        Intent intent = new Intent(this, OwnerMessagesActivity.class);
        intent.putExtra(OwnerMessagesActivity.EXTRA_CONVERSATION_ID, conversation.getId());
        intent.putExtra(OwnerMessagesActivity.EXTRA_CONVERSATION_TITLE, conversation.getTitle());
        startActivity(intent);
    }
}