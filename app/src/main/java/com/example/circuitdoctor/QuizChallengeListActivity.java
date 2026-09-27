package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class QuizChallengeListActivity extends AppCompatActivity {

    private final ChallengeRepository repository = new ChallengeRepository();

    private ImageView btnQuizBack;
    private TextView tabLevelEasy, tabLevelMedium, tabLevelHard;
    private RecyclerView recyclerChallenges;
    private LinearLayout navHome, navWrite, navLearn, navProfile;

    private List<CircuitChallenge> allChallenges;
    private int currentLevel = ChallengeRepository.LEVEL_EASY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_challenge);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        bindViews();
        allChallenges = repository.getChallenges();
        recyclerChallenges.setLayoutManager(new LinearLayoutManager(this));

        setupClickListeners();
        showLevel(ChallengeRepository.LEVEL_EASY);
    }

    private void bindViews() {
        btnQuizBack = findViewById(R.id.btnQuizBack);
        tabLevelEasy = findViewById(R.id.tabLevelEasy);
        tabLevelMedium = findViewById(R.id.tabLevelMedium);
        tabLevelHard = findViewById(R.id.tabLevelHard);
        recyclerChallenges = findViewById(R.id.recyclerChallenges);
        navHome = findViewById(R.id.navHome);
        navWrite = findViewById(R.id.navWrite);
        navLearn = findViewById(R.id.navLearn);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupClickListeners() {
        btnQuizBack.setOnClickListener(v -> finish());

        tabLevelEasy.setOnClickListener(v -> showLevel(ChallengeRepository.LEVEL_EASY));
        tabLevelMedium.setOnClickListener(v -> showLevel(ChallengeRepository.LEVEL_MEDIUM));
        tabLevelHard.setOnClickListener(v -> showLevel(ChallengeRepository.LEVEL_HARD));

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

    private void showLevel(int level) {
        currentLevel = level;
        updateTabSelection();

        List<CircuitChallenge> filtered = new ArrayList<>();
        for (CircuitChallenge c : allChallenges) {
            if (c.level == level) filtered.add(c);
        }

        ChallengeAdapter adapter = new ChallengeAdapter(filtered, challenge -> {
            Intent intent = new Intent(this, BreadboardSimulatorActivity.class);
            intent.putExtra(BreadboardSimulatorActivity.EXTRA_CHALLENGE_ID, challenge.id);
            startActivity(intent);
        });
        recyclerChallenges.setAdapter(adapter);
    }

    private void updateTabSelection() {
        tabLevelEasy.setTextColor(getColor(currentLevel == ChallengeRepository.LEVEL_EASY ? R.color.text_primary : R.color.text_secondary));
        tabLevelMedium.setTextColor(getColor(currentLevel == ChallengeRepository.LEVEL_MEDIUM ? R.color.text_primary : R.color.text_secondary));
        tabLevelHard.setTextColor(getColor(currentLevel == ChallengeRepository.LEVEL_HARD ? R.color.text_primary : R.color.text_secondary));
    }
}