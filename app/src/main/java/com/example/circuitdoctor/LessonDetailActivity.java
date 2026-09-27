package com.example.circuitdoctor;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LessonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "extra_lesson_id";

    private final LessonRepository repository = new LessonRepository();

    private ImageView btnBack;
    private TextView tvTitle;
    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_detail);

        bindViews();
        renderLesson();
        setupClickListeners();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnLessonDetailBack);
        tvTitle = findViewById(R.id.tvLessonDetailTitle);
        tvContent = findViewById(R.id.tvLessonDetailContent);
    }

    private void renderLesson() {
        int lessonId = getIntent().getIntExtra(EXTRA_LESSON_ID, -1);
        tvTitle.setText(repository.getLessonTitle(this, lessonId));
        tvContent.setText(repository.getLessonContent(this, lessonId));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}