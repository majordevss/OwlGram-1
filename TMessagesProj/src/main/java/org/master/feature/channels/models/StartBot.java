package org.master.feature.channels.models;

import com.google.gson.annotations.SerializedName;

public  class StartBot{

        @SerializedName("username")
        public String username;

        @SerializedName("bothash")
        public String botHash;
    }