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

    // transient field populated client-side with number of vocab/flashcards in this topic
    private int wordCount = -1; // -1 means unknown/not yet loaded

    // No-arg constructor required for Gson / for creating copies
    public Topic() {}

    // Getters
    public int getTopicId() { return topicId; }
    public String getTopicName() { return topicName; }
    public String getDifficulty() { return difficulty; }
    public String getStatus() { return status; }

    // Word count accessor (client-side only)
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    // transient flag for whether the user has "saved" or favorited this topic
    private boolean saved = false; // client-side only; not from API

    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }

    // Create a copy of this Topic with a different wordCount (avoids mutating original instance)
    public Topic copyWithWordCount(int newWordCount) {
        Topic copy = new Topic();
        copy.topicId = this.topicId;
        copy.topicName = this.topicName;
        copy.difficulty = this.difficulty;
        copy.status = this.status;
        copy.wordCount = newWordCount;
        return copy;
    }
}