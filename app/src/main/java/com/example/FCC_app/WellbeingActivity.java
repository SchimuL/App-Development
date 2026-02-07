package com.example.FCC_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class WellbeingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WellbeingValues";
    private String loggedInPlayerTag;

    private EditText hrvEditText, trainingTypeEditText, durationEditText;
    private SeekBar stressSeekBar, fatigueSeekBar, sorenessSeekBar, painSeekBar, rpeSeekBar;
    private TextView stressValue, fatigueValue, sorenessValue, painValue, rpeValue;
    private LineChart hooperLineChart, hrvLineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellbeing);

        loggedInPlayerTag = getIntent().getStringExtra("PLAYER_TAG");

        Toolbar toolbar = findViewById(R.id.wellbeing_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        hrvEditText = findViewById(R.id.edittext_hrv);
        stressSeekBar = findViewById(R.id.seekbar_stress); stressValue = findViewById(R.id.value_stress);
        fatigueSeekBar = findViewById(R.id.seekbar_fatigue); fatigueValue = findViewById(R.id.value_fatigue);
        sorenessSeekBar = findViewById(R.id.seekbar_soreness); sorenessValue = findViewById(R.id.value_soreness);
        painSeekBar = findViewById(R.id.seekbar_pain); painValue = findViewById(R.id.value_pain);
        rpeSeekBar = findViewById(R.id.seekbar_rpe); rpeValue = findViewById(R.id.value_rpe);
        trainingTypeEditText = findViewById(R.id.edittext_training_type);
        durationEditText = findViewById(R.id.edittext_duration);
        Button saveButton = findViewById(R.id.save_wellbeing_button);
        hooperLineChart = findViewById(R.id.hooper_line_chart);
        hrvLineChart = findViewById(R.id.hrv_line_chart);

        setupSeekBarListener(stressSeekBar, stressValue);
        setupSeekBarListener(fatigueSeekBar, fatigueValue);
        setupSeekBarListener(sorenessSeekBar, sorenessValue);
        setupSeekBarListener(painSeekBar, painValue);
        setupSeekBarListener(rpeSeekBar, rpeValue);

        if (loggedInPlayerTag != null) {
            populateTodaysValues();
            loadAndDisplayCharts();
        }

        saveButton.setOnClickListener(v -> {
            saveWellbeingData();
            loadAndDisplayCharts();
        });
    }

    private void populateTodaysValues() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String key = loggedInPlayerTag + "_" + todayDate;
        String data = prefs.getString(key, "0,1,1,1,1,0");
        String[] parts = data.split(",");

        int hrv = getSafeInt(parts, 0);
        if (hrv > 0) hrvEditText.setText(String.valueOf(hrv));

        stressSeekBar.setProgress(Math.max(0, getSafeInt(parts, 1) - 1));
        fatigueSeekBar.setProgress(Math.max(0, getSafeInt(parts, 2) - 1));
        sorenessSeekBar.setProgress(Math.max(0, getSafeInt(parts, 3) - 1));
        painSeekBar.setProgress(Math.max(0, getSafeInt(parts, 4) - 1));
    }

    private void saveWellbeingData() {
        if (loggedInPlayerTag == null) return;
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String key = loggedInPlayerTag + "_" + todayDate;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int rpe = rpeSeekBar.getProgress() + 1;
        int duration = 0;
        try { duration = Integer.parseInt(durationEditText.getText().toString()); } catch (NumberFormatException e) {}
        int newTrainingLoad = rpe * duration;

        int hrv = 0;
        try { hrv = Integer.parseInt(hrvEditText.getText().toString()); } catch (NumberFormatException e) {}

        int stress = stressSeekBar.getProgress() + 1;
        int fatigue = fatigueSeekBar.getProgress() + 1;
        int soreness = sorenessSeekBar.getProgress() + 1;
        int pain = painSeekBar.getProgress() + 1;

        String wellbeingData = String.format(Locale.US, "%d,%d,%d,%d,%d,%d", hrv, stress, fatigue, soreness, pain, newTrainingLoad);
        prefs.edit().putString(key, wellbeingData).apply();

        Toast.makeText(this, "Werte gespeichert!", Toast.LENGTH_SHORT).show();
    }

    private void loadAndDisplayCharts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        TreeMap<String, String> sortedData = new TreeMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -13);
        
        for (int i = 0; i < 14; i++) {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            String key = loggedInPlayerTag + "_" + dateStr;
            sortedData.put(dateStr, prefs.getString(key, "0,1,1,1,1,0"));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        updateHooperChart(sortedData);
        updateHrvChart(sortedData);
    }

    private void updateHooperChart(TreeMap<String, String> data) {
        List<String> dates = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM", Locale.GERMANY);
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            try {
                Date d = keyFormat.parse(entry.getKey());
                dates.add(displayFormat.format(d));
                String[] p = entry.getValue().split(",");
                int score = getSafeInt(p, 1) + getSafeInt(p, 2) + getSafeInt(p, 3) + getSafeInt(p, 4);
                entries.add(new Entry(i++, score));
            } catch (Exception e) {}
        }
        
        LineDataSet set = new LineDataSet(entries, "Hooper Score");
        set.setColor(Color.parseColor("#FFC107"));
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(3f);
        set.setCircleRadius(5f);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        
        configureChart(hooperLineChart, new LineData(set), dates);
    }

    private void updateHrvChart(TreeMap<String, String> data) {
        List<String> dates = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM", Locale.GERMANY);
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            try {
                Date d = keyFormat.parse(entry.getKey());
                dates.add(displayFormat.format(d));
                int hrv = getSafeInt(entry.getValue().split(","), 0);
                if (hrv > 0) entries.add(new Entry(i, hrv));
                i++;
            } catch (Exception e) {}
        }
        
        LineDataSet set = new LineDataSet(entries, "HRV");
        set.setColor(Color.CYAN);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(3f);
        set.setCircleRadius(5f);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        
        configureChart(hrvLineChart, new LineData(set), dates);
    }

    private void configureChart(LineChart chart, LineData data, List<String> dates) {
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(10, 10, 10, 20);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setLabelRotationAngle(-90f);
        xAxis.setGranularity(1f);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(Color.WHITE);
        left.setTextSize(11f);
        left.setGridColor(Color.parseColor("#33FFFFFF"));
        left.setDrawGridLines(true);

        chart.getAxisRight().setEnabled(false);
        Legend l = chart.getLegend();
        l.setTextColor(Color.WHITE);
        l.setForm(Legend.LegendForm.LINE);

        chart.setData(data);
        chart.animateX(1000);
        chart.invalidate();
    }

    private int getSafeInt(String[] parts, int index) {
        if (parts != null && index < parts.length) {
            try { return Integer.parseInt(parts[index]); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private void setupSeekBarListener(SeekBar seekBar, TextView valueTextView) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { valueTextView.setText(String.valueOf(progress + 1)); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}