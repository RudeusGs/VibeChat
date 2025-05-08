package com.rudeusgrey.vibechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsListActivity extends AppCompatActivity {
    private RecyclerView friendsRecyclerView;
    private SearchView searchView;
    private TextView emptyStateTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef, friendsRef, friendRequestsRef, chatsRef;
    private List<FriendUser> userList = new ArrayList<>();
    private Map<String, Boolean> friendStatusMap = new HashMap<>();
    private Map<String, Boolean> sentRequestMap = new HashMap<>();
    private SearchResultAdapter adapter;
    private ValueEventListener friendStatusListener, sentRequestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        friendsRef = FirebaseDatabase.getInstance().getReference("friends").child(mAuth.getCurrentUser().getUid());
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friend_requests");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        searchView = findViewById(R.id.searchView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        ImageButton searchButton = findViewById(R.id.searchButton);
        FloatingActionButton addFriendFab = findViewById(R.id.addFriendFab);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchResultAdapter(userList, this::handleFriendAction, friendStatusMap, sentRequestMap);
        friendsRecyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d("SearchView", "Query submitted: " + query);
                searchUsers(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("SearchView", "Query changed: " + newText);
                searchUsers(newText.trim());
                return true;
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = searchView.getQuery().toString().trim();
            Log.d("SearchButton", "Search clicked with query: " + query);
            searchUsers(query);
        });

        addFriendFab.setOnClickListener(v -> {
            Log.d("AddFriendFab", "FAB clicked, loading all users");
            searchUsers("");
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                startActivity(new Intent(FriendsListActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_list) {
                return true;
            } else if (itemId == R.id.nav_info) {
                startActivity(new Intent(FriendsListActivity.this, InfoActivity.class));
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_list);
        loadFriendStatus();
        loadSentRequests();
        loadFriendsList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh sent requests to ensure pendingRequestMap is up-to-date
        loadSentRequests();
        if (searchView.getQuery().toString().trim().isEmpty()) {
            loadFriendsList();
        } else {
            searchUsers(searchView.getQuery().toString().trim());
        }
    }

    private void loadFriendStatus() {
        friendStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                friendStatusMap.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    friendStatusMap.put(child.getKey(), true);
                }
                Log.d("FriendStatus", "Loaded " + friendStatusMap.size() + " friends");
                adapter.notifyDataSetChanged();
                loadFriendsList();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FriendsListActivity.this, "Lỗi tải danh sách bạn bè: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        friendsRef.addValueEventListener(friendStatusListener);
    }

    private void loadSentRequests() {
        sentRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                sentRequestMap.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    sentRequestMap.put(child.getKey(), true);
                }
                Log.d("SentRequests", "Loaded " + sentRequestMap.size() + " sent requests");
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FriendsListActivity.this, "Lỗi tải lời mời đã gửi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        friendRequestsRef.child(mAuth.getCurrentUser().getUid()).addValueEventListener(sentRequestsListener);
    }

    private void loadFriendsList() {
        if (!searchView.getQuery().toString().trim().isEmpty()) {
            Log.d("FriendsList", "Skipping loadFriendsList due to active search query");
            return;
        }
        userList.clear();
        adapter.setSearchMode(false);
        Log.d("FriendsList", "Loading friends list, search mode: false");
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userList.clear(); // Ensure list is cleared
                for (DataSnapshot child : snapshot.getChildren()) {
                    String friendId = child.getKey();
                    Log.d("FriendsList", "Processing friend: " + friendId);
                    usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            RegisterActivity.User baseUser = userSnapshot.getValue(RegisterActivity.User.class);
                            if (baseUser != null) {
                                baseUser.uid = friendId;
                                FriendUser user = new FriendUser(baseUser);
                                String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
                                Log.d("FriendsList", "Chat ID: " + chatId);
                                chatsRef.child(chatId).orderByChild("timestamp").limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot chatSnapshot) {
                                        String lastMessage = "";
                                        String timestamp = "";
                                        boolean isLastMessageFromMe = false;
                                        if (chatSnapshot.hasChildren()) {
                                            DataSnapshot lastMsg = chatSnapshot.getChildren().iterator().next();
                                            lastMessage = lastMsg.child("text").getValue(String.class);
                                            timestamp = lastMsg.child("timestamp").getValue(String.class);
                                            String senderId = lastMsg.child("senderId").getValue(String.class);
                                            isLastMessageFromMe = senderId != null && senderId.equals(mAuth.getCurrentUser().getUid());
                                            Log.d("FriendsList", "Last message: " + lastMessage + ", Timestamp: " + timestamp);
                                        }
                                        user.lastMessage = lastMessage != null ? lastMessage : "";
                                        user.timestamp = timestamp != null ? timestamp : "";
                                        user.isLastMessageFromMe = isLastMessageFromMe;

                                        boolean exists = false;
                                        for (FriendUser existingUser : userList) {
                                            if (existingUser.uid.equals(user.uid)) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            userList.add(user);
                                            Log.d("FriendsList", "Added user: " + user.username + ", List size: " + userList.size());
                                            adapter.notifyItemInserted(userList.size() - 1);
                                        }
                                        updateUIState(userList.isEmpty(), false);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(FriendsListActivity.this, "Lỗi tải tin nhắn cuối: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(FriendsListActivity.this, "Lỗi tải thông tin bạn bè: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                updateUIState(userList.isEmpty(), false);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FriendsListActivity.this, "Lỗi tải danh sách bạn bè: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private void searchUsers(String query) {
        Log.d("SearchUsers", "Searching with query: " + query);
        if (query.isEmpty() && searchView.getQuery().length() == 0) {
            loadFriendsList();
            return;
        }

        // Fetch sent requests before searching to ensure pendingRequestMap is up-to-date
        friendRequestsRef.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                sentRequestMap.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    sentRequestMap.put(child.getKey(), true);
                }
                Log.d("SearchUsers", "Pre-search: Loaded " + sentRequestMap.size() + " sent requests");

                updateUIState(false, true);
                adapter.setSearchMode(true);
                friendsRecyclerView.setAdapter(adapter);

                usersRef.orderByChild("username").startAt(query).endAt(query + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        userList.clear();
                        Log.d("SearchUsers", "Found " + snapshot.getChildrenCount() + " users");
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String userId = child.getKey();
                            Log.d("SearchUsers", "User ID: " + userId);
                            if (!userId.equals(mAuth.getCurrentUser().getUid())) {
                                RegisterActivity.User baseUser = child.getValue(RegisterActivity.User.class);
                                if (baseUser != null) {
                                    baseUser.uid = userId;
                                    FriendUser user = new FriendUser(baseUser);
                                    user.lastMessage = "";
                                    user.timestamp = "";
                                    user.isLastMessageFromMe = false;
                                    userList.add(user);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        updateUIState(userList.isEmpty(), false);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(FriendsListActivity.this, "Lỗi tìm kiếm: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        updateUIState(true, false);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FriendsListActivity.this, "Lỗi tải lời mời đã gửi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleFriendAction(String userId) {
        Log.d("FriendAction", "Handling action for user: " + userId);
        if (friendStatusMap.containsKey(userId)) {
            unfriendUser(userId);
        } else if (sentRequestMap.containsKey(userId)) {
            cancelFriendRequest(userId);
        } else {
            sendFriendRequest(userId);
        }
    }

    private void sendFriendRequest(String recipientUid) {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        DatabaseReference recipientRequestsRef = friendRequestsRef.child(recipientUid);
        recipientRequestsRef.child(currentUserUid).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    sentRequestMap.put(recipientUid, true);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void cancelFriendRequest(String recipientUid) {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        friendRequestsRef.child(recipientUid).child(currentUserUid).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã hủy lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    sentRequestMap.remove(recipientUid);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi hủy yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void unfriendUser(String friendId) {
        friendsRef.child(friendId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    FirebaseDatabase.getInstance().getReference("friends").child(friendId).child(mAuth.getCurrentUser().getUid()).removeValue();
                    friendStatusMap.remove(friendId);
                    userList.removeIf(user -> user.uid.equals(friendId));
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                    updateUIState(userList.isEmpty(), false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi hủy kết bạn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateUIState(boolean showEmptyState, boolean showProgress) {
        emptyStateTextView.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
        friendsRecyclerView.setVisibility(showEmptyState ? View.GONE : View.VISIBLE);
        emptyStateTextView.setText(showEmptyState && !userList.isEmpty() ? "Không tìm thấy người dùng" : "Bạn chưa có bạn bè nào. Hãy tìm kiếm và kết bạn để bắt đầu trò chuyện!");
        Log.d("UIState", "Empty state: " + showEmptyState + ", List size: " + userList.size());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendStatusListener != null) {
            friendsRef.removeEventListener(friendStatusListener);
        }
        if (sentRequestsListener != null) {
            friendRequestsRef.child(mAuth.getCurrentUser().getUid()).removeEventListener(sentRequestsListener);
        }
    }

    public static class FriendUser extends RegisterActivity.User {
        public String lastMessage;
        public String timestamp;
        public boolean isLastMessageFromMe;

        public FriendUser(RegisterActivity.User baseUser) {
            this.uid = baseUser.uid;
            this.username = baseUser.username;
            this.email = baseUser.email;
            this.photoUrl = baseUser.photoUrl;
            this.status = baseUser.status;
            this.createdAt = baseUser.createdAt;
            this.lastMessage = "";
            this.timestamp = "";
            this.isLastMessageFromMe = false;
        }
    }
}