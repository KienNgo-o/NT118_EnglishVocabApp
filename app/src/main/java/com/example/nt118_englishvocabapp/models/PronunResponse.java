package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PronunResponse {
    @SerializedName("topic_id")
    private String topicId;
    @SerializedName("total_words")
    private int totalWords;
    @SerializedName("data")
    private List<PronunItem> data;

    public String getTopicId() { return topicId; }
    public int getTotalWords() { return totalWords; }
    public List<PronunItem> getData() { return data; }
}

