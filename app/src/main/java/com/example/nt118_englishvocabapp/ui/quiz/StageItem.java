package com.example.nt118_englishvocabapp.ui.quiz;

public class StageItem {
    private final boolean isStage;
    private final int stageNumber; // 1-based (only valid when isStage==true)
    private final boolean unlocked;
    private final String name;

    public StageItem(boolean isStage, int stageNumber, boolean unlocked, String name) {
        this.isStage = isStage;
        this.stageNumber = stageNumber;
        this.unlocked = unlocked;
        this.name = name;
    }

    public boolean isStage() { return isStage; }
    public int getStageNumber() { return stageNumber; }
    public boolean isUnlocked() { return unlocked; }
    public String getName() { return name; }
}

