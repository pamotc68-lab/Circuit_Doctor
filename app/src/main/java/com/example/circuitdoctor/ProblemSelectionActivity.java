package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProblemSelectionActivity extends AppCompatActivity implements OnItemClickListener {

    private final DiagnosticRepository repository = new DiagnosticRepository();

    private RecyclerView recyclerSymptoms;
    private ImageView btnBack;
    private LinearLayout navHome, navWrite, navLearn, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_selection);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        bindViews();
        setupSymptomList();
        setupClickListeners();
    }

    private void bindViews() {
        recyclerSymptoms = findViewById(R.id.recyclerSymptoms);
        btnBack = findViewById(R.id.btnBack);
        navHome = findViewById(R.id.navHome);
        navWrite = findViewById(R.id.navWrite);
        navLearn = findViewById(R.id.navLearn);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupSymptomList() {
        List<SymptomItem> symptoms = repository.getSymptomList(this);
        recyclerSymptoms.setLayoutManager(new LinearLayoutManager(this));
        recyclerSymptoms.setAdapter(new SymptomAdapter(symptoms, this));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        navWrite.setOnClickListener(v ->
                startActivity(new Intent(this, LessonListActivity.class)));
        navLearn.setOnClickListener(v ->
                startActivity(new Intent(this, CircuitHistoryActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    public void onItemClick(int itemId) {
        String title;
        switch (itemId) {
            case DiagnosticRepository.SYMPTOM_LED_OFF:
                title = getString(R.string.symptom_led_title);
                break;
            case DiagnosticRepository.SYMPTOM_MOTOR_NOT_WORKING:
                title = getString(R.string.symptom_motor_title);
                break;
            case DiagnosticRepository.SYMPTOM_NO_POWER:
                title = getString(R.string.symptom_no_power_title);
                break;
            case DiagnosticRepository.SYMPTOM_VOLTAGE_DROP:
                title = getString(R.string.symptom_voltage_title);
                break;
            case DiagnosticRepository.SYMPTOM_NO_SIGNAL:
                title = getString(R.string.symptom_signal_title);
                break;
            default:
                return;
        }

        Intent intent = new Intent(this, DiagnosisQuizActivity.class);
        intent.putExtra(DiagnosisQuizActivity.EXTRA_SYMPTOM_ID, itemId);
        intent.putExtra(DiagnosisQuizActivity.EXTRA_SYMPTOM_TITLE, title);
        startActivity(intent);
    }
}