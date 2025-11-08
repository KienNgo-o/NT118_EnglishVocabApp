package com.example.nt118_englishvocabapp.ui.vocab3;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Small ViewModel to host a single word's details for VocabFragment3/4/5.
 * Currently acts as an in-memory holder. When backend is ready, add API calls
 * to fetch word details and post values into the LiveData below.
 */
public class VocabWordViewModel extends AndroidViewModel {

    private final MutableLiveData<String> word = new MutableLiveData<>();
    private final MutableLiveData<String> wordType = new MutableLiveData<>();
    private final MutableLiveData<String> definition = new MutableLiveData<>();

    public VocabWordViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getWord() { return word; }
    public LiveData<String> getWordType() { return wordType; }
    public LiveData<String> getDefinition() { return definition; }

    // Called by fragments to seed values (or by ViewModel after network fetch)
    public void setWordDetails(String w, String type, String def) {
        word.postValue(w);
        wordType.postValue(type);
        definition.postValue(def);
    }
}
