package org.master.feature.database;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "contact_change")
public class ContactChange {

    @SerializedName("mask")
    public int mask;

    @PrimaryKey
    @SerializedName("user_id")
    public long user_id;

    @SerializedName("time")
    public long time;

}
