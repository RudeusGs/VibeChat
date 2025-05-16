package com.rudeusgrey.vibechat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.ViewHolder> {
    private List<FriendSelectItem> friendList;
    private List<String> selectedFriendIds = new ArrayList<>();

    public FriendSelectAdapter(List<FriendSelectItem> friendList) {
        this.friendList = friendList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendSelectItem friend = friendList.get(position);
        holder.friendNameTextView.setText(friend.username);
        holder.friendCheckBox.setChecked(selectedFriendIds.contains(friend.id));

        holder.friendCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedFriendIds.add(friend.id);
            } else {
                selectedFriendIds.remove(friend.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public List<String> getSelectedFriendIds() {
        return selectedFriendIds;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox friendCheckBox;
        TextView friendNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendCheckBox = itemView.findViewById(R.id.friendCheckBox);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView);
        }
    }

    public static class FriendSelectItem {
        public String id;
        public String username;

        public FriendSelectItem(String id, String username) {
            this.id = id;
            this.username = username;
        }
    }
}