package com.rudeusgrey.vibechat;

import android.content.Intent;
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

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_FRIEND = 0;
    private static final int VIEW_TYPE_SEARCH = 1;

    private List<FriendsListActivity.FriendUser> users;
    private Consumer<String> onFriendAction;
    private Map<String, Boolean> friendStatusMap;
    private Map<String, Boolean> pendingRequestMap;
    private boolean isSearchMode;

    public SearchResultAdapter(List<FriendsListActivity.FriendUser> users, Consumer<String> onFriendAction,
                               Map<String, Boolean> friendStatusMap, Map<String, Boolean> pendingRequestMap) {
        this.users = users;
        this.onFriendAction = onFriendAction;
        this.friendStatusMap = friendStatusMap;
        this.pendingRequestMap = pendingRequestMap;
        this.isSearchMode = false;
    }

    public void setSearchMode(boolean isSearchMode) {
        this.isSearchMode = isSearchMode;
        Log.d("SearchResultAdapter", "Set search mode: " + isSearchMode);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isSearchMode ? VIEW_TYPE_SEARCH : VIEW_TYPE_FRIEND;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FRIEND) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            Log.d("SearchResultAdapter", "Creating FriendViewHolder, Layout: item_friend");
            return new FriendViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
            Log.d("SearchResultAdapter", "Creating SearchViewHolder, Layout: item_search_result");
            return new SearchViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FriendsListActivity.FriendUser user = users.get(position);
        Log.d("SearchResultAdapter", "Binding user: " + user.username + ", isSearchMode: " + isSearchMode);

        if (holder instanceof FriendViewHolder) {
            FriendViewHolder friendHolder = (FriendViewHolder) holder;
            friendHolder.friendNameTextView.setText(user.username);
            String lastMessage = user.isLastMessageFromMe ? "Bạn: " + user.lastMessage : user.lastMessage;
            friendHolder.lastMessageTextView.setText(lastMessage != null ? lastMessage : "");
            friendHolder.timestampTextView.setText(user.timestamp != null ? user.timestamp : "");
            Log.d("SearchResultAdapter", "Friend mode - Last message: " + lastMessage + ", Timestamp: " + user.timestamp);
            friendHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("friendId", user.uid);
                v.getContext().startActivity(intent);
            });
        } else if (holder instanceof SearchViewHolder) {
            SearchViewHolder searchHolder = (SearchViewHolder) holder;
            searchHolder.userNameTextView.setText(user.username);
            searchHolder.userStatusTextView.setText(user.status != null ? user.status : "Đang hoạt động");
            String userId = user.uid;
            boolean isFriend = friendStatusMap.containsKey(userId);
            boolean isPending = pendingRequestMap.containsKey(userId);
            if (isFriend) {
                searchHolder.addFriendButton.setText("Hủy kết bạn");
            } else if (isPending) {
                searchHolder.addFriendButton.setText("Hủy gửi");
            } else {
                searchHolder.addFriendButton.setText("Kết bạn");
            }
            searchHolder.addFriendButton.setOnClickListener(v -> {
                Log.d("SearchResultAdapter", "Friend action for: " + userId + ", Action: " + searchHolder.addFriendButton.getText());
                onFriendAction.accept(userId);
                // Update pendingRequestMap and notify adapter if request is sent
                if (searchHolder.addFriendButton.getText().equals("Kết bạn")) {
                    pendingRequestMap.put(userId, true);
                } else if (searchHolder.addFriendButton.getText().equals("Hủy gửi")) {
                    pendingRequestMap.remove(userId);
                }
                notifyDataSetChanged();
            });
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView friendNameTextView, lastMessageTextView, timestampTextView;

        public FriendViewHolder(View itemView) {
            super(itemView);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, userStatusTextView;
        MaterialButton addFriendButton;

        public SearchViewHolder(View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            userStatusTextView = itemView.findViewById(R.id.userStatusTextView);
            addFriendButton = itemView.findViewById(R.id.addFriendButton);
        }
    }
}