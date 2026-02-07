package com.example.FCC_app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PerformanceDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PerformanceData> performanceDataList);

    @Query("SELECT * FROM performance_data WHERE playerTag = :playerTag ORDER BY date DESC")
    List<PerformanceData> getPerformanceDataForPlayer(String playerTag);

    @Query("SELECT * FROM performance_data WHERE playerTag = :playerTag AND date = :date LIMIT 1")
    PerformanceData getPerformanceDataForPlayerOnDate(String playerTag, String date);

}
