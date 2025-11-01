package com.example.nt118_englishvocabapp.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> todayQuote = new MutableLiveData<>();
    private final MutableLiveData<String> activeDaysText = new MutableLiveData<>();
    private final MutableLiveData<String> vocabProgress1 = new MutableLiveData<>();
    private final MutableLiveData<String> vocabProgress2 = new MutableLiveData<>();
    private final MutableLiveData<String> quizProgress1 = new MutableLiveData<>();
    private final MutableLiveData<String> quizProgress2 = new MutableLiveData<>();
    private final MutableLiveData<String> flashProgress = new MutableLiveData<>();

    // New: study days - list of abbreviations (e.g. "Mo","Tu") provided by backend
    private final MutableLiveData<List<String>> studyDays = new MutableLiveData<>();

    public HomeViewModel() {
        // Default sample values; replace these when wiring the backend
        todayQuote.setValue("\"Procrastination is like a credit card: it's a lot of fun until you get the bill.\"");
        activeDaysText.setValue("1 Active day");
        vocabProgress1.setValue("40/48");
        vocabProgress2.setValue("6/24");
        quizProgress1.setValue("40/48");
        quizProgress2.setValue("6/24");
        flashProgress.setValue("0/0");

        // Sample active study days (Mon, Tue, Fri) - backend can replace
        studyDays.setValue(Arrays.asList("Mo", "Tu", "Fr"));
    }

    public LiveData<String> getTodayQuote() {
        return todayQuote;
    }

    public LiveData<String> getActiveDaysText() {
        return activeDaysText;
    }

    public LiveData<String> getVocabProgress1() { return vocabProgress1; }
    public LiveData<String> getVocabProgress2() { return vocabProgress2; }
    public LiveData<String> getQuizProgress1() { return quizProgress1; }
    public LiveData<String> getQuizProgress2() { return quizProgress2; }
    public LiveData<String> getFlashProgress() { return flashProgress; }

    // New getter for study days
    public LiveData<List<String>> getStudyDays() { return studyDays; }

    // Setter helpers for backend updates
    public void setTodayQuote(String q) { todayQuote.setValue(q); }
    public void setActiveDaysText(String t) { activeDaysText.setValue(t); }
    public void setVocabProgress1(String p) { vocabProgress1.setValue(p); }
    public void setVocabProgress2(String p) { vocabProgress2.setValue(p); }
    public void setQuizProgress1(String p) { quizProgress1.setValue(p); }
    public void setQuizProgress2(String p) { quizProgress2.setValue(p); }
    public void setFlashProgress(String p) { flashProgress.setValue(p); }
    public void setStudyDays(List<String> days) { studyDays.setValue(days); }
}