// ui/vocab/VocabViewModel.java
package com.example.nt118_englishvocabapp.ui.vocab;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.models.VocabWord; // 👈 THÊM IMPORT NÀY
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VocabViewModel extends AndroidViewModel {

    private static final String TAG = "VocabViewModel";
    private final ApiService apiService;

    // LiveData cho Màn hình 1 (Danh sách chủ đề)
    private final MutableLiveData<List<Topic>> topics = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // ❗️ BẮT ĐẦU CODE MỚI ❗️
    // LiveData cho Màn hình 2 (Danh sách từ)
    private final MutableLiveData<List<VocabWord>> wordList = new MutableLiveData<>();
    // Simple local cache for topic word counts
    private final SharedPreferences prefs;
    // ❗️ KẾT THÚC CODE MỚI ❗️

    public VocabViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
        this.prefs = application.getApplicationContext().getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE);
    }

    // --- Getters ---
    public LiveData<List<Topic>> getTopics() { return topics; }
    public LiveData<String> getError() { return error; }

    // ❗️ BẮT ĐẦU CODE MỚI ❗️
    public LiveData<List<VocabWord>> getWordList() { return wordList; }
    // ❗️ KẾT THÚC CODE MỚI ❗️


    // --- Logic API ---

    /**
     * API 1: Lấy danh sách chủ đề từ backend
     */
    public void fetchTopics() {
        apiService.getAllTopics().enqueue(new Callback<List<Topic>>() {
            @Override
            public void onResponse(@NonNull Call<List<Topic>> call, @NonNull Response<List<Topic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Topic> tlist = response.body();

                    // Initialize wordCount to -1 (unknown) for each topic
                    for (Topic t : tlist) t.setWordCount(-1);

                    // Apply cached counts immediately so UI can show numbers fast
                    for (Topic t : tlist) {
                        int cached = getCachedCountForTopic(t.getTopicId());
                        if (cached >= 0) t.setWordCount(cached);
                    }

                    // Post the topics immediately so UI can render placeholders
                    topics.postValue(tlist);

                    // Now fetch words for each topic concurrently to compute counts
                    // and post updated lists as counts arrive (avoids UI flicker)
                    fetchCountsForTopicsConcurrently(tlist);

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

    // Fetch words for all topics concurrently and post incremental updates
    private void fetchCountsForTopicsConcurrently(final List<Topic> topicList) {
        if (topicList == null || topicList.isEmpty()) {
            topics.postValue(topicList);
            return;
        }

        final int n = topicList.size();
        final AtomicInteger remaining = new AtomicInteger(n);
        final int[] counts = new int[n];

        for (int i = 0; i < n; i++) counts[i] = 0;

        for (int i = 0; i < n; i++) {
            final int idx = i;
            final Topic topic = topicList.get(idx);
            apiService.getWordsForTopic(topic.getTopicId()).enqueue(new Callback<List<VocabWord>>() {
                @Override
                public void onResponse(@NonNull Call<List<VocabWord>> call, @NonNull Response<List<VocabWord>> response) {
                    int count = 0;
                    if (response.isSuccessful() && response.body() != null) {
                        count = response.body().size();
                    }
                    counts[idx] = count;

                    // Post an updated list with just this topic's count updated so UI shows it ASAP
                    List<Topic> updated = new ArrayList<>(n);
                    for (int j = 0; j < n; j++) {
                        if (j == idx) updated.add(topicList.get(j).copyWithWordCount(counts[j]));
                        else updated.add(topicList.get(j));
                    }
                    // Persist this single count
                    saveCountForTopic(topic.getTopicId(), count);
                    topics.postValue(updated);
                    remaining.decrementAndGet();
                }

                @Override
                public void onFailure(@NonNull Call<List<VocabWord>> call, @NonNull Throwable t) {
                    counts[idx] = 0;
                    List<Topic> updated = new ArrayList<>(n);
                    for (int j = 0; j < n; j++) {
                        if (j == idx) updated.add(topicList.get(j).copyWithWordCount(0));
                        else updated.add(topicList.get(j));
                    }
                    saveCountForTopic(topic.getTopicId(), 0);
                    topics.postValue(updated);
                    remaining.decrementAndGet();
                }
            });
        }
    }

    // Helper: cache helpers (same behavior as FlashcardViewModel but for vocab)
    private int getCachedCountForTopic(int topicId) {
        return prefs.getInt("topic_count_" + topicId, -1);
    }

    private void saveCountForTopic(int topicId, int count) {
        prefs.edit().putInt("topic_count_" + topicId, count).apply();
    }

    // ❗️ BẮT ĐẦU CODE MỚI ❗️
    /**
     * API 2: Lấy danh sách từ vựng cho một chủ đề cụ thể
     */
    public void fetchWords(int topicId) {
        // Xóa dữ liệu cũ / báo hiệu đang tải
        wordList.postValue(null);

        apiService.getWordsForTopic(topicId).enqueue(new Callback<List<VocabWord>>() {
            @Override
            public void onResponse(@NonNull Call<List<VocabWord>> call, @NonNull Response<List<VocabWord>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    wordList.postValue(response.body());
                } else {
                    Log.e(TAG, "fetchWords error: " + response.code());
                    error.postValue("Failed to load words. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VocabWord>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchWords failure: ", t);
                error.postValue("Network error: " + t.getMessage());
            }
        });
    }
    // ❗️ KẾT THÚC CODE MỚI ❗️
}
