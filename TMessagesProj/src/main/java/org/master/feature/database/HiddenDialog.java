package org.master.feature.database;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hidden_dialog")
public class HiddenDialog {

    @PrimaryKey()
    public long dialog_id;

}
