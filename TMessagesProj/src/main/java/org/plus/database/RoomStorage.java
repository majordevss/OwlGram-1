package org.plus.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;


@Database(
        entities = {
                TableModels.CarChanel.class,
                TableModels.Cache.class,
                TableModels.Draft.class,
                TableModels.LocalFolder.class,
                TableModels.Feed.class,
                TableModels.LockedDialog.class,
                TableModels.PendingTask.class,
                TableModels.MusicTables.Playlist.class,
                TableModels.ChannelFeedModel.class,

        },
        exportSchema = false
        ,
        version = RoomStorage.VERSION

)

@TypeConverters({Converter.class})
public abstract class RoomStorage extends RoomDatabase {

    public static final int VERSION = 1;

    public  abstract AppDao.LockedChatsDao lockedChatsDao();
    public  abstract AppDao.LocalFolderDao localFolderDao();
    public  abstract AppDao.CacheDao cacheDao();
    public  abstract AppDao.DraftDao draftDao();
    public  abstract AppDao.FeedDao feedDao();
    public  abstract AppDao.PendingTaskDao pendingTaskDao();
    public  abstract AppDao.MusicTableDao musicTableDao();
    public  abstract AppDao.ChannelFeedModelDao channelFeedModelDao();


}
