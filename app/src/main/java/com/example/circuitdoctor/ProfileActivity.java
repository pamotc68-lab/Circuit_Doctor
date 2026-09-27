package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private ImageView btnBack;
    private TextView tvAccountType;
    private TextView tvAccountEmail;
    private TextView btnLogout;
    private LinearLayout btnUsageHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        new UsageLogRepository(this).logVisit("โปรไฟล์");

        authRepository = new AuthRepository(this);

        bindViews();
        bindAccountInfo();
        setupClickListeners();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnProfileBack);
        tvAccountType = findViewById(R.id.tvAccountType);
        tvAccountEmail = findViewById(R.id.tvAccountEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnUsageHistory = findViewById(R.id.btnUsageHistory);
    }

    private void bindAccountInfo() {
        if (authRepository.isGuest()) {
            tvAccountType.setText(R.string.profile_guest_label);
            tvAccountEmail.setText(R.string.profile_guest_desc);
        } else {
            tvAccountType.setText(R.string.profile_member_label);
            tvAccountEmail.setText(authRepository.getEmail());
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> handleLogout());
        btnUsageHistory.setOnClickListener(v ->
                startActivity(new Intent(this, UsageHistoryActivity.class)));
    }

    private void handleLogout() {
        authRepository.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}