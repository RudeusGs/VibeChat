package com.rudeusgrey.vibechat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private TextInputLayout usernameInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private TextInputEditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private MaterialButton registerButton;
    private TextView loginTextView;
    private ImageButton backButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        initializeUI();

        setClickListeners();
    }

    private void initializeUI() {
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setClickListeners() {
        registerButton.setOnClickListener(v -> registerUser());

        loginTextView.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (!validateInput(username, email, password, confirmPassword)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(uid, username, email);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() :
                                getString(R.string.register_failed);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "createUserWithEmail:failure", task.getException());
                    }
                });
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        usernameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError("Vui lòng nhập tên người dùng");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Email không hợp lệ");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        return isValid;
    }

    private void saveUserToDatabase(String uid, String username, String email) {
        User newUser = new User(uid, username, email);

        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .setValue(newUser)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() :
                                "Lỗi lưu thông tin người dùng";
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "saveUserToDatabase:failure", task.getException());
                    }
                });
    }

    public static class User {
        public String uid;
        public String username;
        public String email;
        public String photoUrl;
        public String status;
        public long createdAt;
        public String fullName;
        public String dateOfBirth;
        public String gender;

        public User() {

        }

        public User(String uid, String username, String email) {
            this.uid = uid;
            this.username = username;
            this.email = email;
            this.photoUrl = "";
            this.status = "Hey, I'm using VibeChat!";
            this.createdAt = System.currentTimeMillis();
            this.fullName = "";
            this.dateOfBirth = "";
            this.gender = "";
        }
    }
}