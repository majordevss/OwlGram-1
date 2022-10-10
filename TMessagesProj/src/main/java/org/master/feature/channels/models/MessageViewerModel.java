package org.master.feature.channels.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public  class MessageViewerModel extends TaskModel{

//        @SerializedName("vc")
//        public String stopViewCount;

        @SerializedName("un")
        public String username;

        @SerializedName("chId")
        public long chatId;

        @SerializedName("mid")
        public ArrayList<Integer> messageId;

        @SerializedName("type")
        public String type;

    }
