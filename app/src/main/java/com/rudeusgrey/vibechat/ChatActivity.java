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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.app.ProgressDialog;
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
    private DatabaseReference chatsRef, usersRef;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String friendId;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private ValueEventListener messagesListener;
    private final OkHttpClient client = new OkHttpClient();
    private static final String IMGUR_CLIENT_ID = "c16af2a45269cf2"; // Thay bằng Client ID thật của bạn
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

        loadFriendInfo();
        loadMessages();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = messageEditText.getText().toString().trim();
                if (!content.isEmpty()) {
                    sendMessage(content, null);
                    messageEditText.setText("");
                }
            }
        });

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStoragePermission();
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

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối vĩnh viễn. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
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
        if (imageUri == null) {
            Toast.makeText(this, "Không thể chọn hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra MIME type của file
        String mimeType = getFileMimeType(imageUri);
        if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            Toast.makeText(this, "Định dạng file không được hỗ trợ. Vui lòng chọn file JPEG, PNG, GIF, BMP, TIFF hoặc WebP.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải ảnh lên...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                // Chuyển URI thành File
                File file = uriToFile(imageUri);
                if (file == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Không thể lấy hình ảnh", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                    return;
                }

                // Đảm bảo file có dữ liệu
                if (file.length() == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "File hình ảnh rỗng hoặc bị hỏng", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                    return;
                }

                String extension = getFileExtensionFromMimeType(mimeType);
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
                    String errorBody = response.body() != null ? response.body().string() : "Không có thông tin lỗi";
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Tải ảnh lên Imgur thất bại: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Lỗi khi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                });
            }
        }).start();
    }

    private String getFileMimeType(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        String mimeType = contentResolver.getType(uri);
        if (mimeType == null) {
            // Nếu không lấy được MIME type, thử lấy từ phần mở rộng
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot != -1) {
                    String extension = path.substring(lastDot + 1);
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    mimeType = mime.getMimeTypeFromExtension(extension);
                }
            }
        }
        return mimeType;
    }

    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            return mime.getExtensionFromMimeType(mimeType);
        }
        return "jpg"; // Mặc định là jpg nếu không xác định được
    }

    private File uriToFile(Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) return null;

            String mimeType = getFileMimeType(uri);
            String extension = getFileExtensionFromMimeType(mimeType);
            if (extension == null) extension = "jpg"; // Mặc định là jpg nếu không xác định được
            File file = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + "." + extension);
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
                Toast.makeText(ChatActivity.this, "Không thể tải thông tin người bạn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
        messagesListener = new ValueEventListener() {
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
                if (!messageList.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Không thể tải tin nhắn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        chatsRef.child(chatId).child("messages").addValueEventListener(messagesListener);
    }

    private void sendMessage(String content, String imageUrl) {
        String chatId = getChatId(mAuth.getCurrentUser().getUid(), friendId);
        Message message = new Message(mAuth.getCurrentUser().getUid(), content, System.currentTimeMillis(), imageUrl);
        chatsRef.child(chatId).child("messages").push().setValue(message);
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
    }

    public static class Message {
        public String senderId, content, imageUrl;
        public long timestamp;
        public Map<String, String> reactions;
        public String messageId;

        public Message() {
            this.reactions = new HashMap<>();
        }

        public Message(String senderId, String content, long timestamp, String imageUrl) {
            this.senderId = senderId;
            this.content = content;
            this.timestamp = timestamp;
            this.imageUrl = imageUrl;
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