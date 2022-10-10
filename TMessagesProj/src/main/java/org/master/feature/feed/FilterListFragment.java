//
//import static org.telegram.ui.ChatActivity.MODE_BOOKMARK;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.graphics.Canvas;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Log;
//import android.util.SparseArray;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.recyclerview.widget.DefaultItemAnimator;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import org.telegram.SQLite.SQLiteCursor;
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.MessageObject;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.NotificationCenter;
//import org.telegram.messenger.R;
//import org.telegram.tgnet.TLRPC;
//import org.telegram.ui.ActionBar.ActionBar;
//import org.telegram.ui.ActionBar.BaseFragment;
//import org.telegram.ui.ActionBar.SimpleTextView;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.Cells.HeaderCell;
//import org.telegram.ui.Cells.ManageChatUserCell;
//import org.telegram.ui.Cells.RadioCell;
//import org.telegram.ui.Cells.ShadowSectionCell;
//import org.telegram.ui.ChatActivity;
//import org.telegram.ui.Components.BulletinFactory;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.ProgressButton;
//import org.telegram.ui.Components.RLottieImageView;
//import org.telegram.ui.Components.RecyclerListView;
//import org.telegram.ui.Components.StickerEmptyView;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Locale;
//
//public class FilterListFragment extends BaseFragment{
//
//    private RecyclerListView listView;
//    private ListAdapter adapter;
//
//
//    private int filterHelpRow;
//    private int filtersHeaderRow;
//    private int filtersStartRow;
//    private int filtersEndRow;
//    private int rowCount = 0;
//    private int filterEmptyRow;
//
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
//    public static class SuggestedFilterCell extends FrameLayout {
//
//        private TextView textView;
//        private TextView valueTextView;
//        private ProgressButton addButton;
//        private boolean needDivider;
//        private TLRPC.TL_dialogFilterSuggested suggestedFilter;
//
//        public SuggestedFilterCell(Context context) {
//            super(context);
//
//            textView = new TextView(context);
//            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
//            textView.setLines(1);
//            textView.setMaxLines(1);
//            textView.setSingleLine(true);
//            textView.setEllipsize(TextUtils.TruncateAt.END);
//            textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
//            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 22, 10, 22, 0));
//
//            valueTextView = new TextView(context);
//            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
//            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
//            valueTextView.setLines(1);
//            valueTextView.setMaxLines(1);
//            valueTextView.setSingleLine(true);
//            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
//            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
//            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 22, 35, 22, 0));
//
//            addButton = new ProgressButton(context);
//            addButton.setText(LocaleController.getString("Add", R.string.Add));
//            addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
//            addButton.setProgressColor(Theme.getColor(Theme.key_featuredStickers_buttonProgress));
//            addButton.setBackgroundRoundRect(Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed));
//            addView(addButton, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, 28, Gravity.TOP | Gravity.END, 0, 18, 14, 0));
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(64));
//            measureChildWithMargins(addButton, widthMeasureSpec, 0, heightMeasureSpec, 0);
//            measureChildWithMargins(textView, widthMeasureSpec, addButton.getMeasuredWidth(), heightMeasureSpec, 0);
//            measureChildWithMargins(valueTextView, widthMeasureSpec, addButton.getMeasuredWidth(), heightMeasureSpec, 0);
//        }
//
//        public void setFilter(TLRPC.TL_dialogFilterSuggested filter, boolean divider) {
//            needDivider = divider;
//            suggestedFilter = filter;
//            setWillNotDraw(!needDivider);
//
//            textView.setText(filter.filter.title);
//            valueTextView.setText(filter.description);
//        }
//
//        public TLRPC.TL_dialogFilterSuggested getSuggestedFilter() {
//            return suggestedFilter;
//        }
//
//        public void setAddOnClickListener(OnClickListener onClickListener) {
//            addButton.setOnClickListener(onClickListener);
//        }
//
//        @Override
//        protected void onDraw(Canvas canvas) {
//            if (needDivider) {
//                canvas.drawLine(0, getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, Theme.dividerPaint);
//            }
//        }
//    }
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
//
//    @Override
//    public boolean onFragmentCreate() {
//        updateRows(true);
//        return super.onFragmentCreate();
//    }
//
//    private void loadBookMarkedChats(){
//        getMessagesStorage().getStorageQueue().postRunnable(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    ArrayList<TLRPC.Chat> chats = new ArrayList<>();
//                    HashMap<Long,Integer> savedCountMap = new HashMap<>();
//                    ArrayList<Long> uids = new ArrayList<>();
//
//                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT uid FROM chat_saved WHERE 1");
//
//                    while (cursor.next()) {
//
//                        long userId = cursor.intValue(0);
//                        Log.i("berhan test"," m  in e = " + "user id = " + userId);
//                        uids.add(-userId);
//                        SQLiteCursor cursor2 = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT count FROM chat_saved_count WHERE uid = %d", userId));
//                        int newCount2;
//                        if (cursor2.next()) {
//                            newCount2 = cursor2.intValue(0);
//                        } else {
//                            newCount2 = 0;
//                        }
//                        savedCountMap.put(-userId,newCount2);
//                        cursor2.dispose();
//                    }
//                    cursor.dispose();
//                    getMessagesStorage().getChatsInternal(TextUtils.join(",",uids),chats);
//                    Log.i("berhan test","udi size = = " + uids.size() + "chat size  = " + chats.size());
//
//                    AndroidUtilities.runOnUIThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            bookMarkChats = chats;
//                            savedCount = savedCountMap;
//                            updateRows(true);
//                        }
//                    });
//
//                } catch (Exception e) {
//                    Log.i("berhan test","expection in e = " + e.getMessage());
//                }
//
//            }
//        }) ;
//    }
//
//    private ArrayList<TLRPC.Chat> bookMarkChats = new ArrayList<>();
//    private HashMap<Long,Integer> savedCount = new HashMap<>();
//
//    private void updateRows(boolean notify) {
//
//        rowCount = 0;
//        filterHelpRow = -1;
//
//        int count = bookMarkChats.size();
//        if (count != 0) {
//            filtersHeaderRow =-1;
//            filtersStartRow = rowCount;
//            rowCount += count;
//            filtersEndRow = rowCount;
//        } else {
//            filtersHeaderRow = -1;
//            filtersStartRow = -1;
//            filtersEndRow = -1;
//        }
//        if(count == 0){
//            filterEmptyRow = rowCount++;
//        }else{
//            filterEmptyRow = -1;
//        }
//        if (notify && adapter != null) {
//            adapter.notifyDataSetChanged();
//        }
//    }
//
//    @Override
//    public void onFragmentDestroy() {
//        super.onFragmentDestroy();
//    }
//
//
//
//    @Override
//    public View createView(Context context) {
//        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
//        actionBar.setAllowOverlayTitle(true);
//        actionBar.setTitle("Bookmarked Messages");
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
//        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//        listView.setAdapter(adapter = new ListAdapter(context));
//        loadBookMarkedChats();
//        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
//            @Override
//            public void onItemClick(View view, int position) {
//                TLRPC.Chat chat = bookMarkChats.get(position - filtersStartRow);
//                Bundle bundle = new Bundle();
//                if (chat != null) {
//                    bundle.putLong("chat_id", chat.id);
//                } else {
////                    bundle.putLong("user_id", currentUser.id);
//                }
////              bundle.putInt("chatMode", MODE_BOOKMARK);
//                ChatActivity fragment = new ChatActivity(bundle);
//                fragment.openSavedMessage = true;
//                presentFragment(fragment);
//            }
//        });
//
//        return fragmentView;
//    }
//
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
//                case 3:
//                    view = new ShadowSectionCell(mContext);
//                    break;
//                case 4:
//                    view = new ManageChatUserCell(mContext,6,0,false);
//                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//                    break;
//                case 6:
//                    view = new RadioCell(mContext);
//                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//                    break;
//                case 5:
//                default:
//                    view = new StickerEmptyView(mContext,null,StickerEmptyView.STICKER_TYPE_NO_CONTACTS,null);
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
//                        headerCell.setText("Bookmarked Chats");
//                    }
//                    break;
//                }
//                case 4: {
//                    ManageChatUserCell userCell = (ManageChatUserCell) holder.itemView;
//                    Object object = bookMarkChats.get(position - filtersStartRow);
//                    if(object instanceof TLRPC.Chat){
//                        TLRPC.Chat chat = (TLRPC.Chat) object;
//                        Integer count = savedCount.get(chat.id);
//                        userCell.setData(chat, null, String.format("%s saved messages",count), true);
//                    }else if(object instanceof TLRPC.User){
//                        TLRPC.User user = (TLRPC.User) object;
//                        Integer count = savedCount.get(userCell.getUserId());
//                        userCell.setData(user, null,String.format("%s saved messages",count), true);
//
//                    }
//                    break;
//                }
//            }
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            if (position == filtersHeaderRow) {
//                return 0;
//            } else if (position == filterHelpRow) {
//                return 1;
//            } if(position == filterEmptyRow){
//                return 5;
//            }
//            return 4;
//        }
//
//    }
//
//
//}
