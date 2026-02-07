package com.example.FCC_app;

import com.google.firebase.firestore.Exclude;

public class TrainingSlot {
    @Exclude
    public String id;

    public String date;         // yyyy-MM-dd
    
    // Trainer's availability window
    public String startTime;    // e.g. "09:00"
    public String endTime;      // e.g. "13:00"
    
    // Player's specific booking (set when PENDING/CONFIRMED)
    public String bookedStartTime;
    public String bookedEndTime;
    
    public String type;         // Gym / Platz
    public String duration;     // 60 / 90
    public String playerTag;
    public String phone;
    public String reason;
    public String status;       // AVAILABLE (Window), PENDING (Requested), CONFIRMED (Booked)

    public TrainingSlot() {
        // Required for Firestore
    }

    public TrainingSlot withId(String id) {
        this.id = id;
        return this;
    }
}