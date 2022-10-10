package org.master.feature.ui;//package org.master.feature.ui;/*
// * This is the source code of Telegram for Android v. 5.x.x.
// * It is licensed under GNU GPL v. 2 or later.
// * You should have received a copy of the license in this archive (see LICENSE).
// *
// * Copyright Nikolai Kudashov, 2013-2018.
// */
//
//
//import android.content.Context;
//import android.text.SpannableStringBuilder;
//import android.text.Spanned;
//import android.text.TextUtils;
//import android.text.style.ForegroundColorSpan;
//import android.util.SparseArray;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//
//import androidx.recyclerview.widget.RecyclerView;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.ChatObject;
//import org.telegram.messenger.ContactsController;
//import org.telegram.messenger.FileLog;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.MessageObject;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.R;
//import org.telegram.messenger.UserObject;
//import org.telegram.messenger.Utilities;
//import org.telegram.tgnet.ConnectionsManager;
//import org.telegram.tgnet.TLObject;
//import org.telegram.tgnet.TLRPC;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.Adapters.SearchAdapterHelper;
//import org.telegram.ui.Cells.GraySectionCell;
//import org.telegram.ui.Cells.ManageChatTextCell;
//import org.telegram.ui.Cells.ManageChatUserCell;
//import org.telegram.ui.ChatUsersActivity;
//import org.telegram.ui.Components.EditTextBoldCursor;
//import org.telegram.ui.Components.FlickerLoadingView;
//import org.telegram.ui.Components.RecyclerListView;
//import org.telegram.ui.Components.UsersAlertBase;
//
//import java.util.ArrayList;
//
//public class OnlineUserAlert extends UsersAlertBase {
//
//    private  SearchAdapter searchAdapter;
//
//    private int delayResults;
//
//    private TLRPC.Chat currentChat;
//    private TLRPC.ChatFull info;
//
//    private boolean contactsEndReached;
//    private ArrayList<TLRPC.User> contacts = new ArrayList<>();
//    private SparseArray<TLRPC.User> contactsMap = new SparseArray<>();
//    private boolean loadingUsers;
//    private boolean firstLoaded;
//
//
//    private GroupVoipInviteAlertDelegate delegate;
//
//    private boolean showContacts;
//
//    private int emptyRow;
//    private int contactsStartRow;
//    private int contactsEndRow;
//    private int flickerProgressRow;
//    private int rowCount;
//    private int lastRow;
//
//
//    private boolean checkOnline(TLRPC.User user) {
//        boolean isOnline = !user.self && (user.status != null && user.status.expires > ConnectionsManager.getInstance(currentAccount).getCurrentTime() || MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(user.id));
//        return isOnline;
//    }
//
//
//    public void loadOnlineUsers(){
//        ArrayList<TLRPC.Dialog> users = MessagesController.getInstance(currentAccount).dialogsUsersOnly;
//        for (int a = 0; a < users.size(); a++){
//            TLRPC.Dialog dialogUser = users.get(a);
//            if(dialogUser == null){
//                continue;
//            }
//            TLRPC.User user =MessagesController.getInstance(currentAccount).getUser((int)dialogUser.id);
//            if(user == null){
//                continue;
//            }
//            if(!checkOnline(user)){
//                continue;
//            }
//            if(contactsMap.indexOfKey(user.id) != -1){
//                continue;
//            }
//            contacts.add(user);
//            contactsMap.put(user.id,user);
//        }
//
//        ArrayList<TLRPC.TL_contact> onlineContacts =  ContactsController.getInstance(currentAccount).contacts;
//        for(int a = 0; a < onlineContacts.size();a++){
//            TLRPC.TL_contact contact = onlineContacts.get(a);
//            if(contact == null){
//                continue;
//            }
//            if(contactsMap.indexOfKey(contact.user_id) != -1){
//                continue;
//            }
//            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(contact.user_id);
//            if(user == null){
//                continue;
//            }
//            if(!checkOnline(user)){
//                continue;
//            }
//            contacts.add(user);
//            contactsMap.put(user.id,user);
//        }
//
//
//        if(listViewAdapter != null){
//            listViewAdapter.notifyDataSetChanged();
//        }
//    }
//
//
//    public OnlineUserAlert(Context context, boolean needFocus, int account) {
//        super(context, needFocus, account);
//    }
//
//    public interface GroupVoipInviteAlertDelegate {
//        void copyInviteLink();
//        void inviteUser(int id);
//        void needOpenSearch(MotionEvent ev, EditTextBoldCursor editText);
//    }
//
//    @Override
//    protected void updateColorKeys() {
//        keyScrollUp = Theme.key_voipgroup_scrollUp;
//        keyListSelector = Theme.key_voipgroup_listSelector;
//        keySearchBackground = Theme.key_voipgroup_searchBackground;
//        keyInviteMembersBackground = Theme.key_voipgroup_inviteMembersBackground;
//        keyListViewBackground = Theme.key_voipgroup_listViewBackground;
//        keyActionBarUnscrolled = Theme.key_voipgroup_actionBarUnscrolled;
//        keyNameText = Theme.key_voipgroup_nameText;
//        keyLastSeenText = Theme.key_voipgroup_lastSeenText;
//        keyLastSeenTextUnscrolled = Theme.key_voipgroup_lastSeenTextUnscrolled;
//        keySearchPlaceholder = Theme.key_voipgroup_searchPlaceholder;
//        keySearchText = Theme.key_voipgroup_searchText;
//        keySearchIcon = Theme.key_voipgroup_mutedIcon;
//        keySearchIconUnscrolled = Theme.key_voipgroup_mutedIconUnscrolled;
//    }
//
//    public OnlineUserAlert(final Context context, int account) {
//        super(context, false, account);
//
//        setDimBehindAlpha(75);
//
//
//        listView.setOnItemClickListener((view, position) -> {
//            if (view instanceof ManageChatUserCell) {
//                ManageChatUserCell cell = (ManageChatUserCell) view;
//                delegate.inviteUser(cell.getUserId());
//            }
//        });
//        searchListViewAdapter = searchAdapter = new SearchAdapter(context);
//        listView.setAdapter(listViewAdapter = new ListAdapter(context));
//        loadChatParticipants();
//        setColorProgress(0.0f);
//    }
//
//    public void setDelegate(GroupVoipInviteAlertDelegate groupVoipInviteAlertDelegate) {
//        delegate = groupVoipInviteAlertDelegate;
//    }
//
//    private void updateRows() {
//        emptyRow = -1;
//        contactsStartRow = -1;
//        contactsEndRow = -1;
//        lastRow = -1;
//
//        rowCount = 0;
//        emptyRow = rowCount++;
//        if (!contacts.isEmpty()) {
//            contactsStartRow = rowCount;
//            rowCount += contacts.size();
//            contactsEndRow = rowCount;
//        }
//        if (loadingUsers) {
//            flickerProgressRow = rowCount++;
//        }
//        lastRow = rowCount++;
//    }
//
//    private void loadChatParticipants() {
//        loadOnlineUsers();
//        updateRows();
//        if (listViewAdapter != null) {
//            listViewAdapter.notifyDataSetChanged();
//            if (emptyView != null && listViewAdapter.getItemCount() == 0 && firstLoaded) {
//                emptyView.showProgress(false, true);
//            }
//        }
//    }
//
//    private class SearchAdapter extends RecyclerListView.SelectionAdapter {
//
//        private Context mContext;
//        private SearchAdapterHelper searchAdapterHelper;
//        private Runnable searchRunnable;
//        private int totalCount;
//
//        private boolean searchInProgress;
//
//        private int lastSearchId;
//
//        private int emptyRow;
//        private int lastRow;
//        private int groupStartRow;
//        private int globalStartRow;
//
//        public SearchAdapter(Context context) {
//            mContext = context;
//            searchAdapterHelper = new SearchAdapterHelper(true);
//            searchAdapterHelper.setDelegate(new SearchAdapterHelper.SearchAdapterHelperDelegate() {
//                @Override
//                public void onDataSetChanged(int searchId) {
//                    if (searchId < 0 || searchId != lastSearchId || searchInProgress) {
//                        return;
//                    }
//                    int oldItemCount = getItemCount() - 1;
//                    boolean emptyViewWasVisible = emptyView.getVisibility() == View.VISIBLE;
//                    notifyDataSetChanged();
//                    if (getItemCount() > oldItemCount) {
//                        showItemsAnimated(oldItemCount);
//                    }
//                    if (!searchAdapterHelper.isSearchInProgress()) {
//                        if (listView.isemptyViewIsVisible()) {
//                            emptyView.showProgress(false, emptyViewWasVisible);
//                        }
//                    }
//                }
//
//                @Override
//                public SparseArray<TLRPC.TL_groupCallParticipant> getExcludeCallParticipants() {
//                    return new SparseArray<>();
//                }
//            });
//        }
//
//        public void searchUsers(final String query) {
//            if (searchRunnable != null) {
//                AndroidUtilities.cancelRunOnUIThread(searchRunnable);
//                searchRunnable = null;
//            }
//            searchAdapterHelper.mergeResults(null);
//            searchAdapterHelper.queryServerSearch(null, true, false, true, false, false, currentChat.id, false, ChatUsersActivity.TYPE_USERS, -1);
//
//            if (!TextUtils.isEmpty(query)) {
//                emptyView.showProgress(true, true);
//                listView.setAnimateEmptyView(false, 0);
//                notifyDataSetChanged();
//                listView.setAnimateEmptyView(true, 0);
//                searchInProgress = true;
//                int searchId = ++lastSearchId;
//                AndroidUtilities.runOnUIThread(searchRunnable = () -> {
//                    if (searchRunnable == null) {
//                        return;
//                    }
//                    searchRunnable = null;
//                    processSearch(query, searchId);
//                }, 300);
//
//                if (listView.getAdapter() != searchListViewAdapter) {
//                    listView.setAdapter(searchListViewAdapter);
//                }
//            } else {
//                lastSearchId = -1;
//            }
//        }
//
//        private void processSearch(final String query, int searchId) {
//            AndroidUtilities.runOnUIThread(() -> {
//                searchRunnable = null;
//
//                final ArrayList<TLObject> participantsCopy = !ChatObject.isChannel(currentChat) && info != null ? new ArrayList<>(info.participants.participants) : null;
//
//                if (participantsCopy != null) {
//                    Utilities.searchQueue.postRunnable(() -> {
//                        String search1 = query.trim().toLowerCase();
//                        if (search1.length() == 0) {
//                            updateSearchResults(new ArrayList<>(), searchId);
//                            return;
//                        }
//                        String search2 = LocaleController.getInstance().getTranslitString(search1);
//                        if (search1.equals(search2) || search2.length() == 0) {
//                            search2 = null;
//                        }
//                        String[] search = new String[1 + (search2 != null ? 1 : 0)];
//                        search[0] = search1;
//                        if (search2 != null) {
//                            search[1] = search2;
//                        }
//                        ArrayList<TLObject> resultArray2 = new ArrayList<>();
//
//                        if (participantsCopy != null) {
//                            for (int a = 0, N = participantsCopy.size(); a < N; a++) {
//                                int userId;
//                                TLObject o = participantsCopy.get(a);
//                                if (o instanceof TLRPC.ChatParticipant) {
//                                    userId = ((TLRPC.ChatParticipant) o).user_id;
//                                } else if (o instanceof TLRPC.ChannelParticipant) {
//                                    userId = MessageObject.getPeerId(((TLRPC.ChannelParticipant) o).peer);
//                                } else {
//                                    continue;
//                                }
//                                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
//                                if (UserObject.isUserSelf(user)) {
//                                    continue;
//                                }
//
//                                String name = UserObject.getUserName(user).toLowerCase();
//                                String tName = LocaleController.getInstance().getTranslitString(name);
//                                if (name.equals(tName)) {
//                                    tName = null;
//                                }
//
//                                int found = 0;
//                                for (String q : search) {
//                                    if (name.startsWith(q) || name.contains(" " + q) || tName != null && (tName.startsWith(q) || tName.contains(" " + q))) {
//                                        found = 1;
//                                    } else if (user.username != null && user.username.startsWith(q)) {
//                                        found = 2;
//                                    }
//
//                                    if (found != 0) {
//                                        resultArray2.add(o);
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                        updateSearchResults(resultArray2, searchId);
//                    });
//                } else {
//                    searchInProgress = false;
//                }
//                searchAdapterHelper.queryServerSearch(query, ChatObject.canAddUsers(currentChat), false, true, false, false, ChatObject.isChannel(currentChat) ? currentChat.id : 0, false, ChatUsersActivity.TYPE_USERS, searchId);
//            });
//        }
//
//        private void updateSearchResults(final ArrayList<TLObject> participants, int searchId) {
//            AndroidUtilities.runOnUIThread(() -> {
//                if (searchId != lastSearchId) {
//                    return;
//                }
//                searchInProgress = false;
//                if (!ChatObject.isChannel(currentChat)) {
//                    searchAdapterHelper.addGroupMembers(participants);
//                }
//                int oldItemCount = getItemCount() - 1;
//                boolean emptyViewWasVisible = emptyView.getVisibility() == View.VISIBLE;
//                notifyDataSetChanged();
//                if (getItemCount() > oldItemCount) {
//                    showItemsAnimated(oldItemCount);
//                }
//                if (!searchInProgress && !searchAdapterHelper.isSearchInProgress()) {
//                    if (listView.isemptyViewIsVisible()) {
//                        emptyView.showProgress(false, emptyViewWasVisible);
//                    }
//                }
//            });
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder) {
//            if (holder.itemView instanceof ManageChatUserCell) {
//                ManageChatUserCell cell = (ManageChatUserCell) holder.itemView;
//            }
//            return holder.getItemViewType() == 0;
//        }
//
//        @Override
//        public int getItemCount() {
//            return totalCount;
//        }
//
//        @Override
//        public void notifyDataSetChanged() {
//            totalCount = 0;
//            emptyRow = totalCount++;
//            int count = searchAdapterHelper.getGroupSearch().size();
//            if (count != 0) {
//                groupStartRow = totalCount;
//                totalCount += count + 1;
//            } else {
//                groupStartRow = -1;
//            }
//            count = searchAdapterHelper.getGlobalSearch().size();
//            if (count != 0) {
//                globalStartRow = totalCount;
//                totalCount += count + 1;
//            } else {
//                globalStartRow = -1;
//            }
//            lastRow = totalCount++;
//            super.notifyDataSetChanged();
//        }
//
//        public TLObject getItem(int i) {
//            if (groupStartRow >= 0 && i > groupStartRow && i < groupStartRow + 1 + searchAdapterHelper.getGroupSearch().size()) {
//                return searchAdapterHelper.getGroupSearch().get(i - groupStartRow - 1);
//            }
//            if (globalStartRow >= 0 && i > globalStartRow && i < globalStartRow + 1 + searchAdapterHelper.getGlobalSearch().size()) {
//                return searchAdapterHelper.getGlobalSearch().get(i - globalStartRow - 1);
//            }
//            return null;
//        }
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view;
//            switch (viewType) {
//                case 0:
//                    ManageChatUserCell manageChatUserCell = new ManageChatUserCell(mContext, 2, 2, false);
//                    manageChatUserCell.setCustomRightImage(R.drawable.msg_invited);
//                    manageChatUserCell.setNameColor(Theme.getColor(Theme.key_voipgroup_nameText));
//                    manageChatUserCell.setStatusColors(Theme.getColor(Theme.key_voipgroup_lastSeenTextUnscrolled), Theme.getColor(Theme.key_voipgroup_listeningText));
//                    manageChatUserCell.setDividerColor(Theme.key_voipgroup_listViewBackground);
//                    view = manageChatUserCell;
//                    break;
//                case 1:
//                    GraySectionCell cell = new GraySectionCell(mContext);
//                    cell.setBackgroundColor(Theme.getColor(Theme.key_voipgroup_actionBarUnscrolled));
//                    cell.setTextColor(Theme.key_voipgroup_searchPlaceholder);
//                    view = cell;
//                    break;
//                case 2:
//                    view = new View(mContext);
//                    view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));
//                    break;
//                case 3:
//                default:
//                    view = new View(mContext);
//                    break;
//            }
//            return new RecyclerListView.Holder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//            switch (holder.getItemViewType()) {
//                case 0: {
//                    TLObject object = getItem(position);
//                    TLRPC.User user;
//                    if (object instanceof TLRPC.User) {
//                        user = (TLRPC.User) object;
//                    } else if (object instanceof TLRPC.ChannelParticipant) {
//                        user = MessagesController.getInstance(currentAccount).getUser(MessageObject.getPeerId(((TLRPC.ChannelParticipant) object).peer));
//                    } else if (object instanceof TLRPC.ChatParticipant) {
//                        user = MessagesController.getInstance(currentAccount).getUser(((TLRPC.ChatParticipant) object).user_id);
//                    } else {
//                        return;
//                    }
//
//                    String un = user.username;
//                    CharSequence username = null;
//                    SpannableStringBuilder name = null;
//
//                    int count = searchAdapterHelper.getGroupSearch().size();
//                    boolean ok = false;
//                    String nameSearch = null;
//                    if (count != 0) {
//                        if (count + 1 > position) {
//                            nameSearch = searchAdapterHelper.getLastFoundChannel();
//                            ok = true;
//                        } else {
//                            position -= count + 1;
//                        }
//                    }
//                    if (!ok && un != null) {
//                        count = searchAdapterHelper.getGlobalSearch().size();
//                        if (count != 0) {
//                            if (count + 1 > position) {
//                                String foundUserName = searchAdapterHelper.getLastFoundUsername();
//                                if (foundUserName.startsWith("@")) {
//                                    foundUserName = foundUserName.substring(1);
//                                }
//                                try {
//                                    int index;
//                                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
//                                    spannableStringBuilder.append("@");
//                                    spannableStringBuilder.append(un);
//                                    if ((index = AndroidUtilities.indexOfIgnoreCase(un, foundUserName)) != -1) {
//                                        int len = foundUserName.length();
//                                        if (index == 0) {
//                                            len++;
//                                        } else {
//                                            index++;
//                                        }
//                                        spannableStringBuilder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_voipgroup_listeningText)), index, index + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                                    }
//                                    username = spannableStringBuilder;
//                                } catch (Exception e) {
//                                    username = un;
//                                    FileLog.e(e);
//                                }
//                            }
//                        }
//                    }
//
//                    if (nameSearch != null) {
//                        String u = UserObject.getUserName(user);
//                        name = new SpannableStringBuilder(u);
//                        int idx = AndroidUtilities.indexOfIgnoreCase(u, nameSearch);
//                        if (idx != -1) {
//                            name.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_voipgroup_listeningText)), idx, idx + nameSearch.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                        }
//                    }
//
//                    ManageChatUserCell userCell = (ManageChatUserCell) holder.itemView;
//                    userCell.setTag(position);
//                    userCell.setCustomImageVisible(false);
//                    userCell.setData(user, name, username, false);
//
//                    break;
//                }
//                case 1: {
//                    GraySectionCell sectionCell = (GraySectionCell) holder.itemView;
//                    if (position == groupStartRow) {
//                        sectionCell.setText(LocaleController.getString("ChannelMembers", R.string.ChannelMembers));
//                    } else if (position == globalStartRow) {
//                        sectionCell.setText(LocaleController.getString("GlobalSearch", R.string.GlobalSearch));
//                    }
//                    break;
//                }
//            }
//        }
//
//        @Override
//        public void onViewRecycled(RecyclerView.ViewHolder holder) {
//            if (holder.itemView instanceof ManageChatUserCell) {
//                ((ManageChatUserCell) holder.itemView).recycle();
//            }
//        }
//
//        @Override
//        public int getItemViewType(int i) {
//            if (i == emptyRow) {
//                return 2;
//            } else if (i == lastRow) {
//                return 3;
//            }
//            if (i == globalStartRow || i == groupStartRow) {
//                return 1;
//            }
//            return 0;
//        }
//    }
//
//    private class ListAdapter extends RecyclerListView.SelectionAdapter {
//
//        private Context mContext;
//
//        public ListAdapter(Context context) {
//            mContext = context;
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder) {
//            if (holder.itemView instanceof ManageChatUserCell) {
//                ManageChatUserCell cell = (ManageChatUserCell) holder.itemView;
//
//            }
//            int viewType = holder.getItemViewType();
//            return viewType == 0 || viewType == 1;
//        }
//
//        @Override
//        public int getItemCount() {
//            return rowCount;
//        }
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view;
//            switch (viewType) {
//                case 0:
//                    ManageChatUserCell manageChatUserCell = new ManageChatUserCell(mContext, 6, 2, false);
//                    manageChatUserCell.setCustomRightImage(R.drawable.msg_invited);
//                    manageChatUserCell.setNameColor(Theme.getColor(Theme.key_voipgroup_nameText));
//                    manageChatUserCell.setStatusColors(Theme.getColor(Theme.key_voipgroup_lastSeenTextUnscrolled), Theme.getColor(Theme.key_voipgroup_listeningText));
//                    manageChatUserCell.setDividerColor(Theme.key_voipgroup_actionBar);
//                    view = manageChatUserCell;
//                    break;
//                case 1:
//                    ManageChatTextCell manageChatTextCell = new ManageChatTextCell(mContext);
//                    manageChatTextCell.setColors(Theme.key_voipgroup_listeningText, Theme.key_voipgroup_listeningText);
//                    manageChatTextCell.setDividerColor(Theme.key_voipgroup_actionBar);
//                    view = manageChatTextCell;
//                    break;
//                case 2:
//                    GraySectionCell cell = new GraySectionCell(mContext);
//                    cell.setBackgroundColor(Theme.getColor(Theme.key_voipgroup_actionBarUnscrolled));
//                    cell.setTextColor(Theme.key_voipgroup_searchPlaceholder);
//                    view = cell;
//                    break;
//                case 3:
//                    view = new View(mContext);
//                    view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));
//                    break;
//                case 5:
//                    FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext);
//                    flickerLoadingView.setViewType(FlickerLoadingView.USERS_TYPE);
//                    flickerLoadingView.setIsSingleCell(true);
//                    flickerLoadingView.setColors(Theme.key_voipgroup_inviteMembersBackground, Theme.key_voipgroup_searchBackground, Theme.key_voipgroup_actionBarUnscrolled);
//                    view = flickerLoadingView;
//                    break;
//                case 4:
//                default:
//                    view = new View(mContext);
//                    break;
//            }
//            return new RecyclerListView.Holder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//            switch (holder.getItemViewType()) {
//                case 0:
//                    ManageChatUserCell userCell = (ManageChatUserCell) holder.itemView;
//                    userCell.setTag(position);
//                    TLObject item = getItem(position);
//                    if(item instanceof TLRPC.User){
//                        TLRPC.User user = (TLRPC.User) item;
//                        userCell.setCustomImageVisible(false);
//                        userCell.setData(user, null, null, position != lastRow - 1);
//                    }
//                    break;
//                case 2:
//                    GraySectionCell sectionCell = (GraySectionCell) holder.itemView;
//                    break;
//            }
//        }
//
//        @Override
//        public void onViewRecycled(RecyclerView.ViewHolder holder) {
//            if (holder.itemView instanceof ManageChatUserCell) {
//                ((ManageChatUserCell) holder.itemView).recycle();
//            }
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            if (position >= contactsStartRow && position < contactsEndRow) {
//                return 0;
//            } if (position == emptyRow) {
//                return 3;
//            } else if (position == lastRow) {
//                return 4;
//            } else if (position == flickerProgressRow) {
//                return 5;
//            }
//            return 0;
//        }
//
//        public TLObject getItem(int position) {
//           if (position >= contactsStartRow && position < contactsEndRow) {
//                return contacts.get(position - contactsStartRow);
//            }
//            return null;
//        }
//    }
//
//    @Override
//    protected void search(String text) {
//        searchAdapter.searchUsers(text);
//    }
//
//    @Override
//    protected void onSearchViewTouched(MotionEvent ev, EditTextBoldCursor searchEditText) {
//        delegate.needOpenSearch(ev, searchEditText);
//    }
//}
