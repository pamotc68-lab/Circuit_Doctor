package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class DiagnosisResultActivity extends AppCompatActivity {

    public static final String EXTRA_SYMPTOM_ID = "extra_symptom_id";
    public static final String EXTRA_CAUSE_A = "extra_cause_a";
    public static final String EXTRA_CAUSE_B = "extra_cause_b";
    public static final String EXTRA_CAUSE_C = "extra_cause_c";

    private final DiagnosticRepository repository = new DiagnosticRepository();

    private ImageView btnClose;
    private TextView tvResultTitle;
    private TextView tvResultDesc;
    private LinearLayout recommendationContainer;
    private TextView btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosis_result);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        bindViews();
        renderResult();
        setupClickListeners();
    }

    private void bindViews() {
        btnClose = findViewById(R.id.btnResultClose);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultDesc = findViewById(R.id.tvResultDesc);
        recommendationContainer = findViewById(R.id.recommendationContainer);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private void renderResult() {
        int symptomId = getIntent().getIntExtra(EXTRA_SYMPTOM_ID, -1);
        int countA = getIntent().getIntExtra(EXTRA_CAUSE_A, 0);
        int countB = getIntent().getIntExtra(EXTRA_CAUSE_B, 0);
        int countC = getIntent().getIntExtra(EXTRA_CAUSE_C, 0);

        DiagnosisResult result = repository.resolveResult(this, symptomId, countA, countB, countC);

        tvResultTitle.setText(result.getTitle());
        tvResultDesc.setText(result.getDescription());

        recommendationContainer.removeAllViews();
        List<String> recommendations = result.getRecommendations();
        for (int i = 0; i < recommendations.size(); i++) {
            recommendationContainer.addView(
                    RecommendationViewBinder.inflate(recommendationContainer, i + 1, recommendations.get(i)));
        }
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> goHome());
        btnBackHome.setOnClickListener(v -> goHome());
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}