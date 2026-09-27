package com.example.circuitdoctor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private ImageView btnSettingsBack;
    private LinearLayout layoutDeveloperList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        layoutDeveloperList = findViewById(R.id.layoutDeveloperList);

        btnSettingsBack.setOnClickListener(v -> finish());

        setupDeveloperList();
    }

    private void setupDeveloperList() {
        List<DeveloperInfo> developers = new ArrayList<>();
        developers.add(new DeveloperInfo("นายปาโมท จันทะคุณ", "6812247016"));
        developers.add(new DeveloperInfo("นาย ธนากร บัวสาย", "6812247011"));
        developers.add(new DeveloperInfo("นายภูวนาท อั่วหงาน", "6812247021"));
        developers.add(new DeveloperInfo("นายพัทนาธร พรมธิดา", "6812247017"));
        developers.add(new DeveloperInfo("นาย ธนัญชัย ประเสริฐกุล", "6812247042"));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < developers.size(); i++) {
            DeveloperInfo dev = developers.get(i);
            View row = inflater.inflate(R.layout.item_developer_row, layoutDeveloperList, false);

            TextView tvNumber = row.findViewById(R.id.tvDeveloperNumber);
            TextView tvName = row.findViewById(R.id.tvDeveloperName);
            TextView tvId = row.findViewById(R.id.tvDeveloperId);

            tvNumber.setText(String.valueOf(i + 1));
            tvName.setText(dev.name);
            tvId.setText(dev.studentId);

            layoutDeveloperList.addView(row);
        }
    }
}