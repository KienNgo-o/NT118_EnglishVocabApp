package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;

public class Topic {

    @SerializedName("topic_id")
    private int topicId;

    @SerializedName("topic_name")
    private String topicName;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("status")
    private String status;

    // Getters
    public int getTopicId() { return topicId; }
    public String getTopicName() { return topicName; }
    public String getDifficulty() { return difficulty; }
    public String getStatus() { return status; }
}