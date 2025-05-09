package com.rudeusgrey.vibechat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

public class ChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton backButton, menuButton;
    private TextView chatNameTextView, statusTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef, usersRef;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String friendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        friendId = getIntent().getStringExtra("friendId");

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        chatNameTextView = findViewById(R.id.chatNameTextView);
        statusTextView = findViewById(R.id.statusTextView);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList, mAuth.getCurrentUser().getUid());
        chatRecyclerView.setAdapter(messageAdapter);

        loadFriendInfo();
        loadMessages();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = messageEditText.getText().toString().trim();
                if (!content.isEmpty()) {
                    sendMessage(content);
                    messageEditText.setText("");
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatActivity.this, "Tính năng menu đang được phát triển", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFriendInfo() {
        usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                RegisterActivity.User user = snapshot.getValue(RegisterActivity.User.class);
                if (user != null) {
                    chatNameTextView.setText(user.username);
                    statusTextView.setText(user.status != null ? user.status : "Đang hoạt động");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void loadMessages() {
        String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
        chatsRef.child(chatId).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = child.getValue(Message.class);
                    if (message != null) {
                        message.messageId = child.getKey(); // Lưu messageId từ key của snapshot
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void sendMessage(String content) {
        String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
        Message message = new Message(mAuth.getCurrentUser().getUid(), content, System.currentTimeMillis());
        chatsRef.child(chatId).child("messages").push().setValue(message);
    }

    private String getChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public static class Message {
        public String senderId, content;
        public long timestamp;
        public Map<String, String> reactions;
        public String messageId; // Thêm trường messageId

        public Message() {
            this.reactions = new HashMap<>();
        }

        public Message(String senderId, String content, long timestamp) {
            this.senderId = senderId;
            this.content = content;
            this.timestamp = timestamp;
            this.reactions = new HashMap<>();
        }
    }
}