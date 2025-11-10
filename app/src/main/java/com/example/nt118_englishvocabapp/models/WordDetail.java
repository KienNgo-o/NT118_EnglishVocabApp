// models/WordDetail.java
package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Model "mẹ" chứa TOÀN BỘ dữ liệu cho 3 tab
public class WordDetail {

    @SerializedName("word_id")
    private int wordId;
    @SerializedName("word_text")
    private String wordText;

    @SerializedName("Pronunciations")
    private List<Pronunciation> pronunciations; // 👈 Tái sử dụng Pronunciation.java
    @SerializedName("Definitions")
    private List<Definition> definitions; // 👈 Tái sử dụng Definition.java

    // --- Tab 2: Word's Forms ---
    @SerializedName("WordForms")
    private WordForms wordForms;

    // --- Tab 3: Synonyms/Antonyms ---
    @SerializedName("Synonyms")
    private List<RelatedWord> synonyms;
    @SerializedName("Antonyms")
    private List<RelatedWord> antonyms;

    // --- Getters ---
    public int getWordId() { return wordId; }
    public String getWordText() { return wordText; }
    public List<Pronunciation> getPronunciations() { return pronunciations; }
    public List<Definition> getDefinitions() { return definitions; }
    public WordForms getWordForms() { return wordForms; }
    public List<RelatedWord> getSynonyms() { return synonyms; }
    public List<RelatedWord> getAntonyms() { return antonyms; }
}

