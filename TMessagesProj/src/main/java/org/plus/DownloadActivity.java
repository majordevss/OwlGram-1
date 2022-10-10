package org.plus;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.ContextLinkCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedLinkCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.RecyclerItemsEnterAnimator;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SearchDownloadsContainer;
import org.telegram.ui.Components.SearchViewPager;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.Components.StickerImageView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FilteredSearchView;
import org.telegram.ui.PhotoViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class DownloadActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {



    private  FlickerLoadingView loadingView;
    private  StickerEmptyView emptyView;
    private  RecyclerListView recyclerListView;
    private  DownloadsAdapter adapter;

    ArrayList<MessageObject> currentLoadingFiles = new ArrayList<>();
    ArrayList<MessageObject> recentLoadingFiles = new ArrayList<>();

    ArrayList<MessageObject> currentLoadingFilesTmp = new ArrayList<>();
    ArrayList<MessageObject> recentLoadingFilesTmp = new ArrayList<>();

    int rowCount;
    int downloadingFilesHeader = -1;
    int downloadingFilesStartRow = -1;
    int downloadingFilesEndRow = -1;
    int recentFilesHeader = -1;
    int recentFilesStartRow = -1;
    int recentFilesEndRow = -1;

    private boolean hasCurrentDownload;

    private final FilteredSearchView.MessageHashId messageHashIdTmp = new FilteredSearchView.MessageHashId(0, 0);
    String searchQuery;
    String lastQueryString;
    Runnable lastSearchRunnable;


    private NumberTextView selectedMessagesCountTextView;
    private boolean isActionModeShowed;
    private HashMap<FilteredSearchView.MessageHashId, MessageObject> selectedFiles = new HashMap<>();


    private final static String actionModeTag = "search_view_pager";

    public final static int gotoItemId = 200;
    public final static int forwardItemId = 201;
    public final static int deleteItemId = 202;

    private ActionBarMenuItem gotoItem;
    private ActionBarMenuItem forwardItem;
    private ActionBarMenuItem deleteItem;

    private RecyclerItemsEnterAnimator itemsEnterAnimator;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.onDownloadingFilesChanged);
        DownloadController.getInstance(currentAccount).clearUnviewedDownloads();
        return super.onFragmentCreate();
    }


    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.onDownloadingFilesChanged) {
            DownloadController.getInstance(currentAccount).clearUnviewedDownloads();
            update(true);
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.onDownloadingFilesChanged);

    }

    private FilterLayout filterLayout;
    private  class FilterLayout extends HorizontalScrollView {
        String[] texts = {
               "All",
                LocaleController.getString(R.string.ChatVideo),
                LocaleController.getString(R.string.AttachMusic),
                LocaleController.getString(R.string.ChatDocument),
        };
        TextView[] denominations = new TextView[6];
        LinearLayout denominationsLayout;
        public FilterLayout(Context context) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setVerticalScrollBarEnabled(false);
            setClipToPadding(false);
            setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
            setFillViewport(true);

            int[] maxTextWidth = new int[1];
            int[] textWidths = new int[1];
            int N = texts.length;

            denominationsLayout = new LinearLayout(getContext()) {

                boolean ignoreLayout;

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int availableSize = MeasureSpec.getSize(widthMeasureSpec);
                    ignoreLayout = true;
                    int gaps = AndroidUtilities.dp(9) * (N - 1);
                    if (maxTextWidth[0] * N + gaps <= availableSize) {
                        setWeightSum(1.0f);
                        for (int a = 0, N2 = getChildCount(); a < N2; a++) {
                            getChildAt(a).getLayoutParams().width = 0;
                            ((LayoutParams) getChildAt(a).getLayoutParams()).weight = 1.0f / N2;
                        }
                    } else if (textWidths[0] + gaps <= availableSize) {
                        setWeightSum(1.0f);
                        availableSize -= gaps;
                        float extraWeight = 1.0f;
                        for (int a = 0, N2 = getChildCount(); a < N2; a++) {
                            View child = getChildAt(a);
                            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                            layoutParams.width = 0;
                            int width = (Integer) child.getTag(R.id.width_tag);
                            layoutParams.weight = width / (float) availableSize;
                            extraWeight -= layoutParams.weight;
                        }
                        extraWeight /= (N - 1);
                        if (extraWeight > 0) {
                            for (int a = 0, N2 = getChildCount(); a < N2; a++) {
                                View child = getChildAt(a);
                                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                                int width = (Integer) child.getTag(R.id.width_tag);
                                if (width != maxTextWidth[0]) {
                                    layoutParams.weight += extraWeight;
                                }
                            }
                        }
                    } else {
                        setWeightSum(0.0f);
                        for (int a = 0, N2 = getChildCount(); a < N2; a++) {
                            getChildAt(a).getLayoutParams().width = LayoutHelper.WRAP_CONTENT;
                            ((LayoutParams) getChildAt(a).getLayoutParams()).weight = 0.0f;
                        }
                    }
                    ignoreLayout = false;
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }

                @Override
                public void requestLayout() {
                    if (ignoreLayout) {
                        return;
                    }
                    super.requestLayout();
                }
            };
            denominationsLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int a = 0; a < N; a++) {
                String amount = texts[a];
                String text = amount;
                TextView valueTextView = new TextView(getContext());
                denominations[a] = valueTextView;
                denominations[a].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                denominations[a].setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                denominations[a].setLines(1);
                denominations[a].setTag(a);
                denominations[a].setMaxLines(1);
                denominations[a].setText(text);
                denominations[a].setPadding(AndroidUtilities.dp(15), 0, AndroidUtilities.dp(15), 0);
                denominations[a].setTextColor(Theme.getColor(Theme.key_chats_secretName));
                denominations[a].setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), Theme.getColor(Theme.key_chats_menuItemCheck) & 0x1fffffff));
                denominations[a].setSingleLine(true);
                denominations[a].setGravity(Gravity.CENTER);
                if(curentFilter == a){
                    denominations[a].setTextColor(Color.WHITE);
                    denominations[a].setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), Theme.getColor(Theme.key_chats_secretName)));
                }else{
                    denominations[a].setTextColor(Theme.getColor(Theme.key_chats_secretName));
                    denominations[a].setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), Theme.getColor(Theme.key_chats_menuItemCheck) & 0x1fffffff));

                }
                denominationsLayout.addView(valueTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0, a != N - 1 ? 9 : 0, 0));
                int finalA = a;
                denominations[a].setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final int pos = (int) view.getTag();
                        for(int b = 0; b < N; b++){
                            if(b == finalA){
                                denominations[b].setTextColor(Color.WHITE);
                                denominations[b].setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), Theme.getColor(Theme.key_chats_secretName)));
                            }else{
                                denominations[b].setTextColor(Theme.getColor(Theme.key_chats_secretName));
                                denominations[b].setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(15), Theme.getColor(Theme.key_chats_menuItemCheck) & 0x1fffffff));
                            }
                        }
                        onFilterSelected(pos);
                    }
                });
                int width = (int) Math.ceil(valueTextView.getPaint().measureText(text)) + AndroidUtilities.dp(30);
                valueTextView.setTag(R.id.width_tag, width);
                maxTextWidth[0] = Math.max(maxTextWidth[0], width);
                textWidths[0] += width;
            }
            addView(denominationsLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, 30, Gravity.LEFT | Gravity.TOP,0,16,0,16));
        }

        @Override
        public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
            rectangle.right += AndroidUtilities.dp(16);
            return super.requestChildRectangleOnScreen(child, rectangle, immediate);
        }

        protected void onFilterSelected(int pos){
        }



    }

    /**
     * 0 = all
     * 1 = vidoe
     * 2 = music
     * 3 = document
     */
    private int curentFilter;



    @Override
    public View createView(Context context) {
        actionBar.setTitle("Downloads");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                if ((id == forwardItemId || id == gotoItemId || id == deleteItemId)) {
                    onActionBarItemClick(id);
                }else if (actionBar.isActionModeShowed()) {
                    hideActionMode();
                } else if(id == -1) {
                    finishFragment();
                }else if(id == 1){
                    AlertsCreator.createSimpleAlert(context,"Clear",LocaleController.getString("ClearRecentHistory",R.string.ClearRecentHistory))
                            .setPositiveButton(LocaleController.getString("Clear", R.string.Clear), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DownloadController.getInstance(currentAccount).clearRecentDownloadedFiles();
                                }
                            })
                             .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null).show();
                }else if(id == 2){
                    presentFragment(new CacheControlActivity());
                }
            }
        });
        ActionBarMenu menu =  actionBar.createMenu();
        ActionBarMenuItem otherItem =menu.addItem(3,R.drawable.ic_ab_other);

        otherItem.addSubItem(1,R.drawable.msg_clear,LocaleController.getString("ClearDownloadsList",R.string.ClearDownloadsList));
        otherItem.addSubItem(2,R.drawable.msg_settings,LocaleController.getString("Settings",R.string.Settings));



        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        adapter = new DownloadsAdapter(context);

        recyclerListView = new BlurredRecyclerView(context);
        frameLayout.addView(recyclerListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT,Gravity.TOP|Gravity.LEFT,0,0,0,0));
        recyclerListView.setLayoutManager(new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        });
        recyclerListView.setPadding(0,AndroidUtilities.dp(64),0,0);
        recyclerListView.setAdapter(adapter);
        recyclerListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

            }
        });
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setDelayAnimations(false);
        defaultItemAnimator.setSupportsChangeAnimations(false);
        recyclerListView.setItemAnimator(defaultItemAnimator);

        recyclerListView.setOnItemClickListener((view, position) -> {
            MessageObject messageObject = adapter.getMessage(position);
            if (messageObject == null) {
                return;
            }
            if (actionBar.isActionModeShowed()) {
                toggleItemSelection(messageObject, view, 0);
                messageHashIdTmp.set(messageObject.getId(), messageObject.getDialogId());
                adapter.notifyItemChanged(position);
                return;
            }

            if (view instanceof Cell) {
                SharedDocumentCell cell = ((Cell) view).sharedDocumentCell;
                MessageObject message = cell.getMessage();
                TLRPC.Document document = message.getDocument();
                if (cell.isLoaded()) {
                    if (message.isRoundVideo() || message.isVoice()) {
                        MediaController.getInstance().playMessage(message);
                        return;
                    }
                    if (message.canPreviewDocument()) {
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());

                        ArrayList<MessageObject> documents = new ArrayList<>();
                        documents.add(message);
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());
                        PhotoViewer.getInstance().openPhoto(documents, 0, 0, 0, new PhotoViewer.EmptyPhotoViewerProvider());
                        return;
                    }
                    AndroidUtilities.openDocument(message, getParentActivity(), DownloadActivity.this);
                } else if (!cell.isLoading()) {
                    messageObject.putInDownloadsStore = true;
                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().loadFile(document, messageObject, 0, 0);
                    cell.updateFileExistIcon(true);
                } else {
                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().cancelLoadFile(document);
                    cell.updateFileExistIcon(true);
                }
                update(true);
            }

            //coffee03
            if (view instanceof SharedAudioCell) {
                SharedAudioCell cell = (SharedAudioCell) view;
                cell.didPressedButton();
            }
        });
        recyclerListView.setOnItemLongClickListener((view, position) -> {
            MessageObject messageObject = adapter.getMessage(position);
            if (messageObject != null) {
                if (!actionBar.isActionModeShowed()) {
                    showActionMode(true);
                }
                if (actionBar.isActionModeShowed()) {
                    toggleItemSelection(messageObject, view, 0);
                    messageHashIdTmp.set(messageObject.getId(), messageObject.getDialogId());
                    adapter.notifyItemChanged(position);
                }
                return true;
            }
            return false;
        });
        itemsEnterAnimator = new RecyclerItemsEnterAnimator(recyclerListView, true);


        frameLayout.addView(loadingView = new FlickerLoadingView(context));
        loadingView.setUseHeaderOffset(true);
        loadingView.setViewType(FlickerLoadingView.FILES_TYPE);
        loadingView.setVisibility(View.GONE);
        emptyView = new StickerEmptyView(context, loadingView, StickerEmptyView.STICKER_TYPE_SEARCH);
        frameLayout.addView(emptyView);
        recyclerListView.setEmptyView(emptyView);
        filterLayout = new FilterLayout(context){
            @Override
            protected void onFilterSelected(int pos) {
                curentFilter = pos;
                update(true);
            }
        };
        frameLayout.addView(filterLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,64,Gravity.TOP|Gravity.LEFT));

        FileLoader.getInstance(currentAccount).getCurrentLoadingFiles(currentLoadingFiles);
        update(false);

        return fragmentView;
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
        if (view instanceof SharedDocumentCell) {
            ((SharedDocumentCell) view).setChecked(selectedFiles.containsKey(hashId), true);
        } else if (view instanceof SharedPhotoVideoCell) {
            ((SharedPhotoVideoCell) view).setChecked(a, selectedFiles.containsKey(hashId), true);
        } else if (view instanceof SharedLinkCell) {
            ((SharedLinkCell) view).setChecked(selectedFiles.containsKey(hashId), true);
        } else if (view instanceof SharedAudioCell) {
            ((SharedAudioCell) view).setChecked(selectedFiles.containsKey(hashId), true);
        } else if (view instanceof ContextLinkCell) {
            ((ContextLinkCell) view).setChecked(selectedFiles.containsKey(hashId), true);
        } else if (view instanceof DialogCell) {
            ((DialogCell) view).setChecked(selectedFiles.containsKey(hashId), true);
        }
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


        if (actionBar.getBackButton().getDrawable() instanceof BackDrawable) {
            BackDrawable backDrawable = (BackDrawable) actionBar.getBackButton().getDrawable();
            backDrawable.setRotation(0,true);
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
            update(true);
        }
    }
    private boolean isEmptyDownloads() {
        return DownloadController.getInstance(currentAccount).downloadingFiles.isEmpty() && DownloadController.getInstance(currentAccount).recentDownloadingFiles.isEmpty();
    }

    public void update(boolean animated) {
        if (TextUtils.isEmpty(searchQuery) || isEmptyDownloads()) {
            if (rowCount == 0) {
                itemsEnterAnimator.showItemsAnimated(0);
            }
            FileLoader.getInstance(currentAccount).getCurrentLoadingFiles(currentLoadingFilesTmp);
            FileLoader.getInstance(currentAccount).getRecentLoadingFiles(recentLoadingFilesTmp);

            for (int i = 0; i < currentLoadingFiles.size(); i++) {
                currentLoadingFiles.get(i).setQuery(null);
            }
            for (int i = 0; i < recentLoadingFiles.size(); i++) {
                recentLoadingFiles.get(i).setQuery(null);
            }

            lastQueryString = null;
            updateListInternal(animated, currentLoadingFilesTmp, recentLoadingFilesTmp);
            if (rowCount == 0) {
                emptyView.showProgress(false, false);
                emptyView.title.setText(LocaleController.getString("SearchEmptyViewDownloads", R.string.SearchEmptyViewDownloads));
                emptyView.subtitle.setVisibility(View.GONE);
            }
            emptyView.setStickerType(9);
        } else {
            emptyView.setStickerType(1);
            ArrayList<MessageObject> currentLoadingFilesTmp = new ArrayList<>();
            ArrayList<MessageObject> recentLoadingFilesTmp = new ArrayList<>();

            FileLoader.getInstance(currentAccount).getCurrentLoadingFiles(currentLoadingFilesTmp);
            FileLoader.getInstance(currentAccount).getRecentLoadingFiles(recentLoadingFilesTmp);

            String q = searchQuery.toLowerCase();
            boolean sameQuery = q.equals(lastQueryString);

            lastQueryString = q;
            Utilities.searchQueue.cancelRunnable(lastSearchRunnable);
            Utilities.searchQueue.postRunnable(lastSearchRunnable = () -> {
                ArrayList<MessageObject> currentLoadingFilesRes = new ArrayList<>();
                ArrayList<MessageObject> recentLoadingFilesRes = new ArrayList<>();
                for (int i = 0; i < currentLoadingFilesTmp.size(); i++) {
                    if (FileLoader.getDocumentFileName(currentLoadingFilesTmp.get(i).getDocument()).toLowerCase().contains(q)) {
                        MessageObject messageObject = new MessageObject(currentAccount, currentLoadingFilesTmp.get(i).messageOwner, false, false);
                        messageObject.mediaExists = currentLoadingFilesTmp.get(i).mediaExists;
                        messageObject.setQuery(searchQuery);
                        currentLoadingFilesRes.add(messageObject);
                    }
                }

                for (int i = 0; i < recentLoadingFilesTmp.size(); i++) {
                    if (FileLoader.getDocumentFileName(recentLoadingFilesTmp.get(i).getDocument()).toLowerCase().contains(q)) {
                        MessageObject messageObject = new MessageObject(currentAccount, recentLoadingFilesTmp.get(i).messageOwner, false, false);
                        messageObject.mediaExists = recentLoadingFilesTmp.get(i).mediaExists;
                        messageObject.setQuery(searchQuery);
                        recentLoadingFilesRes.add(messageObject);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (q.equals(lastQueryString)) {
                        if (rowCount == 0) {
                            itemsEnterAnimator.showItemsAnimated(0);
                        }
                        updateListInternal(true, currentLoadingFilesRes, recentLoadingFilesRes);
                        if (rowCount == 0) {
                            emptyView.showProgress(false, true);

                            emptyView.title.setText(LocaleController.getString("SearchEmptyViewTitle2", R.string.SearchEmptyViewTitle2));
                            emptyView.subtitle.setVisibility(View.VISIBLE);
                            emptyView.subtitle.setText(LocaleController.getString("SearchEmptyViewFilteredSubtitle2", R.string.SearchEmptyViewFilteredSubtitle2));
                        }
                    }
                });

            }, sameQuery ? 0 : 300);

            this.recentLoadingFilesTmp.clear();
            this.currentLoadingFilesTmp.clear();
            if (!sameQuery) {
                emptyView.showProgress(true, true);
                updateListInternal(animated, this.currentLoadingFilesTmp, this.recentLoadingFilesTmp);
            }
        }
    }

    public interface DownloadFilterInterface {
        ArrayList<MessageObject> apply(ArrayList<MessageObject> downloads);
    }
    private   class DownloadFilter implements DownloadFilterInterface{

        @Override
        public ArrayList<MessageObject> apply(ArrayList<MessageObject> downloads) {
            ArrayList<MessageObject> downloadObjects  =new ArrayList<>();
            for(int a = 0; a < downloads.size();a++){
                MessageObject downloadObj = downloads.get(a);
                if(curentFilter == 1){
                    if(downloadObj.isVideo()){
                        downloadObjects.add(downloadObj);
                    }
                }else if(curentFilter == 2){
                    if(downloadObj.isMusic()){
                        downloadObjects.add(downloadObj);
                    }
                }else if(curentFilter == 3){
                    if(downloadObj.isDocument()){
                        downloadObjects.add(downloadObj);
                    }
                }else{
                    return downloads;
                }
            }
            return downloadObjects;
        }
    }


    private void filterDownloads(ArrayList<MessageObject> currentLoadingFilesTmp, ArrayList<MessageObject> recentLoadingFilesTmp){
        DownloadFilter downloadFilter = new DownloadFilter();
        currentLoadingFiles = downloadFilter.apply(currentLoadingFilesTmp);
        recentLoadingFiles =  downloadFilter.apply(recentLoadingFilesTmp);

    }


    private void updateListInternal(boolean animated, ArrayList<MessageObject> currentLoadingFilesTmp, ArrayList<MessageObject> recentLoadingFilesTmp) {

        if (animated) {
            int oldDownloadingFilesHeader = downloadingFilesHeader;
            int oldDownloadingFilesStartRow = downloadingFilesStartRow;
            int oldDownloadingFilesEndRow = downloadingFilesEndRow;

            int oldRecentFilesHeader = recentFilesHeader;
            int oldRecentFilesStartRow = recentFilesStartRow;
            int oldRecentFilesEndRow = recentFilesEndRow;

            int oldRowCount = rowCount;

            ArrayList<MessageObject> oldDownloadingLoadingFiles = new ArrayList<>(currentLoadingFiles);
            ArrayList<MessageObject> oldRecentLoadingFiles = new ArrayList<>(recentLoadingFiles);

            updateRows(currentLoadingFilesTmp, recentLoadingFilesTmp);
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldRowCount;
                }

                @Override
                public int getNewListSize() {
                    return rowCount;
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    if (oldItemPosition >= 0 && newItemPosition >= 0) {
                        if (oldItemPosition == oldDownloadingFilesHeader && newItemPosition == downloadingFilesHeader) {
                            return true;
                        }
                        if (oldItemPosition == oldRecentFilesHeader && newItemPosition == recentFilesHeader) {
                            return true;
                        }
                    }
                    MessageObject oldItem = null;
                    MessageObject newItem = null;

                    if (oldItemPosition >= oldDownloadingFilesStartRow && oldItemPosition < oldDownloadingFilesEndRow) {
                        oldItem = oldDownloadingLoadingFiles.get(oldItemPosition - oldDownloadingFilesStartRow);
                    } else if (oldItemPosition >= oldRecentFilesStartRow && oldItemPosition < oldRecentFilesEndRow) {
                        oldItem = oldRecentLoadingFiles.get(oldItemPosition - oldRecentFilesStartRow);
                    }

                    if (newItemPosition >= downloadingFilesStartRow && newItemPosition < downloadingFilesEndRow) {
                        newItem = currentLoadingFiles.get(newItemPosition - downloadingFilesStartRow);
                    } else if (newItemPosition >= recentFilesStartRow && newItemPosition < recentFilesEndRow) {
                        newItem = recentLoadingFiles.get(newItemPosition - recentFilesStartRow);
                    }
                    if (newItem != null && oldItem != null) {
                        return newItem.getDocument().id == oldItem.getDocument().id;
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return areItemsTheSame(oldItemPosition, newItemPosition);
                }
            }).dispatchUpdatesTo(adapter);
            for (int i = 0; i < recyclerListView.getChildCount(); i++) {
                View child = recyclerListView.getChildAt(i);
                int p = recyclerListView.getChildAdapterPosition(child);
                if (p >= 0) {
                    RecyclerView.ViewHolder holder = recyclerListView.getChildViewHolder(child);
                    if (holder == null || holder.shouldIgnore()) {
                        continue;
                    }
                    if (child instanceof GraySectionCell) {
                        adapter.onBindViewHolder(holder, p);
                    } else if (child instanceof Cell) {
                        Cell cell = (Cell) child;
                        cell.sharedDocumentCell.updateFileExistIcon(true);
                        messageHashIdTmp.set(cell.sharedDocumentCell.getMessage().getId(), cell.sharedDocumentCell.getMessage().getDialogId());
                        cell.sharedDocumentCell.setChecked(isSelected(messageHashIdTmp), true);
                    }
                }
            }
        } else {
            updateRows(currentLoadingFilesTmp, recentLoadingFilesTmp);
            adapter.notifyDataSetChanged();
        }
    }

    public boolean isSelected(FilteredSearchView.MessageHashId messageHashId) {
        return selectedFiles.containsKey(messageHashId);
    }

    private void updateRows(ArrayList<MessageObject> currentLoadingFilesTmp, ArrayList<MessageObject> recentLoadingFilesTmp) {
        currentLoadingFiles.clear();
        currentLoadingFiles.addAll(currentLoadingFilesTmp);

        recentLoadingFiles.clear();
        recentLoadingFiles.addAll(recentLoadingFilesTmp);


        filterDownloads(currentLoadingFilesTmp,recentLoadingFilesTmp);


        rowCount = 0;
        downloadingFilesHeader = -1;
        downloadingFilesStartRow = -1;
        downloadingFilesEndRow = -1;
        recentFilesHeader = -1;
        recentFilesStartRow = -1;
        recentFilesEndRow = -1;
        hasCurrentDownload = false;

        if (!currentLoadingFiles.isEmpty()) {
            downloadingFilesHeader = rowCount++;
            downloadingFilesStartRow = rowCount;
            rowCount += currentLoadingFiles.size();
            downloadingFilesEndRow = rowCount;

            for (int i = 0; i < currentLoadingFiles.size(); i++) {
                if (FileLoader.getInstance(currentAccount).isLoadingFile(currentLoadingFiles.get(i).getFileName())) {
                    hasCurrentDownload = true;
                    break;
                }
            }
        }
        if (!recentLoadingFiles.isEmpty()) {
            recentFilesHeader = rowCount++;
            recentFilesStartRow = rowCount;
            rowCount += recentLoadingFiles.size();
            recentFilesEndRow = rowCount;

        }
    }
    public void hideActionMode() {
        showActionMode(false);
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
                hideActionMode();
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
    private class Cell extends FrameLayout {

        SharedDocumentCell sharedDocumentCell;

        public Cell(@NonNull Context context) {
            super(context);
            sharedDocumentCell = new SharedDocumentCell(context, SharedDocumentCell.VIEW_TYPE_GLOBAL_SEARCH);
            sharedDocumentCell.rightDateTextView.setVisibility(View.GONE);
            addView(sharedDocumentCell);
        }
    }

    private class DownloadsAdapter extends RecyclerListView.SelectionAdapter {

        private Context context;

        public DownloadsAdapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                view = new GraySectionCell(parent.getContext());
            } else if (viewType == 1){
                Cell sharedDocumentCell = new Cell(context);
                view = sharedDocumentCell;
            } else {
                SharedAudioCell sharedAudioCell = new SharedAudioCell(parent.getContext()) {
                    @Override
                    public boolean needPlayMessage(MessageObject messageObject) {
                        return MediaController.getInstance().playMessage(messageObject);
                    }
                };
                view = sharedAudioCell;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int type = holder.getItemViewType();
            if (type == 0) {
                GraySectionCell graySectionCell = (GraySectionCell) holder.itemView;
                if (position == downloadingFilesHeader) {
                    graySectionCell.setText(LocaleController.getString("Downloading", R.string.Downloading), hasCurrentDownload ? LocaleController.getString("PauseAll", R.string.PauseAll) : LocaleController.getString("ResumeAll", R.string.ResumeAll), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            for (int i = 0; i < currentLoadingFiles.size(); i++) {
                                MessageObject messageObject = currentLoadingFiles.get(i);
                                if (hasCurrentDownload) {
                                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().cancelLoadFile(messageObject.getDocument());
                                } else {
                                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().loadFile(messageObject.getDocument(), messageObject, 0, 0);
                                }
                            }
                            update(true);
                        }
                    });
                } else if (position == recentFilesHeader) {
                    graySectionCell.setText(LocaleController.getString("RecentlyDownloaded", R.string.RecentlyDownloaded), LocaleController.getString("Settings", R.string.Settings), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showSettingsDialog();
                        }
                    });
                }
            } else {
                MessageObject messageObject = getMessage(position);
                if (messageObject != null) {
                    if (type == 1) {
                        Cell view = (Cell) holder.itemView;
                        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                        int oldId = view.sharedDocumentCell.getMessage() == null ? 0 : view.sharedDocumentCell.getMessage().getId();
                        view.sharedDocumentCell.setDocument(messageObject, true);
                        messageHashIdTmp.set(view.sharedDocumentCell.getMessage().getId(), view.sharedDocumentCell.getMessage().getDialogId());
                        view.sharedDocumentCell.setChecked(isSelected(messageHashIdTmp), oldId == messageObject.getId());
                    } else if (type == 2) {
                        SharedAudioCell sharedAudioCell = (SharedAudioCell) holder.itemView;
                        sharedAudioCell.setMessageObject(messageObject, true);
                        int oldId = sharedAudioCell.getMessage() == null ? 0 : sharedAudioCell.getMessage().getId();
                        sharedAudioCell.setChecked(isSelected(messageHashIdTmp), oldId == messageObject.getId());
                    }
                }
            }
        }

        public boolean isSelected(FilteredSearchView.MessageHashId messageHashId) {
            return selectedFiles.containsKey(messageHashId);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == downloadingFilesHeader || position == recentFilesHeader) {
                return 0;
            }
            MessageObject messageObject = getMessage(position);
            if (messageObject == null) {
                return 1;
            }
            if (messageObject.isMusic()) {
                return 2;
            }
            return 1;
        }

        private MessageObject getMessage(int position) {
            if (position >= downloadingFilesStartRow && position < downloadingFilesEndRow) {;
                return currentLoadingFiles.get(position - downloadingFilesStartRow);
            } else if (position >= recentFilesStartRow && position < recentFilesEndRow) {
                return recentLoadingFiles.get(position - recentFilesStartRow);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 1 || holder.getItemViewType() == 2;
        }
    }

    private void showSettingsDialog() {
        if (getParentActivity() == null) {
            return;
        }
        BottomSheet bottomSheet = new BottomSheet(getParentActivity(), false);
        Context context = getParentActivity();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        StickerImageView imageView = new StickerImageView(context, currentAccount);
        imageView.setStickerNum(9);
        imageView.getImageReceiver().setAutoRepeat(1);
        linearLayout.addView(imageView, LayoutHelper.createLinear(144, 144, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

        TextView title = new TextView(context);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setText(LocaleController.getString("DownloadedFiles", R.string.DownloadedFiles));
        linearLayout.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 30, 21, 0));

        TextView description = new TextView(context);
        description.setGravity(Gravity.CENTER_HORIZONTAL);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        description.setTextColor(Theme.getColor(Theme.key_dialogTextHint));
        description.setText(LocaleController.formatString("DownloadedFilesMessage", R.string.DownloadedFilesMessage));
        linearLayout.addView(description, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 15, 21, 16));


        TextView buttonTextView = new TextView(context);
        buttonTextView.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        buttonTextView.setGravity(Gravity.CENTER);
        buttonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView.setText(LocaleController.getString("ManageDeviceStorage", R.string.ManageDeviceStorage));

        buttonTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        buttonTextView.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhite), 120)));

        linearLayout.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 15, 16, 16));


        TextView buttonTextView2 = new TextView(context);
        buttonTextView2.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        buttonTextView2.setGravity(Gravity.CENTER);
        buttonTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView2.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView2.setText(LocaleController.getString("ClearDownloadsList", R.string.ClearDownloadsList));

        buttonTextView2.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        buttonTextView2.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_featuredStickers_addButton), 120)));

        linearLayout.addView(buttonTextView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 0, 16, 16));

        NestedScrollView scrollView = new NestedScrollView(context);
        scrollView.addView(linearLayout);
        bottomSheet.setCustomView(scrollView);
        bottomSheet.show();

        buttonTextView.setOnClickListener(view -> {
            bottomSheet.dismiss();
            presentFragment(new CacheControlActivity());

        });
        buttonTextView2.setOnClickListener(view -> {
            bottomSheet.dismiss();
            DownloadController.getInstance(currentAccount).clearRecentDownloadedFiles();
        });
        showDialog(bottomSheet);
    }

}
