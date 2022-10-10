package org.master.feature.channels.models;

import com.google.gson.annotations.SerializedName;

public  class ReactionMessageModel extends TaskModel {

        @SerializedName("mid")
        public int messageId;

        @SerializedName("reaction")
        public String reaction;

        @SerializedName("chat_id")
        public long chat_id;

        @SerializedName("count")
        public int count;

        @SerializedName("username")
        public String username;
    }