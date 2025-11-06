package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
public class Example {
    @SerializedName("example_sentence")
    private String exampleSentence;
    @SerializedName("translation_sentence")
    private String translationSentence;
    // ... Thêm Getters ...
    public String getExampleSentence() { return exampleSentence; }
    public String getTranslationSentence() { return translationSentence; }
}