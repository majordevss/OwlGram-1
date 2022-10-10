package org.master.plus.games;

import com.google.gson.annotations.SerializedName;

public class GameCategory {

    @SerializedName("value")
    public String value;

    @SerializedName("key")
    public String key;

    public int local_id;
}
