package com.example.nt118_englishvocabapp.ui.vocab2;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.Definition;
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel used by VocabFragment2 to fetch words for a topic (vocab list).
 * It calls the same backend endpoint used by flashcards (GET api/topics/{id}/flashcards)
 * and maps the result (List<FlashcardItem>) into a simple UI-friendly List<Topic>.
 *
 * Preparing this now allows `VocabFragment2` to switch from local sample data to backend data
 * when the API is ready.
 */
public class Vocab2ViewModel extends AndroidViewModel {

    private static final String TAG = "Vocab2ViewModel";
    private final ApiService apiService;

    private final MutableLiveData<List<com.example.nt118_englishvocabapp.ui.vocab2.Topic>> topics = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public Vocab2ViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    public LiveData<List<com.example.nt118_englishvocabapp.ui.vocab2.Topic>> getTopics() { return topics; }
    public LiveData<String> getError() { return error; }

    /**
     * Fetch words (flashcards) for a given topic id and map to UI Topic objects.
     */
    public void fetchWords(int topicId) {
        // Clear previous data to indicate loading state (fragment can interpret null as loading)
        topics.postValue(null);

        apiService.getFlashcardsForTopic(topicId).enqueue(new Callback<List<FlashcardItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<FlashcardItem>> call, @NonNull Response<List<FlashcardItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FlashcardItem> items = response.body();
                    List<com.example.nt118_englishvocabapp.ui.vocab2.Topic> mapped = new ArrayList<>();

                    for (FlashcardItem f : items) {
                        String word = f.getWordText() != null ? f.getWordText() : "";
                        String wordType = "";
                        String definition = "";

                        if (f.getDefinitions() != null && !f.getDefinitions().isEmpty()) {
                            Definition d = f.getDefinitions().get(0);
                            if (d != null) {
                                if (d.getPos() != null && d.getPos().getPosName() != null) {
                                    wordType = d.getPos().getPosName();
                                }
                                if (d.getDefinitionText() != null) definition = d.getDefinitionText();
                            }
                        }

                        com.example.nt118_englishvocabapp.ui.vocab2.Topic t = new com.example.nt118_englishvocabapp.ui.vocab2.Topic(word, wordType, definition);
                        mapped.add(t);
                    }

                    topics.postValue(mapped);
                } else {
                    Log.e(TAG, "fetchWords error: " + response.code());
                    error.postValue("Failed to load words. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FlashcardItem>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchWords failure: ", t);
                error.postValue("Network error: " + t.getMessage());
            }
        });
    }
}

