// models/VocabWord.java
package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;

/**
 * Đây là Model (POJO) đại diện cho một từ vựng
 * trong danh sách (Màn hình 2 - image_7ec790.png)
 * Khớp với kết quả trả về của API 2 (getWordsForTopic)
 */
public class VocabWord {

    @SerializedName("word_id")
    private int wordId;

    @SerializedName("word_text")
    private String wordText; // Ví dụ: "cat"

    @SerializedName("primary_definition")
    private String primaryDefinition; // Ví dụ: "A small domesticated..."

    // (Chúng ta đã bỏ "primary_pos" theo yêu cầu của bạn)

    // --- Getters ---
    public int getWordId() {
        return wordId;
    }

    public String getWordText() {
        return wordText;
    }

    public String getPrimaryDefinition() {
        return primaryDefinition;
    }
}