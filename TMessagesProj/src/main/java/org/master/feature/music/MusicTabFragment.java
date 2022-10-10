package org.master.feature.music;

import static org.telegram.ui.Components.RecyclerListView.SECTIONS_TYPE_DATE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Property;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedLinkCell;
import org.telegram.ui.Cells.SharedMediaSectionCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.ClippingImageView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
import org.telegram.ui.Components.StickerEmptyView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MusicTabFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {





    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged) {
            if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged) {
                for (int i = 0; i < mediaPages.length; i++) {
                    MediaPage mediaPage = mediaPages[i];
                    if(mediaPage.listView.getAdapter() instanceof DeviceMusicAdapter){
                        int count = mediaPages[i].listView.getChildCount();
                        for (int a = 0; a < count; a++) {
                            View view = mediaPages[i].listView.getChildAt(a);
                            if (view instanceof SharedAudioCell) {
                                SharedAudioCell cell = (SharedAudioCell) view;
                                MessageObject messageObject = cell.getMessage();
                                if (messageObject != null) {
                                    cell.updateButtonState(false, true);
                                }
                            }
                        }
                    }
                }

            } else if (id == NotificationCenter.messagePlayingDidStart) {
                MessageObject messageObject = (MessageObject) args[0];
                if (messageObject.eventId != 0) {
                    return;
                }
                for (int i = 0; i < mediaPages.length; i++) {
                    MediaPage mediaPage = mediaPages[i];
                    if(mediaPage.listView.getAdapter() instanceof DeviceMusicAdapter){
                        int count = mediaPages[i].listView.getChildCount();
                        for (int a = 0; a < count; a++) {
                            View view = mediaPages[i].listView.getChildAt(a);
                            if (view instanceof SharedAudioCell) {
                                SharedAudioCell cell = (SharedAudioCell) view;
                                MessageObject messageObject1 = cell.getMessage();
                                if (messageObject1 != null) {
                                    cell.updateButtonState(false, true);
                                }
                            }
                        }
                    }
                }

            }
        }else if(id == NotificationCenter.musicDidLoad){
            if(deviceMusicAdapter != null){
                deviceMusicAdapter.notifyDataSetChanged();
            }
        }else if(id == NotificationCenter.mediaCountDidLoad){
            long uid = (Long) args[0];
            int count = (Integer)args[1];
            if(count < MIN_MUSIC_COUNT){
                return;
            }
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(uid);
            if(dialog == null){
                return;
            }
            if(DialogObject.isChannel(dialog)){
                if(channels.contains(dialog)){
                    return;
                }
                channels.add(dialog);
            }else{
                if(botsList.contains(dialog)){
                    return;
                }
                botsList.add(dialog);
            }
            countHashmap.put(dialog.id,count);
            loadingChannel = false;
            loadingBots = false;
            if(botMusicAdapter != null){
                botMusicAdapter.notifyDataSetChanged();
            }

            if(channelMusicAdapter != null){
                channelMusicAdapter.notifyDataSetChanged();
            }
        }else if(id == NotificationCenter.dialogsNeedReload){
            //loadBots();
        }

    }


    private static final int MIN_MUSIC_COUNT  = 1;


    private HashMap<Long,Integer> countHashmap = new HashMap<>();
    private HashMap<Long, TLRPC.Dialog> dialogDict = new HashMap<>();
    private ArrayList<TLRPC.Dialog> botsList = new ArrayList<>();
    private ArrayList<TLRPC.Dialog> channels = new ArrayList<>();

    private void loadBots(){
        getMediaCountDatabase(getUserConfig().clientUserId);
        ArrayList<TLRPC.Dialog> dialogsBots = MessagesController.getInstance(currentAccount).dialogsUsersOnly;
        dialogsBots.add(dialogDict.get(getUserConfig().clientUserId));
        for(int a = 0; a < dialogsBots.size(); a++){
            TLRPC.Dialog dialog = dialogsBots.get(a);
            if(dialog == null){
                continue;
            }
            TLRPC.User user = getMessagesController().getUser(dialog.id);
            if(user == null){
                return;
            }

            if( (!user.bot)){
                continue;
            }
            getMediaCountDatabase(user.id);

        }


    }
    private void getMediaCountDatabase(long dialogId) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                int count = -1;
                int old = 0;
                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT count, old FROM media_counts_v2 WHERE uid = %d AND type = %d LIMIT 1", dialogId, MediaDataController.MEDIA_MUSIC));
                if (cursor.next()) {
                    count = cursor.intValue(0);
                    old = cursor.intValue(1);
                }
                cursor.dispose();
                final int count_finale = count;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if(count_finale > 0){
                         getNotificationCenter().postNotificationName(NotificationCenter.mediaCountDidLoad,dialogId,count_finale);
                        }
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void loadChannels(){
        ArrayList<TLRPC.Dialog> dialogsBots = MessagesController.getInstance(currentAccount).dialogsChannelsOnly;
        for (int i = 0; i < dialogsBots.size(); i++) {
            TLRPC.Dialog dialog = dialogsBots.get(i);
            if(dialog == null || dialogDict.containsKey(dialog.id)){
                continue;
            }
            dialogDict.put(dialog.id,dialog);
            getMediaDataController().getMediaCount(dialog.id, MediaDataController.MEDIA_MUSIC, classGuid, true);
        }

        if(channelMusicAdapter != null){
            channelMusicAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().addObserver(this,NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mediaCountDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.musicDidLoad);
        super.onFragmentDestroy();
    }


    public boolean onFragmentCreate() {
        getNotificationCenter().removeObserver(this,NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.mediaCountDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.musicDidLoad);

        getMessagesController().loadDialogs(0, -1, 1000, true);

        return super.onFragmentCreate();
    }

    private boolean loadingAudio;
    public  ArrayList<MediaController.AudioEntry> audioEntries = new ArrayList<>();
    public ArrayList<MessageObject> musicEntries = new ArrayList<>();


    private void loadAudio() {
        if(loadingAudio){
            return;
        }
        long start = System.currentTimeMillis();
        loadingAudio = true;
        Utilities.globalQueue.postRunnable(() -> {
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM
            };

            final ArrayList<MediaController.AudioEntry> newAudioEntries = new ArrayList<>();
            final ArrayList<MessageObject> newMusicEntries = new ArrayList<>();


            try (Cursor cursor = ApplicationLoader.applicationContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.TITLE)) {
                int id = -2000000000;
                while (cursor.moveToNext()) {
                    MediaController.AudioEntry audioEntry = new MediaController.AudioEntry();
                    audioEntry.id = cursor.getInt(0);
                    audioEntry.author = cursor.getString(1);
                    audioEntry.title = cursor.getString(2);
                    audioEntry.path = cursor.getString(3);
                    audioEntry.duration = (int) (cursor.getLong(4) / 1000);
                    audioEntry.genre = cursor.getString(5);


                    File file = new File(audioEntry.path);

                    TLRPC.TL_message message = new TLRPC.TL_message();
                    message.out = true;
                    message.id = id;
                    message.peer_id = new TLRPC.TL_peerUser();
                    message.from_id = new TLRPC.TL_peerUser();
                    message.peer_id.user_id = message.from_id.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                    message.date = (int) (System.currentTimeMillis() / 1000);
                    message.message = "";
                    message.attachPath = audioEntry.path;
                    message.media = new TLRPC.TL_messageMediaDocument();
                    message.media.flags |= 3;
                    message.media.document = new TLRPC.TL_document();
                    message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;

                    String ext = FileLoader.getFileExtension(file);

                    message.media.document.id = 0;
                    message.media.document.access_hash = 0;
                    message.media.document.file_reference = new byte[0];
                    message.media.document.date = message.date;
                    message.media.document.mime_type = "audio/" + (ext.length() > 0 ? ext : "mp3");
                    message.media.document.size = (int) file.length();
                    message.media.document.dc_id = 0;

                    TLRPC.TL_documentAttributeAudio attributeAudio = new TLRPC.TL_documentAttributeAudio();
                    attributeAudio.duration = audioEntry.duration;
                    attributeAudio.title = audioEntry.title;
                    attributeAudio.performer = audioEntry.author;
                    attributeAudio.flags |= 3;
                    message.media.document.attributes.add(attributeAudio);

                    TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
                    fileName.file_name = file.getName();
                    message.media.document.attributes.add(fileName);

                    audioEntry.messageObject = new MessageObject(currentAccount, message, false, true);
                    newMusicEntries.add(audioEntry.messageObject);
                    newAudioEntries.add(audioEntry);
                    id--;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> {
                long dur  =  System.currentTimeMillis() - start;
                loadingAudio = false;
                audioEntries = newAudioEntries;
                musicEntries = newMusicEntries;
                if(deviceMusicAdapter != null){
                    deviceMusicAdapter.notifyDataSetChanged();
                }
                //notifyDataSetChanged();
            });
        });
    }


    private static class MediaPage extends FrameLayout {
        private RecyclerListView listView;
//        private FlickerLoadingView progressView;
        private StickerEmptyView emptyView;
        private LinearLayoutManager layoutManager;
        private ClippingImageView animatingImageView;
        private int selectedType;

        public MediaPage(Context context) {
            super(context);
        }
    }

    private DeviceMusicAdapter deviceMusicAdapter;
    private BotMusicAdapter botMusicAdapter;
    private ChannelMusicAdapter channelMusicAdapter;
    private DeviceMusicSearchAdapter deviceMusicSearchAdapter;
    private AlbumAdapter albumAdapter;

    private MediaPage[] mediaPages = new MediaPage[2];
    private ActionBarMenuItem searchItem;
    private ActionBarMenuItem otherItem;
    private ActionBarMenuItem albumItem;

    private int searchItemState;
    private Drawable pinnedHeaderShadowDrawable;
    private boolean ignoreSearchCollapse;

    private FragmentContextView fragmentContextView;
    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;
    private Paint backgroundPaint = new Paint();

    private int maximumVelocity;

    private int additionalPadding;

    private boolean searchWas;
    private boolean searching;
    private boolean disableActionBarScrolling;

    private AnimatorSet tabsAnimation;
    private boolean tabsAnimationInProgress;
    private boolean animatingForward;
    private boolean backAnimation;

    private boolean swipeBackEnabled;

    private int columnsCount = 3;

    private boolean scrolling;


    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };

    private ArrayList<SharedAudioCell> audioCellCache = new ArrayList<>(10);
    private ArrayList<SharedAudioCell> audioCache = new ArrayList<>(10);


    private void setScrollY(float value) {
        actionBar.setTranslationY(value);
        if (fragmentContextView != null) {
            fragmentContextView.setTranslationY(additionalPadding + value);
        }
        for (int a = 0; a < mediaPages.length; a++) {
            mediaPages[a].listView.setPinnedSectionOffsetY((int) value);
        }
        fragmentView.invalidate();
    }


    private int initialTab;


    public final Property<MusicTabFragment, Float> SCROLL_Y = new AnimationProperties.FloatProperty<MusicTabFragment>("animationValue") {
        @Override
        public void setValue(MusicTabFragment object, float value) {
            object.setScrollY(value);
        }

        @Override
        public Float get(MusicTabFragment object) {
            return actionBar.getTranslationY();
        }
    };


    private void resetScroll() {
        if (actionBar.getTranslationY() == 0) {
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this, SCROLL_Y, 0));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(180);
        animatorSet.start();
    }

    private void openMusic(long dialog_id){
        Bundle args = new Bundle();
        args.putLong("dialog_id",dialog_id);
        args.putBoolean("audioOnly",true);
        MediaActivity mediaActivity = new MediaActivity(args,null);
        presentFragment(mediaActivity);
    }

    public MusicTabFragment(int initTab){
        initialTab = initTab;

    }



    @Override
    public View createView(Context context) {
        for (int a = 0; a < 10; a++) {
            SharedAudioCell cell = new SharedAudioCell(context);
            audioCellCache.add(cell);
        }

        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();

        searching = false;
        searchWas = false;
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAddToContainer(false);
        actionBar.setClipContent(true);
        actionBar.setTitle("Music");
        actionBar.setExtraHeight(AndroidUtilities.dp(44));
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                if(id == -1){
                   finishFragment();
                }else if(id == 3){
                   loadAudio();
                }else if(id == 4){

                }
            }
        });

        pinnedHeaderShadowDrawable = context.getResources().getDrawable(R.drawable.photos_header_shadow);
        pinnedHeaderShadowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundGrayShadow), PorterDuff.Mode.MULTIPLY));
        if (scrollSlidingTextTabStrip != null) {
            initialTab = scrollSlidingTextTabStrip.getCurrentTabId();
        }
        scrollSlidingTextTabStrip = new ScrollSlidingTextTabStrip(context);
        if (initialTab != -1) {
            scrollSlidingTextTabStrip.setInitialTabId(initialTab);
            initialTab = -1;
        }
        actionBar.addView(scrollSlidingTextTabStrip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.LEFT | Gravity.BOTTOM));
        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
            @Override
            public void onPageSelected(int id, boolean forward) {
                if (mediaPages[0].selectedType == id) {
                    return;
                }
                swipeBackEnabled = id == scrollSlidingTextTabStrip.getFirstTabId();
                mediaPages[1].selectedType = id;
                mediaPages[1].setVisibility(View.VISIBLE);
                switchToCurrentSelectedMode(true);
                animatingForward = forward;
            }

            @Override
            public void onPageScrolled(float progress) {
                if (progress == 1 && mediaPages[1].getVisibility() != View.VISIBLE) {
                    return;
                }
                if (animatingForward) {
                    mediaPages[0].setTranslationX(-progress * mediaPages[0].getMeasuredWidth());
                    mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() - progress * mediaPages[0].getMeasuredWidth());
                } else {
                    mediaPages[0].setTranslationX(progress * mediaPages[0].getMeasuredWidth());
                    mediaPages[1].setTranslationX(progress * mediaPages[0].getMeasuredWidth() - mediaPages[0].getMeasuredWidth());
                }
                if (searchItemState == 1) {
                    searchItem.setAlpha(progress);
                    otherItem.setAlpha(progress);
                } else if (searchItemState == 2) {
                    searchItem.setAlpha(1.0f - progress);
                    otherItem.setAlpha(1.0f - progress);

                }
                if (progress == 1) {
                    MediaPage tempPage = mediaPages[0];
                    mediaPages[0] = mediaPages[1];
                    mediaPages[1] = tempPage;
                    mediaPages[1].setVisibility(View.GONE);
                    if (searchItemState == 2) {
                        searchItem.setVisibility(View.INVISIBLE);
                        otherItem.setVisibility(View.INVISIBLE);

                    }
                    searchItemState = 0;
                }
            }
        });

        final ActionBarMenu menu = actionBar.createMenu();
        searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                resetScroll();
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchWas = false;
                deviceMusicSearchAdapter.search(null);
                if (ignoreSearchCollapse) {
                    ignoreSearchCollapse = false;
                    return;
                }

                switchToCurrentSelectedMode(false);
            }


            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                if (text.length() != 0) {
                    searchWas = true;
                    switchToCurrentSelectedMode(false);
                } else {
                    searchWas = false;
                    switchToCurrentSelectedMode(false);
                }
                if (mediaPages[0].selectedType == Tabs.DEVICE.ordinal()) {
                    if (deviceMusicSearchAdapter == null) {
                        return;
                    }
                    deviceMusicSearchAdapter.search(text);
                }
            }
        });
        searchItem.setSearchFieldHint(LocaleController.getString("Search", R.string.Search));
        searchItem.setContentDescription(LocaleController.getString("Search", R.string.Search));
        searchItem.setVisibility(View.INVISIBLE);

//        albumItem = menu.addItem(4,R.drawable.menu_add);
        otherItem = menu.addItem(2,R.drawable.ic_ab_other);
        otherItem.addSubItem(3,R.drawable.msg_reset,"Reload Music");

        deviceMusicAdapter = new DeviceMusicAdapter(context);
        botMusicAdapter  = new BotMusicAdapter(context);
        channelMusicAdapter  = new ChannelMusicAdapter(context);
        deviceMusicSearchAdapter = new DeviceMusicSearchAdapter(context);
        albumAdapter =  new AlbumAdapter(context);


        FrameLayout frameLayout;
        fragmentView = frameLayout = new FrameLayout(context) {

            private int startedTrackingPointerId;
            private boolean startedTracking;
            private boolean maybeStartTracking;
            private int startedTrackingX;
            private int startedTrackingY;
            private VelocityTracker velocityTracker;
            private boolean globalIgnoreLayout;

            private boolean prepareForMoving(MotionEvent ev, boolean forward) {
                int id = scrollSlidingTextTabStrip.getNextPageId(forward);
                if (id < 0) {
                    return false;
                }

                if (searchItemState != 0) {
                    if (searchItemState == 2) {
                        searchItem.setAlpha(1.0f);
                        otherItem.setAlpha(1.0f);

                    } else if (searchItemState == 1) {
                        searchItem.setAlpha(0.0f);
                        searchItem.setVisibility(View.INVISIBLE);
                        otherItem.setAlpha(0.0f);
                        otherItem.setVisibility(View.INVISIBLE);
                    }
                    searchItemState = 0;
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                maybeStartTracking = false;
                startedTracking = true;
                startedTrackingX = (int) ev.getX();
                actionBar.setEnabled(false);
                scrollSlidingTextTabStrip.setEnabled(false);
                mediaPages[1].selectedType = id;
                mediaPages[1].setVisibility(View.VISIBLE);
                animatingForward = forward;
                switchToCurrentSelectedMode(true);
                if (forward) {
                    mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth());
                } else {
                    mediaPages[1].setTranslationX(-mediaPages[0].getMeasuredWidth());
                }
                return true;
            }

            @Override
            public void forceHasOverlappingRendering(boolean hasOverlappingRendering) {
                super.forceHasOverlappingRendering(hasOverlappingRendering);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);

                setMeasuredDimension(widthSize, heightSize);

                measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);
                int actionBarHeight = actionBar.getMeasuredHeight();
                globalIgnoreLayout = true;
                for (int a = 0; a < mediaPages.length; a++) {
                    if (mediaPages[a] == null) {
                        continue;
                    }
                    if (mediaPages[a].listView != null) {
                        mediaPages[a].listView.setPadding(0, actionBarHeight + additionalPadding, 0, AndroidUtilities.dp(4));
                    }
                    if (mediaPages[a].emptyView != null) {
                        mediaPages[a].emptyView.setPadding(0, actionBarHeight + additionalPadding , 0, 0);
                    }
//                    if (mediaPages[a].progressView != null) {
//                        mediaPages[a].progressView.setPadding(0, actionBarHeight + additionalPadding, 0, 0);
//                    }
                }
                globalIgnoreLayout = false;

                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child == null || child.getVisibility() == GONE || child == actionBar) {
                        continue;
                    }
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                if (fragmentContextView != null) {
                    int y = actionBar.getMeasuredHeight();
                    fragmentContextView.layout(fragmentContextView.getLeft(), fragmentContextView.getTop() + y, fragmentContextView.getRight(), fragmentContextView.getBottom() + y);
                }
            }

            @Override
            public void setPadding(int left, int top, int right, int bottom) {
                additionalPadding = top;
                if (fragmentContextView != null) {
                    fragmentContextView.setTranslationY(top + actionBar.getTranslationY());
                }
                int actionBarHeight = actionBar.getMeasuredHeight();
                for (int a = 0; a < mediaPages.length; a++) {
                    if (mediaPages[a] == null) {
                        continue;
                    }
                    if (mediaPages[a].emptyView != null) {
                        mediaPages[a].emptyView.setPadding(0, actionBarHeight + additionalPadding, 0, 0);
                    }
//                    if (mediaPages[a].progressView != null) {
//                        mediaPages[a].progressView.setPadding(0, actionBarHeight + additionalPadding, 0, 0);
//                    }
                    if (mediaPages[a].listView != null) {
                        mediaPages[a].listView.setPadding(0, actionBarHeight + additionalPadding, 0, AndroidUtilities.dp(4));
                        mediaPages[a].listView.checkSection(true);
                    }
                }
                fixScrollOffset();
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (parentLayout != null) {
                    parentLayout.drawHeaderShadow(canvas, actionBar.getMeasuredHeight() + (int) actionBar.getTranslationY());
                }
                if (fragmentContextView != null && fragmentContextView.isCallStyle()) {
                    canvas.save();
                    canvas.translate(fragmentContextView.getX(), fragmentContextView.getY());
                    fragmentContextView.setDrawOverlay(true);
                    fragmentContextView.draw(canvas);
                    fragmentContextView.setDrawOverlay(false);
                    canvas.restore();
                }
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (child == fragmentContextView && fragmentContextView.isCallStyle()) {
                    return true;
                }
                return super.drawChild(canvas, child, drawingTime);
            }

            @Override
            public void requestLayout() {
                if (globalIgnoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            public boolean checkTabsAnimationInProgress() {
                if (tabsAnimationInProgress) {
                    boolean cancel = false;
                    if (backAnimation) {
                        if (Math.abs(mediaPages[0].getTranslationX()) < 1) {
                            mediaPages[0].setTranslationX(0);
                            mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() * (animatingForward ? 1 : -1));
                            cancel = true;
                        }
                    } else if (Math.abs(mediaPages[1].getTranslationX()) < 1) {
                        mediaPages[0].setTranslationX(mediaPages[0].getMeasuredWidth() * (animatingForward ? -1 : 1));
                        mediaPages[1].setTranslationX(0);
                        cancel = true;
                    }
                    if (cancel) {
                        if (tabsAnimation != null) {
                            tabsAnimation.cancel();
                            tabsAnimation = null;
                        }
                        tabsAnimationInProgress = false;
                    }
                    return tabsAnimationInProgress;
                }
                return false;
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return checkTabsAnimationInProgress() || scrollSlidingTextTabStrip.isAnimatingIndicator() || onTouchEvent(ev);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                backgroundPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                canvas.drawRect(0, actionBar.getMeasuredHeight() + actionBar.getTranslationY(), getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                if (!parentLayout.checkTransitionAnimation() && !checkTabsAnimationInProgress()) {
                    if (ev != null) {
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        }
                        velocityTracker.addMovement(ev);
                    }
                    if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && !startedTracking && !maybeStartTracking) {
                        startedTrackingPointerId = ev.getPointerId(0);
                        maybeStartTracking = true;
                        startedTrackingX = (int) ev.getX();
                        startedTrackingY = (int) ev.getY();
                        velocityTracker.clear();
                    } else if (ev != null && ev.getAction() == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
                        int dx = (int) (ev.getX() - startedTrackingX);
                        int dy = Math.abs((int) ev.getY() - startedTrackingY);
                        if (startedTracking && (animatingForward && dx > 0 || !animatingForward && dx < 0)) {
                            if (!prepareForMoving(ev, dx < 0)) {
                                maybeStartTracking = true;
                                startedTracking = false;
                                mediaPages[0].setTranslationX(0);
                                mediaPages[1].setTranslationX(animatingForward ? mediaPages[0].getMeasuredWidth() : -mediaPages[0].getMeasuredWidth());
                                scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, 0);
                            }
                        }
                        if (maybeStartTracking && !startedTracking) {
                            float touchSlop = AndroidUtilities.getPixelsInCM(0.3f, true);
                            if (Math.abs(dx) >= touchSlop && Math.abs(dx) > dy) {
                                prepareForMoving(ev, dx < 0);
                            }
                        } else if (startedTracking) {
                            mediaPages[0].setTranslationX(dx);
                            if (animatingForward) {
                                mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() + dx);
                            } else {
                                mediaPages[1].setTranslationX(dx - mediaPages[0].getMeasuredWidth());
                            }
                            float scrollProgress = Math.abs(dx) / (float) mediaPages[0].getMeasuredWidth();
                            if (searchItemState == 2) {
                                searchItem.setAlpha(1.0f - scrollProgress);
                            } else if (searchItemState == 1) {
                                searchItem.setAlpha(scrollProgress);
                                otherItem.setAlpha(scrollProgress);

                            }
                            scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
                        }
                    } else if (ev == null || ev.getPointerId(0) == startedTrackingPointerId && (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_POINTER_UP)) {
                        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                        float velX;
                        float velY;
                        if (ev != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
                            velX = velocityTracker.getXVelocity();
                            velY = velocityTracker.getYVelocity();
                            if (!startedTracking) {
                                if (Math.abs(velX) >= 3000 && Math.abs(velX) > Math.abs(velY)) {
                                    prepareForMoving(ev, velX < 0);
                                }
                            }
                        } else {
                            velX = 0;
                            velY = 0;
                        }
                        if (startedTracking) {
                            float x = mediaPages[0].getX();
                            tabsAnimation = new AnimatorSet();
                            backAnimation = Math.abs(x) < mediaPages[0].getMeasuredWidth() / 3.0f && (Math.abs(velX) < 3500 || Math.abs(velX) < Math.abs(velY));
                            float distToMove;
                            float dx;
                            if (backAnimation) {
                                dx = Math.abs(x);
                                if (animatingForward) {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, 0),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, mediaPages[1].getMeasuredWidth())
                                    );
                                } else {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, 0),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, -mediaPages[1].getMeasuredWidth())
                                    );
                                }
                            } else {
                                dx = mediaPages[0].getMeasuredWidth() - Math.abs(x);
                                if (animatingForward) {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, -mediaPages[0].getMeasuredWidth()),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, 0)
                                    );
                                } else {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, mediaPages[0].getMeasuredWidth()),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, 0)
                                    );
                                }
                            }
                            tabsAnimation.setInterpolator(interpolator);

                            int width = getMeasuredWidth();
                            int halfWidth = width / 2;
                            float distanceRatio = Math.min(1.0f, 1.0f * dx / (float) width);
                            float distance = (float) halfWidth + (float) halfWidth * AndroidUtilities.distanceInfluenceForSnapDuration(distanceRatio);
                            velX = Math.abs(velX);
                            int duration;
                            if (velX > 0) {
                                duration = 4 * Math.round(1000.0f * Math.abs(distance / velX));
                            } else {
                                float pageDelta = dx / getMeasuredWidth();
                                duration = (int) ((pageDelta + 1.0f) * 100.0f);
                            }
                            duration = Math.max(150, Math.min(duration, 600));

                            tabsAnimation.setDuration(duration);
                            tabsAnimation.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    tabsAnimation = null;
                                    if (backAnimation) {
                                        mediaPages[1].setVisibility(View.GONE);
                                        if (searchItemState == 2) {
                                            searchItem.setAlpha(1.0f);
                                            otherItem.setAlpha(1.0f);

                                        } else if (searchItemState == 1) {
                                            searchItem.setAlpha(0.0f);
                                            searchItem.setVisibility(View.INVISIBLE);
                                            otherItem.setAlpha(0.0f);
                                            otherItem.setVisibility(View.INVISIBLE);
                                        }
                                        searchItemState = 0;
                                    } else {
                                        MediaPage tempPage = mediaPages[0];
                                        mediaPages[0] = mediaPages[1];
                                        mediaPages[1] = tempPage;
                                        mediaPages[1].setVisibility(View.GONE);
                                        if (searchItemState == 2) {
                                            searchItem.setVisibility(View.INVISIBLE);
                                            otherItem.setVisibility(View.INVISIBLE);

                                        }
                                        searchItemState = 0;
                                        swipeBackEnabled = mediaPages[0].selectedType == scrollSlidingTextTabStrip.getFirstTabId();
                                        scrollSlidingTextTabStrip.selectTabWithId(mediaPages[0].selectedType, 1.0f);
                                    }
                                    tabsAnimationInProgress = false;
                                    maybeStartTracking = false;
                                    startedTracking = false;
                                    actionBar.setEnabled(true);
                                    scrollSlidingTextTabStrip.setEnabled(true);
                                }
                            });
                            tabsAnimation.start();
                            tabsAnimationInProgress = true;
                            startedTracking = false;
                        } else {
                            maybeStartTracking = false;
                            actionBar.setEnabled(true);
                            scrollSlidingTextTabStrip.setEnabled(true);
                        }
                        if (velocityTracker != null) {
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                    }
                    return startedTracking;
                }
                return false;
            }
        };
        frameLayout.setWillNotDraw(false);

        int scrollToPositionOnRecreate = -1;
        int scrollToOffsetOnRecreate = 0;


        mediaPages = new MediaPage[2];
        for (int a = 0; a < mediaPages.length; a++) {
            if (a == 0) {
                if (mediaPages[a] != null && mediaPages[a].layoutManager != null) {
                    scrollToPositionOnRecreate = mediaPages[a].layoutManager.findFirstVisibleItemPosition();
                    if (scrollToPositionOnRecreate != mediaPages[a].layoutManager.getItemCount() - 1) {
                        RecyclerListView.Holder holder = (RecyclerListView.Holder) mediaPages[a].listView.findViewHolderForAdapterPosition(scrollToPositionOnRecreate);
                        if (holder != null) {
                            scrollToOffsetOnRecreate = holder.itemView.getTop();
                        } else {
                            scrollToPositionOnRecreate = -1;
                        }
                    } else {
                        scrollToPositionOnRecreate = -1;
                    }
                }
            }
            final MediaPage mediaPage = new MediaPage(context) {
                @Override
                public void setTranslationX(float translationX) {
                    super.setTranslationX(translationX);
                    if (tabsAnimationInProgress) {
                        if (mediaPages[0] == this) {
                            float scrollProgress = Math.abs(mediaPages[0].getTranslationX()) / (float) mediaPages[0].getMeasuredWidth();
                            scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
                            if (searchItemState == 2) {
                                searchItem.setAlpha(1.0f - scrollProgress);
                                otherItem.setAlpha(1.0f - scrollProgress);

                            } else if (searchItemState == 1) {
                                searchItem.setAlpha(scrollProgress);
                                otherItem.setAlpha(scrollProgress);

                            }
                        }
                    }
                }
            };
            frameLayout.addView(mediaPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a] = mediaPage;

            final LinearLayoutManager layoutManager = mediaPages[a].layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }

                @Override
                protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extraLayoutSpace) {
                    super.calculateExtraLayoutSpace(state, extraLayoutSpace);
                    if (mediaPage.selectedType == 0) {
                        extraLayoutSpace[1] = Math.max(extraLayoutSpace[1], SharedPhotoVideoCell.getItemSize(columnsCount) * 2);
                    } else if (mediaPage.selectedType == 1) {
                        extraLayoutSpace[1] = Math.max(extraLayoutSpace[1], AndroidUtilities.dp(56f) * 2);
                    }
                }
            };
            mediaPages[a].listView = new RecyclerListView(context);
            mediaPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
            mediaPages[a].listView.setItemAnimator(null);
            mediaPages[a].listView.setClipToPadding(false);
            mediaPages[a].listView.setSectionsType(SECTIONS_TYPE_DATE);
            mediaPages[a].listView.setLayoutManager(layoutManager);
            mediaPages[a].addView(mediaPages[a].listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a].listView.setOnItemClickListener((view, position) -> {
                if (mediaPage.selectedType == Tabs.CHANNEL.ordinal()  && view instanceof UserCell) {
                    openMusic(channels.get(position).id);
                } else if (mediaPage.selectedType == Tabs.BOTS.ordinal()  && view instanceof UserCell) {
                    openMusic(botsList.get(position).id);
                }else if(mediaPage.selectedType  == Tabs.DEVICE.ordinal() && view instanceof SharedAudioCell){
                    SharedAudioCell audioCell = (SharedAudioCell) view;
                    playMusic(audioCell.getMessage());
                }
            });
            mediaPages[a].listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                    scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                    if (newState != RecyclerView.SCROLL_STATE_DRAGGING) {
                        int scrollY = (int) -actionBar.getTranslationY();
                        int actionBarHeight = ActionBar.getCurrentActionBarHeight();
                        if (scrollY != 0 && scrollY != actionBarHeight) {
                            if (scrollY < actionBarHeight / 2) {
                                mediaPages[0].listView.smoothScrollBy(0, -scrollY);
                            } else if (mediaPages[0].listView.canScrollVertically(1)) {
                                mediaPages[0].listView.smoothScrollBy(0, actionBarHeight - scrollY);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (searching && searchWas) {
                        return;
                    }
                    if (recyclerView == mediaPages[0].listView && !searching && !actionBar.isActionModeShowed() && !disableActionBarScrolling) {
                        float currentTranslation = actionBar.getTranslationY();
                        float newTranslation = currentTranslation - dy;
                        if (newTranslation < -ActionBar.getCurrentActionBarHeight()) {
                            newTranslation = -ActionBar.getCurrentActionBarHeight();
                        } else if (newTranslation > 0) {
                            newTranslation = 0;
                        }
                        if (newTranslation != currentTranslation) {
                            setScrollY(newTranslation);
                        }
                    }
                }
            });
            if (a == 0 && scrollToPositionOnRecreate != -1) {
                layoutManager.scrollToPositionWithOffset(scrollToPositionOnRecreate, scrollToOffsetOnRecreate);
            }

            final RecyclerListView listView = mediaPages[a].listView;
            mediaPages[a].animatingImageView = new ClippingImageView(context) {
                @Override
                public void invalidate() {
                    super.invalidate();
                    listView.invalidate();
                }
            };
            mediaPages[a].animatingImageView.setVisibility(View.GONE);
            mediaPages[a].listView.addOverlayView(mediaPages[a].animatingImageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

//            mediaPages[a].progressView = new FlickerLoadingView(context) {
//
//                @Override
//                public int getColumnsCount() {
//                    return columnsCount;
//                }
//
//                @Override
//                public int getViewType() {
//                    return FlickerLoadingView.USERS_TYPE;
//                }
//
//                @Override
//                protected void onDraw(Canvas canvas) {
//                    backgroundPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//                    canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
//                    super.onDraw(canvas);
//                }
//            };
//            mediaPages[a].progressView.setUseHeaderOffset(true);
//            mediaPages[a].progressView.showDate(false);
            if (a != 0) {
                mediaPages[a].setVisibility(View.GONE);
            }

            mediaPages[a].emptyView = new StickerEmptyView(context, null, StickerEmptyView.STICKER_TYPE_SEARCH);
            mediaPages[a].emptyView.setVisibility(View.GONE);
            mediaPages[a].emptyView.setAnimateLayoutChange(true);
            mediaPages[a].addView(mediaPages[a].emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a].emptyView.setOnTouchListener((v, event) -> true);
            mediaPages[a].emptyView.showProgress(false, false);
            mediaPages[a].emptyView.title.setText(LocaleController.getString("NoResult", R.string.NoResult));
            mediaPages[a].emptyView.subtitle.setText("");

            mediaPages[a].listView.setEmptyView(mediaPages[a].emptyView);
            mediaPages[a].listView.setAnimateEmptyView(true, 0);
        }

        if (!AndroidUtilities.isTablet()) {
            frameLayout.addView(fragmentContextView = new FragmentContextView(context, this, false), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, 8, 0, 0));
        }

        frameLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateTabs();
        switchToCurrentSelectedMode(false);
        swipeBackEnabled = scrollSlidingTextTabStrip.getCurrentTabId() == scrollSlidingTextTabStrip.getFirstTabId();


        loadAudio();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        scrolling = true;
        if (channelMusicAdapter != null) {
            channelMusicAdapter.notifyDataSetChanged();
        }
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
        if(botMusicAdapter != null){
            botMusicAdapter.notifyDataSetChanged();
        }

        if(deviceMusicAdapter != null){
            deviceMusicAdapter.notifyDataSetChanged();
        }
        for (int a = 0; a < mediaPages.length; a++) {
            fixLayoutInternal(a);
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        for (int a = 0; a < mediaPages.length; a++) {
            if (mediaPages[a].listView != null) {
                final int num = a;
                ViewTreeObserver obs = mediaPages[a].listView.getViewTreeObserver();
                obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mediaPages[num].getViewTreeObserver().removeOnPreDrawListener(this);
                        fixLayoutInternal(num);
                        return true;
                    }
                });
            }
        }
    }


    public  enum Tabs{
        DEVICE,
        CHANNEL,
        BOTS,
        ALBUM
    }



    private void updateTabs() {
        if (scrollSlidingTextTabStrip == null) {
            return;
        }

//
//        if (!scrollSlidingTextTabStrip.hasTab(Tabs.ALBUM.ordinal())) {
//            scrollSlidingTextTabStrip.addTextTab(Tabs.ALBUM.ordinal(), "Album");
//        }


        if (!scrollSlidingTextTabStrip.hasTab(Tabs.CHANNEL.ordinal())) {
            scrollSlidingTextTabStrip.addTextTab(Tabs.CHANNEL.ordinal(), "Channels");
        }



        if (!scrollSlidingTextTabStrip.hasTab(Tabs.BOTS.ordinal())) {
            scrollSlidingTextTabStrip.addTextTab(Tabs.BOTS.ordinal(), "Bots");
        }

        if (!scrollSlidingTextTabStrip.hasTab(Tabs.DEVICE.ordinal())) {
            scrollSlidingTextTabStrip.addTextTab(Tabs.DEVICE.ordinal(), "Device");
        }


        if (scrollSlidingTextTabStrip.getTabsCount() <= 1) {
            scrollSlidingTextTabStrip.setVisibility(View.GONE);
            actionBar.setExtraHeight(0);
        } else {
            scrollSlidingTextTabStrip.setVisibility(View.VISIBLE);
            actionBar.setExtraHeight(AndroidUtilities.dp(44));
        }
        int id = scrollSlidingTextTabStrip.getCurrentTabId();
        if (id >= 0) {
            mediaPages[0].selectedType = id;
        }
        scrollSlidingTextTabStrip.finishAddingTabs();
    }

    private void recycleAdapter(RecyclerView.Adapter adapter) {
         if (adapter == deviceMusicAdapter) {
            audioCellCache.addAll(audioCache);
            audioCache.clear();
        }
    }
    private void fixLayoutInternal(int num) {
    }

    private void fixScrollOffset() {
        if (actionBar.getTranslationY() != 0f) {
            final RecyclerListView listView = mediaPages[0].listView;
            final View child = listView.getChildAt(0);
            if (child != null) {
                final int offset = (int) (child.getY() - (actionBar.getMeasuredHeight() + actionBar.getTranslationY() + additionalPadding));
                if (offset > 0) {
                    scrollWithoutActionBar(listView, offset);
                }
            }
        }
    }

    private void scrollWithoutActionBar(RecyclerView listView, int dy) {
        disableActionBarScrolling = true;
        listView.scrollBy(0, dy);
        disableActionBarScrolling = false;
    }


    private boolean loadingChannel;
    private boolean loadingBots;



    private void switchToCurrentSelectedMode(boolean animated) {
        for (int a = 0; a < mediaPages.length; a++) {
            mediaPages[a].listView.stopScroll();
        }
        int a = animated ? 1 : 0;
        RecyclerView.Adapter currentAdapter = mediaPages[a].listView.getAdapter();
        if (searching && searchWas) {
            if (animated) {
                if (mediaPages[a].selectedType == Tabs.CHANNEL.ordinal() || mediaPages[a].selectedType == Tabs.BOTS.ordinal()) {
                    searching = false;
                    searchWas = false;
                    switchToCurrentSelectedMode(true);
                    return;
                } else {
                    String text = searchItem.getSearchField().getText().toString();
                    if (mediaPages[a].selectedType == Tabs.DEVICE.ordinal()) {
                        if (deviceMusicSearchAdapter != null) {
                            deviceMusicSearchAdapter.search(text);
                            if (currentAdapter != deviceMusicSearchAdapter) {
                                recycleAdapter(currentAdapter);
                                mediaPages[a].listView.setAdapter(deviceMusicSearchAdapter);
                            }
                        }
                    }
                }
            } else {
                if (mediaPages[a].listView != null) {
                    if (mediaPages[a].selectedType == Tabs.DEVICE.ordinal()) {
                        if (currentAdapter != deviceMusicSearchAdapter) {
                            recycleAdapter(currentAdapter);
                            mediaPages[a].listView.setAdapter(deviceMusicSearchAdapter);
                        }
                        deviceMusicSearchAdapter.notifyDataSetChanged();
                    }
                }
            }

        } else {
            mediaPages[a].listView.setPinnedHeaderShadowDrawable(null);

            if (mediaPages[a].selectedType == Tabs.DEVICE.ordinal()) {
                if (currentAdapter != deviceMusicAdapter) {
                    recycleAdapter(currentAdapter);
                    mediaPages[a].listView.setAdapter(deviceMusicAdapter);
                }
                mediaPages[a].listView.setPinnedHeaderShadowDrawable(pinnedHeaderShadowDrawable);
            } else if (mediaPages[a].selectedType == Tabs.CHANNEL.ordinal()) {
                if (currentAdapter != channelMusicAdapter) {
                    recycleAdapter(currentAdapter);
                    mediaPages[a].listView.setAdapter(channelMusicAdapter);
                }
            } else if (mediaPages[a].selectedType == Tabs.BOTS.ordinal()) {
                if (currentAdapter != botMusicAdapter) {
                    recycleAdapter(currentAdapter);
                    mediaPages[a].listView.setAdapter(botMusicAdapter);
                }
            }else if (mediaPages[a].selectedType == Tabs.ALBUM.ordinal()) {
                if (currentAdapter != albumAdapter) {
                    recycleAdapter(currentAdapter);
                    mediaPages[a].listView.setAdapter(albumAdapter);
                }
            }
            if (mediaPages[a].selectedType == Tabs.BOTS.ordinal() || mediaPages[a].selectedType == Tabs.CHANNEL.ordinal()) {
                if (animated) {
                    searchItemState = 2;
                } else {
                    searchItemState = 0;
                    searchItem.setVisibility(View.INVISIBLE);
                    otherItem.setVisibility(View.INVISIBLE);

                }
            } else {
                if (animated) {
                    if (searchItem.getVisibility() == View.INVISIBLE && !actionBar.isSearchFieldVisible()) {
                        searchItemState = 1;
                        searchItem.setVisibility(View.VISIBLE);
                        searchItem.setAlpha(0.0f);
                        otherItem.setVisibility(View.VISIBLE);
                        otherItem.setAlpha(0.0f);
                    } else {
                        searchItemState = 0;
                    }
                } else if (searchItem.getVisibility() == View.INVISIBLE) {
                    searchItemState = 0;
                    searchItem.setAlpha(1.0f);
                    searchItem.setVisibility(View.VISIBLE);
                    otherItem.setAlpha(1.0f);
                    otherItem.setVisibility(View.VISIBLE);
                }
            }

            if(mediaPages[a].selectedType == Tabs.CHANNEL.ordinal() && !loadingChannel && channels.isEmpty()){
                loadingChannel = true;
                loadChannels();
            }else if(mediaPages[a].selectedType == Tabs.BOTS.ordinal()){
                mediaPages[a].listView.setEmptyView(mediaPages[a].emptyView);
                if(botsList.isEmpty()){
                    loadingBots = true;
                    loadBots();
                }else{
                    loadingBots = false;

                }

            }
            mediaPages[a].listView.setVisibility(View.VISIBLE);
        }
        if (searchItemState == 2 && actionBar.isSearchFieldVisible()) {
            ignoreSearchCollapse = true;
            actionBar.closeSearchField();
        }

        if (actionBar.getTranslationY() != 0) {
            mediaPages[a].layoutManager.scrollToPositionWithOffset(0, (int) actionBar.getTranslationY());
        }
    }

    private boolean playMusic(MessageObject messageObject){
//        ArrayList<MessageObject> arrayList = new ArrayList<>();
//        arrayList.add(musicEntries);
        return MediaController.getInstance().setPlaylist(musicEntries, messageObject, 0);
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return swipeBackEnabled;
    }

    private  class DeviceMusicAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public DeviceMusicAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            if ( audioEntries.isEmpty()) {
                return 1;
            }
            return   audioEntries.size() + 1;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                SharedAudioCell sharedAudioCell = new SharedAudioCell(mContext) {
                    @Override
                    public boolean needPlayMessage(MessageObject messageObject) {
                        return playMusic(messageObject);
                    }
                };
                sharedAudioCell.setCheckForButtonPress(true);
                view = sharedAudioCell;
            } else {
                view = new View(mContext);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                MediaController.AudioEntry audioEntry =  audioEntries.get(position);

                SharedAudioCell audioCell = (SharedAudioCell) holder.itemView;
                audioCell.setTag(audioEntry);
                audioCell.setMessageObject(audioEntry.messageObject, position !=   audioEntries.size() - 1);
            }
        }

        @Override
        public int getItemViewType(int i) {
            if (i == getItemCount() - 1) {
                return 2;
            }
            return 0;
        }
    }


    private class BotMusicAdapter extends RecyclerListView.SelectionAdapter{

        private Context mContext;

        public BotMusicAdapter(Context context) {
            this.mContext = context;
        }


        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            UserCell view = new UserCell(mContext,8,0,false);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserCell userCell = (UserCell) holder.itemView;
            TLRPC.Dialog dialog = botsList.get(position);
            if(dialog != null){
                TLRPC.User chat = getMessagesController().getUser(dialog.id);
                if(chat != null){
                    int count = countHashmap.get(dialog.id);
                    userCell.setData(chat, UserObject.getFirstName(chat),String.format("%d Music files",count),0,position != botsList.size() - 1);

                }

            }
        }


        @Override
        public int getItemCount() {
            return botsList != null? botsList.size():0;
        }
    }

    private class ChannelMusicAdapter extends RecyclerListView.SelectionAdapter{

        private Context mContext;

        public ChannelMusicAdapter(Context context) {
            this.mContext = context;
        }


        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            UserCell view = new UserCell(mContext,8,0,false);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserCell userCell = (UserCell) holder.itemView;
            TLRPC.Dialog dialog = channels.get(position);
            if(dialog != null){
                TLRPC.Chat chat = getMessagesController().getChat(-dialog.id);
                if(chat != null){
                    String name = chat.title;
                    int count = countHashmap.get(dialog.id);
                    userCell.setData(chat,name,String.format("%d Music files",count),0,position != channels.size() - 1);
                }

            }

        }

        @Override
        public int getItemCount() {
            return channels != null? channels.size():0;
        }
    }

    public class DeviceMusicSearchAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;
        private ArrayList<MediaController.AudioEntry> searchResult = new ArrayList<>();
        private Runnable searchRunnable;
        private int lastSearchId;
        private int reqId = 0;
        private int lastReqId;

        public DeviceMusicSearchAdapter(Context context) {
            mContext = context;
        }

        public void search(final String query) {
            if (searchRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(searchRunnable);
                searchRunnable = null;
            }
            if (TextUtils.isEmpty(query)) {
                if (!searchResult.isEmpty()) {
                    searchResult.clear();
                }
//                if (listView.getAdapter() != listAdapter) {
//                    listView.setAdapter(listAdapter);
//                }
                notifyDataSetChanged();
            } else {
                int searchId = ++lastSearchId;
                AndroidUtilities.runOnUIThread(searchRunnable = () -> {
                    final ArrayList<MediaController.AudioEntry> copy = new ArrayList<>( audioEntries);
                    Utilities.searchQueue.postRunnable(() -> {
                        String search1 = query.trim().toLowerCase();
                        if (search1.length() == 0) {
                            updateSearchResults(new ArrayList<>(), query, lastSearchId);
                            return;
                        }
                        String search2 = LocaleController.getInstance().getTranslitString(search1);
                        if (search1.equals(search2) || search2.length() == 0) {
                            search2 = null;
                        }
                        String[] search = new String[1 + (search2 != null ? 1 : 0)];
                        search[0] = search1;
                        if (search2 != null) {
                            search[1] = search2;
                        }

                        ArrayList<MediaController.AudioEntry> resultArray = new ArrayList<>();

                        for (int a = 0; a < copy.size(); a++) {
                            MediaController.AudioEntry entry = copy.get(a);
                            for (int b = 0; b < search.length; b++) {
                                String q = search[b];

                                boolean ok = false;
                                if (entry.author != null) {
                                    ok = entry.author.toLowerCase().contains(q);
                                }
                                if (!ok && entry.title != null) {
                                    ok = entry.title.toLowerCase().contains(q);
                                }
                                if (ok) {
                                    resultArray.add(entry);
                                    break;
                                }
                            }
                        }

                        updateSearchResults(resultArray, query, searchId);
                    });
                }, 300);
            }
        }

        private void updateSearchResults(final ArrayList<MediaController.AudioEntry> result, String query, final int searchId) {
            AndroidUtilities.runOnUIThread(() -> {
                if (searchId != lastSearchId) {
                    return;
                }
//                if (searchId != -1 && listView.getAdapter() != searchAdapter) {
//                    listView.setAdapter(searchAdapter);
//                }
//                if (listView.getAdapter() == searchAdapter) {
//                    emptySubtitleTextView.setText(AndroidUtilities.replaceTags(LocaleController.formatString("NoAudioFoundInfo", R.string.NoAudioFoundInfo, query)));
//                }
                int count = getItemCount();
                for (int a = 0; a < mediaPages.length; a++) {
                    if (mediaPages[a].listView.getAdapter() == this && count == 0 && actionBar.getTranslationY() != 0) {
                        mediaPages[a].layoutManager.scrollToPositionWithOffset(0, (int) actionBar.getTranslationY());
                        break;
                    }
                }
                searchResult = result;
                notifyDataSetChanged();
            });
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            //updateEmptyView();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 0;
        }

        @Override
        public int getItemCount() {
            if (searchResult.isEmpty()) {
                return 1;
            }
            return searchResult.size() + (searchResult.isEmpty() ? 0 : 1);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    SharedAudioCell sharedAudioCell = new SharedAudioCell(mContext) {
                        @Override
                        public boolean needPlayMessage(MessageObject messageObject) {
                            return playMusic(messageObject);

                        }
                    };
                    sharedAudioCell.setCheckForButtonPress(true);
                    view = sharedAudioCell;
                    break;
                default:
                    view = new View(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                MediaController.AudioEntry audioEntry = searchResult.get(position);

                SharedAudioCell audioCell = (SharedAudioCell) holder.itemView;
                audioCell.setTag(audioEntry);
                audioCell.setMessageObject(audioEntry.messageObject, position != searchResult.size() - 1);
                //  audioCell.setChecked(selectedAudios.indexOfKey(audioEntry.id) >= 0, false);
            }
        }

        @Override
        public int getItemViewType(int i) {
            if (i == getItemCount() - 1) {
                return 2;
            }

            return 0;
        }
    }




    private class AlbumAdapter extends RecyclerListView.SelectionAdapter{

        private Context mContext;

        public AlbumAdapter(Context context) {
            this.mContext = context;
        }


        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            UserCell view = new UserCell(mContext,8,0,false);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserCell userCell = (UserCell) holder.itemView;
            TLRPC.Dialog dialog = botsList.get(position);
            if(dialog != null){
                TLRPC.User chat = getMessagesController().getUser(dialog.id);
                if(chat != null){
                    int count = countHashmap.get(dialog.id);
                    userCell.setData(chat, UserObject.getFirstName(chat),String.format("%d Music files",count),0,position != channels.size() - 1);
                }

            }
        }

        @Override
        public int getItemCount() {
            return botsList != null? botsList.size():0;
        }
    }



    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();

        arrayList.add(new ThemeDescription(fragmentView, 0, null, null, null, null, Theme.key_windowBackgroundWhite));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubmenuItemIcon));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));


        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerBackground));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"playButton"}, null, null, null, Theme.key_inappPlayerPlayPause));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerTitle));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_FASTSCROLL, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerPerformer));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"closeButton"}, null, null, null, Theme.key_inappPlayerClose));

        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_returnToCallBackground));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_returnToCallText));

        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip, 0, new Class[]{ScrollSlidingTextTabStrip.class}, new String[]{"selectorDrawable"}, null, null, null, Theme.key_actionBarTabLine));
        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip.getTabsContainer(), ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextView.class}, null, null, null, Theme.key_actionBarTabActiveText));
        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip.getTabsContainer(), ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextView.class}, null, null, null, Theme.key_actionBarTabUnactiveText));
        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip.getTabsContainer(), ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{TextView.class}, null, null, null, Theme.key_actionBarTabSelector));

        for (int a = 0; a < mediaPages.length; a++) {
            final int num = a;
            ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
                if (mediaPages[num].listView != null) {
                    int count = mediaPages[num].listView.getChildCount();
                    for (int a1 = 0; a1 < count; a1++) {
                        View child = mediaPages[num].listView.getChildAt(a1);
                        if (child instanceof SharedPhotoVideoCell) {
                            ((SharedPhotoVideoCell) child).updateCheckboxColor();
                        }
                    }
                }
            };

            arrayList.add(new ThemeDescription(mediaPages[a].emptyView, 0, null, null, null, null, Theme.key_windowBackgroundGray));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
            arrayList.add(new ThemeDescription(mediaPages[a].emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_SECTIONS, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_graySectionText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_SECTIONS, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{SharedDocumentCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{SharedDocumentCell.class}, new String[]{"dateTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText3));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_PROGRESSBAR, new Class[]{SharedDocumentCell.class}, new String[]{"progressView"}, null, null, null, Theme.key_sharedMedia_startStopLoadIcon));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{SharedDocumentCell.class}, new String[]{"statusImageView"}, null, null, null, Theme.key_sharedMedia_startStopLoadIcon));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{SharedDocumentCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkbox));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{SharedDocumentCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{SharedDocumentCell.class}, new String[]{"thumbImageView"}, null, null, null, Theme.key_files_folderIcon));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{SharedDocumentCell.class}, new String[]{"extTextView"}, null, null, null, Theme.key_files_iconText));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{SharedAudioCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkbox));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{SharedAudioCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{SharedAudioCell.class}, Theme.chat_contextResult_titleTextPaint, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{SharedAudioCell.class}, Theme.chat_contextResult_descriptionTextPaint, null, null, Theme.key_windowBackgroundWhiteGrayText2));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{SharedLinkCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkbox));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{SharedLinkCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{SharedLinkCell.class}, new String[]{"titleTextPaint"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{SharedLinkCell.class}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{SharedLinkCell.class}, Theme.linkSelectionPaint, null, null, Theme.key_windowBackgroundWhiteLinkSelection));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{SharedLinkCell.class}, new String[]{"letterDrawable"}, null, null, null, Theme.key_sharedMedia_linkPlaceholderText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{SharedLinkCell.class}, new String[]{"letterDrawable"}, null, null, null, Theme.key_sharedMedia_linkPlaceholder));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_SECTIONS, new Class[]{SharedMediaSectionCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_SECTIONS, new Class[]{SharedMediaSectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{SharedMediaSectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{SharedPhotoVideoCell.class}, new String[]{"backgroundPaint"}, null, null, null, Theme.key_sharedMedia_photoPlaceholder));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{SharedPhotoVideoCell.class}, null, null, cellDelegate, Theme.key_checkbox));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{SharedPhotoVideoCell.class}, null, null, cellDelegate, Theme.key_checkboxCheck));

            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, null, null, new Drawable[]{pinnedHeaderShadowDrawable}, null, Theme.key_windowBackgroundGrayShadow));

            arrayList.add(new ThemeDescription(mediaPages[a].emptyView.title, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPages[a].emptyView.subtitle, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText));
        }

        return arrayList;
    }


}
