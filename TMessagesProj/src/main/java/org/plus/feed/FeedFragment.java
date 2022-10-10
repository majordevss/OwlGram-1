package org.plus.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.plus.database.DataStorage;
import org.plus.database.TableModels;
import org.plus.swiperefresh.SwipeRefreshLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FeedFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate,ImageUpdater.ImageUpdaterDelegate{



    private TLRPC.FileLocation avatar;
    private TLRPC.FileLocation avatarBig;

    @Override
    public void didUploadPhoto(TLRPC.InputFile photo, TLRPC.InputFile video, double videoStartTimestamp, String videoPath, TLRPC.PhotoSize bigSize, TLRPC.PhotoSize smallSize) {
        AndroidUtilities.runOnUIThread(() -> {
            if (photo != null || video != null) {
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                if (photo != null) {
                    req.file = photo;
                    req.flags |= 1;
                }
                if (video != null) {
                    req.video = video;
                    req.flags |= 2;
                    req.video_start_ts = videoStartTimestamp;
                    req.flags |= 4;
                }
                getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        TLRPC.User user = getMessagesController().getUser(getUserConfig().getClientUserId());
                        if (user == null) {
                            user = getUserConfig().getCurrentUser();
                            if (user == null) {
                                return;
                            }
                            getMessagesController().putUser(user, false);
                        } else {
                            getUserConfig().setCurrentUser(user);
                        }
                        TLRPC.TL_photos_photo photos_photo = (TLRPC.TL_photos_photo) response;
                        ArrayList<TLRPC.PhotoSize> sizes = photos_photo.photo.sizes;
                        TLRPC.PhotoSize small = FileLoader.getClosestPhotoSizeWithSize(sizes, 150);
                        TLRPC.PhotoSize big = FileLoader.getClosestPhotoSizeWithSize(sizes, 800);
                        TLRPC.VideoSize videoSize = photos_photo.photo.video_sizes.isEmpty() ? null : photos_photo.photo.video_sizes.get(0);
                        user.photo = new TLRPC.TL_userProfilePhoto();
                        user.photo.photo_id = photos_photo.photo.id;
                        if (small != null) {
                            user.photo.photo_small = small.location;
                        }
                        if (big != null) {
                            user.photo.photo_big = big.location;
                        }

                        if (small != null && avatar != null) {
                            File destFile = FileLoader.getInstance(currentAccount).getPathToAttach(small, true);
                            File src = FileLoader.getInstance(currentAccount).getPathToAttach(avatar, true);
                            src.renameTo(destFile);
                            String oldKey = avatar.volume_id + "_" + avatar.local_id + "@50_50";
                            String newKey = small.location.volume_id + "_" + small.location.local_id + "@50_50";
                            ImageLoader.getInstance().replaceImageInCache(oldKey, newKey, ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL), false);
                        }
                        if (big != null && avatarBig != null) {
                            File destFile = FileLoader.getInstance(currentAccount).getPathToAttach(big, true);
                            File src = FileLoader.getInstance(currentAccount).getPathToAttach(avatarBig, true);
                            src.renameTo(destFile);
                        }
                        if (videoSize != null && videoPath != null) {
                            File destFile = FileLoader.getInstance(currentAccount).getPathToAttach(videoSize, "mp4", true);
                            File src = new File(videoPath);
                            src.renameTo(destFile);
                        }

                        getMessagesStorage().clearUserPhotos(user.id);
                        ArrayList<TLRPC.User> users = new ArrayList<>();
                        users.add(user);
                        getMessagesStorage().putUsersAndChats(users, null, false, true);


                    }

                    avatar = null;
                    avatarBig = null;
                    updateUserData();
                    showAvatarProgress(false, true);
                    getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                    getUserConfig().saveConfig(true);
                }));
            } else {
                avatar = smallSize.location;
                avatarBig = bigSize.location;
                avatarImage.setImage(ImageLocation.getForLocal(avatar), "50_50", avatarDrawable, null);
                showAvatarProgress(true, false);
            }
            actionBar.createMenu().requestLayout();
        });

    }


    @Override
    public void onUploadProgressChanged(float progress) {
        if (avatarProgressView == null) {
            return;
        }
        avatarProgressView.setProgress(progress);
    }

    @Override
    public void didStartUpload(boolean isVideo) {
        if (avatarProgressView == null) {
            return;
        }
        avatarProgressView.setProgress(0.0f);    }



    public static class UserFeed {
        public TLRPC.User user;
        public TableModels.Feed feed;
        public long userId;
        public FeedCell.Data data;
        public TLRPC.UserFull userFull;
    }

    private int reqId;

    private boolean showIntro;

    private boolean profile;

    public boolean isProfile() {
        return profile;
    }
    private boolean visible;

    private final RecyclerView.RecycledViewPool recycledViewPools = new RecyclerView.RecycledViewPool();
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private SwipeRefreshLayout refreshLayout;
    private LinearLayoutManager layoutManager;
    private FeedCell currentTouchedFeedCell;
    private final ArrayList<UserFeed> feedArrayList = new ArrayList<>();

    private boolean runningInstaAnimation;

    private boolean showInvite;
    private int rowCount;
    private int storyIntroRow;
    private int storySecRow;
    private int storyHeaderRow;
    private int storyRow;
    private int inviteUserRow;
    private int inviteSecRow;
    private int feedHeaderRow;
    private int feedSecRow;
    private int feedStartRow;
    private int feedEndRow;
    private int feedEmptyRow;
    private int pullRow;
    private int paddingRow;

    public static float viewOffset = 0.0f;



    private long startArchivePullingTime;
    private boolean canShowHiddenPull;
    private boolean wasPulled;
    private long lastUpdateTime;
    private PullForegroundDrawable pullForegroundDrawable;

    private int count;
//    public class PullRecyclerView extends RecyclerListView {
//
//        private boolean firstLayout = true;
//        private boolean ignoreLayout;
//
//        private int lastListPadding;
//
//
//        public PullRecyclerView(Context context) {
//            super(context);
//
//        }
//
//
//        @Override
//        protected void onLayout(boolean changed, int l, int t, int r, int b) {
//            super.onLayout(changed, l, t, r, b);
//            lastListPadding = getPaddingTop();
//
//        }
//
//        public void setViewsOffset(float offset) {
//            viewOffset = offset;
//            int n = getChildCount();
//            for (int i = 0; i < n; i++) {
//                getChildAt(i).setTranslationY(viewOffset);
//            }
//            invalidate();
//            fragmentView.invalidate();
//        }
//
//        public float getViewOffset() {
//            return viewOffset;
//        }
//
//        @Override
//        protected void onMeasure(int widthSpec, int heightSpec) {
//
//            int t = actionBar.getMeasuredHeight();
//            ignoreLayout = true;
//            setTopGlowOffset(t);
//            setPadding(0, t, 0, 0);
//            ignoreLayout = false;
//
////            int pos = layoutManager.findFirstVisibleItemPosition();
////            if (pos != RecyclerView.NO_POSITION ) {
////                RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(pos);
////                if (holder != null) {
////                    int top = holder.itemView.getTop();
////                    ignoreLayout = true;
////                    layoutManager.scrollToPositionWithOffset(pos,  (top - lastListPadding));
////                    ignoreLayout = false;
////                }
////            }
//            if (firstLayout) {
//                ignoreLayout = true;
//                layoutManager.scrollToPositionWithOffset(1, 0);
//                ignoreLayout = false;
//                firstLayout = false;
//            }
//
//
////            if (firstLayout && !feedArrayList.isEmpty()) {
////                if (true) {
////                    ignoreLayout = true;
////                    LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
////                    layoutManager.scrollToPositionWithOffset(1, (int) actionBar.getTranslationY());
////                    ignoreLayout = false;
////                }
////                firstLayout = false;
////            }
//
//            super.onMeasure(widthSpec, heightSpec);
//        }
//
//
//        @Override
//        public void requestLayout() {
//            if (ignoreLayout) {
//                return;
//            }
//            super.requestLayout();
//        }
//
//        @Override
//        public void setAdapter(RecyclerView.Adapter adapter) {
//            super.setAdapter(adapter);
//            firstLayout = true;
//        }
//
//
//        @Override
//        public void addView(View child, int index, ViewGroup.LayoutParams params) {
//            super.addView(child, index, params);
//            child.setTranslationY(viewOffset);
//        }
//
//        @Override
//        public void removeView(View view) {
//            super.removeView(view);
//            view.setTranslationY(0);
//        }
//
//        @Override
//        public void onDraw(Canvas canvas) {
//            if (pullForegroundDrawable != null && viewOffset != 0) {
//                int pTop = getPaddingTop();
//                if (pTop != 0) {
//                    canvas.save();
//                    canvas.translate(0, pTop);
//                }
//                pullForegroundDrawable.drawOverScroll(canvas);
//                if (pTop != 0) {
//                    canvas.restore();
//                }
//            }
//            super.onDraw(canvas);
//        }
//
//
////        @Override
////        public void onDraw(Canvas canvas) {
////            if (pullForegroundDrawable != null && viewOffset != 0) {
////                pullForegroundDrawable.drawOverScroll(canvas);
////            }
////            super.onDraw(canvas);
////        }
//
//        @Override
//        public boolean onTouchEvent(MotionEvent e) {
//            int action = e.getAction();
//            if (action == MotionEvent.ACTION_DOWN) {
//                listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
//                if (wasPulled) {
//                    wasPulled = false;
//                    if (pullForegroundDrawable != null) {
//                        pullForegroundDrawable.doNotShow();
//                    }
//                    canShowHiddenPull = false;
//                }
//            }
//            boolean result = super.onTouchEvent(e);
//            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
//                int currentPosition = layoutManager.findFirstVisibleItemPosition();
//                if (currentPosition == 0) {
//                    View view = layoutManager.findViewByPosition(currentPosition);
//                    if (view != null) {
//                        int height = (int) (AndroidUtilities.dp(72) * PullForegroundDrawable.SNAP_HEIGHT);
//                        int diff = view.getTop() + view.getMeasuredHeight();
//                        long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
//                        listView.smoothScrollBy(0, diff, CubicBezierInterpolator.EASE_OUT_QUINT);
//                        if (diff >= height && pullingTime >= PullForegroundDrawable.minPullingTime) {
//                            wasPulled = true;
//                            //AndroidUtilities.cancelRunOnUIThread(updateTimeRunnable);
//                            lastUpdateTime = 0;
//                            loadData();
//                            loadStory();
////                             loadAccountState();
//                           // updateTime(false);
//                        }
//
//                        if (viewOffset != 0) {
//                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(viewOffset, 0f);
//                            valueAnimator.addUpdateListener(animation -> listView.setViewsOffset((float) animation.getAnimatedValue()));
//                            valueAnimator.setDuration((long) (350f - 120f * (viewOffset / PullForegroundDrawable.getMaxOverscroll())));
//                            valueAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
//                            listView.setScrollEnabled(false);
//                            valueAnimator.addListener(new AnimatorListenerAdapter() {
//                                @Override
//                                public void onAnimationEnd(Animator animation) {
//                                    super.onAnimationEnd(animation);
//                                    listView.setScrollEnabled(true);
//                                }
//                            });
//                            valueAnimator.start();
//                        }
//                    }
//                }
//            }
//            return result;
//        }
//    }
    private static class ReactionTabHolderView extends FrameLayout {
        private Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path path = new Path();
        private RectF rect = new RectF();
        private float radius = AndroidUtilities.dp(32);

        private BackupImageView reactView;
        private ImageView iconView;
        View overlaySelectorView;
        private float outlineProgress;
        Drawable drawable;
        public ReactionTabHolderView(@NonNull Context context) {
            super(context);

            overlaySelectorView = new View(context);
            addView(overlaySelectorView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            iconView = new ImageView(context);
            drawable = ContextCompat.getDrawable(context, R.drawable.msg_reactions_filled).mutate();
            iconView.setImageDrawable(drawable);
            addView(iconView, LayoutHelper.createFrameRelatively(24, 24, Gravity.START | Gravity.CENTER_VERTICAL, 8, 0, 8, 0));

            reactView = new BackupImageView(context);
            reactView.setSize(AndroidUtilities.dp(18),AndroidUtilities.dp(18));
            addView(reactView, LayoutHelper.createFrameRelatively(24, 24, Gravity.START | Gravity.CENTER_VERTICAL, 8, 0, 8, 0));
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(AndroidUtilities.dp(1));

            setWillNotDraw(false);

            setOutlineProgress(outlineProgress);
        }

        public void setOutlineProgress(float outlineProgress) {
            this.outlineProgress = outlineProgress;
            int backgroundSelectedColor = Theme.getColor(Theme.key_chat_inReactionButtonBackground);
            int backgroundColor = ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_chat_inReactionButtonBackground), 0x10);

            int textSelectedColor = Theme.getColor(Theme.key_chat_inReactionButtonTextSelected);
            int textColor = Theme.getColor(Theme.key_chat_inReactionButtonText);
            int textFinalColor = ColorUtils.blendARGB(textColor, textSelectedColor, outlineProgress);

            bgPaint.setColor(ColorUtils.blendARGB(backgroundColor, backgroundSelectedColor, outlineProgress));
            drawable.setColorFilter(new PorterDuffColorFilter(textFinalColor, PorterDuff.Mode.MULTIPLY));

            if (outlineProgress == 1f) {
                overlaySelectorView.setBackground(Theme.createSimpleSelectorRoundRectDrawable((int) radius, Color.TRANSPARENT, ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_chat_inReactionButtonTextSelected), (int) (0.3f * 255))));
            } else if (outlineProgress == 0) {
                overlaySelectorView.setBackground(Theme.createSimpleSelectorRoundRectDrawable((int) radius, Color.TRANSPARENT, ColorUtils.setAlphaComponent(backgroundSelectedColor, (int) (0.3f * 255))));
            }
            invalidate();
        }

        public void setCounter(int count) {
            iconView.setVisibility(VISIBLE);
            reactView.setVisibility(GONE);
        }

        TLRPC.TL_availableReaction r;

        public TLRPC.TL_availableReaction getR() {
            return r;
        }
        public void setEmoji(TLRPC.TL_availableReaction r) {
            this.r = r;
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(r.static_icon, Theme.key_windowBackgroundGray, 1.0f);
            reactView.setImage(ImageLocation.getForDocument(r.static_icon), "50_50", "webp", svgThumb, r);
            reactView.setVisibility(VISIBLE);
            iconView.setVisibility(GONE);
        }

        public void setCounter(int currentAccount, TLRPC.TL_reactionCount counter) {
            String e = counter.reaction.toString();
            for (TLRPC.TL_availableReaction r : MediaDataController.getInstance(currentAccount).getReactionsList()) {
                if (r.reaction.equals(e)) {
                    SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(r.static_icon, Theme.key_windowBackgroundGray, 1.0f);
                    reactView.setImage(ImageLocation.getForDocument(r.static_icon), "50_50", "webp", svgThumb, r);
                    reactView.setVisibility(VISIBLE);
                    iconView.setVisibility(GONE);
                    break;
                }
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            rect.set(0, 0, getWidth(), getHeight());

            canvas.drawRoundRect(rect, radius, radius, bgPaint);
            super.dispatchDraw(canvas);

        }
    }


    private int extraHeight;
    private class TopView extends View {

        private int currentColor;
        private Paint paint = new Paint();

        public TopView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + AndroidUtilities.dp(91));
        }

        @Override
        public void setBackgroundColor(int color) {
            if (color != currentColor) {
                paint.setColor(color);
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int height = getMeasuredHeight() - AndroidUtilities.dp(91);
            canvas.drawRect(0, 0, getMeasuredWidth(), height + extraHeight , paint);

            if (parentLayout != null) {
                parentLayout.drawHeaderShadow(canvas, height + extraHeight);
            }
        }
    }

    private boolean openSelf;


    private final PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() {


        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview) {

            ImageReceiver imageReceiver;
            View cell;
            long dialogId = 0;
            if (currentTouchedFeedCell != null) {
                FeedCell feedCell = currentTouchedFeedCell;
                imageReceiver = feedCell.getImageReceiver();
                cell = feedCell;
                dialogId = currentTouchedFeedCell.getUser().id;
            }else if(openSelf && avatarImage != null){
                openSelf = false;
                dialogId = getUserConfig().getClientUserId();
                imageReceiver = avatarImage.getImageReceiver();
                cell = avatarImage;
            }else{
                return null;
            }


            if (imageReceiver != null && cell != null) {
                int[] coords = new int[2];
                cell.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                object.parentView = listView;
                object.imageReceiver = imageReceiver;
                object.dialogId = dialogId;
                object.thumb = object.imageReceiver.getBitmapSafe();
                object.size = -1;
                object.radius = imageReceiver.getRoundRadius();
                return object;
            }

            return null;
        }


    };


    private void reset(){
        storyIntroRow= -1;
        storySecRow= -1;
        storyHeaderRow= -1;
        storyRow= -1;
        inviteUserRow= -1;
        inviteSecRow= -1;
        feedHeaderRow= -1;
        feedSecRow= -1;
        feedStartRow= -1;
        feedEndRow= -1;
        feedEmptyRow= -1;
        pullRow = -1;
        paddingRow = -1;

    }

    private void updateRow(boolean notify) {
        reset();
        rowCount = 0;
//        if(feedArrayList.size() > 0){
//            pullRow = rowCount++;
//        }else{
//            pullRow = -1;
//        }
        pullRow = -1;
        pullRow = -1;
        storyHeaderRow = -1;
        storyRow = -1;
        if (showIntro) {
            storyIntroRow = -1;
        }
        int count = feedArrayList.size();
        if(count >0 && showInvite){
            inviteUserRow = -1;
        }
        if (count > 0) {
            feedSecRow = rowCount++;
            feedHeaderRow = rowCount++;
            feedStartRow = rowCount;
            rowCount += count;
            feedEndRow = rowCount;
            paddingRow = rowCount++;
        }

        if(feedStartRow == -1){
            feedEmptyRow  = rowCount++;
        }
        if (listAdapter != null && notify) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private ImageUpdater imageUpdater;

    @Override
    public boolean onFragmentCreate() {

        getNotificationCenter().addObserver(this, NotificationCenter.didFeedLoaded);
        getNotificationCenter().addObserver(this, NotificationCenter.updateData);

        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        showIntro = preferences.getBoolean("showStoryIntro", true);
        showIntro = false;
        loadData();


        imageUpdater = new ImageUpdater(true);
        imageUpdater.setOpenWithFrontfaceCamera(true);
        imageUpdater.parentFragment = this;
        imageUpdater.setDelegate(this);

        getMediaDataController().checkFeaturedStickers();
        getMessagesController().loadSuggestedFilters();

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        visible = false;

        getNotificationCenter().removeObserver(this, NotificationCenter.didFeedLoaded);
        getNotificationCenter().removeObserver(this,NotificationCenter.updateData);

        if (imageUpdater != null) {
            imageUpdater.clear();
        }

        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }

        super.onFragmentDestroy();
    }

    private boolean loading;

    private void loadData(){
        if(loading){
            return;
        }
        loading = true;
        
        DataStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            List<TableModels.Feed> feedList  = DataStorage.getInstance(currentAccount).getDatabase().feedDao().getFeeds();
            ArrayList<TableModels.Feed> feeds = new ArrayList<>(feedList);
            Collections.sort(feeds, (o2, o1) -> Integer.compare(o1.date, o2.date));
            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.didFeedLoaded, feeds);
            });
        });

        if (paddingView != null) {
            paddingView.requestLayout();
        }

    }

    private View paddingView;
    private TopView topView;
    private FrameLayout avatarContainer;
    private BackupImageView avatarImage;
    private TextView nameTextView;
    private TextView onlineTextView;
    private AvatarDrawable avatarDrawable;
    private RadialProgressView avatarProgressView;
    private AnimatorSet avatarAnimation;


    public void didSelectSearchPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos) {
        if (photos.isEmpty()) {
            return;
        }

    }

    private FrameLayout frameLayout;

    public FrameLayout getFrameLayout() {
        return frameLayout;
    }

    @Override
    protected ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_avatar_actionBarSelectorBlue), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_avatar_actionBarIconBlue), false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setOccupyStatusBar(Build.VERSION.SDK_INT >= 21 && !AndroidUtilities.isTablet());
        return actionBar;
    }



    @Override
    public View createView(Context context) {
        extraHeight = AndroidUtilities.dp(88);
        Theme.createProfileResources(context);

        pullForegroundDrawable = new PullForegroundDrawable("Swip To refresh", "Realse to refresh") {
            @Override
            protected float getViewOffset() {
                return 0;
               // return listView.getViewOffset();
            }
        };
        pullForegroundDrawable.setColors(Theme.key_chats_archivePullDownBackground, Theme.key_chats_archivePullDownBackgroundActive);
        pullForegroundDrawable.showHidden();
        pullForegroundDrawable.setWillDraw(true);


        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 4) {
                    clearFeeds();
                }
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(4, R.drawable.msg_clear);

        actionBar.setOnClickListener(v -> {
            if (layoutManager != null) {
                layoutManager.scrollToPosition(0);
            }
        });

        fragmentView =  refreshLayout = new SwipeRefreshLayout(context);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData();
            }
        });



        frameLayout = new FrameLayout(context){

            private Paint paint = new Paint();
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                checkListViewScroll();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                View contentView = listView;
                canvas.drawRect(contentView.getLeft(), contentView.getTop() + extraHeight, contentView.getRight(), contentView.getBottom(), paint);


            }
        };
        frameLayout.setWillNotDraw(false);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        refreshLayout.addView(frameLayout,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setItemAnimator(null);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        listView.setPadding(0, AndroidUtilities.dp(88), 0, 0);
        listView.setLayoutAnimation(null);
        listView.setGlowColor(Theme.getColor(Theme.key_dialogScrollGlow));
        listView.setClipToPadding(false);
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkListViewScroll();

            }
        });

        frameLayout.addView(listView,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));


        topView = new TopView(context);
        topView.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(topView);

        frameLayout.addView(actionBar);

        avatarContainer = new FrameLayout(context);
        avatarContainer.setPivotX(0);
        avatarContainer.setPivotY(0);
        frameLayout.addView(avatarContainer, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarContainer.setOnClickListener(v -> {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(UserConfig.getInstance(currentAccount).getClientUserId());
            if (user != null && user.photo != null && user.photo.photo_big != null) {
                openSelf = true;
                PhotoViewer.getInstance().setParentActivity(getParentActivity());
                if (user.photo.dc_id != 0) {
                    user.photo.photo_big.dc_id = user.photo.dc_id;
                }
                PhotoViewer.getInstance().openPhoto(user.photo.photo_big, provider);
            }
        });

        avatarImage = new BackupImageView(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(21));
        avatarImage.setContentDescription(LocaleController.getString("AccDescrProfilePicture", R.string.AccDescrProfilePicture));
        avatarContainer.addView(avatarImage, LayoutHelper.createFrame(42, 42));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x55000000);

         avatarProgressView = new RadialProgressView(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                if (avatarImage != null && avatarImage.getImageReceiver().hasNotThumb()) {
                    paint.setAlpha((int) (0x55 * avatarImage.getImageReceiver().getCurrentAlpha()));
                    canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2, AndroidUtilities.dp(21), paint);
                }
                super.onDraw(canvas);
            }
        };
        avatarProgressView.setSize(AndroidUtilities.dp(26));
        avatarProgressView.setProgressColor(0xffffffff);
        avatarContainer.addView(avatarProgressView, LayoutHelper.createFrame(42, 42));
        showAvatarProgress(false, false);


        nameTextView = new TextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_profile_title));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setPivotX(0);
        nameTextView.setPivotY(0);
        frameLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 96, 0));


        onlineTextView = new TextView(context);
        onlineTextView.setTextColor(Theme.getColor(Theme.key_profile_status));
        onlineTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        onlineTextView.setLines(1);
        onlineTextView.setMaxLines(1);
        onlineTextView.setSingleLine(true);
        onlineTextView.setEllipsize(TextUtils.TruncateAt.END);
        onlineTextView.setGravity(Gravity.LEFT);
        frameLayout.addView(onlineTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 96, 0));

        needLayout();

        return fragmentView;
    }

      private void showAvatarProgress(boolean show, boolean animated) {
        if (avatarProgressView == null) {
            return;
        }
        if (avatarAnimation != null) {
            avatarAnimation.cancel();
            avatarAnimation = null;
        }
        if (animated) {
            avatarAnimation = new AnimatorSet();
            if (show) {
                avatarProgressView.setVisibility(View.VISIBLE);
                avatarAnimation.playTogether(ObjectAnimator.ofFloat(avatarProgressView, View.ALPHA, 1.0f));
            } else {
                avatarAnimation.playTogether(ObjectAnimator.ofFloat(avatarProgressView, View.ALPHA, 0.0f));
            }
            avatarAnimation.setDuration(180);
            avatarAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (avatarAnimation == null || avatarProgressView == null) {
                        return;
                    }
                    if (!show) {
                        avatarProgressView.setVisibility(View.INVISIBLE);
                    }
                    avatarAnimation = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    avatarAnimation = null;
                }
            });
            avatarAnimation.start();
        } else {
            if (show) {
                avatarProgressView.setAlpha(1.0f);
                avatarProgressView.setVisibility(View.VISIBLE);
            } else {
                avatarProgressView.setAlpha(0.0f);
                avatarProgressView.setVisibility(View.INVISIBLE);
            }
        }
    }
      private void needLayout() {
        FrameLayout.LayoutParams layoutParams;
        int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        if (listView != null) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = newTop;
                listView.setLayoutParams(layoutParams);
            }
        }

        if (avatarContainer != null) {
            float diff = extraHeight / (float) AndroidUtilities.dp(88);
            listView.setTopGlowOffset(extraHeight);


            float avatarY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f * (1.0f + diff) - 21 * AndroidUtilities.density + 27 * AndroidUtilities.density * diff;
            avatarContainer.setScaleX((42 + 18 * diff) / 42.0f);
            avatarContainer.setScaleY((42 + 18 * diff) / 42.0f);
            avatarContainer.setTranslationX(-AndroidUtilities.dp(47) * diff);
            avatarContainer.setTranslationY((float) Math.ceil(avatarY));

            if (nameTextView != null) {
                nameTextView.setTranslationX(-21 * AndroidUtilities.density * diff);
                onlineTextView.setTranslationX(-21 * AndroidUtilities.density * diff);

                nameTextView.setTranslationY((float) Math.floor(avatarY) - (float) Math.ceil(AndroidUtilities.density) + (float) Math.floor(7 * AndroidUtilities.density * diff));
                onlineTextView.setTranslationY((float) Math.floor(avatarY) + AndroidUtilities.dp(22) + (float) Math.floor(11 * AndroidUtilities.density) * diff);

                float scale = 1.0f + 0.12f * diff;
                nameTextView.setScaleX(scale);
                nameTextView.setScaleY(scale);
                int viewWidth;
                if (AndroidUtilities.isTablet()) {
                    viewWidth = AndroidUtilities.dp(490);
                } else {
                    viewWidth = AndroidUtilities.displaySize.x;
                }
                int buttonsWidth = AndroidUtilities.dp(118 + 8 + 40 + 48);
                int minWidth = viewWidth - buttonsWidth;

                int width = (int) (viewWidth - buttonsWidth * Math.max(0.0f, 1.0f - (diff != 1.0f ? diff * 0.15f / (1.0f - diff) : 1.0f)) - nameTextView.getTranslationX());
                float width2 = nameTextView.getPaint().measureText(nameTextView.getText().toString()) * scale;
                layoutParams = (FrameLayout.LayoutParams) nameTextView.getLayoutParams();
                if (width < width2) {
                    layoutParams.width = Math.max(minWidth, (int) Math.ceil((width - AndroidUtilities.dp(24)) / (scale + (1.12f - scale) * 7.0f)));
                } else {
                    layoutParams.width = (int) Math.ceil(width2);
                }
                layoutParams.width = (int) Math.min((viewWidth - nameTextView.getX()) / scale - AndroidUtilities.dp(8), layoutParams.width);
                nameTextView.setLayoutParams(layoutParams);

                width2 = onlineTextView.getPaint().measureText(onlineTextView.getText().toString());
                layoutParams = (FrameLayout.LayoutParams) onlineTextView.getLayoutParams();
                layoutParams.rightMargin = (int) Math.ceil(onlineTextView.getTranslationX() + AndroidUtilities.dp(8) + AndroidUtilities.dp(40) * (1.0f - diff));
                if (width < width2) {
                    layoutParams.width = (int) Math.ceil(width);
                } else {
                    layoutParams.width = LayoutHelper.WRAP_CONTENT;
                }
                onlineTextView.setLayoutParams(layoutParams);
            }
        }
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    checkListViewScroll();
                    needLayout();
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }


    private void checkListViewScroll() {
        if (listView.getVisibility() != View.VISIBLE || listView.getChildCount() <= 0) {
            return;
        }

        View child = listView.getChildAt(0);
        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findContainingViewHolder(child);
        int top = child.getTop();
        int newOffset = 0;
        if (top >= 0 && holder != null && holder.getAdapterPosition() == 0) {
            newOffset = top;
        }
        if (extraHeight != newOffset) {
            extraHeight = newOffset;
            topView.invalidate();
            needLayout();
        }
    }


    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 11;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 2:
                    StickerEmptyView stickerEmptyView = new StickerEmptyView(mContext, null, StickerEmptyView.STICKER_TYPE_NO_CONTACTS){
                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            int height = Math.max(AndroidUtilities.dp(280), fragmentView.getMeasuredHeight() - AndroidUtilities.dp(236 + 6));
                            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                        }
                    };
                    stickerEmptyView.getTitle().setText("No contact change yet!");
                    stickerEmptyView.stickerView.getImageReceiver().setAutoRepeat(3);
                    stickerEmptyView.getSubtitle().setText("come back later!");
                    stickerEmptyView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
                    view = stickerEmptyView;
                    break;
                case 7:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case 0:
                    view = new FeedLayout(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case 1:
                    view = new ShadowSectionCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
                    view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case 12:
                    view = new View(mContext) {
                        @Override
                        protected void onDraw(Canvas canvas) {
                            pullForegroundDrawable.draw(canvas);
                        }

                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(AndroidUtilities.dp(72)), MeasureSpec.EXACTLY));
                        }
                    };
                    pullForegroundDrawable.setCell(view);
                    break;
                case 13:
                    view = paddingView = new View(mContext) {
                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            int n = listView.getChildCount();
                            int itemsCount = listAdapter.getItemCount();
                            int totalHeight = 0;
                            for (int i = 0; i < n; i++) {
                                View view = listView.getChildAt(i);
                                int pos = listView.getChildAdapterPosition(view);
                                if (pos != 0 && pos != itemsCount - 1) {
                                    totalHeight += listView.getChildAt(i).getMeasuredHeight();
                                }
                            }
                            int paddingHeight = fragmentView.getMeasuredHeight() - totalHeight;
                            if (paddingHeight <= 0) {
                                paddingHeight = 0;
                            }
                            setMeasuredDimension(listView.getMeasuredWidth(), paddingHeight);
                        }
                    };
                    break;
                case 10:
                default:
                    view = new View(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    FeedLayout feedCell = (FeedLayout) holder.itemView;
                    if (position >= feedStartRow && position < feedEndRow) {
                        UserFeed userFeed = feedArrayList.get(position - feedStartRow);
                        feedCell.setUserFeed(userFeed);
                        feedCell.feedCell.setTag(userFeed.user.id);
                    }
                    break;
                case 7:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == feedHeaderRow) {
                        headerCell.setText("Your Feeds");
                    } else if (position == storyHeaderRow) {
                        headerCell.setText("Your Stories");
                    }
                    break;
            }
        }


        @Override
        public int getItemViewType(int position) {
            if(position == pullRow){
                return 12;
            }else if (position >= feedStartRow && position < feedEndRow) {
                return 0;
            } else if (position == feedEmptyRow) {
                return 2;
            } else if (position == feedSecRow || position == storySecRow || position == inviteSecRow) {
                return 1;
            } else if (position == feedHeaderRow || position == storyHeaderRow) {
                return 7;
            } else if (position == inviteUserRow) {
                return 8;
            } else if (position == storyIntroRow) {
                return 9;
            }else if(position == storyRow){
                return 11;
            }else if(position == paddingRow){
                return 13;
            }
            return 1;
        }
    }



    @Override
    public void onPause() {
        super.onPause();
        if (imageUpdater != null) {
            imageUpdater.onPause();
        }
    }

    private void updateUserData() {
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(UserConfig.getInstance(currentAccount).getClientUserId());
        if (user == null) {
            return;
        }
        TLRPC.FileLocation photoBig = null;
        if (user.photo != null) {
            photoBig = user.photo.photo_big;
        }
        avatarDrawable = new AvatarDrawable(user, true);

        avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
        if (avatarImage != null) {
            avatarImage.setImage(ImageLocation.getForUser(user,ImageLocation.TYPE_BIG), "50_50", avatarDrawable, user);
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig), false);

            nameTextView.setText(UserObject.getUserName(user));
            onlineTextView.setText(LocaleController.getString("Online", R.string.Online));

            avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig), false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        visible = true;
        if(listAdapter != null){
            listAdapter.notifyDataSetChanged();
        }

        if (imageUpdater != null) {
            imageUpdater.onResume();
        }

        updateUserData();
        fixLayout();


    }




    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        imageUpdater.onActivityResult(requestCode, resultCode, data);

    }




    public static boolean canPreview(FeedCell.Data data) {
        return data != null && (data.imageLocation != null || data.thumbImageLocation != null);
    }

    private void updateVisibleRows(int mask) {
        if (listView == null) {
            return;
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof FeedLayout) {
                ((FeedLayout) child).update(mask);
            }
        }
    }
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

        if(id == NotificationCenter.updateInterfaces){
            int mask = (Integer) args[0];
            boolean infoChanged = (mask & MessagesController.UPDATE_MASK_AVATAR) != 0;
            if (infoChanged) {
               updateUserData();
               updateVisibleRows(MessagesController.UPDATE_MASK_AVATAR);
               loadData();

            }
        }else if (id == NotificationCenter.reactionsDidLoad) {
            if (account != currentAccount) return;
            if(listAdapter != null){
                listAdapter.notifyDataSetChanged();
            }
        }else if (id == NotificationCenter.didFeedLoaded) {
            if(refreshLayout != null){
                refreshLayout.setRefreshing(false);
            }
            loading = false;
            feedArrayList.clear();
            ArrayList<TableModels.Feed> feeds = (ArrayList<TableModels.Feed>) args[0];
            if (feeds != null) {
                for (int a = 0; a < feeds.size(); a++) {
                    TableModels.Feed feed = feeds.get(a);
                    if (feed == null) {
                        continue;
                    }
                    TLRPC.User user = getMessagesController().getUser(feed.user_id);
                    if (!UserObject.hasPhoto(user)) {
                        continue;
                    }
                    final TLRPC.UserFull userFull = getMessagesController().getUserFull(user.id);
                    final FeedCell.Data data;
                    if (userFull != null) {
                        data = FeedCell.Data.of(userFull);
                    } else {
                        data = FeedCell.Data.of(user, classGuid);
                    }
                    if (!canPreview(data)) {
                        continue;
                    }
                    UserFeed userFeed = new UserFeed();
                    userFeed.feed = feed;
                    userFeed.user = user;
                    userFeed.userId = user.id;
                    userFeed.data = data;
                    userFeed.userFull = userFull;
                    feedArrayList.add(userFeed);
                }
            }
            updateRow(true);
        } else if (id == NotificationCenter.updateData) {
            if(listAdapter != null){
                listAdapter.notifyDataSetChanged();
            }
        }
    }



    public void createClearFeedAlert(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        int account = fragment.getCurrentAccount();

        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        long selfUserId = UserConfig.getInstance(account).getClientUserId();

        TLRPC.User user = getUserConfig().getCurrentUser();

        TextView messageTextView = new TextView(context);
        messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);


        FrameLayout frameLayout = new FrameLayout(context);
        builder.setView(frameLayout);

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setTextSize(AndroidUtilities.dp(12));
        avatarDrawable.setInfo(user);

        BackupImageView imageView = new BackupImageView(context);
        imageView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, user);
        imageView.setRoundRadius(AndroidUtilities.dp(20));
        frameLayout.addView(imageView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 22, 5, 22, 0));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText("Delete All Feeds!");

        frameLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, (LocaleController.isRTL ? 21 : 76), 11, (LocaleController.isRTL ? 76 : 21), 0));
        frameLayout.addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 24, 57, 24, 9));

        messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.formatString("AreYouSureDeleteAndExitName", R.string.AreYouSureDeleteAndExitName, "Your")));
        messageTextView.setText("Are you sure you want to delete all your feeds!");

        String actionText;
        actionText = LocaleController.getString("DeleteAll", R.string.DeleteAll);

        builder.setPositiveButton(actionText, (dialogInterface, i) -> {
            clearFeeds();

        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        fragment.showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
        }
    }

    @Override
    public boolean onBackPressed() {
        return super.onBackPressed();
    }



    private void clearFeeds() {
        DataStorage.getInstance(currentAccount).getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                DataStorage.getInstance(currentAccount).getDatabase().feedDao().clearFeeds();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        loadData();
                    }
                });
            }
        });
    }

    private void showUserPreview(UserFeed userFeed) {
        TLRPC.User chat = userFeed.user;
        TLRPC.UserFull info = userFeed.userFull;

        if (chat.photo != null && chat.photo.photo_big != null) {
            PhotoViewer.getInstance().setParentActivity(getParentActivity());
            if (chat.photo.dc_id != 0) {
                chat.photo.photo_big.dc_id = chat.photo.dc_id;
            }
            ImageLocation videoLocation;
            if (info != null && (info.profile_photo instanceof TLRPC.TL_photo) && !info.profile_photo.video_sizes.isEmpty()) {
                videoLocation = ImageLocation.getForPhoto(info.profile_photo.video_sizes.get(0), info.profile_photo);
            } else {
                videoLocation = null;
            }
            PhotoViewer.getInstance().openPhotoWithVideo(userFeed.user.photo.photo_big, videoLocation, provider);
        }
    }





    private class FeedLayout extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {

        public FeedCell feedCell;
        private TextView bioTextView;
        private ProfileUserCell profileUserCell;

        private UserFeed userFeed;
        private TLRPC.UserFull userFull;
        private long user_id;

        public FeedLayout(Context context) {
            super(context);
            setOrientation(VERTICAL);
            profileUserCell = new ProfileUserCell(context, 6, 2, false);
            profileUserCell.setOnClickListener(v -> {
                if (userFeed == null) {
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putLong("user_id", userFeed.userId);
                ProfileActivity profileActivity = new ProfileActivity(bundle);
                presentFragment(profileActivity);
            });
            addView(profileUserCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 6, 0, 6, 0));

            feedCell = new FeedCell(context);
            feedCell.setOnClickListener(v -> {
                if (userFeed == null) {
                    return;
                }
                currentTouchedFeedCell = feedCell;
                showUserPreview(userFeed);
            });
            addView(feedCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 6, 4, 6, 0));


            bioTextView = new TextView(context);
            bioTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            bioTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            bioTextView.setMaxLines(2);
            bioTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            bioTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            bioTextView.setEllipsize(TextUtils.TruncateAt.END);
            addView(bioTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 12 + 6, 0, 12 + 6, 4));

        }

        private void update(int mask){
            feedCell.update(mask);
        }


        public void setUserFeed(UserFeed userFeed) {
            this.userFeed = userFeed;
            user_id = userFeed.userId;
            feedCell.setData(userFeed.data);
            if (userFeed.userFull == null) {
                userFull = getMessagesController().getUserFull(userFeed.userId);
            } else {
                userFull = userFeed.userFull;
            }
            profileUserCell.setData(userFeed.user, ContactsController.formatName(userFeed.user.first_name, userFeed.user.last_name), LocaleController.formatDateTime(userFeed.feed.date), false);
            updateBioView();
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.userInfoDidLoad);
            if (userFull == null) {
                getMessagesController().loadUserInfo(userFeed.user, true, classGuid, classGuid);
            }

            feedCell.update(0);

        }


        private void setlectRxnstring(){

        }

        private void updateBioView() {
            if (userFull == null) {
                return;
            }
            if (userFeed.user != null && userFeed.user.photo != null) {
//                isVideo = userFeed.user.photo.has_video;
            }
            if (!TextUtils.isEmpty(userFull.about)) {
                bioTextView.setText(userFull.about);
            } else {
                bioTextView.setVisibility(GONE);
            }
//            if(isVideo){
//                Drawable back = Theme.createCircleDrawable(AndroidUtilities.dp(30),0x70000000);
//                Drawable icon = getContext().getResources().getDrawable(R.drawable.video_play1);
//                CombinedDrawable combinedDrawable = new CombinedDrawable(back,icon);
//                combinedDrawable.setIconSize(AndroidUtilities.dp(20),AndroidUtilities.dp(20));
//                videoPlayView.setImageDrawable(combinedDrawable);
//                videoPlayView.setVisibility(VISIBLE);
//            }else{
//                videoPlayView.setVisibility(GONE);
//
//            }
        }




        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.userInfoDidLoad);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.userInfoDidLoad) {
                Long uid = (Long) args[0];
                if (uid == user_id) {
                    userFull = (TLRPC.UserFull) args[1];
                    updateBioView();
                }

            }
        }
    }



//    public class ProfileHintInnerCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {
//
//        private BackupImageView backupImageView;
//        private TextView titleTextView;
//        private TextView messageTextView;
//        private TLRPC.UserFull userFull;
//
//        public ProfileHintInnerCell(Context context) {
//            super(context);
//            int top = (int) ((ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0)) / AndroidUtilities.density) - 44;
//
//            BackupImageView imageView = new BackupImageView(context);
//            AvatarDrawable avatarDrawable = new AvatarDrawable(getUserConfig().getCurrentUser());
//            avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
//            imageView.setForUserOrChat(getUserConfig().getCurrentUser(), avatarDrawable);
//          //  addView(imageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//
//
//               int colorBackground = 0x80000000;
//
//            FrameLayout backlayout = new FrameLayout(context);
//            GradientDrawable gd = new GradientDrawable(
//                    GradientDrawable.Orientation.BOTTOM_TOP,
//                    new int[] {colorBackground, HulugramUtils.getTransparentColor(colorBackground, 0)});
//            gd.setCornerRadius(0f);
//            backlayout.setBackground(gd);
//           // addView(backlayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//
//
//            backupImageView = new BackupImageView(context);
//            backupImageView.setRoundRadius(AndroidUtilities.dp(37));
//            TLRPC.User user = getUserConfig().getCurrentUser();
//             avatarDrawable = new AvatarDrawable(user);
//            avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
//            backupImageView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, user);
//            addView(backupImageView, LayoutHelper.createFrame(74, 74, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, top + 27, 0, 0));
//
//            titleTextView = new TextView(context);
//            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
//            titleTextView.setGravity(Gravity.CENTER);
//            titleTextView.setText(ContactsController.formatName(user.first_name,user.last_name));
//            addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 17, top + 120, 17, 27));
//
//            messageTextView = new TextView(context);
//            messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
//            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
//            messageTextView.setGravity(Gravity.CENTER);
//            addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP|Gravity.CENTER_HORIZONTAL, 40, top + 161, 40, 27));
//
//            userFull = getMessagesController().getUserFull(getUserConfig().getClientUserId());
//            updateUserAbout();
//
//        }
//
//        private void updateUserAbout(){
//            if(userFull == null){
//                loadUserFull();
//                return;
//            }
//            if(!TextUtils.isEmpty(userFull.about)){
//                messageTextView.setText(userFull.about);
//              //  messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
////                messageTextView.setBackgroundColor(0);
////                messageTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
////                Drawable drawable = getContext().getResources().getDrawable(R.drawable.msg_bio);
////                if(drawable!= null){
////                    drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.MULTIPLY));
////                }
////                messageTextView.setCompoundDrawables(drawable,null,null,null);
//            }else{
//               // messageTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
//                messageTextView.setText("Add Bio!");
//               messageTextView.setOnClickListener(new OnClickListener() {
//                   @Override
//                   public void onClick(View v) {
//                       presentFragment(new ChangeBioActivity());
//                   }
//               });
////                messageTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
////                Drawable drawable = getContext().getResources().getDrawable(R.drawable.msg_addbio);
////                if(drawable!= null){
////                    drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_buttonText), PorterDuff.Mode.MULTIPLY));
////                }
//               // messageTextView.setCompoundDrawables(drawable,null,null,null);
//                //messageTextView.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
//
//            }
//
//        }
//
//        public void loadUserFull(){
//            if(userFull == null){
//                NotificationCenter.getInstance(currentAccount).addObserver(this,NotificationCenter.userInfoDidLoad);
//                getMessagesController().loadUserInfo(getUserConfig().getCurrentUser(),true,classGuid,classGuid);
//            }
//            updateUserAbout();
//
//        }
//        @Override
//        public void didReceivedNotification(int id, int account, Object... args) {
//            if(id == NotificationCenter.userInfoDidLoad){
//                Long uid = (Long) args[0];
//                if(uid == getUserConfig().getClientUserId()){
//                    userFull = (TLRPC.UserFull) args[1];
//                    updateUserAbout();
//
//                }
//
//            }
//        }
//
//        @Override
//        protected void onDetachedFromWindow() {
//            super.onDetachedFromWindow();
//            NotificationCenter.getInstance(currentAccount).removeObserver(this,NotificationCenter.userInfoDidLoad);
//
//        }
//    }

}
