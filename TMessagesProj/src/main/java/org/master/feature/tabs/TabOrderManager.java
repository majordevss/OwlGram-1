package org.master.feature.tabs;

import org.json.JSONArray;
import org.json.JSONException;
import org.master.feature.SharedAppConfig;
import org.master.feature.TelegramMessageUpdateManager;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;

import java.util.ArrayList;

public class TabOrderManager {

    public static final int TAB_USERS_ALL = 200;
    public static final int TAB_USER_PREMIUM =  201;
    public static final int TAB_USER_SECRETE = 202;
    public static final int TAB_USER_MUTUAL =  203;
    public static final int TAB_USER_BOTS = 204;
    public static final int TAB_GROUP_ALL= 205;
    public static final int TAB_GROUP_PUBLIC= 206;
    public static final int TAB_GROUP_PRIVATE= 207;
    public static final int TAB_GROUP_SUPER= 208;
    public static final int TAB_CHANNEL_ALL = 209;
    public static final int TAB_CHANNEL_PUBLIC= 210;
    public static final int TAB_CHANNEL_PRIVATE = 211;
    public static final int TAB_ADMIN_ALL = 212;
    public static final int TAB_CREATOR_ALL= 213;
    public static final int TAB_ADMIN_GROUP= 214;
    public static final int TAB_ADMIN_CHANNEL= 215;
    public static final int TAB_CREATOR_CHANNEL = 222;
    public static final int TAB_CREATOR_GROUP = 222;
    public static final int TAB_UNREAD_ALL = 216;
    public static final int TAB_UNREAD_USER = 217;
    public static final int TAB_UNREAD_GROUP= 218;
    public static final int TAB_UNREAD_CHANNEL= 219;
    public static final int TAB_EXPLICIT = 220;
    public static final int TAB_VERIFIED= 221;


    public static int[] ids = {
            TAB_USERS_ALL,   TAB_GROUP_ALL,  TAB_CHANNEL_ALL,TAB_USER_BOTS,TAB_ADMIN_ALL,TAB_UNREAD_ALL,TAB_VERIFIED
    };

    public static class EditableTabItem {

        public final String id;
        public final String text;
        public final boolean isDefault;
        public final int stableId;
        public final String emoticon;
        public EditableTabItem(int stable_id,String menu_id, String menu_text, String _emoticon,boolean menu_default) {
            stableId = stable_id;
            id = menu_id;
            text = menu_text;
            isDefault = menu_default;
            emoticon = _emoticon;
        }
    }

    private static final Object sync = new Object();

    private static boolean configLoaded;
    private static JSONArray data;


    private static final String[] list_items_emoticon = new String[]{
             "\uD83D\uDC64"
            , "\uD83D\uDC64"
            , "\uD83D\uDC64"
            , "\uD83D\uDC64"
            , "\uD83E\uDD16"
            , "\uD83D\uDC65"
            , "\uD83D\uDC65"
            , "\uD83D\uDC65"
            , "\uD83D\uDC65"
            , "\uD83D\uDCE2"
            , "\uD83D\uDCE2"
            , "\uD83D\uDCE2"
            , "\u2705"
            , "\u2705"
            , "\u2705"
            , "\u2705"
            , "\uD83D\uDCE2"
            , "\uD83D\uDC65"
            , "\uD83C\uDF93"
            ,  "\uD83C\uDF93"
            , "\uD83C\uDF93"
            , "\uD83C\uDF93"
            , "\u2B50"
    };

    private static final String[] list_items = new String[]{
              "Users"
            , "Premium Users"
            , "Secret Users"
            , "Mutual Users"
            , "Bots"
            , "Groups"
            , "Public Groups"
            , "Private Groups"
            , "Super Groups"
            , "Channels"
            , "Public Channel"
            , "Private Channel"
            , "Admin"
            , "Creator"
            , "Group Admin"
            , "Chanel Admin"
            , "Creator Channel"
            , "Creator Group"
            , "Unread"
            ,  "Unread User"
            , "Unread Group"
            , "Unread Channel"
            , "Verified"
    };

    private static void loadDefaultItems() {
        data.put(list_items[0]);
        data.put(list_items[4]);
        data.put(list_items[5]);
        data.put(list_items[9]);
        data.put(list_items[12]);
        data.put(list_items[13]);
        data.put(list_items[18]);

    }


    private static final int[] list_items_ids = new int[]{
      TAB_USERS_ALL
    , TAB_USER_PREMIUM
    , TAB_USER_SECRETE
    , TAB_USER_MUTUAL
    , TAB_USER_BOTS
    , TAB_GROUP_ALL
    , TAB_GROUP_PUBLIC
    , TAB_GROUP_PRIVATE
    , TAB_GROUP_SUPER
    , TAB_CHANNEL_ALL
    , TAB_CHANNEL_PUBLIC
    , TAB_CHANNEL_PRIVATE
    , TAB_ADMIN_ALL
    , TAB_CREATOR_ALL
    , TAB_ADMIN_GROUP
    , TAB_ADMIN_CHANNEL
    , TAB_CREATOR_CHANNEL
    , TAB_CREATOR_GROUP
    , TAB_UNREAD_ALL
    , TAB_UNREAD_USER
    , TAB_UNREAD_GROUP
    , TAB_UNREAD_CHANNEL
    , TAB_VERIFIED

};
    public static ArrayList<MessagesController.DialogFilter> getLocalDialogFilters(){
        ArrayList<MessagesController.DialogFilter> filterArrayList = new ArrayList<>();
        for(int a = 0; a < TabOrderManager.sizeAvailable();a++){
            TabOrderManager.EditableTabItem data = TabOrderManager.getSingleAvailableMenuItem(a);
            if(data == null){
                continue;
            }
            MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
            filter.id = data.stableId;
            filter.order = 1;
            filter.name = data.text;
            filter.emoticon = data.emoticon;
            filterArrayList.add(filter);
        }

        return filterArrayList;
    }

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }
            String items = SharedAppConfig.tabsItem;
            try {
                data = new JSONArray(items);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (data.length() == 0) {
                loadDefaultItems();
            }
            configLoaded = true;
        }
    }



    private static int getArrayPosition(String id) {
        try {
            for (int i = 0;i < data.length(); i++) {
                if(data.getString(i).equals(id)) {
                    return i;
                }
            }
        } catch (JSONException ignored) {}
        return -1;
    }


    public static int getPositionItem(String id, boolean isDefault) {
        int position = getArrayPosition(id);
        if(position == -1 && isDefault) {
            position = 0;
            data.put(id);
            SharedAppConfig.setTabsItems(data.toString());
        }
        return position;
    }

    public static void changePosition(int oldPosition, int newPosition) {
        try {
            String data1 = data.getString(newPosition);
            String data2 = data.getString(oldPosition);
            data.put(oldPosition, data1);
            data.put(newPosition, data2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SharedAppConfig.setTabsItems(data.toString());
    }

    public static EditableTabItem getSingleAvailableMenuItem(int position) {
        ArrayList<EditableTabItem> list = getMenuItemsEditable();
        for (int i = 0; i < list.size(); i++) {
            if (getPositionItem(list.get(i).id, list.get(i).isDefault) == position) {
                return list.get(i);
            }
        }
        return null;
    }

    public static Boolean isAvailable(String id) {
        ArrayList<EditableTabItem> list = getMenuItemsEditable();
        for (int i = 0; i < list.size(); i++) {
            if(getPositionItem(list.get(i).id, list.get(i).isDefault) != -1) {
                if(list.get(i).id.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static EditableTabItem getSingleNotAvailableMenuItem(int position) {
        ArrayList<EditableTabItem> list = getMenuItemsEditable();
        int curr_pos = -1;
        for (int i = 0; i < list.size(); i++) {
            if(getPositionItem(list.get(i).id, list.get(i).isDefault) == -1) {
                curr_pos++;
            }
            if (curr_pos == position) {
                return list.get(i);
            }
        }
        return null;
    }

    public static int sizeHints() {
        ArrayList<EditableTabItem> list = getMenuItemsEditable();
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            if (getPositionItem(list.get(i).id, list.get(i).isDefault) == -1) {
                size++;
            }
        }
        return size;
    }

    public static int sizeAvailable() {
        ArrayList<EditableTabItem> list = getMenuItemsEditable();
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            if (getPositionItem(list.get(i).id, list.get(i).isDefault) != -1) {
                size++;
            }
        }
        return size;
    }

    public static int getPositionOf(String id) {
        ArrayList<EditableTabItem> list = getMenuItemsEditable();
        int sizeNAv = 0;
        for (int i = 0; i < list.size(); i++) {
            boolean isAv = getPositionItem(list.get(i).id, list.get(i).isDefault) != -1;
            if (list.get(i).id.equals(id) && !isAv) {
                return sizeNAv;
            }
            if(!isAv) {
                sizeNAv++;
            }
        }
        for (int i = 0; i < sizeAvailable(); i++) {
            EditableTabItem editableMenuItem = getSingleAvailableMenuItem(i);
            if (editableMenuItem != null && editableMenuItem.id.equals(id)) {
                return i;
            }
        }
        return -1;
    }


    public static void addItem(String id) {
        if(getArrayPosition(id) == -1) {
            addAsFirst(id);
        }
    }

    private static void addAsFirst(String id) {
        JSONArray result = new JSONArray();
        result.put(id);
        for (int i = 0; i < data.length(); i++) {
            try {
                result.put(data.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        data = result;
        SharedAppConfig.setTabsItems(data.toString());
    }

    public static void removeItem(String id) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < data.length(); i++) {
            try {
                String idTmp = data.getString(i);
                if(!idTmp.equals(id)) {
                    result.put(idTmp);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        data = result;
        SharedAppConfig.setTabsItems(data.toString());
    }



    public static ArrayList<EditableTabItem> getMenuItemsEditable() {
        ArrayList<EditableTabItem> list = new ArrayList<>();
        for(int a = 0; a < list_items.length;a++){
            list.add(
                    new EditableTabItem(
                            list_items_ids[a],
                            list_items[a],
                            list_items[a],
                            list_items_emoticon[a],
                            false
                    )
            );
        }
        return list;
    }

}