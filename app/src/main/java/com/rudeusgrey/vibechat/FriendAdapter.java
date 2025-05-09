package com.rudeusgrey.vibechat;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private List<MainActivity.Friend> friends;

    public FriendAdapter(List<MainActivity.Friend> friends) {
        this.friends = friends;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MainActivity.Friend friend = friends.get(position);
        holder.friendNameTextView.setText(friend.username);
        String lastMessage = friend.isLastMessageFromMe ? "Bạn: " + friend.lastMessage : friend.lastMessage;
        Log.d("FriendAdapter", "Friend: " + friend.username + ", LastMessage: " + lastMessage + ", Timestamp: " + friend.timestamp);

        if (lastMessage != null && !lastMessage.isEmpty()) {
            holder.lastMessageTextView.setText(lastMessage);
            holder.lastMessageTextView.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessageTextView.setText("No message");
            holder.lastMessageTextView.setVisibility(View.VISIBLE);
        }
        if (friend.timestamp != null && !friend.timestamp.isEmpty()) {
            holder.timestampTextView.setText(friend.timestamp);
            holder.timestampTextView.setVisibility(View.VISIBLE);
        } else {
            holder.timestampTextView.setText("No time");
            holder.timestampTextView.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("friendId", friend.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView friendNameTextView, lastMessageTextView, timestampTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}