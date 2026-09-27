package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new UsageLogRepository(this).clearAll();

        new Handler(Looper.getMainLooper()).postDelayed(this::goNext, SPLASH_DELAY_MS);
    }

    private void goNext() {
        AuthRepository authRepository = new AuthRepository(this);
        Class<?> target = authRepository.isLoggedIn() ? MainActivity.class : LoginActivity.class;
        startActivity(new Intent(this, target));
        finish();
    }
}