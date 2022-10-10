package org.master.feature.feed;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.CallLogActivity;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.RadioCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedLinkCell;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.DataUsageActivity;
import org.telegram.ui.FilterCreateActivity;
import org.telegram.ui.FiltersSetupActivity;

import java.util.ArrayList;
import java.util.HashMap;

import it.owlgram.android.OwlConfig;

public class FeedFragment  extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private class ViewPage extends FrameLayout {
        private RecyclerListView listView;
        private FeedAdapter listAdapter;
        private LinearLayoutManager layoutManager;
        private int selectedType;

        public ViewPage(Context context) {
            super(context);
        }
    }
    private Paint backgroundPaint = new Paint();
    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;
    private ViewPage[] viewPages = new ViewPage[2];
    private AnimatorSet tabsAnimation;
    private boolean tabsAnimationInProgress;
    private boolean animatingForward;
    private boolean backAnimation;
    private int maximumVelocity;
    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };

    private boolean swipeBackEnabled = true;

    public static class FeedData {
        public ArrayList<MessageObject> messages = new ArrayList<>();
        public SparseArray<MessageObject>[] messagesDict = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
        public ArrayList<String> sections = new ArrayList<>();
        public HashMap<String, ArrayList<MessageObject>> sectionArrays = new HashMap<>();
        public int totalCount;
        public boolean loading;
        public boolean[] endReached = new boolean[]{false, true};

        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();

        public void setTotalCount(int count) {
            totalCount = count;
        }



        public void setEndReached(int num, boolean value) {
            endReached[num] = value;
        }

        public boolean addMessage(MessageObject messageObject, int loadIndex, boolean isNew) {
            if (messagesDict[loadIndex].indexOfKey(messageObject.getId()) >= 0) {
                return false;
            }
            ArrayList<MessageObject> messageObjects = sectionArrays.get(messageObject.monthKey);
            if (messageObjects == null) {
                messageObjects = new ArrayList<>();
                sectionArrays.put(messageObject.monthKey, messageObjects);
                if (isNew) {
                    sections.add(0, messageObject.monthKey);
                } else {
                    sections.add(messageObject.monthKey);
                }
            }
            if (isNew) {
                messageObjects.add(0, messageObject);
                messages.add(0, messageObject);
            } else {
                messageObjects.add(messageObject);
                messages.add(messageObject);
            }
            messagesDict[loadIndex].put(messageObject.getId(), messageObject);
            return true;
        }

        public MessageObject deleteMessage(int mid, int loadIndex) {
            MessageObject messageObject = messagesDict[loadIndex].get(mid);
            if (messageObject == null) {
                return null;
            }
            ArrayList<MessageObject> messageObjects = sectionArrays.get(messageObject.monthKey);
            if (messageObjects == null) {
                return null;
            }
            messageObjects.remove(messageObject);
            messages.remove(messageObject);
            messagesDict[loadIndex].remove(messageObject.getId());
            if (messageObjects.isEmpty()) {
                sectionArrays.remove(messageObject.monthKey);
                sections.remove(messageObject.monthKey);
            }
            totalCount--;
            return messageObject;
        }

        public void replaceMid(int oldMid, int newMid) {
            MessageObject obj = messagesDict[0].get(oldMid);
            if (obj != null) {
                messagesDict[0].remove(oldMid);
                messagesDict[0].put(newMid, obj);
                obj.messageOwner.id = newMid;
            }
        }

        public ArrayList<MessageObject> getMessages() {
            return  messages;
        }

    }
    private FeedData[] feedData;


    public FeedFilter feedFilter;
    private ArrayList<Long> feedDialogs;
    private boolean loading;
    private boolean endReached;
    public ArrayList<MessageObject> messages = new ArrayList<>();
    public SparseArray<MessageObject> messagesDict = new SparseArray();
    public ArrayList<String> sections = new ArrayList<>();
    public HashMap<String, ArrayList<MessageObject>> sectionArrays = new HashMap<>();
    public boolean addMessage(MessageObject messageObject, boolean isNew) {
        if (messagesDict.indexOfKey(messageObject.getId()) >= 0) {
            return false;
        }
        ArrayList<MessageObject> messageObjects = sectionArrays.get(messageObject.monthKey);
        if (messageObjects == null) {
            messageObjects = new ArrayList<>();
            sectionArrays.put(messageObject.monthKey, messageObjects);
            if (isNew) {
                sections.add(0, messageObject.monthKey);
            } else {
                sections.add(messageObject.monthKey);
            }
        }
        if (isNew) {
            messageObjects.add(0, messageObject);
            messages.add(0, messageObject);
        } else {
            messageObjects.add(messageObject);
            messages.add(messageObject);
        }
        messagesDict.put(messageObject.getId(), messageObject);
        return true;
    }

    public FeedFragment(FeedFilter feedFilter){
        this.feedFilter = feedFilter;
        feedDialogs = new ArrayList<>(feedFilter.feedDialogsId);
        feedData = new FeedData[feedDialogs.size()];
    }


    private RecyclerListView listView;
    private FeedAdapter feedAdapter;
    private LinearLayoutManager layoutManager;
    private ArrayList<Long> dialogsIds = new ArrayList<>();
    private EmptyTextProgressView emptyView;
    private FlickerLoadingView flickerLoadingView;

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.messagesDidLoad);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.messagesDidLoad);
        super.onFragmentDestroy();
    }


    private StickerEmptyView stickerEmptyView;


    @Override
    public View createView(Context context) {
        Theme.createChatResources(context,false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(feedFilter.title);
        actionBar.setSubtitle(feedDialogs.size() + " Sources");

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1,R.drawable.msg_add);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));


        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setViewType(FlickerLoadingView.CALL_LOG_TYPE);
        flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flickerLoadingView.showDate(false);
        emptyView = new EmptyTextProgressView(context, flickerLoadingView);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


        listView = new RecyclerListView(context);
        listView.setEmptyView(emptyView);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setFastScrollEnabled(RecyclerListView.FastScroll.DATE_TYPE);
        listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        listView.setPinnedSectionOffsetY(-AndroidUtilities.dp(2));
        listView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        listView.setItemAnimator(null);
        listView.setClipToPadding(false);
        listView.setSectionsType(RecyclerListView.SECTIONS_TYPE_DATE);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(feedAdapter = new FeedAdapter(context));


        prepareFeed();
        return fragmentView;
    }


    private static class EmptyTextProgressView extends FrameLayout {

        private TextView emptyTextView1;
        private TextView emptyTextView2;
        private View progressView;
        private RLottieImageView imageView;

        public EmptyTextProgressView(Context context) {
            this(context, null);
        }

        public EmptyTextProgressView(Context context, View progressView) {
            super(context);

            addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            this.progressView = progressView;

            imageView = new RLottieImageView(context);
            imageView.setAnimation(R.raw.utyan_call, 120, 120);
            imageView.setAutoRepeat(false);
            addView(imageView, LayoutHelper.createFrame(140, 140, Gravity.CENTER, 52, 4, 52, 60));
            imageView.setOnClickListener(v -> {
                if (!imageView.isPlaying()) {
                    imageView.setProgress(0.0f);
                    imageView.playAnimation();
                }
            });

            emptyTextView1 = new TextView(context);
            emptyTextView1.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            emptyTextView1.setText("No Feed");
            emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            emptyTextView1.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            emptyTextView1.setGravity(Gravity.CENTER);
            addView(emptyTextView1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 40, 17, 0));
            emptyTextView2 = new TextView(context);
            String help = "You don't have feed!start by creating one";
            if (AndroidUtilities.isTablet() && !AndroidUtilities.isSmallTablet()) {
                help = help.replace('\n', ' ');
            }
            emptyTextView2.setText(help);
            emptyTextView2.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
            emptyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            emptyTextView2.setGravity(Gravity.CENTER);
            emptyTextView2.setLineSpacing(AndroidUtilities.dp(2), 1);
            addView(emptyTextView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 80, 17, 0));

            progressView.setAlpha(0f);
            imageView.setAlpha(0f);
            emptyTextView1.setAlpha(0f);
            emptyTextView2.setAlpha(0f);

            setOnTouchListener((v, event) -> true);
        }


        public void showProgress() {
            imageView.animate().alpha(0f).setDuration(150).start();
            emptyTextView1.animate().alpha(0f).setDuration(150).start();
            emptyTextView2.animate().alpha(0f).setDuration(150).start();
            progressView.animate().alpha(1f).setDuration(150).start();
        }

        public void showTextView() {
            imageView.animate().alpha(1f).setDuration(150).start();
            emptyTextView1.animate().alpha(1f).setDuration(150).start();
            emptyTextView2.animate().alpha(1f).setDuration(150).start();
            progressView.animate().alpha(0f).setDuration(150).start();
            imageView.playAnimation();
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(feedAdapter != null){
            feedAdapter.notifyDataSetChanged();
        }
    }

    private LongSparseArray<Boolean> loadingSparseArray = new LongSparseArray<>();

    private void prepareFeed(){
        for(int a = 0,N= feedDialogs.size();a < N; a++){
           Long dialogId =  feedDialogs.get(a);
           loadFeed(dialogId);
        }
    }

    private void loadFeed(long dialogId){
        if (loadingSparseArray.indexOfKey(dialogId) >= 0) {
            return;
        }
        loadingSparseArray.put(dialogId,true);
        getMessagesController().loadMessages(-dialogId, 0, false, 20, 0, 0, true, 0, classGuid, 0, 0, 0, 0, 0, 1);

    }


    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagesDidLoad) {
            int guid = (Integer) args[10];
            if (guid == classGuid) {
                Long dialogId = (Long) args[0];
                loadingSparseArray.remove(dialogId);
                feedDialogs.remove(dialogId);
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[2];
                Log.i("didRec","size = " + arr.size());
                for (int a = 0; a < arr.size(); a++) {
                    MessageObject message = arr.get(a);
                    addMessage(message, false);

//                    if(message.type == MessageObject.TYPE_PHOTO || message.type == MessageObject.TYPE_VIDEO){
//                    }
                }
                if(feedAdapter != null){
                    feedAdapter.notifyDataSetChanged();
                }

            }


        }else if (id == NotificationCenter.mediaDidLoad) {
            long uid = (Long) args[0];
            int guid = (Integer) args[3];
            int requestIndex = (Integer) args[7];
            int type = (Integer) args[4];
            boolean fromStart = (boolean) args[6];

            if (type == 6 || type == 7) {
                type = 0;
            }

            if (guid == getClassGuid()) {
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[2];

                int oldItemCount;
                int oldMessagesCount = messages.size();
                if (feedAdapter != null) {
                    oldItemCount = feedAdapter.getItemCount();
                    if (feedAdapter instanceof RecyclerListView.SectionsAdapter) {
                        RecyclerListView.SectionsAdapter sectionsAdapter = (RecyclerListView.SectionsAdapter) feedAdapter;
                        sectionsAdapter.notifySectionsChanged();
                    }
                } else {
                    oldItemCount = 0;
                }
                loading = false;

                SparseBooleanArray addedMesages = new SparseBooleanArray();

                if (fromStart) {
                    for (int a = arr.size() - 1; a >= 0; a--) {
                        MessageObject message = arr.get(a);
                        boolean added = addMessage(message, true);
                        if (added) {
                            addedMesages.put(message.getId(), true);
                        }
                    }

                } else {
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject message = arr.get(a);
                        if (addMessage(message,  false)) {
                            addedMesages.put(message.getId(), true);
                        }
                    }

                }

                if (feedAdapter != null) {
                    listView.stopScroll();
                    int newItemCount = feedAdapter.getItemCount();
                    feedAdapter.notifyDataSetChanged();
                }
            }
        }

    }


    private class FeedAdapter extends RecyclerListView.SectionsAdapter {

        private Context mContext;

        public FeedAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object getItem(int section, int position) {
            return null;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder, int section, int row) {
            if (sections.size() == 0 && !loading) {
                return false;
            }
            return section == 0 || row != 0;
        }

        @Override
        public int getSectionCount() {
            if (sections.size() == 0 && !loading) {
                return 1;
            }
            return sections.size() + (sections.isEmpty() || endReached ? 0 : 1);
        }

        @Override
        public int getCountForSection(int section) {
            if (sections.size() == 0 && !loading) {
                return 1;
            }
            if (section < sections.size()) {
                return sectionArrays.get(sections.get(section)).size() + (section != 0 ? 1 : 0);
            }
            return 1;
        }

        @Override
        public View getSectionHeaderView(int section, View view) {
            if (view == null) {
                view = new GraySectionCell(mContext);
                view.setBackgroundColor(Theme.getColor(Theme.key_graySection) & 0xf2ffffff);
            }
            if (section == 0) {
                view.setAlpha(0.0f);
            } else if (section < sections.size()) {
                view.setAlpha(1.0f);
                String name = sections.get(section);
                ArrayList<MessageObject> messageObjects = sectionArrays.get(name);
                MessageObject messageObject = messageObjects.get(0);
                ((GraySectionCell) view).setText(LocaleController.formatSectionDate(messageObject.messageOwner.date));
            }
            return view;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new GraySectionCell(mContext);
                    break;
                case 1:
                    ChatMessageCell chatMessageCell = new ChatMessageCell(mContext);
                    chatMessageCell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {

                    });
                    chatMessageCell.isChat = true;
                    chatMessageCell.setFullyDraw(true);
                    view = chatMessageCell;
//                    view = new TwitterFeedCell(mContext);
                    break;
                case 3:
                    View emptyStubView = createEmptyStubView(mContext);
                    emptyStubView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    return new RecyclerListView.Holder(emptyStubView);
                case 2:
                default:
                    FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext);
                    flickerLoadingView.setIsSingleCell(true);
                    flickerLoadingView.showDate(false);
                    flickerLoadingView.setViewType(FlickerLoadingView.LINKS_TYPE);
                    view = flickerLoadingView;
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(int section, int position, RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() != 2 && holder.getItemViewType() != 3) {
                String name = sections.get(section);
                ArrayList<MessageObject> messageObjects = sectionArrays.get(name);
                switch (holder.getItemViewType()) {
                    case 0: {
                        MessageObject messageObject = messageObjects.get(0);
                        ((GraySectionCell) holder.itemView).setText(LocaleController.formatSectionDate(messageObject.messageOwner.date));
                        break;
                    }
                    case 1: {
                        if (section != 0) {
                            position--;
                        }
                        TwitterFeedCell sharedLinkCell = (TwitterFeedCell) holder.itemView;
                         MessageObject messageObject = messageObjects.get(position);
                         if(messageObject != null){
                             sharedLinkCell.setMessage(messageObject);
//                             sharedLinkCell.setMessageObject(messageObject,null,false,false);
                         }


//                        if (isActionModeShowed) {
//                            sharedLinkCell.setChecked(selectedFiles[messageObject.getDialogId() == dialog_id ? 0 : 1].indexOfKey(messageObject.getId()) >= 0, !scrolling);
//                        } else {
//                            sharedLinkCell.setChecked(false, !scrolling);
//                        }
                        break;
                    }
                }
            }
        }

        @Override
        public int getItemViewType(int section, int position) {
            if (sections.size() == 0 && !loading) {
                return 3;
            }
            if (section < sections.size()) {
                if (section != 0 && position == 0) {
                    return 0;
                } else {
                    return 1;
                }
            }
            return 2;
        }

        @Override
        public String getLetter(int position) {
            return null;
        }

        @Override
        public void getPositionForScrollProgress(RecyclerListView listView, float progress, int[] position) {
            position[0] = 0;
            position[1] = 0;
        }
    }

    public static View createEmptyStubView(Context context) {
        EmptyStubView emptyStubView = new EmptyStubView(context);
        emptyStubView.emptyImageView.setImageResource(R.drawable.photo_tooltip2);
        emptyStubView.emptyTextView.setText(LocaleController.getString("NoMedia", R.string.NoMedia));

        return emptyStubView;
    }
    private static class EmptyStubView extends LinearLayout {

        final TextView emptyTextView;
        final ImageView emptyImageView;

        boolean ignoreRequestLayout;

        public EmptyStubView(Context context) {
            super(context);
            emptyTextView = new TextView(context);
            emptyImageView = new ImageView(context);

            setOrientation(LinearLayout.VERTICAL);
            setGravity(Gravity.CENTER);

            addView(emptyImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            emptyTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            emptyTextView.setGravity(Gravity.CENTER);
            emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), AndroidUtilities.dp(128));
            addView(emptyTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 24, 0, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
            int rotation = manager.getDefaultDisplay().getRotation();
            ignoreRequestLayout = true;
            if (AndroidUtilities.isTablet()) {
                emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), AndroidUtilities.dp(128));
            } else {
                if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                    emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), 0);
                } else {
                    emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), AndroidUtilities.dp(128));
                }
            }
            ignoreRequestLayout = false;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        public void requestLayout() {
            if (ignoreRequestLayout) {
                return;
            }
            super.requestLayout();
        }
    }


}
