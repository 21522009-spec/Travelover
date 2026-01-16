package com.example.nptudttbdd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserConversationsAdapter
        extends RecyclerView.Adapter<UserConversationsAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(@NonNull UserConversation conversation);
    }

    private final List<UserConversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;

    public UserConversationsAdapter(@NonNull OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<UserConversation> data) {
        conversations.clear();
        conversations.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(conversations.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvConversationTitle;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final View viewUnreadDot;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversationTitle = itemView.findViewById(R.id.tvConversationTitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }

        void bind(@NonNull UserConversation conversation,
                  @NonNull OnConversationClickListener listener) {
            tvConversationTitle.setText(conversation.getTitle());
            tvLastMessage.setText(conversation.getLastMessage());
            tvTime.setText(conversation.getLastTime());
            viewUnreadDot.setVisibility(conversation.hasNewMessage() ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }
}
