package org.master.feature;

import static org.master.feature.tabs.TabOrderManager.TAB_USERS_ALL;
import static org.master.feature.tabs.TabOrderManager.ids;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Relation;

import org.master.feature.database.AppDatabase;
import org.master.feature.tabs.TabFilterView;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TelegramMessageUpdateManager {

    private static volatile TelegramMessageUpdateManager[] Instance = new TelegramMessageUpdateManager[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }



    public static TelegramMessageUpdateManager getInstance(int num) {
        TelegramMessageUpdateManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects[num]) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new TelegramMessageUpdateManager(num);
                }
            }
        }
        return localInstance;
    }

    private int currentAccount;
    public TelegramMessageUpdateManager(int num){
        currentAccount = num;
    }

    public  void addUser(long user_id){
        Utilities.stageQueue.postRunnable(() -> {
            SharedPreferences preferences = null;
            if (currentAccount == 0) {
                preferences =  ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
            } else {
                preferences =  ApplicationLoader.applicationContext.getSharedPreferences("userconfig" + currentAccount, Context.MODE_PRIVATE);
            }

            String userChange = preferences.getString("userchange","");
            String[]  users = TextUtils.split(userChange,",");
            ArrayList<String> list = new ArrayList<>(Arrays.asList(users));
            list.add(0,user_id + "");
            preferences.edit().putString("userchange",TextUtils.join(",",list)).commit();
        });

    }

    public static final int UPDATE_PHOTO = 1;
    public static final int UPDATE_PHONE = 2;
    public static final int UPDATE_NAME = 3;
    public static final int UPDATE_BLOCKED = 4;

    @Entity(tableName = "update_user_table")
    public static class UpdateUser{

        @PrimaryKey(autoGenerate = false)
        @ColumnInfo(name = "user_id")
        public long user_id;

    }


    @Entity(tableName = "update_table")
    public  static class UpdateObject{

        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "update_id")
        public long update_id;

        @ColumnInfo(name = "type")
        public int type;

        @ColumnInfo(name = "date")
        public int date;


        @ColumnInfo(name = "blocked")
        public boolean blocked;

        @ColumnInfo(name = "user_id")
        public long user_id;

    }




    //exuclisive class tha manger users with udpate
    public static class UserWithUpdate {
        @Embedded
        public UpdateUser user_id;
        @Relation(
                parentColumn = "user_id",
                entityColumn = "user_id"
        )
        public List<UpdateObject> updateObjects;

    }





    public void parseMessageUpdate(TLRPC.Update baseUpdate){
        UpdateObject updateObject = new UpdateObject();
        updateObject.date =(int) System.currentTimeMillis()/1000;
        if (baseUpdate instanceof TLRPC.TL_updateUserPhoto) {
            TLRPC.TL_updateUserPhoto update = (TLRPC.TL_updateUserPhoto) baseUpdate;
            updateObject.type = UPDATE_PHOTO;
            updateObject.date = update.date;
            updateObject.user_id = update.user_id;
        }else if(baseUpdate instanceof TLRPC.TL_updateUserPhone){
            TLRPC.TL_updateUserPhone update = (TLRPC.TL_updateUserPhone) baseUpdate;
            updateObject.user_id = update.user_id;
            updateObject.type = UPDATE_PHONE;

        }else if(baseUpdate instanceof TLRPC.TL_updateUserName){
            TLRPC.TL_updateUserName update = (TLRPC.TL_updateUserName) baseUpdate;
            updateObject.user_id = update.user_id;
            updateObject.type = UPDATE_NAME;
        }else if(baseUpdate instanceof TLRPC.TL_updatePeerBlocked){
            TLRPC.TL_updatePeerBlocked update = (TLRPC.TL_updatePeerBlocked) baseUpdate;
            if(update.peer_id != null && update.peer_id.user_id != 0 && UserConfig.getInstance(currentAccount).getClientUserId() != ((TLRPC.TL_updatePeerBlocked) baseUpdate).peer_id.user_id){
                updateObject.user_id = update.peer_id.user_id;
                updateObject.blocked = update.blocked;
                updateObject.type = UPDATE_BLOCKED;
            }else{
                return;
            }
        }
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                UpdateUser updateUser =new UpdateUser();
                updateUser.user_id  = updateObject.user_id;
                long identifier = AppDatabase.getInstance(currentAccount).appDao().insertUser(updateUser);
                if(identifier > 0){
                    ArrayList<UpdateObject> updateObjects = new ArrayList<>();
                    updateObjects.add(updateObject);
                    AppDatabase.getInstance(currentAccount).appDao().insertUpdates(updateObjects);
                }
            }
        });



    }



    public static String[] title = {
          "Users","Groups","Channel","Bots","Admin","Unread","Verified"
    } ;

    public static String[] emoticon = {
            "\uD83D\uDC64","\uD83D\uDC65","\uD83D\uDCE2","\uD83E\uDD16","\u2705","\uD83C\uDF93","\u2B50"
    } ;


    public ArrayList<MessagesController.DialogFilter> getLocalDialogFilters(){
        ArrayList<MessagesController.DialogFilter> filterArrayList = new ArrayList<>();
        for(int a = 0; a < ids.length;a++){
            MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
            filter.id = ids[a];
            filter.order = 1;
            filter.name = title[a];
            filter.emoticon = emoticon[a];
            filterArrayList.add(filter);
        }

        return filterArrayList;
    }
    public static boolean isLocalFilter(int id){
        return id >= 200 && id <= 222;
    }

    public SparseIntArray counterMap = new SparseIntArray();
    public void clearCounter(){
        counterMap.clear();
    }

    public int getCounterForTab(int tabId){
        return counterMap.get(tabId);
    }
    public void clearCounter(int filter){
        counterMap.put(filter,0);
    }
    public void addDialogToLocalFilter(int filter, TLRPC.Dialog dialog, SparseArray<ArrayList<TLRPC.Dialog>> localFilter){
        ArrayList<TLRPC.Dialog> dialogs =  localFilter.get(filter);
        if(dialogs == null){
            dialogs = new ArrayList<>();
        }
        if(dialog != null && dialog.unread_count > 0){
            int unreadCount = counterMap.get(filter);
            unreadCount += dialog.unread_count;
            counterMap.put(unreadCount,unreadCount);
        }
        dialogs.add(dialog);
        localFilter.put(filter,dialogs);
    }

    public static   ArrayList<TabFilterView.FilterData>  getTabFilters(int type, int folder){
        ArrayList<TabFilterView.FilterData> filterDataArrayList = new ArrayList<>();
//        TabFilterView.FilterData filterData = new TabFilterView.FilterData();
//        filterData.tab_id = 0;
//        filterData.folder_id = 1;
//        filterData.title = "Archive";
//        filterData.emoticon = "\uD83D\uDDC4";
//        filterDataArrayList.add(filterData);
//        if(folder == 0){
//           if(type == TAB_USERS_ALL){
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_USER_PREMIUM;
//                filterData.folder_id = 0;
//                filterData.title = "Premium";
//                filterData.emoticon = "\u2B50";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_USER_SECRETE;
//                filterData.folder_id = 0;
//                filterData.title = "Secrete";
//                filterData.emoticon = "\uD83C\uDFAD";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_USER_MUTUAL;
//                filterData.folder_id = 0;
//                filterData.title = "Mutual";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//
//            }else if(type == TAB_GROUP_ALL){
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_GROUP_PUBLIC;
//                filterData.folder_id = 0;
//                filterData.title = "Public";
//                filterData.emoticon = "\uD83C\uDFAD";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_GROUP_PRIVATE;
//                filterData.folder_id = 0;
//                filterData.title = "Private";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_GROUP_SUPER;
//                filterData.folder_id = 0;
//                filterData.title = "Supper";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//            }else if(type == TAB_CHANNEL_ALL){
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_CHANNEL_PUBLIC;
//                filterData.folder_id = 0;
//                filterData.title = "Public";
//                filterData.emoticon = "\uD83C\uDFAD";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_CHANNEL_PRIVATE;
//                filterData.folder_id = 0;
//                filterData.title = "Private";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//            }else if(type == TAB_ADMIN_ALL){
//
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_CREATOR_ALL;
//                filterData.folder_id = 0;
//                filterData.title = "Creator";
//                filterData.emoticon = "\uD83C\uDFAD";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_ADMIN_GROUP;
//                filterData.folder_id = 0;
//                filterData.title = "Admin Group";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_ADMIN_CHANNEL;
//                filterData.folder_id = 0;
//                filterData.title = "Admin Channel";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_CREATOR_CHANNEL;
//                filterData.folder_id = 0;
//                filterData.title = "Creator Channel";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//
//
//                filterData = new TabFilterView.FilterData();
//                filterData.tab_id = TAB_CREATOR_GROUP;
//                filterData.folder_id = 0;
//                filterData.title = "Creator Group";
//                filterData.emoticon = "\uD83D\uDC65";
//                filterDataArrayList.add(filterData);
//            }
//
//
//        }

        return filterDataArrayList;
    }

}
