package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TrainerDashboardActivity extends AppCompatActivity {

    private enum Metric { TRAINING_LOAD, HOOPER, HRV }

    private String selectedTeam;
    private List<String> playerTagsInTeam = new ArrayList<>();
    private List<String> dateLabels = new ArrayList<>();

    private Spinner metricSpinner;
    private LineChart teamAverageChart;
    private TextView criticalPlayersTextView;
    private LinearLayout individualChartsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer_dashboard);

        selectedTeam = getIntent().getStringExtra("SELECTED_TEAM");

        Toolbar toolbar = findViewById(R.id.trainer_dashboard_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (selectedTeam != null) {
                getSupportActionBar().setTitle("Dashboard: " + selectedTeam);
            } else {
                getSupportActionBar().setTitle("Dashboard");
                Toast.makeText(this, "Fehler: Kein Team ausgewählt", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        metricSpinner = findViewById(R.id.spinner_dashboard_metric);
        teamAverageChart = findViewById(R.id.chart_team_average);
        criticalPlayersTextView = findViewById(R.id.textview_critical_players);
        individualChartsContainer = findViewById(R.id.container_individual_charts);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Trainingsbelastung & ACWR", "Hooper-Index", "HRV"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        metricSpinner.setAdapter(adapter);

        metricSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDashboard();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        dateLabels.clear();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -59);
        for (int i = 0; i < 60; i++) {
            dateLabels.add(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboard();
    }

    private void updateDashboard() {
        findPlayersInTeam();
        if (playerTagsInTeam.isEmpty()) {
            criticalPlayersTextView.setText("Keine Spieler in diesem Team gefunden.");
            teamAverageChart.clear();
            teamAverageChart.invalidate();
            individualChartsContainer.removeAllViews();
            return;
        }

        Metric selectedMetric = Metric.values()[metricSpinner.getSelectedItemPosition()];
        individualChartsContainer.removeAllViews();
        individualChartsContainer.setVisibility(View.VISIBLE);

        Map<String, TreeMap<String, String>> allPlayersData = loadAllDataForTeam();
        List<List<Float>> allPlayerMetricValues = new ArrayList<>();
        StringBuilder criticalText = new StringBuilder();

        for (String playerTag : playerTagsInTeam) {
            TreeMap<String, String> playerData = allPlayersData.get(playerTag);
            if (playerData == null) continue;

            addIndividualPlayerChart(playerTag, playerData, selectedMetric);
            allPlayerMetricValues.add(getMetricValuesForPlayer(playerData, selectedMetric));
            checkForCriticalIssues(playerTag, playerData, criticalText);
        }

        if (selectedMetric == Metric.TRAINING_LOAD) {
            updateAcwrTeamChart(allPlayerMetricValues);
        } else {
            updateTeamAverageChart(allPlayerMetricValues, selectedMetric);
        }

        if (criticalText.length() > 0) {
            criticalPlayersTextView.setText(criticalText.toString());
        } else {
            criticalPlayersTextView.setText("Keine kritischen Spieler identifiziert.");
        }
    }

    private void findPlayersInTeam() {
        playerTagsInTeam.clear();
        SharedPreferences profilePrefs = getSharedPreferences("UserProfileData", Context.MODE_PRIVATE);
        List<String> teams = Arrays.asList(getResources().getStringArray(R.array.mannschaften));
        int targetTeamIndex = teams.indexOf(selectedTeam);
        if (targetTeamIndex == -1) return;

        Set<String> allPlayerTags = new HashSet<>();
        for (String key : profilePrefs.getAll().keySet()) {
            if (key.endsWith("_name")) {
                 String playerTag = key.substring(0, key.length() - "_name".length());
                 if (!playerTag.isEmpty()) {
                    allPlayerTags.add(playerTag);
                 }
            }
        }

        for (String playerTag : allPlayerTags) {
            int playerTeamIndex = profilePrefs.getInt(playerTag + "_team", -1);
            if (playerTeamIndex == targetTeamIndex) {
                if(!playerTagsInTeam.contains(playerTag)) playerTagsInTeam.add(playerTag);
            }
        }
        Collections.sort(playerTagsInTeam);
    }

    private Map<String, TreeMap<String, String>> loadAllDataForTeam() {
        Map<String, TreeMap<String, String>> dataMap = new TreeMap<>();
        SharedPreferences wellbeingPrefs = getSharedPreferences("WellbeingValues", Context.MODE_PRIVATE);
        for (String playerTag : playerTagsInTeam) {
            TreeMap<String, String> playerData = new TreeMap<>();
            for (String date : dateLabels) {
                String key = playerTag + "_" + date;
                playerData.put(date, wellbeingPrefs.getString(key, "0,1,1,1,1,0"));
            }
            dataMap.put(playerTag, playerData);
        }
        return dataMap;
    }

    private List<Float> getMetricValuesForPlayer(TreeMap<String, String> playerData, Metric metric) {
        List<Float> values = new ArrayList<>();
        for (String date : dateLabels) {
            String data = playerData.get(date);
            values.add((float) extractMetricValue(data, metric));
        }
        return values;
    }

    private void updateAcwrTeamChart(List<List<Float>> allPlayerMetricValues) {
        List<Entry> loadEntries = new ArrayList<>();
        List<Entry> acwrEntries = new ArrayList<>();
        List<Float> teamDailyLoadValues = new ArrayList<>();

        for (int i = 0; i < dateLabels.size(); i++) {
            float dailyLoadSum = 0;
            int playerCount = 0;
            for (List<Float> playerValues : allPlayerMetricValues) {
                if(i < playerValues.size()) {
                    dailyLoadSum += playerValues.get(i);
                    playerCount++;
                }
            }
            float avgLoad = (playerCount > 0) ? dailyLoadSum / playerCount : 0;
            loadEntries.add(new Entry(i, avgLoad));
            teamDailyLoadValues.add(avgLoad);
        }

        for (int i = 30; i < teamDailyLoadValues.size(); i++) {
            float acuteLoad = 0; for(int j=0; j<7; j++) acuteLoad += teamDailyLoadValues.get(i-j);
            float chronicLoad = 0; for(int j=0; j<31; j++) chronicLoad += teamDailyLoadValues.get(i-j);
            if (chronicLoad > 0) acwrEntries.add(new Entry(i, (acuteLoad / 7.0f) / (chronicLoad / 31.0f)));
        }

        LineDataSet loadSet = new LineDataSet(loadEntries, "Team Avg Load");
        loadSet.setAxisDependency(YAxis.AxisDependency.LEFT); loadSet.setColor(Color.BLUE);
        LineDataSet acwrSet = new LineDataSet(acwrEntries, "Team ACWR");
        acwrSet.setAxisDependency(YAxis.AxisDependency.RIGHT); acwrSet.setColor(Color.MAGENTA);
        
        configureChart(teamAverageChart, new LineData(loadSet, acwrSet), true);
    }

    private void updateTeamAverageChart(List<List<Float>> allPlayerMetricValues, Metric metric) {
        List<Entry> teamAverageEntries = new ArrayList<>();
        for (int i = 0; i < dateLabels.size(); i++) {
            float dailySum = 0;
            int playerCount = 0;
            for (List<Float> playerValues : allPlayerMetricValues) {
                 if(i < playerValues.size()) {
                    float value = playerValues.get(i);
                    if (value > 0) { dailySum += value; playerCount++; }
                 }
            }
            if (playerCount > 0) teamAverageEntries.add(new Entry(i, dailySum / playerCount));
        }

        LineDataSet teamDataSet = new LineDataSet(teamAverageEntries, "Team-Schnitt");
        teamDataSet.setColor(Color.parseColor("#FFA000"));
        configureChart(teamAverageChart, new LineData(teamDataSet), false);
    }

    private void addIndividualPlayerChart(String playerTag, TreeMap<String, String> playerData, Metric metric) {
        View playerChartView = LayoutInflater.from(this).inflate(R.layout.list_item_player_chart, individualChartsContainer, false);
        TextView playerName = playerChartView.findViewById(R.id.textview_player_name);
        LineChart chart = playerChartView.findViewById(R.id.chart_individual_player);
        playerName.setText(playerTag);

        List<Entry> playerEntries = new ArrayList<>();
        for (int i = 0; i < dateLabels.size(); i++) {
            int value = extractMetricValue(playerData.get(dateLabels.get(i)), metric);
            playerEntries.add(new Entry(i, value));
        }

        LineDataSet playerDataSet = new LineDataSet(playerEntries, metric.name());
        configureChart(chart, new LineData(playerDataSet), false);

        individualChartsContainer.addView(playerChartView);
    }

    private void checkForCriticalIssues(String playerTag, TreeMap<String, String> playerData, StringBuilder criticalText) {
        List<String> issues = new ArrayList<>();

        // Rule 1: ACWR
        List<Integer> loads = new ArrayList<>();
        for(String data : playerData.values()) loads.add(getSafeInt(data.split(","), 5));
        int highAcwrStreak = 0;
        for (int i = 30; i < loads.size(); i++) {
            float acuteLoad = 0; for(int j=0; j<7; j++) acuteLoad += loads.get(i-j);
            float chronicLoad = 0; for(int j=0; j<31; j++) chronicLoad += loads.get(i-j);
            if (chronicLoad > 0 && (acuteLoad / 7.0f) / (chronicLoad / 31.0f) > 1.3) highAcwrStreak++; else highAcwrStreak = 0;
            if (highAcwrStreak > 2) { issues.add("ACWR zu hoch"); break; }
        }

        // Rule 2: HRV
        List<Integer> hrvHistory = new ArrayList<>();
        for(String data : playerData.values()) hrvHistory.add(getSafeInt(data.split(","), 0));
        int minHrv = Integer.MAX_VALUE;
        for (int i = Math.max(0, hrvHistory.size() - 28); i < hrvHistory.size(); i++){
             if(hrvHistory.get(i) > 0 && hrvHistory.get(i) < minHrv) minHrv = hrvHistory.get(i);
        }
        if (minHrv != Integer.MAX_VALUE) {
            float threshold = minHrv * 1.1f;
            int lowHrvStreak = 0;
            for (int i = hrvHistory.size() - 3; i < hrvHistory.size(); i++){
                if(i >= 0 && hrvHistory.get(i) > 0 && hrvHistory.get(i) <= threshold) lowHrvStreak++;
            }
            if(lowHrvStreak >= 3) issues.add("HRV kritisch niedrig (" + hrvHistory.get(hrvHistory.size()-1) + " / Min: " + minHrv + ")");
        }

        // Rule 3: Hooper
        String latestData = playerData.get(dateLabels.get(dateLabels.size() - 1));
        if (latestData != null) {
            String[] parts = latestData.split(",");
            int stress = getSafeInt(parts, 1), fatigue = getSafeInt(parts, 2), soreness = getSafeInt(parts, 3), pain = getSafeInt(parts, 4);
            if (stress >= 7 || fatigue >= 7 || soreness >= 7 || pain >= 7) {
                issues.add("Hooper-Wert >= 7");
            } else {
                List<Integer> hooperScores = new ArrayList<>();
                for (String data : playerData.values()) {
                    String[] dailyParts = data.split(",");
                    hooperScores.add(getSafeInt(dailyParts, 1) + getSafeInt(dailyParts, 2) + getSafeInt(dailyParts, 3) + getSafeInt(dailyParts, 4));
                }
                if (hooperScores.size() > 1) {
                    double mean = 0; for(int score : hooperScores) mean += score; mean /= hooperScores.size();
                    if (mean > 0) {
                        double stdDev = 0; for(int score : hooperScores) stdDev += Math.pow(score - mean, 2); 
                        stdDev = Math.sqrt(stdDev / hooperScores.size());
                        double threshold = mean + (0.65 * stdDev);
                        if ((stress + fatigue + soreness + pain) > threshold && threshold > 0) issues.add("Hooper statistisch auffällig");
                    }
                }
            }
        }

        if (!issues.isEmpty()) {
             criticalText.append("- ").append(playerTag).append(": ").append(joinStrings(", ", issues)).append("\n");
        }
    }

    private String joinStrings(CharSequence delimiter, List<String> elements) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String element : elements) {
            if (first) first = false; else sb.append(delimiter);
            sb.append(element);
        }
        return sb.toString();
    }

    private int extractMetricValue(String data, Metric metric) {
        if (data == null) return 0;
        String[] parts = data.split(",");
        switch (metric) {
            case HRV: return getSafeInt(parts, 0);
            case HOOPER: return getSafeInt(parts, 1) + getSafeInt(parts, 2) + getSafeInt(parts, 3) + getSafeInt(parts, 4);
            case TRAINING_LOAD: return getSafeInt(parts, 5);
            default: return 0;
        }
    }

    private int getSafeInt(String[] parts, int index) {
        if (parts != null && index < parts.length) {
            try { return Integer.parseInt(parts[index]); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private void configureChart(LineChart chart, LineData data, boolean enableRightAxis) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(enableRightAxis);
        
        chart.setData(data);
        chart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
