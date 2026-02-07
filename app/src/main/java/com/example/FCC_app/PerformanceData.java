package com.example.FCC_app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Map;

@Entity(tableName = "performance_data")
@TypeConverters(Converters.class)
public class PerformanceData {

    @PrimaryKey(autoGenerate = true)
    public int id;

    private String playerTag;
    private String date;

    // Existing fields
    private double averageHeartRate;
    private double maxHeartRate;
    private double totalDistance;
    private int sprints;
    private double maxSpeed;
    private Map<String, Double> timeInHrZones;
    private Map<String, Double> distanceInSpeedZones;

    // New fields
    private double relativeMaxHeartRate;
    private double distancePerMinute;
    private Map<String, Double> accelerations;
    private Map<String, Double> decelerations;

    // Constructor
    public PerformanceData(String playerTag, String date, double averageHeartRate, double maxHeartRate, double totalDistance, int sprints, double maxSpeed, Map<String, Double> timeInHrZones, Map<String, Double> distanceInSpeedZones, double relativeMaxHeartRate, double distancePerMinute, Map<String, Double> accelerations, Map<String, Double> decelerations) {
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


    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public String getPlayerTag() {
        return playerTag;
    }

    public void setPlayerTag(String playerTag) {
        this.playerTag = playerTag;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(double averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    public double getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(double maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getSprints() {
        return sprints;
    }

    public void setSprints(int sprints) {
        this.sprints = sprints;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Map<String, Double> getTimeInHrZones() {
        return timeInHrZones;
    }

    public void setTimeInHrZones(Map<String, Double> timeInHrZones) {
        this.timeInHrZones = timeInHrZones;
    }

    public Map<String, Double> getDistanceInSpeedZones() {
        return distanceInSpeedZones;
    }

    public void setDistanceInSpeedZones(Map<String, Double> distanceInSpeedZones) {
        this.distanceInSpeedZones = distanceInSpeedZones;
    }

    public double getRelativeMaxHeartRate() {
        return relativeMaxHeartRate;
    }

    public void setRelativeMaxHeartRate(double relativeMaxHeartRate) {
        this.relativeMaxHeartRate = relativeMaxHeartRate;
    }

    public double getDistancePerMinute() {
        return distancePerMinute;
    }

    public void setDistancePerMinute(double distancePerMinute) {
        this.distancePerMinute = distancePerMinute;
    }

    public Map<String, Double> getAccelerations() {
        return accelerations;
    }

    public void setAccelerations(Map<String, Double> accelerations) {
        this.accelerations = accelerations;
    }

    public Map<String, Double> getDecelerations() {
        return decelerations;
    }

    public void setDecelerations(Map<String, Double> decelerations) {
        this.decelerations = decelerations;
    }
}
