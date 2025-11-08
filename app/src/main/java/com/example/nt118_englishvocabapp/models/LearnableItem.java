// models/LearnableItem.java
package com.example.nt118_englishvocabapp.models;

/**
 * Class bọc (wrapper) đại diện cho MỘT thẻ học duy nhất.
 * Nó kết hợp một từ (FlashcardItem) với MỘT trong các định nghĩa của nó.
 */
public class LearnableItem {

    public FlashcardItem word; // Từ ("Red")
    public Definition definition; // Một định nghĩa ("The color of blood")

    public LearnableItem(FlashcardItem word, Definition definition) {
        this.word = word;
        this.definition = definition;
    }
}