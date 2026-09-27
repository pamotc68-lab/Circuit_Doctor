package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class DiagnosisQuizActivity extends AppCompatActivity {

    public static final String EXTRA_SYMPTOM_ID = "extra_symptom_id";
    public static final String EXTRA_SYMPTOM_TITLE = "extra_symptom_title";

    private final QuizRepository quizRepository = new QuizRepository();

    private int symptomId;
    private String symptomTitle;
    private List<QuizQuestion> questions;

    private int currentIndex = 0;
    private int selectedOptionIndex = -1;
    private final int[] causeCount = new int[4]; // index 1=A, 2=B, 3=C

    private ImageView btnBack;
    private TextView tvSymptomTitle;
    private TextView tvQuestionProgress;
    private TextView tvProgressPercent;
    private ProgressBar progressBar;
    private TextView tvQuestionText;
    private LinearLayout optionsContainer;
    private TextView btnPrevious;
    private TextView btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosis_quiz);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        symptomId = getIntent().getIntExtra(EXTRA_SYMPTOM_ID, -1);
        symptomTitle = getIntent().getStringExtra(EXTRA_SYMPTOM_TITLE);
        questions = quizRepository.getQuestions(symptomId);

        bindViews();
        setupClickListeners();
        renderQuestion();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnQuizBack);
        tvSymptomTitle = findViewById(R.id.tvQuizSymptomTitle);
        tvQuestionProgress = findViewById(R.id.tvQuestionProgress);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        tvSymptomTitle.setText(symptomTitle);
        progressBar.setMax(questions.size());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnPrevious.setOnClickListener(v -> goToPrevious());
        btnNext.setOnClickListener(v -> goToNext());
    }

    private void renderQuestion() {
        QuizQuestion question = questions.get(currentIndex);
        selectedOptionIndex = -1;

        tvQuestionProgress.setText(getString(R.string.quiz_progress_label, currentIndex + 1, questions.size()));
        int percent = (int) (((currentIndex + 1) / (float) questions.size()) * 100);
        tvProgressPercent.setText(percent + "%");
        progressBar.setProgress(currentIndex + 1);

        tvQuestionText.setText(question.getQuestionText());

        optionsContainer.removeAllViews();
        List<QuizOption> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            View row = QuizOptionViewBinder.inflate(optionsContainer, options.get(i).getText(), i);
            int optionIndex = i;
            row.setOnClickListener(v -> selectOption(optionIndex));
            optionsContainer.addView(row);
        }

        btnPrevious.setEnabled(currentIndex > 0);
        btnPrevious.setAlpha(currentIndex > 0 ? 1f : 0.4f);
        btnNext.setText(currentIndex == questions.size() - 1
                ? R.string.quiz_button_finish
                : R.string.quiz_button_next);

        updateOptionHighlights();
    }

    private void selectOption(int optionIndex) {
        selectedOptionIndex = optionIndex;
        updateOptionHighlights();
    }

    private void updateOptionHighlights() {
        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            QuizOptionViewBinder.setSelected(optionsContainer.getChildAt(i), i == selectedOptionIndex);
        }
    }

    private void goToPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            renderQuestion();
        }
    }

    private void goToNext() {
        if (selectedOptionIndex == -1) {
            return;
        }

        QuizOption chosen = questions.get(currentIndex).getOptions().get(selectedOptionIndex);
        causeCount[chosen.getCauseCode()]++;

        if (currentIndex == questions.size() - 1) {
            goToResult();
        } else {
            currentIndex++;
            renderQuestion();
        }
    }

    private void goToResult() {
        Intent intent = new Intent(this, DiagnosisResultActivity.class);
        intent.putExtra(DiagnosisResultActivity.EXTRA_SYMPTOM_ID, symptomId);
        intent.putExtra(DiagnosisResultActivity.EXTRA_CAUSE_A, causeCount[QuizOption.CAUSE_A]);
        intent.putExtra(DiagnosisResultActivity.EXTRA_CAUSE_B, causeCount[QuizOption.CAUSE_B]);
        intent.putExtra(DiagnosisResultActivity.EXTRA_CAUSE_C, causeCount[QuizOption.CAUSE_C]);
        startActivity(intent);
        finish();
    }
}