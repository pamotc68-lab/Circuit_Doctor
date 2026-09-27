package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private EditText etEmail;
    private EditText etPassword;
    private TextView btnLogin;
    private TextView btnRegister;
    private TextView btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);

        bindViews();
        setupClickListeners();
    }

    private void bindViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        btnGuest.setOnClickListener(v -> handleGuestLogin());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authRepository.hasRegisteredAccount()) {
            Toast.makeText(this, R.string.login_error_no_account, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = authRepository.login(email, password);
        if (success) {
            goToMain();
        } else {
            Toast.makeText(this, R.string.login_error_invalid, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGuestLogin() {
        authRepository.loginAsGuest();
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}