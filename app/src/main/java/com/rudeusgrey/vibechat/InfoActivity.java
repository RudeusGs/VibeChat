package com.rudeusgrey.vibechat;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;

public class InfoActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private ImageView userImageView;
    private TextView userNameTextView, dobTextView, genderTextView;
    private MaterialButton editButton, logoutButton, securityButton;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        userImageView = findViewById(R.id.userImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        dobTextView = findViewById(R.id.dobTextView);
        genderTextView = findViewById(R.id.genderTextView);
        editButton = findViewById(R.id.editButton);
        logoutButton = findViewById(R.id.logoutButton);
        securityButton = findViewById(R.id.securityButton);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setSelectedItemId(R.id.nav_info);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                startActivity(new Intent(InfoActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_list) {
                startActivity(new Intent(InfoActivity.this, FriendsListActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_info) {
                return true;
            }
            return false;
        });

        loadUserInfo();

        editButton.setOnClickListener(v -> showEditDialog());
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(InfoActivity.this, LoginActivity.class));
            finish();
            Toast.makeText(InfoActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        });

        securityButton.setOnClickListener(v -> {
            Toast.makeText(InfoActivity.this, "Chức năng bảo mật đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserInfo() {
        String userId = mAuth.getCurrentUser().getUid();
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                RegisterActivity.User user = snapshot.getValue(RegisterActivity.User.class);
                if (user != null) {
                    userNameTextView.setText(user.username);
                    dobTextView.setText(user.dateOfBirth != null && !user.dateOfBirth.isEmpty() ?
                            "Ngày sinh: " + user.dateOfBirth : "Ngày sinh: Chưa cập nhật");
                    genderTextView.setText(user.gender != null && !user.gender.isEmpty() ?
                            "Giới tính: " + user.gender : "Giới tính: Chưa cập nhật");

                    String photoUrl = user.photoUrl;
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(InfoActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(userImageView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(InfoActivity.this, "Không thể tải thông tin: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        TextInputEditText fullNameEditText = dialogView.findViewById(R.id.fullNameEditText);
        TextInputEditText dobEditText = dialogView.findViewById(R.id.dobEditText);
        TextInputEditText genderEditText = dialogView.findViewById(R.id.genderEditText);
        MaterialButton saveButton = dialogView.findViewById(R.id.saveButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);

        fullNameEditText.setText(userNameTextView.getText().toString());
        dobEditText.setText(dobTextView.getText().toString().replace("Ngày sinh: ", ""));
        genderEditText.setText(genderTextView.getText().toString().replace("Giới tính: ", ""));

        dobEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(InfoActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                        dobEditText.setText(date);
                    }, year, month, day);
            datePickerDialog.show();
        });

        genderEditText.setOnClickListener(v -> {
            String[] genders = {"Nam", "Nữ", "Khác"};
            new AlertDialog.Builder(InfoActivity.this)
                    .setTitle("Chọn giới tính")
                    .setItems(genders, (dialog, which) -> genderEditText.setText(genders[which]))
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String fullName = fullNameEditText.getText().toString().trim();
            String dob = dobEditText.getText().toString().trim();
            String gender = genderEditText.getText().toString().trim();

            if (fullName.isEmpty()) {
                Toast.makeText(InfoActivity.this, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();
            RegisterActivity.User updatedUser = new RegisterActivity.User();
            updatedUser.uid = userId;
            updatedUser.username = fullName;
            updatedUser.fullName = fullName;
            updatedUser.dateOfBirth = dob.isEmpty() || dob.equals("Chưa cập nhật") ? "" : dob;
            updatedUser.gender = gender.isEmpty() || gender.equals("Chưa cập nhật") ? "" : gender;
            updatedUser.photoUrl = "";
            updatedUser.status = "Hey, I'm using VibeChat!";
            updatedUser.createdAt = System.currentTimeMillis();

            usersRef.child(userId).setValue(updatedUser)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(InfoActivity.this, "Thông tin đã được lưu", Toast.LENGTH_SHORT).show();
                            loadUserInfo();
                        } else {
                            Toast.makeText(InfoActivity.this, "Lỗi khi lưu thông tin", Toast.LENGTH_SHORT).show();
                        }
                    });

            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}