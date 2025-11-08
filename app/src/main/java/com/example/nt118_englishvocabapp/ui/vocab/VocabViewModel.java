package com.example.nt118_englishvocabapp.ui.vocab;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VocabViewModel extends AndroidViewModel {

    private static final String TAG = "VocabViewModel";
    private final ApiService apiService;

    private final MutableLiveData<List<Topic>> topics = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public VocabViewModel(@NonNull Application application) {
        super(application);
        this.apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    public LiveData<List<Topic>> getTopics() { return topics; }
    public LiveData<String> getError() { return error; }

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
}