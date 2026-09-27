package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CircuitHistoryActivity extends AppCompatActivity {

    private ImageView btnHistoryBack;
    private RecyclerView recyclerCircuitHistory;
    private TextView tvHistoryEmpty;

    private CircuitStorageRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circuit_history);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        btnHistoryBack = findViewById(R.id.btnHistoryBack);
        recyclerCircuitHistory = findViewById(R.id.recyclerCircuitHistory);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);

        repository = new CircuitStorageRepository(this);
        btnHistoryBack.setOnClickListener(v -> finish());
        recyclerCircuitHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadList();
    }

    private void loadList() {
        List<SavedCircuit> circuits = repository.listAll();
        tvHistoryEmpty.setVisibility(circuits.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerCircuitHistory.setVisibility(circuits.isEmpty() ? View.GONE : View.VISIBLE);

        CircuitHistoryAdapter adapter = new CircuitHistoryAdapter(circuits, new CircuitHistoryAdapter.OnCircuitActionListener() {
            @Override
            public void onOpen(SavedCircuit circuit) {
                Intent intent = new Intent(CircuitHistoryActivity.this, BreadboardSimulatorActivity.class);
                intent.putExtra(BreadboardSimulatorActivity.EXTRA_CIRCUIT_ID, circuit.id);
                startActivity(intent);
            }

            @Override
            public void onDelete(SavedCircuit circuit) {
                repository.delete(circuit.id);
                Toast.makeText(CircuitHistoryActivity.this, R.string.circuit_history_deleted, Toast.LENGTH_SHORT).show();
                loadList();
            }
        });
        recyclerCircuitHistory.setAdapter(adapter);
    }
}