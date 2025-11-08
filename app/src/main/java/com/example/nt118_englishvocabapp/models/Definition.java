// models/Definition.java
package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Definition {
    @SerializedName("definition_text")
    private String definitionText;
    @SerializedName("translation_text")
    private String translationText;

    // ❗️ SỬA Ở ĐÂY: từ "POS" thành "PO"
    @SerializedName("PO")
    private POS pos;

    @SerializedName("Examples")
    private List<Example> examples;

    // Getters
    public String getDefinitionText() { return definitionText; }
    public String getTranslationText() { return translationText; }
    public POS getPos() { return pos; }
    public List<Example> getExamples() { return examples; }
}