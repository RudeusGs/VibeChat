package com.rudeusgrey.vibechat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private List<MainActivity.Group> groupList;
    private OnGroupClickListener onGroupClickListener;

    public interface OnGroupClickListener {
        void onGroupClick(String groupId);
    }

    public GroupAdapter(List<MainActivity.Group> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.onGroupClickListener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_item, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        MainActivity.Group group = groupList.get(position);
        holder.groupNameTextView.setText(group.name);
        holder.lastMessageTextView.setText(group.lastMessage.isEmpty() ? "Chưa có tin nhắn" : group.lastMessage);
        holder.timestampTextView.setText(group.timestamp);

        // Xử lý click
        holder.itemView.setOnClickListener(v -> {
            if (onGroupClickListener != null) {
                onGroupClickListener.onGroupClick(group.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView, lastMessageTextView, timestampTextView;
        CircleImageView groupAvatarImageView;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            groupAvatarImageView = itemView.findViewById(R.id.groupAvatarImageView);
        }
    }
}