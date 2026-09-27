package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView btnRegister;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository(this);

        bindViews();
        setupClickListeners();
    }

    private void bindViews() {
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegisterSubmit);
        btnBack = findViewById(R.id.btnRegisterBack);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.register_error_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        authRepository.register(email, password);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}