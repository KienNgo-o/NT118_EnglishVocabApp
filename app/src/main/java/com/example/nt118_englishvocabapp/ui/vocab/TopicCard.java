package com.example.nt118_englishvocabapp.ui.vocab;

public class TopicCard {
    public String title;
    public String difficulty;
    public int wordsCount;
    public int imageResId;

    public TopicCard(String title, String difficulty, int wordsCount, int imageResId) {
        this.title = title;
        this.difficulty = difficulty;
        this.wordsCount = wordsCount;
        this.imageResId = imageResId;
    }
}
