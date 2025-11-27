package com.example.nt118_englishvocabapp.ui.quiz;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.nt118_englishvocabapp.models.*;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizViewModel extends AndroidViewModel {
    private final ApiService apiService;

    // Data đề thi
    private final MutableLiveData<QuizData> quizData = new MutableLiveData<>();
    // Index câu hỏi hiện tại (0, 1, 2...)
    private final MutableLiveData<Integer> currentQuestionIndex = new MutableLiveData<>(0);
    // Thời gian còn lại (giây)
    private final MutableLiveData<Integer> timeRemaining = new MutableLiveData<>();
    // Kết quả nộp bài
    private final MutableLiveData<QuizResult> quizResult = new MutableLiveData<>();

    // Lưu câu trả lời của user (Key: questionId, Value: Answer object)
    private Map<Integer, QuizSubmission.Answer> userAnswers = new HashMap<>();
    private CountDownTimer timer;

    public QuizViewModel(@NonNull Application application) {
        super(application);
        apiService = RetrofitClient.getApiService(application.getApplicationContext());
    }

    // Getters
    public LiveData<QuizData> getQuizData() { return quizData; }
    public LiveData<Integer> getCurrentQuestionIndex() { return currentQuestionIndex; }
    public LiveData<Integer> getTimeRemaining() { return timeRemaining; }
    public LiveData<QuizResult> getQuizResult() { return quizResult; }

    // 1. Tải đề thi
    public void fetchQuiz(int topicId) {
        // 1. RESET TRẠNG THÁI CŨ (Dùng setValue để reset NGAY LẬP TỨC)
        currentQuestionIndex.setValue(0); // Reset về câu 0 ngay
        userAnswers.clear();
        quizResult.setValue(null);        // Xóa kết quả cũ ngay (để không bị tự động thoát)
        timeRemaining.setValue(null);

        if (timer != null) timer.cancel();

        // 2. GỌI API
        apiService.getQuiz(topicId).enqueue(new Callback<QuizData>() {
            @Override
            public void onResponse(Call<QuizData> call, Response<QuizData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    QuizData data = response.body();
                    quizData.postValue(data);
                    int minutes = (data.durationMinutes > 0) ? data.durationMinutes : 10;
                    startTimer(minutes * 60);
                }
            }
            @Override
            public void onFailure(Call<QuizData> call, Throwable t) { /* Handle Error */ }
        });
    }

    // 2. Timer
    private void startTimer(int seconds) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(seconds * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
                timeRemaining.postValue((int) (millisUntilFinished / 1000));
            }
            public void onFinish() {
                // Hết giờ -> Tự động nộp bài (Logic này làm sau)
                timeRemaining.postValue(0);
            }
        }.start();
    }

    // 3. Điều hướng câu hỏi
    public void nextQuestion() {
        QuizData data = quizData.getValue();
        if (data != null && currentQuestionIndex.getValue() < data.questions.size() - 1) {
            currentQuestionIndex.postValue(currentQuestionIndex.getValue() + 1);
        }
    }

    public void prevQuestion() {
        if (currentQuestionIndex.getValue() > 0) {
            currentQuestionIndex.postValue(currentQuestionIndex.getValue() - 1);
        }
    }

    // 4. Lưu câu trả lời tạm thời
    public void saveAnswer(int questionId, QuizSubmission.Answer answer) {
        userAnswers.put(questionId, answer);
    }

    public QuizSubmission.Answer getAnswer(int questionId) {
        return userAnswers.get(questionId);
    }

    // 5. Nộp bài
    public void submitQuiz(int topicId) {
        if (timer != null) timer.cancel();

        List<QuizSubmission.Answer> answerList = new ArrayList<>(userAnswers.values());
        QuizSubmission submission = new QuizSubmission(answerList);

        apiService.submitQuiz(topicId, submission).enqueue(new Callback<QuizResult>() {
            @Override
            public void onResponse(Call<QuizResult> call, Response<QuizResult> response) {
                if (response.isSuccessful()) {
                    quizResult.postValue(response.body());
                }
            }
            @Override
            public void onFailure(Call<QuizResult> call, Throwable t) { /* Handle error */ }
        });
    }
}