// ui/flashcard/FlashcardViewModel.java
package com.example.nt118_englishvocabapp.ui.flashcard;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardViewModel extends AndroidViewModel {

    private final ApiService apiService;
    private static final String TAG = "FlashcardViewModel";

    // LiveData cho Màn hình 1 (Danh sách chủ đề)
    private final MutableLiveData<List<Topic>> topicList = new MutableLiveData<>();
    // LiveData cho Màn hình 2 (Danh sách thẻ)
    private final MutableLiveData<List<FlashcardItem>> flashcardList = new MutableLiveData<>();
    // LiveData cho thông báo lỗi
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public FlashcardViewModel(@NonNull Application application) {
        super(application);
        // Lấy ApiService từ RetrofitClient
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    // --- Getters cho Fragments ---
    public LiveData<List<Topic>> getTopics() { return topicList; }
    public LiveData<List<FlashcardItem>> getFlashcards() { return flashcardList; }
    public LiveData<String> getError() { return error; }

    // --- Logic gọi API ---

    /**
     * Gọi API 1: Lấy tất cả chủ đề
     */
    public void fetchTopics() {

        apiService.getAllTopics().enqueue(new Callback<List<Topic>>() {

            @Override
            public void onResponse(@NonNull Call<List<Topic>> call, @NonNull Response<List<Topic>> response) {
                if (response.isSuccessful()) {
                    topicList.postValue(response.body());
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

    /**
     * Gọi API 2: Lấy flashcards cho một chủ đề
     */
    public void fetchFlashcards(int topicId) {
        flashcardList.postValue(null);
        apiService.getFlashcardsForTopic(topicId).enqueue(new Callback<List<FlashcardItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<FlashcardItem>> call, @NonNull Response<List<FlashcardItem>> response) {
                if (response.isSuccessful()) {
                    flashcardList.postValue(response.body());
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
}