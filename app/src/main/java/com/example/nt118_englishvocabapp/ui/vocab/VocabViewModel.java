package com.example.nt118_englishvocabapp.ui.vocab;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class VocabViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public VocabViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Vocab fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}