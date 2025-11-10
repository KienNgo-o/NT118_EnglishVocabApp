package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model cho JSON: { "word_id": 1, "word_text": "red" }
 */
public class RelatedWord {
    @SerializedName("word_id")
    public int wordId;
    @SerializedName("word_text")
    public String wordText;
}
