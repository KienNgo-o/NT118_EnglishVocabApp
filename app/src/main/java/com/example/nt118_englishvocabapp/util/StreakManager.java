package com.example.nt118_englishvocabapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;
import com.example.nt118_englishvocabapp.network.SessionManager;
import com.example.nt118_englishvocabapp.network.dto.UpdateStreakRequest;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Simple local streak manager.
 * - Records dates (yyyy-MM-dd) when user performed an activity (finish 1 flashcard)
 * - Exposes methods to query active days for a given month and total active days
 * - Persists using SharedPreferences
 * - Syncs streak count to backend when user completes daily activity
 */
public class StreakManager {
    private static final String TAG = "StreakManager";
    private static final String PREFS = "streak_prefs";
    private static final String KEY_DATES = "streak_dates"; // stored as comma-separated yyyy-MM-dd
    private static final String KEY_PENDING_ANNOUNCE = "streak_pending_announce"; // date string when announcement pending
    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SharedPreferences prefs;
    private final Context context;

    public StreakManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // record today as active (idempotent). Returns true if today was newly added (i.e. first mark today)
    public boolean markTodayActive() {
        String today = ISO.format(new Date());
        Set<String> set = getDatesSet();
        if (!set.contains(today)) {
            set.add(today);
            persistSet(set); // persist synchronously
            // set pending announce so HomeFragment can show dialog when it becomes visible
            try {
                prefs.edit().putString(KEY_PENDING_ANNOUNCE, today).commit(); // commit to avoid race
            } catch (Exception e) {
                // fallback to apply if commit fails
                prefs.edit().putString(KEY_PENDING_ANNOUNCE, today).apply();
            }

            return true;
        }
        return false;
    }

    // whether there is a pending announcement for today
    public boolean hasPendingAnnouncementForToday() {
        String today = ISO.format(new Date());
        String pending = prefs.getString(KEY_PENDING_ANNOUNCE, null);
        return today.equals(pending);
    }

    // clear any pending announcement (call after showing dialog)
    public void clearPendingAnnouncement() {
        prefs.edit().remove(KEY_PENDING_ANNOUNCE).apply();
    }

    /**
     * Sync current streak count to backend server.
     * Call this after showing streak dialog/animation to user.
     */
    public void syncStreakToServer() {
        int currentStreak = getCurrentStreak();
        try {
            // Log the outgoing payload for easier debugging
            Log.d(TAG, "Preparing to sync streak to server: " + currentStreak);

            // Log access token presence (masked) to help debug auth-related issues
            try {
                SessionManager sm = SessionManager.getInstance(context);
                String token = sm != null ? sm.getAccessToken() : null;
                String masked = (token == null) ? "<null>" : (token.length() <= 8 ? "<present>" : (token.substring(0,4) + "..." + token.substring(token.length()-4)));
                Log.d(TAG, "Access token: " + masked);
            } catch (Exception ignored) {}

            // Log stored dates and today's date to confirm what's being sent
            try {
                Set<String> dates = getDatesSet();
                Log.d(TAG, "Stored streak dates: " + dates.toString());
                Log.d(TAG, "Today (local): " + ISO.format(new Date()));
            } catch (Exception ignored) {}

            // Check access token - if not present, skip network call and log clearly
            try {
                SessionManager sm = SessionManager.getInstance(context);
                String token = sm != null ? sm.getAccessToken() : null;
                if (token == null) {
                    Log.w(TAG, "Not sending streak to server: no access token available (user not logged in)");
                    return;
                }
            } catch (Exception ignored) {}

            ApiService apiService = RetrofitClient.getApiService(context);
            UpdateStreakRequest request = new UpdateStreakRequest(currentStreak);

            // serialize with Gson and log body (non-sensitive) for troubleshooting
            try {
                String json = new Gson().toJson(request);
                Log.d(TAG, "Streak request JSON: " + json);
            } catch (Exception ignored) {}

            // Send the request with explicit Authorization header using the stored access token
            try {
                String token = SessionManager.getInstance(context).getAccessToken();

                // Decode JWT payload for debugging (do not expose full token) and log truncated payload
                try {
                    if (token != null && token.split("\\.").length >= 2) {
                        String[] parts = token.split("\\.");
                        String payloadB64 = parts[1];
                        byte[] decoded = android.util.Base64.decode(payloadB64, android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING | android.util.Base64.NO_WRAP);
                        String payload = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                        String trunc = payload.length() > 300 ? payload.substring(0,300) + "..." : payload;
                        Log.d(TAG, "Decoded token payload: " + trunc);
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Failed to decode token payload", ex);
                }

                String authHeader = "Bearer " + token;
                apiService.updateStreakWithAuth(authHeader, request).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Streak synced to server successfully: " + currentStreak);
                        } else {
                            try {
                                String err = "<no body>";
                                if (response.errorBody() != null) {
                                    try { err = response.errorBody().string(); } catch (Exception ex) { err = "<error reading body>"; }
                                }
                                String url = "<unknown>";
                                String respHeaders = "";
                                try {
                                    okhttp3.Request req = response.raw().request();
                                    if (req != null && req.url() != null) url = req.url().toString();
                                    respHeaders = response.headers() != null ? response.headers().toString() : "";
                                } catch (Exception ex) { /* ignore */ }

                                Log.w(TAG, "Failed to sync streak. URL: " + url + " Response code: " + response.code() + " headers: " + respHeaders + " body: " + err);

                            } catch (Exception ex) {
                                Log.w(TAG, "Failed to sync streak. Response code: " + response.code() + " (error reading body)");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Network error syncing streak to server", t);
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, "Error sending streak with explicit auth", ex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating API call for streak sync", e);
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
        // persist synchronously to avoid races where HomeFragment reads before apply() completes
        try {
            prefs.edit().putString(KEY_DATES, sb.toString()).commit();
        } catch (Exception e) {
            prefs.edit().putString(KEY_DATES, sb.toString()).apply();
        }
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

    /**
     * Returns the current consecutive streak ending today (number of consecutive days including today).
     * If today is not active, returns 0.
     */
    public int getCurrentStreak() {
        Set<String> set = getDatesSet();
        if (set == null || set.isEmpty()) return 0;
        Calendar cal = Calendar.getInstance();
        int streak = 0;
        while (true) {
            String day = ISO.format(cal.getTime());
            if (set.contains(day)) {
                streak++;
                cal.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Returns the longest consecutive streak found among recorded dates.
     */
    public int getLongestStreak() {
        Set<String> set = getDatesSet();
        if (set == null || set.isEmpty()) return 0;
        int best = 0;
        for (String s : set) {
            try {
                Date d = ISO.parse(s);
                if (d == null) continue;
                Calendar start = Calendar.getInstance();
                start.setTime(d);
                Calendar prev = (Calendar) start.clone();
                prev.add(Calendar.DAY_OF_MONTH, -1);
                String prevStr = ISO.format(prev.getTime());
                if (set.contains(prevStr)) continue; // not start of a chain

                // expand forward
                int len = 0;
                Calendar cur = (Calendar) start.clone();
                while (true) {
                    String curStr = ISO.format(cur.getTime());
                    if (set.contains(curStr)) {
                        len++;
                        cur.add(Calendar.DAY_OF_MONTH, 1);
                    } else break;
                }
                if (len > best) best = len;
            } catch (Exception ignored) {}
        }
        return best;
    }

}
