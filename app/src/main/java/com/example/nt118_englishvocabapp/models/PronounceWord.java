package com.example.nt118_englishvocabapp.models;

public class PronounceWord {
    private int wordId;
    private String wordText;
    private String primaryDefinition;
    private String phoneticSpelling;

    public PronounceWord(int wordId, String wordText, String primaryDefinition) {
        this.wordId = wordId;
        this.wordText = wordText;
        this.primaryDefinition = primaryDefinition;
        this.phoneticSpelling = null;
    }

    public int getWordId() { return wordId; }
    public String getWordText() { return wordText; }
    public String getPrimaryDefinition() { return primaryDefinition; }
    public String getPhoneticSpelling() { return phoneticSpelling; }
    public void setPhoneticSpelling(String phoneticSpelling) { this.phoneticSpelling = phoneticSpelling; }
}

