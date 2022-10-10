package org.plus.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


import java.util.ArrayList;

public class TableModels {


    @Entity(tableName = "channel_feed_model")
    public static class ChannelFeedModel{

        @NonNull
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        public int id;

        @ColumnInfo(name = "order")
        public int order;

        @ColumnInfo(name = "title")
        public String title;

        @ColumnInfo(name = "dialogs")
        public ArrayList<Long> dialogs = new ArrayList<>();

    }

    public static class MusicTables{

        @Entity(tableName = "playlist")
        public static class Playlist{

            @NonNull
            @PrimaryKey
            @ColumnInfo(name = "name")
            public String name;
        }

    }


    @Entity(tableName = "pending_task")
    public static class PendingTask{

        @PrimaryKey()
        @ColumnInfo(name = "task_id")
        public long task_id;

        @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
        public byte[] data;

    }


    @Entity(tableName = LockedDialog.TABLE_NAME)
    public static class LockedDialog {

        public static final String TABLE_NAME = "locked_dialog";

        @PrimaryKey
        @ColumnInfo(name = "dialogId")
        public long dialogId;
    }


    @Entity(tableName = "car_channel")
    public static class CarChanel{

        @NonNull
        @PrimaryKey
        public int id;


        @ColumnInfo(name = "data")
        public String data;
    }

    @Entity(tableName = "cache_table")
    public static class Cache{

        @NonNull
        @PrimaryKey
        @ColumnInfo(name = "cache_type")
        public int data_type;


        @ColumnInfo(name = "data")
        public String data;
    }


    @Entity(tableName = "draft_table")
    public static class Draft{

        @ColumnInfo(name = "message")
        public String  message;


        @ColumnInfo(name = "title")
        public String title;


        @NonNull
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "id")
        public int id;


        @ColumnInfo(name = "messageLink")
        public String messageLink;

    }

    @Entity(tableName = "local_folder")
    public static class LocalFolder{

        @NonNull
        @PrimaryKey(autoGenerate = true)
        public int id;

        @ColumnInfo(name = "title")
        public String title;


        @ColumnInfo(name = "pos")
        public int pos;

        @ColumnInfo(name = "dialogs")
        public ArrayList<Long> dialogs = new ArrayList<>();

    }

    @Entity(tableName = "feed_table")
    public  static class   Feed{

        @NonNull
        @PrimaryKey()
        public long user_id;


        @ColumnInfo(name = "date")
        public int date;
    }



}
