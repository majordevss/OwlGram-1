package org.master.feature.database;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.master.feature.TelegramMessageUpdateManager;

import java.util.List;

@Dao
public abstract class   AppDao {


    @Transaction
    @Query("SELECT * FROM update_table")
    public abstract List<TelegramMessageUpdateManager.UserWithUpdate> getUserWithUpdates();

    @Transaction
    @Insert(onConflict  = OnConflictStrategy.REPLACE)
    public abstract long insertUser(TelegramMessageUpdateManager.UpdateUser course);

    @Insert
   public abstract void insertUpdates(List<TelegramMessageUpdateManager.UpdateObject> students);


    @Query("delete  from update_table")
    public abstract void clearAllUpdate();

    @Query("delete  from update_user_table")
    public abstract void clearAllUserUpdate();
}
