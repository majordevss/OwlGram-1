package org.master.feature.database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;


@Dao
public interface CategoryDao{

    @Query("SELECT * FROM category order by `order`")
    List<Category> getAll();


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    @Delete
    void delete(Category category);

    @Query("DELETE   FROM category")
    void deleteAll();
}
