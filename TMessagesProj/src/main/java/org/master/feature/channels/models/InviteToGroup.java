package org.master.feature.channels.models;

import com.google.gson.annotations.SerializedName;

public  class InviteToGroup extends TaskModel{

        @SerializedName("username")
        public String username;

        @SerializedName("stop_count")
        public int stop_count;

        @SerializedName("user_count")
        public int user_count;
    }
