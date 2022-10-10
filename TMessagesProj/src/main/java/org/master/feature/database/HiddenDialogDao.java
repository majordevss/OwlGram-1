package org.master.feature.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface  HiddenDialogDao {

    @Query("SELECT * FROM hidden_dialog")
    List<HiddenDialog> getAll();


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HiddenDialog category);

    @Delete
    void delete(HiddenDialog category);

    @Query("DELETE   FROM hidden_dialog")
    void deleteAll();
}
