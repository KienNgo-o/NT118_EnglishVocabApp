package com.example.nt118_englishvocabapp.ui.vocab;

public class TopicCard {
    public String title;
    public String difficulty;
    public int wordsCount;
    public int imageResId;
    public boolean saved = false; // default not saved
    public int topicId = -1; // backend topic id if available

    // Existing constructor (kept for compatibility)
    public TopicCard(String title, String difficulty, int wordsCount, int imageResId) {
        this.title = title;
        this.difficulty = difficulty;
        this.wordsCount = wordsCount;
        this.imageResId = imageResId;
    }

    // New constructor that includes topicId
    public TopicCard(int topicId, String title, String difficulty, int wordsCount, int imageResId) {
        this.topicId = topicId;
        this.title = title;
        this.difficulty = difficulty;
        this.wordsCount = wordsCount;
        this.imageResId = imageResId;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public int getTopicId() { return topicId; }
}
