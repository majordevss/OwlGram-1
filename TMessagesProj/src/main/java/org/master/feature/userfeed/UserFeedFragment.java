package org.master.feature.userfeed;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.master.feature.TelegramMessageUpdateManager;
import org.master.feature.database.AppDatabase;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ManageChatUserCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ProfileGalleryView;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserFeedFragment extends BaseFragment {

    private ActionBarMenuItem otherItem;
    private FlickerLoadingView flickerLoadingView;
    private EmptyTextProgressView emptyView;
    private RecyclerListView listView;
    private ListAdapter adapter;

    public ArrayList<TelegramMessageUpdateManager.UserWithUpdate> userFeeds = new ArrayList<>();
    public HashMap<Long,TelegramMessageUpdateManager.UpdateObject> updateDict = new HashMap<>();


    public void loadUpdates(){
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                List<TelegramMessageUpdateManager.UserWithUpdate> feeds =  AppDatabase.getInstance(currentAccount).appDao().getUserWithUpdates();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if(feeds != null){
                            userFeeds = new ArrayList<>(feeds);
                        }
//                        for(int a = 0; a < userFeeds.size();a++){
//                            TelegramMessageUpdateManager.UserWithUpdate update =  userFeeds.get(a);
//                            updateDict.put(update.user_id,update);
//                        }
                        if (emptyView != null) {
                            emptyView.showTextView();
                        }
                        if(adapter != null){
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }

//    public void loadUsersFeeds(){
//        Utilities.searchQueue.postRunnable(new Runnable() {
//            @Override
//            public void run() {
//                SharedPreferences preferences = null;
//                if (currentAccount == 0) {
//                    preferences =  ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
//                } else {
//                    preferences =  ApplicationLoader.applicationContext.getSharedPreferences("userconfig" + currentAccount, Context.MODE_PRIVATE);
//                }
//                String userChange = preferences.getString("userchange","");
//                String[]  users = TextUtils.split(userChange,",");
//                for(int a = 0; a < users.length;a++){
//                    long user_id = Long.parseLong(users[a]);
//                    TLRPC.User userFinale =  getMessagesController().getUser(user_id);
//                    if(userFinale == null){
//                        continue;
//                    }
//                    userFeeds.add(userFinale);
//                }
//                AndroidUtilities.runOnUIThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (emptyView != null) {
//                            emptyView.showTextView();
//                        }
//                        if(adapter != null){
//                            adapter.notifyDataSetChanged();
//                        }
//                    }
//                });
//            }
//        });
//    }


    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return false;
    }

    private static class EmptyTextProgressView extends FrameLayout {

        private TextView emptyTextView1;
        private TextView emptyTextView2;
        private View progressView;
        private RLottieImageView imageView;

        public EmptyTextProgressView(Context context) {
            this(context, null);
        }

        public EmptyTextProgressView(Context context, View progressView) {
            super(context);

            addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            this.progressView = progressView;

            imageView = new RLottieImageView(context);
            imageView.setAnimation(R.raw.filter_no_chats, 120, 120);
            imageView.setAutoRepeat(false);
            addView(imageView, LayoutHelper.createFrame(140, 140, Gravity.CENTER, 52, 4, 52, 60));
            imageView.setOnClickListener(v -> {
                if (!imageView.isPlaying()) {
                    imageView.setProgress(0.0f);
                    imageView.playAnimation();
                }
            });

            emptyTextView1 = new TextView(context);
            emptyTextView1.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            emptyTextView1.setText("No Recent UserFeed!");
            emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            emptyTextView1.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            emptyTextView1.setGravity(Gravity.CENTER);
            addView(emptyTextView1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 40, 17, 0));

            emptyTextView2 = new TextView(context);
            String help = "Your recent voice and UserFeed\\ will appear here.";
            if (AndroidUtilities.isTablet() && !AndroidUtilities.isSmallTablet()) {
                help = help.replace('\n', ' ');
            }
            emptyTextView2.setText(help);
            emptyTextView2.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
            emptyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            emptyTextView2.setGravity(Gravity.CENTER);
            emptyTextView2.setLineSpacing(AndroidUtilities.dp(2), 1);
            addView(emptyTextView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 80, 17, 0));

            progressView.setAlpha(0f);
            imageView.setAlpha(0f);
            emptyTextView1.setAlpha(0f);
            emptyTextView2.setAlpha(0f);

            setOnTouchListener((v, event) -> true);
        }

        public void showProgress() {
            imageView.animate().alpha(0f).setDuration(150).start();
            emptyTextView1.animate().alpha(0f).setDuration(150).start();
            emptyTextView2.animate().alpha(0f).setDuration(150).start();
            progressView.animate().alpha(1f).setDuration(150).start();
        }

        public void showTextView() {
            imageView.animate().alpha(1f).setDuration(150).start();
            emptyTextView1.animate().alpha(1f).setDuration(150).start();
            emptyTextView2.animate().alpha(1f).setDuration(150).start();
            progressView.animate().alpha(0f).setDuration(150).start();
            imageView.playAnimation();
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
    }


    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }


    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Feed");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showDeleteAlert();
                }
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        otherItem = menu.addItem(10, R.drawable.ic_ab_other);
        otherItem.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        otherItem.addSubItem(1, R.drawable.msg_delete,"Delete All");

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setViewType(FlickerLoadingView.CALL_LOG_TYPE);
        flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flickerLoadingView.showDate(false);
        emptyView = new EmptyTextProgressView(context, flickerLoadingView);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


        listView = new RecyclerListView(context);
        listView.setEmptyView(emptyView);
        listView.setAdapter(adapter = new ListAdapter(context));
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(!(view instanceof UserDetialCell) || view.getTag() == null){
                    return;
                }
                long user_id = (Long)view.getTag();
                Bundle args = new Bundle();
                args.putLong("user_id",user_id);
                ProfileActivity profileActivity = new ProfileActivity(args);
                presentFragment(profileActivity);
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(adapter = new ListAdapter(context));

        if (emptyView != null) {
            emptyView.showProgress();
        }
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                loadUpdates();
            }
        },300);
        return fragmentView;
    }
    private void showDeleteAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Delete All");
        builder.setMessage("Do you want to delete all UserFeed");
        FrameLayout frameLayout = new FrameLayout(getParentActivity());
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {

            userFeeds.clear();
            otherItem.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();

            SharedPreferences preferences = null;
            if (currentAccount == 0) {
                preferences =  ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
            } else {
                preferences =  ApplicationLoader.applicationContext.getSharedPreferences("userconfig" + currentAccount, Context.MODE_PRIVATE);
            }
            preferences.edit().putString("userchange","").commit();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter{

        public Context mContext;

        public ListAdapter(Context context){
            mContext = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            UserDetialCell userFeedCel = new UserDetialCell(mContext);
            return new RecyclerListView.Holder(userFeedCel);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserDetialCell userFeedCel = (UserDetialCell) holder.itemView;
            TelegramMessageUpdateManager.UserWithUpdate userFeed = userFeeds.get(position);
            userFeedCel.setUser(userFeed,position != userFeeds.size() - 1);


        }

        @Override
        public int getItemCount() {
            return userFeeds.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }
    }

    private class UserFeedCel extends LinearLayout {

        private ManageChatUserCell userCell;
        ProfileGalleryView profileGalleryView;
        private TLRPC.User currentUser;
        public UserFeedCel(Context context) {
            super(context);
            setOrientation(VERTICAL);

            userCell = new ManageChatUserCell(context,6,2,false);
            userCell.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentUser != null){
                        Bundle args = new Bundle();
                        args.putLong("user_id",currentUser.id);
                        ProfileActivity profileActivity = new ProfileActivity(args);
                        presentFragment(profileActivity);
                    }

                }
            });
            addView(userCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            profileGalleryView = new ProfileGalleryView(context, actionBar, listView, new ProfileGalleryView.Callback() {
                @Override
                public void onDown(boolean left) {

                }

                @Override
                public void onRelease() {

                }

                @Override
                public void onPhotosLoaded() {

                }

                @Override
                public void onVideoSet() {

                }
            });
            addView(profileGalleryView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 500));

        }
        public void setUser(TLRPC.User user){
            if(user == currentUser){
                return;
            }
            currentUser = user;
            userCell.setData(user,null,null,false);
            profileGalleryView.setParentAvatarImage(null);
            profileGalleryView.setHasActiveVideo(false);
            profileGalleryView.setData(user.id, true);
            profileGalleryView.setCreateThumbFromParent(true);
        }
    }

    /*
     * This is the source code of Telegram for Android v. 5.x.x.
     * It is licensed under GNU GPL v. 2 or later.
     * You should have received a copy of the license in this archive (see LICENSE).
     *
     * Copyright Nikolai Kudashov, 2013-2018.
     */


    private static class UserDetialCell extends FrameLayout {

        private TextView textView;
        private TextView valueTextView;
        private BackupImageView imageView;
        private boolean needDivider;
        private boolean multiline;
        private AvatarDrawable avatarDrawable;

        public UserDetialCell(Context context) {
            super(context);

            avatarDrawable = new AvatarDrawable();

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 21, 10, 21, 0));

            valueTextView = new TextView(context);
            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            valueTextView.setLines(1);
            valueTextView.setMaxLines(1);
            valueTextView.setSingleLine(true);
            valueTextView.setPadding(0, 0, 0, 0);
            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 21, 35, 21, 0));

            imageView = new BackupImageView(context);
            imageView.setVisibility(GONE);
            imageView.setRoundRadius(AndroidUtilities.dp(12));
            addView(imageView, LayoutHelper.createFrame(52, 52, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 8, 6, 8, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (!multiline) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
            } else {
                super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }
        }

        public void setUser(  TelegramMessageUpdateManager.UserWithUpdate userFeed,boolean divider){
            String status = "";
            if(userFeed.updateObjects!= null && userFeed.updateObjects.size() > 0){
                TLRPC.User user = MessagesController.getInstance(UserConfig.selectedAccount).getUser(userFeed.user_id.user_id);
                if(user != null){
                    TelegramMessageUpdateManager.UpdateObject updateObject = userFeed.updateObjects.get(userFeed.updateObjects.size() - 1);
                    if(updateObject.type == TelegramMessageUpdateManager.UPDATE_BLOCKED){
                        status = "Blocked By " + UserObject.getUserName(user);
                    }else  if(updateObject.type == TelegramMessageUpdateManager.UPDATE_NAME){
                        status = UserObject.getUserName(user) + " updated name";
                    }else  if(updateObject.type == TelegramMessageUpdateManager.UPDATE_PHOTO){
                        status = UserObject.getUserName(user) + " updated profile photo at " +
                                LocaleController.formatDate(updateObject.date);
                    }else  if(updateObject.type == TelegramMessageUpdateManager.UPDATE_PHONE){
                        status = UserObject.getUserName(user) + " updated pone number";
                    }
                    setTag(user.id);
                    textView.setText(UserObject.getUserName(user));
                    valueTextView.setText(status);
                    if(user.photo != null && user.photo.photo_small != null){
                        imageView.setVisibility(VISIBLE);
                        avatarDrawable.setInfo(user);
                        imageView.setImage(ImageLocation.getForUser(user,ImageLocation.TYPE_SMALL),"100_100",null,avatarDrawable,null);
                        textView.setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(50), 0, LocaleController.isRTL ? AndroidUtilities.dp(50) : 0, 0);
                        valueTextView.setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(50), 0, LocaleController.isRTL ? AndroidUtilities.dp(50) : 0, multiline ? AndroidUtilities.dp(12) : 0);
                    }else{
                        imageView.setVisibility(GONE);
                    }

                    needDivider = divider;
                    setWillNotDraw(!divider);
                    setMultilineDetail(true);
                }

            }
        }

        public TextView getTextView() {
            return textView;
        }

        public TextView getValueTextView() {
            return valueTextView;
        }

        public void setMultilineDetail(boolean value) {
            multiline = value;
            if (value) {
                valueTextView.setLines(0);
                valueTextView.setMaxLines(0);
                valueTextView.setSingleLine(false);
                valueTextView.setPadding(0, 0, 0, AndroidUtilities.dp(12));
            } else {
                valueTextView.setLines(1);
                valueTextView.setMaxLines(1);
                valueTextView.setSingleLine(true);
                valueTextView.setPadding(0, 0, 0, 0);
            }
        }

        public void setTextAndValue(String text, CharSequence value, boolean divider) {
            textView.setText(text);
            valueTextView.setText(value);
            needDivider = divider;
            imageView.setVisibility(GONE);
            setWillNotDraw(!divider);
        }

        public void setTextAndValueAndIcon(String text, CharSequence value, int resId, boolean divider) {
            textView.setText(text);
            valueTextView.setText(value);
            imageView.setImageResource(resId);
            imageView.setVisibility(VISIBLE);
            textView.setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(50), 0, LocaleController.isRTL ? AndroidUtilities.dp(50) : 0, 0);
            valueTextView.setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(50), 0, LocaleController.isRTL ? AndroidUtilities.dp(50) : 0, multiline ? AndroidUtilities.dp(12) : 0);
            needDivider = divider;
            setWillNotDraw(!divider);
        }

        public void setValue(CharSequence value) {
            valueTextView.setText(value);
        }

        @Override
        public void invalidate() {
            super.invalidate();
            textView.invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider && Theme.dividerPaint != null) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 71 : 20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 71 : 20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

}
