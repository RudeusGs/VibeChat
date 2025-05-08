package com.rudeusgrey.vibechat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton;
    private TextView registerTextView, forgotPasswordTextView;
    private MaterialCardView googleLoginButton, facebookLoginButton, phoneLoginButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initializeUI();

        // Set click listeners
        setClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void initializeUI() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        googleLoginButton = findViewById(R.id.googleLoginButton);
        facebookLoginButton = findViewById(R.id.facebookLoginButton);
        phoneLoginButton = findViewById(R.id.phoneLoginButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setClickListeners() {
        loginButton.setOnClickListener(v -> loginUser());

        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        forgotPasswordTextView.setOnClickListener(v -> {
            // TODO: Implement forgot password functionality
            Toast.makeText(LoginActivity.this, "Chức năng đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        googleLoginButton.setOnClickListener(v -> {
            // TODO: Implement Google sign-in
            Toast.makeText(LoginActivity.this, "Đăng nhập với Google đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        facebookLoginButton.setOnClickListener(v -> {
            // TODO: Implement Facebook sign-in
            Toast.makeText(LoginActivity.this, "Đăng nhập với Facebook đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        phoneLoginButton.setOnClickListener(v -> {
            // TODO: Implement Phone sign-in
            Toast.makeText(LoginActivity.this, "Đăng nhập với số điện thoại đang được phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Vui lòng nhập email");
            return;
        } else {
            emailInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Vui lòng nhập mật khẩu");
            return;
        } else {
            passwordInputLayout.setError(null);
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // If sign in fails, display a message to the user
                        String errorMessage = task.getException() != null ? task.getException().getMessage() :
                                getString(R.string.login_failed);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
