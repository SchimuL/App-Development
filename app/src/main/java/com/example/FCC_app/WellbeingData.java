package com.example.FCC_app;

import org.json.JSONException;
import org.json.JSONObject;

public class WellbeingData {

    // --- Fields for our data container ---
    private int hrv = 0;
    private int stress = 1;
    private int fatigue = 1;
    private int soreness = 1;
    private int pain = 1;
    private int trainingLoad = 0;

    // --- Constructors ---

    // Default constructor for a new, empty day
    public WellbeingData() {}

    // Constructor to create an object from a saved JSON string
    public WellbeingData(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            this.hrv = json.optInt("hrv", 0);
            this.stress = json.optInt("stress", 1);
            this.fatigue = json.optInt("fatigue", 1);
            this.soreness = json.optInt("soreness", 1);
            this.pain = json.optInt("pain", 1);
            this.trainingLoad = json.optInt("trainingLoad", 0);
        } catch (JSONException e) {
            // If JSON is malformed, we use default values
        }
    }

    // --- Methods to interact with the data ---

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

    // --- Getters and Setters for each field ---

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
