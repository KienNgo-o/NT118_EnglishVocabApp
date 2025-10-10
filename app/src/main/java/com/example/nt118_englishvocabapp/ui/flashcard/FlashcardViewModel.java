package com.example.nt118_englishvocabapp.ui.flashcard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FlashcardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public FlashcardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Flashcard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}