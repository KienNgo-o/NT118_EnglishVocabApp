package com.example.nt118_englishvocabapp.ui.flashcard;

import android.app.Application;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.Definition;
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.models.LearnableItem;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardViewModel extends AndroidViewModel {

    private final ApiService apiService;
    private static final String TAG = "FlashcardViewModel";

    // LiveData cho Màn hình 1 (Danh sách chủ đề)
    private final MutableLiveData<List<Topic>> topicList = new MutableLiveData<>();

    // LiveData cho Màn hình 2 (Danh sách thẻ học "LearnableItem" đã làm phẳng)
    private final MutableLiveData<List<LearnableItem>> learnableList = new MutableLiveData<>();

    // LiveData cho thông báo lỗi
    private final MutableLiveData<String> error = new MutableLiveData<>();
    // Bộ nhớ đệm local đơn giản cho số lượng từ theo topic để hiển thị nhanh trong khi network refresh
    private final SharedPreferences prefs;

    // --- Mới: Lưu tiến trình học Flashcard ---
    // Lưu tiến trình theo chủ đề vào SharedPreferences với key "flashcard_progress_topic_{id}" và giá trị:
    // base64(topicName)|studied|total|timestamp
    private final SharedPreferences progressPrefs;
    private final MutableLiveData<List<ProgressItem>> progressList = new MutableLiveData<>();

    public FlashcardViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
        // Use the shared primary cache name to match VocabViewModel
        this.prefs = application.getApplicationContext().getSharedPreferences("topic_count_cache", Context.MODE_PRIVATE);
        this.progressPrefs = application.getApplicationContext().getSharedPreferences("flashcard_progress", Context.MODE_PRIVATE);

        // Initialize progress LiveData from stored prefs
        loadProgressFromPrefs();
    }

    public LiveData<List<Topic>> getTopics() { return topicList; }
    public LiveData<List<LearnableItem>> getLearnableItems() { return learnableList; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<ProgressItem>> getProgressList() { return progressList; }

    // --- Methods to save/load progress ---
    /**
     * Lưu tiến trình cho một chủ đề vào local (SharedPreferences).
     * topicName có thể chứa ký tự đặc biệt nên được mã hóa base64.
     * studied và total là các bộ đếm số nguyên. timestamp là System.currentTimeMillis().
     */
    public void saveProgress(int topicId, @NonNull String topicName, int studied, int total) {
        long ts = System.currentTimeMillis();
        // Resolve final topic name if not provided
        String finalName = topicName;
        try {
            if ((finalName == null || finalName.isEmpty()) && topicList.getValue() != null) {
                for (Topic t : topicList.getValue()) {
                    if (t != null && t.getTopicId() == topicId) {
                        finalName = t.getTopicName() != null ? t.getTopicName() : "";
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        if (finalName == null) finalName = "";

        // Check existing stored progress for this topic to keep the highest value
        int existingStudied = -1;
        int existingTotal = -1;
        long existingTs = -1L;
        try {
            String existing = progressPrefs.getString("progress_topic_" + topicId, null);
            if (existing != null) {
                String[] parts = existing.split("\\|", -1);
                if (parts.length >= 4) {
                    try { existingStudied = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    try { existingTotal = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                    try { existingTs = Long.parseLong(parts[3]); } catch (NumberFormatException ignored) {}
                }
            } else {
                // also check in-memory LiveData list
                ProgressItem pi = getProgressForTopic(topicId);
                if (pi != null) {
                    existingStudied = pi.studied;
                    existingTotal = pi.total;
                    existingTs = pi.timestamp;
                }
            }
        } catch (Exception ignored) {}

        float newFraction = (total <= 0) ? 0f : (float) studied / (float) total;
        float oldFraction = (existingTotal <= 0) ? 0f : (float) existingStudied / (float) existingTotal;

        // If existing progress is better or equal, skip update (keep highest)
        if (existingStudied >= 0) {
            if (newFraction < oldFraction) {
                Log.d(TAG, "saveProgress skipped (lower fraction): topicId=" + topicId + " new=" + newFraction + " old=" + oldFraction);
                return;
            } else if (Math.abs(newFraction - oldFraction) < 1e-6 && studied <= existingStudied) {
                Log.d(TAG, "saveProgress skipped (no improvement): topicId=" + topicId + " studied=" + studied + " existing=" + existingStudied);
                return;
            }
        }

        String encodedName = Base64.encodeToString(finalName.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String value = encodedName + "|" + studied + "|" + total + "|" + ts;
        progressPrefs.edit().putString("progress_topic_" + topicId, value).apply();

        Log.d(TAG, "saveProgress: topicId=" + topicId + " name=" + finalName + " studied=" + studied + " total=" + total + " ts=" + ts);

        // Update in-memory LiveData
        List<ProgressItem> current = progressList.getValue();
        if (current == null) current = new ArrayList<>();
        boolean found = false;
        for (int i = 0; i < current.size(); i++) {
            ProgressItem pi = current.get(i);
            if (pi.topicId == topicId) {
                current.set(i, new ProgressItem(topicId, finalName, studied, total, ts));
                found = true;
                break;
            }
        }
        if (!found) current.add(new ProgressItem(topicId, finalName, studied, total, ts));
        progressList.postValue(current);
    }

    public ProgressItem getProgressForTopic(int topicId) {
        List<ProgressItem> current = progressList.getValue();
        if (current == null) return null;
        for (ProgressItem pi : current) if (pi.topicId == topicId) return pi;
        return null;
    }

    private void loadProgressFromPrefs() {
        List<ProgressItem> list = new ArrayList<>();
        try {
            for (String key : progressPrefs.getAll().keySet()) {
                if (!key.startsWith("progress_topic_")) continue;
                Object obj = progressPrefs.getAll().get(key);
                if (!(obj instanceof String)) continue;
                String s = (String) obj;
                String[] parts = s.split("\\|", -1);
                if (parts.length < 4) continue;
                String encodedName = parts[0];
                String name = new String(Base64.decode(encodedName, Base64.NO_WRAP), StandardCharsets.UTF_8);
                int studied = 0;
                int total = 0;
                long ts = 0L;
                try { studied = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                try { total = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                try { ts = Long.parseLong(parts[3]); } catch (NumberFormatException ignored) {}
                String idStr = key.substring("progress_topic_".length());
                int id = -1;
                try { id = Integer.parseInt(idStr); } catch (NumberFormatException ignored) {}
                if (id >= 0) list.add(new ProgressItem(id, name, studied, total, ts));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load progress from prefs", e);
        }
        Log.d(TAG, "loadProgressFromPrefs: loaded " + list.size() + " items");
        progressList.postValue(list);
    }

    // Inner class representing saved progress for a topic
    public static class ProgressItem {
        public final int topicId;
        public final String topicName;
        public final int studied;
        public final int total;
        public final long timestamp;

        public ProgressItem(int topicId, String topicName, int studied, int total, long timestamp) {
            this.topicId = topicId;
            this.topicName = topicName;
            this.studied = studied;
            this.total = total;
            this.timestamp = timestamp;
        }

        public float getProgressFraction() {
            if (total <= 0) return 0f;
            return (float) studied / (float) total;
        }

        public int getProgressPercent() {
            return Math.round(getProgressFraction() * 100f);
        }
    }

    // --- Logic gọi API ---

    /**
     * Gọi API 1: Lấy tất cả chủ đề
     */
    public void fetchTopics() {
        apiService.getAllTopics().enqueue(new Callback<List<Topic>>() {
            @Override
            public void onResponse(@NonNull Call<List<Topic>> call, @NonNull Response<List<Topic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Topic> topics = response.body();

                    // Initialize wordCount to -1 (unknown) for each topic
                    for (Topic t : topics) t.setWordCount(-1);

                    // Apply cached counts immediately so UI can show numbers fast
                    for (Topic t : topics) {
                        int cached = getCachedCountForTopic(t.getTopicId());
                        if (cached >= 0) t.setWordCount(cached);
                    }

                    // Post the topics immediately so UI can render placeholders
                    topicList.postValue(topics);

                    // Now fetch flashcards for each topic concurrently to compute counts
                    // and post the updated list only once when all counts are ready to avoid UI flicker.
                    fetchCountsForTopicsConcurrently(topics);

                } else {
                    Log.e(TAG, "fetchTopics error: " + response.code());
                    error.postValue("Failed to load topics. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Topic>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchTopics failure: ", t);
                error.postValue("Network error: " + t.getMessage());
            }
        });
    }

    // Helper: để gọi API flashcards cho mỗi chủ đề song song và thu thập số lượng
    private void fetchCountsForTopicsConcurrently(final List<Topic> topics) {
        if (topics == null || topics.isEmpty()) {
            topicList.postValue(topics);
            return;
        }

        final int n = topics.size();
        final AtomicInteger remaining = new AtomicInteger(n);
        final int[] counts = new int[n];

        for (int i = 0; i < n; i++) counts[i] = -1; // unknown until fetched

        for (int i = 0; i < n; i++) {
            final int idx = i;
            final Topic topic = topics.get(idx);
            apiService.getFlashcardsForTopic(topic.getTopicId()).enqueue(new Callback<List<FlashcardItem>>() {
                @Override
                public void onResponse(@NonNull Call<List<FlashcardItem>> call, @NonNull Response<List<FlashcardItem>> response) {
                    int count = 0;
                    if (response.isSuccessful() && response.body() != null) {
                        List<FlashcardItem> items = response.body();
                        // Count total definitions (each Definition -> 1 LearnableItem)
                        int total = 0;
                        for (FlashcardItem fi : items) {
                            if (fi.getDefinitions() != null && !fi.getDefinitions().isEmpty()) {
                                total += fi.getDefinitions().size();
                            } else {
                                total += 1;
                            }
                        }
                        count = total;
                    }

                    counts[idx] = count;

                    // Build updated list reflecting current known counts
                    List<Topic> updatedList = new ArrayList<>(n);
                    for (int j = 0; j < n; j++) {
                        int wc = counts[j] >= 0 ? counts[j] : topics.get(j).getWordCount();
                        updatedList.add(topics.get(j).copyWithWordCount(wc));
                    }

                    // Persist this single count
                    saveCountForTopic(topic.getTopicId(), count);
                    Log.d(TAG, "Computed count for topicId=" + topic.getTopicId() + " -> " + count);
                    // Debug: log snapshot of counts
                    try {
                        StringBuilder sb = new StringBuilder();
                        for (Topic tt : updatedList) sb.append(tt.getTopicId()).append("=").append(tt.getWordCount()).append(",");
                        Log.d(TAG, "Posting updated topic list counts: " + sb.toString());
                    } catch (Exception ignored) {}
                    topicList.postValue(updatedList);

                    remaining.decrementAndGet();
                }

                @Override
                public void onFailure(@NonNull Call<List<FlashcardItem>> call, @NonNull Throwable t) {
                    counts[idx] = 0;
                    List<Topic> updatedList = new ArrayList<>(n);
                    for (int j = 0; j < n; j++) {
                        int wc = counts[j] >= 0 ? counts[j] : topics.get(j).getWordCount();
                        updatedList.add(topics.get(j).copyWithWordCount(wc));
                    }
                    saveCountForTopic(topic.getTopicId(), 0);
                    topicList.postValue(updatedList);
                    remaining.decrementAndGet();
                }
            });
        }
    }

    /**
     * Gọi API 2: Lấy flashcards cho một chủ đề
     * Sau đó làm phẳng dữ liệu thành List<LearnableItem>
     */
    public void fetchFlashcards(int topicId) {
        apiService.getFlashcardsForTopic(topicId).enqueue(new Callback<List<FlashcardItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<FlashcardItem>> call, @NonNull Response<List<FlashcardItem>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<FlashcardItem> originalItems = response.body();
                    List<LearnableItem> flatList = new ArrayList<>();

                    // Làm phẳng danh sách: mỗi định nghĩa của 1 từ → 1 LearnableItem riêng
                    for (FlashcardItem item : originalItems) {
                        if (item.getDefinitions() != null && !item.getDefinitions().isEmpty()) {
                            for (Definition def : item.getDefinitions()) {
                                flatList.add(new LearnableItem(item, def));
                            }
                        } else {
                            // Nếu từ không có định nghĩa, vẫn thêm vào danh sách để tránh mất dữ liệu
                            flatList.add(new LearnableItem(item, null));
                        }
                    }

                    learnableList.postValue(flatList);

                } else {
                    Log.e(TAG, "fetchFlashcards error: " + response.code());
                    error.postValue("Failed to load flashcards. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FlashcardItem>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchFlashcards failure: ", t);
                error.postValue("Network error: " + t.getMessage());
            }
        });
    }

    // Local cache helpers
    private void saveCountForTopic(int topicId, int count) {
        // Save to primary shared cache so Vocab and Flashcard share the same store
        prefs.edit().putInt("topic_count_" + topicId, count).apply();
    }

    private int getCachedCountForTopic(int topicId) {
        // Try primary cache first
        int primary = prefs.getInt("topic_count_" + topicId, -1);
        if (primary >= 0) return primary;

        // Fallback to legacy storages to preserve previously saved counts
        SharedPreferences legacyVocab = getApplication().getApplicationContext().getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE);
        int v = legacyVocab.getInt("topic_count_" + topicId, -1);
        if (v >= 0) return v;

        SharedPreferences legacyFlash = getApplication().getApplicationContext().getSharedPreferences("flashcard_prefs", Context.MODE_PRIVATE);
        int f = legacyFlash.getInt("topic_count_" + topicId, -1);
        return f >= 0 ? f : -1;
    }
}
