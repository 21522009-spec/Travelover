package com.example.nptudttbdd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();

    public void submitList(@NonNull List<ChatMessage> data) {
        messages.clear();
        messages.addAll(data);
        notifyDataSetChanged();
    }

    public void addMessage(@NonNull ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return (message.getSender() == ChatMessage.Sender.OWNER
                || message.getSender() == ChatMessage.Sender.ADMIN)
                ? VIEW_TYPE_SENT
                : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tvMessageTextSent);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tvMessageTextReceived);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
        }
    }
}