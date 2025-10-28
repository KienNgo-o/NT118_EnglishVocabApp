package com.example.nt118_englishvocabapp.ui.vocab2;

public class Topic {
    private String word;
    private String wordType;
    private String definition;

    public Topic(String word, String wordType, String definition) {
        this.word = word;
        this.wordType = wordType;
        this.definition = definition;
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
