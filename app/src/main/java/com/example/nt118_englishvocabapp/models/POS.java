// models/POS.java
package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
public class POS {
    @SerializedName("pos_name")
    private String posName; // "noun"

    // ❗️ THÊM TRƯỜNG MỚI:
    @SerializedName("pos_name_vie")
    private String posNameVie; // "Danh từ"

    // Getters
    public String getPosName() { return posName; }

    // ❗️ THÊM GETTER MỚI:
    public String getPosNameVie() { return posNameVie; }
}