package com.example.circuitdoctor;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class LessonRepository {

    public static final int LESSON_BASIC_ELECTRICITY = 301;
    public static final int LESSON_OHMS_LAW = 302;
    public static final int LESSON_SERIES_CIRCUIT = 303;
    public static final int LESSON_PARALLEL_CIRCUIT = 304;
    public static final int LESSON_KIRCHHOFF = 305;
    public static final int LESSON_RESISTOR_COLOR = 306;
    public static final int LESSON_BREADBOARD = 307;
    public static final int LESSON_DIODE_LED = 308;

    public List<LessonItem> getLessons(Context c) {
        List<LessonItem> list = new ArrayList<>();

        list.add(new LessonItem(LESSON_BASIC_ELECTRICITY,
                c.getString(R.string.lesson_basic_title),
                c.getString(R.string.lesson_basic_desc),
                c.getString(R.string.lesson_level_beginner),
                R.drawable.ic_bolt, c.getColor(R.color.accent_orange)));

        list.add(new LessonItem(LESSON_OHMS_LAW,
                c.getString(R.string.lesson_ohm_title),
                c.getString(R.string.lesson_ohm_desc),
                c.getString(R.string.lesson_level_beginner),
                R.drawable.ic_voltage, c.getColor(R.color.accent_blue)));

        list.add(new LessonItem(LESSON_SERIES_CIRCUIT,
                c.getString(R.string.lesson_series_title),
                c.getString(R.string.lesson_series_desc),
                c.getString(R.string.lesson_level_beginner),
                R.drawable.ic_circuit, c.getColor(R.color.accent_green)));

        list.add(new LessonItem(LESSON_PARALLEL_CIRCUIT,
                c.getString(R.string.lesson_parallel_title),
                c.getString(R.string.lesson_parallel_desc),
                c.getString(R.string.lesson_level_beginner),
                R.drawable.ic_circuit, c.getColor(R.color.accent_teal)));

        list.add(new LessonItem(LESSON_KIRCHHOFF,
                c.getString(R.string.lesson_kirchhoff_title),
                c.getString(R.string.lesson_kirchhoff_desc),
                c.getString(R.string.lesson_level_intermediate),
                R.drawable.ic_signal, c.getColor(R.color.accent_purple)));

        list.add(new LessonItem(LESSON_RESISTOR_COLOR,
                c.getString(R.string.lesson_resistor_title),
                c.getString(R.string.lesson_resistor_desc),
                c.getString(R.string.lesson_level_beginner),
                R.drawable.ic_led, c.getColor(R.color.accent_pink)));

        list.add(new LessonItem(LESSON_BREADBOARD,
                c.getString(R.string.lesson_breadboard_title),
                c.getString(R.string.lesson_breadboard_desc),
                c.getString(R.string.lesson_level_beginner),
                R.drawable.ic_breadboard, c.getColor(R.color.accent_blue)));

        list.add(new LessonItem(LESSON_DIODE_LED,
                c.getString(R.string.lesson_diode_title),
                c.getString(R.string.lesson_diode_desc),
                c.getString(R.string.lesson_level_intermediate),
                R.drawable.ic_led, c.getColor(R.color.accent_orange)));

        return list;
    }

    public String getLessonContent(Context c, int lessonId) {
        switch (lessonId) {
            case LESSON_BASIC_ELECTRICITY: return c.getString(R.string.lesson_basic_content);
            case LESSON_OHMS_LAW: return c.getString(R.string.lesson_ohm_content);
            case LESSON_SERIES_CIRCUIT: return c.getString(R.string.lesson_series_content);
            case LESSON_PARALLEL_CIRCUIT: return c.getString(R.string.lesson_parallel_content);
            case LESSON_KIRCHHOFF: return c.getString(R.string.lesson_kirchhoff_content);
            case LESSON_RESISTOR_COLOR: return c.getString(R.string.lesson_resistor_content);
            case LESSON_BREADBOARD: return c.getString(R.string.lesson_breadboard_content);
            case LESSON_DIODE_LED: return c.getString(R.string.lesson_diode_content);
            default: return "";
        }
    }

    public String getLessonTitle(Context c, int lessonId) {
        for (LessonItem item : getLessons(c)) {
            if (item.getId() == lessonId) return item.getTitle();
        }
        return "";
    }
}