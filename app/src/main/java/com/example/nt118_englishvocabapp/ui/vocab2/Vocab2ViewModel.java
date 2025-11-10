// ui/vocab2/Vocab2ViewModel.java
package com.example.nt118_englishvocabapp.ui.vocab2;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// ❗️ THAY ĐỔI IMPORT
import com.example.nt118_englishvocabapp.models.VocabWord; // 👈 Sửa
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Vocab2ViewModel extends AndroidViewModel {

    private static final String TAG = "Vocab2ViewModel";
    private final ApiService apiService;

    // ❗️ THAY ĐỔI: Dùng Model mới
    private final MutableLiveData<List<VocabWord>> wordList = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public Vocab2ViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    // ❗️ THAY ĐỔI: Sửa Getter
    public LiveData<List<VocabWord>> getWordList() { return wordList; }
    public LiveData<String> getError() { return error; }

    /**
     * ❗️ THAY ĐỔI: Gọi API 2 (getWordsForTopic)
     */
    public void fetchWords(int topicId) {
        // Báo hiệu đang tải
        wordList.postValue(null);

        // ❗️ SỬA: Gọi API 2
        apiService.getWordsForTopic(topicId).enqueue(new Callback<List<VocabWord>>() {
            @Override
            public void onResponse(@NonNull Call<List<VocabWord>> call, @NonNull Response<List<VocabWord>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // API đã trả về đúng định dạng, không cần map (ánh xạ)
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
}