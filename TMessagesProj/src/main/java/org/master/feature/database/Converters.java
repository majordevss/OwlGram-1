package org.master.feature.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.master.feature.TelegramMessageUpdateManager;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class Converters {
    @TypeConverter
    public static ArrayList<Long> fromString(String value) {
        Type listType = new TypeToken<ArrayList<Long>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<Long> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }

    @TypeConverter
    public static ArrayList<HiddenDialog> fromHiddenDialogs(String value) {
        Type listType = new TypeToken<ArrayList<HiddenDialog>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String toHiddenDialogs(ArrayList<HiddenDialog> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }
    @TypeConverter
    public static ArrayList<TelegramMessageUpdateManager.UpdateUser> fromUpdate(String value) {
        Type listType = new TypeToken<ArrayList<TelegramMessageUpdateManager.UpdateUser>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String toUpdate(ArrayList<TelegramMessageUpdateManager.UpdateUser> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }


}
