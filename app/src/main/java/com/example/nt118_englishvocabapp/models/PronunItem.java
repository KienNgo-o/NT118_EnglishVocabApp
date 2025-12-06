package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PronunItem {
    @SerializedName("word_id")
    private int wordId;
    @SerializedName("word_text")
    private String wordText;
    @SerializedName("Pronunciations")
    private List<Pronunciation> pronunciations;
    @SerializedName("Definitions")
    private List<Definition> definitions;

    public int getWordId() { return wordId; }
    public String getWordText() { return wordText; }
    public List<Pronunciation> getPronunciations() { return pronunciations; }
    public List<Definition> getDefinitions() { return definitions; }
}

