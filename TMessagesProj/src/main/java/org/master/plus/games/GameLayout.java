//package org.master.plus.games;
//
//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
//import android.animation.AnimatorSet;
//import android.animation.ObjectAnimator;
//import android.animation.ValueAnimator;
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.PorterDuff;
//import android.graphics.PorterDuffColorFilter;
//import android.graphics.drawable.Drawable;
//import android.text.TextUtils;
//import android.util.SparseArray;
//import android.util.SparseBooleanArray;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.MotionEvent;
//import android.view.Surface;
//import android.view.VelocityTracker;
//import android.view.View;
//import android.view.ViewConfiguration;
//import android.view.ViewGroup;
//import android.view.ViewTreeObserver;
//import android.view.WindowManager;
//import android.view.animation.Interpolator;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//
//import com.google.android.gms.common.api.Api;
//
//import org.checkerframework.checker.units.qual.A;
//import org.master.AppUtils;
//import org.master.network.APIError;
//import org.master.network.ApiManager;
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.ApplicationLoader;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.NotificationCenter;
//import org.telegram.messenger.R;
//import org.telegram.messenger.UserConfig;
//import org.telegram.ui.ActionBar.ActionBar;
//import org.telegram.ui.ActionBar.BaseFragment;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.Cells.GraySectionCell;
//import org.telegram.ui.Components.CombinedDrawable;
//import org.telegram.ui.Components.FlickerLoadingView;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
//import org.telegram.ui.Components.RecyclerListView;
//import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
//import org.telegram.ui.Components.StickerEmptyView;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.HashMap;
//import java.util.Locale;
//
//
//@SuppressLint("ViewConstructor")
//public class GameLayout extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {
//
//    public static final int ORDER_TYPE_SENT = 0;
//    public static final int ORDER_TYPE_INCOMING = 1;
//    public OrderData[] orderData  = new OrderData[2];
//    public Drawable placeHolder;
//
//    private static class OrderData{
//
//        public ArrayList<GameModel> orders = new ArrayList<>();
//        public SparseArray<GameModel> ordersDict = new SparseArray();
//        public ArrayList<String> sections = new ArrayList<>();
//        public HashMap<String, ArrayList<GameModel>> sectionArrays = new HashMap<>();
//        public boolean loading;
//        public String nextLoadLInk;
//        public boolean endReached;
//        public int offset;
//        public String cat;
//
//
//        public boolean addOrder(GameModel orderModel, boolean isNew) {
//            if (ordersDict.indexOfKey(orderModel.id) >= 0) {
//                return false;
//            }
//            String monthKey = getDateKey(orderModel.created_at);
//            ArrayList<GameModel> orderModels = sectionArrays.get(monthKey);
//            if (orderModels == null) {
//                orderModels = new ArrayList<>();
//                sectionArrays.put(monthKey, orderModels);
//                if (isNew) {
//                    sections.add(0, monthKey);
//                } else {
//                    sections.add(monthKey);
//                }
//            }
//            if (isNew) {
//                orderModels.add(0, orderModel);
//                orders.add(0, orderModel);
//            } else {
//                orderModels.add(orderModel);
//            }
//            ordersDict.put(orderModel.id, orderModel);
//            return true;
//        }
//
//        public void setOffset(int offset) {
//            this.offset = offset;
//        }
//
//        public GameModel deleteOrder(int orderId) {
//            GameModel orderModel = ordersDict.get(orderId);
//            if (orderModel == null) {
//                return null;
//            }
//            String monthKey = getDateKey(orderModel.created_at);
//            ArrayList<GameModel> orderModels = sectionArrays.get(monthKey);
//            if (orderModels == null) {
//                return null;
//            }
//            orderModels.remove(orderModel);
//            orders.remove(orderModel);
//            ordersDict.remove(orderModel.id);
//            if (orderModels.isEmpty()) {
//                sectionArrays.remove(monthKey);
//                sections.remove(monthKey);
//            }
//            return orderModel;
//        }
//
//        public void setEndReached(boolean reacehd) {
//            endReached = reacehd;
//        }
//
//        private  String getDateKey(String created_at){
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTimeInMillis(AppUtils.getTimeInMill(created_at));
//            int dateDay = calendar.get(Calendar.DAY_OF_YEAR);
//            int dateYear = calendar.get(Calendar.YEAR);
//            int dateMonth = calendar.get(Calendar.MONTH);
//            return String.format(Locale.US, "%d_%02d_%02d", dateYear, dateMonth, dateDay);
//        }
//    }
//
//    private final int parentClassGuid;
//    private final ArrayList<Integer> reqIds = new ArrayList<>();
//
//    private static class MediaPage extends FrameLayout {
//
//        private RecyclerListView listView;
//        private LinearLayoutManager layoutManager;
//        private FlickerLoadingView progressView;
//        private StickerEmptyView emptyView;
//        private RecyclerAnimationScrollHelper scrollHelper;
//        private int selectedType;
//
//
//        public MediaPage(Context context) {
//            super(context);
//        }
//    }
//
//
//    public ArrayList<GameCategory> gameCategories = new ArrayList<>();
//
//
//    public void setGameCategories(ArrayList<GameCategory> gameCategories) {
//        this.gameCategories = gameCategories;
//        updateTabs();
//    }
//
//    private final ActionBar actionBar;
//
//    private MediaPage[] mediaPages = new MediaPage[2];
//    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;
//    private View shadowLine;
//
//    private int maximumVelocity;
//    private Paint backgroundPaint = new Paint();
//
//    private AnimatorSet tabsAnimation;
//    private boolean tabsAnimationInProgress;
//    private boolean animatingForward;
//    private boolean backAnimation;
//
//    private int columnsCount = 2;
//
//    private OrderStateAdapter[] adapter = OrderStateAdapter[];
//
//    private static final Interpolator interpolator = t -> {
//        --t;
//        return t * t * t * t * t + 1.0F;
//    };
//
//    private int startedTrackingPointerId;
//    private boolean startedTracking;
//    private boolean maybeStartTracking;
//    private int startedTrackingX;
//    private int startedTrackingY;
//    private VelocityTracker velocityTracker;
//
//    private BaseFragment profileActivity;
//    private boolean scrolling;
//    private boolean forceClear;
//
//
//    public void forceRefreshTransactions(){
//        for(int a= 0; a < mediaPages.length;a++){
//            if (!orderData[mediaPages[a].selectedType].loading) {
//                //orderData[mediaPages[a].selectedType].loading = true;
//                //forceClear = true;
//               // loadGames(mediaPages[a].selectedType);
//                //reqIds.add(req_id);
//            }
//        }
//    }
//
//
//    public GameLayout(Context context, BaseFragment parent) {
//        super(context);
//
//        placeHolder = context.getResources().getDrawable(R.drawable.filter_game);
//        parentClassGuid  = parent.getClassGuid();
//        profileActivity = parent;
//        actionBar = profileActivity.getActionBar();
//        for (int a = 0; a < orderData.length; a++) {
//            orderData[a] = new OrderData();
//        }
//
//        ViewConfiguration configuration = ViewConfiguration.get(context);
//        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
//
//        scrollSlidingTextTabStrip = new ScrollSlidingTextTabStrip(context);
//        scrollSlidingTextTabStrip.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//        scrollSlidingTextTabStrip.setColors(Theme.key_profile_tabSelectedLine, Theme.key_profile_tabSelectedText, Theme.key_profile_tabText, Theme.key_profile_tabSelector);
//        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
//            @Override
//            public void onPageSelected(int id, boolean forward) {
//                if (mediaPages[0].selectedType == id) {
//                    return;
//                }
//                mediaPages[1].selectedType = id;
//                mediaPages[1].setVisibility(View.VISIBLE);
//                switchToCurrentSelectedMode(true);
//                animatingForward = forward;
//                onSelectedTabChanged();
//            }
//
//            @Override
//            public void onSamePageSelected() {
//                scrollToTop();
//            }
//
//
//            @Override
//            public void onPageScrolled(float progress) {
//                if (progress == 1 && mediaPages[1].getVisibility() != View.VISIBLE) {
//                    return;
//                }
//                if (animatingForward) {
//                    mediaPages[0].setTranslationX(-progress * mediaPages[0].getMeasuredWidth());
//                    mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() - progress * mediaPages[0].getMeasuredWidth());
//                } else {
//                    mediaPages[0].setTranslationX(progress * mediaPages[0].getMeasuredWidth());
//                    mediaPages[1].setTranslationX(progress * mediaPages[0].getMeasuredWidth() - mediaPages[0].getMeasuredWidth());
//                }
//
//                if (progress == 1) {
//                    MediaPage tempPage = mediaPages[0];
//                    mediaPages[0] = mediaPages[1];
//                    mediaPages[1] = tempPage;
//                    mediaPages[1].setVisibility(View.GONE);
//                }
//
//            }
//        });
//
//        activeStateAdapter = new OrderStateAdapter(context,0);
//        settledStateAdapter = new OrderStateAdapter(context,1);
//        setWillNotDraw(false);
//
//        int scrollToPositionOnRecreate = -1;
//        int scrollToOffsetOnRecreate = 0;
//
//        for (int a = 0; a < mediaPages.length; a++) {
//            if (a == 0) {
//                if (mediaPages[a] != null && mediaPages[a].layoutManager != null) {
//                    scrollToPositionOnRecreate = mediaPages[a].layoutManager.findFirstVisibleItemPosition();
//                    if (scrollToPositionOnRecreate != mediaPages[a].layoutManager.getItemCount() - 1) {
//                        RecyclerListView.Holder holder = (RecyclerListView.Holder) mediaPages[a].listView.findViewHolderForAdapterPosition(scrollToPositionOnRecreate);
//                        if (holder != null) {
//                            scrollToOffsetOnRecreate = holder.itemView.getTop();
//                        } else {
//                            scrollToPositionOnRecreate = -1;
//                        }
//                    } else {
//                        scrollToPositionOnRecreate = -1;
//                    }
//                }
//            }
//            final MediaPage mediaPage = new MediaPage(context) {
//                @Override
//                public void setTranslationX(float translationX) {
//                    super.setTranslationX(translationX);
//                    if (tabsAnimationInProgress) {
//                        if (mediaPages[0] == this) {
//                            float scrollProgress = Math.abs(mediaPages[0].getTranslationX()) / (float) mediaPages[0].getMeasuredWidth();
//                            scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
//                        }
//                    }
//                }
//            };
//            addView(mediaPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 48, 0, 0));
//            mediaPages[a] = mediaPage;
//
//
//            final LinearLayoutManager layoutManager = mediaPages[a].layoutManager = new LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false);
//
//            mediaPages[a].listView = new RecyclerListView(context) {
//                @Override
//                protected void onLayout(boolean changed, int l, int t, int r, int b) {
//                    super.onLayout(changed, l, t, r, b);
//                    checkLoadMoreScroll(mediaPage, mediaPage.listView, layoutManager);
//                }
//
//            };
//            mediaPages[a].listView.setFocusable(true);
//            mediaPages[a].listView.setFocusableInTouchMode(true);
//            mediaPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
//            mediaPages[a].listView.setPinnedSectionOffsetY(-AndroidUtilities.dp(2));
//            mediaPages[a].listView.setItemAnimator(null);
//            mediaPages[a].listView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
//            mediaPages[a].listView.setSectionsType(RecyclerListView.SECTIONS_TYPE_DATE);
//            mediaPages[a].listView.setClipToPadding(false);
//            mediaPages[a].listView.setOnItemClickListener((view, position) -> {
//
//           if(view instanceof GameCell) {
//               GameCell tradeCell = (GameCell) view;
////               GameCell orderModel = tradeCell.getCurrentOrder();
//
//           }
//            });
//            mediaPages[a].listView.setLayoutManager(layoutManager);
//            mediaPages[a].addView(mediaPages[a].listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//            mediaPages[a].listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
//                @Override
//                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                    scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
//                }
//
//                @Override
//                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                    checkLoadMoreScroll(mediaPage, (RecyclerListView) recyclerView, layoutManager);
//                }
//            });
//            if (a == 0 && scrollToPositionOnRecreate != -1) {
//                layoutManager.scrollToPositionWithOffset(scrollToPositionOnRecreate, scrollToOffsetOnRecreate);
//            }
//
//            if (a != 0) {
//                mediaPages[a].setVisibility(View.GONE);
//            }
//
//            mediaPages[a].scrollHelper = new RecyclerAnimationScrollHelper(mediaPages[a].listView, mediaPages[a].layoutManager);
//        }
//
//        addView(scrollSlidingTextTabStrip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.TOP));
//
//        shadowLine = new View(context);
//        shadowLine.setBackgroundColor(Theme.getColor(Theme.key_divider));
//        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
//        layoutParams.topMargin = AndroidUtilities.dp(48) - 1;
//        addView(shadowLine, layoutParams);
//
//        updateTabs();
//
//        switchToCurrentSelectedMode(false);
//    }
//
//
//    private void switchToCurrentSelectedMode(boolean animated){
//        for (int a = 0; a < mediaPages.length; a++) {
//            mediaPages[a].listView.stopScroll();
//        }
//        int a = animated ? 1 : 0;
//        RecyclerView.Adapter currentAdapter = mediaPages[a].listView.getAdapter();
//        mediaPages[a].listView.setPinnedHeaderShadowDrawable(null);
//        if (mediaPages[a].selectedType == 0) {
//            if (currentAdapter != activeStateAdapter) {
//                mediaPages[a].listView.setAdapter(activeStateAdapter);
//            }
//        }else if (mediaPages[a].selectedType == 1) {
//            if (currentAdapter != settledStateAdapter) {
//                mediaPages[a].listView.setAdapter(settledStateAdapter);
//            }
//        }
//        if (!orderData[mediaPages[a].selectedType].loading && !orderData[mediaPages[a].selectedType].endReached && orderData[mediaPages[a].selectedType].orders.isEmpty()) {
//            orderData[mediaPages[a].selectedType].loading = true;
//            activeStateAdapter.notifyDataSetChanged();
//            int req_id = ApiManager.getInstance(profileActivity.getCurrentAccount()).getGamesForCategory(orderData[a].cat, orderData[a].offset, 100, new ApiManager.ResponseCallback() {
//                @Override
//                public void onResponse(Object response, APIError apiError) {
//                    if(apiError == null){
//                        ArrayList<GameModel> arr = (ArrayList<GameModel>) response;
//
//                    }else{
//                        orderData[type].loading = false;
//                        RecyclerListView.Adapter adapter = null;
//                        if (type == 0) {
//                            adapter = activeStateAdapter;
//                        } else if (type == 1) {
//                            adapter = settledStateAdapter;
//                        }
//                        adapter.notifyDataSetChanged();
//                    }
//                }
//            });
//            reqIds.add(req_id);
//        }
//        mediaPages[a].listView.setVisibility(View.VISIBLE);
//    }
//
//
//
//    public void loadGames(int post){
//
//    }
//
//
//
//    @Override
//    public void didReceivedNotification(int id, int account, Object... args) {
//         if (id == NotificationCenter.didTradeLoaded) {
//             boolean loaded = (boolean) args[0];
//            int guid = (Integer) args[1];
//             boolean active_order = (boolean)args[2];
//             int type = active_order?0:1;
//            if (guid == profileActivity.getClassGuid()) {
//                if(loaded){
//                    ArrayList<OrderModel> arr = (ArrayList<OrderModel>) args[3];
//                    String nextUrl = (String)args[4];
//                    RecyclerListView.Adapter adapter = null;
//                    if (type == 0) {
//                        adapter = activeStateAdapter;
//                    } else if (type == 1) {
//                        adapter = settledStateAdapter;
//                    }
//                    if(forceClear){
//                        orderData[type].orders.clear();
//                        orderData[type].sections.clear();
//                        orderData[type].sectionArrays.clear();
//                        orderData[type].ordersDict.clear();
//                        forceClear = false;
//                    }
//                    int oldItemCount;
//                    if (adapter != null) {
//                        oldItemCount = adapter.getItemCount();
//                        if (adapter instanceof RecyclerListView.SectionsAdapter) {
//                            RecyclerListView.SectionsAdapter sectionsAdapter = (RecyclerListView.SectionsAdapter) adapter;
//                            sectionsAdapter.notifySectionsChanged();
//                        }
//                    } else {
//                        oldItemCount = 0;
//                    }
//                    orderData[type].loading = false;
//                    orderData[type].endReached = ShopUtils.isEmpty(nextUrl);
//                    orderData[type].nextLoadLInk = nextUrl;
//
//                    SparseBooleanArray addedOrders = new SparseBooleanArray();
//
//                    for (int a = 0; a < arr.size(); a++) {
//                        OrderModel orderModel = arr.get(a);
//                        if (orderData[type].addOrder(orderModel, false)) {
//                            addedOrders.put(orderModel.id, true);
//                        }
//                    }
//                    if (adapter != null) {
//                        RecyclerListView listView = null;
//                        for (int a = 0; a < mediaPages.length; a++) {
//                            if (mediaPages[a].listView.getAdapter() == adapter) {
//                                listView = mediaPages[a].listView;
//                                mediaPages[a].listView.stopScroll();
//                            }
//                        }
//                        int newItemCount = adapter.getItemCount();
//                        adapter.notifyDataSetChanged();
//                        if (orderData[type].orders.isEmpty() && !orderData[type].loading) {
//                            if (listView != null) {
//                                animateItemsEnter(listView, oldItemCount, addedOrders);
//                            }
//                        } else {
//                            if (listView != null && (newItemCount >= oldItemCount)) {
//                               animateItemsEnter(listView, oldItemCount, addedOrders);
//                            }
//                        }
//                    }
//                    scrolling = true;
//                }else{
//                    APIError apiError = (APIError)args[5];
//                    orderData[type].loading = false;
//                    RecyclerListView.Adapter adapter = null;
//                    if (type == 0) {
//                        adapter = activeStateAdapter;
//                    } else if (type == 1) {
//                        adapter = settledStateAdapter;
//                    }
//                    adapter.notifyDataSetChanged();
//                }
//
//            }
//        }
//    }
//
//    SparseArray<Float> messageAlphaEnter = new SparseArray<>();
//
//    private void animateItemsEnter(final RecyclerListView finalListView, int oldItemCount, SparseBooleanArray addedMesages) {
//        int n = finalListView.getChildCount();
//        View progressView = null;
//        for (int i = 0; i < n; i++) {
//            View child = finalListView.getChildAt(i);
//            if (child instanceof FlickerLoadingView) {
//                progressView = child;
//            }
//        }
//        final View finalProgressView = progressView;
//        if (progressView != null) {
//            finalListView.removeView(progressView);
//        }
//        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                getViewTreeObserver().removeOnPreDrawListener(this);
//                RecyclerView.Adapter adapter = finalListView.getAdapter();
//                if ( adapter == activeStateAdapter || adapter == settledStateAdapter) {
//                    if (addedMesages != null) {
//                        int n = finalListView.getChildCount();
//                        for (int i = 0; i < n; i++) {
//                            View child = finalListView.getChildAt(i);
//                            int tradeId = getMessageId(child);
//                            if (tradeId != 0 && addedMesages.get(tradeId, false)) {
//                                messageAlphaEnter.put(tradeId, 0f);
//                                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
//                                valueAnimator.addUpdateListener(valueAnimator1 -> {
//                                    messageAlphaEnter.put(tradeId, (Float) valueAnimator1.getAnimatedValue());
//                                    finalListView.invalidate();
//                                });
//                                valueAnimator.addListener(new AnimatorListenerAdapter() {
//                                    @Override
//                                    public void onAnimationEnd(Animator animation) {
//                                        messageAlphaEnter.remove(tradeId);
//                                        finalListView.invalidate();
//                                    }
//                                });
//                                int s = Math.min(finalListView.getMeasuredHeight(), Math.max(0, child.getTop()));
//                                int delay = (int) ((s / (float) finalListView.getMeasuredHeight()) * 100);
//                                valueAnimator.setStartDelay(delay);
//                                valueAnimator.setDuration(250);
//                                valueAnimator.start();
//                            }
//                            finalListView.invalidate();
//                        }
//                    }
//                } else {
//                    int n = finalListView.getChildCount();
//                    AnimatorSet animatorSet = new AnimatorSet();
//                    for (int i = 0; i < n; i++) {
//                        View child = finalListView.getChildAt(i);
//                        if (child != finalProgressView && finalListView.getChildAdapterPosition(child) >= oldItemCount - 1) {
//                            child.setAlpha(0);
//                            int s = Math.min(finalListView.getMeasuredHeight(), Math.max(0, child.getTop()));
//                            int delay = (int) ((s / (float) finalListView.getMeasuredHeight()) * 100);
//                            ObjectAnimator a = ObjectAnimator.ofFloat(child, View.ALPHA, 0, 1f);
//                            a.setStartDelay(delay);
//                            a.setDuration(200);
//                            animatorSet.playTogether(a);
//                        }
//                        if (finalProgressView != null && finalProgressView.getParent() == null) {
//                            finalListView.addView(finalProgressView);
//                            RecyclerView.LayoutManager layoutManager = finalListView.getLayoutManager();
//                            if (layoutManager != null) {
//                                layoutManager.ignoreView(finalProgressView);
//                                Animator animator = ObjectAnimator.ofFloat(finalProgressView, ALPHA, finalProgressView.getAlpha(), 0);
//                                animator.addListener(new AnimatorListenerAdapter() {
//                                    @Override
//                                    public void onAnimationEnd(Animator animation) {
//                                        finalProgressView.setAlpha(1f);
//                                        layoutManager.stopIgnoringView(finalProgressView);
//                                        finalListView.removeView(finalProgressView);
//                                    }
//                                });
//                                animator.start();
//                            }
//                        }
//                    }
//                    animatorSet.start();
//                }
//                return true;
//            }
//        });
//    }
//
//    private int getMessageId(View child) {
//        if (child instanceof GameCell) {
//            if(((GameCell)child).getCurrentGameModel() == null)
//            {
//                return 0;
//            }
//            return ((GameCell) child).getCurrentGameModel().id;
//        }
//
//        return 0;
//    }
//
//    private void scrollToTop() {
//        int height;
//        switch (mediaPages[0].selectedType) {
//            default:
//                height = AndroidUtilities.dp(104);
//                break;
//        }
//        int scrollDistance = mediaPages[0].layoutManager.findFirstVisibleItemPosition() * height;
//        if (scrollDistance >= mediaPages[0].listView.getMeasuredHeight() * 1.2f) {
//            mediaPages[0].scrollHelper.setScrollDirection(RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP);
//            mediaPages[0].scrollHelper.scrollToPosition(0, 0, false, true);
//        } else {
//            mediaPages[0].listView.smoothScrollToPosition(0);
//        }
//    }
//
//    private void checkLoadMoreScroll(MediaPage mediaPage, RecyclerListView recyclerView, LinearLayoutManager layoutManager) {
//        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
//        int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
//        int totalItemCount = recyclerView.getAdapter().getItemCount();
//        final int threshold = 3;
//        if ((firstVisibleItem + visibleItemCount > totalItemCount - threshold && !orderData[mediaPage.selectedType].loading)) {
//            if (!orderData[mediaPage.selectedType].endReached) {
//                orderData[mediaPage.selectedType].loading = true;
//                if (!orderData[mediaPages[mediaPage.selectedType].selectedType].loading && !orderData[mediaPages[mediaPage.selectedType].selectedType].endReached && orderData[mediaPages[mediaPage.selectedType].selectedType].orders.isEmpty()) {
////                    orderData[mediaPages[mediaPage.selectedType].selectedType].loading = true;
////                    int req_id  = ApiManager.getInstance(profileActivity.getClassGuid()).getGamesForCategory(mediaPages[mediaPage.selectedType].selectedType)
//////                    int req_id = ServicesDataController.getInstance(profileActivity.getCurrentAccount()).loadOrders(mediaPages[mediaPage.selectedType].selectedType == 0, orderData[mediaPage.selectedType].nextLoadLInk, parentClassGuid,profileActivity instanceof  PurchasesProfileActivity?ORDER_TYPE_SENT: ORDER_TYPE_INCOMING);
////                    reqIds.add(req_id);
//                }
//            }
//        }
//    }
//
//    public int getSelectedTab() {
//        return scrollSlidingTextTabStrip.getCurrentTabId();
//    }
//
//    public void onDestroy() {
//        ApiManager.getInstance(UserConfig.selectedAccount).cancelRequestsForGuid(parentClassGuid);
//    }
//
//    private boolean prepareForMoving(MotionEvent ev, boolean forward) {
//        int id = scrollSlidingTextTabStrip.getNextPageId(forward);
//        if (id < 0) {
//            return false;
//        }
//
//        getParent().requestDisallowInterceptTouchEvent(true);
//        maybeStartTracking = false;
//        startedTracking = true;
//        startedTrackingX = (int) ev.getX();
//        actionBar.setEnabled(false);
//        scrollSlidingTextTabStrip.setEnabled(false);
//        mediaPages[1].selectedType = id;
//        mediaPages[1].setVisibility(View.VISIBLE);
//        animatingForward = forward;
//        switchToCurrentSelectedMode(true);
//        if (forward) {
//            mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth());
//        } else {
//            mediaPages[1].setTranslationX(-mediaPages[0].getMeasuredWidth());
//        }
//        return true;
//    }
//
//    @Override
//    public void forceHasOverlappingRendering(boolean hasOverlappingRendering) {
//        super.forceHasOverlappingRendering(hasOverlappingRendering);
//    }
//
//    @Override
//    public void setPadding(int left, int top, int right, int bottom) {
//        for (int a = 0; a < mediaPages.length; a++) {
//            mediaPages[a].setTranslationY(top);
//        }
//    }
//
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int heightSize = 0;
//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        if(profileActivity instanceof GameListFragment) {
//
//            heightSize = ((GameListFragment) profileActivity).getListView().getHeight();
//
//            if (heightSize == 0) {
//                heightSize = MeasureSpec.getSize(heightMeasureSpec);
//            }
//        }
//        setMeasuredDimension(widthSize, heightSize);
//        int childCount = getChildCount();
//        for (int i = 0; i < childCount; i++) {
//            View child = getChildAt(i);
//            if (child == null || child.getVisibility() == GONE) {
//                continue;
//            }
//            if (child instanceof MediaPage) {
//                measureChildWithMargins(child, widthMeasureSpec, 0, MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY), 0);
//            } else {
//                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
//            }
//        }
//    }
//
//    public boolean checkTabsAnimationInProgress() {
//        if (tabsAnimationInProgress) {
//            boolean cancel = false;
//            if (backAnimation) {
//                if (Math.abs(mediaPages[0].getTranslationX()) < 1) {
//                    mediaPages[0].setTranslationX(0);
//                    mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() * (animatingForward ? 1 : -1));
//                    cancel = true;
//                }
//            } else if (Math.abs(mediaPages[1].getTranslationX()) < 1) {
//                mediaPages[0].setTranslationX(mediaPages[0].getMeasuredWidth() * (animatingForward ? -1 : 1));
//                mediaPages[1].setTranslationX(0);
//                cancel = true;
//            }
//            if (cancel) {
//                if (tabsAnimation != null) {
//                    tabsAnimation.cancel();
//                    tabsAnimation = null;
//                }
//                tabsAnimationInProgress = false;
//            }
//            return tabsAnimationInProgress;
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return checkTabsAnimationInProgress() || scrollSlidingTextTabStrip.isAnimatingIndicator() || onTouchEvent(ev);
//    }
//
//    public boolean isCurrentTabFirst() {
//        return scrollSlidingTextTabStrip.getCurrentTabId() == scrollSlidingTextTabStrip.getFirstTabId();
//    }
//
//    public RecyclerListView getCurrentListView() {
//        return mediaPages[0].listView;
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        if (profileActivity.getParentLayout() != null && !profileActivity.getParentLayout().checkTransitionAnimation() && !checkTabsAnimationInProgress()) {
//            if (ev != null) {
//                if (velocityTracker == null) {
//                    velocityTracker = VelocityTracker.obtain();
//                }
//                velocityTracker.addMovement(ev);
//            }
//            if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && !startedTracking && !maybeStartTracking && ev.getY() >= AndroidUtilities.dp(48)) {
//                startedTrackingPointerId = ev.getPointerId(0);
//                maybeStartTracking = true;
//                startedTrackingX = (int) ev.getX();
//                startedTrackingY = (int) ev.getY();
//                velocityTracker.clear();
//            } else if (ev != null && ev.getAction() == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
//                int dx = (int) (ev.getX() - startedTrackingX);
//                int dy = Math.abs((int) ev.getY() - startedTrackingY);
//                if (startedTracking && (animatingForward && dx > 0 || !animatingForward && dx < 0)) {
//                    if (!prepareForMoving(ev, dx < 0)) {
//                        maybeStartTracking = true;
//                        startedTracking = false;
//                        mediaPages[0].setTranslationX(0);
//                        mediaPages[1].setTranslationX(animatingForward ? mediaPages[0].getMeasuredWidth() : -mediaPages[0].getMeasuredWidth());
//                        scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, 0);
//                    }
//                }
//                if (maybeStartTracking && !startedTracking) {
//                    float touchSlop = AndroidUtilities.getPixelsInCM(0.3f, true);
//                    if (Math.abs(dx) >= touchSlop && Math.abs(dx) > dy) {
//                        prepareForMoving(ev, dx < 0);
//                    }
//                } else if (startedTracking) {
//                    mediaPages[0].setTranslationX(dx);
//                    if (animatingForward) {
//                        mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() + dx);
//                    } else {
//                        mediaPages[1].setTranslationX(dx - mediaPages[0].getMeasuredWidth());
//                    }
//                    float scrollProgress = Math.abs(dx) / (float) mediaPages[0].getMeasuredWidth();
//
//                    scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
//                }
//            } else if (ev == null || ev.getPointerId(0) == startedTrackingPointerId && (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_POINTER_UP)) {
//                velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
//                float velX;
//                float velY;
//                if (ev != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
//                    velX = velocityTracker.getXVelocity();
//                    velY = velocityTracker.getYVelocity();
//                    if (!startedTracking) {
//                        if (Math.abs(velX) >= 3000 && Math.abs(velX) > Math.abs(velY)) {
//                            prepareForMoving(ev, velX < 0);
//                        }
//                    }
//                } else {
//                    velX = 0;
//                    velY = 0;
//                }
//                if (startedTracking) {
//                    float x = mediaPages[0].getX();
//                    tabsAnimation = new AnimatorSet();
//                    backAnimation = Math.abs(x) < mediaPages[0].getMeasuredWidth() / 3.0f && (Math.abs(velX) < 3500 || Math.abs(velX) < Math.abs(velY));
//                    float distToMove;
//                    float dx;
//                    if (backAnimation) {
//                        dx = Math.abs(x);
//                        if (animatingForward) {
//                            tabsAnimation.playTogether(
//                                    ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, 0),
//                                    ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, mediaPages[1].getMeasuredWidth())
//                            );
//                        } else {
//                            tabsAnimation.playTogether(
//                                    ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, 0),
//                                    ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, -mediaPages[1].getMeasuredWidth())
//                            );
//                        }
//                    } else {
//                        dx = mediaPages[0].getMeasuredWidth() - Math.abs(x);
//                        if (animatingForward) {
//                            tabsAnimation.playTogether(
//                                    ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, -mediaPages[0].getMeasuredWidth()),
//                                    ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, 0)
//                            );
//                        } else {
//                            tabsAnimation.playTogether(
//                                    ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, mediaPages[0].getMeasuredWidth()),
//                                    ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, 0)
//                            );
//                        }
//                    }
//                    tabsAnimation.setInterpolator(interpolator);
//
//                    int width = getMeasuredWidth();
//                    int halfWidth = width / 2;
//                    float distanceRatio = Math.min(1.0f, 1.0f * dx / (float) width);
//                    float distance = (float) halfWidth + (float) halfWidth * AndroidUtilities.distanceInfluenceForSnapDuration(distanceRatio);
//                    velX = Math.abs(velX);
//                    int duration;
//                    if (velX > 0) {
//                        duration = 4 * Math.round(1000.0f * Math.abs(distance / velX));
//                    } else {
//                        float pageDelta = dx / getMeasuredWidth();
//                        duration = (int) ((pageDelta + 1.0f) * 100.0f);
//                    }
//                    duration = Math.max(150, Math.min(duration, 600));
//
//                    tabsAnimation.setDuration(duration);
//                    tabsAnimation.addListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animator) {
//                            tabsAnimation = null;
//                            if (backAnimation) {
//                                mediaPages[1].setVisibility(View.GONE);
//
//                            } else {
//                                MediaPage tempPage = mediaPages[0];
//                                mediaPages[0] = mediaPages[1];
//                                mediaPages[1] = tempPage;
//                                mediaPages[1].setVisibility(View.GONE);
//
//                                scrollSlidingTextTabStrip.selectTabWithId(mediaPages[0].selectedType, 1.0f);
//                                onSelectedTabChanged();
//                            }
//                            tabsAnimationInProgress = false;
//                            maybeStartTracking = false;
//                            startedTracking = false;
//                            actionBar.setEnabled(true);
//                            scrollSlidingTextTabStrip.setEnabled(true);
//                        }
//                    });
//                    tabsAnimation.start();
//                    tabsAnimationInProgress = true;
//                    startedTracking = false;
//                } else {
//                    maybeStartTracking = false;
//                    actionBar.setEnabled(true);
//                    scrollSlidingTextTabStrip.setEnabled(true);
//                }
//                if (velocityTracker != null) {
//                    velocityTracker.recycle();
//                    velocityTracker = null;
//                }
//            }
//            return startedTracking;
//        }
//        return false;
//    }
//
//
//    public void setVisibleHeight(int height) {
//        height = Math.max(height, AndroidUtilities.dp(120));
//        for (int a = 0; a < mediaPages.length; a++) {
//            float t = -(getMeasuredHeight() - height) / 2f;
//            //mediaPages[a].emptyView.setTranslationY(t);
//            //mediaPages[a].progressView.setTranslationY(-t);
//        }
//    }
//
//
//    public void onResume() {
//        scrolling = true;
//        if (activeStateAdapter != null) {
//            activeStateAdapter.notifyDataSetChanged();
//        }
//
//        if (settledStateAdapter != null) {
//            settledStateAdapter.notifyDataSetChanged();
//        }
//        for (int a = 0; a < mediaPages.length; a++) {
//            fixLayoutInternal(a);
//        }
//
//    }
//
//    private class OrderStateAdapter extends RecyclerListView.SectionsAdapter {
//
//        private Context mContext;
//        private final int currentType;
//
//        public OrderStateAdapter(Context context, int type) {
//            mContext = context;
//            currentType = type;
//        }
//
//        @Override
//        public Object getItem(int section, int position) {
//            return null;
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder, int section, int row) {
//            if (orderData[currentType].sections.size() == 0 && !orderData[currentType].loading) {
//                return false;
//            }
//            return section == 0 || row != 0;
//        }
//
//        @Override
//        public int getSectionCount() {
//            if (orderData[currentType].sections.size() == 0 && !orderData[currentType].loading) {
//                return 1;
//            }
//            if (orderData[currentType].sections.size() == 0 && orderData[currentType].loading) {
//                return 1;
//            }
//            return orderData[currentType].sections.size() + (orderData[currentType].sections.isEmpty() || orderData[currentType].endReached ? 0 : 1);
//
//        }
//
//        @Override
//        public int getCountForSection(int section) {
//            if (orderData[currentType].sections.size() == 0 && !orderData[currentType].loading) {
//                return 1;
//            }
//            if (orderData[currentType].sections.size() == 0 && orderData[currentType].loading) {
//                return 1;
//            }
//            if (section < orderData[currentType].sections.size()) {
//                return orderData[currentType].sectionArrays.get(orderData[currentType].sections.get(section)).size() + (section != 0 ? 1 : 0);
//            }
//            return 1;
//        }
//        @Override
//        public int getItemViewType(int section, int position) {
//            if (orderData[currentType].sections.size() == 0 && !orderData[currentType].loading) {
//                return 3;
//            }
//            if (orderData[currentType].sections.size() == 0 && !orderData[currentType].loading) {
//                return 2;
//            }
//            if (section < orderData[currentType].sections.size()) {
//                if (section != 0 && position == 0) {
//                    return 0;
//                } else {
//                    return 1;
//                }
//            }
//            return 2;
//        }
//
//        @Override
//        public View getSectionHeaderView(int section, View view) {
//            if (view == null) {
//                view = new GraySectionCell(mContext);
//                view.setBackgroundColor(Theme.getColor(Theme.key_graySection) & 0xf2ffffff);
//            }
//            if (section == 0) {
//                view.setAlpha(0.0f);
//            } else if (section < orderData[currentType].sections.size()) {
//                view.setAlpha(1.0f);
//                String name = orderData[currentType].sections.get(section);
//                ArrayList<GameModel> orderModels = orderData[currentType].sectionArrays.get(name);
//                long time = AppUtils.getTimeInMill(orderModels.get(0).created_at);
//                ((GraySectionCell) view).setText(LocaleController.formatDateChat(time/1000));
//            }
//            return view;
//        }
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view;
//            switch (viewType) {
//                case 0:
//                    view = new GraySectionCell(mContext);
//                    break;
//                case 1:
//                    view = new GameCell(mContext,placeHolder);
//                    break;
//                case 3:
//                    View emptyStubView = createEmptyStubView(mContext, currentType,0);
//                    emptyStubView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//                    return new RecyclerListView.Holder(emptyStubView);
//                case 2:
//                default:
//                    FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext);
//                    flickerLoadingView.setIsSingleCell(true);
//                    flickerLoadingView.showDate(false);
//                    flickerLoadingView.setViewType(FlickerLoadingView.LINKS_TYPE);
//                    view = flickerLoadingView;
//                    break;
//            }
//            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//            return new RecyclerListView.Holder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(int section, int position, RecyclerView.ViewHolder holder) {
//            if (holder.getItemViewType() != 2 && holder.getItemViewType() != 3) {
//                String name = orderData[currentType].sections.get(section);
//                ArrayList<GameModel> messageObjects = orderData[currentType].sectionArrays.get(name);
//                switch (holder.getItemViewType()) {
//                    case 0: {
//                        GameModel messageObject = messageObjects.get(0);
//                        ((GraySectionCell) holder.itemView).setText(LocaleController.formatDateChat(AppUtils.getTimeInMill(messageObject.created_at)/1000));
//                        break;
//                    }
//                    case 1: {
//                        if (section != 0) {
//                            position--;
//                        }
//                        GameCell sharedLinkCell = (GameCell) holder.itemView;
//                        GameModel messageObject = messageObjects.get(position);
//                        sharedLinkCell.setGame(messageObject);
//                        break;
//                    }
//                }
//            }
//        }
//
//
//        @Override
//        public String getLetter(int position) {
//            return null;
//        }
//
//        @Override
//        public void getPositionForScrollProgress(RecyclerListView listView, float progress, int[] position) {
//            position[0] = 0;
//            position[1] = 0;
//        }
//    }
//
//    protected void onSelectedTabChanged() {
//            switchToCurrentSelectedMode(true);
//    }
//
//    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        for (int a = 0; a < mediaPages.length; a++) {
//            if (mediaPages[a].listView != null) {
//                final int num = a;
//                ViewTreeObserver obs = mediaPages[a].listView.getViewTreeObserver();
//                obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                    @Override
//                    public boolean onPreDraw() {
//                        mediaPages[num].getViewTreeObserver().removeOnPreDrawListener(this);
//                        fixLayoutInternal(num);
//                        return true;
//                    }
//                });
//            }
//        }
//    }
//
//
//    private void updateTabs()  {
//        if (scrollSlidingTextTabStrip == null) {
//            return;
//        }
//        if(gameCategories.isEmpty()){
//            return;
//        }
//        for(int a = 0; a < gameCategories.size();a++){
//            GameCategory gameCategory = gameCategories.get(a);
//            scrollSlidingTextTabStrip.addTextTab(gameCategory.local_id, gameCategory.value.toUpperCase());
//        }
//        int id = scrollSlidingTextTabStrip.getCurrentTabId();
//        if (id >= 0) {
//            mediaPages[0].selectedType = id;
//        }
//        scrollSlidingTextTabStrip.finishAddingTabs();
//    }
//
//    private void fixLayoutInternal(int num) {
//    }
//
//    public static View createEmptyStubView(Context context, int currentType,int parent) {
//
////
////        StickerEmptyView stickerEmptyView = new StickerEmptyView(context,null,StickerEmptyView.STICKER_TYPE_NO_CONTACTS);
////        stickerEmptyView.title.setText("Empty");
////        stickerEmptyView.subtitle.setText("You haven't purchase any items yet");
////
////        if(1==1){
////            return stickerEmptyView;
////        }
//        EmptyStubView emptyStubView = new EmptyStubView(context);
//        Drawable icon;
//       if(parent == 0){
//           icon =context.getResources().getDrawable(R.drawable.filter_game);
//       }else{
//            icon =context.getResources().getDrawable(R.drawable.filter_game);
//
//       }
//        if(icon != null){
//            icon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogTextBlack), PorterDuff.Mode.SRC_IN));
//        }
//
//        emptyStubView.emptyImageView.setImageDrawable(icon);
//        if (parent == 0) {
//            emptyStubView.emptyTextView.setText("Empty! No Games");
//        } else if (currentType == 1) {
//            emptyStubView.emptyTextView.setText("Empty! No Games!");
//        }
//        return emptyStubView;
//    }
//
//
//
//
//    private static class EmptyStubView extends LinearLayout {
//
//        final TextView emptyTextView;
//        final ImageView emptyImageView;
//
//        boolean ignoreRequestLayout;
//
//        public EmptyStubView(Context context) {
//            super(context);
//            emptyTextView = new TextView(context);
//            emptyImageView = new ImageView(context);
//            emptyImageView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
//
//
//            setOrientation(LinearLayout.VERTICAL);
//            setGravity(Gravity.CENTER);
//
//            addView(emptyImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
//
//            emptyTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
//            emptyTextView.setGravity(Gravity.CENTER);
//            emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
//            emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), AndroidUtilities.dp(128));
//            addView(emptyTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 24, 0, 0));
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
//            int rotation = manager.getDefaultDisplay().getRotation();
//            ignoreRequestLayout = true;
//            if (AndroidUtilities.isTablet()) {
//                emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), AndroidUtilities.dp(128));
//            } else {
//                if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
//                    emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), 0);
//                } else {
//                    emptyTextView.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), AndroidUtilities.dp(128));
//                }
//            }
//            ignoreRequestLayout = false;
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
//
//        @Override
//        public void requestLayout() {
//            if (ignoreRequestLayout) {
//                return;
//            }
//            super.requestLayout();
//        }
//    }
//}
