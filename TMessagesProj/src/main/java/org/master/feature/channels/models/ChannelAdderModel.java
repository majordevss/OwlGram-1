package org.master.feature.channels.models;

import com.google.gson.annotations.SerializedName;

public  class ChannelAdderModel extends TaskModel {

        @SerializedName("jc")
        public String stopJoinCount;

        @SerializedName("un")
        public String username;

        @SerializedName("ac")
        public boolean active;

        @SerializedName("chId")
        public String chatId;

        @SerializedName("co")
        public String code;

        @SerializedName("type")
        public String type;

    }
