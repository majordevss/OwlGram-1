package org.master.feature.database;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;

@Entity
public  class Category{

    @PrimaryKey
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "order")
    public int order;

    @ColumnInfo(name = "dialogs")
    public ArrayList<Long> dialogs;

    @ColumnInfo(name = "locked")
    public boolean locked;

    @ColumnInfo(name = "hash")
    public String hash;
}