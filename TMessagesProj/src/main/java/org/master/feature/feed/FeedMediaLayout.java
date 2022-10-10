package org.master.feature.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.Components.ClippingImageView;
import org.telegram.ui.Components.ExtendedGridLayoutManager;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
import org.telegram.ui.Components.SharedMediaFastScrollTooltip;
import org.telegram.ui.Components.Size;
import org.telegram.ui.Components.StickerEmptyView;

import java.util.ArrayList;

public class FeedMediaLayout extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {


    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if(id == NotificationCenter.didNewsLoaded){
            String key = (String) args[0];
            ArrayList<MessageObject> messageObjects = (ArrayList<MessageObject>) args[1];

             int index = 0;
             for(int a = 0; a < FeedController.categoryData.length; a++){
                if(FeedController.categoryData[a].equals(key)){
                    index = a;
                    break;
                }
            }
            newsData[index].messageObjects = messageObjects;
            if(newsAdapter != null){
                newsAdapter.notifyDataSetChanged();
            }
            Log.i("dataIos","recived:  " + messageObjects.size());
        }
    }

    private static class MediaPage extends FrameLayout {
        public long lastCheckScrollTime;
        public boolean fastScrollEnabled;
        public ObjectAnimator fastScrollAnimator;
        private RecyclerListView listView;
        private RecyclerListView animationSupportingListView;
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
        }

    }


    public static class NewsData{
        public ArrayList<MessageObject> messageObjects  = new ArrayList<>();
        public int totalCount;

    }
    private NewsData[] newsData= new NewsData[FeedController.categoryData.length];

    private BaseFragment profileActivity;
    private ActionBar actionBar;

    private int startedTrackingPointerId;
    private boolean startedTracking;
    private boolean maybeStartTracking;
    private int startedTrackingX;
    private int startedTrackingY;
    private VelocityTracker velocityTracker;

    private AnimatorSet tabsAnimation;
    private boolean tabsAnimationInProgress;
    private boolean animatingForward;
    private boolean backAnimation;

    private Drawable pinnedHeaderShadowDrawable;
    private MediaPage[] mediaPages = new MediaPage[2];

    private int maximumVelocity;

    private Paint backgroundPaint = new Paint();


    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };

    private int initialTab;

    private FragmentContextView fragmentContextView;
    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;
    private View shadowLine;

    private NewsAdapter newsAdapter;


    public FeedMediaLayout(Context context,BaseFragment baseFragment) {
        super(context);
        profileActivity = baseFragment;
        actionBar = profileActivity.getActionBar();

        for(int a = 0; a < FeedController.categoryData.length;a++){
            newsData[a] = new NewsData();
        }

        NotificationCenter.getInstance(profileActivity.getCurrentAccount()).addObserver(this,NotificationCenter.didNewsLoaded);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();

        pinnedHeaderShadowDrawable = context.getResources().getDrawable(R.drawable.photos_header_shadow);
        pinnedHeaderShadowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundGrayShadow), PorterDuff.Mode.MULTIPLY));

        if (scrollSlidingTextTabStrip != null) {
            initialTab = scrollSlidingTextTabStrip.getCurrentTabId();
        }
        scrollSlidingTextTabStrip = createScrollingTextTabStrip(context);


        int scrollToPositionOnRecreate = -1;
        int scrollToOffsetOnRecreate = 0;

        newsAdapter = new NewsAdapter(context,0);

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
                        }
                    }
                }
            };
            addView(mediaPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 48, 0, 0));
            mediaPages[a] = mediaPage;

            final ExtendedGridLayoutManager layoutManager = mediaPages[a].layoutManager = new ExtendedGridLayoutManager(context, 100) {

                private Size size = new Size();

                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }

                @Override
                protected void calculateExtraLayoutSpace(RecyclerView.State state, int[] extraLayoutSpace) {
                    super.calculateExtraLayoutSpace(state, extraLayoutSpace);
                    if (mediaPage.selectedType == 0) {
                        extraLayoutSpace[1] = Math.max(extraLayoutSpace[1], SharedPhotoVideoCell.getItemSize(1) * 2);
                    } else if (mediaPage.selectedType == 1) {
                        extraLayoutSpace[1] = Math.max(extraLayoutSpace[1], AndroidUtilities.dp(56f) * 2);
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

            mediaPages[a].listView = new RecyclerListView(context);
            mediaPages[a].listView.setFastScrollEnabled(RecyclerListView.FastScroll.DATE_TYPE);
            mediaPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
            mediaPages[a].listView.setPinnedSectionOffsetY(-AndroidUtilities.dp(2));
            mediaPages[a].listView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            mediaPages[a].listView.setItemAnimator(null);
            mediaPages[a].listView.setClipToPadding(false);
            mediaPages[a].listView.setSectionsType(2);
            mediaPages[a].listView.setLayoutManager(layoutManager);
            mediaPages[a].listView.setAdapter(newsAdapter);
            mediaPages[a].addView(mediaPages[a].listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a].animationSupportingListView = new RecyclerListView(context);
            mediaPages[a].animationSupportingListView.setLayoutManager(mediaPages[a].animationSupportingLayoutManager = new GridLayoutManager(context, 3) {

                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }

                @Override
                public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                    return super.scrollVerticallyBy(dy, recycler, state);
                }
            });
            mediaPages[a].addView(mediaPages[a].animationSupportingListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a].animationSupportingListView.setVisibility(View.GONE);

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

            mediaPages[a].progressView = new FlickerLoadingView(context) {

                @Override
                public int getColumnsCount() {
                    return 1;
                }

                @Override
                public int getViewType() {
                    setIsSingleCell(false);
                    if (mediaPage.selectedType == 0 || mediaPage.selectedType == 5) {
                        return 2;
                    } else if (mediaPage.selectedType == 1) {
                        return 3;
                    } else if (mediaPage.selectedType == 2 || mediaPage.selectedType == 4) {
                        return 4;
                    } else if (mediaPage.selectedType == 3) {
                        return 5;
                    } else if (mediaPage.selectedType == 7) {
                        return FlickerLoadingView.USERS_TYPE;
                    } else if (mediaPage.selectedType == 6) {
                        if (scrollSlidingTextTabStrip.getTabsCount() == 1) {
                            setIsSingleCell(true);
                        }
                        return 1;
                    }
                    return 1;
                }

                @Override
                protected void onDraw(Canvas canvas) {
                    backgroundPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
                    super.onDraw(canvas);
                }
            };
            mediaPages[a].progressView.showDate(false);
            if (a != 0) {
                mediaPages[a].setVisibility(View.GONE);
            }

            mediaPages[a].emptyView = new StickerEmptyView(context, mediaPages[a].progressView, StickerEmptyView.STICKER_TYPE_SEARCH);
            mediaPages[a].emptyView.setVisibility(View.GONE);
            mediaPages[a].emptyView.setAnimateLayoutChange(true);
            mediaPages[a].addView(mediaPages[a].emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a].emptyView.setOnTouchListener((v, event) -> true);
            mediaPages[a].emptyView.showProgress(true, false);
            mediaPages[a].emptyView.title.setText(LocaleController.getString("NoResult", R.string.NoResult));
            mediaPages[a].emptyView.subtitle.setText(LocaleController.getString("SearchEmptyViewFilteredSubtitle2", R.string.SearchEmptyViewFilteredSubtitle2));
            mediaPages[a].emptyView.addView(mediaPages[a].progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            mediaPages[a].listView.setEmptyView(mediaPages[a].emptyView);
            mediaPages[a].listView.setAnimateEmptyView(true, 0);

            mediaPages[a].scrollHelper = new RecyclerAnimationScrollHelper(mediaPages[a].listView, mediaPages[a].layoutManager);
        }

        addView(fragmentContextView = new FragmentContextView(context, baseFragment, this, false, null), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, 48, 0, 0));
        fragmentContextView.setDelegate((start, show) -> {
            if (!start) {
                requestLayout();
            }
        });
        addView(scrollSlidingTextTabStrip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.TOP));

        shadowLine = new View(context);
        shadowLine.setBackgroundColor(Theme.getColor(Theme.key_divider));
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        layoutParams.topMargin = AndroidUtilities.dp(48) - 1;
        addView(shadowLine, layoutParams);

        updateTabs(false);
        switchToCurrentSelectedMode(false);

    }
    public boolean isSwipeBackEnabled() {
        return !tabsAnimationInProgress;
    }



    private void updateTabs(boolean animated) {
        if (scrollSlidingTextTabStrip == null) {
            return;
        }
        for(int a = 0; a < FeedController.categoryData.length;a++){
            scrollSlidingTextTabStrip.addTextTab(a,FeedController.categoryData[a]);
        }

        int id = scrollSlidingTextTabStrip.getCurrentTabId();
        if (id >= 0) {
            mediaPages[0].selectedType = id;
        }
        scrollSlidingTextTabStrip.finishAddingTabs();
    }
      private ScrollSlidingTextTabStrip createScrollingTextTabStrip(Context context) {
        ScrollSlidingTextTabStrip scrollSlidingTextTabStrip = new ScrollSlidingTextTabStrip(context);
        if (initialTab != -1) {
            scrollSlidingTextTabStrip.setInitialTabId(initialTab);
            initialTab = -1;
        }
        scrollSlidingTextTabStrip.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        scrollSlidingTextTabStrip.setColors(Theme.key_profile_tabSelectedLine, Theme.key_profile_tabSelectedText, Theme.key_profile_tabText, Theme.key_profile_tabSelector);
        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
            @Override
            public void onPageSelected(int id, boolean forward) {
                if (mediaPages[0].selectedType == id) {
                    return;
                }
                mediaPages[1].selectedType = id;
                mediaPages[1].setVisibility(View.VISIBLE);
                switchToCurrentSelectedMode(true);
                animatingForward = forward;
                onSelectedTabChanged();
            }

            @Override
            public void onSamePageSelected() {
                scrollToTop();
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


                if (progress == 1) {
                    MediaPage tempPage = mediaPages[0];
                    mediaPages[0] = mediaPages[1];
                    mediaPages[1] = tempPage;
                    mediaPages[1].setVisibility(View.GONE);
                }
            }
        });
        return scrollSlidingTextTabStrip;
    }


    private void switchToCurrentSelectedMode(boolean animated) {
        for (int a = 0; a < mediaPages.length; a++) {
            mediaPages[a].listView.stopScroll();
        }
        int a = animated ? 1 : 0;
        LayoutParams layoutParams = (LayoutParams) mediaPages[a].getLayoutParams();
        // layoutParams.leftMargin = layoutParams.rightMargin = 0;
        boolean fastScrollVisible = false;
        int spanCount = 100;
        RecyclerView.Adapter currentAdapter = mediaPages[a].listView.getAdapter();
        RecyclerView.RecycledViewPool viewPool = null;

       ArrayList<MessageObject> newMessageObject= newsData[mediaPages[a].selectedType].messageObjects;
       if(newMessageObject.isEmpty()){
           FeedController.getInstance(profileActivity.getCurrentAccount()).loadMessageForTab(FeedController.categoryData[mediaPages[a].selectedType]);
       }
        mediaPages[a].listView.setAdapter(newsAdapter);
       if(newsAdapter != null){
           newsAdapter.notifyDataSetChanged();
       }

    }


    protected void onSelectedTabChanged() {

    }


    private void scrollToTop() {

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

    public boolean isCurrentTabFirst() {
        return scrollSlidingTextTabStrip.getCurrentTabId() == scrollSlidingTextTabStrip.getFirstTabId();
    }

    public RecyclerListView getCurrentListView() {
        return mediaPages[0].listView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (profileActivity.getParentLayout() != null && !profileActivity.getParentLayout().checkTransitionAnimation() && !checkTabsAnimationInProgress()) {
            if (ev != null) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                velocityTracker.addMovement(ev);


            }
            if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && !startedTracking && !maybeStartTracking && ev.getY() >= AndroidUtilities.dp(48)) {
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

                    scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
                    onSelectedTabChanged();
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
                            } else {
                                MediaPage tempPage = mediaPages[0];
                                mediaPages[0] = mediaPages[1];
                                mediaPages[1] = tempPage;
                                mediaPages[1].setVisibility(View.GONE);
                                scrollSlidingTextTabStrip.selectTabWithId(mediaPages[0].selectedType, 1.0f);
                                onSelectedTabChanged();
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
                    onSelectedTabChanged();
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



    private class NewsAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;
        private int currentType;
        private boolean inFastScrollMode;

        public NewsAdapter(Context context, int type) {
            mContext = context;
            currentType = type;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return newsData[currentType].totalCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            SharedDocumentCell cell = new SharedDocumentCell(mContext);
            view = cell;
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ArrayList<MessageObject> messageObjects = newsData[currentType].messageObjects;
            SharedDocumentCell sharedDocumentCell = (SharedDocumentCell) holder.itemView;
            MessageObject messageObject = messageObjects.get(position);
            sharedDocumentCell.setDocument(messageObject, position != messageObjects.size() - 1);

        }


    }


    private boolean prepareForMoving(MotionEvent ev, boolean forward) {
        int id = scrollSlidingTextTabStrip.getNextPageId(forward);
        if (id < 0) {
            return false;
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

    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        return arrayList;
    }
}
