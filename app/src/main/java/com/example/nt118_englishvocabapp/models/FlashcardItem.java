package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FlashcardItem {

    @SerializedName("word_id")
    private int wordId;

    @SerializedName("word_text")
    private String wordText; // "Watermelon"

    @SerializedName("Pronunciations")
    private List<Pronunciation> pronunciations;

    @SerializedName("Definitions")
    private List<Definition> definitions;

    // Getters
    public int getWordId() { return wordId; }
    public String getWordText() { return wordText; }
    public List<Pronunciation> getPronunciations() { return pronunciations; }
    public List<Definition> getDefinitions() { return definitions; }
}