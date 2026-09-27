package com.example.circuitdoctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private final MainRepository repository = new MainRepository();

    private LinearLayout layoutFeatureCards;
    private RecyclerView recyclerMenu;
    private View btnStartDiagnostic;
    private View btnSettings;
    private LinearLayout navHome, navWrite, navLearn, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        bindViews();
        setupFeatureCards();
        setupMenuList();
        setupClickListeners();
    }

    private void bindViews() {
        layoutFeatureCards = findViewById(R.id.layoutFeatureCards);
        recyclerMenu = findViewById(R.id.recyclerMenu);
        btnStartDiagnostic = findViewById(R.id.btnStartDiagnostic);
        btnSettings = findViewById(R.id.btnSettings);
        navHome = findViewById(R.id.navHome);
        navWrite = findViewById(R.id.navWrite);
        navLearn = findViewById(R.id.navLearn);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupFeatureCards() {
        List<FeatureCard> cards = repository.getFeatureCards(this);
        layoutFeatureCards.removeAllViews();

        for (int i = 0; i < cards.size(); i++) {
            View cardView = FeatureCardViewBinder.inflate(layoutFeatureCards, cards.get(i), this);

            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                llParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.card_spacing);
            }
            cardView.setLayoutParams(llParams);

            layoutFeatureCards.addView(cardView);
        }
    }

    private void setupMenuList() {
        List<MenuItem> menuItems = repository.getMenuItems(this);
        recyclerMenu.setLayoutManager(new LinearLayoutManager(this));
        recyclerMenu.setAdapter(new MenuAdapter(menuItems, this));
    }

    private void setupClickListeners() {
        btnStartDiagnostic.setOnClickListener(v ->
                startActivity(new Intent(this, ProblemSelectionActivity.class)));

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        navHome.setOnClickListener(v -> {
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
        switch (itemId) {
            case FeatureCard.TYPE_WRITE_CIRCUIT:
                startActivity(new Intent(this, LessonListActivity.class));
                break;
            case FeatureCard.TYPE_QUIZ:
                startActivity(new Intent(this, QuizChallengeListActivity.class));
                break;
            case MainRepository.MENU_HISTORY:
                startActivity(new Intent(this, CircuitHistoryActivity.class));
                break;
            case MainRepository.MENU_AI_CHAT:
                startActivity(new Intent(this, AiChatActivity.class));
                break;
            case MainRepository.MENU_BREADBOARD:
                startActivity(new Intent(this, BreadboardSimulatorActivity.class));
                break;
        }
    }
}