package com.example.cookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutBot, layoutUser;
        TextView tvBotMessage, tvUserMessage;

        public ChatViewHolder(View view) {
            super(view);
            layoutBot = view.findViewById(R.id.layout_bot);
            layoutUser = view.findViewById(R.id.layout_user);
            tvBotMessage = view.findViewById(R.id.tv_bot_message);
            tvUserMessage = view.findViewById(R.id.tv_user_message);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (msg.getType() == ChatMessage.TYPE_USER) {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutBot.setVisibility(View.GONE);
            holder.tvUserMessage.setText(msg.getContent());
        } else {
            holder.layoutBot.setVisibility(View.VISIBLE);
            holder.layoutUser.setVisibility(View.GONE);
            holder.tvBotMessage.setText(msg.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
