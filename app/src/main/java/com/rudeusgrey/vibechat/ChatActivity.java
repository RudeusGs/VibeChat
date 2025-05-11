package com.rudeusgrey.vibechat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.app.ProgressDialog;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton, attachButton;
    private ImageButton backButton, menuButton;
    private TextView chatNameTextView, statusTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef, usersRef, messagesRef;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String friendId;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 3;
    private ValueEventListener messagesListener;
    private ChildEventListener notificationListener;
    private final OkHttpClient client = new OkHttpClient();
    private static final String IMGUR_CLIENT_ID = "47065d1ba8ace09";
    private ProgressDialog progressDialog;
    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>();

    static {
        SUPPORTED_MIME_TYPES.add("image/jpeg");
        SUPPORTED_MIME_TYPES.add("image/png");
        SUPPORTED_MIME_TYPES.add("image/gif");
        SUPPORTED_MIME_TYPES.add("image/bmp");
        SUPPORTED_MIME_TYPES.add("image/tiff");
        SUPPORTED_MIME_TYPES.add("image/webp");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        messagesRef = FirebaseDatabase.getInstance().getReference("chats");
        friendId = getIntent().getStringExtra("friendId");
        if (friendId == null) {
            Toast.makeText(this, "Không tìm thấy ID người bạn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        attachButton = findViewById(R.id.attachButton);
        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        chatNameTextView = findViewById(R.id.chatNameTextView);
        statusTextView = findViewById(R.id.statusTextView);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList, mAuth.getCurrentUser().getUid());
        chatRecyclerView.setAdapter(messageAdapter);

        NotificationHelper.createNotificationChannel(this);
        NotificationHelper.setCurrentChatFriendId(friendId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            } else {
                setupNotificationListener();
            }
        } else {
            setupNotificationListener();
        }

        loadFriendInfo();
        loadMessages();

        sendButton.setOnClickListener(v -> {
            String content = messageEditText.getText().toString().trim();
            if (!content.isEmpty()) {
                sendMessage(content, null);
                messageEditText.setText("");
            }
        });

        attachButton.setOnClickListener(v -> requestStoragePermission());
        backButton.setOnClickListener(v -> finish());
        menuButton.setOnClickListener(v -> Toast.makeText(ChatActivity.this, "Tính năng menu đang được phát triển", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupNotificationListener();
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            uploadImageToImgur(imageUri);
        }
    }

    private void uploadImageToImgur(Uri imageUri) {
        if (imageUri == null) return;

        String mimeType = getFileMimeType(imageUri);
        if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            Toast.makeText(this, "Định dạng file không hỗ trợ", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                File file = uriToFile(imageUri);
                if (file == null || file.length() == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Lỗi hình ảnh", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                    return;
                }

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", file.getName(), RequestBody.create(file, MediaType.parse(mimeType)))
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.imgur.com/3/image")
                        .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String imageUrl = json.getJSONObject("data").getString("link");
                    runOnUiThread(() -> {
                        sendMessage(null, imageUrl);
                        progressDialog.dismiss();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Tải ảnh thất bại: " + response.code(), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                });
            }
        }).start();
    }

    private String getFileMimeType(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        return contentResolver.getType(uri) != null ? contentResolver.getType(uri) : "image/jpeg";
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String mimeType = getFileMimeType(uri);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) != null
                    ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) : "jpg";
            File file = new File(getCacheDir(), "temp_" + System.currentTimeMillis() + "." + extension);
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
        messagesListener = chatsRef.child(chatId).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = child.getValue(Message.class);
                    if (message != null) {
                        message.messageId = child.getKey();
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi tải tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String content, String imageUrl) {
        String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
        String messageId = chatsRef.child(chatId).child("messages").push().getKey();
        Message message = new Message(mAuth.getCurrentUser().getUid(), friendId, content, System.currentTimeMillis(), imageUrl);
        if (messageId != null) {
            chatsRef.child(chatId).child("messages").child(messageId).setValue(message);
        }
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
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null && message.receiverId != null && message.senderId != null &&
                            message.receiverId.equals(currentUserId) && !message.senderId.equals(currentUserId)) {
                        usersRef.child(message.senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                RegisterActivity.User sender = snapshot.getValue(RegisterActivity.User.class);
                                if (sender != null) {
                                    NotificationHelper.showNotification(
                                            ChatActivity.this,
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
                    Message message = lastMessageSnapshot.getValue(Message.class);
                    if (message != null && message.receiverId != null && message.senderId != null &&
                            message.receiverId.equals(currentUserId) && !message.senderId.equals(currentUserId)) {
                        usersRef.child(message.senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                RegisterActivity.User sender = snapshot.getValue(RegisterActivity.User.class);
                                if (sender != null) {
                                    NotificationHelper.showNotification(
                                            ChatActivity.this,
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

    private String getChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
            chatsRef.child(chatId).child("messages").removeEventListener(messagesListener);
        }
        if (notificationListener != null) {
            messagesRef.removeEventListener(notificationListener);
        }
        NotificationHelper.clearCurrentChatFriendId();
    }

    public static class Message {
        public String senderId, receiverId, content, imageUrl, messageId;
        public long timestamp;
        public Map<String, String> reactions;

        public Message() {
            this.reactions = new HashMap<>();
        }

        public Message(String senderId, String receiverId, String content, long timestamp, String imageUrl) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.content = content;
            this.timestamp = timestamp;
            this.imageUrl = imageUrl;
            this.reactions = new HashMap<>();
        }
    }
}