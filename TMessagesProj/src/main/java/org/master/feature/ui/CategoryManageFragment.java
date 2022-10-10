package org.master.feature.ui;//package org.master.feature.ui;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.SharedPreferences;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.PorterDuff;
//import android.graphics.PorterDuffColorFilter;
//import android.graphics.drawable.Drawable;
//import android.text.TextPaint;
//import android.text.TextUtils;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.recyclerview.widget.DefaultItemAnimator;
//import androidx.recyclerview.widget.ItemTouchHelper;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import org.master.feature.categories.CategoryManager;
//import org.master.feature.database.Category;
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.Emoji;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.R;
//import org.telegram.ui.ActionBar.ActionBar;
//import org.telegram.ui.ActionBar.AlertDialog;
//import org.telegram.ui.ActionBar.BaseFragment;
//import org.telegram.ui.ActionBar.SimpleTextView;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.ActionBar.ThemeDescription;
//import org.telegram.ui.Cells.HeaderCell;
//import org.telegram.ui.Cells.ShadowSectionCell;
//import org.telegram.ui.Components.CombinedDrawable;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.RLottieImageView;
//import org.telegram.ui.Components.RecyclerListView;
//
//import java.util.ArrayList;
//
//
//public class CategoryManageFragment extends BaseFragment {
//
//    private RecyclerListView listView;
//    private ListAdapter adapter;
//    private ItemTouchHelper itemTouchHelper;
//
//    private boolean orderChanged;
//
//    private int filterHelpRow;
//    private int filtersHeaderRow;
//    private int filtersStartRow;
//    private int filtersEndRow;
//    private int createFilterRow;
//    private int createSectionRow;
//    private int rowCount = 0;
//
//    private boolean ignoreUpdates;
//
//    private ArrayList<Category> categoryArrayList = new ArrayList<>();
//
//    public static class TextCell extends FrameLayout {
//
//        private SimpleTextView textView;
//        private ImageView imageView;
//
//        public TextCell(Context context) {
//            super(context);
//
//            textView = new SimpleTextView(context);
//            textView.setTextSize(16);
//            textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
//            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
//            textView.setTag(Theme.key_windowBackgroundWhiteBlueText2);
//            addView(textView);
//
//            imageView = new ImageView(context);
//            imageView.setScaleType(ImageView.ScaleType.CENTER);
//            addView(imageView);
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            int width = MeasureSpec.getSize(widthMeasureSpec);
//            int height = AndroidUtilities.dp(48);
//
//            textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(71 + 23), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
//            imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY));
//            setMeasuredDimension(width, AndroidUtilities.dp(50));
//        }
//
//        @Override
//        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//            int height = bottom - top;
//            int width = right - left;
//
//            int viewLeft;
//            int viewTop = (height - textView.getTextHeight()) / 2;
//            if (LocaleController.isRTL) {
//                viewLeft = getMeasuredWidth() - textView.getMeasuredWidth() - AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 64 : 23);
//            } else {
//                viewLeft = AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 64 : 23);
//            }
//            textView.layout(viewLeft, viewTop, viewLeft + textView.getMeasuredWidth(), viewTop + textView.getMeasuredHeight());
//
//            viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(20) : width - imageView.getMeasuredWidth() - AndroidUtilities.dp(20);
//            imageView.layout(viewLeft, 0, viewLeft + imageView.getMeasuredWidth(), imageView.getMeasuredHeight());
//        }
//
//        public void setTextAndIcon(String text, Drawable icon, boolean divider) {
//            textView.setText(text);
//            imageView.setImageDrawable(icon);
//        }
//    }
//
//
//    @SuppressWarnings("FieldCanBeLocal")
//    public static class HintInnerCell extends FrameLayout {
//
//        private RLottieImageView imageView;
//        private TextView messageTextView;
//
//        public HintInnerCell(Context context) {
//            super(context);
//
//            imageView = new RLottieImageView(context);
//            imageView.setAnimation(R.raw.filters, 90, 90);
//            imageView.setScaleType(ImageView.ScaleType.CENTER);
//            imageView.playAnimation();
//            addView(imageView, LayoutHelper.createFrame(90, 90, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 14, 0, 0));
//            imageView.setOnClickListener(v -> {
//                if (!imageView.isPlaying()) {
//                    imageView.setProgress(0.0f);
//                    imageView.playAnimation();
//                }
//            });
//
//            messageTextView = new TextView(context);
//            messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
//            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//            messageTextView.setGravity(Gravity.CENTER);
//            messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.formatString("CreateNewFilterInfo", R.string.CreateNewFilterInfo)));
//            addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 40, 121, 40, 24));
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), heightMeasureSpec);
//        }
//    }
//
//    public static class FilterCell extends FrameLayout {
//
//        private TextView textView;
//        private TextView valueTextView;
//        @SuppressWarnings("FieldCanBeLocal")
//        private ImageView moveImageView;
//        @SuppressWarnings("FieldCanBeLocal")
//        private ImageView optionsImageView;
//        private boolean needDivider;
//
//        private Category currentCategory;
//
//        public FilterCell(Context context) {
//            super(context);
//            setWillNotDraw(false);
//
//            moveImageView = new ImageView(context);
//            moveImageView.setFocusable(false);
//            moveImageView.setScaleType(ImageView.ScaleType.CENTER);
//            moveImageView.setImageResource(R.drawable.list_reorder);
//            moveImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_stickers_menu), PorterDuff.Mode.MULTIPLY));
//            moveImageView.setContentDescription(LocaleController.getString("FilterReorder", R.string.FilterReorder));
//            moveImageView.setClickable(true);
//            addView(moveImageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 6, 0, 6, 0));
//
//            textView = new TextView(context);
//            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
//            textView.setLines(1);
//            textView.setMaxLines(1);
//            textView.setSingleLine(true);
//            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
//            textView.setEllipsize(TextUtils.TruncateAt.END);
//            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 80 : 64, 14, LocaleController.isRTL ? 64 : 80, 0));
//
//            valueTextView = new TextView(context);
//            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
//            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
//            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
//            valueTextView.setLines(1);
//            valueTextView.setMaxLines(1);
//            valueTextView.setSingleLine(true);
//            valueTextView.setPadding(0, 0, 0, 0);
//            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
//            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 80 : 64, 35, LocaleController.isRTL ? 64 : 80, 0));
//            valueTextView.setVisibility(GONE);
//
//            optionsImageView = new ImageView(context);
//            optionsImageView.setFocusable(false);
//            optionsImageView.setScaleType(ImageView.ScaleType.CENTER);
//            optionsImageView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_stickers_menuSelector)));
//            optionsImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_stickers_menu), PorterDuff.Mode.MULTIPLY));
//            optionsImageView.setImageResource(R.drawable.msg_actions);
//            optionsImageView.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
//            addView(optionsImageView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 6, 0, 6, 0));
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY));
//        }
//
//        public void setCatgory(Category category.json, boolean divider) {
//            currentCategory = category.json;
//
//
//            textView.setText(Emoji.replaceEmoji(category.json.title, textView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20), false));
//            //valueTextView.setText("");
//            //valueTextView.setVisibility(VISIBLE);
//            needDivider = divider;
//        }
//
//        public Category getCurrentCategory() {
//            return currentCategory;
//        }
//
//        public void setOnOptionsClick(OnClickListener listener) {
//            optionsImageView.setOnClickListener(listener);
//        }
//
//        @Override
//        protected void onDraw(Canvas canvas) {
//            if (needDivider) {
//                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(62), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(62) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
//            }
//        }
//
//        @SuppressLint("ClickableViewAccessibility")
//        public void setOnReorderButtonTouchListener(OnTouchListener listener) {
//            moveImageView.setOnTouchListener(listener);
//        }
//    }
//
//    @Override
//    public boolean onFragmentCreate() {
//        updateRows(true);
//        //CategoryManager.loadTabList(true);
//        //categoryArrayList = CategoryManager.categories;
//        return true;
//
//    }
//
//
//    private void updateRows(boolean notify) {
//
//        rowCount = 0;
//        filterHelpRow = rowCount++;
//        int count = categoryArrayList.size();
//
//        if (count != 0) {
//            filtersHeaderRow = rowCount++;
//            filtersStartRow = rowCount;
//            rowCount += count;
//            filtersEndRow = rowCount;
//        } else {
//            filtersHeaderRow = -1;
//            filtersStartRow = -1;
//            filtersEndRow = -1;
//        }
//        if (count < CategoryManager.MAX_CATEGORY) {
//            createFilterRow = rowCount++;
//        } else {
//            createFilterRow = -1;
//        }
//
//        createSectionRow = rowCount++;
//        if (notify && adapter != null) {
//            adapter.notifyDataSetChanged();
//        }
//    }
//
//    @Override
//    public void onFragmentDestroy() {
//        if (orderChanged) {
//           //  CategoryManager.saveTabList();
//        }
//        super.onFragmentDestroy();
//    }
//
//    @Override
//    public View createView(Context context) {
//        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
//        actionBar.setAllowOverlayTitle(true);
//        actionBar.setTitle("Category");
//        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
//            @Override
//            public void onItemClick(int id) {
//                if (id == -1) {
//                    finishFragment();
//                }
//            }
//        });
//
//        fragmentView = new FrameLayout(context);
//        FrameLayout frameLayout = (FrameLayout) fragmentView;
//        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
//
//        listView = new RecyclerListView(context);
//        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
//        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
//        listView.setVerticalScrollBarEnabled(false);
//        itemTouchHelper = new ItemTouchHelper(new TouchHelperCallback());
//        itemTouchHelper.attachToRecyclerView(listView);
//        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//        listView.setAdapter(adapter = new ListAdapter(context));
//        listView.setOnItemClickListener((view, position, x, y) -> {
//            if (position >= filtersStartRow && position < filtersEndRow) {
//               // presentFragment(new FilterCreateActivity(getMessagesController().dialogFilters.get(position - filtersStartRow)));
//            } else if (position == createFilterRow) {
//               // presentFragment(new FilterCreateActivity());
//            }
//        });
//
//        updateRows(true);
//
//        return fragmentView;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (adapter != null) {
//            adapter.notifyDataSetChanged();
//        }
//    }
//
//
//    private class ListAdapter extends RecyclerListView.SelectionAdapter {
//
//        private Context mContext;
//
//        public ListAdapter(Context context) {
//            mContext = context;
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder) {
//            int type = holder.getItemViewType();
//            return type != 3 && type != 0 && type != 5 && type != 1;
//        }
//
//        @Override
//        public int getItemCount() {
//            return rowCount;
//        }
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            View view;
//            switch (viewType) {
//                case 0:
//                    view = new HeaderCell(mContext);
//                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//                    break;
//                case 1:
//                    view = new HintInnerCell(mContext);
//                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_top, Theme.key_windowBackgroundGrayShadow));
//                    break;
//                case 2:
//                    FilterCell filterCell = new FilterCell(mContext);
//                    filterCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//                    filterCell.setOnReorderButtonTouchListener((v, event) -> {
//                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                            itemTouchHelper.startDrag(listView.getChildViewHolder(filterCell));
//                        }
//                        return false;
//                    });
//                    filterCell.setOnOptionsClick(v -> {
//                        FilterCell cell = (FilterCell) v.getParent();
//                        Category category.json = cell.getCurrentCategory();
//                        AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity());
//                        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
//                        paint.setTextSize(AndroidUtilities.dp(20));
//                        builder1.setTitle(Emoji.replaceEmoji(category.json.title, paint.getFontMetricsInt(), AndroidUtilities.dp(20), false));
//                        final CharSequence[] items = new CharSequence[]{
//                                LocaleController.getString("FilterEditItem", R.string.FilterEditItem),
//                                LocaleController.getString("FilterDeleteItem", R.string.FilterDeleteItem),
//                        };
//                        final int[] icons = new int[]{
//                                R.drawable.msg_edit,
//                                R.drawable.msg_delete
//                        };
//                        builder1.setItems(items, icons, (dialog, which) -> {
//                            if (which == 0) {
//
//                               // presentFragment(new FilterCreateActivity(filter));
//                            } else if (which == 1) {
//                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
//                                builder.setTitle(LocaleController.getString("CategoryDelete", R.string.CategoryDelete));
//                                builder.setMessage(LocaleController.getString("CategoryDeleteAlert", R.string.CategoryDeleteAlert));
//                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
//                                builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog2, which2) -> {
//                                    CategoryManager.deleteCategories(category.json, new Runnable() {
//                                        @Override
//                                        public void run() {
//                                           // categoryArrayList = CategoryManager.categories;
//                                            updateRows(true);
//                                            Toast.makeText(getParentActivity(),"Cateogry remoed!",Toast.LENGTH_LONG).show();
//                                        }
//                                    });
//
//                                });
//                                AlertDialog alertDialog = builder.create();
//                                showDialog(alertDialog);
//                                TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
//                                if (button != null) {
//                                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
//                                }
//                            }
//                        });
//                        final AlertDialog dialog = builder1.create();
//                        showDialog(dialog);
//                        dialog.setItemColor(items.length - 1, Theme.getColor(Theme.key_dialogTextRed2), Theme.getColor(Theme.key_dialogRedIcon));
//                    });
//                    view = filterCell;
//                    break;
//                case 3:
//                    view = new ShadowSectionCell(mContext);
//                    break;
//                case 4:
//                    view = new TextCell(mContext);
//                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//                    break;
//                case 5:
//                default:
//                    view = new View(mContext);
//                    break;
//            }
//            return new RecyclerListView.Holder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//            switch (holder.getItemViewType()) {
//                case 0: {
//                    HeaderCell headerCell = (HeaderCell) holder.itemView;
//                    if (position == filtersHeaderRow) {
//                        headerCell.setText(LocaleController.getString("Category", R.string.Category));
//                    }
//                    break;
//                }
//                case 2: {
//                    FilterCell filterCell = (FilterCell) holder.itemView;
//                    filterCell.setCatgory(categoryArrayList.get(position - filtersStartRow), true);
//                    break;
//                }
//                case 3: {
//                    if (position == createSectionRow) {
//                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
//                    } else {
//                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
//                    }
//                    break;
//                }
//                case 4: {
//                    TextCell textCell = (TextCell) holder.itemView;
//                    SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
//                    if (position == createFilterRow) {
//                        Drawable drawable1 = mContext.getResources().getDrawable(R.drawable.poll_add_circle);
//                        Drawable drawable2 = mContext.getResources().getDrawable(R.drawable.poll_add_plus);
//                        drawable1.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_switchTrackChecked), PorterDuff.Mode.MULTIPLY));
//                        drawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_checkboxCheck), PorterDuff.Mode.MULTIPLY));
//                        CombinedDrawable combinedDrawable = new CombinedDrawable(drawable1, drawable2);
//
//                        textCell.setTextAndIcon(LocaleController.getString("CreateNewFilter", R.string.CreateNewFilter), combinedDrawable, false);
//                    }
//                    break;
//                }
//
//            }
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            if (position == filtersHeaderRow) {
//                return 0;
//            } else if (position == filterHelpRow) {
//                return 1;
//            } else if (position >= filtersStartRow && position < filtersEndRow) {
//                return 2;
//            } else if (position == createSectionRow) {
//                return 3;
//            } else if (position == createFilterRow) {
//                return 4;
//            } else {
//                return 5;
//            }
//        }
//
//        public void swapElements(int fromIndex, int toIndex) {
//            int idx1 = fromIndex - filtersStartRow;
//            int idx2 = toIndex - filtersStartRow;
//            int count = filtersEndRow - filtersStartRow;
//            if (idx1 < 0 || idx2 < 0 || idx1 >= count || idx2 >= count) {
//                return;
//            }
//            ArrayList<Category> filters = categoryArrayList;
//            Category category1 = filters.get(idx1);
//            Category category2 = filters.get(idx2);
//            int temp = category1.order;
//            category1.order = category2.order;
//            category2.order = temp;
//            filters.set(idx1, category2);
//            filters.set(idx2, category1);
//            orderChanged = true;
//            notifyItemMoved(fromIndex, toIndex);
//        }
//    }
//
//    public class TouchHelperCallback extends ItemTouchHelper.Callback {
//
//        @Override
//        public boolean isLongPressDragEnabled() {
//            return true;
//        }
//
//        @Override
//        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//            if (viewHolder.getItemViewType() != 2) {
//                return makeMovementFlags(0, 0);
//            }
//            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
//        }
//
//        @Override
//        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
//            if (source.getItemViewType() != target.getItemViewType()) {
//                return false;
//            }
//            adapter.swapElements(source.getAdapterPosition(), target.getAdapterPosition());
//            return true;
//        }
//
//        @Override
//        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
//            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
//        }
//
//        @Override
//        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
//            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
//                listView.cancelClickRunnables(false);
//                viewHolder.itemView.setPressed(true);
//            }
//            super.onSelectedChanged(viewHolder, actionState);
//        }
//
//        @Override
//        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//
//        }
//
//        @Override
//        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//            super.clearView(recyclerView, viewHolder);
//            viewHolder.itemView.setPressed(false);
//        }
//    }
//
//    @Override
//    public ArrayList<ThemeDescription> getThemeDescriptions() {
//        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
//
//        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
//
//        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
//        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
//        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
//        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
//        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
//
//        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
//
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));
//
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
//
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"moveImageView"}, null, null, null, Theme.key_stickers_menu));
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"optionsImageView"}, null, null, null, Theme.key_stickers_menu));
//        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{FilterCell.class}, new String[]{"optionsImageView"}, null, null, null, Theme.key_stickers_menuSelector));
//
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText2));
//        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_switchTrackChecked));
//        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_checkboxCheck));
//
//        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
//
//        return themeDescriptions;
//    }
//}
