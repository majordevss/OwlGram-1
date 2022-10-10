package org.master.video;

import static org.telegram.messenger.MediaDataController.MEDIA_PHOTOVIDEO;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.master.feature.AppUtils;
import org.master.video.ui.SharedPhotoVideoCell3;
import org.master.video.ui.SharedVideoItemCell;
import org.master.video.ui.SharedVideoItemCell;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.ArticleViewer;
import org.telegram.ui.CalendarActivity;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ContextLinkCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedLinkCell;
import org.telegram.ui.Cells.SharedMediaSectionCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BlurredLinearLayout;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.ClippingImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.ExtendedGridLayoutManager;
import org.telegram.ui.Components.FiltersListBottomSheet;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.HintView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
import org.telegram.ui.Components.SearchViewPager;
import org.telegram.ui.Components.SharedMediaFastScrollTooltip;
import org.telegram.ui.Components.Size;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FilterCreateActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.ProxyListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

import it.owlgram.android.OwlConfig;
import it.owlgram.android.helpers.ForwardContext;

public class VideoFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.mediaDidLoad) {
            long uid = (Long) args[0];
            int guid = (Integer) args[3];
            int requestIndex = (Integer) args[7];
            int type = (Integer) args[4];
            boolean fromStart = (boolean) args[6];

            if (type == 6 || type == 7) {
                type = 0;
            }

            if (guid == getClassGuid() && requestIndex == sharedMediaData.requestIndex) {
                if (type != 0 && type != 1 && type != 2 && type != 4) {
                    sharedMediaData.totalCount = (Integer) args[1];
                }
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[2];

                boolean enc = DialogObject.isEncryptedDialog(dialog_id);
                int loadIndex = uid == dialog_id ? 0 : 1;

                RecyclerListView.Adapter adapter = null;
                if (type == 0) {
                    adapter = photoVideoAdapter;
                }
                int oldItemCount;
                int oldMessagesCount = sharedMediaData.messages.size();
                if (adapter != null) {
                    oldItemCount = adapter.getItemCount();
                } else {
                    oldItemCount = 0;
                }
                sharedMediaData.loading = false;
                SparseBooleanArray addedMesages = new SparseBooleanArray();

                if (fromStart) {
                    for (int a = arr.size() - 1; a >= 0; a--) {
                        MessageObject message = arr.get(a);
                        boolean added = sharedMediaData.addMessage(message, loadIndex, true, enc);
                        if (added) {
                            addedMesages.put(message.getId(), true);
                            sharedMediaData.startOffset--;
                            if (sharedMediaData.startOffset < 0) {
                                sharedMediaData.startOffset = 0;
                            }
                        }
                    }
                    sharedMediaData.startReached = (Boolean) args[5];
                    if (sharedMediaData.startReached) {
                        sharedMediaData.startOffset = 0;
                    }
                } else {
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject message = arr.get(a);
                        if (sharedMediaData.addMessage(message, loadIndex, false, enc)) {
                            addedMesages.put(message.getId(), true);
                            sharedMediaData.endLoadingStubs--;
                            if (sharedMediaData.endLoadingStubs < 0) {
                                sharedMediaData.endLoadingStubs = 0;
                            }
                        }
                    }
                    if (sharedMediaData.loadingAfterFastScroll && sharedMediaData.messages.size() > 0) {
                        sharedMediaData.min_id = sharedMediaData.messages.get(0).getId();
                    }
                    sharedMediaData.endReached[loadIndex] = (Boolean) args[5];
                    if (sharedMediaData.endReached[loadIndex]) {
                        sharedMediaData.totalCount = sharedMediaData.messages.size() + sharedMediaData.startOffset;
                    }
                }
                if (!fromStart && loadIndex == 0 && sharedMediaData.endReached[loadIndex] && mergeDialogId != 0) {
                    sharedMediaData.loading = true;
                    getMediaDataController().loadMedia(mergeDialogId, 50, sharedMediaData.max_id[1], 0, type, 1, getClassGuid(), sharedMediaData.requestIndex);
                }
                if (adapter != null) {
                    RecyclerListView listView = null;
                    if (mediaPage.listView.getAdapter() == adapter) {
                        listView = mediaPage.listView;
                        mediaPage.listView.stopScroll();
                    }
                    int newItemCount = adapter.getItemCount();
                    if (adapter == photoVideoAdapter) {
                        if (photoVideoAdapter.getItemCount() == oldItemCount) {
                            AndroidUtilities.updateVisibleRows(listView);
                        } else {
                            photoVideoAdapter.notifyDataSetChanged();
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                    if (sharedMediaData.messages.isEmpty() && !sharedMediaData.loading) {
                        if (listView != null) {
                            animateItemsEnter(listView, oldItemCount, addedMesages);
                        }
                    } else {
                        if (listView != null && (adapter == photoVideoAdapter || newItemCount >= oldItemCount)) {
                            animateItemsEnter(listView, oldItemCount, addedMesages);
                        }
                    }
                    if (listView != null && !sharedMediaData.loadingAfterFastScroll) {
                        if (oldMessagesCount == 0) {
                            for (int k = 0; k < 2; k++) {
                                int position = photoVideoAdapter.getPositionForIndex(0);
                                ((LinearLayoutManager) listView.getLayoutManager()).scrollToPositionWithOffset(position, 0);

                            }
                        } else {
                            saveScrollPosition();
                        }
                    }
                }
                if (sharedMediaData.loadingAfterFastScroll) {
                    if (sharedMediaData.messages.size() == 0) {
                        loadFromStart();
                    } else {
                        sharedMediaData.loadingAfterFastScroll = false;
                    }
                }
                scrolling = true;
            } else if (sharedMediaPreloader != null && sharedMediaData.messages.isEmpty() && !sharedMediaData.loadingAfterFastScroll) {
                if (fillMediaData()) {
                    RecyclerListView.Adapter adapter = null;
                    if (type == 0) {
                        adapter = photoVideoAdapter;
                    }
                    if (adapter != null) {
                        if (mediaPage.listView.getAdapter() == adapter) {
                            mediaPage.listView.stopScroll();
                        }
                        adapter.notifyDataSetChanged();
                    }
                    scrolling = true;
                }
            }
        } else if (id == NotificationCenter.messagesDeleted) {
            boolean scheduled = (Boolean) args[2];
            if (scheduled) {
                return;
            }
            TLRPC.Chat currentChat = null;
            if (DialogObject.isChatDialog(dialog_id)) {
                currentChat = getMessagesController().getChat(-dialog_id);
            }
            long channelId = (Long) args[1];
            int loadIndex = 0;
            if (ChatObject.isChannel(currentChat)) {
                if (channelId == 0 && mergeDialogId != 0) {
                    loadIndex = 1;
                } else if (channelId == currentChat.id) {
                    loadIndex = 0;
                } else {
                    return;
                }
            } else if (channelId != 0) {
                return;
            }
            ArrayList<Integer> markAsDeletedMessages = (ArrayList<Integer>) args[0];
            boolean updated = false;
            int type = -1;
            for (int a = 0, N = markAsDeletedMessages.size(); a < N; a++) {
                if (sharedMediaData.deleteMessage(markAsDeletedMessages.get(a), loadIndex) != null) {
                    updated = true;
                }
            }
            if (updated) {
                scrolling = true;
                if (photoVideoAdapter != null) {
                    photoVideoAdapter.notifyDataSetChanged();
                }
                if (type == 0) {
                    loadFastScrollData(true);
                }
            }
        } else if (id == NotificationCenter.didReceiveNewMessages) {
            boolean scheduled = (Boolean) args[2];
            if (scheduled) {
                return;
            }
            long uid = (Long) args[0];
            if (uid == dialog_id) {
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
                boolean enc = DialogObject.isEncryptedDialog(dialog_id);
                boolean updated = false;
                for (int a = 0; a < arr.size(); a++) {
                    MessageObject obj = arr.get(a);
                    if (obj.messageOwner.media == null || obj.needDrawBluredPreview()) {
                        continue;
                    }
                    int type = MediaDataController.getMediaType(obj.messageOwner);
                    if (type == -1) {
                        return;
                    }
                    if(!obj.isVideo()){
                        return;
                    }
                    if (sharedMediaData.startReached && sharedMediaData.addMessage(obj, obj.getDialogId() == dialog_id ? 0 : 1, true, enc)) {
                        updated = true;
                        hasMedia = 1;
                    }
                }
                if (updated) {
                    scrolling = true;
                    RecyclerListView.Adapter adapter = null;
                    if (mediaPage.selectedType == 0) {
                        adapter = photoVideoAdapter;
                    }
                    if (adapter != null) {
                        photoVideoAdapter.notifyDataSetChanged();
                    }
                }
            }
        } else if (id == NotificationCenter.messageReceivedByServer) {
            Boolean scheduled = (Boolean) args[6];
            if (scheduled) {
                return;
            }
            Integer msgId = (Integer) args[0];
            Integer newMsgId = (Integer) args[1];
            sharedMediaData.replaceMid(msgId, newMsgId);
        }
    }


    private void loadFastScrollData(boolean force) {
        for (int k = 0; k < supportedFastScrollTypes.length; k++) {
            int type = supportedFastScrollTypes[k];
            if ((sharedMediaData.fastScrollDataLoaded && !force) || DialogObject.isEncryptedDialog(dialog_id)) {
                return;
            }
            sharedMediaData.fastScrollDataLoaded = false;
            TLRPC.TL_messages_getSearchResultsPositions req = new TLRPC.TL_messages_getSearchResultsPositions();
            req.filter = new TLRPC.TL_inputMessagesFilterVideo();
            req.limit = 100;
            req.peer = MessagesController.getInstance(getCurrentAccount()).getInputPeer(dialog_id);
            int reqIndex = sharedMediaData.requestIndex;
            int reqId = ConnectionsManager.getInstance(getCurrentAccount()).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (error != null) {
                    return;
                }
                if (reqIndex != sharedMediaData.requestIndex) {
                    return;
                }
                TLRPC.TL_messages_searchResultsPositions res = (TLRPC.TL_messages_searchResultsPositions) response;
                sharedMediaData.fastScrollPeriods.clear();
                for (int i = 0, n = res.positions.size(); i < n; i++) {
                    TLRPC.TL_searchResultPosition serverPeriod = res.positions.get(i);
                    if (serverPeriod.date != 0) {
                        Period period = new Period(serverPeriod);
                        sharedMediaData.fastScrollPeriods.add(period);
                    }
                }
                Collections.sort(sharedMediaData.fastScrollPeriods, (period, period2) -> period2.date - period.date);
                sharedMediaData.setTotalCount(res.count);
                sharedMediaData.fastScrollDataLoaded = true;
                if (!sharedMediaData.fastScrollPeriods.isEmpty()) {
                    if (mediaPage.selectedType == type) {
                        mediaPage.fastScrollEnabled = true;
                        updateFastScrollVisibility(mediaPage, true);
                    }
                }
                photoVideoAdapter.notifyDataSetChanged();
            }));
            ConnectionsManager.getInstance(getCurrentAccount()).bindRequestToGuid(reqId, getClassGuid());
        }
    }

    private PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview) {
            if (messageObject == null || mediaPage.selectedType != 0) {
                return null;
            }
            final RecyclerListView listView = mediaPage.listView;
            int firstVisiblePosition = -1;
            int lastVisiblePosition = -1;
            for (int a = 0, count = listView.getChildCount(); a < count; a++) {
                View view = listView.getChildAt(a);
                int visibleHeight = mediaPage.listView.getMeasuredHeight();
                View parent = (View) frameLayout;
                if (parent != null) {
                    if (frameLayout.getY() + frameLayout.getMeasuredHeight() > parent.getMeasuredHeight()) {
                        visibleHeight -= frameLayout.getBottom() - parent.getMeasuredHeight();
                    }
                }

                if (view.getTop() >= visibleHeight) {
                    continue;
                }
                int adapterPosition = listView.getChildAdapterPosition(view);
                if (adapterPosition < firstVisiblePosition || firstVisiblePosition == -1) {
                    firstVisiblePosition = adapterPosition;
                }
                if (adapterPosition > lastVisiblePosition || lastVisiblePosition == -1) {
                    lastVisiblePosition = adapterPosition;
                }
                int[] coords = new int[2];
                ImageReceiver imageReceiver = null;
                if (view instanceof SharedVideoItemCell) {
                    SharedVideoItemCell cell = (SharedVideoItemCell) view;
                    MessageObject message = cell.getMessageObject();
                    if (message == null) {
                        continue;
                    }
                    if (message.getId() == messageObject.getId()) {
                        imageReceiver = cell.imageReceiver();
                        cell.getLocationInWindow(coords);
                        coords[0] += Math.round(cell.imageReceiver().getImageX());
                        coords[1] += Math.round(cell.imageReceiver().getImageY());
                    }
                } else if (view instanceof SharedDocumentCell) {
                    SharedDocumentCell cell = (SharedDocumentCell) view;
                    MessageObject message = cell.getMessage();
                    if (message.getId() == messageObject.getId()) {
                        BackupImageView imageView = cell.getImageView();
                        imageReceiver = imageView.getImageReceiver();
                        imageView.getLocationInWindow(coords);
                    }
                } else if (view instanceof ContextLinkCell) {
                    ContextLinkCell cell = (ContextLinkCell) view;
                    MessageObject message = (MessageObject) cell.getParentObject();
                    if (message != null && message.getId() == messageObject.getId()) {
                        imageReceiver = cell.getPhotoImage();
                        cell.getLocationInWindow(coords);
                    }
                } else if (view instanceof SharedLinkCell) {
                    SharedLinkCell cell = (SharedLinkCell) view;
                    MessageObject message = cell.getMessage();
                    if (message != null && message.getId() == messageObject.getId()) {
                        imageReceiver = cell.getLinkImageView();
                        cell.getLocationInWindow(coords);
                    }
                }
                if (imageReceiver != null) {
                    PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                    object.viewX = coords[0];
                    object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                    object.parentView = listView;
                    object.animatingImageView = mediaPage.animatingImageView;
                    mediaPage.listView.getLocationInWindow(coords);
                    object.animatingImageViewYOffset = -coords[1];
                    object.imageReceiver = imageReceiver;
                    object.allowTakeAnimation = false;
                    object.radius = object.imageReceiver.getRoundRadius();
                    object.thumb = object.imageReceiver.getBitmapSafe();
                    object.parentView.getLocationInWindow(coords);
                    object.clipTopAddition = 0;
                    object.starOffset = sharedMediaData.startOffset;
//                 

                    if (PhotoViewer.isShowingImage(messageObject)) {
                        final View pinnedHeader = listView.getPinnedHeader();
                        if (pinnedHeader != null) {
                            int top = 0;

                            final int topOffset = top - object.viewY;
                            if (topOffset > view.getHeight()) {
                                listView.scrollBy(0, -(topOffset + pinnedHeader.getHeight()));
                            } else {
                                int bottomOffset = object.viewY - listView.getHeight();
                                if (view instanceof SharedDocumentCell) {
                                    bottomOffset -= AndroidUtilities.dp(8f);
                                }
                                if (bottomOffset >= 0) {
                                    listView.scrollBy(0, bottomOffset + view.getHeight());
                                }
                            }
                        }
                    }

                    return object;
                }
            }
            if (mediaPage.selectedType == 0 && firstVisiblePosition >= 0 && lastVisiblePosition >= 0) {
                int position = photoVideoAdapter.getPositionForIndex(index);
                if (position <= firstVisiblePosition) {
                    mediaPage.layoutManager.scrollToPositionWithOffset(position, 0);
                } else if (position >= lastVisiblePosition && lastVisiblePosition >= 0) {
                    mediaPage.layoutManager.scrollToPositionWithOffset(position, 0, true);
                }
            }

            return null;
        }
    };

    private static class MediaPage extends FrameLayout {

        public long lastCheckScrollTime;
        public boolean fastScrollEnabled;
        public ObjectAnimator fastScrollAnimator;
        private BlurredRecyclerView listView;
        private BlurredRecyclerView animationSupportingListView;
        private GridLayoutManager animationSupportingLayoutManager;
        private FlickerLoadingView progressView;
        private StickerEmptyView emptyView;
        private ExtendedGridLayoutManager layoutManager;
        private ClippingImageView animatingImageView;
        private RecyclerAnimationScrollHelper scrollHelper;
        private int selectedType;

        public SharedMediaFastScrollTooltip fastScrollHintView;
        public Runnable fastScrollHideHintRunnable;
        public boolean fastScrollHinWasShown;

        public int highlightMessageId;
        public boolean highlightAnimation;
        public float highlightProgress;


        public MediaPage(Context context) {
            super(context);
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (child == animationSupportingListView) {
                return true;
            }
            return super.drawChild(canvas, child, drawingTime);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (fastScrollHintView != null && fastScrollHintView.getVisibility() == View.VISIBLE) {
                boolean isVisible = false;
                RecyclerListView.FastScroll fastScroll = listView.getFastScroll();
                if (fastScroll != null) {
                    float y = fastScroll.getScrollBarY() + AndroidUtilities.dp(36);
                    float x = (getMeasuredWidth() - fastScrollHintView.getMeasuredWidth() - AndroidUtilities.dp(16));
                    fastScrollHintView.setPivotX(fastScrollHintView.getMeasuredWidth());
                    fastScrollHintView.setPivotY(0);
                    fastScrollHintView.setTranslationX(x);
                    fastScrollHintView.setTranslationY(y);
                }

                if (fastScroll.getProgress() > 0.85f) {
                    showFastScrollHint(this, null, false);
                }
            }
        }

    }

    private static void showFastScrollHint(MediaPage mediaPage, SharedMediaData sharedMediaData, boolean show) {
        if (show) {
            if (SharedConfig.fastScrollHintCount <= 0 || mediaPage.fastScrollHintView != null || mediaPage.fastScrollHinWasShown || mediaPage.listView.getFastScroll() == null  || mediaPage.listView.getFastScroll().getVisibility() != View.VISIBLE || sharedMediaData.totalCount < 50) {
                return;
            }
            SharedConfig.setFastScrollHintCount(SharedConfig.fastScrollHintCount - 1);
            mediaPage.fastScrollHinWasShown = true;
            SharedMediaFastScrollTooltip tooltip = new SharedMediaFastScrollTooltip(mediaPage.getContext());
            mediaPage.fastScrollHintView = tooltip;
            mediaPage.addView(mediaPage.fastScrollHintView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            mediaPage.fastScrollHintView.setAlpha(0);
            mediaPage.fastScrollHintView.setScaleX(0.8f);
            mediaPage.fastScrollHintView.setScaleY(0.8f);
            mediaPage.fastScrollHintView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start();
            mediaPage.invalidate();

            AndroidUtilities.runOnUIThread(mediaPage.fastScrollHideHintRunnable = () -> {
                mediaPage.fastScrollHintView = null;
                mediaPage.fastScrollHideHintRunnable = null;
                tooltip.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(220).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (tooltip.getParent() != null) {
                            ((ViewGroup) tooltip.getParent()).removeView(tooltip);
                        }
                    }
                }).start();
            }, 4000);
        } else {
            if (mediaPage.fastScrollHintView == null || mediaPage.fastScrollHideHintRunnable == null) {
                return;
            }
            AndroidUtilities.cancelRunOnUIThread(mediaPage.fastScrollHideHintRunnable);
            mediaPage.fastScrollHideHintRunnable.run();
            mediaPage.fastScrollHideHintRunnable = null;
            mediaPage.fastScrollHintView = null;
        }
    }

    FlickerLoadingView globalGradientView;
    public static final int FILTER_VIDEOS_ONLY = 2;

    private static final int[] supportedFastScrollTypes = new int[] {
            MediaDataController.MEDIA_PHOTOVIDEO,
    };
    public boolean isInFastScroll() {
        return mediaPage != null && mediaPage.listView.getFastScroll() != null && mediaPage.listView.getFastScroll().isPressed();
    }
    public boolean dispatchFastScrollEvent(MotionEvent ev) {
        View view = (View)frameLayout;
        ev.offsetLocation(-view.getX() - frameLayout.getX() - mediaPage.listView.getFastScroll().getX(), -view.getY() - frameLayout.getY() - mediaPage.getY() - mediaPage.listView.getFastScroll().getY());
        return mediaPage.listView.getFastScroll().dispatchTouchEvent(ev);
    }

    boolean isPinnedToTop;

    public boolean isPinnedToTop() {
        return isPinnedToTop;
    }

    public void setPinnedToTop(boolean pinnedToTop) {
        if (isPinnedToTop != pinnedToTop) {
            isPinnedToTop = pinnedToTop;
            updateFastScrollVisibility(mediaPage, true);

        }
    }

    public void drawListForBlur(Canvas blurCanvas) {
        if (mediaPage != null && mediaPage.getVisibility() == View.VISIBLE) {
            for (int j = 0; j < mediaPage.listView.getChildCount(); j++) {
                View child = mediaPage.listView.getChildAt(j);
                if (child.getY() < mediaPage.listView.blurTopPadding + AndroidUtilities.dp(100)) {
                    int restore = blurCanvas.save();
                    blurCanvas.translate(mediaPage.getX() + child.getX(), frameLayout.getY() +mediaPage.getY() + mediaPage.listView.getY() + child.getY());
                    child.draw(blurCanvas);
                    blurCanvas.restoreToCount(restore);
                }
            }
        }
    }
    public int getPhotosVideosTypeFilter() {
        return sharedMediaData.filterType;
    }

    public void updateFastScrollVisibility(MediaPage mediaPage, boolean animated) {
        boolean show = mediaPage.fastScrollEnabled && isPinnedToTop;
        View view = mediaPage.listView.getFastScroll();
        if (mediaPage.fastScrollAnimator != null) {
            mediaPage.fastScrollAnimator.removeAllListeners();
            mediaPage.fastScrollAnimator.cancel();
        }
        if (!animated) {
            view.animate().setListener(null).cancel();
            view.setVisibility(show ? View.VISIBLE : View.GONE);
            view.setTag(show ? 1 : null);
            view.setAlpha(1f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        } else if (show && view.getTag() == null) {
            view.animate().setListener(null).cancel();
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(0f);
            }
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 1f);
            mediaPage.fastScrollAnimator = objectAnimator;
            objectAnimator.setDuration(150).start();
            view.setTag(1);
        } else if (!show && view.getTag() != null) {

            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 0f);
            objectAnimator.addListener(new HideViewAfterAnimation(view));
            mediaPage.fastScrollAnimator = objectAnimator;
            objectAnimator.setDuration(150).start();
            view.animate().setListener(null).cancel();

            view.setTag(null);
        }
    }

    private SharedPhotoVideoAdapter photoVideoAdapter;
    private SharedPhotoVideoAdapter animationSupportingPhotoVideoAdapter;

    private MediaPage mediaPage;
    private FrameLayout frameLayout;
    private ActionBarMenuItem forwardItem;
    private ActionBarMenuItem gotoItem;
    private ActionBarMenuItem deleteItem;
    private ActionBarMenuItem forwardNoQuoteItem;
    private ArrayList<SharedPhotoVideoCell> cellCache = new ArrayList<>(10);
    private ArrayList<SharedPhotoVideoCell> cache = new ArrayList<>(10);
    private ChatActionCell floatingDateView;
    private AnimatorSet floatingDateAnimation;
    private Runnable hideFloatingDateRunnable = () -> hideFloatingDateView(true);
    private ArrayList<View> actionModeViews = new ArrayList<>();


    private float additionalFloatingTranslation;
    private Paint backgroundPaint = new Paint();

    private SparseArray<MessageObject>[] selectedFiles = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private int cantDeleteMessagesCount;
    private boolean scrolling;
    private long mergeDialogId;
    private TLRPC.ChatFull info;

    private long dialog_id;
    public boolean scrollingByUser;
    private int mediaColumnsCount = 3;

    private float photoVideoChangeColumnsProgress;
    private boolean photoVideoChangeColumnsAnimation;
    private ArrayList<SharedVideoItemCell> animationSupportingSortedCells = new ArrayList<>();
    private int animateToColumnsCount;


    private NumberTextView selectedMessagesCountTextView;
    private ImageView closeButton;
    private BackDrawable backDrawable;

    private int hasMedia;
    public interface SharedMediaPreloaderDelegate {
        void mediaCountUpdated();
    }
    public static class SharedMediaPreloader implements NotificationCenter.NotificationCenterDelegate {

        private int mediaCount;
        private int mediaMergeCount;
        private int lastMediaCount;
        private int lastLoadMediaCount;
        private SharedMediaData sharedMediaData;
        private long dialogId;
        private long mergeDialogId;
        private BaseFragment parentFragment;
        private ArrayList<SharedMediaPreloaderDelegate> delegates = new ArrayList<>();
        private boolean mediaWasLoaded;


        public SharedMediaPreloader(BaseFragment fragment,long dialog_id) {
            parentFragment = fragment;
            dialogId = dialog_id;
            sharedMediaData = new SharedMediaData();
            sharedMediaData.setMaxId(0, DialogObject.isEncryptedDialog(dialogId) ? Integer.MIN_VALUE : Integer.MAX_VALUE);

            loadMediaCounts();

            NotificationCenter notificationCenter = parentFragment.getNotificationCenter();
            notificationCenter.addObserver(this, NotificationCenter.mediaCountsDidLoad);
            notificationCenter.addObserver(this, NotificationCenter.mediaCountDidLoad);
            notificationCenter.addObserver(this, NotificationCenter.didReceiveNewMessages);
            notificationCenter.addObserver(this, NotificationCenter.messageReceivedByServer);
            notificationCenter.addObserver(this, NotificationCenter.mediaDidLoad);
            notificationCenter.addObserver(this, NotificationCenter.messagesDeleted);
            notificationCenter.addObserver(this, NotificationCenter.replaceMessagesObjects);
            notificationCenter.addObserver(this, NotificationCenter.chatInfoDidLoad);
            notificationCenter.addObserver(this, NotificationCenter.fileLoaded);
        }

        public void addDelegate(SharedMediaPreloaderDelegate delegate) {
            delegates.add(delegate);
        }

        public void removeDelegate(SharedMediaPreloaderDelegate delegate) {
            delegates.remove(delegate);
        }

        public void onDestroy(BaseFragment fragment) {
            if (fragment != parentFragment) {
                return;
            }
            delegates.clear();
            NotificationCenter notificationCenter = parentFragment.getNotificationCenter();
            notificationCenter.removeObserver(this, NotificationCenter.mediaCountsDidLoad);
            notificationCenter.removeObserver(this, NotificationCenter.mediaCountDidLoad);
            notificationCenter.removeObserver(this, NotificationCenter.didReceiveNewMessages);
            notificationCenter.removeObserver(this, NotificationCenter.messageReceivedByServer);
            notificationCenter.removeObserver(this, NotificationCenter.mediaDidLoad);
            notificationCenter.removeObserver(this, NotificationCenter.messagesDeleted);
            notificationCenter.removeObserver(this, NotificationCenter.replaceMessagesObjects);
            notificationCenter.removeObserver(this, NotificationCenter.chatInfoDidLoad);
            notificationCenter.removeObserver(this, NotificationCenter.fileLoaded);
        }

        public int getLastMediaCount() {
            return lastMediaCount;
        }

        public SharedMediaData  getSharedMediaData() {
            return sharedMediaData;
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.mediaCountsDidLoad) {
                long did = (Long) args[0];
                if (did == dialogId || did == mergeDialogId) {
                    int[] counts = (int[]) args[1];
                    if (did == dialogId) {
                        mediaCount = counts[0];
                    } else {
                        mediaMergeCount = counts[0];
                    }
                    if (mediaCount >= 0 && mediaMergeCount >= 0) {
                        lastMediaCount = mediaCount + mediaMergeCount;
                    } else if (mediaCount >= 0) {
                        lastMediaCount = mediaCount;
                    } else {
                        lastMediaCount = Math.max(mediaMergeCount, 0);
                    }
                    if (did == dialogId && lastMediaCount != 0 && lastLoadMediaCount != mediaCount) {
                        int  type = MediaDataController.MEDIA_VIDEOS_ONLY;
                        parentFragment.getMediaDataController().loadMedia(did, lastLoadMediaCount == -1 ? 30 : 20, 0, 0, type, 2, parentFragment.getClassGuid(), 0);
                        lastLoadMediaCount = mediaCount;
                    }
                    mediaWasLoaded = true;
                    for (int a = 0, N = delegates.size(); a < N; a++) {
                        delegates.get(a).mediaCountUpdated();
                    }
                }
            } else if (id == NotificationCenter.mediaCountDidLoad) {
                long did = (Long) args[0];
                if (did == dialogId || did == mergeDialogId) {
                    int type = (Integer) args[3];
                    int mCount = (Integer) args[1];
                    if (did == dialogId) {
                        mediaCount = mCount;
                    } else {
                        mediaMergeCount= mCount;
                    }
                    if (mediaCount >= 0 && mediaMergeCount >= 0) {
                        lastMediaCount = mediaCount + mediaMergeCount;
                    } else if (mediaCount >= 0) {
                        lastMediaCount = mediaCount;
                    } else {
                        lastMediaCount = Math.max(mediaMergeCount, 0);
                    }
                    for (int a = 0, N = delegates.size(); a < N; a++) {
                        delegates.get(a).mediaCountUpdated();
                    }
                }
            } else if (id == NotificationCenter.didReceiveNewMessages) {
                boolean scheduled = (Boolean) args[2];
                if (scheduled) {
                    return;
                }
                if (dialogId == (Long) args[0]) {
                    boolean enc = DialogObject.isEncryptedDialog(dialogId);
                    ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject obj = arr.get(a);
                        if (obj.messageOwner.media == null || obj.needDrawBluredPreview()) {
                            continue;
                        }
                        int type = MediaDataController.getMediaType(obj.messageOwner);
                        if (type == -1) {
                            continue;
                        }
                        if (!obj.isVideo()) {
                            continue;
                        }
                        if (sharedMediaData.startReached) {
                            sharedMediaData.addMessage(obj, 0, true, enc);
                        }
                        sharedMediaData.totalCount++;
                        for (int i = 0; i < sharedMediaData.fastScrollPeriods.size(); i++) {
                            sharedMediaData.fastScrollPeriods.get(i).startOffset++;
                        }
                    }
                    loadMediaCounts();
                }
            } else if (id == NotificationCenter.messageReceivedByServer) {
                Boolean scheduled = (Boolean) args[6];
                if (scheduled) {
                    return;
                }
                Integer msgId = (Integer) args[0];
                Integer newMsgId = (Integer) args[1];
                sharedMediaData.replaceMid(msgId, newMsgId);
            } else if (id == NotificationCenter.mediaDidLoad) {
                long did = (Long) args[0];
                int guid = (Integer) args[3];
                if (guid == parentFragment.getClassGuid()) {
                    int type = (Integer) args[4];
                    if (type != 0 && type != 6 && type != 7 && type != 1 && type != 2 && type != 4) {
                        sharedMediaData.setTotalCount((Integer) args[1]);
                    }
                    ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[2];
                    boolean enc = DialogObject.isEncryptedDialog(did);
                    int loadIndex = did == dialogId ? 0 : 1;
                    if (type == 0 || type == 6 || type == 7) {
                        if (type != sharedMediaData.filterType) {
                            return;
                        }
                        type = 0;
                    }
                    if (!arr.isEmpty()) {
                        sharedMediaData.setEndReached(loadIndex, (Boolean) args[5]);
                    }
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject message = arr.get(a);
                        sharedMediaData.addMessage(message, loadIndex, false, enc);
                    }
                }
            } else if (id == NotificationCenter.messagesDeleted) {
                boolean scheduled = (Boolean) args[2];
                if (scheduled) {
                    return;
                }
                long channelId = (Long) args[1];
                TLRPC.Chat currentChat;
                if (DialogObject.isChatDialog(dialogId)) {
                    currentChat = parentFragment.getMessagesController().getChat(-dialogId);
                } else {
                    currentChat = null;
                }
                if (ChatObject.isChannel(currentChat)) {
                    if (!(channelId == 0 && mergeDialogId != 0 || channelId == currentChat.id)) {
                        return;
                    }
                } else if (channelId != 0) {
                    return;
                }

                boolean changed = false;
                int type;
                ArrayList<Integer> markAsDeletedMessages = (ArrayList<Integer>) args[0];
                for (int a = 0, N = markAsDeletedMessages.size(); a < N; a++) {
                    MessageObject messageObject = sharedMediaData.deleteMessage(markAsDeletedMessages.get(a), 0);
                    if (messageObject != null) {
                        if (messageObject.getDialogId() == dialogId) {
                            if (mediaCount > 0) {
                                mediaCount--;
                            }
                        } else {
                            if (mediaMergeCount > 0) {
                                mediaMergeCount--;
                            }
                        }
                        changed = true;
                    }
                }
                if (changed) {
                    if (mediaCount >= 0 && mediaMergeCount >= 0) {
                        lastMediaCount = mediaCount + mediaMergeCount;
                    } else if (mediaCount >= 0) {
                        lastMediaCount = mediaCount;
                    } else {
                        lastMediaCount = Math.max(mediaMergeCount, 0);
                    }
                    for (int a = 0, N = delegates.size(); a < N; a++) {
                        delegates.get(a).mediaCountUpdated();
                    }
                }
                loadMediaCounts();
            } else if (id == NotificationCenter.replaceMessagesObjects) {
                long did = (long) args[0];
                if (did != dialogId && did != mergeDialogId) {
                    return;
                }
                int loadIndex = did == dialogId ? 0 : 1;
                ArrayList<MessageObject> messageObjects = (ArrayList<MessageObject>) args[1];
                for (int b = 0, N = messageObjects.size(); b < N; b++) {
                    MessageObject messageObject = messageObjects.get(b);
                    int mid = messageObject.getId();
                    int type = MediaDataController.getMediaType(messageObject.messageOwner);
                    MessageObject old = sharedMediaData.messagesDict[loadIndex].get(mid);
                    if (old != null) {
                        int oldType = MediaDataController.getMediaType(messageObject.messageOwner);
                        if (type == -1 || oldType != type) {
                            sharedMediaData.deleteMessage(mid, loadIndex);
                            if (loadIndex == 0) {
                                if (mediaCount > 0) {
                                    mediaCount--;
                                }
                            } else {
                                if (mediaMergeCount > 0) {
                                    mediaMergeCount--;
                                }
                            }
                        } else {
                            int idx = sharedMediaData.messages.indexOf(old);
                            if (idx >= 0) {
                                sharedMediaData.messagesDict[loadIndex].put(mid, messageObject);
                                sharedMediaData.messages.set(idx, messageObject);
                            }
                        }
                        break;
                    }
                }
            } else if (id == NotificationCenter.chatInfoDidLoad) {
                TLRPC.ChatFull chatFull = (TLRPC.ChatFull) args[0];
                if (dialogId < 0 && chatFull.id == -dialogId) {
                    setChatInfo(chatFull);
                }
            } else if (id == NotificationCenter.fileLoaded) {
                ArrayList<MessageObject> allMessages = new ArrayList<>();
                allMessages.addAll(sharedMediaData.messages);
                Utilities.globalQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        FileLoader.getInstance(account).checkMediaExistance(allMessages);
                    }
                });
            }
        }

        private void loadMediaCounts() {
            parentFragment.getMediaDataController().getMediaCounts(dialogId, parentFragment.getClassGuid());
            if (mergeDialogId != 0) {
                parentFragment.getMediaDataController().getMediaCounts(mergeDialogId, parentFragment.getClassGuid());
            }
        }

        private void setChatInfo(TLRPC.ChatFull chatInfo) {
            if (chatInfo != null && chatInfo.migrated_from_chat_id != 0 && mergeDialogId == 0) {
                mergeDialogId = -chatInfo.migrated_from_chat_id;
                parentFragment.getMediaDataController().getMediaCounts(mergeDialogId, parentFragment.getClassGuid());
            }
        }

        public boolean isMediaWasLoaded() {
            return mediaWasLoaded;
        }
    }

    public static class SharedMediaData {
        public ArrayList<MessageObject> messages = new ArrayList<>();
        public SparseArray<MessageObject>[] messagesDict = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
        public ArrayList<String> sections = new ArrayList<>();
        public HashMap<String, ArrayList<MessageObject>> sectionArrays = new HashMap<>();
        public ArrayList<Period> fastScrollPeriods = new ArrayList<>();
        public int totalCount;
        public boolean loading;
        public boolean fastScrollDataLoaded;
        public boolean[] endReached = new boolean[]{false, true};
        public int[] max_id = new int[]{0, 0};
        public int min_id;
        public boolean startReached = true;
        private int startOffset;
        private int endLoadingStubs;
        public boolean loadingAfterFastScroll;
        public int requestIndex;

        public int filterType = FILTER_VIDEOS_ONLY;
        public boolean isFrozen;
        public ArrayList<MessageObject> frozenMessages = new ArrayList<>();
        public int frozenStartOffset;
        public int frozenEndLoadingStubs;
        private boolean hasVideos;
        private boolean hasPhotos;

        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();

        public void setTotalCount(int count) {
            totalCount = count;
        }

        public void setMaxId(int num, int value) {
            max_id[num] = value;
        }

        public void setEndReached(int num, boolean value) {
            endReached[num] = value;
        }

        public boolean addMessage(MessageObject messageObject, int loadIndex, boolean isNew, boolean enc) {
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
            if (!enc) {
                if (messageObject.getId() > 0) {
                    max_id[loadIndex] = Math.min(messageObject.getId(), max_id[loadIndex]);
                    min_id = Math.max(messageObject.getId(), min_id);
                }
            } else {
                max_id[loadIndex] = Math.max(messageObject.getId(), max_id[loadIndex]);
                min_id = Math.min(messageObject.getId(), min_id);
            }
            if (!hasVideos && messageObject.isVideo()) {
                hasVideos = true;
            }
            if (!hasPhotos && messageObject.isPhoto()) {
                hasPhotos = true;
            }
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
                max_id[0] = Math.min(newMid, max_id[0]);
            }
        }

        public ArrayList<MessageObject> getMessages() {
            return isFrozen ? frozenMessages : messages;
        }

        public int getStartOffset() {
            return isFrozen ? frozenStartOffset : startOffset;
        }

        public void setListFrozen(boolean frozen) {
            if (isFrozen == frozen) {
                return;
            }
            isFrozen = frozen;
            if (frozen) {
                frozenStartOffset = startOffset;
                frozenEndLoadingStubs = endLoadingStubs;
                frozenMessages.clear();
                frozenMessages.addAll(messages);
            }
        }

        public int getEndLoadingStubs() {
            return isFrozen ? frozenEndLoadingStubs : endLoadingStubs;
        }
    }

    public static class Period {
        public String formatedDate;
        public int startOffset;
        int date;
        //int messagesCount;
        int maxId;

        public Period(TLRPC.TL_searchResultPosition calendarPeriod) {
            this.date = calendarPeriod.date;
            this.maxId = calendarPeriod.msg_id;
            this.startOffset = calendarPeriod.offset;
            formatedDate = LocaleController.formatYearMont(this.date, true);
        }
    }

    private SharedMediaData sharedMediaData;
    private SharedMediaPreloader sharedMediaPreloader;

    private Drawable pinnedHeaderShadowDrawable;
    private ActionBarMenu actionModeLayout;


    private final static int forward = 100;
    private final static int forward_noquote = 1001;
    private final static int delete = 101;
    private final static int gotochat = 102;

    private TLRPC.Chat currentChat;
    private TLRPC.User currentUser;
    private TLRPC.EncryptedChat currentEncryptedChat;


    public VideoFragment(Bundle args){
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        final long chatId = arguments.getLong("chat_id", 0);
        final long userId = arguments.getLong("user_id", 0);
        final int encId = arguments.getInt("enc_id", 0);

        if (chatId != 0) {
            currentChat = getMessagesController().getChat(chatId);
            if (currentChat == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                final MessagesStorage messagesStorage = getMessagesStorage();
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentChat = messagesStorage.getChat(chatId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentChat != null) {
                    getMessagesController().putChat(currentChat, true);
                } else {
                    return false;
                }
            }
            dialog_id = -chatId;
            if (ChatObject.isChannel(currentChat)) {
                getMessagesController().startShortPoll(currentChat, classGuid, false);
            }
        } else if (userId != 0) {
            currentUser = getMessagesController().getUser(userId);
            if (currentUser == null) {
                final MessagesStorage messagesStorage = getMessagesStorage();
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentUser = messagesStorage.getUser(userId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentUser != null) {
                    getMessagesController().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = userId;
        } else if (encId != 0) {
            currentEncryptedChat = getMessagesController().getEncryptedChat(encId);
            final MessagesStorage messagesStorage = getMessagesStorage();
            if (currentEncryptedChat == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentEncryptedChat = messagesStorage.getEncryptedChat(encId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentEncryptedChat != null) {
                    getMessagesController().putEncryptedChat(currentEncryptedChat, true);
                } else {
                    return false;
                }
            }
            currentUser = getMessagesController().getUser(currentEncryptedChat.user_id);
            if (currentUser == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    currentUser = messagesStorage.getUser(currentEncryptedChat.user_id);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentUser != null) {
                    getMessagesController().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = DialogObject.makeEncryptedDialogId(encId);
        } else {
            return false;
        }
        if (this.sharedMediaPreloader == null) {
            this.sharedMediaPreloader = new SharedMediaPreloader(VideoFragment.this,dialog_id);
        }
        getNotificationCenter().addObserver(this, NotificationCenter.mediaDidLoad);
        getNotificationCenter().addObserver(this, NotificationCenter.messagesDeleted);
        getNotificationCenter().addObserver(this, NotificationCenter.didReceiveNewMessages);
        getNotificationCenter().addObserver(this, NotificationCenter.messageReceivedByServer);
        getNotificationCenter().addObserver(this, NotificationCenter.messagePlayingDidReset);
        getNotificationCenter().addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        getNotificationCenter().addObserver(this, NotificationCenter.messagePlayingDidStart);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.mediaDidLoad);
        getNotificationCenter().removeObserver(this, NotificationCenter.didReceiveNewMessages);
        getNotificationCenter().removeObserver(this, NotificationCenter.messagesDeleted);
        getNotificationCenter().removeObserver(this, NotificationCenter.messageReceivedByServer);
        getNotificationCenter().removeObserver(this, NotificationCenter.messagePlayingDidReset);
        getNotificationCenter().removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        getNotificationCenter().removeObserver(this, NotificationCenter.messagePlayingDidStart);

        super.onFragmentDestroy();
    }

    public String getTitle(){
//        if(currentChat != null){
//            return  currentChat.title;
//        }else if(currentUser != null){
//            return currentUser.first_name;
//        }
        return "Videos";
    }


    @Override
    public View createView(Context context) {
        actionBar.setBackButtonDrawable(backDrawable = new BackDrawable(false));
        actionBar.setTitle(getTitle());
        frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;

        globalGradientView = new FlickerLoadingView(context);
        globalGradientView.setIsSingleCell(true);
        sharedMediaPreloader = new SharedMediaPreloader(this,dialog_id);
        hasMedia = 0;
        if (info != null) {
            mergeDialogId = -info.migrated_from_chat_id;
        }
        sharedMediaData = new SharedMediaData();
        sharedMediaData.max_id[0] = DialogObject.isEncryptedDialog(dialog_id) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        fillMediaData();
        if (mergeDialogId != 0 && info != null) {
            sharedMediaData.max_id[1] = info.migrated_from_max_id;
            sharedMediaData.endReached[1] = false;
        }
        mediaColumnsCount = 2;
        pinnedHeaderShadowDrawable = context.getResources().getDrawable(R.drawable.photos_header_shadow);
        pinnedHeaderShadowDrawable.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundGrayShadow), PorterDuff.Mode.MULTIPLY));
        for (int a = 1; a >= 0; a--) {
            selectedFiles[a].clear();
        }
        cantDeleteMessagesCount = 0;
        actionModeViews.clear();
        createActionMode();
        photoVideoAdapter = new SharedPhotoVideoAdapter(context) {
            @Override
            public void notifyDataSetChanged() {
                super.notifyDataSetChanged();
                if (mediaPage != null && mediaPage.animationSupportingListView.getVisibility() == View.VISIBLE) {
                    animationSupportingPhotoVideoAdapter.notifyDataSetChanged();
                }
            }
        };
        animationSupportingPhotoVideoAdapter = new SharedPhotoVideoAdapter(context);
        int scrollToPositionOnRecreate = -1;
        int scrollToOffsetOnRecreate = 0;



        //extrct scoll postion
        if (mediaPage != null && mediaPage.layoutManager != null) {
            scrollToPositionOnRecreate = mediaPage.layoutManager.findFirstVisibleItemPosition();
            if (scrollToPositionOnRecreate !=mediaPage.layoutManager.getItemCount() - 1) {
                RecyclerListView.Holder holder = (RecyclerListView.Holder) mediaPage.listView.findViewHolderForAdapterPosition(scrollToPositionOnRecreate);
                if (holder != null) {
                    scrollToOffsetOnRecreate = holder.itemView.getTop();
                } else {
                    scrollToPositionOnRecreate = -1;
                }
            } else {
                scrollToPositionOnRecreate = -1;
            }
        }

        final MediaPage mediaPage = new MediaPage(context) {
            @Override
            public void setTranslationX(float translationX) {
                super.setTranslationX(translationX);
                invalidateBlur();
            }
        };
        frameLayout.addView(mediaPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 0));
        this.mediaPage = mediaPage;

        final ExtendedGridLayoutManager layoutManager  = new ExtendedGridLayoutManager(context, 100) {

            private Size size = new Size();

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }

            @Override
            protected void calculateExtraLayoutSpace(RecyclerView.State state, int[] extraLayoutSpace) {
                super.calculateExtraLayoutSpace(state, extraLayoutSpace);
                if (mediaPage.selectedType == 0) {
                    extraLayoutSpace[1] = Math.max(extraLayoutSpace[1], getItemSize(1) * 2);
                }
            }

            @Override
            protected Size getSizeForItem(int i) {
                size.width = size.height = 100;
                return size;
            }

            @Override
            protected int getFlowItemCount() {
                return 0;
            }

            @Override
            public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
                final AccessibilityNodeInfoCompat.CollectionItemInfoCompat itemInfo = info.getCollectionItemInfo();
                if (itemInfo != null && itemInfo.isHeading()) {
                    info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(itemInfo.getRowIndex(), itemInfo.getRowSpan(), itemInfo.getColumnIndex(), itemInfo.getColumnSpan(), false));
                }
            }
        };
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mediaPage.listView.getAdapter() == photoVideoAdapter) {
                    if (photoVideoAdapter.getItemViewType(position) == 2) {
                        return mediaColumnsCount;
                    }
                    return 1;
                }
                return mediaPage.layoutManager.getSpanSizeForItem(position);
            }
        });
        mediaPage.listView = new BlurredRecyclerView(context) {

            HashSet<SharedVideoItemCell> excludeDrawViews = new HashSet<>();
            ArrayList<SharedVideoItemCell> drawingViews = new ArrayList<>();
            ArrayList<SharedVideoItemCell> drawingViews2 = new ArrayList<>();
            ArrayList<SharedVideoItemCell> drawingViews3 = new ArrayList<>();

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                checkLoadMoreScroll(mediaPage, mediaPage.listView, layoutManager);
                if (mediaPage.selectedType == 0) {
                    PhotoViewer.getInstance().checkCurrentImageVisibility();
                }
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (getAdapter() == photoVideoAdapter) {
                    int firstVisibleItemPosition = 0;
                    int firstVisibleItemPosition2 = 0;
                    int lastVisibleItemPosition = 0;
                    int lastVisibleItemPosition2 = 0;

                    int rowsOffset = 0;
                    int columnsOffset = 0;
                    float minY = getMeasuredHeight();
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        if (child.getTop() > getMeasuredHeight() || child.getBottom() < 0) {
                            if (child instanceof SharedVideoItemCell) {
                                SharedVideoItemCell cell = (SharedVideoItemCell) getChildAt(i);
                                cell.setCrossfadeView(null, 0, 0);
                                cell.setTranslationX(0);
                                cell.setTranslationY(0);
                                cell.setImageScale(1f, !photoVideoChangeColumnsAnimation);
                            }
                            continue;
                        }
                        if (child instanceof SharedVideoItemCell) {
                            SharedVideoItemCell cell = (SharedVideoItemCell) getChildAt(i);

                            if (cell.getMessageId() == mediaPage.highlightMessageId && cell.imageReceiver().hasBitmapImage()) {
                                if (!mediaPage.highlightAnimation) {
                                    mediaPage.highlightProgress = 0;
                                    mediaPage.highlightAnimation = true;
                                }
                                float p = 1f;
                                if (mediaPage.highlightProgress < 0.3f) {
                                    p = mediaPage.highlightProgress / 0.3f;
                                } else if (mediaPage.highlightProgress > 0.7f) {
                                    p = (1f - mediaPage.highlightProgress) / 0.3f;
                                }
                                cell.setHighlightProgress(p);
                            } else {
                                cell.setHighlightProgress(0);
                            }

                            MessageObject messageObject = cell.getMessageObject();
                            float alpha = 1f;
                            if (messageObject != null && messageAlphaEnter.get(messageObject.getId(), null) != null) {
                                alpha = messageAlphaEnter.get(messageObject.getId(), 1f);
                            }
                            cell.setImageAlpha(alpha, !photoVideoChangeColumnsAnimation);

                            boolean inAnimation = false;
                            if (photoVideoChangeColumnsAnimation) {
                                float fromScale = 1f;

                                int currentColumn = ((GridLayoutManager.LayoutParams) cell.getLayoutParams()).getViewAdapterPosition() % mediaColumnsCount + columnsOffset;
                                int currentRow = (((GridLayoutManager.LayoutParams) cell.getLayoutParams()).getViewAdapterPosition() - firstVisibleItemPosition) / mediaColumnsCount + rowsOffset;
                                int toIndex = currentRow * animateToColumnsCount + currentColumn;
                                if (currentColumn >= 0 && currentColumn < animateToColumnsCount && toIndex >= 0 && toIndex < animationSupportingSortedCells.size()) {
                                    inAnimation = true;
                                    float toScale = (animationSupportingSortedCells.get(toIndex).getMeasuredWidth() - AndroidUtilities.dpf2(2)) / (float) (cell.getMeasuredWidth() - AndroidUtilities.dpf2(2));
                                    float scale = fromScale * (1f - photoVideoChangeColumnsProgress) + toScale * photoVideoChangeColumnsProgress;
                                    float fromX = cell.getLeft();
                                    float fromY = cell.getTop();
                                    float toX = animationSupportingSortedCells.get(toIndex).getLeft();
                                    float toY = animationSupportingSortedCells.get(toIndex).getTop();

                                    cell.setPivotX(0);
                                    cell.setPivotY(0);
                                    cell.setImageScale(scale, !photoVideoChangeColumnsAnimation);
                                    cell.setTranslationX((toX - fromX) * photoVideoChangeColumnsProgress);
                                    cell.setTranslationY((toY - fromY) * photoVideoChangeColumnsProgress);
                                    cell.setCrossfadeView(animationSupportingSortedCells.get(toIndex), photoVideoChangeColumnsProgress, animateToColumnsCount);
                                    excludeDrawViews.add(animationSupportingSortedCells.get(toIndex));
                                    drawingViews3.add(cell);
                                    canvas.save();
                                    canvas.translate(cell.getX(), cell.getY());
                                    cell.draw(canvas);
                                    canvas.restore();

                                    if (cell.getY() < minY) {
                                        minY = cell.getY();
                                    }

                                }
                            }

                            if (!inAnimation) {
                                if (photoVideoChangeColumnsAnimation) {
                                    drawingViews2.add(cell);
                                }
                                cell.setCrossfadeView(null, 0, 0);
                                cell.setTranslationX(0);
                                cell.setTranslationY(0);
                                cell.setImageScale(1f, !photoVideoChangeColumnsAnimation);
                            }
                        }
                    }

                    if (photoVideoChangeColumnsAnimation && !drawingViews.isEmpty()) {
                        float toScale = animateToColumnsCount / (float) mediaColumnsCount;
                        float scale = toScale * (1f - photoVideoChangeColumnsProgress) + photoVideoChangeColumnsProgress;

                        float sizeToScale = ((getMeasuredWidth() / (float) mediaColumnsCount) - AndroidUtilities.dpf2(2)) / ((getMeasuredWidth() / (float) animateToColumnsCount) - AndroidUtilities.dpf2(2));
                        float scaleSize = sizeToScale * (1f - photoVideoChangeColumnsProgress) + photoVideoChangeColumnsProgress;

                        float fromSize = getMeasuredWidth() / (float) mediaColumnsCount;
                        float toSize = (getMeasuredWidth() / (float) animateToColumnsCount);
                        float size1 = (float) ((Math.ceil((getMeasuredWidth() / (float) animateToColumnsCount)) - AndroidUtilities.dpf2(2)) * scaleSize + AndroidUtilities.dpf2(2));

                        for (int i = 0; i < drawingViews.size(); i++) {
                            SharedVideoItemCell view = drawingViews.get(i);
                            if (excludeDrawViews.contains(view)) {
                                continue;
                            }
                            view.setCrossfadeView(null, 0, 0);
                            int fromColumn = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getViewAdapterPosition() % animateToColumnsCount;
                            int toColumn = fromColumn - columnsOffset;
                            int currentRow = (((GridLayoutManager.LayoutParams) view.getLayoutParams()).getViewAdapterPosition() - firstVisibleItemPosition2) / animateToColumnsCount;
                            currentRow -= rowsOffset;

                            canvas.save();
                            canvas.translate(toColumn * fromSize * (1f - photoVideoChangeColumnsProgress) + toSize * fromColumn * photoVideoChangeColumnsProgress, minY + size1 * currentRow);
                            view.setImageScale(scaleSize, !photoVideoChangeColumnsAnimation);
                            if (toColumn < mediaColumnsCount) {
                                canvas.saveLayerAlpha(0, 0, view.getMeasuredWidth() * scale, view.getMeasuredWidth() * scale, (int) (photoVideoChangeColumnsProgress * 255), Canvas.ALL_SAVE_FLAG);
                                view.draw(canvas);
                                canvas.restore();
                            } else {
                                view.draw(canvas);
                            }
                            canvas.restore();
                        }
                    }

                    super.dispatchDraw(canvas);

                    if (photoVideoChangeColumnsAnimation) {
                        float toScale = mediaColumnsCount / (float) animateToColumnsCount;
                        float scale = toScale * photoVideoChangeColumnsProgress + (1f - photoVideoChangeColumnsProgress);

                        float sizeToScale = ((getMeasuredWidth() / (float) animateToColumnsCount) - AndroidUtilities.dpf2(2)) / ((getMeasuredWidth() / (float) mediaColumnsCount) - AndroidUtilities.dpf2(2));
                        float scaleSize = sizeToScale * photoVideoChangeColumnsProgress + (1f - photoVideoChangeColumnsProgress);

                        float size1 = (float) ((Math.ceil((getMeasuredWidth() / (float) mediaColumnsCount)) - AndroidUtilities.dpf2(2)) * scaleSize + AndroidUtilities.dpf2(2));
                        float fromSize = getMeasuredWidth() / (float) mediaColumnsCount;
                        float toSize = getMeasuredWidth() / (float) animateToColumnsCount;

                        for (int i = 0; i < drawingViews2.size(); i++) {
                            SharedVideoItemCell view = drawingViews2.get(i);
                            int fromColumn = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getViewAdapterPosition() % mediaColumnsCount;
                            int currentRow = (((GridLayoutManager.LayoutParams) view.getLayoutParams()).getViewAdapterPosition() - firstVisibleItemPosition) / mediaColumnsCount;

                            currentRow += rowsOffset;
                            int toColumn = fromColumn + columnsOffset;

                            canvas.save();
                            view.setImageScale(scaleSize, !photoVideoChangeColumnsAnimation);
                            canvas.translate(fromColumn * fromSize * (1f - photoVideoChangeColumnsProgress) + toSize * toColumn * photoVideoChangeColumnsProgress, minY + size1 * currentRow);
                            if (toColumn < animateToColumnsCount) {
                                canvas.saveLayerAlpha(0, 0, view.getMeasuredWidth() * scale, view.getMeasuredWidth() * scale, (int) ((1f - photoVideoChangeColumnsProgress) * 255), Canvas.ALL_SAVE_FLAG);
                                view.draw(canvas);
                                canvas.restore();
                            } else {
                                view.draw(canvas);
                            }
                            canvas.restore();
                        }

                        if (!drawingViews3.isEmpty()) {
                            canvas.saveLayerAlpha(0, 0, getMeasuredWidth(), getMeasuredHeight(), (int) (255 * photoVideoChangeColumnsProgress), Canvas.ALL_SAVE_FLAG);
                            for (int i = 0; i < drawingViews3.size(); i++) {
                                drawingViews3.get(i).drawCrossafadeImage(canvas);
                            }
                            canvas.restore();
                        }
                    }
                } else {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        int messageId = getMessageId(child);
                        float alpha = 1;
                        if (messageId != 0 && messageAlphaEnter.get(messageId, null) != null) {
                            alpha = messageAlphaEnter.get(messageId, 1f);
                        }
                        if (child instanceof SharedDocumentCell) {
                            SharedDocumentCell cell = (SharedDocumentCell) child;
                            cell.setEnterAnimationAlpha(alpha);
                        } else if (child instanceof SharedAudioCell) {
                            SharedAudioCell cell = (SharedAudioCell) child;
                            cell.setEnterAnimationAlpha(alpha);
                        }
                    }
                    super.dispatchDraw(canvas);
                }


                if (mediaPage.highlightAnimation) {
                    mediaPage.highlightProgress += 16f / 1500f;
                    if (mediaPage.highlightProgress >= 1) {
                        mediaPage.highlightProgress = 0;
                        mediaPage.highlightAnimation = false;
                        mediaPage.highlightMessageId = 0;
                    }
                    invalidate();
                }

            }

            @Override
            public boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (getAdapter() == photoVideoAdapter) {
                    if (photoVideoChangeColumnsAnimation && child instanceof SharedVideoItemCell) {
                        return true;
                    }
                }
                return super.drawChild(canvas, child, drawingTime);
            }

        };
        mediaPage.layoutManager = layoutManager;
        mediaPage.listView.setFastScrollEnabled(RecyclerListView.FastScroll.DATE_TYPE);
        mediaPage.listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        mediaPage.listView.setPinnedSectionOffsetY(-AndroidUtilities.dp(2));
        mediaPage.listView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        mediaPage.listView.setItemAnimator(null);
        mediaPage.listView.setClipToPadding(false);
        mediaPage.listView.setSectionsType(RecyclerListView.SECTIONS_TYPE_DATE);
        mediaPage.listView.setLayoutManager(layoutManager);
        mediaPage.addView(mediaPage.listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        mediaPage.animationSupportingListView = new BlurredRecyclerView(context);
        mediaPage.animationSupportingListView.setLayoutManager(mediaPage.animationSupportingLayoutManager = new GridLayoutManager(context, 3) {

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (photoVideoChangeColumnsAnimation) {
                    dy = 0;
                }
                return super.scrollVerticallyBy(dy, recycler, state);
            }
        });
        mediaPage.addView(mediaPage.animationSupportingListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        mediaPage.animationSupportingListView.setVisibility(View.GONE);


        mediaPage.listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = 0;
                outRect.top = 0;
                outRect.bottom = 0;
                outRect.right = 0;
            }
        });
        mediaPage.listView.setOnItemClickListener((view, position) -> {
            MessageObject messageObject = ((SharedVideoItemCell) view).getMessageObject();
            if (messageObject != null) {
                onItemClick(position, view, messageObject, 0, mediaPage.selectedType);
            }
        });
        mediaPage.listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkLoadMoreScroll(mediaPage, (RecyclerListView) recyclerView, layoutManager);
                if (dy != 0 && (mediaPage.selectedType == 0 && !sharedMediaData.messages.isEmpty())) {
                    showFloatingDateView();
                }
                if (dy != 0 && mediaPage.selectedType == 0) {
                    showFastScrollHint(mediaPage, sharedMediaData, true);
                }
                mediaPage.listView.checkSection(true);
                if (mediaPage.fastScrollHintView != null) {
                    mediaPage.invalidate();
                }
                invalidateBlur();
            }
        });
        mediaPage.listView.setOnItemLongClickListener((view, position) -> {
            if (photoVideoChangeColumnsAnimation) {
                return false;
            }
            if (actionBar.isActionModeShowed()) {
                mediaPage.listView.getOnItemClickListener().onItemClick(view, position);
                return true;
            }
            if (mediaPage.selectedType == 0 && view instanceof SharedVideoItemCell) {
                MessageObject messageObject = ((SharedVideoItemCell) view).getMessageObject();
                if (messageObject != null) {
                    return onItemLongClick(messageObject, view, 0);
                }
            }
            return false;
        });

        if ( scrollToPositionOnRecreate != -1) {
            layoutManager.scrollToPositionWithOffset(scrollToPositionOnRecreate, scrollToOffsetOnRecreate);
        }

        final RecyclerListView listView = mediaPage.listView;

        mediaPage.animatingImageView = new ClippingImageView(context) {
            @Override
            public void invalidate() {
                super.invalidate();
                listView.invalidate();
            }
        };
        mediaPage.animatingImageView.setVisibility(View.GONE);
        mediaPage.listView.addOverlayView(mediaPage.animatingImageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        mediaPage.progressView = new FlickerLoadingView(context) {

            @Override
            public int getColumnsCount() {
                return mediaColumnsCount;
            }

            @Override
            public int getViewType() {
                setIsSingleCell(false);
                return 2;
            }

            @Override
            protected void onDraw(Canvas canvas) {
                backgroundPaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
                super.onDraw(canvas);
            }
        };
        mediaPage.progressView.showDate(false);


        mediaPage.emptyView = new StickerEmptyView(context, mediaPage.progressView, StickerEmptyView.STICKER_TYPE_SEARCH);
        mediaPage.emptyView.setVisibility(View.GONE);
        mediaPage.emptyView.setAnimateLayoutChange(true);
        mediaPage.addView(mediaPage.emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        mediaPage.emptyView.setOnTouchListener((v, event) -> true);
        mediaPage.emptyView.showProgress(true, false);
        mediaPage.emptyView.title.setText(LocaleController.getString("NoResult", R.string.NoResult));
        mediaPage.emptyView.subtitle.setText(LocaleController.getString("SearchEmptyViewFilteredSubtitle2", R.string.SearchEmptyViewFilteredSubtitle2));
        mediaPage.emptyView.addView(mediaPage.progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        mediaPage.listView.setEmptyView(mediaPage.emptyView);
        mediaPage.listView.setAnimateEmptyView(true, 0);
        mediaPage.scrollHelper = new RecyclerAnimationScrollHelper(mediaPage.listView, mediaPage.layoutManager);


        floatingDateView = new ChatActionCell(context);
        floatingDateView.setCustomDate((int) (System.currentTimeMillis() / 1000), false, false);
        floatingDateView.setAlpha(0.0f);
        floatingDateView.setOverrideColor(Theme.key_chat_mediaTimeBackground, Theme.key_chat_mediaTimeText);
        floatingDateView.setTranslationY(-AndroidUtilities.dp(48));
        frameLayout.addView(floatingDateView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        switchToCurrentSelectedMode();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        scrolling = true;
        if (photoVideoAdapter != null) {
            photoVideoAdapter.notifyDataSetChanged();
        }
        fixLayoutInternal(0);
    }
    private void saveScrollPosition() {
        RecyclerListView listView = mediaPage.listView;
        if (listView != null) {
            int messageId = 0;
            int offset = 0;
            for (int i = 0; i < listView.getChildCount(); i++) {
                View child = listView.getChildAt(i);
                if (child instanceof SharedVideoItemCell) {
                    SharedVideoItemCell cell = (SharedVideoItemCell) child;
                    messageId = cell.getMessageId();
                    offset = cell.getTop();
                }
                if (messageId != 0) {
                    break;
                }
            }
            if (messageId != 0) {
                int index = -1;
                for (int i = 0; i < sharedMediaData.messages.size(); i++) {
                    if (messageId == sharedMediaData.messages.get(i).getId()) {
                        index = i;
                        break;
                    }
                }

                int position = sharedMediaData.startOffset + index;
                if (index >= 0) {
                    ((LinearLayoutManager) listView.getLayoutManager()).scrollToPositionWithOffset(position, -mediaPage.listView.getPaddingTop() + offset);
                    if (photoVideoChangeColumnsAnimation) {
                        mediaPage.animationSupportingLayoutManager.scrollToPositionWithOffset(position, -mediaPage.listView.getPaddingTop() + offset);
                    }
                }
            }
        }

    }

    SparseArray<Float> messageAlphaEnter = new SparseArray<>();

    private void animateItemsEnter(final RecyclerListView finalListView, int oldItemCount, SparseBooleanArray addedMesages) {
        int n = finalListView.getChildCount();
        View progressView = null;
        for (int i = 0; i < n; i++) {
            View child = finalListView.getChildAt(i);
            if (child instanceof FlickerLoadingView) {
                progressView = child;
            }
        }
        final View finalProgressView = progressView;
        if (progressView != null) {
            finalListView.removeView(progressView);
        }
        frameLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                frameLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                RecyclerView.Adapter adapter = finalListView.getAdapter();
                if (adapter == photoVideoAdapter ) {
                    if (addedMesages != null) {
                        int n = finalListView.getChildCount();
                        for (int i = 0; i < n; i++) {
                            View child = finalListView.getChildAt(i);
                            int messageId = getMessageId(child);
                            if (messageId != 0 && addedMesages.get(messageId, false)) {
                                messageAlphaEnter.put(messageId, 0f);
                                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
                                valueAnimator.addUpdateListener(valueAnimator1 -> {
                                    messageAlphaEnter.put(messageId, (Float) valueAnimator1.getAnimatedValue());
                                    finalListView.invalidate();
                                });
                                valueAnimator.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        messageAlphaEnter.remove(messageId);
                                        finalListView.invalidate();
                                    }
                                });
                                int s = Math.min(finalListView.getMeasuredHeight(), Math.max(0, child.getTop()));
                                int delay = (int) ((s / (float) finalListView.getMeasuredHeight()) * 100);
                                valueAnimator.setStartDelay(delay);
                                valueAnimator.setDuration(250);
                                valueAnimator.start();
                            }
                            finalListView.invalidate();
                        }
                    }
                } else {
                    int n = finalListView.getChildCount();
                    AnimatorSet animatorSet = new AnimatorSet();
                    for (int i = 0; i < n; i++) {
                        View child = finalListView.getChildAt(i);
                        if (child != finalProgressView && finalListView.getChildAdapterPosition(child) >= oldItemCount - 1) {
                            child.setAlpha(0);
                            int s = Math.min(finalListView.getMeasuredHeight(), Math.max(0, child.getTop()));
                            int delay = (int) ((s / (float) finalListView.getMeasuredHeight()) * 100);
                            ObjectAnimator a = ObjectAnimator.ofFloat(child, View.ALPHA, 0, 1f);
                            a.setStartDelay(delay);
                            a.setDuration(200);
                            animatorSet.playTogether(a);
                        }
                        if (finalProgressView != null && finalProgressView.getParent() == null) {
                            finalListView.addView(finalProgressView);
                            RecyclerView.LayoutManager layoutManager = finalListView.getLayoutManager();
                            if (layoutManager != null) {
                                layoutManager.ignoreView(finalProgressView);
                                Animator animator = ObjectAnimator.ofFloat(finalProgressView, View.ALPHA, finalProgressView.getAlpha(), 0);
                                animator.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        finalProgressView.setAlpha(1f);
                                        layoutManager.stopIgnoringView(finalProgressView);
                                        finalListView.removeView(finalProgressView);
                                    }
                                });
                                animator.start();
                            }
                        }
                    }
                    animatorSet.start();
                }
                return true;
            }
        });
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mediaPage.listView != null) {
            ViewTreeObserver obs = mediaPage.listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mediaPage.getViewTreeObserver().removeOnPreDrawListener(this);
                    fixLayoutInternal(0);
                    return true;
                }
            });
        }
    }

    public void updateAdapters() {
        if (photoVideoAdapter != null) {
            photoVideoAdapter.notifyDataSetChanged();
        }
    }

    private void updateRowsSelection() {
        int count = mediaPage.listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = mediaPage.listView.getChildAt(a);
            if (child instanceof SharedVideoItemCell) {
                ((SharedVideoItemCell) child).setChecked(false, true);
            }
        }
    }

    public void setMergeDialogId(long did) {
        mergeDialogId = did;
    }

    protected void invalidateBlur() {

    }
    private int getMessageId(View child) {
        if (child instanceof SharedVideoItemCell) {
            return ((SharedVideoItemCell) child).getMessageId();
        }
        return 0;
    }
    private void showFloatingDateView() {

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
                    ObjectAnimator.ofFloat(floatingDateView, View.TRANSLATION_Y, -AndroidUtilities.dp(48) + additionalFloatingTranslation));
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

    public static int getItemSize(int itemsCount) {
        final int itemWidth;
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
        }
        return itemWidth;
    }

    private void scrollToTop() {
        int   height = getItemSize(1);
        int scrollDistance;
        scrollDistance = mediaPage.layoutManager.findFirstVisibleItemPosition() / mediaColumnsCount * height;

    }
    Runnable jumpToRunnable;
    private void checkLoadMoreScroll(MediaPage mediaPage, RecyclerListView recyclerView, LinearLayoutManager layoutManager) {
        if (photoVideoChangeColumnsAnimation || jumpToRunnable != null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if ((recyclerView.getFastScroll() != null && recyclerView.getFastScroll().isPressed()) && (currentTime - mediaPage.lastCheckScrollTime) < 300) {
            return;
        }
        mediaPage.lastCheckScrollTime = currentTime;
        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
        int totalItemCount = recyclerView.getAdapter().getItemCount();
        if (mediaPage.selectedType == 0) {
            int type = mediaPage.selectedType;
            totalItemCount = sharedMediaData.getStartOffset() + sharedMediaData.messages.size();
            if (sharedMediaData.fastScrollDataLoaded && sharedMediaData.fastScrollPeriods.size() > 2 && mediaPage.selectedType == 0 && sharedMediaData.messages.size() != 0) {
                int columnsCount = 1;
                if (type == 0) {
                    columnsCount = mediaColumnsCount;
                }
                int jumpToTreshold = (int) ((recyclerView.getMeasuredHeight() / ((float) (recyclerView.getMeasuredWidth() / (float) columnsCount))) * columnsCount * 1.5f);
                if (jumpToTreshold < 100) {
                    jumpToTreshold = 100;
                }
                if (jumpToTreshold < sharedMediaData.fastScrollPeriods.get(1).startOffset) {
                    jumpToTreshold = sharedMediaData.fastScrollPeriods.get(1).startOffset;
                }
                if ((firstVisibleItem > totalItemCount && firstVisibleItem - totalItemCount > jumpToTreshold) || ((firstVisibleItem + visibleItemCount) < sharedMediaData.startOffset && sharedMediaData.startOffset - (firstVisibleItem + visibleItemCount) > jumpToTreshold)) {
                    AndroidUtilities.runOnUIThread(jumpToRunnable = () -> {
                        findPeriodAndJumpToDate(type, recyclerView, false);
                        jumpToRunnable = null;
                    });
                    return;
                }
            }
        }

        final int threshold = 3;

        if ((firstVisibleItem + visibleItemCount > totalItemCount - threshold || sharedMediaData.loadingAfterFastScroll) && !sharedMediaData.loading) {
            int type = MediaDataController.MEDIA_VIDEOS_ONLY;
            if (mediaPage.selectedType == 0) {
            }
            if (!sharedMediaData.endReached[0]) {
                sharedMediaData.loading = true;
                getMediaDataController().loadMedia(dialog_id, 50, sharedMediaData.max_id[0], 0, type, 1, getClassGuid(), sharedMediaData.requestIndex);
            } else if (mergeDialogId != 0 && !sharedMediaData.endReached[1]) {
                sharedMediaData.loading = true;
                getMediaDataController().loadMedia(mergeDialogId, 50, sharedMediaData.max_id[1], 0, type, 1, getClassGuid(), sharedMediaData.requestIndex);
            }
        }

        int startOffset = sharedMediaData.startOffset;
        if (mediaPage.selectedType == 0) {
            startOffset = photoVideoAdapter.getPositionForIndex(0);
        }
        if (firstVisibleItem - startOffset < threshold + 1 && !sharedMediaData.loading && !sharedMediaData.startReached && !sharedMediaData.loadingAfterFastScroll) {
            loadFromStart();
        }
        if (mediaPage.listView == recyclerView  && firstVisibleItem != RecyclerView.NO_POSITION) {
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
    private void loadFromStart() {
        int type = MediaDataController.MEDIA_VIDEOS_ONLY;;
        sharedMediaData.loading = true;
        getMediaDataController().loadMedia(dialog_id, 50, 0, sharedMediaData.min_id, type, 1, getClassGuid(), sharedMediaData.requestIndex);
    }


    private void createActionMode() {
        if (actionBar.actionModeIsExist(null)) {
            return;
        }
        final ActionBarMenu actionMode = actionBar.createActionMode(false, null);
        actionModeLayout = actionMode;
        actionMode.setBackgroundColor(Color.TRANSPARENT);
        actionModeLayout.drawBlur = true;

        selectedMessagesCountTextView = new NumberTextView(actionMode.getContext());
        selectedMessagesCountTextView.setTextSize(18);
        selectedMessagesCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedMessagesCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedMessagesCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
        selectedMessagesCountTextView.setOnTouchListener((v, event) -> true);

        gotoItem = actionMode.addItemWithWidth(gotochat, R.drawable.msg_message, AndroidUtilities.dp(54));
        forwardNoQuoteItem = actionMode.addItemWithWidth(forward_noquote, R.drawable.msg_forward_noquote, AndroidUtilities.dp(54));
        forwardNoQuoteItem.setVisibility(OwlConfig.showNoQuoteForward ? View.VISIBLE:View.GONE);
        forwardItem = actionMode.addItemWithWidth(forward, R.drawable.msg_forward, AndroidUtilities.dp(54));
        deleteItem = actionMode.addItemWithWidth(delete, R.drawable.msg_delete, AndroidUtilities.dp(54));

        actionModeViews.add(gotoItem);
        actionModeViews.add(forwardNoQuoteItem);
        actionModeViews.add(forwardItem);
        actionModeViews.add(deleteItem);
        //updateForwardItem();

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if(id == -1){
                    if(actionBar.isActionModeShowed()){
                        closeActionMode();
                    }else{
                        finishFragment();
                    }
                }else if(id == gotochat){
                    if (selectedFiles[0].size() + selectedFiles[1].size() != 1) {
                        return;
                    }
                    MessageObject messageObject = selectedFiles[selectedFiles[0].size() == 1 ? 0 : 1].valueAt(0);
                    Bundle args = new Bundle();
                    long dialogId = messageObject.getDialogId();
                    if (DialogObject.isEncryptedDialog(dialogId)) {
                        args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
                    } else if (DialogObject.isUserDialog(dialogId)) {
                        args.putLong("user_id", dialogId);
                    } else {
                        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                        if (chat != null && chat.migrated_to != null) {
                            args.putLong("migrated_to", dialogId);
                            dialogId = -chat.migrated_to.channel_id;
                        }
                        args.putLong("chat_id", -dialogId);
                    }
                    args.putInt("message_id", messageObject.getId());
                    args.putBoolean("need_remove_previous_same_chat_activity", false);
                    presentFragment(new ChatActivity(args), false);
                }else if(id == forward_noquote || id == forward){
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", 3);
                    DialogsActivity fragment = new DialogsActivity(args);
                    ArrayList<MessageObject> fmessages = new ArrayList<>();
                    for (int a = 1; a >= 0; a--) {
                        ArrayList<Integer> ids = new ArrayList<>();
                        for (int b = 0; b < selectedFiles[a].size(); b++) {
                            ids.add(selectedFiles[a].keyAt(b));
                        }
                        Collections.sort(ids);
                        for (Integer id1 : ids) {
                            if (id1 > 0) {
                                fmessages.add(selectedFiles[a].get(id1));
                            }
                        }
                    }
                    fragment.forwardContext = () -> fmessages;
                    ForwardContext.ForwardParams forwardParams = fragment.forwardContext.getForwardParams();
                    forwardParams.noQuote = id == forward_noquote;
                    fragment.setDelegate((fragment1, dids, message, param) -> {
                        for (int a = 1; a >= 0; a--) {
                            selectedFiles[a].clear();
                        }
                        cantDeleteMessagesCount = 0;
                        showActionMode(false);

                        if (dids.size() > 1 || dids.get(0) == getUserConfig().getClientUserId() || message != null) {
                            updateRowsSelection();
                            for (int a = 0; a < dids.size(); a++) {
                                long did = dids.get(a);
                                if (message != null) {
                                    getSendMessagesHelper().sendMessage(message.toString(), did, null, null, null, true, null, null, null, forwardParams.notify, forwardParams.scheduleDate, null,false);
                                }
                                getSendMessagesHelper().sendMessage(fmessages, did, forwardParams.noQuote, forwardParams.noCaption, forwardParams.notify, forwardParams.scheduleDate);
                            }
                            fragment1.finishFragment();
                            UndoView undoView = null;
                            if (undoView != null) {
                                if (dids.size() == 1) {
                                    undoView.showWithAction(dids.get(0), UndoView.ACTION_FWD_MESSAGES, fmessages.size());
                                } else {
                                    undoView.showWithAction(0, UndoView.ACTION_FWD_MESSAGES, fmessages.size(), dids.size(), null, null);
                                }
                            }
                        } else {
                            long did = dids.get(0);
                            Bundle args1 = new Bundle();
                            args1.putBoolean("forward_noQuote", forwardParams.noQuote);
                            args1.putBoolean("forward_noCaption", forwardParams.noCaption);
                            args1.putBoolean("scrollToTopOnResume", true);
                            if (DialogObject.isEncryptedDialog(did)) {
                                args1.putInt("enc_id", DialogObject.getEncryptedChatId(did));
                            } else {
                                if (DialogObject.isUserDialog(did)) {
                                    args1.putLong("user_id", did);
                                } else {
                                    args1.putLong("chat_id", -did);
                                }
                                if (!getMessagesController().checkCanOpenChat(args1, fragment1)) {
                                    return;
                                }
                            }

                            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);

                            ChatActivity chatActivity = new ChatActivity(args1);
                            fragment1.presentFragment(chatActivity, true);
                            chatActivity.showFieldPanelForForward(true, fmessages);
                        }
                    });
                    presentFragment(fragment);
                }else if(id == delete){
                    TLRPC.Chat currentChat = null;
                    TLRPC.User currentUser = null;
                    TLRPC.EncryptedChat currentEncryptedChat = null;
                    if (DialogObject.isEncryptedDialog(dialog_id)) {
                        currentEncryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialog_id));
                    } else if (DialogObject.isUserDialog(dialog_id)) {
                        currentUser = getMessagesController().getUser(dialog_id);
                    } else {
                        currentChat = getMessagesController().getChat(-dialog_id);
                    }
                    AlertsCreator.createDeleteMessagesAlert(VideoFragment.this, currentUser, currentChat, currentEncryptedChat, null, mergeDialogId, null, selectedFiles, null, false, 1, () -> {
                        showActionMode(false);
                        cantDeleteMessagesCount = 0;
                    }, null, getResourceProvider());
                }
            }

        });
    }

    private void switchToCurrentSelectedMode() {
        mediaPage.listView.stopScroll();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mediaPage.getLayoutParams();
        // layoutParams.leftMargin = layoutParams.rightMargin = 0;
        boolean fastScrollVisible = false;
        int spanCount = 100;
        RecyclerView.Adapter currentAdapter = mediaPage.listView.getAdapter();
        RecyclerView.RecycledViewPool viewPool = null;
        mediaPage.listView.setPinnedHeaderShadowDrawable(null);
        if (currentAdapter != photoVideoAdapter) {
            recycleAdapter(currentAdapter);
            mediaPage.listView.setAdapter(photoVideoAdapter);
        }
        layoutParams.leftMargin = layoutParams.rightMargin = -AndroidUtilities.dp(1);
        if (sharedMediaData.fastScrollDataLoaded && !sharedMediaData.fastScrollPeriods.isEmpty()) {
            fastScrollVisible = true;
        }
        spanCount = mediaColumnsCount;
        mediaPage.listView.setPinnedHeaderShadowDrawable(pinnedHeaderShadowDrawable);
        if (sharedMediaData.recycledViewPool == null) {
            sharedMediaData.recycledViewPool = new RecyclerView.RecycledViewPool();
        }
        viewPool = sharedMediaData.recycledViewPool;

        if (!sharedMediaData.loading && !sharedMediaData.endReached[0] && sharedMediaData.messages.isEmpty()) {
            sharedMediaData.loading = true;
            int type  = MediaDataController.MEDIA_VIDEOS_ONLY;
            getMediaDataController().loadMedia(dialog_id, 50, 0, 0, type, 1,classGuid, sharedMediaData.requestIndex);
        }
        mediaPage.listView.setVisibility(View.VISIBLE);
        mediaPage.fastScrollEnabled = fastScrollVisible;
        updateFastScrollVisibility(mediaPage, false);
        mediaPage.layoutManager.setSpanCount(spanCount);
        mediaPage.listView.setRecycledViewPool(viewPool);
        mediaPage.animationSupportingListView.setRecycledViewPool(viewPool);

    }

    private void onItemClick(int index, View view, MessageObject message, int a, int selectedMode) {
        if (message == null || photoVideoChangeColumnsAnimation) {
            return;
        }
        if (actionBar.isActionModeShowed()) {
            int loadIndex = message.getDialogId() == dialog_id ? 0 : 1;
            if (selectedFiles[loadIndex].indexOfKey(message.getId()) >= 0) {
                selectedFiles[loadIndex].remove(message.getId());
                if (!message.canDeleteMessage(false, null)) {
                    cantDeleteMessagesCount--;
                }
            } else {
                if (selectedFiles[0].size() + selectedFiles[1].size() >= 100) {
                    return;
                }
                selectedFiles[loadIndex].put(message.getId(), message);
                if (!message.canDeleteMessage(false, null)) {
                    cantDeleteMessagesCount++;
                }
            }
            if (selectedFiles[0].size() == 0 && selectedFiles[1].size() == 0) {
                showActionMode(false);
            } else {
                selectedMessagesCountTextView.setNumber(selectedFiles[0].size() + selectedFiles[1].size(), true);
                deleteItem.setVisibility(cantDeleteMessagesCount == 0 ? View.VISIBLE : View.GONE);
                if (gotoItem != null) {
                    gotoItem.setVisibility(selectedFiles[0].size() == 1 ? View.VISIBLE : View.GONE);
                }
            }
            scrolling = false;
            if (view instanceof SharedPhotoVideoCell) {
                ((SharedPhotoVideoCell) view).setChecked(a, selectedFiles[loadIndex].indexOfKey(message.getId()) >= 0, true);
            } else if (view instanceof SharedVideoItemCell) {
                ((SharedVideoItemCell) view).setChecked(selectedFiles[loadIndex].indexOfKey(message.getId()) >= 0, true);
            }
        } else {
            if (selectedMode == 0) {
                int i = index - sharedMediaData.startOffset;
                if (i >= 0 && i < sharedMediaData.messages.size()) {
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhoto(sharedMediaData.messages, i, dialog_id, mergeDialogId, provider);
                }
            } else if (selectedMode == 2 || selectedMode == 4) {
                if (view instanceof SharedAudioCell) {
                    ((SharedAudioCell) view).didPressedButton();
                }
            }
        }
        updateForwardItem();
    }

    private boolean onItemLongClick(MessageObject item, View view, int a) {
        if (actionBar.isActionModeShowed() || getParentActivity() == null || item == null) {
            return false;
        }
        selectedFiles[item.getDialogId() == dialog_id ? 0 : 1].put(item.getId(), item);
        if (!item.canDeleteMessage(false, null)) {
            cantDeleteMessagesCount++;
        }
        deleteItem.setVisibility(cantDeleteMessagesCount == 0 ? View.VISIBLE : View.GONE);
        if (gotoItem != null) {
            gotoItem.setVisibility(View.VISIBLE);
        }
        selectedMessagesCountTextView.setNumber(1, false);
        AnimatorSet animatorSet = new AnimatorSet();
        ArrayList<Animator> animators = new ArrayList<>();
        for (int i = 0; i < actionModeViews.size(); i++) {
            View view2 = actionModeViews.get(i);
            AndroidUtilities.clearDrawableAnimation(view2);
            animators.add(ObjectAnimator.ofFloat(view2, View.SCALE_Y, 0.1f, 1.0f));
        }
        animatorSet.playTogether(animators);
        animatorSet.setDuration(250);
        animatorSet.start();
        scrolling = false;
        if (view instanceof SharedPhotoVideoCell) {
            ((SharedPhotoVideoCell) view).setChecked(a, true, true);
        }  if (view instanceof SharedVideoItemCell) {
            ((SharedVideoItemCell) view).setChecked(true, true);
        }
        if (!actionBar.isActionModeShowed()) {
            showActionMode(true);
        }
        updateForwardItem();
        return true;
    }

    private void showActionMode(boolean show) {
        if (actionBar.isActionModeShowed() == show) {
            return;
        }
        if(show){
            if (backDrawable != null) {
                backDrawable.setRotation(1, true);
            }
            actionBar.showActionMode();
        }else{
            if (backDrawable != null) {
                backDrawable.setRotation(0, true);
            }
            actionBar.hideActionMode();
        }
    }

    public boolean closeActionMode() {
        if (actionBar.isActionModeShowed()) {
            for (int a = 1; a >= 0; a--) {
                selectedFiles[a].clear();
            }
            cantDeleteMessagesCount = 0;
            showActionMode(false);
            updateRowsSelection();
            return true;
        } else {
            return false;
        }
    }



    private void recycleAdapter(RecyclerView.Adapter adapter) {
        if (adapter instanceof SharedPhotoVideoAdapter) {
            cellCache.addAll(cache);
            cache.clear();
        }
    }

    private void fixLayoutInternal(int num) {
        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();
        if (num == 0) {
            if (!AndroidUtilities.isTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                selectedMessagesCountTextView.setTextSize(18);
            } else {
                selectedMessagesCountTextView.setTextSize(20);
            }
        }
        if (num == 0) {
            photoVideoAdapter.notifyDataSetChanged();
        }
    }


    public static View createEmptyStubView(Context context, int currentType, long dialog_id, Theme.ResourcesProvider resourcesProvider) {
        EmptyStubView emptyStubView = new EmptyStubView(context, resourcesProvider);
        if (DialogObject.isEncryptedDialog(dialog_id)) {
            emptyStubView.emptyTextView.setText(LocaleController.getString("NoMediaSecret", R.string.NoMediaSecret));
        } else {
            emptyStubView.emptyTextView.setText(LocaleController.getString("NoMedia", R.string.NoMedia));
        }
        return emptyStubView;
    }

    private static class EmptyStubView extends LinearLayout {

        final TextView emptyTextView;
        final ImageView emptyImageView;

        boolean ignoreRequestLayout;

        public EmptyStubView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            emptyTextView = new TextView(context);
            emptyImageView = new ImageView(context);

            setOrientation(LinearLayout.VERTICAL);
            setGravity(Gravity.CENTER);

            addView(emptyImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            emptyTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
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

    private void updateForwardItem() {
        if (forwardItem == null) {
            return;
        }
        boolean noforwards = getMessagesController().isChatNoForwards(-dialog_id) || hasNoforwardsMessage();
        forwardItem.setAlpha(noforwards ? 0.5f : 1f);
        forwardNoQuoteItem.setAlpha(noforwards ? 0.5f : 1f);
        if (noforwards && forwardItem.getBackground() != null) {
            forwardItem.setBackground(null);
            forwardNoQuoteItem.setBackground(null);
        } else if (!noforwards && forwardItem.getBackground() == null) {
            forwardItem.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_actionBarActionModeDefaultSelector), 5));
            forwardNoQuoteItem.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_actionBarActionModeDefaultSelector), 5));
        }
    }
    private boolean hasNoforwardsMessage() {
        boolean hasNoforwardsMessage = false;
        for (int a = 1; a >= 0; a--) {
            ArrayList<Integer> ids = new ArrayList<>();
            for (int b = 0; b < selectedFiles[a].size(); b++) {
                ids.add(selectedFiles[a].keyAt(b));
            }
            for (Integer id1 : ids) {
                if (id1 > 0) {
                    MessageObject msg = selectedFiles[a].get(id1);
                    if (msg != null && msg.messageOwner != null && msg.messageOwner.noforwards) {
                        hasNoforwardsMessage = true;
                        break;
                    }
                }
            }
            if (hasNoforwardsMessage)
                break;
        }
        return hasNoforwardsMessage;
    }
    private boolean fillMediaData() {
        SharedMediaData mediaData = sharedMediaPreloader.getSharedMediaData();
        if (mediaData == null) {
            return false;
        }
        if (!sharedMediaData.fastScrollDataLoaded) {
            sharedMediaData.totalCount = mediaData.totalCount;
        }
        sharedMediaData.messages.addAll(mediaData.messages);

        sharedMediaData.sections.addAll(mediaData.sections);
        for (HashMap.Entry<String, ArrayList<MessageObject>> entry : mediaData.sectionArrays.entrySet()) {
            sharedMediaData.sectionArrays.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        for (int i = 0; i < 2; i++) {
            sharedMediaData.messagesDict[i] = mediaData.messagesDict[i].clone();
            sharedMediaData.max_id[i] = mediaData.max_id[i];
            sharedMediaData.endReached[i] = mediaData.endReached[i];
        }
        sharedMediaData.fastScrollPeriods.addAll(mediaData.fastScrollPeriods);
        return !mediaData.messages.isEmpty();
    }

    private class SharedPhotoVideoAdapter extends RecyclerListView.FastScrollAdapter {

        private Context mContext;
        private boolean inFastScrollMode;
        SharedPhotoVideoCell3.SharedResources sharedResources;

        public SharedPhotoVideoAdapter(Context context) {
            mContext = context;
        }

        public int getPositionForIndex(int i) {
            return sharedMediaData.startOffset + i;
        }

        @Override
        public int getItemCount() {
            if (DialogObject.isEncryptedDialog(dialog_id)) {
                if (sharedMediaData.messages.size() == 0 && !sharedMediaData.loading) {
                    return 1;
                }
                if (sharedMediaData.messages.size() == 0 && (!sharedMediaData.endReached[0] || !sharedMediaData.endReached[1])) {
                    return 0;
                }
                int count = sharedMediaData.getStartOffset() + sharedMediaData.getMessages().size();
                if (count != 0 && (!sharedMediaData.endReached[0] || !sharedMediaData.endReached[1])) {
                    count++;
                }
                return count;
            }
            if (sharedMediaData.loadingAfterFastScroll) {
                return sharedMediaData.totalCount;
            }
            if (sharedMediaData.messages.size() == 0 && !sharedMediaData.loading) {
                return 1;
            }
            if (sharedMediaData.messages.size() == 0 && (!sharedMediaData.endReached[0] || !sharedMediaData.endReached[1]) && sharedMediaData.startReached) {
                return 0;
            }
            if (sharedMediaData.totalCount == 0) {
                int count = sharedMediaData.getStartOffset() + sharedMediaData.getMessages().size();
                if (count != 0 && (!sharedMediaData.endReached[0] || !sharedMediaData.endReached[1])) {
                    if (sharedMediaData.getEndLoadingStubs() != 0) {
                        count += sharedMediaData.getEndLoadingStubs();
                    } else {
                        count++;
                    }
                }
                return count;
            } else {
                return sharedMediaData.totalCount;
            }
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
                    if (sharedResources == null) {
                        sharedResources = new SharedPhotoVideoCell3.SharedResources(parent.getContext(), null);
                    }
                    view = new SharedVideoItemCell(mContext, sharedResources, getCurrentAccount());
                    SharedVideoItemCell cell = (SharedVideoItemCell) view;
                    cell.setGradientView(globalGradientView);
                    break;
                default:
                case 2:
                    View emptyStubView = createEmptyStubView(mContext, 0, dialog_id, null);
                    emptyStubView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    return new RecyclerListView.Holder(emptyStubView);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                ArrayList<MessageObject> messageObjects = sharedMediaData.getMessages();
                int index = position - sharedMediaData.getStartOffset();
                SharedVideoItemCell cell = (SharedVideoItemCell) holder.itemView;
                int oldMessageId = cell.getMessageId();
                int parentCount = this == photoVideoAdapter ? mediaColumnsCount : animateToColumnsCount;
                if (index >= 0 && index < messageObjects.size()) {
                    MessageObject messageObject = messageObjects.get(index);
                    boolean animated = messageObject.getId() == oldMessageId;

                    if (actionBar.isActionModeShowed()) {
                        cell.setChecked(selectedFiles[messageObject.getDialogId() == dialog_id ? 0 : 1].indexOfKey(messageObject.getId()) >= 0, animated);
                    } else {
                        cell.setChecked(false, animated);
                    }
                    cell.setMessageObject(messageObject, parentCount);
                } else {
                    cell.setMessageObject(null, parentCount);
                    cell.setChecked(false, false);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (!inFastScrollMode && sharedMediaData.getMessages().size() == 0 && !sharedMediaData.loading && sharedMediaData.startReached) {
                return 2;
            }
            int count = sharedMediaData.getStartOffset() + sharedMediaData.getMessages().size();
            if (position - sharedMediaData.getStartOffset() >= 0 && position < count) {
                return 0;
            }
            return 0;
        }

        @Override
        public String getLetter(int position) {
            if (sharedMediaData.fastScrollPeriods == null) {
                return "";
            }
            int index = position;
            ArrayList<Period> periods = sharedMediaData.fastScrollPeriods;
            if (!periods.isEmpty()) {
                for (int i = 0; i < periods.size(); i++) {
                    if (index <= periods.get(i).startOffset) {
                        return periods.get(i).formatedDate;
                    }
                }
                return periods.get(periods.size() - 1).formatedDate;
            }
            return "";
        }

        @Override
        public void getPositionForScrollProgress(RecyclerListView listView, float progress, int[] position) {
            int viewHeight = listView.getChildAt(0).getMeasuredHeight();
            int totalHeight = (int) (Math.ceil(getTotalItemsCount() / (float) mediaColumnsCount) * viewHeight);
            int listHeight =  listView.getMeasuredHeight() - listView.getPaddingTop();
            position[0] = (int) ((progress * (totalHeight -listHeight)) / viewHeight) * mediaColumnsCount;
            position[1] = (int) (progress * (totalHeight - listHeight)) % viewHeight;
        }

        @Override
        public void onStartFastScroll() {
            inFastScrollMode = true;
            MediaPage mediaPage = getMediaPage();
            if (mediaPage != null) {
                showFastScrollHint(mediaPage, null, false);
            }
        }

        @Override
        public void onFinishFastScroll(RecyclerListView listView) {
            if (inFastScrollMode) {
                inFastScrollMode = false;
                if (listView != null) {
                    int messageId = 0;
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        View child = listView.getChildAt(i);
                        if (child instanceof SharedVideoItemCell) {
                            SharedVideoItemCell cell = (SharedVideoItemCell) child;
                            messageId = cell.getMessageId();
                        }
                        if (messageId != 0) {
                            break;
                        }
                    }
                    if (messageId == 0) {
                        findPeriodAndJumpToDate(0, listView, true);
                    }
                }
            }
        }

        @Override
        public int getTotalItemsCount() {
            return sharedMediaData.totalCount;
        }

        @Override
        public float getScrollProgress(RecyclerListView listView) {
            int parentCount = this == photoVideoAdapter ? mediaColumnsCount : animateToColumnsCount;
            int cellCount = (int) Math.ceil(getTotalItemsCount() / (float) parentCount);
            if (listView.getChildCount() == 0) {
                return 0;
            }
            int cellHeight = listView.getChildAt(0).getMeasuredHeight();
            View firstChild = listView.getChildAt(0);
            int firstPosition = listView.getChildAdapterPosition(firstChild);
            if (firstPosition < 0) {
                return 0;
            }
            float childTop = firstChild.getTop() - listView.getPaddingTop();
            float listH = listView.getMeasuredHeight() - listView.getPaddingTop();
            float scrollY = (firstPosition / parentCount) * cellHeight - childTop;
            return scrollY / (((float) cellCount) * cellHeight - listH);
        }

        public boolean fastScrollIsVisible(RecyclerListView listView) {
            int parentCount = this == photoVideoAdapter ? mediaColumnsCount : animateToColumnsCount;
            int cellCount = (int) Math.ceil(getTotalItemsCount() / (float) parentCount);
            if (listView.getChildCount() == 0) {
                return false;
            }
            int cellHeight = listView.getChildAt(0).getMeasuredHeight();
            return cellCount * cellHeight > listView.getMeasuredHeight();
        }

        @Override
        public void onFastScrollSingleTap() {
//            showMediaCalendar(true);
        }
    }

    private MediaPage getMediaPage() {
        return mediaPage;
    }

    private void findPeriodAndJumpToDate(int type, RecyclerListView listView, boolean scrollToPosition) {
        ArrayList<Period> periods = sharedMediaData.fastScrollPeriods;
        Period period = null;
        int position = ((LinearLayoutManager) listView.getLayoutManager()).findFirstVisibleItemPosition();
        if (position >= 0) {
            if (periods != null) {
                for (int i = 0; i < periods.size(); i++) {
                    if (position <= periods.get(i).startOffset) {
                        period = periods.get(i);
                        break;
                    }
                }
                if (period == null) {
                    period = periods.get(periods.size() - 1);
                }
            }
            if (period != null) {
                jumpToDate(type, period.maxId, period.startOffset + 1, scrollToPosition);
                return;
            }
        }
    }

    private void jumpToDate(int type, int messageId, int startOffset, boolean scrollToPosition) {
        sharedMediaData.messages.clear();
        sharedMediaData.messagesDict[0].clear();
        sharedMediaData.messagesDict[1].clear();
        sharedMediaData.setMaxId(0, messageId);
        sharedMediaData.setEndReached(0, false);
        sharedMediaData.startReached = false;
        sharedMediaData.startOffset = startOffset;
        sharedMediaData.endLoadingStubs = sharedMediaData.totalCount - startOffset - 1;
        if (sharedMediaData.endLoadingStubs < 0) {
            sharedMediaData.endLoadingStubs = 0;
        }
        sharedMediaData.min_id = messageId;
        sharedMediaData.loadingAfterFastScroll = true;
        sharedMediaData.loading = false;
        sharedMediaData.requestIndex++;
        MediaPage mediaPage = getMediaPage();
        if (mediaPage != null && mediaPage.listView.getAdapter() != null) {
            mediaPage.listView.getAdapter().notifyDataSetChanged();
        }
        if (scrollToPosition) {
            mediaPage.layoutManager.scrollToPositionWithOffset(Math.min(sharedMediaData.totalCount - 1, sharedMediaData.startOffset), 0);
        }
    }
    public void setNewMediaCounts(int[] mediaCounts) {
        boolean hadMedia = false;
        for (int a = 0; a < 6; a++) {
            if (hasMedia >= 0) {
                hadMedia = true;
                break;
            }
        }
        System.arraycopy(mediaCounts, 0, hasMedia, 0, 6);
        if (hasMedia >= 0) {
            loadFastScrollData(false);
        }
    }


    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();

        arrayList.add(new ThemeDescription(selectedMessagesCountTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));


        arrayList.add(new ThemeDescription(deleteItem.getIconView(), ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        arrayList.add(new ThemeDescription(deleteItem, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));
        if (gotoItem != null) {
            arrayList.add(new ThemeDescription(gotoItem.getIconView(), ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
            arrayList.add(new ThemeDescription(gotoItem, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));
        }
        if (forwardNoQuoteItem != null) {
            arrayList.add(new ThemeDescription(forwardNoQuoteItem.getIconView(), ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
            arrayList.add(new ThemeDescription(forwardNoQuoteItem, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));
        }
        if (forwardItem != null) {
            arrayList.add(new ThemeDescription(forwardItem.getIconView(), ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
            arrayList.add(new ThemeDescription(forwardItem, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));
        }
        arrayList.add(new ThemeDescription(closeButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, new Drawable[]{backDrawable}, null, Theme.key_windowBackgroundWhiteGrayText2));
        arrayList.add(new ThemeDescription(closeButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));


        arrayList.add(new ThemeDescription(floatingDateView, 0, null, null, null, null, Theme.key_chat_mediaTimeBackground));
        arrayList.add(new ThemeDescription(floatingDateView, 0, null, null, null, null, Theme.key_chat_mediaTimeText));

        for (int a = 0; a < 1; a++) {
            final int num = a;
            ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
                if (mediaPage.listView != null) {
                    int count = mediaPage.listView.getChildCount();
                    for (int a1 = 0; a1 < count; a1++) {
                        View child = mediaPage.listView.getChildAt(a1);
                        if (child instanceof SharedPhotoVideoCell) {
                            ((SharedPhotoVideoCell) child).updateCheckboxColor();
                        }
                    }
                }
            };

            arrayList.add(new ThemeDescription(mediaPage.listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

            arrayList.add(new ThemeDescription(mediaPage.progressView, 0, null, null, null, null, Theme.key_windowBackgroundWhite));
            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
            arrayList.add(new ThemeDescription(mediaPage.emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));

            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_SECTIONS, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_graySectionText));
            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_SECTIONS, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection));

            arrayList.add(new ThemeDescription(mediaPage.listView, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle));


            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{EmptyStubView.class}, new String[]{"emptyTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

            arrayList.add(new ThemeDescription(mediaPage.listView, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle));

            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_SECTIONS, new Class[]{SharedMediaSectionCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_SECTIONS, new Class[]{SharedMediaSectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPage.listView, 0, new Class[]{SharedMediaSectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));

            arrayList.add(new ThemeDescription(mediaPage.listView, 0, new Class[]{SharedPhotoVideoCell.class}, new String[]{"backgroundPaint"}, null, null, null, Theme.key_sharedMedia_photoPlaceholder));
            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{SharedPhotoVideoCell.class}, null, null, cellDelegate, Theme.key_checkbox));
            arrayList.add(new ThemeDescription(mediaPage.listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{SharedPhotoVideoCell.class}, null, null, cellDelegate, Theme.key_checkboxCheck));


            arrayList.add(new ThemeDescription(mediaPage.listView, 0, null, null, new Drawable[]{pinnedHeaderShadowDrawable}, null, Theme.key_windowBackgroundGrayShadow));

            arrayList.add(new ThemeDescription(mediaPage.emptyView.title, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(mediaPage.emptyView.subtitle, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText));
        }

        return arrayList;
    }

}
