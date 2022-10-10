package org.master.feature.feed;

import android.text.TextUtils;
import android.util.SparseArray;

import org.checkerframework.checker.units.qual.A;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.SwipeGestureSettingsView;

import java.util.ArrayList;
import java.util.Collections;

public class FeedManager extends BaseController{
    public static String[] suggested_filter = {
            "News",
            "Politics",
            "Entertainment",
            "Sports",
            "Technology",
            "Economy",
    };
    public static String[] suggested_filter_emoticon = {
            "\uD83D\uDDDE",
            "\uD83D\uDCA9",
            "\uD83E\uDD73",
            "⚽️",
            "\uD83E\uDDD1\u200D\uD83D\uDCBB",
            "\uD83D\uDCC8",
    };
    public ArrayList<FeedFilter> feedFilters = new ArrayList<>();
    public SparseArray<FeedFilter> feedFiltersById = new SparseArray<>();
    private boolean loadingSuggestedFilters;
    public boolean feedFiltersLoaded;
    public ArrayList<FeedFilter> suggestedFilters = new ArrayList<>();

    private static volatile FeedManager[] Instance = new FeedManager[UserConfig.MAX_ACCOUNT_COUNT];



    public SparseArray<ArrayList<Long>> dialogsByFeedFilter = new SparseArray<>();



    public static FeedManager getInstance(int num) {
        FeedManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (FeedManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new FeedManager(num);
                }
            }
        }
        return localInstance;
    }

    public FeedManager(int account){
        super(account);
    }

    public void loadSuggestedFilters() {
        if (loadingSuggestedFilters) {
            return;
        }
        loadingSuggestedFilters = true;
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                loadingSuggestedFilters = false;
                suggestedFilters.clear();
//                for(int a = 0; a < suggested_filter.length;a++){
//                    FeedFilter filter = new FeedFilter();
//                    filter.id = a;
//                    filter.order = a;
//                    filter.title = suggested_filter[a];
//                    filter.emoticon = suggested_filter_emoticon[a];
//                    suggestedFilters.add(filter);
//
//                }
//                AndroidUtilities.runOnUIThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        getNotificationCenter().postNotificationName(NotificationCenter.suggestedFeedFiltersLoaded);
//
//                    }
//                });

            }
        },400);
    }

    public void addFilter(FeedFilter filter, boolean atBegin) {
        if (atBegin) {
            int order = 254;
            for (int a = 0, N = feedFilters.size(); a < N; a++) {
                order = Math.min(order, feedFilters.get(a).order);
            }
            filter.order = order - 1;
            feedFilters.add(0, filter);
        } else {
            int order = 0;
            for (int a = 0, N = feedFilters.size(); a < N; a++) {
                order = Math.max(order, feedFilters.get(a).order);
            }
            filter.order = order + 1;
            feedFilters.add(filter);
        }
        feedFiltersById.put(filter.id, filter);

    }
    public FeedFilter [] selectedDialogFilter = new FeedFilter[2];

    public void onFilterUpdate(FeedFilter filter) {
        for (int a = 0; a < 2; a++) {
            if (selectedDialogFilter[a] == filter) {
                getMessagesController().sortDialogs(null);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload, true);
                break;
            }
        }
    }

    public void selectFeedFilter(FeedFilter filter, int index) {
        if (selectedDialogFilter[index] == filter) {
            return;
        }
        FeedFilter prevFilter = selectedDialogFilter[index];
        selectedDialogFilter[index] = filter;
        if (selectedDialogFilter[index == 0 ? 1 : 0] == filter) {
            selectedDialogFilter[index == 0 ? 1 : 0] = null;
        }
        if (selectedDialogFilter[index] == null) {
            if (prevFilter != null) {
                prevFilter.feedDialogsId.clear();
            }
        } else {
            getMessagesController().sortDialogs(null);
        }
    }

    public void removeFilter(FeedFilter filter) {
        feedFilters.remove(filter);
        feedFiltersById.remove(filter.id);
       // getNotificationCenter().postNotificationName(NotificationCenter.feedFiltersUpdated);
    }


    public void loadFeedFilters() {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                ArrayList<FeedFilter> feedFiltersLocal = new ArrayList<>();
                SparseArray<FeedFilter> feedFiltersByIdLocal = new SparseArray<>();
                SparseArray<FeedFilter> filtersById = new SparseArray<>();
                ArrayList<Long> chatsToLoad = new ArrayList<>();
                SQLiteCursor filtersCursor = getMessagesStorage().getDatabase().queryFinalized("SELECT id, ord, title, emoticon FROM feed_filter WHERE 1");

                while (filtersCursor.next()) {
                    FeedFilter filter = new FeedFilter();
                    filter.id = filtersCursor.intValue(0);
                    filter.order = filtersCursor.intValue(1);
                    filter.title = filtersCursor.stringValue(2);
                    filter.emoticon = filtersCursor.stringValue(3);
                    feedFiltersLocal.add(filter);
                    feedFiltersByIdLocal.put(filter.id, filter);
                    filtersById.put(filter.id, filter);
                    SQLiteCursor cursor2 = getMessagesStorage().getDatabase().queryFinalized("SELECT peer FROM feed_filter_ep WHERE id = " + filter.id);
                    while (cursor2.next()) {
                        long did = cursor2.longValue(0);
                        if (DialogObject.isChatDialog(did)) {
                            if (!chatsToLoad.contains(-did)) {
                                 chatsToLoad.add(-did);
                            }
                        }
                    }
                    filter.feedDialogsId = chatsToLoad;
                    cursor2.dispose();
                }
                filtersCursor.dispose();

                Collections.sort(feedFilters, (o1, o2) -> {
                    if (o1.order > o2.order) {
                        return 1;
                    } else if (o1.order < o2.order) {
                        return -1;
                    }
                    return 0;
                });
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                if (!chatsToLoad.isEmpty()) {
                    getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                }

                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        feedFilters = feedFiltersLocal;
                        feedFiltersById = feedFiltersByIdLocal;
                        getNotificationCenter().postNotificationName(NotificationCenter.feedFiltersUpdated);
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void saveFeedFilter(FeedFilter feedFilter, boolean atBegin, boolean peers) {
        getMessagesStorage().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!feedFilters.contains(feedFilter)) {
                        if (atBegin) {
                            feedFilters.add(0, feedFilter);
                        } else {
                            feedFilters.add(feedFilter);
                        }
                        feedFiltersById.put(feedFilter.id, feedFilter);
                    }

                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO feed_filter VALUES(?, ?, ?, ?)");
                    state.bindInteger(1, feedFilter.id);
                    state.bindInteger(2, feedFilter.order);
                    state.bindString(3, feedFilter.title);
                    if (feedFilter.emoticon != null) {
                        state.bindString(4, feedFilter.emoticon);
                    } else {
                        state.bindNull(4);
                    }
                    state.step();
                    state.dispose();
                    if (peers) {
                        getMessagesStorage().getDatabase().executeFast("DELETE FROM feed_filter_ep WHERE id = " + feedFilter.id).stepThis().dispose();
                        getMessagesStorage().getDatabase().beginTransaction();
                        state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO feed_filter_ep VALUES(?, ?)");
                        for (int a = 0, N = feedFilter.feedDialogsId.size(); a < N; a++) {
                            state.requery();
                            state.bindInteger(1, feedFilter.id);
                            state.bindLong(2, feedFilter.feedDialogsId.get(a));
                            state.step();
                        }
                        state.dispose();
                        getMessagesStorage().getDatabase().commitTransaction();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });

    }

    private void deleteFeedFilterInternal(FeedFilter filter) {
        try {
            feedFilters.remove(filter);
            feedFiltersById.remove(filter.id);
            getMessagesStorage().getDatabase().executeFast("DELETE FROM filter_filter WHERE id = " + filter.id).stepThis().dispose();
            getMessagesStorage().getDatabase().executeFast("DELETE FROM filter_filter_ep WHERE id = " + filter.id).stepThis().dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void deleteFeedFilter(FeedFilter filter) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> deleteFeedFilterInternal(filter));
    }


    public void saveFeedFiltersOrderInternal() {
        try {
            SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("UPDATE feed_filter SET ord = ? WHERE id = ?");
            for (int a = 0, N = feedFilters.size(); a < N; a++) {
                FeedFilter filter = feedFilters.get(a);
                state.requery();
                state.bindInteger(1, filter.order);
                state.bindInteger(2, filter.id);
                state.step();
            }
            state.dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void saveFeedFiltersOrder() {
        getMessagesStorage().getStorageQueue().postRunnable(this::saveFeedFiltersOrderInternal);
    }

    public void clear(){
        feedFilters.clear();
        feedFiltersById.clear();
        loadingSuggestedFilters = false;
        feedFiltersLoaded = false;
        suggestedFilters.clear();
    }

}
