package org.master.feature.feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.collection.LongSparseArray;

import com.google.android.exoplayer2.util.Log;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class FeedController extends BaseController implements NotificationCenter.NotificationCenterDelegate {

    public interface Delegate{
        void onDataLoaded(ArrayList<MessageObject> messageObjects);
    }

    private static volatile FeedController[] Instance = new FeedController[UserConfig.MAX_ACCOUNT_COUNT];
    public static FeedController getInstance(int num) {
        FeedController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (FeedController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new FeedController(num);
                }
            }
        }
        return localInstance;
    }


    public static String[] categoryData = {
                    "News",
                    "Society",
                    "Politics",
                    "Entertainment",
                    "Sports",
                    "Technology",
                    "Economy",
                    "Others"
    };

    private ArrayList<TLRPC.Chat> allChannels = new ArrayList<>();
    private ArrayList<Long> allChannelsId= new ArrayList<>();
    public HashMap<String, ArrayList<TLRPC.Chat>> channelCatDict = new HashMap<>();

    private SharedPreferences preferences;

    public FeedController(int num) {
        super(num);
        preferences = getPreferences();
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance(currentAccount).addObserver(FeedController.this,NotificationCenter.didReceiveNewMessages);
                NotificationCenter.getInstance(currentAccount).addObserver(FeedController.this,NotificationCenter.messagesDidLoad);
            }
        });
        loadChannels();
    }
    private SharedPreferences getPreferences() {
        if (currentAccount == 0) {
            return ApplicationLoader.applicationContext.getSharedPreferences("feedConfig", Context.MODE_PRIVATE);
        } else {
            return ApplicationLoader.applicationContext.getSharedPreferences("feedConfig" + currentAccount, Context.MODE_PRIVATE);
        }
    }


    public void addChannelToCategory(ArrayList<TLRPC.Chat> chats,String category){
        StringBuilder builder = new StringBuilder();
        for(int a = 0; a < chats.size(); a++){
            builder.append(chats.get(a).id);
            if(a != chats.size() - 1){
                builder.append(",");
            }
        }
        preferences.edit().putString(category,builder.toString()).commit();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if(id == NotificationCenter.messagesDidLoad){
        }

    }



    private void loadChannels(){
        Utilities.stageQueue.postRunnable(() -> {
            for(int a = 0; a < categoryData.length; a++){
                String category = categoryData[a];
                String channelBasedCategory = preferences.getString(category,"");
                if(TextUtils.isEmpty(channelBasedCategory)){
                    continue;
                }
                String[] channelList = channelBasedCategory.split(",");
                ArrayList<TLRPC.Chat> chatBasedOnCategory  = new ArrayList<>();
                for(int i = 0; i < channelList.length; i++){
                    Long channelId = Long.parseLong(channelList[i]);
                    TLRPC.Chat chat =  getMessagesController().getChat(channelId);
                    if(chat == null){
                        continue;
                    }
                    if(!allChannelsId.contains(channelId)){
                        allChannelsId.add(channelId);
                        allChannels.add(chat);
                    }
                    chatBasedOnCategory.add(chat);
                }
                channelCatDict.put(channelBasedCategory,chatBasedOnCategory);
            }

        });
    }


    public void loadMessageForTab(String tabId){
//        Log.i("loadMessage"," loadMessageForTabtab id = " + tabId);
//        ArrayList<TLRPC.Chat> chats = channelCatDict.get(tabId);
////        if(chats == null || chats.isEmpty()){
////            return;
////        }
//        for(int a = 0; a < chats.size();a++){
//            TLRPC.Chat chat = chats.get(a);
//            if(chat == null){
//                continue;
//            }
//            long dialogId = -chat.id;
//            getMessagesController().loadMessages(dialogId, 0, false, 1, 0, 0, true, 0, -1, 2, 0, 0, 0, 0, 0);
//        }

       LongSparseArray<MessageObject> sparseArray =   getMessagesController().dialogMessage;
        ArrayList<MessageObject> messageObjects = new ArrayList<>();
        for(int i = 0; i < sparseArray.size(); i++) {
            long key = sparseArray.keyAt(i);
            // get the object by the key.
            MessageObject obj = sparseArray.get(key);
            messageObjects.add(obj);
        }
        Log.i("loadMessage"," messag size id = " + messageObjects.size());

//        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.didNewsLoaded,tabId,messageObjects);

    }

    public void processMessages(MessageObject messageObject){

    }


    public  static void addMessage(final ArrayList<TLRPC.Message> messages, String cat, BaseFragment baseFragment) {
        MessagesStorage.getInstance(baseFragment.getCurrentAccount()).getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                try {
                    MessagesStorage.getInstance(baseFragment.getCurrentAccount()).getDatabase().beginTransaction();
                    SQLitePreparedStatement state = MessagesStorage.getInstance(baseFragment.getCurrentAccount()).getDatabase().executeFast("REPLACE INTO feed_table VALUES(?, ?, ?, ?)");
                    for (int a = 0; a < messages.size(); a++) {
                        TLRPC.Message message =  messages.get(a);
                        state.requery();
                        state.bindLong(1,  message.id);
                        state.bindLong(2, message.dialog_id);
                        state.bindString(3,cat);
                        state.bindInteger(4, ConnectionsManager.getInstance(baseFragment.getCurrentAccount()).getCurrentTime());
                        NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                        message.serializeToStream(data);
                        state.bindByteBuffer(4, data);
                        state.step();
                        data.reuse();
                    }
                    state.dispose();
                    MessagesStorage.getInstance(baseFragment.getCurrentAccount()).getDatabase().commitTransaction();
                } catch (Exception e) {
                    FileLog.e("hulugrammessage", e);
                }
            }
        });
    }

    public static boolean cannMessageBeSaved(MessageObject messageObject) {
        if (messageObject == null || messageObject.isSticker()) {
            return false;
        }
        if (messageObject.getDocument() == null && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) && !messageObject.isMusic() && !messageObject.isVideo()) {
            return false;
        }
        boolean canSave = false;
        if (!(messageObject.messageOwner.attachPath == null || messageObject.messageOwner.attachPath.length() == 0 || !new File(messageObject.messageOwner.attachPath).exists())) {
            canSave = true;
        }
        if (!canSave && FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner).exists()) {
            canSave = true;
        }
        if (canSave) {
            return false;
        }
        return true;
    }

    public void loadMessages() {
        getMessagesStorage().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                ArrayList<MessageObject> un =new ArrayList<>();
                ArrayList<MessageObject> ed =new ArrayList<>();
                ArrayList<MessageObject> ing =new ArrayList<>();

                ArrayList<MessageObject> objects;
                AbstractMap<Long, TLRPC.User> usersDict;
                AbstractMap<Long, TLRPC.Chat> chatsDict;

                int a;
                TLRPC.TL_messages_messages res = new TLRPC.TL_messages_messages();
                SQLiteCursor cursor = null;
                try {
                    cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT * FROM feed_table ORDER BY date ASC", new Object[0]), new Object[0]);
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(3);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                            message.id = cursor.intValue(0);
                            message.dialog_id =  cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            res.messages.add(message);
                        }
                    }
                    if (cursor != null) {
                        cursor.dispose();
                    }
                } catch (Exception e) {

                    FileLog.e("tmessages", e);
                    objects = new ArrayList();
                    usersDict = new HashMap();
                    chatsDict = new HashMap();
                    for (a = 0; a < res.users.size(); a++) {
                        TLRPC.User u = res.users.get(a);
                        usersDict.put(Long.valueOf(u.id), u);
                    }
                    for (a = 0; a < res.chats.size(); a++) {
                        TLRPC.Chat c = res.chats.get(a);
                        chatsDict.put(Long.valueOf(c.id), c);
                    }
                    for (a = 0; a < res.messages.size(); a++) {
                        objects.add(new MessageObject(currentAccount, res.messages.get(a), usersDict, chatsDict,false,true));
                    }
//                    AndroidUtilities.runOnUIThread(new Runnable() {
//                        public void run() {
//                            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messagesDidLoad, Integer.valueOf(0), Integer.valueOf(objects.size()), objects, Boolean.valueOf(true), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Boolean.valueOf(true), Integer.valueOf(classGuid), Integer.valueOf(0));
//                        }
//                    });
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
                objects = new ArrayList();
                usersDict = new HashMap();
                chatsDict = new HashMap();
                for (a = 0; a < res.users.size(); a++) {
                    TLRPC.User u2 =  res.users.get(a);
                    usersDict.put(u2.id, u2);
                }
                for (a = 0; a < res.chats.size(); a++) {
                    TLRPC.Chat c2 =  res.chats.get(a);
                    chatsDict.put(c2.id, c2);
                }
                for (a = 0; a < res.messages.size(); a++) {
                    objects.add(new MessageObject(currentAccount, res.messages.get(a), usersDict, chatsDict,false,true));
                }
                ArrayList<String> names = new ArrayList<>();
                ArrayList<MessageObject> musicEntriesFinal = new ArrayList<>();

                for(int i = 0; i < objects.size();  i++){
                    MessageObject messageObject = objects.get(i);
                    String fileName = messageObject.getFileName();
                    if(!TextUtils.isEmpty(fileName) && !names.contains(fileName)){
                        names.add(messageObject.getFileName());
                    }
                    if(messageObject.attachPathExists || messageObject.mediaExists){
                        ed.add(messageObject);
                    }else if(getFileLoader().isLoadingFile(messageObject.getFileName())){
                        ing.add(messageObject);
                    }else{
                        un.add(messageObject);
                    }
                    musicEntriesFinal.add(messageObject);
                }


            }
        });
    }


    public void deleteMessage(final ArrayList<TLRPC.Message> messages) {
        MessagesStorage.getInstance(this.currentAccount).getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                int a = 0;
                while (a < messages.size()) {
                    try {
                        TLRPC.Message message =  messages.get(a);
                        getMessagesStorage().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM feed_table WHERE mid = %d", new Object[]{Integer.valueOf(message.id)})).stepThis().dispose();
                        a++;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        return;
                    }
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        loadMessages();
                    }
                });
            }
        });
    }

    private void deleteDownloaded(final ArrayList<TLRPC.Message> messages) {
        getMessagesStorage().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                int a = 0;
                while (a < messages.size()) {
                    try {
                        TLRPC.Message message =  messages.get(a);
                        boolean downloaded = false;
                        if (!(message.attachPath == null || message.attachPath.length() == 0 || !new File(message.attachPath).exists())) {
                            downloaded = true;
                        }
                        if (!downloaded && FileLoader.getInstance(currentAccount).getPathToMessage(message).exists()) {
                            downloaded = true;
                        }
                        if (downloaded) {
                            getMessagesStorage().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM feed_table WHERE mid = %d", new Object[]{Integer.valueOf(message.id)})).stepThis().dispose();
                        }
                        a++;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        return;
                    }
                }
            }
        });
    }

    public void deleteAll() {
        MessagesStorage.getInstance(this.currentAccount).getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                try {
                    getMessagesStorage().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM feed_table", new Object[0])).stepThis().dispose();
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            loadMessages();
                        }
                    });
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });
    }

    public TLObject getDownloadObject(MessageObject messageObject) {
        TLRPC.MessageMedia media = messageObject.messageOwner.media;
        if (media != null) {
            if (media.document != null) {
                return media.document;
            }
            if (media.webpage != null && media.webpage.document != null) {
                return media.webpage.document;
            }
            if (media.webpage != null && media.webpage.photo != null) {
                return FileLoader.getClosestPhotoSizeWithSize(media.webpage.photo.sizes, AndroidUtilities.getPhotoSize());
            }
            if (media.photo != null) {
                return FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, AndroidUtilities.getPhotoSize());
            }
        }
        return new TLRPC.TL_messageMediaEmpty();
    }
}
