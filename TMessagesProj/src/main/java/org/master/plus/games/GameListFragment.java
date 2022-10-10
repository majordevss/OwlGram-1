//package org.master.plus.games;
//
//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
//import android.animation.ValueAnimator;
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.PorterDuff;
//import android.graphics.PorterDuffColorFilter;
//import android.graphics.Typeface;
//import android.graphics.drawable.Drawable;
//import android.graphics.drawable.GradientDrawable;
//import android.os.Build;
//import android.os.Bundle;
//import android.text.SpannableStringBuilder;
//import android.text.Spanned;
//import android.text.TextUtils;
//import android.text.style.DynamicDrawableSpan;
//import android.text.style.ImageSpan;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.HapticFeedbackConstants;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.cardview.widget.CardView;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.NestedScrollingParent3;
//import androidx.core.view.NestedScrollingParentHelper;
//import androidx.core.view.ViewCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.NotificationCenter;
//import org.telegram.messenger.R;
//import org.telegram.ui.ActionBar.ActionBar;
//import org.telegram.ui.ActionBar.ActionBarMenu;
//import org.telegram.ui.ActionBar.AlertDialog;
//import org.telegram.ui.ActionBar.BaseFragment;
//import org.telegram.ui.ActionBar.SimpleTextView;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.CameraScanActivity;
//import org.telegram.ui.Cells.EmptyCell;
//import org.telegram.ui.Cells.HeaderCell;
//import org.telegram.ui.Cells.ShadowSectionCell;
//import org.telegram.ui.Cells.TextCell;
//import org.telegram.ui.Cells.TextInfoPrivacyCell;
//import org.telegram.ui.Components.AlertsCreator;
//import org.telegram.ui.Components.BulletinFactory;
//import org.telegram.ui.Components.CubicBezierInterpolator;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.PullForegroundDrawable;
//import org.telegram.ui.Components.RecyclerListView;
//import org.telegram.ui.Components.SizeNotifierFrameLayout;
//import org.telegram.ui.Components.TypefaceSpan;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//
//public class GameListFragment extends BaseFragment{
//
//
//    public class PullRecyclerView extends RecyclerListView {
//
//        private boolean firstLayout = true;
//        private boolean ignoreLayout;
//
//        public PullRecyclerView(Context context) {
//            super(context);
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
//            if (firstLayout) {
//                ignoreLayout = true;
//                layoutManager.scrollToPositionWithOffset(1, 0);
//                ignoreLayout = false;
//                firstLayout = false;
//            }
//            super.onMeasure(widthSpec, heightSpec);
//        }
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
//        public void setAdapter(Adapter adapter) {
//            super.setAdapter(adapter);
//            firstLayout = true;
//        }
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
//                pullForegroundDrawable.drawOverScroll(canvas);
//            }
//            super.onDraw(canvas);
//        }
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
//                            AndroidUtilities.cancelRunOnUIThread(updateTimeRunnable);
//                            lastUpdateTime = 0;
//                            loadAdd();
//                            updateTime(false);
//                        }
//
//                        if (viewOffset != 0) {
//                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(viewOffset, 0f);
//                            valueAnimator.addUpdateListener(animation -> listView.setViewsOffset((float) animation.getAnimatedValue()));
//
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
//
//    public static float viewOffset = 0.0f;
//
//    private BalanceCell balanceCell;
//    private long startArchivePullingTime;
//    private boolean canShowHiddenPull;
//    private boolean wasPulled;
//    private PullForegroundDrawable pullForegroundDrawable;
//    private SimpleTextView statusTextView;
//
//    private LinearLayoutManager layoutManager;
//    private PullRecyclerView listView;
//    private ListAdapter listAdapter;
//
//
//    private ShopMediaLayoutProfile shopMediaLayout;
//    private boolean shopMediaLayoutAttached;
//
//    private int rowCount;
//    private int shopActionSecRow;
//    private int gridItemsRow;
//    private int balanceRow;
//    private int productSecRow;
//    private int productRow;
//
//
//    private View paddingView;
//
//    private int paddingRow;
//    private int emptyRow;
//    private int buttonRow;
//
//    private void updateRowsIds() {
//        rowCount = 0;
//        paddingRow = rowCount++;
//        balanceRow = rowCount++;
//        buttonRow = rowCount++;
//        shopActionSecRow = -1;
//        gridItemsRow = rowCount++;
//        productSecRow = rowCount++;
//        productRow = rowCount++;
//        emptyRow = rowCount++;
//        if (listAdapter != null) {
//            listAdapter.notifyDataSetChanged();
//        }
//    }
//
//    public RecyclerListView getListView() {
//        return listView;
//    }
//    private Runnable updateTimeRunnable = () -> updateTime(true);
//    private long lastUpdateTime;
//    private void updateTime(boolean schedule) {
//        if (statusTextView != null) {
//            if (lastUpdateTime == 0) {
//                statusTextView.setText("Updating...");
//            } else {
//                long newTime = getConnectionsManager().getCurrentTime();
//                long dt = newTime - lastUpdateTime;
//                if (dt < 60) {
//                    statusTextView.setText("tUpdated Few Seconds Ago");
//                } else {
//                    String time;
//                    if (dt < 60 * 60) {
//                        time = LocaleController.formatPluralString("Minutes", (int) (dt / 60));
//                    } else if (dt < 60 * 60 * 24) {
//                        time = LocaleController.formatPluralString("Hours", (int) (dt / 60 / 60));
//                    } else {
//                        time = LocaleController.formatPluralString("Days", (int) (dt / 60 / 60 / 24));
//                    }
//                    statusTextView.setText("Updated " + LocaleController.formatString("AutoLockInTime", R.string.AutoLockInTime, time));
//                }
//            }
//        }
//        if (schedule) {
//            AndroidUtilities.runOnUIThread(updateTimeRunnable, 60 * 1000);
//        }
//    }
//
//    private ArrayList<Integer> reqIds= new ArrayList<>();
//    private class NestedFrameLayout extends SizeNotifierFrameLayout implements NestedScrollingParent3 {
//
//        private NestedScrollingParentHelper nestedScrollingParentHelper;
//
//        public NestedFrameLayout(Context context) {
//            super(context);
//            nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
//        }
//
//        @Override
//        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
//            if (target == listView && shopMediaLayoutAttached) {
//                RecyclerListView innerListView = shopMediaLayout.getCurrentListView();
//                int top = shopMediaLayout.getTop();
//                if (top == 0) {
//                    consumed[1] = dyUnconsumed;
//                    innerListView.scrollBy(0, dyUnconsumed);
//                }
//            }
//        }
//
//        @Override
//        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
//
//        }
//
//        @Override
//        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
//            return super.onNestedPreFling(target, velocityX, velocityY);
//        }
//
//        @Override
//        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
//            if (target == listView && productRow != -1 && shopMediaLayoutAttached) {
//                boolean searchVisible = actionBar.isSearchFieldVisible();
//                int t = shopMediaLayout.getTop();
//                if (dy < 0) {
//                    boolean scrolledInner = false;
//                    if (t <= 0) {
//                        RecyclerListView innerListView = shopMediaLayout.getCurrentListView();
//                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) innerListView.getLayoutManager();
//                        int pos = linearLayoutManager.findFirstVisibleItemPosition();
//                        if (pos != RecyclerView.NO_POSITION) {
//                            RecyclerView.ViewHolder holder = innerListView.findViewHolderForAdapterPosition(pos);
//                            int top = holder != null ? holder.itemView.getTop() : -1;
//                            int paddingTop = innerListView.getPaddingTop();
//                            if (top != paddingTop || pos != 0) {
//                                consumed[1] = pos != 0 ? dy : Math.max(dy, (top - paddingTop));
//                                innerListView.scrollBy(0, dy);
//                                scrolledInner = true;
//                            }
//                        }
//                    }
//                    if (searchVisible) {
//                        if (!scrolledInner && t < 0) {
//                            consumed[1] = dy - Math.max(t, dy);
//                        } else {
//                            consumed[1] = dy;
//                        }
//                    }
//                } else {
//                    if (searchVisible) {
//                        RecyclerListView innerListView = shopMediaLayout.getCurrentListView();
//                        consumed[1] = dy;
//                        if (t > 0) {
//                            consumed[1] -= Math.min(consumed[1], dy);
//                        }
//                        if (consumed[1] > 0) {
//                            innerListView.scrollBy(0, consumed[1]);
//                        }
//                    }
//                }
//            }
//        }
//
//        @Override
//        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
//            return productRow != -1 && axes == ViewCompat.SCROLL_AXIS_VERTICAL;
//        }
//
//        @Override
//        public void onNestedScrollAccepted(View child, View target, int axes, int type) {
//            nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
//        }
//
//        @Override
//        public void onStopNestedScroll(View target, int type) {
//            nestedScrollingParentHelper.onStopNestedScroll(target);
//        }
//
//        @Override
//        public void onStopNestedScroll(View child) {
//
//        }
//    }
//
//    @Override
//    protected ActionBar createActionBar(Context context) {
//        ActionBar actionBar = new ActionBar(context){
//            @Override
//            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                if (Build.VERSION.SDK_INT >= 21 && statusTextView != null) {
//                    LayoutParams layoutParams = (LayoutParams) statusTextView.getLayoutParams();
//                    layoutParams.topMargin = AndroidUtilities.statusBarHeight;
//                }
//                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//            }
//        };
//        actionBar.setWillNotDraw(false);
//        actionBar.setBackgroundColor(Theme.getColor(Theme.key_chats_actionBackground));
//        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
//        actionBar.setTitleColor(Theme.getColor(Theme.key_chats_actionIcon));
//        actionBar.setItemsColor(Theme.getColor(Theme.key_chats_actionIcon), false);
//        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_chats_actionPressedBackground), false);
//
//        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
//            @Override
//            public void onItemClick(int id) {
//                if (id == -1) {
//                    finishFragment();
//                }
//            }
//        });
//
//        ActionBarMenu menu = actionBar.createMenu();
//
//        statusTextView = new SimpleTextView(context);
//        statusTextView.setTextSize(14);
//        statusTextView.setGravity(Gravity.CENTER);
//        statusTextView.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
//        actionBar.addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.BOTTOM, 48, 0, 48, 0));
//
//        actionBar.setCastShadows(false);
//
//        return actionBar;
//    }
//
//    @Override
//    public boolean onFragmentCreate() {
////        loadAdd();
//        return super.onFragmentCreate();
//    }
//
//    @Override
//    public void onFragmentDestroy() {
//        if(shopMediaLayout != null){
//            shopMediaLayout.onDestroy();
//        }
//        super.onFragmentDestroy();
//    }
//
//    private SimpleTextView titleTextView;
//    private boolean isSwipeBackEnabled;
//
//    private Paint blackPaint = new Paint();
//    private GradientDrawable backgroundDrawable;
//    private float[] radii;
//
//
//    @Override
//    public View createView(Context context) {
//        updateRowsIds();
//        if (shopMediaLayout != null) {
//            shopMediaLayout.onDestroy();
//        }
//        shopMediaLayout = new ShopMediaLayoutProfile(context, this) {
//            @Override
//            protected void onSelectedTabChanged() {
//                isSwipeBackEnabled = shopMediaLayout.getSelectedTab() == 0;
//            }
//        };
//        shopMediaLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
//
//        Theme.createProfileResources(context);
//        pullForegroundDrawable = new PullForegroundDrawable("Swipe To Refresh" ,"Wallet Release To Refresh") {
//            @Override
//            protected float getViewOffset() {
//                return listView.getViewOffset();
//            }
//        };
//
//        pullForegroundDrawable.setColors(Theme.key_chats_archivePullDownBackground, Theme.key_actionBarDefault);
//        pullForegroundDrawable.showHidden();
//        pullForegroundDrawable.setWillDraw(true);
//
//        blackPaint.setColor(Theme.getColor(Theme.key_chats_actionBackground));
//        backgroundDrawable = new GradientDrawable();
//        backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
//        int r = AndroidUtilities.dp(13);
//        backgroundDrawable.setCornerRadii(radii = new float[] { r, r, r, r, 0, 0, 0, 0 });
//        backgroundDrawable.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//
//        fragmentView = new NestedFrameLayout(context) {
//
//
//            @Override
//            public boolean hasOverlappingRendering() {
//                return false;
//            }
//
//            @Override
//            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            }
//
//            @Override
//            protected void onLayout(boolean changed, int l, int t, int r, int b) {
//                super.onLayout(changed, l, t, r, b);
//                checkListViewScroll();
//                needLayout();
//            }
//
//            @Override
//            public void onDraw(Canvas c) {
//                int bottom;
//                RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(1);
//                if (holder != null) {
//                    bottom = holder.itemView.getBottom();
//                } else {
//                    bottom = 0;
//                }
//                float rad = AndroidUtilities.dp(13);
//                if (bottom < rad) {
//                    rad *= bottom / rad;
//                }
//                bottom += viewOffset;
//                radii[0] = radii[1] = radii[2] = radii[3] = rad;
//                c.drawRect(0, 0, getMeasuredWidth(), bottom + AndroidUtilities.dp(6), blackPaint);
//                backgroundDrawable.setBounds(0, bottom - AndroidUtilities.dp(7), getMeasuredWidth(), getMeasuredHeight());
//                backgroundDrawable.draw(c);
//            }
//
//        };
//        fragmentView.setWillNotDraw(false);
//        SizeNotifierFrameLayout frameLayout = (SizeNotifierFrameLayout) fragmentView;
//
//        listAdapter = new ListAdapter(context);
//        listView = new PullRecyclerView(context);
//        listView.setItemAnimator(null);
//        listView.setAdapter(listAdapter);
//        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
//            @Override
//            public boolean supportsPredictiveItemAnimations() {
//                return false;
//            }
//
//            @Override
//            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
//                boolean isDragging = listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING;
//
//                int measuredDy = dy;
//                if (dy < 0) {
//                    listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
//                    int currentPosition = layoutManager.findFirstVisibleItemPosition();
//                    if (currentPosition == 0) {
//                        View view = layoutManager.findViewByPosition(currentPosition);
//                        if (view != null && view.getBottom() <= AndroidUtilities.dp(1)) {
//                            currentPosition = 1;
//                        }
//                    }
//                    if (!isDragging) {
//                        View view = layoutManager.findViewByPosition(currentPosition);
//
//                        int dialogHeight = AndroidUtilities.dp(72) + 1;
//                        int canScrollDy = -view.getTop() + (currentPosition - 1) * dialogHeight;
//                        int positiveDy = Math.abs(dy);
//                        if (canScrollDy < positiveDy) {
//                            measuredDy = -canScrollDy;
//                        }
//                    } else if (currentPosition == 0) {
//                        View v = layoutManager.findViewByPosition(currentPosition);
//                        float k = 1f + (v.getTop() / (float) v.getMeasuredHeight());
//                        if (k > 1f) {
//                            k = 1f;
//                        }
//                        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
//                        measuredDy *= PullForegroundDrawable.startPullParallax - PullForegroundDrawable.endPullParallax * k;
//                        if (measuredDy > -1) {
//                            measuredDy = -1;
//                        }
//                    }
//                }
//
//                if (viewOffset != 0 && dy > 0 && isDragging) {
//                    float ty = (int) viewOffset;
//                    ty -= dy;
//                    if (ty < 0) {
//                        measuredDy = (int) ty;
//                        ty = 0;
//                    } else {
//                        measuredDy = 0;
//                    }
//                    listView.setViewsOffset(ty);
//                }
//
//                int usedDy = super.scrollVerticallyBy(measuredDy, recycler, state);
//                if (pullForegroundDrawable != null) {
//                    pullForegroundDrawable.scrollDy = usedDy;
//                }
//                int currentPosition = layoutManager.findFirstVisibleItemPosition();
//                View firstView = null;
//                if (currentPosition == 0) {
//                    firstView = layoutManager.findViewByPosition(currentPosition);
//                }
//                if (currentPosition == 0 && firstView != null && firstView.getBottom() >= AndroidUtilities.dp(4)) {
//                    if (startArchivePullingTime == 0) {
//                        startArchivePullingTime = System.currentTimeMillis();
//                    }
//                    if (pullForegroundDrawable != null) {
//                        pullForegroundDrawable.showHidden();
//                    }
//                    float k = 1f + (firstView.getTop() / (float) firstView.getMeasuredHeight());
//                    if (k > 1f) {
//                        k = 1f;
//                    }
//                    long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
//                    boolean canShowInternal = k > PullForegroundDrawable.SNAP_HEIGHT && pullingTime > PullForegroundDrawable.minPullingTime + 20;
//                    if (canShowHiddenPull != canShowInternal) {
//                        canShowHiddenPull = canShowInternal;
//                        if (!wasPulled) {
//                            listView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
//                            if (pullForegroundDrawable != null) {
//                                pullForegroundDrawable.colorize(canShowInternal);
//                            }
//                        }
//                    }
//                    if (measuredDy - usedDy != 0 && dy < 0 && isDragging) {
//                        float ty;
//                        float tk = (viewOffset / PullForegroundDrawable.getMaxOverscroll());
//                        tk = 1f - tk;
//                        ty = (viewOffset - dy * PullForegroundDrawable.startPullOverScroll * tk);
//                        listView.setViewsOffset(ty);
//                    }
//                    if (pullForegroundDrawable != null) {
//                        pullForegroundDrawable.pullProgress = k;
//                        pullForegroundDrawable.setListView(listView);
//                    }
//                } else {
//                    startArchivePullingTime = 0;
//                    canShowHiddenPull = false;
//                    if (pullForegroundDrawable != null) {
//                        pullForegroundDrawable.resetText();
//                        pullForegroundDrawable.pullProgress = 0f;
//                        pullForegroundDrawable.setListView(listView);
//                    }
//                }
//                if (firstView != null) {
//                    firstView.invalidate();
//                }
//                return usedDy;
//            }
//        });
//        listView.setGlowColor(Theme.getColor(Theme.key_dialogScrollGlow));
//        listView.setLayoutAnimation(null);
//        listView.setVerticalScrollBarEnabled(false);
//        listView.setClipToPadding(false);
//        listView.setEmptyView(null);
//        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                fragmentView.invalidate();
//                checkListViewScroll();
//            }
//        });
//        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//
//
//        return fragmentView;
//    }
//
//    private void removeReqId(int reqId){
//        int index = reqIds.indexOf(reqId);
//        if(index != -1){
//            reqIds.remove(index);
//        }
//    }
//
//    private void loadAdd() {
////        final int[] finalReqId= {0};
////        finalReqId[0] =  ServicesDataController.getInstance(currentAccount).loadAdd((response, apiError) -> {
////            removeReqId(finalReqId[0]);
////            if(apiError == null){
////                merchantBalances = (ServicesModel.EscrowBalance)response;
////                if(balanceCell != null){
////                    balanceCell.setBalance(merchantBalances);
////                }else{
////                    updateRowsIds();
////                }
////                if(shopMediaLayout != null){
////                    shopMediaLayout.forceRefreshTransactions();
////                }
////                lastUpdateTime = getConnectionsManager().getCurrentTime();
////                updateTime(true);
////            }
////        });
////        reqIds.add(finalReqId[0]);
//    }
//
//    private void needLayout() {
//
//    }
//
//    private void checkListViewScroll() {
//        if (listView.getChildCount() <= 0) {
//            return;
//        }
//        if (shopMediaLayoutAttached) {
//            shopMediaLayout.setVisibleHeight(listView.getMeasuredHeight() - shopMediaLayout.getTop());
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (shopMediaLayout != null) {
//            shopMediaLayout.onResume();
//        }
//        if (listAdapter != null) {
//            listAdapter.notifyDataSetChanged();
//        }
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//    }
//
//    private AlertDialog progressDialog;
//    private void showErrorAlert(String message,boolean dialog){
//        if(dialog){
//            showDialog(AlertsCreator.showSimpleAlert(GameListFragment.this,message));
//        }else{
//            BulletinFactory.of((NestedFrameLayout)fragmentView,null).createSimpleBulletin(R.raw.error,message).show();
//        }
//    }
//
//    private class ListAdapter extends RecyclerListView.SelectionAdapter {
//
//        private Context mContext;
//
//        public ListAdapter(Context mContext) {
//            this.mContext = mContext;
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder) {
//            return holder.getItemViewType() == 4;
//        }
//
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view = null;
//            switch (viewType) {
//
//                case 1: {
//                    view = new ShadowSectionCell(mContext);
//                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
//                    break;
//                }
//                case 11: {
//                    view = new View(mContext) {
//                        @Override
//                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
//                        }
//                    };
//                    break;
//                }
//                case 3: {
//                    if (shopMediaLayout.getParent() != null) {
//                        ((ViewGroup) shopMediaLayout.getParent()).removeView(shopMediaLayout);
//                    }
//                    view = shopMediaLayout;
//                    break;
//                }
//                case 5:
//                    view = new TextCell(mContext);
//                    break;
//                case 6:
//                    view = new HeaderCell(mContext);
//                    break;
//                case 7:
//                    TextInfoPrivacyCell textInfoPrivacyCell = new TextInfoPrivacyCell(mContext);
//                    view = textInfoPrivacyCell;
//                    break;
//                case 12: {
//                    view = new View(mContext) {
//                        @Override
//                        protected void onDraw(Canvas canvas) {
//                            pullForegroundDrawable.draw(canvas);
//                        }
//
//                        @Override
//                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(AndroidUtilities.dp(72)), MeasureSpec.EXACTLY));
//                        }
//                    };
//                    pullForegroundDrawable.setCell(view);
//                    break;
//                }
//                case 13:
//                    view = balanceCell = new BalanceCell(mContext);
//                    break;
//                case 14: {
//                    view = paddingView = new View(mContext) {
//                        @Override
//                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                            int n = listView.getChildCount();
//                            int itemsCount = listAdapter.getItemCount();
//                            int totalHeight = 0;
//                            for (int i = 0; i < n; i++) {
//                                View view = listView.getChildAt(i);
//                                int pos = listView.getChildAdapterPosition(view);
//                                if (pos != 0 && pos != itemsCount - 1) {
//                                    totalHeight += listView.getChildAt(i).getMeasuredHeight();
//                                }
//                            }
//                            int paddingHeight = fragmentView.getMeasuredHeight() - totalHeight;
//                            if (paddingHeight <= 0) {
//                                paddingHeight = 0;
//                            }
//                            setMeasuredDimension(listView.getMeasuredWidth(), paddingHeight);
//                        }
//                    };
//                }
//                break;
//                default:
//                    view = new EmptyCell(mContext);
//                    break;
//            }
//            return new RecyclerListView.Holder(view);
//
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//          if(holder.getItemViewType() == 9){
//                BalanceCell balanceCell = (BalanceCell) holder.itemView;
////                if(merchantBalances != null){
////                    balanceCell.setBalance(merchantBalances);
////                }
//            } else if(holder.getItemViewType() == 7){
//                if(position == productSecRow){
//                    TextInfoPrivacyCell textInfoPrivacyCell =(TextInfoPrivacyCell)holder.itemView;
//                    textInfoPrivacyCell.setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray));
//                    textInfoPrivacyCell.setText(AndroidUtilities.replaceTags("All of your **Sales** from all of your **Shops**  will be shown here."));
//
//                }
//            }
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            if ( position == shopActionSecRow) {
//                return 1;
//            } else if (position == productRow) {
//                return 3;
//            } else  if (position == gridItemsRow) {
//                return 8;
//            } else if(position == balanceRow){
//                return 13;
//            }else if(position == paddingRow) {
//                return 12;
//            }else if(position == emptyRow){
//                return 14;
//            }else if(position == buttonRow){
//                return 15;
//            }else if(position == productSecRow){
//                return 7;
//            }
//            return super.getItemViewType(position);
//        }
//
//        @Override
//        public int getItemCount() {
//            return rowCount;
//        }
//
//        @Override
//        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
//            if (holder.itemView == shopMediaLayout) {
//                shopMediaLayoutAttached = true;
//            }
//        }
//
//
//        @Override
//        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
//            if (holder.itemView == shopMediaLayout) {
//                shopMediaLayoutAttached = false;
//            }
//        }
//    }
//
//
//
//    private static class BalanceCell extends FrameLayout {
//
//        private Typeface defaultTypeFace;
//        private SimpleTextView valueTextView;
//        private TextView yourBalanceTextView;
//
//
//        public BalanceCell(Context context) {
//            super(context);
//
//            valueTextView = new SimpleTextView(context);
//            valueTextView.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
//            valueTextView.setTextSize(41);
//            valueTextView.setDrawablePadding(AndroidUtilities.dp(7));
//            valueTextView.setGravity(Gravity.CENTER_HORIZONTAL);
//            valueTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 16, 35, 0, 0));
//
//            yourBalanceTextView = new TextView(context);
//            yourBalanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//            yourBalanceTextView.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
//            defaultTypeFace = yourBalanceTextView.getTypeface();
//            addView(yourBalanceTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 16, 90, 0, 0));
//
//            setWillNotDraw(false);
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            int width = MeasureSpec.getSize(widthMeasureSpec);
//            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(168), MeasureSpec.EXACTLY));
//        }
//
//
////        public void setBalance(ServicesModel.EscrowBalance escrowBalance) {
////            if (escrowBalance != null) {
////                SpannableStringBuilder stringBuilder = new SpannableStringBuilder(WalletUtils.formatCurrency(escrowBalance.balance));
////                int index = TextUtils.indexOf(stringBuilder, '.');
////                if (index >= 0) {
////                    stringBuilder.setSpan(new TypefaceSpan(defaultTypeFace, AndroidUtilities.dp(27)), index + 1, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
////                }
////                valueTextView.setText(stringBuilder);
////                valueTextView.setTranslationX(0);
////                yourBalanceTextView.setVisibility(VISIBLE);
////                if ( escrowBalance.escrowed_balance > 0) {
////                    String str = LocaleController.formatString("WalletLockedBalance", R.string.WalletLockedBalance, WalletUtils.formatCurrency(escrowBalance.escrowed_balance ));
////                    SpannableStringBuilder builder = new SpannableStringBuilder(str);
////                    int idx = str.indexOf('*');
////                    if (idx >= 0) {
////                        builder.setSpan(new ImageSpan(getContext(), R.drawable.ic_lock_header, DynamicDrawableSpan.ALIGN_BOTTOM) {
////                            @Override
////                            public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
////                                Drawable b = getDrawable();
////                                canvas.save();
////                                int transY = (bottom - top) / 2 - b.getBounds().height() / 2;
////                                canvas.translate(x, transY);
////                                b.draw(canvas);
////                                canvas.restore();
////                            }
////                        }, idx, idx + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
////                    }
////                    yourBalanceTextView.setText(builder);
////                } else {
////                    yourBalanceTextView.setText(LocaleController.getString("WalletYourBalance", R.string.WalletYourBalance));
////
////                }
////            } else {
////                valueTextView.setText("");
////                valueTextView.setTranslationX(-AndroidUtilities.dp(4));
////                yourBalanceTextView.setVisibility(GONE);
////            }
////
//////            int visibility = balance <= 0 ? GONE : VISIBLE;
//////            if (sendButton.getVisibility() != visibility) {
//////                sendButton.setVisibility(visibility);
//////            }
////        }
//
//
//
//
//    }
//
//
//}
