package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model cho JSON: { "noun": [...], "verb": [...] }
 */
public class WordForms {
    @SerializedName("noun")
    public List<RelatedWord> noun;
    @SerializedName("verb")
    public List<RelatedWord> verb;
    @SerializedName("adjective")
    public List<RelatedWord> adjective;
    @SerializedName("adverb")
    public List<RelatedWord> adverb;
}
