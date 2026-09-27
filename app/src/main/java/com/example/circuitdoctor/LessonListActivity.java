package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LessonListActivity extends AppCompatActivity implements OnItemClickListener {

    private final LessonRepository repository = new LessonRepository();

    private RecyclerView recyclerLessons;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_list);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        bindViews();
        setupLessonList();
        setupClickListeners();
    }

    private void bindViews() {
        recyclerLessons = findViewById(R.id.recyclerLessons);
        btnBack = findViewById(R.id.btnLessonListBack);
    }

    private void setupLessonList() {
        List<LessonItem> lessons = repository.getLessons(this);
        recyclerLessons.setLayoutManager(new LinearLayoutManager(this));
        recyclerLessons.setAdapter(new LessonAdapter(lessons, this));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public void onItemClick(int itemId) {
        Intent intent = new Intent(this, LessonDetailActivity.class);
        intent.putExtra(LessonDetailActivity.EXTRA_LESSON_ID, itemId);
        startActivity(intent);
    }
}