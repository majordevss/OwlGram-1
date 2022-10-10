package org.plus.database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * collection of all application dao
 */
public  class AppDao {


    @Dao
    public static abstract class ChannelFeedModelDao{

        @Query("SELECT * FROM channel_feed_model")
        public  abstract List<TableModels.ChannelFeedModel> getChannelFeeds();

        @Query("SELECT * FROM channel_feed_model where id = :id limit 1")
        public  abstract List<TableModels.ChannelFeedModel> getChannelFeedById(int id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract void insert(TableModels.ChannelFeedModel localFolder);

        @Update()
        public abstract void update(TableModels.ChannelFeedModel localFolder);

        @Delete
        public abstract void delete(TableModels.ChannelFeedModel draft);

        @Query("DELETE FROM channel_feed_model")
        public abstract void clearFolders();

        @Transaction
        public void removeDialogsFromChannelFeed(List<Long> dialogsToRemove, TableModels.ChannelFeedModel localFolder){
            List<Long> allDialogs  =  localFolder.dialogs;
            if(allDialogs != null){
                allDialogs.removeAll(dialogsToRemove);
                localFolder.dialogs = new ArrayList<>(allDialogs);
                update(localFolder);
            }
        }
    }


    @Dao
    public abstract static class MusicTableDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract  void insertOrUpdate(TableModels.MusicTables.Playlist playlist);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract  void insertOrUpdateMulti(List<TableModels.MusicTables.Playlist> playlist);

        @Query("DELETE FROM playlist WHERE name = :name")
        public abstract void delete(String name);

        @Query("SELECT * FROM playlist")
        public abstract List<TableModels.MusicTables.Playlist> getAll();


    }

    @Dao
    public abstract static class PendingTaskDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract  void insertOrUpdate(TableModels.PendingTask pendingTask);

        @Query("DELETE FROM pending_task WHERE task_id = :task_id")
        public abstract void delete(long task_id);

        @Query("SELECT * FROM pending_task")
        public abstract List<TableModels.PendingTask> getAll();


    }

    @Dao
    public abstract static class LockedChatsDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract  void insertOrUpdate(TableModels.LockedDialog lockedDialogEntry);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract  void insertOrUpdateMany(List<TableModels.LockedDialog> lockedDialogs);

        @Query("DELETE FROM locked_dialog WHERE dialogId = :dialog_id")
        public abstract void delete(Long dialog_id);

        @Query("SELECT dialogId FROM locked_dialog")
        public abstract List<Long> getAll();

        @Query("SELECT * FROM locked_dialog WHERE dialogId = :dialog_id")
        public abstract  Long getDialog(Long dialog_id);

        @Query("DELETE FROM locked_dialog")
        public abstract void clear();



    }

    @Dao
    public static abstract class LocalFolderDao{

        @Query("SELECT * FROM local_folder")
        public  abstract List<TableModels.LocalFolder> getLocalFolders();

        @Query("SELECT * FROM local_folder where id = :id limit 1")
        public  abstract List<TableModels.LocalFolder> getLocalFolderById(int id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract void insert(TableModels.LocalFolder localFolder);

        @Update()
        public abstract void update(TableModels.LocalFolder localFolder);

        @Delete
        public abstract void delete(TableModels.LocalFolder draft);

        @Query("DELETE FROM local_folder")
        public abstract void clearFolders();

        @Transaction
        public void removeDialogsFromLocalFolder(List<Long> dialogsToRemove, TableModels.LocalFolder localFolder){
            List<Long> allDialogs  =  localFolder.dialogs;
            if(allDialogs != null){
                allDialogs.removeAll(dialogsToRemove);
                localFolder.dialogs = new ArrayList<>(allDialogs);
                update(localFolder);
            }
        }
    }


    @Dao
    public static abstract class CacheDao{

        @Query("select * from cache_table where cache_type = :type limit 1")
        public  abstract TableModels.Cache loadCache(int type);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract long  insert(TableModels.Cache cache);

        @Query("DELETE FROM cache_table")
          public abstract void  clear();
    }

    @Dao
    public  static abstract class DraftDao{

        @Query("SELECT * FROM draft_table")
        public  abstract List< TableModels.Draft> getDrafts();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract void insert( TableModels.Draft draft);

        @Update()
        public abstract void update(TableModels.Draft draft);


        @Delete
        public abstract void delete( TableModels.Draft draft);

        @Query("DELETE FROM draft_table")
        public abstract void clearDrafts();
    }


    @Dao
    public abstract static class FeedDao {

        @Query("SELECT * FROM feed_table")
        public abstract List<TableModels.Feed> getFeeds();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract long insert(TableModels.Feed feed);

        @Delete
        public abstract int delete(TableModels.Feed feed);

        @Query("DELETE FROM feed_table")
        public abstract void clearFeeds();

        @Transaction
        public void clearOldFeeds() {
            List<TableModels.Feed> newFeeds = getFeeds();
            for (int a = 0; a < newFeeds.size(); a++) {
                TableModels.Feed feed = newFeeds.get(a);
                if (feed == null) {
                    continue;
                }
                Instant instantNow = Instant.now();
                long durationsec = instantNow.getEpochSecond() - feed.date;
                // PlusBuildVars.log(FeedActivity.class.getSimpleName(),"duration  = " + durationsec);
                if (durationsec > 60 * 60 * 24 * 7) {
                    // delete(feed);
                }
            }
        }

    }
}
