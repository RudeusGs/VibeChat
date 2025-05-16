package com.rudeusgrey.vibechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView friendsRecyclerView, friendRequestsRecyclerView, groupsRecyclerView;
    private SearchView searchView;
    private TextView friendsHeaderTextView, requestsHeaderTextView, groupsHeaderTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference friendsRef, friendRequestsRef, usersRef, chatsRef, messagesRef, groupsRef, groupRef;
    private List<Friend> friendList = new ArrayList<>();
    private List<Friend> filteredFriendList = new ArrayList<>();
    private List<FriendRequest> friendRequestList = new ArrayList<>();
    private List<Group> groupList = new ArrayList<>();
    private FriendAdapter friendAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    private GroupAdapter groupAdapter;
    private Map<String, ValueEventListener> chatListeners = new HashMap<>();
    private static final int REQUEST_NOTIFICATION_PERMISSION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing Firebase: " + e.getMessage());
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth == null) {
            Log.e("MainActivity", "FirebaseAuth is null, cannot proceed");
            Toast.makeText(this, "Lỗi khởi tạo Firebase, vui lòng thử lại", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d("UserId", "Current User ID: " + currentUserId);

        friendsRef = FirebaseDatabase.getInstance().getReference("friends").child(currentUserId);
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friend_requests").child(currentUserId);
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        messagesRef = FirebaseDatabase.getInstance().getReference("chats");
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        groupRef = FirebaseDatabase.getInstance().getReference("groups");

        searchView = findViewById(R.id.searchView);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView);
        groupsRecyclerView = findViewById(R.id.groupsRecyclerView);
        friendsHeaderTextView = findViewById(R.id.friendsHeaderTextView);
        requestsHeaderTextView = findViewById(R.id.requestsHeaderTextView);
        groupsHeaderTextView = findViewById(R.id.groupsHeaderTextView);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        friendAdapter = new FriendAdapter(filteredFriendList);
        friendRequestAdapter = new FriendRequestAdapter(friendRequestList, this::acceptFriendRequest, this::rejectFriendRequest);
        groupAdapter = new GroupAdapter(groupList, this::openGroupChat);

        friendsRecyclerView.setAdapter(friendAdapter);
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);
        groupsRecyclerView.setAdapter(groupAdapter);

        loadFriends();
        loadFriendRequests();
        loadGroups();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterFriends(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFriends(newText.trim());
                return true;
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                return true;
            } else if (itemId == R.id.nav_list) {
                startActivity(new Intent(MainActivity.this, FriendsListActivity.class));
                return true;
            } else if (itemId == R.id.nav_info) {
                startActivity(new Intent(MainActivity.this, InfoActivity.class));
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_chat);
    }

    private void loadFriends() {
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                friendList.clear();
                filteredFriendList.clear();
                chatListeners.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String friendId = child.getKey();
                    usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            RegisterActivity.User user = userSnapshot.getValue(RegisterActivity.User.class);
                            if (user != null) {
                                String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
                                Friend friend = new Friend(friendId, user.username, "", "", false);
                                int index = friendList.indexOf(friend);
                                if (index == -1) {
                                    friendList.add(friend);
                                    filteredFriendList.add(friend);
                                } else {
                                    friendList.set(index, friend);
                                    filteredFriendList.set(index, friend);
                                }

                                ValueEventListener chatListener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot chatSnapshot) {
                                        String lastMessage = "";
                                        String timestamp = "";
                                        boolean isLastMessageFromMe = false;
                                        if (chatSnapshot.hasChildren()) {
                                            DataSnapshot lastMsg = chatSnapshot.getChildren().iterator().next();
                                            lastMessage = lastMsg.child("content").getValue(String.class);
                                            Long timestampValue = lastMsg.child("timestamp").getValue(Long.class);
                                            String senderId = lastMsg.child("senderId").getValue(String.class);
                                            isLastMessageFromMe = senderId != null && senderId.equals(mAuth.getCurrentUser().getUid());
                                            if (timestampValue != null) {
                                                timestamp = formatTimestamp(timestampValue);
                                            }
                                        }
                                        Friend updatedFriend = new Friend(friendId, user.username, lastMessage, timestamp, isLastMessageFromMe);
                                        int updateIndex = friendList.indexOf(new Friend(friendId, user.username));
                                        if (updateIndex >= 0) {
                                            friendList.set(updateIndex, updatedFriend);
                                            filteredFriendList.set(updateIndex, updatedFriend);
                                            friendAdapter.notifyItemChanged(updateIndex);
                                        }
                                        updateVisibility();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Log.e("MainActivity", "Error loading chat data: " + error.getMessage());
                                    }
                                };
                                chatsRef.child(chatId).child("messages").orderByChild("timestamp").limitToLast(1).addValueEventListener(chatListener);
                                chatListeners.put(chatId, chatListener);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e("MainActivity", "Error loading user data: " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MainActivity", "Error loading friends: " + error.getMessage());
            }
        });
    }

    private void loadGroups() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d("UserId", "Loading groups for user: " + currentUserId);
        groupsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                groupList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String groupId = child.getKey();
                    DataSnapshot groupSnapshot = child;
                    String groupName = groupSnapshot.child("groupName").getValue(String.class);
                    DataSnapshot membersSnapshot = groupSnapshot.child("members");

                    // Kiểm tra xem currentUserId có trong danh sách giá trị của members không
                    boolean isMember = false;
                    for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                        String memberId = memberSnapshot.getValue(String.class);
                        if (currentUserId.equals(memberId)) {
                            isMember = true;
                            break;
                        }
                    }

                    Log.d("GroupData", "Group: " + groupName + ", ID: " + groupId + ", User in members: " + isMember + ", Members: " + membersSnapshot.toString());
                    if (groupName != null && isMember) {
                        groupRef.child(groupId).child("messages").orderByChild("timestamp").limitToLast(1)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot messagesSnapshot) {
                                        String lastMessage = "";
                                        String timestamp = "";
                                        boolean isLastMessageFromMe = false;
                                        if (messagesSnapshot.hasChildren()) {
                                            DataSnapshot lastMsg = messagesSnapshot.getChildren().iterator().next();
                                            lastMessage = lastMsg.child("content").getValue(String.class);
                                            Long timestampValue = lastMsg.child("timestamp").getValue(Long.class);
                                            String senderId = lastMsg.child("senderId").getValue(String.class);
                                            isLastMessageFromMe = senderId != null && senderId.equals(currentUserId);
                                            if (timestampValue != null) {
                                                timestamp = formatTimestamp(timestampValue);
                                            }
                                            Log.d("GroupMessage", "Group: " + groupName + ", LastMessage: " + lastMessage + ", Timestamp: " + timestamp);
                                        } else {
                                            Log.d("GroupMessage", "No messages found for group: " + groupName);
                                        }
                                        MainActivity.Group group = new MainActivity.Group(groupId, groupName, lastMessage, timestamp, isLastMessageFromMe);
                                        groupList.add(group);
                                        Log.d("GroupList", "Added group: " + groupName + ", List size: " + groupList.size());
                                        groupAdapter.notifyDataSetChanged();
                                        updateVisibility();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Log.e("MainActivity", "Error loading group messages: " + error.getMessage());
                                    }
                                });
                    }
                }
                Log.d("GroupList", "Total groups: " + groupList.size());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MainActivity", "Error loading groups: " + error.getMessage());
            }
        });
    }

    private void openGroupChat(String groupId) {
        Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
        intent.putExtra("groupId", groupId);
        startActivity(intent);
    }

    private void loadFriendRequests() {
        friendRequestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                friendRequestList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String requesterId = child.getKey();
                    usersRef.child(requesterId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            RegisterActivity.User user = userSnapshot.getValue(RegisterActivity.User.class);
                            if (user != null) {
                                friendRequestList.add(new FriendRequest(requesterId, user.username));
                                friendRequestAdapter.notifyDataSetChanged();
                                updateVisibility();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void acceptFriendRequest(String requesterId) {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        friendsRef.child(requesterId).setValue(true);
        FirebaseDatabase.getInstance().getReference("friends").child(requesterId).child(currentUserUid).setValue(true);
        friendRequestsRef.child(requesterId).removeValue();
    }

    private void rejectFriendRequest(String requesterId) {
        friendRequestsRef.child(requesterId).removeValue();
    }

    private void filterFriends(String query) {
        filteredFriendList.clear();
        if (query.isEmpty()) {
            filteredFriendList.addAll(friendList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Friend friend : friendList) {
                if (friend.username.toLowerCase().contains(lowerCaseQuery)) {
                    filteredFriendList.add(friend);
                }
            }
        }
        friendAdapter.notifyDataSetChanged();
        updateVisibility();
    }

    private void updateVisibility() {
        boolean hasFilteredFriends = !filteredFriendList.isEmpty();
        boolean hasFriends = !friendList.isEmpty();
        boolean hasGroups = !groupList.isEmpty();
        boolean hasRequests = !friendRequestList.isEmpty();

        friendsRecyclerView.setVisibility(hasFilteredFriends ? View.VISIBLE : View.GONE);
        friendsHeaderTextView.setVisibility(hasFriends ? View.VISIBLE : View.GONE);
        groupsRecyclerView.setVisibility(hasGroups ? View.VISIBLE : View.GONE);
        groupsHeaderTextView.setVisibility(hasGroups ? View.VISIBLE : View.GONE);
        friendRequestsRecyclerView.setVisibility(hasRequests ? View.VISIBLE : View.GONE);
        requestsHeaderTextView.setVisibility(hasRequests ? View.VISIBLE : View.GONE);

        if (!hasFriends) {
            friendsHeaderTextView.setText("Bạn chưa có bạn bè");
        } else if (!hasFilteredFriends) {
            friendsHeaderTextView.setText("Không tìm thấy bạn bè");
        } else {
            friendsHeaderTextView.setText("Bạn bè");
        }

        if (!hasGroups) {
            groupsHeaderTextView.setText("Bạn chưa có nhóm");
        } else {
            groupsHeaderTextView.setText("Nhóm");
        }

        Log.d("UIState", "Groups size: " + groupList.size() + ", Visibility: " + groupsRecyclerView.getVisibility());
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class Friend {
        public String id, username, lastMessage, timestamp;
        public boolean isLastMessageFromMe;
        public boolean isImage;

        public Friend(String id, String username, String lastMessage, String timestamp, boolean isLastMessageFromMe) {
            this.id = id;
            this.username = username;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.isLastMessageFromMe = isLastMessageFromMe;
        }

        public Friend(String id, String username) {
            this(id, username, "", "", false);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Friend friend = (Friend) obj;
            return id.equals(friend.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static class FriendRequest {
        public String requesterId, username;

        public FriendRequest(String requesterId, String username) {
            this.requesterId = requesterId;
            this.username = username;
        }
    }

    public static class Group {
        public String id, name, lastMessage, timestamp;
        public boolean isLastMessageFromMe;

        public Group(String id, String name, String lastMessage, String timestamp, boolean isLastMessageFromMe) {
            this.id = id;
            this.name = name;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.isLastMessageFromMe = isLastMessageFromMe;
        }

        public Group(String id, String name) {
            this(id, name, "", "", false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ValueEventListener listener : chatListeners.values()) {
            chatsRef.removeEventListener(listener);
        }
    }
}