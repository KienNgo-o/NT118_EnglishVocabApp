package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QuizData {
    @SerializedName("quiz_id")
    public int quizId;
    @SerializedName("title")
    public String title;
    @SerializedName("passing_score")
    public int passingScore;
    @SerializedName("Questions")
    public List<Question> questions;
    @SerializedName("duration_minutes")
    public int durationMinutes;

    // Helper class Question
    public static class Question {
        @SerializedName("question_id")
        public int questionId;
        @SerializedName("question_type")
        public String questionType; // "LISTEN_CHOOSE_IMG", "IMG_CHOOSE_TEXT", etc.
        @SerializedName("prompt")
        public String prompt;
        @SerializedName("image_url")
        public String imageUrl;
        @SerializedName("audio_url")
        public String audioUrl;

        @SerializedName("QuestionOptions")
        public List<Option> options;

        @SerializedName("MatchingPairs")
        public List<Pair> pairs;
    }

    // Helper class Option (Cho trắc nghiệm)
    public static class Option {
        @SerializedName("option_id")
        public int optionId;
        @SerializedName("option_text")
        public String optionText;
        @SerializedName("option_image_url")
        public String optionImageUrl;
    }

    // Helper class Pair (Cho nối từ)
    public static class Pair {
        @SerializedName("pair_id")
        public int pairId;
        @SerializedName("image_url")
        public String imageUrl;
        @SerializedName("word_text")
        public String wordText;

        // Field dùng cho UI (để biết user đã chọn hay chưa)
        public boolean isSelected = false;
        public boolean isMatched = false;
    }
}