package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;

public class PronunGradeResponse {
    @SerializedName("success")
    private boolean success;
    @SerializedName("data")
    private PronunGradeData data;

    public boolean isSuccess() { return success; }
    public PronunGradeData getData() { return data; }

    public static class PronunGradeData {
        @SerializedName("word_id")
        private int wordId;
        @SerializedName("word_text")
        private String wordText;
        @SerializedName("details")
        private PronunDetails details;
        @SerializedName("feedback")
        private String feedback;
        @SerializedName("score")
        private double score;

        public int getWordId() { return wordId; }
        public String getWordText() { return wordText; }
        public PronunDetails getDetails() { return details; }
        public String getFeedback() { return feedback; }
        public double getScore() { return score; }
    }

    public static class PronunDetails {
        @SerializedName("accuracy_score")
        private double accuracyScore;
        @SerializedName("detected_text")
        private String detectedText;
        @SerializedName("intonation_score")
        private double intonationScore;

        public double getAccuracyScore() { return accuracyScore; }
        public String getDetectedText() { return detectedText; }
        public double getIntonationScore() { return intonationScore; }
    }
}

