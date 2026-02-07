package com.example.FCC_app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PolarDataParser {

    private static final String TAG = "PolarDataParser";

    public static List<PlayerData> parseExcelFile(Context context, Uri fileUri) {
        List<PlayerData> playersData = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri)) {
            if (inputStream == null) {
                Log.e(TAG, "Unable to open input stream from URI");
                return playersData; // Return empty list
            }

            try (Workbook workbook = WorkbookFactory.create(inputStream)) {
                Sheet sheet = workbook.getSheetAt(0);

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    if (row.getCell(0) == null || row.getCell(0).getCellType() != CellType.NUMERIC) break;

                    String playerTag = generatePlayerTag(getStringCellValue(row, 1), (int) getNumericCellValue(row, 0));
                    String date = getDateCellValue(row.getCell(6));

                    // --- Heart Rate ---
                    double avgHr = getNumericCellValue(row, 9);
                    double maxHr = getNumericCellValue(row, 10);
                    double relMaxHr = getNumericCellValue(row, 13);
                    Map<String, Double> timeInHrZones = new HashMap<>();
                    timeInHrZones.put("Zone 1", getNumericCellValue(row, 14) * 24 * 60);
                    timeInHrZones.put("Zone 2", getNumericCellValue(row, 15) * 24 * 60);
                    timeInHrZones.put("Zone 3", getNumericCellValue(row, 16) * 24 * 60);
                    timeInHrZones.put("Zone 4", getNumericCellValue(row, 17) * 24 * 60);
                    timeInHrZones.put("Zone 5", getNumericCellValue(row, 18) * 24 * 60);

                    // --- Speed / Distance ---
                    double totalDistance = getNumericCellValue(row, 19);
                    double distPerMin = getNumericCellValue(row, 20);
                    double maxSpeed = getNumericCellValue(row, 21);
                    int sprints = (int) getNumericCellValue(row, 23);
                    Map<String, Double> distanceInSpeedZones = new HashMap<>();
                    distanceInSpeedZones.put("Zone 1", getNumericCellValue(row, 24));
                    distanceInSpeedZones.put("Zone 2", getNumericCellValue(row, 25));
                    distanceInSpeedZones.put("Zone 3", getNumericCellValue(row, 26));
                    distanceInSpeedZones.put("Zone 4", getNumericCellValue(row, 27));
                    distanceInSpeedZones.put("Zone 5", getNumericCellValue(row, 28));

                    // --- Accelerations/Decelerations ---
                    Map<String, Double> decelerations = new HashMap<>();
                    decelerations.put("Zone 4", getNumericCellValue(row, 34));
                    decelerations.put("Zone 5", getNumericCellValue(row, 33));
                    Map<String, Double> accelerations = new HashMap<>();
                    accelerations.put("Zone 1", getNumericCellValue(row, 39));
                    accelerations.put("Zone 2", getNumericCellValue(row, 40));

                    PlayerData player = new PlayerData(playerTag, date, avgHr, maxHr, totalDistance, sprints, maxSpeed, timeInHrZones, distanceInSpeedZones, relMaxHr, distPerMin, accelerations, decelerations);
                    playersData.add(player);
                }
            } // Workbook is auto-closed here
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Excel file", e);
            // Returning an empty list which the calling activity will handle as a failure.
        } // InputStream is auto-closed here

        return playersData;
    }

    private static String generatePlayerTag(String fullName, int jerseyNumber) {
        if (fullName == null || fullName.trim().isEmpty()) return String.valueOf(jerseyNumber);
        String[] names = fullName.trim().split("\\s+");
        char first = names[0].isEmpty() ? 'X' : names[0].charAt(0);
        char last = names.length > 1 && !names[names.length - 1].isEmpty() ? names[names.length - 1].charAt(0) : ' ';
        return (String.valueOf(first) + (last == ' ' ? "" : last) + jerseyNumber).toUpperCase();
    }

    private static String getDateCellValue(Cell cell) {
        if (cell == null) return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cell.getDateCellValue());
        }
        return cell.toString();
    }

    private static double getNumericCellValue(Row row, int idx) {
        Cell cell = row.getCell(idx);
        return (cell != null && cell.getCellType() == CellType.NUMERIC) ? cell.getNumericCellValue() : 0.0;
    }

    private static String getStringCellValue(Row row, int idx) {
        Cell cell = row.getCell(idx);
        return (cell != null && cell.getCellType() == CellType.STRING) ? cell.getStringCellValue() : "";
    }
}
