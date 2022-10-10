package org.master.video;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.FiltersView;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SearchViewPager;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FilteredSearchView;
import org.telegram.ui.PhotoViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class VideoFeedFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public RecyclerListView recyclerListView;
    StickerEmptyView emptyView;
    RecyclerListView.Adapter adapter;

    Runnable searchRunnable;

    public ArrayList<MessageObject> messages = new ArrayList<>();
    public SparseArray<MessageObject> messagesById = new SparseArray<>();
    public ArrayList<String> sections = new ArrayList<>();
    public HashMap<String, ArrayList<MessageObject>> sectionArrays = new HashMap<>();

    private int columnsCount = 2;
    private int nextSearchRate;
    String lastMessagesSearchString;
    String lastSearchFilterQueryString;

    FiltersView.MediaFilterData currentSearchFilter;
    long currentSearchDialogId;
    long currentSearchMaxDate;
    long currentSearchMinDate;
    String currentSearchString;
    boolean currentIncludeFolder;

    private boolean isLoading;
    private boolean endReached;
    private int totalCount;
    private int requestIndex;

    private String currentDataQuery;

    private static SpannableStringBuilder arrowSpan;
    private SharedPhotoVideoAdapter sharedPhotoVideoAdapter;

    private int searchIndex;

    private boolean isActionModeShowed;

    ArrayList<Object> localTipChats = new ArrayList<>();
    ArrayList<FiltersView.DateData> localTipDates = new ArrayList<>();
    boolean localTipArchive;


    private HashMap<FilteredSearchView.MessageHashId, MessageObject> selectedFiles = new HashMap<>();


    Runnable clearCurrentResultsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLoading) {
                messages.clear();
                sections.clear();
                sectionArrays.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };
    private PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public int getTotalImageCount() {
            return totalCount;
        }

        @Override
        public boolean loadMore() {
            if (!endReached) {
                search(currentSearchDialogId, currentSearchMinDate, currentSearchMaxDate, currentSearchFilter, currentIncludeFolder, lastMessagesSearchString, false);
            }
            return true;
        }

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview) {
            if (messageObject == null) {
                return null;
            }
            final RecyclerListView listView = recyclerListView;
            for (int a = 0, count = listView.getChildCount(); a < count; a++) {
                View view = listView.getChildAt(a);
                int[] coords = new int[2];
                ImageReceiver imageReceiver = null;
                if (view instanceof SharedPhotoVideoCell) {
                    SharedPhotoVideoCell cell = (SharedPhotoVideoCell) view;
                    for (int i = 0; i < 6; i++) {
                        MessageObject message = cell.getMessageObject(i);
                        if (message == null) {
                            break;
                        }
                        if (message.getId() == messageObject.getId()) {
                            BackupImageView imageView = cell.getImageView(i);
                            imageReceiver = imageView.getImageReceiver();
                            imageView.getLocationInWindow(coords);
                        }
                    }
                }
                if (imageReceiver != null) {
                    PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                    object.viewX = coords[0];
                    object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                    object.parentView = listView;
                    listView.getLocationInWindow(coords);
                    object.animatingImageViewYOffset = -coords[1];
                    object.imageReceiver = imageReceiver;
                    object.allowTakeAnimation = false;
                    object.radius = object.imageReceiver.getRoundRadius();
                    object.thumb = object.imageReceiver.getBitmapSafe();
                    object.parentView.getLocationInWindow(coords);
                    object.clipTopAddition = 0;

                    if (PhotoViewer.isShowingImage(messageObject)) {
                        final View pinnedHeader = listView.getPinnedHeader();
                        if (pinnedHeader != null) {
                            int top = 0;
                            final int topOffset = (int) (top - object.viewY);
                            if (topOffset > view.getHeight()) {
                                listView.scrollBy(0, -(topOffset + pinnedHeader.getHeight()));
                            } else {
                                int bottomOffset = (int) (object.viewY - listView.getHeight());
                                if (bottomOffset >= 0) {
                                    listView.scrollBy(0, bottomOffset + view.getHeight());
                                }
                            }
                        }
                    }

                    return object;
                }
            }
            return null;
        }

        @Override
        public CharSequence getTitleFor(int i) {
            return createFromInfoString(messages.get(i));
        }

        @Override
        public CharSequence getSubtitleFor(int i) {
            return LocaleController.formatDateAudio(messages.get(i).messageOwner.date, false);
        }
    };

    private FilteredSearchView.Delegate delegate;
    private SearchViewPager.ChatPreviewDelegate chatPreviewDelegate;
    public  LinearLayoutManager layoutManager;
    private  FlickerLoadingView loadingView;
    private boolean firstLoading = true;
    private int animationIndex = -1;
    public int keyboardHeight;
    private  ChatActionCell floatingDateView;

    private AnimatorSet floatingDateAnimation;
    private Runnable hideFloatingDateRunnable = () -> hideFloatingDateView(true);

    private final FilteredSearchView.MessageHashId messageHashIdTmp = new FilteredSearchView.MessageHashId(0, 0);


    private BackDrawable backDrawable;
    @Override
    public View createView(Context context) {
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                if(id == -1){
                    finishFragment();
                }
            }
        });

        actionBar.setBackButtonDrawable(backDrawable = new BackDrawable(false));
        actionBar.setTitle("Video Feed");
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout)fragmentView;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        recyclerListView = new BlurredRecyclerView(context) {

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (getAdapter() == sharedPhotoVideoAdapter) {
                    for (int i = 0; i < getChildCount(); i++) {
                        if (getChildViewHolder(getChildAt(i)).getItemViewType() == 1) {
                            canvas.save();
                            canvas.translate(getChildAt(i).getX(), getChildAt(i).getY() - getChildAt(i).getMeasuredHeight() + AndroidUtilities.dp(2));
                            getChildAt(i).draw(canvas);
                            canvas.restore();
                            invalidate();
                        }
                    }
                }
                super.dispatchDraw(canvas);
            }

            @Override
            public boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (getAdapter() == sharedPhotoVideoAdapter) {
                    if (getChildViewHolder(child).getItemViewType() == 1) {
                        return true;
                    }
                }
                return super.drawChild(canvas, child, drawingTime);
            }
        };
        recyclerListView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }
        });
        recyclerListView.setPadding(0, 0, 0, AndroidUtilities.dp(3));
        layoutManager = new LinearLayoutManager(context);
        recyclerListView.setLayoutManager(layoutManager);
        frameLayout.addView(loadingView = new FlickerLoadingView(context) {
            @Override
            public int getColumnsCount() {
                return columnsCount;
            }
        }, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        frameLayout.addView(recyclerListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));

        recyclerListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (recyclerView.getAdapter() == null || adapter == null) {
                    return;
                }
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                int visibleItemCount = Math.abs(lastVisibleItem - firstVisibleItem) + 1;
                int totalItemCount = recyclerView.getAdapter().getItemCount();
                if (!isLoading && visibleItemCount > 0 && lastVisibleItem >= totalItemCount - 10 && !endReached) {
                    AndroidUtilities.runOnUIThread(() -> {
                        search(currentSearchDialogId, currentSearchMinDate, currentSearchMaxDate, currentSearchFilter, currentIncludeFolder, lastMessagesSearchString, false);
                    });
                }

                if (adapter == sharedPhotoVideoAdapter) {
                    if (dy != 0 && !messages.isEmpty() && TextUtils.isEmpty(currentDataQuery)) {
                        showFloatingDateView();
                    }
                    RecyclerListView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(firstVisibleItem);
                    if (holder != null && holder.getItemViewType() == 0) {
                        if (holder.itemView instanceof SharedPhotoVideoCell) {
                            SharedPhotoVideoCell cell = (SharedPhotoVideoCell) holder.itemView;
                            MessageObject messageObject = cell.getMessageObject(0);
                            if (messageObject != null) {
                                floatingDateView.setCustomDate(messageObject.messageOwner.date, false, true);
                            }
                        }
                    }
                }
            }
        });

        floatingDateView = new ChatActionCell(context);
        floatingDateView.setCustomDate((int) (System.currentTimeMillis() / 1000), false, false);
        floatingDateView.setAlpha(0.0f);
        floatingDateView.setOverrideColor(Theme.key_chat_mediaTimeBackground, Theme.key_chat_mediaTimeText);
        floatingDateView.setTranslationY(-AndroidUtilities.dp(48));
        frameLayout.addView(floatingDateView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        sharedPhotoVideoAdapter = new SharedPhotoVideoAdapter(context);
        emptyView = new StickerEmptyView(context, loadingView, StickerEmptyView.STICKER_TYPE_SEARCH);
        frameLayout.addView(emptyView,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        recyclerListView.setEmptyView(emptyView);
        emptyView.setVisibility(View.GONE);
        search(currentSearchDialogId, currentSearchMinDate, currentSearchMaxDate, currentSearchFilter, currentIncludeFolder, lastMessagesSearchString, false);
        return fragmentView;
    }



    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    public void onActionBarItemClick(int id) {
        if (id == deleteItemId) {
            if (getParentActivity() == null) {
                return;
            }
            ArrayList<MessageObject> messageObjects = new ArrayList<>(selectedFiles.values());
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.formatPluralString("RemoveDocumentsTitle", selectedFiles.size()));

            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder
                    .append(AndroidUtilities.replaceTags(LocaleController.formatPluralString("RemoveDocumentsMessage", selectedFiles.size())))
                    .append("\n\n")
                    .append(LocaleController.getString("RemoveDocumentsAlertMessage", R.string.RemoveDocumentsAlertMessage));

            builder.setMessage(spannableStringBuilder);
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> dialogInterface.dismiss());
            builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {
                dialogInterface.dismiss();
                getDownloadController().deleteRecentFiles(messageObjects);
                showActionMode(false);
            });
            AlertDialog alertDialog = builder.show();
            TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
            }

        } else if (id == gotoItemId) {
            if (selectedFiles.size() != 1) {
                return;
            }
            MessageObject messageObject = selectedFiles.values().iterator().next();
            goToMessage(messageObject);
        } else if (id == forwardItemId) {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putInt("dialogsType", 3);
            DialogsActivity fragment = new DialogsActivity(args);
            fragment.setDelegate((fragment1, dids, message, param) -> {
                ArrayList<MessageObject> fmessages = new ArrayList<>();
                Iterator<FilteredSearchView.MessageHashId> idIterator = selectedFiles.keySet().iterator();
                while (idIterator.hasNext()) {
                    FilteredSearchView.MessageHashId hashId = idIterator.next();
                    fmessages.add(selectedFiles.get(hashId));
                }
                selectedFiles.clear();

                showActionMode(false);

                if (dids.size() > 1 || dids.get(0) == AccountInstance.getInstance(currentAccount).getUserConfig().getClientUserId() || message != null) {
                    for (int a = 0; a < dids.size(); a++) {
                        long did = dids.get(a);
                        if (message != null) {
                            AccountInstance.getInstance(currentAccount).getSendMessagesHelper().sendMessage(message.toString(), did, null, null, null, true, null, null, null, true, 0, null,false);
                        }
                        AccountInstance.getInstance(currentAccount).getSendMessagesHelper().sendMessage(fmessages, did, false,false, true, 0);
                    }
                    fragment1.finishFragment();
                } else {
                    long did = dids.get(0);
                    Bundle args1 = new Bundle();
                    args1.putBoolean("scrollToTopOnResume", true);
                    if (DialogObject.isEncryptedDialog(did)) {
                        args1.putInt("enc_id", DialogObject.getEncryptedChatId(did));
                    } else {
                        if (DialogObject.isUserDialog(did)) {
                            args1.putLong("user_id", did);
                        } else {
                            args1.putLong("chat_id", -did);
                        }
                        if (!AccountInstance.getInstance(currentAccount).getMessagesController().checkCanOpenChat(args1, fragment1)) {
                            return;
                        }
                    }
                    ChatActivity chatActivity = new ChatActivity(args1);
                    fragment1.presentFragment(chatActivity, true);
                    chatActivity.showFieldPanelForForward(true, fmessages);
                }
            });
            presentFragment(fragment);
        }
    }

    public void goToMessage(MessageObject messageObject) {
        Bundle args = new Bundle();
        long dialogId = messageObject.getDialogId();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
        } else if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else {
            TLRPC.Chat chat = AccountInstance.getInstance(currentAccount).getMessagesController().getChat(-dialogId);
            if (chat != null && chat.migrated_to != null) {
                args.putLong("migrated_to", dialogId);
                dialogId = -chat.migrated_to.channel_id;
            }
            args.putLong("chat_id", -dialogId);
        }
        args.putInt("message_id", messageObject.getId());
        presentFragment(new ChatActivity(args));
        showActionMode(false);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }

    public void search(long dialogId, long minDate, long maxDate, FiltersView.MediaFilterData currentSearchFilter, boolean includeFolder, String query, boolean clearOldResults) {
        String currentSearchFilterQueryString = String.format(Locale.ENGLISH, "%d%d%d%d%s%s", dialogId, minDate, maxDate, currentSearchFilter == null ? -1 : currentSearchFilter.filterType, query, includeFolder);
        boolean filterAndQueryIsSame = lastSearchFilterQueryString != null && lastSearchFilterQueryString.equals(currentSearchFilterQueryString);
        boolean forceClear = !filterAndQueryIsSame && clearOldResults;
        this.currentSearchFilter = currentSearchFilter;
        this.currentSearchDialogId = dialogId;
        this.currentSearchMinDate = minDate;
        this.currentSearchMaxDate = maxDate;
        this.currentSearchString = query;
        this.currentIncludeFolder = includeFolder;
        if (searchRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(searchRunnable);
        }
        AndroidUtilities.cancelRunOnUIThread(clearCurrentResultsRunnable);
        if (filterAndQueryIsSame && clearOldResults) {
            return;
        }
        if (forceClear || currentSearchFilter == null && dialogId == 0 && minDate == 0 && maxDate == 0) {
            messages.clear();
            sections.clear();
            sectionArrays.clear();
            isLoading = true;
            emptyView.setVisibility(View.VISIBLE);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            requestIndex++;
            firstLoading = true;
            if (recyclerListView.getPinnedHeader() != null) {
                recyclerListView.getPinnedHeader().setAlpha(0);
            }
            localTipChats.clear();
            localTipDates.clear();
            if (!forceClear) {
                return;
            }
        } else if (clearOldResults && !messages.isEmpty()) {
            return;
        }
        isLoading = true;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (!filterAndQueryIsSame) {
            clearCurrentResultsRunnable.run();
            emptyView.showProgress(true, !clearOldResults);
        }

        if (TextUtils.isEmpty(query)) {
            localTipDates.clear();
            localTipChats.clear();
            if (delegate != null) {
                delegate.updateFiltersView(false, null, null, false);
            }
        }
        requestIndex++;
        final int requestId = requestIndex;
        int currentAccount = UserConfig.selectedAccount;

        AndroidUtilities.runOnUIThread(searchRunnable = () -> {
            TLObject request;

            ArrayList<Object> resultArray = null;
            if (dialogId != 0) {
                final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
                req.q = query;
                req.limit = 20;
                req.filter = currentSearchFilter == null ? new TLRPC.TL_inputMessagesFilterEmpty() : currentSearchFilter.filter;
                req.peer = AccountInstance.getInstance(currentAccount).getMessagesController().getInputPeer(dialogId);
                if (minDate > 0) {
                    req.min_date = (int) (minDate / 1000);
                }
                if (maxDate > 0) {
                    req.max_date = (int) (maxDate / 1000);
                }
                if (filterAndQueryIsSame && query.equals(lastMessagesSearchString) && !messages.isEmpty()) {
                    MessageObject lastMessage = messages.get(messages.size() - 1);
                    req.offset_id = lastMessage.getId();
                } else {
                    req.offset_id = 0;
                }
                request = req;
            } else {
                if (!TextUtils.isEmpty(query)) {
                    resultArray = new ArrayList<>();
                    ArrayList<CharSequence> resultArrayNames = new ArrayList<>();
                    ArrayList<TLRPC.User> encUsers = new ArrayList<>();
                    MessagesStorage.getInstance(currentAccount).localSearch(0, query, resultArray, resultArrayNames, encUsers, includeFolder ? 1 : 0);
                }

                final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
                req.limit = 20;
                req.q = query;
                req.filter = currentSearchFilter == null ? new TLRPC.TL_inputMessagesFilterEmpty() : currentSearchFilter.filter;
                if (minDate > 0) {
                    req.min_date = (int) (minDate / 1000);
                }
                if (maxDate > 0) {
                    req.max_date = (int) (maxDate / 1000);
                }
                if (filterAndQueryIsSame && query.equals(lastMessagesSearchString) && !messages.isEmpty()) {
                    MessageObject lastMessage = messages.get(messages.size() - 1);
                    req.offset_id = lastMessage.getId();
                    req.offset_rate = nextSearchRate;
                    long id = MessageObject.getPeerId(lastMessage.messageOwner.peer_id);
                    req.offset_peer = MessagesController.getInstance(currentAccount).getInputPeer(id);
                } else {
                    req.offset_rate = 0;
                    req.offset_id = 0;
                    req.offset_peer = new TLRPC.TL_inputPeerEmpty();
                }
                req.flags |= 1;
                req.folder_id = includeFolder ? 1 : 0;
                request = req;
            }

            lastMessagesSearchString = query;
            lastSearchFilterQueryString = currentSearchFilterQueryString;

            ArrayList<Object> finalResultArray = resultArray;
            final ArrayList<FiltersView.DateData> dateData = new ArrayList<>();
            FiltersView.fillTipDates(lastMessagesSearchString, dateData);
            ConnectionsManager.getInstance(currentAccount).sendRequest(request, (response, error) -> {
                ArrayList<MessageObject> messageObjects = new ArrayList<>();
                if (error == null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    int n = res.messages.size();
                    for (int i = 0; i < n; i++) {
                        MessageObject messageObject = new MessageObject(currentAccount, res.messages.get(i), false, true);
                        messageObject.setQuery(query);
                        messageObjects.add(messageObject);
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    if (requestId != requestIndex) {
                        return;
                    }
                    isLoading = false;
                    if (error != null) {
                        emptyView.title.setText(LocaleController.getString("SearchEmptyViewTitle2", R.string.SearchEmptyViewTitle2));
                        emptyView.subtitle.setVisibility(View.VISIBLE);
                        emptyView.subtitle.setText(LocaleController.getString("SearchEmptyViewFilteredSubtitle2", R.string.SearchEmptyViewFilteredSubtitle2));
                        emptyView.showProgress(false, true);
                        return;
                    }

                    emptyView.showProgress(false);

                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    nextSearchRate = res.next_rate;
                    MessagesStorage.getInstance(currentAccount).putUsersAndChats(res.users, res.chats, true, true);
                    MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                    MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                    if (!filterAndQueryIsSame) {
                        messages.clear();
                        messagesById.clear();
                        sections.clear();
                        sectionArrays.clear();
                    }
                    totalCount = res.count;
                    currentDataQuery = query;
                    int n = messageObjects.size();
                    for (int i = 0; i < n; i++) {
                        MessageObject messageObject = messageObjects.get(i);
                        ArrayList<MessageObject> messageObjectsByDate = sectionArrays.get(messageObject.monthKey);
                        if (messageObjectsByDate == null) {
                            messageObjectsByDate = new ArrayList<>();
                            sectionArrays.put(messageObject.monthKey, messageObjectsByDate);
                            sections.add(messageObject.monthKey);
                        }
                        messageObjectsByDate.add(messageObject);
                        messages.add(messageObject);
                        messagesById.put(messageObject.getId(), messageObject);

                        if (PhotoViewer.getInstance().isVisible()) {
                            PhotoViewer.getInstance().addPhoto(messageObject, classGuid);
                        }
                    }
                    if (messages.size() > totalCount) {
                        totalCount = messages.size();
                    }
                    endReached = messages.size() >= totalCount;

                    if (messages.isEmpty()) {
                        if (currentSearchFilter != null) {
                            if (TextUtils.isEmpty(currentDataQuery) && dialogId == 0 && minDate == 0) {
                                emptyView.title.setText(LocaleController.getString("SearchEmptyViewTitle", R.string.SearchEmptyViewTitle));
                                String str = LocaleController.getString("SearchEmptyViewFilteredSubtitleMedia", R.string.SearchEmptyViewFilteredSubtitleMedia);
                                emptyView.subtitle.setVisibility(View.VISIBLE);
                                emptyView.subtitle.setText(str);
                            } else {
                                emptyView.title.setText(LocaleController.getString("SearchEmptyViewTitle2", R.string.SearchEmptyViewTitle2));
                                emptyView.subtitle.setVisibility(View.VISIBLE);
                                emptyView.subtitle.setText(LocaleController.getString("SearchEmptyViewFilteredSubtitle2", R.string.SearchEmptyViewFilteredSubtitle2));
                            }
                        } else {
                            emptyView.title.setText(LocaleController.getString("SearchEmptyViewTitle2", R.string.SearchEmptyViewTitle2));
                            emptyView.subtitle.setVisibility(View.GONE);
                        }
                    }
                    adapter = sharedPhotoVideoAdapter;
                    if (recyclerListView.getAdapter() != adapter) {
                        recyclerListView.setAdapter(adapter);
                    }

                    if (!filterAndQueryIsSame) {
                        localTipChats.clear();
                        if (finalResultArray != null) {
                            localTipChats.addAll(finalResultArray);
                        }
                        if (query.length() >= 3 && (LocaleController.getString("SavedMessages", R.string.SavedMessages).toLowerCase().startsWith(query) ||
                                "saved messages".startsWith(query))) {
                            boolean found = false;
                            for (int i = 0; i < localTipChats.size(); i++) {
                                if (localTipChats.get(i) instanceof TLRPC.User)
                                    if (UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id == ((TLRPC.User) localTipChats.get(i)).id) {
                                        found = true;
                                        break;
                                    }
                            }
                            if (!found) {
                                localTipChats.add(0, UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser());
                            }
                        }
                        localTipDates.clear();
                        localTipDates.addAll(dateData);
                        localTipArchive = false;
                        if (query.length() >= 3 && (LocaleController.getString("ArchiveSearchFilter", R.string.ArchiveSearchFilter).toLowerCase().startsWith(query) ||
                                "archive".startsWith(query))) {
                            localTipArchive = true;
                        }
                        if (delegate != null) {
                            delegate.updateFiltersView(TextUtils.isEmpty(currentDataQuery), localTipChats, localTipDates, localTipArchive);
                        }
                    }
                    firstLoading = false;
                    View progressView = null;
                    int progressViewPosition = -1;
                    for (int i = 0; i < n; i++) {
                        View child = recyclerListView.getChildAt(i);
                        if (child instanceof FlickerLoadingView) {
                            progressView = child;
                            progressViewPosition = recyclerListView.getChildAdapterPosition(child);
                        }
                    }
                    final View finalProgressView = progressView;
                    if (progressView != null) {
                        recyclerListView.removeView(progressView);
                    }
                    if ((loadingView.getVisibility() == View.VISIBLE && recyclerListView.getChildCount() == 0) || (recyclerListView.getAdapter() != sharedPhotoVideoAdapter && progressView != null)) {
                        int finalProgressViewPosition = progressViewPosition;
                        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                                int n = recyclerListView.getChildCount();
                                AnimatorSet animatorSet = new AnimatorSet();
                                for (int i = 0; i < n; i++) {
                                    View child = recyclerListView.getChildAt(i);
                                    if (finalProgressView != null) {
                                        if (recyclerListView.getChildAdapterPosition(child) < finalProgressViewPosition) {
                                            continue;
                                        }
                                    }
                                    child.setAlpha(0);
                                    int s = Math.min(recyclerListView.getMeasuredHeight(), Math.max(0, child.getTop()));
                                    int delay = (int) ((s / (float) recyclerListView.getMeasuredHeight()) * 100);
                                    ObjectAnimator a = ObjectAnimator.ofFloat(child, View.ALPHA, 0, 1f);
                                    a.setStartDelay(delay);
                                    a.setDuration(200);
                                    animatorSet.playTogether(a);
                                }
                                animatorSet.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        NotificationCenter.getInstance(currentAccount).onAnimationFinish(animationIndex);
                                    }
                                });
                                animationIndex = NotificationCenter.getInstance(currentAccount).setAnimationInProgress(animationIndex, null);
                                animatorSet.start();

                                if (finalProgressView != null && finalProgressView.getParent() == null) {
                                    recyclerListView.addView(finalProgressView);
                                    RecyclerView.LayoutManager layoutManager = recyclerListView.getLayoutManager();
                                    if (layoutManager != null) {
                                        layoutManager.ignoreView(finalProgressView);
                                        Animator animator = ObjectAnimator.ofFloat(finalProgressView, View.ALPHA, finalProgressView.getAlpha(), 0);
                                        animator.addListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                finalProgressView.setAlpha(1f);
                                                layoutManager.stopIgnoringView(finalProgressView);
                                                recyclerListView.removeView(finalProgressView);
                                            }
                                        });
                                        animator.start();
                                    }
                                }
                                return true;
                            }
                        });
                    }
                    adapter.notifyDataSetChanged();
                });
            });
        }, (filterAndQueryIsSame && !messages.isEmpty()) ? 0 : 350);
        loadingView.setViewType(FlickerLoadingView.PHOTOS_TYPE);
    }

    public void update() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void messagesDeleted(long channelId, ArrayList<Integer> markAsDeletedMessages) {
        boolean changed = false;
        for (int j = 0; j < messages.size(); j++) {
            MessageObject messageObject = messages.get(j);
            long dialogId = messageObject.getDialogId();
            int currentChannelId = dialogId < 0 && ChatObject.isChannel((int) -dialogId, UserConfig.selectedAccount) ? (int) -dialogId : 0;
            if (currentChannelId == channelId) {
                for (int i = 0; i < markAsDeletedMessages.size(); i++) {
                    if (messageObject.getId() == markAsDeletedMessages.get(i)) {
                        changed = true;
                        messages.remove(j);
                        messagesById.remove(messageObject.getId());

                        ArrayList<MessageObject> section = sectionArrays.get(messageObject.monthKey);
                        section.remove(messageObject);
                        if (section.size() == 0) {
                            sections.remove(messageObject.monthKey);
                            sectionArrays.remove(messageObject.monthKey);
                        }
                        j--;
                        totalCount--;
                    }
                }
            }
        }
        if (changed && adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public final static int gotoItemId = 200;
    public final static int forwardItemId = 201;
    public final static int deleteItemId = 202;

    private ActionBarMenuItem gotoItem;
    private ActionBarMenuItem forwardItem;
    private ActionBarMenuItem deleteItem;
    private NumberTextView selectedMessagesCountTextView;
    private final static String actionModeTag = "search_view_pager";
    public boolean actionModeShowing() {
        return isActionModeShowed;
    }
    private void showActionMode(boolean show) {
        if (isActionModeShowed == show) {
            return;
        }
        if (show && actionBar.isActionModeShowed()) {
            return;
        }
        if (show && !actionBar.actionModeIsExist(actionModeTag)) {
            ActionBarMenu actionMode = actionBar.createActionMode(true, actionModeTag);

            selectedMessagesCountTextView = new NumberTextView(actionMode.getContext());
            selectedMessagesCountTextView.setTextSize(18);
            selectedMessagesCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            selectedMessagesCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
            actionMode.addView(selectedMessagesCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
            selectedMessagesCountTextView.setOnTouchListener((v, event) -> true);

            gotoItem = actionMode.addItemWithWidth(gotoItemId, R.drawable.msg_message, AndroidUtilities.dp(54), LocaleController.getString("AccDescrGoToMessage", R.string.AccDescrGoToMessage));
            forwardItem = actionMode.addItemWithWidth(forwardItemId, R.drawable.msg_forward, AndroidUtilities.dp(54), LocaleController.getString("Forward", R.string.Forward));
            deleteItem = actionMode.addItemWithWidth(deleteItemId, R.drawable.msg_delete, AndroidUtilities.dp(54), LocaleController.getString("Delete", R.string.Delete));
        }

        isActionModeShowed = show;
        if (show) {
            AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
            actionBar.showActionMode();
            selectedMessagesCountTextView.setNumber(selectedFiles.size(), false);
            gotoItem.setVisibility(View.VISIBLE);
            forwardItem.setVisibility(View.VISIBLE);
            deleteItem.setVisibility(View.VISIBLE);
        } else {
            actionBar.hideActionMode();
            selectedFiles.clear();
            update();
        }
    }


    public void toggleItemSelection(MessageObject message, View view, int a) {
        FilteredSearchView.MessageHashId hashId = new FilteredSearchView.MessageHashId(message.getId(), message.getDialogId());
        if (selectedFiles.containsKey(hashId)) {
            selectedFiles.remove(hashId);
        } else {
            if (selectedFiles.size() >= 100) {
                return;
            }
            selectedFiles.put(hashId, message);
        }
        if (selectedFiles.size() == 0) {
            showActionMode(false);
        } else {
            selectedMessagesCountTextView.setNumber(selectedFiles.size(), true);
            if (gotoItem != null) {
                gotoItem.setVisibility(selectedFiles.size() == 1 ? View.VISIBLE : View.GONE);
            }
            if (deleteItem != null) {
                boolean canShowDelete = true;
                Set<FilteredSearchView.MessageHashId> keySet = selectedFiles.keySet();
                for (FilteredSearchView.MessageHashId key : keySet) {
                    if (!selectedFiles.get(key).isDownloadingFile) {
                        canShowDelete = false;
                        break;
                    }
                }
                deleteItem.setVisibility(canShowDelete ? View.VISIBLE : View.GONE);
            }
        }
         if (view instanceof SharedPhotoVideoCell) {
            ((SharedPhotoVideoCell) view).setChecked(a, selectedFiles.containsKey(hashId), true);
        }
    }

    public boolean isSelected(FilteredSearchView.MessageHashId messageHashId) {
        return selectedFiles.containsKey(messageHashId);
    }
    private boolean onItemLongClick(MessageObject item, View view, int a) {
        if (!actionBar.isActionModeShowed()) {
            actionBar.showActionMode();
        }
        if (actionBar.isActionModeShowed()) {
            toggleItemSelection(item, view, a);
        }
        return true;
    }

    private class SharedPhotoVideoAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public SharedPhotoVideoAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            if (messages.isEmpty()) {
                return 0;
            }
            return (int) Math.ceil(messages.size() / (float) columnsCount) +  (endReached ? 0 : 1);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new SharedPhotoVideoCell(mContext, SharedPhotoVideoCell.VIEW_TYPE_GLOBAL_SEARCH);
                    SharedPhotoVideoCell cell = (SharedPhotoVideoCell) view;
                    cell.setDelegate(new SharedPhotoVideoCell.SharedPhotoVideoCellDelegate() {
                        @Override
                        public void didClickItem(SharedPhotoVideoCell cell, int index, MessageObject messageObject, int a) {
                            onItemClick(index, cell, messageObject, a);
                        }

                        @Override
                        public boolean didLongClickItem(SharedPhotoVideoCell cell, int index, MessageObject messageObject, int a) {
                            if (actionModeShowing()) {
                                didClickItem(cell, index, messageObject, a);
                                return true;
                            }
                            return onItemLongClick(messageObject, cell, a);
                        }
                    });
                    break;
                case 2:
                    view = new GraySectionCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_graySection) & 0xf2ffffff);
                    break;
                case 1:
                default:
                    FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext) {
                        @Override
                        public int getColumnsCount() {
                            return columnsCount;
                        }
                    };
                    flickerLoadingView.setIsSingleCell(true);
                    flickerLoadingView.setViewType(FlickerLoadingView.PHOTOS_TYPE);
                    view = flickerLoadingView;
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                ArrayList<MessageObject> messageObjects = messages;
                SharedPhotoVideoCell cell = (SharedPhotoVideoCell) holder.itemView;
                cell.setItemsCount(columnsCount);
                cell.setIsFirst(position == 0);
                for (int a = 0; a < columnsCount; a++) {
                    int index = position * columnsCount + a;
                    if (index < messageObjects.size()) {
                        MessageObject messageObject = messageObjects.get(index);
                        cell.setItem(a, messages.indexOf(messageObject), messageObject);
                        if (actionModeShowing()) {
                            messageHashIdTmp.set(messageObject.getId(), messageObject.getDialogId());
                            cell.setChecked(a, isSelected(messageHashIdTmp), true);
                        } else {
                            cell.setChecked(a, false, true);
                        }
                    } else {
                        cell.setItem(a, index, null);
                    }
                }
                cell.requestLayout();
            } else if (holder.getItemViewType() == 1) {
                FlickerLoadingView flickerLoadingView = (FlickerLoadingView) holder.itemView;
                int count = (int) Math.ceil(messages.size() / (float) columnsCount);
                flickerLoadingView.skipDrawItemsCount(columnsCount - (columnsCount * count - messages.size()));
            }
        }

        @Override
        public int getItemViewType(int position) {
            int count = (int) Math.ceil(messages.size() / (float) columnsCount);
            if (position < count) {
                return 0;
            }
            return 1;
        }
    }


    private void onItemClick(int index, View view, MessageObject message, int a) {
        if (message == null) {
            return;
        }
        if (actionModeShowing()) {
            toggleItemSelection(message, view, a);
            return;
        }
        if (currentSearchFilter.filterType == FiltersView.FILTER_TYPE_MEDIA) {
            PhotoViewer.getInstance().setParentActivity(getParentActivity());
            PhotoViewer.getInstance().openPhoto(messages, index, 0, 0, provider);
        }
    }


    public static CharSequence createFromInfoString(MessageObject messageObject) {
        if (arrowSpan == null) {
            arrowSpan = new SpannableStringBuilder("-");
            arrowSpan.setSpan(new ColoredImageSpan(ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.search_arrow).mutate()), 0, 1, 0);
        }
        CharSequence fromName = null;
        TLRPC.User user = messageObject.messageOwner.from_id.user_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getUser(messageObject.messageOwner.from_id.user_id) : null;
        TLRPC.Chat chatFrom = messageObject.messageOwner.from_id.chat_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.chat_id) : null;
        if (chatFrom == null) {
            chatFrom = messageObject.messageOwner.from_id.channel_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.channel_id) : null;
        }
        TLRPC.Chat chatTo = messageObject.messageOwner.peer_id.channel_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.channel_id) : null;
        if (chatTo == null) {
            chatTo = messageObject.messageOwner.peer_id.chat_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.chat_id) : null;
        }
        if (user != null && chatTo != null) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder
                    .append(ContactsController.formatName(user.first_name, user.last_name))
                    .append(' ').append(arrowSpan).append(' ')
                    .append(chatTo.title);
            fromName = spannableStringBuilder;
        } else if (user != null) {
            fromName = ContactsController.formatName(user.first_name, user.last_name);
        } else if (chatFrom != null) {
            fromName = chatFrom.title;
        }
        return fromName == null ? "" : fromName;
    }

    private void showFloatingDateView() {
        AndroidUtilities.cancelRunOnUIThread(hideFloatingDateRunnable);
        AndroidUtilities.runOnUIThread(hideFloatingDateRunnable, 650);
        if (floatingDateView.getTag() != null) {
            return;
        }
        if (floatingDateAnimation != null) {
            floatingDateAnimation.cancel();
        }
        floatingDateView.setTag(1);
        floatingDateAnimation = new AnimatorSet();
        floatingDateAnimation.setDuration(180);
        floatingDateAnimation.playTogether(
                ObjectAnimator.ofFloat(floatingDateView, View.ALPHA, 1.0f),
                ObjectAnimator.ofFloat(floatingDateView, View.TRANSLATION_Y, 0));
        floatingDateAnimation.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        floatingDateAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                floatingDateAnimation = null;
            }
        });
        floatingDateAnimation.start();
    }

    private void hideFloatingDateView(boolean animated) {
        AndroidUtilities.cancelRunOnUIThread(hideFloatingDateRunnable);
        if (floatingDateView.getTag() == null) {
            return;
        }
        floatingDateView.setTag(null);
        if (floatingDateAnimation != null) {
            floatingDateAnimation.cancel();
            floatingDateAnimation = null;
        }
        if (animated) {
            floatingDateAnimation = new AnimatorSet();
            floatingDateAnimation.setDuration(180);
            floatingDateAnimation.playTogether(
                    ObjectAnimator.ofFloat(floatingDateView, View.ALPHA, 0.0f),
                    ObjectAnimator.ofFloat(floatingDateView, View.TRANSLATION_Y, -AndroidUtilities.dp(48)));
            floatingDateAnimation.setInterpolator(CubicBezierInterpolator.EASE_OUT);
            floatingDateAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    floatingDateAnimation = null;
                }
            });
            floatingDateAnimation.start();
        } else {
            floatingDateView.setAlpha(0.0f);
        }
    }
}
