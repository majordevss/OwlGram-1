package org.master.feature.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;


@Dao
public interface ContactChangeDao {

    @Query("select * from contact_change")
    List<ContactChange> getContactChagne();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContactChange contactChange);


    @Delete
    void deleteContactChange(ContactChange contactChange);

    @Query("delete  from contact_change")
    void clearAll();

}
