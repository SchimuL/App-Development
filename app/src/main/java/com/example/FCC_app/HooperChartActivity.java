package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HooperChartActivity extends AppCompatActivity {

    private LineChart lineChart;
    private String loggedInPlayerTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hooper_chart);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.hooper_chart_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        lineChart = findViewById(R.id.hooper_line_chart);

        if (loggedInPlayerTag != null) {
            loadAndDisplayChartData();
        }
    }

    private void loadAndDisplayChartData() {
        SharedPreferences prefs = getSharedPreferences("HooperValues", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        // Use TreeMap to automatically sort entries by date
        Map<String, Integer> dailyScores = new TreeMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(loggedInPlayerTag + "_")) {
                String dateStr = key.substring(loggedInPlayerTag.length() + 1);
                String[] values = entry.getValue().toString().split(",");
                int totalScore = 0;
                for (String value : values) {
                    try {
                        totalScore += Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // Ignore malformed data
                    }
                }
                dailyScores.put(dateStr, totalScore);
            }
        }

        if (dailyScores.isEmpty()) return; // No data to display

        List<Entry> dailyEntries = new ArrayList<>();
        List<Entry> movingAverageEntries = new ArrayList<>();
        List<String> dates = new ArrayList<>(dailyScores.keySet());
        List<Integer> scores = new ArrayList<>(dailyScores.values());

        for (int i = 0; i < scores.size(); i++) {
            dailyEntries.add(new Entry(i, scores.get(i)));

            // Calculate 7-day moving average
            if (i >= 6) {
                int sum = 0;
                for (int j = 0; j < 7; j++) {
                    sum += scores.get(i - j);
                }
                float average = (float) sum / 7.0f;
                movingAverageEntries.add(new Entry(i, average));
            }
        }

        LineDataSet dailyDataSet = new LineDataSet(dailyEntries, "Täglicher Hooper-Wert");
        dailyDataSet.setColor(Color.BLUE);
        dailyDataSet.setCircleColor(Color.BLUE);

        LineDataSet movingAverageDataSet = new LineDataSet(movingAverageEntries, "7-Tage-Schnitt");
        movingAverageDataSet.setColor(Color.RED);
        movingAverageDataSet.setDrawCircles(false);

        LineData lineData = new LineData(dailyDataSet, movingAverageDataSet);

        // Setup X-Axis to show dates
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        lineChart.getDescription().setEnabled(false);
        lineChart.setData(lineData);
        lineChart.invalidate(); // Refresh the chart
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}