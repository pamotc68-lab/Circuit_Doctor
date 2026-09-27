package com.example.circuitdoctor;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UsageHistoryActivity extends AppCompatActivity {

    private ImageView btnUsageHistoryBack;
    private RecyclerView recyclerUsageHistory;
    private TextView tvUsageHistoryEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_history);

        btnUsageHistoryBack = findViewById(R.id.btnUsageHistoryBack);
        recyclerUsageHistory = findViewById(R.id.recyclerUsageHistory);
        tvUsageHistoryEmpty = findViewById(R.id.tvUsageHistoryEmpty);

        btnUsageHistoryBack.setOnClickListener(v -> finish());
        recyclerUsageHistory.setLayoutManager(new LinearLayoutManager(this));

        List<UsageLogEntry> logs = new UsageLogRepository(this).getRecentLogs();
        tvUsageHistoryEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerUsageHistory.setVisibility(logs.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerUsageHistory.setAdapter(new UsageHistoryAdapter(logs));
    }
}