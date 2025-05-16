package com.rudeusgrey.vibechat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.function.Consumer;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private List<MainActivity.FriendRequest> friendRequests;
    private Consumer<String> onAcceptClick;
    private Consumer<String> onRejectClick;

    public FriendRequestAdapter(List<MainActivity.FriendRequest> friendRequests,
                                Consumer<String> onAcceptClick,
                                Consumer<String> onRejectClick) {
        this.friendRequests = friendRequests;
        this.onAcceptClick = onAcceptClick;
        this.onRejectClick = onRejectClick;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MainActivity.FriendRequest request = friendRequests.get(position);
        holder.requesterNameTextView.setText(request.username);
        Log.d("FriendRequestAdapter", "Binding request from: " + request.username);

        holder.acceptButton.setOnClickListener(v -> {
            Log.d("FriendRequestAdapter", "Accept clicked for: " + request.requesterId);
            onAcceptClick.accept(request.requesterId);
            removeItem(position);
        });

        holder.rejectButton.setOnClickListener(v -> {
            Log.d("FriendRequestAdapter", "Reject clicked for: " + request.requesterId);
            onRejectClick.accept(request.requesterId);
            removeItem(position);
        });
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    private void removeItem(int position) {
        if (position >= 0 && position < friendRequests.size()) {
            friendRequests.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, friendRequests.size());
        }
    }

    public void updateRequests(List<MainActivity.FriendRequest> newRequests) {
        this.friendRequests.clear();
        this.friendRequests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView requesterNameTextView;
        Button acceptButton, rejectButton;

        public ViewHolder(View itemView) {
            super(itemView);
            requesterNameTextView = itemView.findViewById(R.id.requesterNameTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}