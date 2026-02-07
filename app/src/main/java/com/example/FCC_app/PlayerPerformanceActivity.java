package com.example.FCC_app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart.ScatterShape;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerPerformanceActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private String playerTag;
    private List<PerformanceData> performanceHistory;

    private TextView tvSessionDate, tvTotalDistance, tvSprints, tvMaxSpeed;
    private LineChart chartTrend;
    private CombinedChart chartHrZones, chartSpeedZones;
    private HorizontalBarChart chartAccelDecel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_performance);

        playerTag = getIntent().getStringExtra("PLAYER_TAG");
        if (playerTag == null) {
            Toast.makeText(this, "Fehler: Kein Spieler-Tag übergeben", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.performance_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Leistungsdaten: " + playerTag);
        }

        // Find Views
        tvSessionDate = findViewById(R.id.tv_session_date);
        tvTotalDistance = findViewById(R.id.tv_total_distance);
        tvSprints = findViewById(R.id.tv_sprints);
        tvMaxSpeed = findViewById(R.id.tv_max_speed);
        chartTrend = findViewById(R.id.chart_trend);
        chartHrZones = findViewById(R.id.chart_hr_zones);
        chartSpeedZones = findViewById(R.id.chart_speed_zones);
        chartAccelDecel = findViewById(R.id.chart_accel_decel);

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            performanceHistory = db.performanceDataDao().getPerformanceDataForPlayer(playerTag);

            if (performanceHistory != null && !performanceHistory.isEmpty()) {
                runOnUiThread(this::populateUi);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Keine Leistungsdaten für diesen Spieler gefunden", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void populateUi() {
        PerformanceData latestData = performanceHistory.get(0);
        List<PerformanceData> last5Sessions = performanceHistory.stream().limit(5).collect(Collectors.toList());

        double avgDistance = last5Sessions.stream().mapToDouble(PerformanceData::getTotalDistance).average().orElse(0);
        double avgSprints = last5Sessions.stream().mapToDouble(PerformanceData::getSprints).average().orElse(0);
        double avgMaxSpeed = last5Sessions.stream().mapToDouble(PerformanceData::getMaxSpeed).average().orElse(0);

        tvSessionDate.setText(String.format("Daten vom: %s", latestData.getDate()));
        tvTotalDistance.setText(String.format(Locale.GERMAN, "Gesamtdistanz: %.0f m (Ø %.0f m)", latestData.getTotalDistance(), avgDistance));
        tvSprints.setText(String.format(Locale.GERMAN, "Sprints: %d (Ø %.0f)", latestData.getSprints(), avgSprints));
        tvMaxSpeed.setText(String.format(Locale.GERMAN, "Max. Speed: %.1f km/h (Ø %.1f km/h)", latestData.getMaxSpeed(), avgMaxSpeed));

        setupTrendChart();
        setupHrZoneChart(latestData);
        setupSpeedZoneChart(latestData);
        setupAccelDecelChart(latestData);
    }

    private void setupTrendChart() {
        List<Entry> distanceEntries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        List<PerformanceData> reversedHistory = new ArrayList<>(performanceHistory.stream().limit(5).collect(Collectors.toList()));
        Collections.reverse(reversedHistory);

        for (int i = 0; i < reversedHistory.size(); i++) {
            PerformanceData data = reversedHistory.get(i);
            distanceEntries.add(new Entry(i, (float) data.getTotalDistance()));
            dates.add(data.getDate().substring(5)); // MM-DD
        }

        LineDataSet distanceSet = new LineDataSet(distanceEntries, "Distanz (m)");
        styleLineDataSet(distanceSet);

        chartTrend.setData(new LineData(distanceSet));
        configureChart(chartTrend, dates);
        chartTrend.invalidate();
    }

    private void setupHrZoneChart(PerformanceData data) {
        CombinedData combinedData = new CombinedData();
        combinedData.setData(generateHrBarData(data));
        combinedData.setData(generateHrScatterData(data));

        chartHrZones.setData(combinedData);
        configureCombinedChart(chartHrZones);
        chartHrZones.invalidate();
    }

    private BarData generateHrBarData(PerformanceData data) {
        Map<String, Double> zones = data.getTimeInHrZones();
        BarEntry entry = new BarEntry(0.5f, new float[]{
                getZoneValue(zones, "Zone 1"), getZoneValue(zones, "Zone 2"),
                getZoneValue(zones, "Zone 3"), getZoneValue(zones, "Zone 4"),
                getZoneValue(zones, "Zone 5")
        });
        BarDataSet set = new BarDataSet(Collections.singletonList(entry), "");
        set.setColors(getMatlabColors());
        set.setStackLabels(new String[]{"50-59%", "60-69%", "70-79%", "80-89%", "90-100%"});
        return new BarData(set);
    }

    private ScatterData generateHrScatterData(PerformanceData data) {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0.5f, (float) data.getRelativeMaxHeartRate()));
        ScatterDataSet set = new ScatterDataSet(entries, "Max HF (%)");
        set.setColor(Color.BLACK);
        set.setScatterShape(ScatterShape.X);
        set.setScatterShapeSize(15f);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return new ScatterData(set);
    }

    private void setupSpeedZoneChart(PerformanceData data) {
        CombinedData combinedData = new CombinedData();
        combinedData.setData(generateSpeedBarData(data));
        combinedData.setData(generateSpeedScatterData(data));

        chartSpeedZones.setData(combinedData);
        configureCombinedChart(chartSpeedZones);
        chartSpeedZones.invalidate();
    }

    private BarData generateSpeedBarData(PerformanceData data) {
        Map<String, Double> zones = data.getDistanceInSpeedZones();
        BarEntry entry = new BarEntry(0.5f, new float[]{
                getZoneValue(zones, "Zone 1"), getZoneValue(zones, "Zone 2"),
                getZoneValue(zones, "Zone 3"), getZoneValue(zones, "Zone 4"),
                getZoneValue(zones, "Zone 5")
        });
        BarDataSet set = new BarDataSet(Collections.singletonList(entry), "");
        set.setColors(getMatlabColors());
        set.setStackLabels(new String[]{"<7.2", "7.2-14.4", "14.4-19.8", "19.8-25.2", ">25.2"});
        return new BarData(set);
    }

    private ScatterData generateSpeedScatterData(PerformanceData data) {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0.5f, (float) data.getDistancePerMinute()));
        ScatterDataSet set = new ScatterDataSet(entries, "Distanz/min");
        set.setColor(Color.DKGRAY);
        set.setScatterShape(ScatterShape.CIRCLE);
        set.setScatterShapeSize(15f);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return new ScatterData(set);
    }

    private void setupAccelDecelChart(PerformanceData data) {
        Map<String, Double> accels = data.getAccelerations();
        Map<String, Double> decels = data.getDecelerations();

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, getZoneValue(decels, "Zone 5") + getZoneValue(decels, "Zone 4")));
        entries.add(new BarEntry(1, getZoneValue(accels, "Zone 2") + getZoneValue(accels, "Zone 1")));

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(Color.RED, Color.BLUE);

        chartAccelDecel.setData(new BarData(set));
        chartAccelDecel.getXAxis().setValueFormatter(new IndexAxisValueFormatter(Arrays.asList("Entschl.", "Beschl.")));
        chartAccelDecel.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartAccelDecel.getDescription().setEnabled(false);
        chartAccelDecel.setOnChartValueSelectedListener(this);
        chartAccelDecel.invalidate();
    }

    private float getZoneValue(Map<String, Double> zones, String zoneKey) {
        if (zones == null) {
            return 0f;
        }
        Double value = zones.get(zoneKey);
        return (value != null) ? value.floatValue() : 0f;
    }

    // --- Chart Helpers ---
    private void styleLineDataSet(LineDataSet set) {
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
    }

    private void configureChart(LineChart chart, List<String> xAxisLabels) {
        chart.getDescription().setEnabled(false);
        chart.setOnChartValueSelectedListener(this);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        xAxis.setGranularity(1f);
    }

    private void configureCombinedChart(CombinedChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.SCATTER});
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setAxisMinimum(0f);
        chart.setOnChartValueSelectedListener(this);
        chart.getXAxis().setDrawLabels(false);
    }

    private int[] getMatlabColors() {
        return new int[]{Color.rgb(127, 255, 0), Color.rgb(255, 255, 0), Color.rgb(255, 191, 0), Color.rgb(255, 127, 0), Color.rgb(255, 0, 0)};
    }

    // --- Interactivity ---
    @Override
    public void onValueSelected(Entry e, Highlight h) {
        String valueText;
        if (h.getStackIndex() >= 0) { // Stacked Bar Chart
            BarEntry barEntry = (BarEntry) e;
            valueText = String.format(Locale.GERMAN, "%.1f", barEntry.getYVals()[h.getStackIndex()]);
        } else { // Line or Scatter Chart
            valueText = String.format(Locale.GERMAN, "%.1f", e.getY());
        }
        Toast.makeText(this, "Ausgewählter Wert: " + valueText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {}

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }
}
