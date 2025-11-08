// models/Pronunciation.java
package com.example.nt118_englishvocabapp.models;

import com.google.gson.annotations.SerializedName;
public class Pronunciation {
    @SerializedName("phonetic_spelling")
    private String phoneticSpelling;
    @SerializedName("audio_file_url")
    private String audioFileUrl;
    @SerializedName("region")
    private String region;

    // ❗️ THÊM CÁC GETTERS:
    public String getPhoneticSpelling() {
        return phoneticSpelling;
    }

    public String getAudioFileUrl() {
        return audioFileUrl;
    }

    public String getRegion() {
        return region;
    }
}