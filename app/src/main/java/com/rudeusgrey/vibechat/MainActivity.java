package com.rudeusgrey.vibechat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView friendsRecyclerView, friendRequestsRecyclerView;
    private SearchView searchView;
    private TextView friendsHeaderTextView, requestsHeaderTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference friendsRef, friendRequestsRef, usersRef, chatsRef, messagesRef;
    private List<Friend> friendList = new ArrayList<>();
    private List<Friend> filteredFriendList = new ArrayList<>();
    private List<FriendRequest> friendRequestList = new ArrayList<>();
    private FriendAdapter friendAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    private Map<String, ValueEventListener> chatListeners = new HashMap<>();
    private ChildEventListener notificationListener;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        friendsRef = FirebaseDatabase.getInstance().getReference("friends").child(mAuth.getCurrentUser().getUid());
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friend_requests").child(mAuth.getCurrentUser().getUid());
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        messagesRef = FirebaseDatabase.getInstance().getReference("chats");

        searchView = findViewById(R.id.searchView);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView);
        friendsHeaderTextView = findViewById(R.id.friendsHeaderTextView);
        requestsHeaderTextView = findViewById(R.id.requestsHeaderTextView);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        friendAdapter = new FriendAdapter(filteredFriendList);
        friendRequestAdapter = new FriendRequestAdapter(friendRequestList, this::acceptFriendRequest, this::rejectFriendRequest);

        friendsRecyclerView.setAdapter(friendAdapter);
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);

        NotificationHelper.createNotificationChannel(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            } else {
                setupNotificationListener();
            }
        } else {
            setupNotificationListener();
        }

        loadFriends();
        loadFriendRequests();

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupNotificationListener();
            } else {
                Log.w("MainActivity", "Notification permission denied");
            }
        }
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
                                        updateFriendsVisibility();
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

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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
        updateFriendsVisibility();
    }

    private void updateFriendsVisibility() {
        boolean hasFilteredFriends = !filteredFriendList.isEmpty();
        boolean hasFriends = !friendList.isEmpty();

        friendsRecyclerView.setVisibility(hasFilteredFriends ? View.VISIBLE : View.GONE);
        friendsHeaderTextView.setVisibility(hasFriends ? View.VISIBLE : View.GONE);

        if (!hasFriends) {
            friendsHeaderTextView.setText("Bạn chưa có bạn bè");
        } else if (!hasFilteredFriends) {
            friendsHeaderTextView.setText("Không tìm thấy bạn bè");
        } else {
            friendsHeaderTextView.setText("Bạn bè");
        }

        requestsHeaderTextView.setVisibility(friendRequestList.isEmpty() ? View.GONE : View.VISIBLE);
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
                                updateFriendsVisibility();
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

    private void setupNotificationListener() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        notificationListener = messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                String chatId = dataSnapshot.getKey();
                Log.d("Notification", "New message received for chatId: " + chatId);
                String userId1 = chatId.split("_")[0];
                String userId2 = chatId.split("_")[1];
                String senderId = userId1.equals(currentUserId) ? userId2 : userId1;

                for (DataSnapshot messageSnapshot : dataSnapshot.child("messages").getChildren()) {
                    ChatActivity.Message message = messageSnapshot.getValue(ChatActivity.Message.class);
                    if (message != null && message.receiverId != null && message.senderId != null &&
                            message.receiverId.equals(currentUserId) && !message.senderId.equals(currentUserId)) {
                        usersRef.child(message.senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                RegisterActivity.User sender = snapshot.getValue(RegisterActivity.User.class);
                                if (sender != null) {
                                    NotificationHelper.showNotification(
                                            MainActivity.this,
                                            sender.username,
                                            message.content != null ? message.content : "Đã gửi hình ảnh",
                                            message.senderId
                                    );
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {}
                        });
                    } else {
                        Log.w("Notification", "Skipping notification due to null senderId/receiverId or message mismatch");
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                String chatId = dataSnapshot.getKey();
                Log.d("Notification", "Message changed for chatId: " + chatId);
                String userId1 = chatId.split("_")[0];
                String userId2 = chatId.split("_")[1];
                String senderId = userId1.equals(currentUserId) ? userId2 : userId1;

                DataSnapshot lastMessageSnapshot = null;
                for (DataSnapshot messageSnapshot : dataSnapshot.child("messages").getChildren()) {
                    lastMessageSnapshot = messageSnapshot;
                }
                if (lastMessageSnapshot != null) {
                    ChatActivity.Message message = lastMessageSnapshot.getValue(ChatActivity.Message.class);
                    if (message != null && message.receiverId != null && message.senderId != null &&
                            message.receiverId.equals(currentUserId) && !message.senderId.equals(currentUserId)) {
                        usersRef.child(message.senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                RegisterActivity.User sender = snapshot.getValue(RegisterActivity.User.class);
                                if (sender != null) {
                                    NotificationHelper.showNotification(
                                            MainActivity.this,
                                            sender.username,
                                            message.content != null ? message.content : "Đã gửi hình ảnh",
                                            message.senderId
                                    );
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {}
                        });
                    } else {
                        Log.w("Notification", "Skipping notification due to null senderId/receiverId or message mismatch in onChildChanged");
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ValueEventListener listener : chatListeners.values()) {
            chatsRef.removeEventListener(listener);
        }
        if (notificationListener != null) {
            messagesRef.removeEventListener(notificationListener);
        }
    }
}