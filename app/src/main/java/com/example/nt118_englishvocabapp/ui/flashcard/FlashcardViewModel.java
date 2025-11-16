package com.example.nt118_englishvocabapp.ui.flashcard;

import android.app.Application;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;

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
    // Simple local cache for topic word counts to show instant results while network refreshes
    private final SharedPreferences prefs;

    public FlashcardViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
        // Use the shared primary cache name to match VocabViewModel
        this.prefs = application.getApplicationContext().getSharedPreferences("topic_count_cache", Context.MODE_PRIVATE);
    }

    public LiveData<List<Topic>> getTopics() { return topicList; }
    public LiveData<List<LearnableItem>> getLearnableItems() { return learnableList; }
    public LiveData<String> getError() { return error; }

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
