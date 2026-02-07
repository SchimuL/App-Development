package com.example.FCC_app;

import java.util.Map;

// Data class to hold all performance metrics for a single player from a single session.
public class PlayerData {

    private final String playerTag;
    private final String date;

    // Existing fields
    private final double averageHeartRate;
    private final double maxHeartRate;
    private final double totalDistance;
    private final int sprints;
    private final double maxSpeed;
    private final Map<String, Double> timeInHrZones;
    private final Map<String, Double> distanceInSpeedZones;

    // New fields from user feedback
    private final double relativeMaxHeartRate;
    private final double distancePerMinute;
    private final Map<String, Double> accelerations;
    private final Map<String, Double> decelerations;


    public PlayerData(String playerTag, String date, double averageHeartRate, double maxHeartRate, double totalDistance, int sprints, double maxSpeed, Map<String, Double> timeInHrZones, Map<String, Double> distanceInSpeedZones, double relativeMaxHeartRate, double distancePerMinute, Map<String, Double> accelerations, Map<String, Double> decelerations) {
        this.playerTag = playerTag;
        this.date = date;
        this.averageHeartRate = averageHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.totalDistance = totalDistance;
        this.sprints = sprints;
        this.maxSpeed = maxSpeed;
        this.timeInHrZones = timeInHrZones;
        this.distanceInSpeedZones = distanceInSpeedZones;
        this.relativeMaxHeartRate = relativeMaxHeartRate;
        this.distancePerMinute = distancePerMinute;
        this.accelerations = accelerations;
        this.decelerations = decelerations;
    }

    // Getters for all fields

    public String getPlayerTag() {
        return playerTag;
    }

    public String getDate() {
        return date;
    }

    public double getAverageHeartRate() {
        return averageHeartRate;
    }

    public double getMaxHeartRate() {
        return maxHeartRate;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public int getSprints() {
        return sprints;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public Map<String, Double> getTimeInHrZones() {
        return timeInHrZones;
    }

    public Map<String, Double> getDistanceInSpeedZones() {
        return distanceInSpeedZones;
    }

    public double getRelativeMaxHeartRate() {
        return relativeMaxHeartRate;
    }

    public double getDistancePerMinute() {
        return distancePerMinute;
    }

    public Map<String, Double> getAccelerations() {
        return accelerations;
    }

    public Map<String, Double> getDecelerations() {
        return decelerations;
    }
}
