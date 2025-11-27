package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QuizSubmission {
    @SerializedName("answers")
    private List<Answer> answers;

    public QuizSubmission(List<Answer> answers) {
        this.answers = answers;
    }

    public static class Answer {
        @SerializedName("question_id")
        public int questionId;

        @SerializedName("selected_option_id")
        public Integer selectedOptionId; // Có thể null

        @SerializedName("text_input")
        public String textInput; // Có thể null

        @SerializedName("pairs")
        public List<PairSubmission> pairs; // Có thể null

        // Constructor cho trắc nghiệm
        public Answer(int qId, int optId) {
            this.questionId = qId;
            this.selectedOptionId = optId;
        }
        // Constructor cho điền từ
        public Answer(int qId, String text) {
            this.questionId = qId;
            this.textInput = text;
        }
        // Constructor cho nối từ
        public Answer(int qId, List<PairSubmission> pairs) {
            this.questionId = qId;
            this.pairs = pairs;
        }
    }

    public static class PairSubmission {
        @SerializedName("image_url")
        public String imageUrl;
        @SerializedName("word_text")
        public String wordText;

        public PairSubmission(String img, String txt) {
            this.imageUrl = img;
            this.wordText = txt;
        }
    }
}