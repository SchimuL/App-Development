package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ChartActivity extends AppCompatActivity {

    public static final String EXTRA_EXERCISE_NAME = "EXERCISE_NAME";
    public static final String EXTRA_PLAYER_TAG = "PLAYER_TAG";

    private LineChart lineChart;
    private String exerciseName;
    private String playerTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        Toolbar toolbar = findViewById(R.id.chart_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        exerciseName = getIntent().getStringExtra(EXTRA_EXERCISE_NAME);
        playerTag = getIntent().getStringExtra(EXTRA_PLAYER_TAG);

        TextView chartTitle = findViewById(R.id.chart_title_text);
        lineChart = findViewById(R.id.progress_chart);
        lineChart.setHardwareAccelerationEnabled(true);
        lineChart.setNoDataTextColor(Color.WHITE);

        if (exerciseName == null || playerTag == null) {
            Toast.makeText(this, "Fehler: Übung oder Spieler nicht gefunden", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        chartTitle.setText("Fortschritt: " + exerciseName);
        loadChartDataAsync();
    }

    private void loadChartDataAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Entry> chartEntries = new ArrayList<>();
            SharedPreferences prefs = getSharedPreferences("WorkoutLog", Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getKey().startsWith(playerTag + "_")) {
                    try {
                        JSONObject workoutJson = new JSONObject((String) entry.getValue());
                        long dateMillis = workoutJson.getLong("date");
                        JSONArray exercisesArray = workoutJson.getJSONArray("exercises");

                        for (int i = 0; i < exercisesArray.length(); i++) {
                            JSONObject exerciseJson = exercisesArray.getJSONObject(i);
                            if (exerciseJson.getString("name").equals(exerciseName)) {
                                String weightStr = exerciseJson.getString("weight").replace(",", ".");
                                float weight = Float.parseFloat(weightStr);
                                if (weight > 0) chartEntries.add(new Entry(dateMillis, weight));
                                break; 
                            }
                        }
                    } catch (JSONException | NumberFormatException e) {}
                }
            }

            Collections.sort(chartEntries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
            final List<Entry> finalEntries = chartEntries;

            runOnUiThread(() -> {
                if (finalEntries.size() < 2) {
                    lineChart.setNoDataText("Zu wenig Daten vorhanden.");
                    lineChart.invalidate();
                } else {
                    setupChart(finalEntries);
                }
            });
        });
    }

    private void setupChart(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Gewicht (kg)");
        dataSet.setColor(Color.parseColor("#FFC107")); // Club Yellow
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.parseColor("#121212"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGridColor(Color.parseColor("#33FFFFFF"));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.GERMANY);
            @Override public String getAxisLabel(float value, AxisBase axis) { return sdf.format(new Date((long) value)); }
        });

        YAxis left = lineChart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setGridColor(Color.parseColor("#33FFFFFF"));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(Color.WHITE);
        lineChart.getDescription().setEnabled(false);
        
        lineChart.setData(new LineData(dataSet));
        lineChart.animateX(800);
        lineChart.invalidate(); 
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}