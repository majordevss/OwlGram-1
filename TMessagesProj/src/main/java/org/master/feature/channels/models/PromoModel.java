package org.master.feature.channels.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public  class PromoModel extends TaskModel{

        @SerializedName("url")
        public String url;

        @SerializedName("exclude")
        public ArrayList<Long> exclude;
    }
