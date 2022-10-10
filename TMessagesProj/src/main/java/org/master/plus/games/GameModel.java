package org.master.plus.games;

import com.google.gson.annotations.SerializedName;

import org.master.feature.database.Category;

import java.util.ArrayList;

public class GameModel {

    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("link")
    public String link;

    @SerializedName("photo")
    public String photo;

    @SerializedName("tags")
    public ArrayList<String> tags;

    @SerializedName("category")
    public Category category;

    @SerializedName("created_at")
    public String created_at;

}
