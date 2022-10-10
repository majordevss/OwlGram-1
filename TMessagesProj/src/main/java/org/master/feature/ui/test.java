package org.master.feature.ui;//package org.telegram.ui;
//
//import android.content.Context;
//import android.graphics.Color;
//import android.graphics.Typeface;
//import android.graphics.drawable.Drawable;
//import android.graphics.drawable.GradientDrawable;
//import android.icu.util.Measure;
//import android.text.Layout;
//import android.util.Log;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewTreeObserver;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.DefaultItemAnimator;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.gms.vision.Frame;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.ImageLocation;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.R;
//import org.telegram.tgnet.TLRPC;
//import org.telegram.ui.ActionBar.ActionBar;
//import org.telegram.ui.ActionBar.ActionBarMenu;
//import org.telegram.ui.ActionBar.ActionBarMenuItem;
//import org.telegram.ui.ActionBar.BaseFragment;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.ActionBar.ThemeDescription;
//import org.telegram.ui.Components.AvatarDrawable;
//import org.telegram.ui.Components.BackupImageView;
//import org.telegram.ui.Components.Bulletin;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.RecyclerListView;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class WalletProfile extends BaseFragment {
//
//    TransactionCell transactionCell;
//    private RecyclerListView listView;
//    private ListAdapter adapter;
//
//    private FrameLayout partOne;
//    private FrameLayout partTwo;
//    private TextView balanceTextView;
//    private TextView updatedTimeTextView;
//
//    List<Transaction> transactionList = new ArrayList<>();
//
//    public class TransactionCell extends FrameLayout {
//
//
//        private Transaction transaction;
//
//        public void setTransaction(Transaction transaction) {
//            this.transaction = transaction;
//        }
//
//        public TransactionCell(@NonNull Context context) {
//            super(context);
//
//            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
//                    new int[]{0XFF3FBF3F, 0XFF23DB7F});
//
//            partOne = new FrameLayout(context);
//            partOne.setBackgroundDrawable(gradientDrawable);
//            addView(partOne, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(400),
//                    Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));
//
//            partTwo = new FrameLayout(context);
//            partTwo.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
//            addView(partTwo, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
//                    Gravity.CENTER, 0, 900, 0, 0));
//
//            balanceTextView = new TextView(context);
//            balanceTextView.setText(LocaleController.getString("AmountETB", R.string.AmountETB));
//            balanceTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            balanceTextView.setGravity(Gravity.CENTER);
//            balanceTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
//            balanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
//            balanceTextView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
//            partOne.addView(balanceTextView);
//
//            updatedTimeTextView = new TextView(context);
//            updatedTimeTextView.setText(LocaleController.getString("UpdatedTime", R.string.UpdatedTime));
//            updatedTimeTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            updatedTimeTextView.setGravity(Gravity.CENTER);
//            updatedTimeTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
//            updatedTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
//            partOne.addView(updatedTimeTextView);
//
//
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            int width = MeasureSpec.getSize(widthMeasureSpec);
//            int height = MeasureSpec.getSize(heightMeasureSpec);
//
//            partOne.measure(width, (int) (height * 0.3));
//            partTwo.measure(width, height);
//
//            balanceTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
//            updatedTimeTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
//
//            setMeasuredDimension(width, height);
//        }
//
//        @Override
//        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//            super.onLayout(changed, left, top, right, bottom);
//            int width = right - left;
//            int height = bottom - top;
//            partOne.layout(0, 0, width, (int) (height * 0.3));
//            partTwo.layout(0, (int) (height * 0.3), width, height);
//            int x = (int) (width * 0.5f);
//            int y = (int) (height * 0.02f);
//
//            updatedTimeTextView.layout(0, 0, x + updatedTimeTextView.getMeasuredWidth(),
//                    y + updatedTimeTextView.getMeasuredHeight());
//
//            y = (int) (height * 0.1);
//            balanceTextView.layout(0, y, x + balanceTextView.getMeasuredWidth(),
//                    y + balanceTextView.getMeasuredHeight());
//
//        }
//
//    }
//
//
//    public class TransactionRow extends FrameLayout {
//
//        private ImageView imageView;
//        private TextView typeTextView;
//        private TextView infoTextView;
//        private TextView amountTextView;
//        private TextView timeTextView;
//
//        public TransactionRow(@NonNull Context context) {
//            super(context);
//            imageView = new ImageView(context);
//            imageView.setImageDrawable(getResources().getDrawable(R.drawable.dollar));
//            addView(imageView, LayoutHelper.createFrame(AndroidUtilities.dp(64), AndroidUtilities.dp(64)));
//
//            typeTextView = new TextView(context);
//            typeTextView.setText(LocaleController.getString("Deposit", R.string.Deposit));
//            typeTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            typeTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
//            typeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//            typeTextView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
//            addView(typeTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
//
//            amountTextView = new TextView(context);
//            amountTextView.setText("-10 ETB");
//            amountTextView.setTextColor(Color.RED);
//            amountTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
//            amountTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//            amountTextView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
//            addView(amountTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
//
//            infoTextView = new TextView(context);
//            infoTextView.setText(LocaleController.getString("DepositInfo", R.string.DepositInfo));
//            infoTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
//            infoTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
//            infoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
//            addView(infoTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
//
//            timeTextView = new TextView(context);
//            timeTextView.setText(LocaleController.getString("TransactionTime", R.string.TransactionTime));
//            timeTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
//            timeTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
//            timeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
//            addView(timeTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
//        }
//
//        public void setFields(String amount, String time, String type, String info) {
//            amountTextView.setText(amount);
//            typeTextView.setText(type);
//            infoTextView.setText(info);
//            timeTextView.setText(time);
//        }
//
//        @Override
//        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            int width = MeasureSpec.getSize(widthMeasureSpec);
//            int height = MeasureSpec.getSize(heightMeasureSpec);
//
//            imageView.measure(AndroidUtilities.dp(64), AndroidUtilities.dp(64));
//            typeTextView.measure(MeasureSpec.makeMeasureSpec((int) (width * 0.4), MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
//            amountTextView.measure(MeasureSpec.makeMeasureSpec((int) (width * 0.4), MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
//            timeTextView.measure(MeasureSpec.makeMeasureSpec((int) (width * 0.4), MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
//            setMeasuredDimension(width, height);
//
//        }
//
//        @Override
//        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//            super.onLayout(changed, left, top, right, bottom);
//            int width = right - left;
//            int height = bottom - top;
//
//            imageView.layout(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(48 + 10), AndroidUtilities.dp(48 + 10));
//            typeTextView.layout(AndroidUtilities.dp(35), AndroidUtilities.dp(20),
//                    AndroidUtilities.dp(200), AndroidUtilities.dp(45));
//            amountTextView.layout(width - 400, AndroidUtilities.dp(20),
//                    width, AndroidUtilities.dp(45));
//            timeTextView.layout(width - 400, AndroidUtilities.dp(20 + 20),
//                    width, AndroidUtilities.dp(45 + 20));
//            infoTextView.layout(AndroidUtilities.dp(35), AndroidUtilities.dp(20 + 20),
//                    AndroidUtilities.dp(230), AndroidUtilities.dp(70));
//        }
//    }
//
//    @Override
//    public boolean onFragmentCreate() {
//        for(int a = 0; a < 10; a++){
//            transactionList.add(new Transaction("Deposit", "Deposit from Amole",
//                    "-10 ETB", "Deposit"));
//        }
//        return super.onFragmentCreate();
//    }
//
//    @Override
//    public View createView(Context context) {
//
//        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
//        actionBar.setAllowOverlayTitle(false);
//        actionBar.setCastShadows(false);
//        actionBar.setAddToContainer(true);
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
//        ActionBarMenuItem notificationItem = menu.addItemWithWidth(2, R.drawable.menu_notifications, AndroidUtilities.dp(24));
//
//        ActionBarMenuItem switchItem = menu.addItemWithWidth(1, 0, AndroidUtilities.dp(56));
//        AvatarDrawable avatarDrawable = new AvatarDrawable();
//        avatarDrawable.setTextSize(AndroidUtilities.dp(12));
//
//        BackupImageView imageView = new BackupImageView(context);
//        imageView.setRoundRadius(AndroidUtilities.dp(18));
//        switchItem.addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.CENTER));
//
//        TLRPC.User user = getUserConfig().getCurrentUser();
//        avatarDrawable.setInfo(user);
//        imageView.getImageReceiver().setCurrentAccount(currentAccount);
//        imageView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, user);
//
//
//        fragmentView = new FrameLayout(context);
//        FrameLayout frameLayout = (FrameLayout) fragmentView;
//        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
//
//        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
//                new int[]{0XFF3FBF3F, 0XFF23DB7F});
//        actionBar.setBackgroundDrawable(gradientDrawable);
//
//
//        listView = new RecyclerListView(context);
//        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getParentActivity());
//        layoutManager.setOrientation(RecyclerListView.VERTICAL);
//        listView.setLayoutManager(layoutManager);
//        listView.setVerticalScrollBarEnabled(false);
//        frameLayout.addView(listView,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
//
//        adapter = new ListAdapter( context);
//        listView.setAdapter(adapter);
//
//        return fragmentView;
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
//        public int getItemCount() {
//            return transactionList.size();
//        }
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//
//            TransactionCell transactionCell = new TransactionCell(mContext);
//            return new RecyclerListView.Holder(transactionCell);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//
//            TransactionCell transactionCell = (TransactionCell)holder.itemView;
//            transactionCell.setTransaction(transactionList.get(position));
//
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder) {
//            return true;
//        }
//    }
//
//
//    private class Transaction {
//
//        public String transactionType;
//        public String transactionInfo;
//        public String transactionAmount;
//        public String transactoinTime;
//
//        public Transaction(String transactionType, String transactionInfo,
//                           String transactionAmount, String transactoinTime) {
//            this.transactionType = transactionType;
//            this.transactionInfo = transactionInfo;
//            this.transactionAmount = transactionAmount;
//            this.transactoinTime = transactoinTime;
//        }
//    }
//}
