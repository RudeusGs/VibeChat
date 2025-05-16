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

public class GroupChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton, attachButton;
    private ImageButton backButton;
    private TextView groupNameTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference groupsRef, messagesRef;
    private List<ChatActivity.Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private String groupId;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 3;
    private ValueEventListener messagesListener;
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

        groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        messagesRef = FirebaseDatabase.getInstance().getReference("groups");
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Không tìm thấy ID nhóm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        attachButton = findViewById(R.id.attachButton);
        backButton = findViewById(R.id.backButton);
        groupNameTextView = findViewById(R.id.chatNameTextView);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList, mAuth.getCurrentUser().getUid());
        chatRecyclerView.setAdapter(messageAdapter);

        loadGroupInfo();
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
    }

    private void loadGroupInfo() {
        groupsRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String groupName = snapshot.child("groupName").getValue(String.class);
                if (groupName != null) {
                    groupNameTextView.setText(groupName);
                } else {
                    groupNameTextView.setText("Nhóm không xác định");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(GroupChatActivity.this, "Lỗi tải thông tin nhóm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        messagesListener = messagesRef.child(groupId).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatActivity.Message message = child.getValue(ChatActivity.Message.class);
                    if (message != null) {
                        message.messageId = child.getKey();
                        // Gán receiverId là groupId để phân biệt tin nhắn nhóm
                        message.receiverId = groupId;
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(GroupChatActivity.this, "Lỗi tải tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String content, String imageUrl) {
        String messageId = messagesRef.child(groupId).child("messages").push().getKey();
        ChatActivity.Message message = new ChatActivity.Message(mAuth.getCurrentUser().getUid(), groupId, content, System.currentTimeMillis(), imageUrl);
        if (messageId != null) {
            messagesRef.child(groupId).child("messages").child(messageId).setValue(message);
        }
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
                        Toast.makeText(GroupChatActivity.this, "Lỗi hình ảnh", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(GroupChatActivity.this, "Tải ảnh thất bại: " + response.code(), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(GroupChatActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesRef.child(groupId).child("messages").removeEventListener(messagesListener);
        }
    }
}