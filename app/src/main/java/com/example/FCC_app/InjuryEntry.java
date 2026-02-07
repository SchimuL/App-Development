package com.example.FCC_app;

// A simple data class to hold all information about a single injury entry.
public class InjuryEntry {

    final String playerName;
    final String bodyPart;
    final String date;
    final int painLevel;
    final String diagnosis;
    final String description;
    final String trigger;
    final String afterExercise;
    final String mechanism;

    public InjuryEntry(String playerName, String bodyPart, String date, int painLevel, String diagnosis, String description, String trigger, String afterExercise, String mechanism) {
        this.playerName = playerName;
        this.bodyPart = bodyPart;
        this.date = date;
        this.painLevel = painLevel;
        this.diagnosis = diagnosis;
        this.description = description;
        this.trigger = trigger;
        this.afterExercise = afterExercise;
        this.mechanism = mechanism;
    }
}
