package org.master.feature.channelCategory;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Category {

    @SerializedName("key")
    public String key;
    @SerializedName("image")
    public String image;
    @SerializedName("title")
    public String title;
    @SerializedName("channels")
    public ArrayList<Channel> channels;


}
