package org.plus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.SerializedData;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.DrawerLayoutAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import it.owlgram.android.OwlConfig;
import it.owlgram.android.helpers.MenuOrderManager;
import it.owlgram.android.helpers.PasscodeHelper;

public class FilterManager extends BaseController {

    public static final int FILTER_TPE_USER = 100;
    public static final int FILTER_TPE_GROUP = 101;
    public static final int FILTER_TPE_BOT = 102;
    public static final int FILTER_TPE_CHANNEL = 103;
    public static final int FILTER_TPE_ADMIN = 104;
    public static final int FILTER_TPE_UNREAD = 105;

    public static class ChatFilter{

        public boolean disabled;
        public int id;
        public String emoticon;
        public int order;
        public String title;

        public static ChatFilter create(int id,String title,String emoticon,int order,boolean disabled){
            return new ChatFilter(id,title,emoticon,order,disabled);
        }

        public ChatFilter(int id,String title,String emoticon,int order,boolean disabled){
            this.id = id;
            this.title = title;
            this.emoticon =emoticon;
            this.order = order;
            this.disabled = disabled;

            if(this.title == null){
                this.title = "";
            }
            if(this.emoticon == null){
                this.emoticon = "";
            }
        }

        public void write(SerializedData data){
            data.writeInt32(id);
            data.writeString(title);
            data.writeString(emoticon);
            data.writeInt32(order);
            data.writeBool(disabled);
        }

    }

    private final Object sync = new Object();
    private boolean configLoaded;

    private static volatile FilterManager[] Instance = new FilterManager[UserConfig.MAX_ACCOUNT_COUNT];
    public static FilterManager getInstance(int num) {
        FilterManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (FilterManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new FilterManager(num);
                }
            }
        }
        return localInstance;
    }

    public FilterManager(int num){
        super(num);
    }

    private SharedPreferences getPreferences() {
        if (currentAccount == 0) {
            return ApplicationLoader.applicationContext.getSharedPreferences("filterConfig", Context.MODE_PRIVATE);
        } else {
            return ApplicationLoader.applicationContext.getSharedPreferences("filterConfig" + currentAccount, Context.MODE_PRIVATE);
        }
    }

    public boolean chatFilterListLoaded;
    private final ArrayList<ChatFilter> chatFilters = new ArrayList<>();
    public static boolean isLocalFilter(int id){
        return id >= 100 && id <= 106;
    }

    public int getTabCounter(int id){
        return id;
    }
    public ArrayList<ChatFilter> getChatFilters() {
        return chatFilters;
    }

    public  void loadChatFilterList() {
        if (chatFilterListLoaded) {
            return;
        }
        SharedPreferences preferences = getPreferences();
        chatFilterListLoaded = true;
        chatFilters.clear();
        String list = preferences.getString("chat_filter_list", null);
        if (!TextUtils.isEmpty(list)) {
            byte[] bytes = Base64.decode(list, Base64.DEFAULT);
            SerializedData data = new SerializedData(bytes);
            int count = data.readInt32(false);
            for (int a = 0; a < count; a++) {
                ChatFilter info = new ChatFilter(
                        data.readInt32(false),
                        data.readString(false),
                        data.readString(false),
                        data.readInt32(false),
                        data.readBool(false));
                chatFilters.add(info);
            }
            data.cleanup();
        }else{
            chatFilters.add(ChatFilter.create(FILTER_TPE_USER, LocaleController.getString("NotificationHiddenChatUserName",R.string.NotificationHiddenChatUserName),"\uD83D\uDC64",1,false));
            chatFilters.add(ChatFilter.create(FILTER_TPE_GROUP, LocaleController.getString("AccDescrGroup",R.string.AccDescrGroup),"\uD83D\uDC65",2,false));
            chatFilters.add(ChatFilter.create(FILTER_TPE_BOT, LocaleController.getString("Bot",R.string.Bot),"\uD83D\uDC64",3,false));
            chatFilters.add(ChatFilter.create(FILTER_TPE_CHANNEL, LocaleController.getString("AccDescrChannel",R.string.AccDescrChannel),"\uD83D\uDCE2",4,false));
            chatFilters.add(ChatFilter.create(FILTER_TPE_ADMIN, LocaleController.getString("ChannelAdmin",R.string.ChannelAdmin),"\uD83D\uDC51",5,false));
            chatFilters.add(ChatFilter.create(FILTER_TPE_UNREAD, LocaleController.getString("FilterUnread",R.string.FilterUnread),"\u2705",6,false));
            saveChatFilterList();
        }
        Collections.sort(chatFilters, new Comparator<ChatFilter>() {
            @Override
            public int compare(ChatFilter chatFilter, ChatFilter t1) {
                return Integer.compare(chatFilter.order,t1.order);
            }
        });
    }

    public void saveChatFilterList() {
        SerializedData serializedData = new SerializedData();
        int count = chatFilters.size();
        serializedData.writeInt32(count);
        for (int a = 0; a < count; a++) {
            ChatFilter info = chatFilters.get(a);
            info.write(serializedData);
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("filterConfig", Activity.MODE_PRIVATE);
        preferences.edit().putString("chat_filter_list", Base64.encodeToString(serializedData.toByteArray(), Base64.NO_WRAP)).commit();
        serializedData.cleanup();
    }

    public ChatFilter addChatFilter(ChatFilter chatFilter) {
        loadChatFilterList();
        int count = chatFilters.size();
        for (int a = 0; a < count; a++) {
            ChatFilter  info = chatFilters.get(a);
            if (chatFilter.title.equals(info.title) && chatFilter.id == info.id && chatFilter.order == info.order && chatFilter.disabled == info.disabled) {
                return info;
            }
        }
        chatFilters.add(chatFilter);
        saveChatFilterList();
        return chatFilter;
    }

    public  void deleteChatFilter(ChatFilter chatFilter) {
        chatFilters.remove(chatFilter);
        saveChatFilterList();
    }



}
