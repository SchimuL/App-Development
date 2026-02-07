package com.example.FCC_app;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class Converters {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromMap(Map<String, Double> map) {
        if (map == null) {
            return null;
        }
        return gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, Double> toMap(String json) {
        if (json == null) {
            return Collections.emptyMap();
        }
        Type mapType = new TypeToken<Map<String, Double>>() {}.getType();
        return gson.fromJson(json, mapType);
    }
}
