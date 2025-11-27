package com.example.nt118_englishvocabapp.models;
import com.google.gson.annotations.SerializedName;

public class QuizResult {
    @SerializedName("score")
    public int score;
    @SerializedName("passed")
    public boolean passed;
    @SerializedName("user_points")
    public int userPoints;
    @SerializedName("total_possible_points")
    public int totalPossiblePoints;
    @SerializedName("is_next_topic_unlocked")
    public boolean isNextTopicUnlocked;
}