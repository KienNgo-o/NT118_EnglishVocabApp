package com.example.nt118_englishvocabapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Simple local streak manager.
 * - Records dates (yyyy-MM-dd) when user performed an activity (finish 1 flashcard)
 * - Exposes methods to query active days for a given month and total active days
 * - Persists using SharedPreferences
 */
public class StreakManager {
    private static final String PREFS = "streak_prefs";
    private static final String KEY_DATES = "streak_dates"; // stored as comma-separated yyyy-MM-dd
    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SharedPreferences prefs;

    public StreakManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // record today as active (idempotent)
    public void markTodayActive() {
        String today = ISO.format(new Date());
        Set<String> set = getDatesSet();
        if (!set.contains(today)) {
            set.add(today);
            persistSet(set);
        }
    }

    private Set<String> getDatesSet() {
        String raw = prefs.getString(KEY_DATES, null);
        Set<String> set = new HashSet<>();
        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split(",");
            for (String p : parts) if (!p.trim().isEmpty()) set.add(p.trim());
        }
        return set;
    }

    private void persistSet(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : set) {
            if (!first) sb.append(",");
            sb.append(s);
            first = false;
        }
        prefs.edit().putString(KEY_DATES, sb.toString()).apply();
    }

    // Returns list of day numbers that are active for the given month/year (1-based)
    public List<Integer> getActiveDaysForMonth(int year, int monthZeroBased) {
        Set<String> set = getDatesSet();
        List<Integer> days = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (String s : set) {
            try {
                Date d = ISO.parse(s);
                if (d == null) continue;
                cal.setTime(d);
                int y = cal.get(Calendar.YEAR);
                int m = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                if (y == year && m == monthZeroBased) days.add(day);
            } catch (Exception ignored) {}
        }
        return days;
    }

    // total unique active days recorded
    public int getTotalActiveDays() {
        return getDatesSet().size();
    }

}

