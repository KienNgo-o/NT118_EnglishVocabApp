package com.example.nt118_englishvocabapp.ui.vocab2;

// ❗️ File này đã được nâng cấp để chứa 'wordId'
public class Topic {
    private int wordId; // 👈 THÊM
    private String word;
    private String wordType;
    private String definition;
    private boolean isFavorite = false;

    // ❗️ SỬA: Constructor cũ
    public Topic(String word, String wordType, String definition) {
        this.wordId = -1; // Đặt ID mặc định
        this.word = word;
        this.wordType = wordType;
        this.definition = definition;
    }

    // Constructor cũ (giữ lại)
    public Topic(String word, String wordType, String definition, boolean isFavorite) {
        this.wordId = -1; // Đặt ID mặc định
        this.word = word;
        this.wordType = wordType;
        this.definition = definition;
        this.isFavorite = isFavorite;
    }

    // ❗️ THÊM: Constructor mới mà VocabFragment2 cần
    public Topic(int wordId, String word, String wordType, String definition) {
        this.wordId = wordId;
        this.word = word;
        this.wordType = wordType;
        this.definition = definition;
    }

    // ❗️ THÊM: Getter mới
    public int getWordId() {
        return wordId;
    }

    // --- Getters/Setters cũ ---
    public String getWord() { return word; }
    public String getWordType() { return wordType; }
    public String getDefinition() { return definition; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setWord(String word) { this.word = word; }
    public void setWordType(String wordType) { this.wordType = wordType; }
    public void setDefinition(String definition) { this.definition = definition; }
}