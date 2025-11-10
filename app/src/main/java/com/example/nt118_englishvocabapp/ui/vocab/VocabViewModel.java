// ui/vocab/VocabViewModel.java
package com.example.nt118_englishvocabapp.ui.vocab;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.models.VocabWord; // 👈 THÊM IMPORT NÀY
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.List;

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
    // ❗️ KẾT THÚC CODE MỚI ❗️

    public VocabViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
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
                    topics.postValue(response.body());
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
