package com.rudeusgrey.vibechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RecyclerView friendsRecyclerView, friendRequestsRecyclerView;
    private SearchView searchView;
    private TextView friendsHeaderTextView, requestsHeaderTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference friendsRef, friendRequestsRef, usersRef, chatsRef;
    private List<Friend> friendList = new ArrayList<>();
    private List<Friend> filteredFriendList = new ArrayList<>();
    private List<FriendRequest> friendRequestList = new ArrayList<>();
    private FriendAdapter friendAdapter;
    private FriendRequestAdapter friendRequestAdapter;

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

    private void loadFriends() {
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                friendList.clear();
                filteredFriendList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String friendId = child.getKey();
                    usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            RegisterActivity.User user = userSnapshot.getValue(RegisterActivity.User.class);
                            if (user != null) {
                                String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
                                chatsRef.child(chatId).child("messages").orderByChild("timestamp").limitToLast(1).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot chatSnapshot) {
                                        String lastMessage = "";
                                        String timestamp = "";
                                        boolean isLastMessageFromMe = false;
                                        Log.d("MainActivity", "Chat data for " + chatId + ": " + chatSnapshot.toString());
                                        if (chatSnapshot.hasChildren()) {
                                            DataSnapshot lastMsg = chatSnapshot.getChildren().iterator().next();
                                            lastMessage = lastMsg.child("content").getValue(String.class);
                                            Long timestampValue = lastMsg.child("timestamp").getValue(Long.class);
                                            String senderId = lastMsg.child("senderId").getValue(String.class);
                                            isLastMessageFromMe = senderId != null && senderId.equals(mAuth.getCurrentUser().getUid());
                                            if (timestampValue != null) {
                                                timestamp = formatTimestamp(timestampValue);
                                            }
                                        } else {
                                            Log.w("MainActivity", "No messages found for chatId: " + chatId);
                                        }
                                        Friend updatedFriend = new Friend(friendId, user.username, lastMessage, timestamp, isLastMessageFromMe);
                                        int index = -1;
                                        for (int i = 0; i < friendList.size(); i++) {
                                            if (friendList.get(i).id.equals(friendId)) {
                                                index = i;
                                                break;
                                            }
                                        }
                                        if (index >= 0) {
                                            friendList.set(index, updatedFriend);
                                            filteredFriendList.set(index, updatedFriend);
                                        } else {
                                            friendList.add(updatedFriend);
                                            filteredFriendList.add(updatedFriend);
                                        }
                                        friendAdapter.notifyDataSetChanged();
                                        updateFriendsVisibility();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Log.e("MainActivity", "Error loading chat data: " + error.getMessage());
                                    }
                                });
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
        friendsHeaderTextView.setVisibility(View.VISIBLE);

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
        DatabaseReference currentUserRequestsRef = FirebaseDatabase.getInstance().getReference("friend_requests").child(currentUserUid);
        currentUserRequestsRef.child(requesterId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    friendsRef.child(requesterId).setValue(true);
                    FirebaseDatabase.getInstance().getReference("friends").child(requesterId).child(currentUserUid).setValue(true);
                    currentUserRequestsRef.child(requesterId).removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void rejectFriendRequest(String requesterId) {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        DatabaseReference currentUserRequestsRef = FirebaseDatabase.getInstance().getReference("friend_requests").child(currentUserUid);
        currentUserRequestsRef.child(requesterId).removeValue();
    }

    public static class Friend {
        public String id, username, lastMessage, timestamp;
        public boolean isLastMessageFromMe;

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
    }

    public static class FriendRequest {
        public String requesterId, username;

        public FriendRequest(String requesterId, String username) {
            this.requesterId = requesterId;
            this.username = username;
        }
    }
}