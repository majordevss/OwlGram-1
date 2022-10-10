package org.master.feature.feed;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class FeedFilter {

    public int id;
    public String title;
    public int order;
    public String emoticon;
    public ArrayList<Long> feedDialogsId = new ArrayList<>();
//    public ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();

    public String description;


}
