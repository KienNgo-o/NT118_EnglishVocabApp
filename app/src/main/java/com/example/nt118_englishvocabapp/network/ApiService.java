package com.example.nt118_englishvocabapp.network;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.network.dto.RefreshRequest;
import com.example.nt118_englishvocabapp.network.dto.RefreshResponse;
import retrofit2.http.HTTP;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import retrofit2.http.Path;

import com.example.nt118_englishvocabapp.models.User;
import com.example.nt118_englishvocabapp.models.VocabWord;
import com.example.nt118_englishvocabapp.models.WordDetail;
import com.example.nt118_englishvocabapp.network.dto.DeleteAccountRequest;
import com.example.nt118_englishvocabapp.network.dto.SignInRequest;
import com.example.nt118_englishvocabapp.network.dto.SignInResponse;
import com.example.nt118_englishvocabapp.network.dto.SignOutRequest;
import com.example.nt118_englishvocabapp.network.dto.SignUpRequest;
import com.example.nt118_englishvocabapp.network.dto.UpdateProfileRequest;

import retrofit2.http.PATCH;
import com.example.nt118_englishvocabapp.models.QuizData;
import com.example.nt118_englishvocabapp.models.QuizSubmission;
import com.example.nt118_englishvocabapp.models.QuizResult;
public interface ApiService {

    @POST("/api/auth/signin")
    Call<SignInResponse> signIn(@Body SignInRequest request);

    @POST("/api/auth/refresh")
    Call<RefreshResponse> refresh(@Body RefreshRequest request);

    @POST("/api/auth/signout")
    Call<Void> signOut(@Body SignOutRequest request); // <Void> vì server chỉ trả về 204 No Content
    @POST("/api/auth/signup")
    Call<Void> signUp(@Body SignUpRequest request); // Server trả về 204 No Content, nên dùng <Void>
//    @GET("/api/users/me")
//    Call<User> getMe(); // Giả sử bạn có 1 lớp User.java
    @GET("api/topics")
    Call<List<Topic>> getAllTopics();

    /**
     * Lấy tất cả flashcards cho một chủ đề cụ thể (Màn hình 2)
     * (Token sẽ được AuthInterceptor tự động thêm vào)
     */
    @GET("api/topics/{id}/flashcards")
    Call<List<FlashcardItem>> getFlashcardsForTopic(
            @Path("id") int topicId
    );
    @GET("api/topics/{id}/words")
    Call<List<VocabWord>> getWordsForTopic(@Path("id") int topicId);
    @GET("api/words/{id}")
    Call<WordDetail> getWordDetails(@Path("id") int wordId);
    @HTTP(method = "DELETE", path = "/api/users/me", hasBody = true)
    Call<Void> deleteAccount(@Body DeleteAccountRequest request);
    @PATCH("/api/users/me")
    Call<User> updateUserProfile(@Body UpdateProfileRequest request);

    // 1. Lấy đề thi
    @GET("api/topics/{id}/quiz")
    Call<QuizData> getQuiz(@Path("id") int topicId);

    // 2. Nộp bài
    @POST("api/topics/{id}/quiz/submit")
    Call<QuizResult> submitQuiz(@Path("id") int topicId, @Body QuizSubmission submission);
}