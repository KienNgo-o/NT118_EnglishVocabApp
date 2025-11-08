package com.example.nt118_englishvocabapp.ui.vocab2;

public class Topic {
    private String word;
    private String wordType;
    private String definition;
    private boolean isFavorite = false; // new field to track favorite state

    public Topic(String word, String wordType, String definition) {
        this.word = word;
        this.wordType = wordType;
        this.definition = definition;
    }

    // New constructor that allows setting favorite initially
    public Topic(String word, String wordType, String definition, boolean isFavorite) {
        this.word = word;
        this.wordType = wordType;
        this.definition = definition;
        this.isFavorite = isFavorite;
    }

    public String getWord() {
        return word;
    }

    public String getWordType() {
        return wordType;
    }

    public String getDefinition() {
        return definition;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setWordType(String wordType) {
        this.wordType = wordType;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }
}
