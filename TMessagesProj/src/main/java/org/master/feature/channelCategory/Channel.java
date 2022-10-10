package org.master.feature.channelCategory;

import com.google.gson.annotations.SerializedName;

public class Channel {

    @SerializedName("title")
    public String title;
    @SerializedName("image")
    public String image;
    @SerializedName("sub_count")
    public String  sub_count;

    @SerializedName("username")
    public String username;

    @SerializedName("about")
    public String about;

}

