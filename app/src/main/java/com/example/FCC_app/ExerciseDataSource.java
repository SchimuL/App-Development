package com.example.FCC_app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExerciseDataSource {

    // Helper class to hold all details for a single exercise
    public static class ExerciseDetails {
        private final String videoName;
        private final String defaultNote;

        ExerciseDetails(String videoName, String defaultNote) {
            this.videoName = videoName;
            this.defaultNote = defaultNote;
        }

        public String getVideoName() {
            return videoName;
        }

        public String getDefaultNote() {
            return defaultNote;
        }
    }

    private static final Map<String, ExerciseDetails> exerciseMap = new LinkedHashMap<>();

    static {
        // Format: exerciseName, new ExerciseDetails(videoFileName, "Default Note")
        exerciseMap.put("Fußkreisen", new ExerciseDetails("fusskreisen", "Langsam und kontrolliert in beide Richtungen kreisen."));
        exerciseMap.put("Isometrisches Anspannen", new ExerciseDetails("", "Spannung für 15 Sekunden halten, dann lösen."));
        exerciseMap.put("Wadenheben", new ExerciseDetails("wadenheben", "Beim Absenken die Ferse nicht den Boden berühren lassen."));
        exerciseMap.put("Einbeinstand", new ExerciseDetails("", "Blick auf einen festen Punkt richten, um das Gleichgewicht zu halten."));
        exerciseMap.put("Kniebeugen", new ExerciseDetails("", "Rücken gerade halten, Knie nicht über die Fußspitzen schieben."));
        exerciseMap.put("Liegestütze (auf Knien)", new ExerciseDetails("", "Körperspannung halten, den gesamten Körper absenken."));
        exerciseMap.put("Klimmzüge", new ExerciseDetails("", "Aus dem Rücken ziehen, nicht nur aus den Armen."));
        exerciseMap.put("Kreuzheben", new ExerciseDetails("", "Rücken absolut gerade halten, Bewegung aus der Hüfte einleiten."));
        // --- ADD ALL NEW EXERCISES HERE ---
    }

    /**
     * Returns a list of all exercise names.
     */
    public static List<String> getAllExerciseNames() {
        return new ArrayList<>(exerciseMap.keySet());
    }

    /**
     * Returns the video file name for a given exercise.
     */
    public static String getVideoForExercise(String exerciseName) {
        ExerciseDetails details = exerciseMap.get(exerciseName);
        return (details != null) ? details.getVideoName() : "";
    }

    /**
     * Returns the default note for a given exercise.
     */
    public static String getDefaultNoteForExercise(String exerciseName) {
        ExerciseDetails details = exerciseMap.get(exerciseName);
        return (details != null) ? details.getDefaultNote() : "";
    }
}
