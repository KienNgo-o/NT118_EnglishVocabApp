package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
public class POS {
    @SerializedName("pos_name")
    private String posName; // "noun"
    // ... Thêm Getters ...
    public String getPosName() { return posName; }
}