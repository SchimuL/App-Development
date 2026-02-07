package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Manages loading and saving of WellbeingData.
 * This class also contains the nested data model class, WellbeingData.
 */
public class WellbeingRepository {

    private static final String PREFS_NAME = "WellbeingValues";
    private SharedPreferences prefs;

    public WellbeingRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveData(String playerTag, String date, WellbeingData data) {
        String key = playerTag + "_" + date;
        prefs.edit().putString(key, data.toJsonString()).apply();
    }

    public WellbeingData loadDataForDay(String playerTag, String date) {
        String key = playerTag + "_" + date;
        String json = prefs.getString(key, null);
        return new WellbeingData(json); // The constructor handles null gracefully
    }

    public TreeMap<String, WellbeingData> loadDataForDateRange(String playerTag, int days) {
        TreeMap<String, WellbeingData> results = new TreeMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1));

        for (int i = 0; i < days; i++) {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            results.put(dateStr, loadDataForDay(playerTag, dateStr));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return results;
    }

    // --- Nested Data Model Class ---
    public static class WellbeingData {
        private int hrv = 0;
        private int stress = 1;
        private int fatigue = 1;
        private int soreness = 1;
        private int pain = 1;
        private int trainingLoad = 0;

        // Constructor for a new day or from malformed data
        public WellbeingData(String jsonString) {
            if (jsonString == null) return; // Use default values
            try {
                JSONObject json = new JSONObject(jsonString);
                this.hrv = json.optInt("hrv", 0);
                this.stress = json.optInt("stress", 1);
                this.fatigue = json.optInt("fatigue", 1);
                this.soreness = json.optInt("soreness", 1);
                this.pain = json.optInt("pain", 1);
                this.trainingLoad = json.optInt("trainingLoad", 0);
            } catch (JSONException e) {
                // Use default values if JSON is invalid
            }
        }

        public String toJsonString() {
            JSONObject json = new JSONObject();
            try {
                json.put("hrv", hrv);
                json.put("stress", stress);
                json.put("fatigue", fatigue);
                json.put("soreness", soreness);
                json.put("pain", pain);
                json.put("trainingLoad", trainingLoad);
                return json.toString();
            } catch (JSONException e) {
                return "{}";
            }
        }

        // Getters and Setters
        public int getHrv() { return hrv; }
        public void setHrv(int hrv) { this.hrv = hrv; }
        public int getStress() { return stress; }
        public void setStress(int stress) { this.stress = stress; }
        public int getFatigue() { return fatigue; }
        public void setFatigue(int fatigue) { this.fatigue = fatigue; }
        public int getSoreness() { return soreness; }
        public void setSoreness(int soreness) { this.soreness = soreness; }
        public int getPain() { return pain; }
        public void setPain(int pain) { this.pain = pain; }
        public int getTrainingLoad() { return trainingLoad; }
        public void addTrainingLoad(int additionalLoad) { this.trainingLoad += additionalLoad; }
    }
}
