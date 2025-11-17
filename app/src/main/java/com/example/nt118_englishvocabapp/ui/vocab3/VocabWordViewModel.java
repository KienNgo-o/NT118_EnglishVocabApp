// ui/vocab3/VocabWordViewModel.java
package com.example.nt118_englishvocabapp.ui.vocab3;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.WordDetail;
import com.example.nt118_englishvocabapp.network.ApiService;

import com.example.nt118_englishvocabapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VocabWordViewModel extends AndroidViewModel {

    private static final String TAG = "VocabWordViewModel";
    private final ApiService apiService;

    // Một LiveData duy nhất chứa TẤT CẢ dữ liệu chi tiết
    private final MutableLiveData<WordDetail> wordDetail = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private int currentWordId = -1;

    public VocabWordViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    // --- Getters ---
    public LiveData<WordDetail> getWordDetail() { return wordDetail; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * Gọi API 3: Lấy chi tiết từ vựng
     */
    public void fetchWordDetails(int wordId) {
        // Tránh gọi lại API nếu đã có dữ liệu
        if (wordId == currentWordId && wordDetail.getValue() != null) {
            return;
        }

        currentWordId = wordId;
        isLoading.postValue(true);
        wordDetail.postValue(null); // Xóa dữ liệu cũ

        apiService.getWordDetails(wordId).enqueue(new Callback<WordDetail>() {
            @Override
            public void onResponse(@NonNull Call<WordDetail> call, @NonNull Response<WordDetail> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    wordDetail.postValue(response.body());
                } else {
                    Log.e(TAG, "fetchWordDetails error: " + response.code());
                    error.postValue("Failed to load details. Code: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<WordDetail> call, @NonNull Throwable t) {
                isLoading.postValue(false);
                Log.e(TAG, "fetchWordDetails failure: ", t);
                error.postValue("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Gọi API 4: Thêm/Xóa Bookmark
     */


    // Xóa hàm setWordDetails cũ

    // Getter để các Fragment khác có thể lấy wordId hiện tại (để truyền lại khi chuyển tab)
    public int getCurrentWordId() {
        return currentWordId;
    }
}