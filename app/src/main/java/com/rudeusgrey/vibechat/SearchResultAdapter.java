package com.rudeusgrey.vibechat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<FriendsListActivity.FriendUser> users;
    private Consumer<String> friendActionCallback;
    private Map<String, Boolean> friendStatusMap;
    private Map<String, Boolean> sentRequestMap;
    private boolean isSearchMode = false;

    public SearchResultAdapter(List<FriendsListActivity.FriendUser> users, Consumer<String> friendActionCallback,
                               Map<String, Boolean> friendStatusMap, Map<String, Boolean> sentRequestMap) {
        this.users = users;
        this.friendActionCallback = friendActionCallback;
        this.friendStatusMap = friendStatusMap;
        this.sentRequestMap = sentRequestMap;
    }

    public void setSearchMode(boolean searchMode) {
        this.isSearchMode = searchMode;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FriendsListActivity.FriendUser user = users.get(position);
        holder.friendNameTextView.setText(user.username);
        Log.d("SearchResultAdapter", "Binding user: " + user.username);

        if (isSearchMode) {
            if (friendStatusMap.containsKey(user.uid)) {
                holder.actionButton.setText("Hủy kết bạn");
                holder.actionButton.setVisibility(View.VISIBLE);
            } else if (sentRequestMap.containsKey(user.uid)) {
                holder.actionButton.setText("Hủy yêu cầu");
                holder.actionButton.setVisibility(View.VISIBLE);
            } else {
                holder.actionButton.setText("Kết bạn");
                holder.actionButton.setVisibility(View.VISIBLE);
            }
        } else {
            holder.actionButton.setText("Hủy kết bạn");
            holder.actionButton.setVisibility(View.VISIBLE);
        }

        holder.actionButton.setOnClickListener(v -> friendActionCallback.accept(user.uid));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView friendNameTextView;
        MaterialButton actionButton;

        public ViewHolder(View itemView) {
            super(itemView);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView);
            actionButton = itemView.findViewById(R.id.unfriendButton);
        }
    }
}