//package org.master.feature.tabs;
//
//import android.content.SharedPreferences;
//import android.text.TextUtils;
//import android.util.Base64;
//
//import org.master.feature.SharedAppConfig;
//import org.telegram.messenger.MessagesController;
//import org.telegram.tgnet.SerializedData;
//import org.telegram.tgnet.TLRPC;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//
//public class TabController {
//
//
//    public static final int TAB_USER = 100;
//    public static final int TAB_GROUP = 101;
//    public static final int TAB_CHANNEL = 103;
//    public static final int TAB_BOTS = 104;
//    public static final int TAB_ADMINS = 105;
//    public static final int TAB_CREATOR = 106;
//
//    public static boolean isLocalTab(int id){
//        if(id >= 100 && id <= 106){
//            return true;
//        }
//        return false;
//    }
//
//    public static int currentTabCount;
//
//    public static ArrayList<TabItem> tabItems = new ArrayList<>();
//    public static ArrayList<Integer> visibleTabIds = new ArrayList<>();
//    public static HashMap<Integer,TabItem> tabItemHashMap = new HashMap<>();
//    private static boolean tabListLoaded;
//
//
//
//
//    public static final TabItem[] default_tab_items = {
//            TabItem.create("User",1,TAB_USER, 0,0,true),
//            TabItem.create("Group",1,TAB_GROUP, 1,1,true),
//            TabItem.create("Channel",1,TAB_CHANNEL, 3,3,true),
//            TabItem.create("Bots",1,TAB_BOTS,4,4,true),
//            TabItem.create("Admins",1,TAB_ADMINS, 5,5,true),
//            TabItem.create("Creator",1,TAB_CREATOR, 6,6,true)
//    };
//
//
//    public static ArrayList<TLRPC.Dialog> dialogs(int type,int account){
//        MessagesController messagesController = MessagesController.getInstance(account);
//        if(type == TAB_USER){
//            return messagesController.dialogsUsersOnly;
//        }else if(type == TAB_BOTS){
//            return messagesController.dialogsBots;
//        }else if(type == TAB_CHANNEL){
//            return messagesController.dialogsChannelsOnly;
//        }else if(type == TAB_ADMINS){
//            return messagesController.dialogsAdmin;
//        }else if(type == TAB_GROUP){
//            return messagesController.dialogsGroupsOnly;
//        }else if(type == TAB_CREATOR){
//            return messagesController.dialogsCreator;
//        }
//        return new ArrayList<>();
//    }
//
//    public static void loadTabList(boolean force) {
//        if (tabListLoaded &&  !force) {
//            return;
//        }
//        tabListLoaded = true;
//        tabItems.clear();
//        visibleTabIds.clear();
//        currentTabCount = 0;
//        tabItemHashMap.clear();
//        String list = SharedAppConfig.getPrefrence().getString("tabList", null);
//        if (!TextUtils.isEmpty(list)) {
//            byte[] bytes = Base64.decode(list, Base64.DEFAULT);
//            SerializedData data = new SerializedData(bytes);
//            int count = data.readInt32(false);
//            for (int a = 0; a < count; a++) {
//                TabItem tabItem = new TabItem(
//                        data.readString(false),
//                        data.readInt32(false),
//                        data.readInt32(false),
//                        data.readInt32(false),
//                        data.readInt32(false),
//                        data.readBool(false));
//
//                if(tabItem.enabled){
//                    tabItems.add(tabItem);
//                    currentTabCount++;
//                    visibleTabIds.add(tabItem.id);
//                }
//
//            }
//            data.cleanup();
//        }else{
//            tabItems.addAll(Arrays.asList(default_tab_items));
//            for (TabItem default_tab_item : default_tab_items) {
//                visibleTabIds.add(default_tab_item.id);
//            }
//            currentTabCount = tabItems.size();
//            saveTabList();
//        }
//        Collections.sort(tabItems, (tabItem, t1) -> Integer.compare(tabItem.order,t1.order));
//        for(int a = 0; a < tabItems.size();a++){
//            tabItemHashMap.put(tabItems.get(a).id,tabItems.get(a));
//        }
//    }
//
//    public static void saveTabList() {
//        SerializedData serializedData = new SerializedData();
//        int count = tabItems.size();
//        serializedData.writeInt32(count);
//        for (int a = 0; a < count; a++) {
//            TabItem tabItem = tabItems.get(a);
//            serializedData.writeString(tabItem.title);
//            serializedData.writeInt32(tabItem.order);
//            serializedData.writeInt32(tabItem.id);
//            serializedData.writeInt32(tabItem.icon_in_pos);
//            serializedData.writeInt32(tabItem.icon_out_pos);
//            serializedData.writeBool(tabItem.enabled);
//
//        }
//        SharedPreferences preferences = SharedAppConfig.getPrefrence();
//        preferences.edit().putString("tabList", Base64.encodeToString(serializedData.toByteArray(), Base64.NO_WRAP)).commit();
//        serializedData.cleanup();
//    }
//
//
//
//    public static int getTabUnreadCount(int type,int account){
//        ArrayList<TLRPC.Dialog> dialogs = dialogs(type,account);
//        int count = 0;
//        for(int a = 0; a < dialogs.size(); a++){
//            TLRPC.Dialog dialog = dialogs.get(a);
//            if(dialog.unread_count > 0){
//                count++;
//            }
//        }
//        return count;
//    }
//
//}
