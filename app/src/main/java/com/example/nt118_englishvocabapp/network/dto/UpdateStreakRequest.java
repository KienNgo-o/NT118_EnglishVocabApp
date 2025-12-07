package com.example.nt118_englishvocabapp.network.dto;

public class UpdateStreakRequest {
    private int streak;

    public UpdateStreakRequest(int streak) {
        this.streak = streak;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }
}
