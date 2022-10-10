package org.master.feature.tabs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

import it.owlgram.android.helpers.FolderIconHelper;

public class TabFilterView extends RecyclerListView {


    public static class FilterData{
        public String title;
        public int tab_id;
        public String emoticon;
        public int folder_id;
    }

    LinearLayoutManager layoutManager;
    private ArrayList<FilterData> usersFilters = new ArrayList<>();
    private ArrayList<FilterData> oldItems = new ArrayList<>();

    public TabFilterView(Context context,Theme.ResourcesProvider resourcesProvider) {
        super(context,resourcesProvider);

        layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull Recycler recycler, @NonNull State state, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(recycler, state, info);
                if (!isEnabled()) {
                    info.setVisibleToUser(false);
                }
            }
        };
        layoutManager.setOrientation(HORIZONTAL);
        setLayoutManager(layoutManager);
        setAdapter(new Adapter());
        addItemDecoration(new ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int position = parent.getChildAdapterPosition(view);
                outRect.left = AndroidUtilities.dp(8);
                if (position == state.getItemCount() - 1) {
                    outRect.right = AndroidUtilities.dp(10);
                }
                if (position == 0) {
                    outRect.left = AndroidUtilities.dp(10);
                }
            }
        });
        setItemAnimator(new DefaultItemAnimator() {

            private final float scaleFrom = 0;

            @Override
            protected long getMoveAnimationDelay() {
                return 0;
            }

            @Override
            protected long getAddAnimationDelay(long removeDuration, long moveDuration, long changeDuration) {
                return 0;
            }

            @Override
            public long getMoveDuration() {
                return 220;
            }

            @Override
            public long getAddDuration() {
                return 220;
            }

            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                boolean r = super.animateAdd(holder);
                if (r) {
                    holder.itemView.setScaleX(scaleFrom);
                    holder.itemView.setScaleY(scaleFrom);
                }
                return r;
            }

            @Override
            public void animateAddImpl(RecyclerView.ViewHolder holder) {
                final View view = holder.itemView;
                final ViewPropertyAnimator animation = view.animate();
                mAddAnimations.add(holder);
                animation.alpha(1).scaleX(1f).scaleY(1f).setDuration(getAddDuration())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                dispatchAddStarting(holder);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                                view.setAlpha(1);
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                animation.setListener(null);
                                dispatchAddFinished(holder);
                                mAddAnimations.remove(holder);
                                dispatchFinishedWhenDone();
                            }
                        }).start();
            }

            @Override
            protected void animateRemoveImpl(RecyclerView.ViewHolder holder) {
                final View view = holder.itemView;
                final ViewPropertyAnimator animation = view.animate();
                mRemoveAnimations.add(holder);
                animation.setDuration(getRemoveDuration()).alpha(0).scaleX(scaleFrom).scaleY(scaleFrom).setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                dispatchRemoveStarting(holder);
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                animation.setListener(null);
                                view.setAlpha(1);
                                view.setTranslationX(0);
                                view.setTranslationY(0);
                                view.setScaleX(1f);
                                view.setScaleY(1f);
                                dispatchRemoveFinished(holder);
                                mRemoveAnimations.remove(holder);
                                dispatchFinishedWhenDone();
                            }
                        }).start();
            }
        });
        setWillNotDraw(false);
        setHideIfEmpty(false);
        setSelectorRadius(AndroidUtilities.dp(28));
        setSelectorDrawableColor(getThemedColor(Theme.key_listSelector));
        setBackgroundColor(Theme.getColor(Theme.key_player_background));
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(36), MeasureSpec.EXACTLY));
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawRect(0, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight(), Theme.dividerPaint);
    }



    public void setUsersAndDates(ArrayList<FilterData> filterData) {
        oldItems.clear();
        oldItems.addAll(usersFilters);
        usersFilters.clear();
        if (filterData != null) {
            for (int i = 0; i < filterData.size(); i++) {
                FilterData data = filterData.get(i);
                String title = data.title;
                if (data.title.length() > 12) {
                    title = String.format("%s...", title.substring(0, 10));
                }
                data.title = title;
                usersFilters.add(data);
            }
        }

        if (getAdapter() != null) {
            UpdateCallback updateCallback = new UpdateCallback(getAdapter());
            DiffUtil.calculateDiff(diffUtilsCallback).dispatchUpdatesTo(updateCallback);
            if (!usersFilters.isEmpty() && updateCallback.changed) {
                layoutManager.scrollToPositionWithOffset(0, 0);
            }
        }


    }



    public FilterData getFilterAt(int i) {
        return usersFilters.get(i);
    }

    public void clearSelection(){
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof FilterView) {
                ((FilterView) view).setSelectedForDelete(false);
            }
        }

        for (int i = 0; i < getCachedChildCount(); i++) {
            View view = getCachedChildAt(i);
            if (view instanceof FilterView) {
                ((FilterView) view).setSelectedForDelete(false);
            }
        }

        for (int i = 0; i < getAttachedScrapChildCount(); i++) {
            View view = getAttachedScrapChildAt(i);
            if (view instanceof FilterView) {
                ((FilterView) view).setSelectedForDelete(false);
            }
        }
    }


    public void updateColors() {
        getRecycledViewPool().clear();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof FilterView) {
                ((FilterView) view).updateColors();
            }
        }

        for (int i = 0; i < getCachedChildCount(); i++) {
            View view = getCachedChildAt(i);
            if (view instanceof FilterView) {
                ((FilterView) view).updateColors();
            }
        }

        for (int i = 0; i < getAttachedScrapChildCount(); i++) {
            View view = getAttachedScrapChildAt(i);
            if (view instanceof FilterView) {
                ((FilterView) view).updateColors();
            }
        }
        setSelectorDrawableColor(getThemedColor(Theme.key_listSelector));
        setBackgroundColor(Theme.getColor(Theme.key_player_background));
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewHolder holder = new ViewHolder(new FilterView(parent.getContext(), resourcesProvider));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(24));
            lp.topMargin = AndroidUtilities.dp(6);
            lp.bottomMargin = AndroidUtilities.dp(6);
            holder.itemView.setLayoutParams(lp);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FilterData data;
            data = usersFilters.get(position);
            ((ViewHolder) holder).filterView.setData(data);
        }

        @Override
        public int getItemCount() {
            return usersFilters.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }
    }

    DiffUtil.Callback diffUtilsCallback = new DiffUtil.Callback() {
        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return usersFilters.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            FilterData oldItem = oldItems.get(oldItemPosition);
            FilterData newItem = usersFilters.get(newItemPosition);
            return oldItem.tab_id == (newItem.tab_id);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return true;
        }
    };

    private class ViewHolder extends RecyclerListView.ViewHolder {

        FilterView filterView;

        public ViewHolder(@NonNull FilterView itemView) {
            super(itemView);
            filterView = itemView;
        }
    }

    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        arrayList.add(new ThemeDescription(this, 0, null, null, null, null, Theme.key_graySection));
        arrayList.add(new ThemeDescription(this, 0, null, null, null, null, Theme.key_graySectionText));
        return arrayList;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (!isEnabled()) {
            return false;
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) {
            return false;
        }
        return super.onTouchEvent(e);
    }

    private static class UpdateCallback implements ListUpdateCallback {

        final RecyclerView.Adapter adapter;
        boolean changed;

        private UpdateCallback(RecyclerView.Adapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onInserted(int position, int count) {
            changed = true;
            adapter.notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            changed = true;
            adapter.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            changed = true;
            adapter.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count, @Nullable Object payload) {
            adapter.notifyItemRangeChanged(position, count, payload);
        }
    }

    public static class FilterView extends FrameLayout {

        private final Theme.ResourcesProvider resourcesProvider;

        BackupImageView avatarImageView;
        TextView titleView;
        CombinedDrawable thumbDrawable;
        FilterData data;

        private boolean selectedForDelete;
        private float selectedProgress;
        ValueAnimator selectAnimator;

        Runnable removeSelectionRunnable = new Runnable() {
            @Override
            public void run() {
                if (selectedForDelete) {
                    setSelectedForDelete(false);
                }
            }
        };

        ImageView closeIconView;
        ShapeDrawable shapeDrawable;

        public FilterView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            avatarImageView = new BackupImageView(context);
            addView(avatarImageView, LayoutHelper.createFrame(18, 18, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

            closeIconView = new ImageView(context);
            closeIconView.setImageResource(R.drawable.ic_close_white);
            addView(closeIconView, LayoutHelper.createFrame(18, 18, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));


            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 36, 0, 16, 0));

            shapeDrawable = (ShapeDrawable) Theme.createRoundRectDrawable(AndroidUtilities.dp(18), 0xFF446F94);
            setBackground(shapeDrawable);
            updateColors();
        }

        private void updateColors() {
            int defaultBackgroundColor = getThemedColor(Theme.key_groupcreate_spanBackground);
            int selectedBackgroundColor = getThemedColor(Theme.key_avatar_backgroundBlue);
            int textDefaultColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
            int textSelectedColor = getThemedColor(Theme.key_avatar_actionBarIconBlue);
            shapeDrawable.getPaint().setColor(ColorUtils.blendARGB(defaultBackgroundColor, selectedBackgroundColor, selectedProgress));
            titleView.setTextColor(ColorUtils.blendARGB(textDefaultColor, textSelectedColor, selectedProgress));
            closeIconView.setColorFilter(textSelectedColor);

            closeIconView.setAlpha(selectedProgress);
            closeIconView.setScaleX(0.82f * selectedProgress);
            closeIconView.setScaleY(0.82f * selectedProgress);
            if (thumbDrawable != null) {
                Theme.setCombinedDrawableColor(thumbDrawable, getThemedColor(Theme.key_avatar_backgroundArchived), false);
                Theme.setCombinedDrawableColor(thumbDrawable, getThemedColor(Theme.key_avatar_actionBarIconBlue), true);
            }
            avatarImageView.setAlpha(1f - selectedProgress);

            if(data != null){
                setData(data);
            }
            invalidate();
        }
//        private void updateColors() {
//            setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(28), getThemedColor(Theme.key_groupcreate_spanBackground)));
//            titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
//            if (thumbDrawable != null) {
//                Theme.setCombinedDrawableColor(thumbDrawable, getThemedColor(Theme.key_avatar_backgroundArchived), false);
//                Theme.setCombinedDrawableColor(thumbDrawable, getThemedColor(Theme.key_avatar_actionBarIconBlue), true);
//            }
//        }



        public void setData(FilterData data) {
            this.data = data;
            avatarImageView.getImageReceiver().clearImage();
            thumbDrawable = Theme.createCircleDrawableWithIcon(AndroidUtilities.dp(18), FolderIconHelper.getTabIcon(data.emoticon));
            thumbDrawable.setIconSize(AndroidUtilities.dp(14), AndroidUtilities.dp(14));
            thumbDrawable.setCustomSize(AndroidUtilities.dp(18),AndroidUtilities.dp(18));
            Theme.setCombinedDrawableColor(thumbDrawable, getThemedColor(Theme.key_avatar_backgroundArchived), false);
            Theme.setCombinedDrawableColor(thumbDrawable, getThemedColor(Theme.key_avatar_actionBarIconBlue), true);
            avatarImageView.setImageDrawable(thumbDrawable);
            titleView.setText(data.title);
        }

        private int getThemedColor(String key) {
            Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
            return color != null ? color : Theme.getColor(key);
        }


        public void setExpanded(boolean expanded) {
            if (expanded) {
                titleView.setVisibility(View.VISIBLE);
            } else {
                titleView.setVisibility(View.GONE);
                setSelectedForDelete(false);
            }
        }

        public boolean isSelectedForDelete() {
            return selectedForDelete;
        }

        public void setSelectedForDelete(boolean select) {
            if (selectedForDelete == select) {
                return;
            }
            AndroidUtilities.cancelRunOnUIThread(removeSelectionRunnable);
            selectedForDelete = select;
            if (selectAnimator != null) {
                selectAnimator.removeAllListeners();
                selectAnimator.cancel();
            }
            selectAnimator = ValueAnimator.ofFloat(selectedProgress, select ? 1f : 0f);
            selectAnimator.addUpdateListener(valueAnimator -> {
                selectedProgress = (float) valueAnimator.getAnimatedValue();
                updateColors();
            });
            selectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    selectedProgress = select ? 1f : 0f;
                    updateColors();
                }
            });
            selectAnimator.setDuration(150).start();
            if (selectedForDelete) {
              //  AndroidUtilities.runOnUIThread(removeSelectionRunnable, 2000);
            }
        }

    }

}
