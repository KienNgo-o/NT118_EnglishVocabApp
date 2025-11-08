// ui/flashcard/FlashcardViewModel.java
package com.example.nt118_englishvocabapp.ui.flashcard;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.Definition; // 👈 THÊM
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.models.LearnableItem; // 👈 THÊM
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.ArrayList; // 👈 THÊM
import java.util.List;

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

    public FlashcardViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    // --- Getters cho Fragments ---
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
     * Sau đó làm phẳng dữ liệu thành List<LearnableItem>
     */
    public void fetchFlashcards(int topicId) {
        // Xóa dữ liệu cũ để tránh "nháy" khi đang load
        learnableList.postValue(null);

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
}
