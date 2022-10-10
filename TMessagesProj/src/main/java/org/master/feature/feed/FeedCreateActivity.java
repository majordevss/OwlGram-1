package org.master.feature.feed;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.PollEditTextCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;
import java.util.Collections;

import it.owlgram.android.components.IconSelectorAlert;
import it.owlgram.android.helpers.FolderIconHelper;

public class FeedCreateActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;
    private ActionBarMenuItem doneItem;

    private int imageRow;
    private int namePreSectionRow;
    private int nameRow;
    private int nameSectionRow;
    private int includeHeaderRow;
    private int includeAddRow;
    private int includeStartRow;
    private int includeEndRow;
    private int includeShowMoreRow;
    private int includeSectionRow;
    private int removeRow;
    private int removeSectionRow;
    private int rowCount = 0;

    private boolean includeExpanded;

    private FeedFilter feedFilter;

    private boolean creatingNew;
    private String newFilterName;
    private String newFilterEmoticon;
    private ArrayList<Long> dialogs = new ArrayList<>();

    private static final int MAX_NAME_LENGTH = 12;
    private static final int done_button = 1;


    public static class HintInnerCell extends FrameLayout {

        private RLottieImageView imageView;

        public HintInnerCell(Context context) {
            super(context);

            imageView = new RLottieImageView(context);
            imageView.setAnimation(R.raw.filter_new, 100, 100);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.playAnimation();
            addView(imageView, LayoutHelper.createFrame(100, 100, Gravity.CENTER, 0, 0, 0, 0));
            imageView.setOnClickListener(v -> {
                if (!imageView.isPlaying()) {
                    imageView.setProgress(0.0f);
                    imageView.playAnimation();
                }
            });
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(156), MeasureSpec.EXACTLY));
        }
    }

    public FeedCreateActivity() {
        this(null, null);
    }

    public FeedCreateActivity(FeedFilter feedFilter) {
        this(feedFilter, null);
    }


    public FeedCreateActivity(FeedFilter feed_filter, ArrayList<Long> _dialogs) {
        super();
        feedFilter = feed_filter;
        if (feedFilter == null) {
            feedFilter = new FeedFilter();
            feedFilter.id = 2;
            while (FeedManager.getInstance(currentAccount).feedFiltersById.get(feedFilter.id) != null) {
                feedFilter.id++;
            }
            feedFilter.title = "";
            creatingNew = true;
        }
        newFilterName = feedFilter.title;
        newFilterEmoticon = feedFilter.emoticon;
        if (_dialogs != null) {
            dialogs.addAll(_dialogs);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows();
        return super.onFragmentCreate();
    }

    private void updateRows() {
        rowCount = 0;

        if (creatingNew) {
            imageRow = rowCount++;
            namePreSectionRow = -1;
        } else {
            imageRow = -1;
            namePreSectionRow = rowCount++;
        }
        nameRow = rowCount++;
        nameSectionRow = rowCount++;
        includeHeaderRow = rowCount++;
        includeAddRow = rowCount++;

        if (!dialogs.isEmpty()) {
            includeStartRow = rowCount;
            int count = includeExpanded || dialogs.size() < 8 ? dialogs.size() : Math.min(5, dialogs.size());
            rowCount += count;
            includeEndRow = rowCount;
            if (count != dialogs.size()) {
                includeShowMoreRow = rowCount++;
            } else {
                includeShowMoreRow = -1;
            }
        } else {
            includeStartRow = -1;
            includeEndRow = -1;
            includeShowMoreRow = -1;
        }
        includeSectionRow = rowCount++;
        if (!creatingNew) {
            removeRow = rowCount++;
            removeSectionRow = rowCount++;
        } else {
            removeRow = -1;
            removeSectionRow = -1;
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        ActionBarMenu menu = actionBar.createMenu();
        if (creatingNew) {
            actionBar.setTitle("New Feed");
        } else {
            TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(AndroidUtilities.dp(20));
            actionBar.setTitle(Emoji.replaceEmoji(feedFilter.title, paint.getFontMetricsInt(), AndroidUtilities.dp(20), false));
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (checkDiscard()) {
                        finishFragment();
                    }
                } else if (id == done_button) {
                    processDone();
                }
            }
        });
        doneItem = menu.addItem(done_button, LocaleController.getString("Save", R.string.Save).toUpperCase());

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context) {
            @Override
            public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
                return false;
            }
        };
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> {
            if (getParentActivity() == null) {
                return;
            }
            if (position == includeShowMoreRow) {
                includeExpanded = true;
                updateRows();
            } else if (position == includeAddRow ) {
                ArrayList<Long> arrayList = dialogs;
                FeedFilterChatActivity fragment = new FeedFilterChatActivity(arrayList);
                fragment.setDelegate((ids) -> {
                    dialogs = ids;
                    fillFilterName();
                    checkDoneButton(false);
                    updateRows();
                });
                presentFragment(fragment);
            } else if (position == removeRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle("Delete Filter");
                builder.setMessage("Are you sure you want to delete this folder?");
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                    AlertDialog progressDialog = null;
                    if (getParentActivity() != null) {
                        progressDialog = new AlertDialog(getParentActivity(), 3);
                        progressDialog.setCanCancel(false);
                        progressDialog.show();
                    }

                    final AlertDialog progressDialogFinal = progressDialog;
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (progressDialogFinal != null) {
                                    progressDialogFinal.dismiss();
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            FeedManager.getInstance(currentAccount).removeFilter(feedFilter);
                            FeedManager.getInstance(currentAccount).deleteFeedFilter(feedFilter);
                            finishFragment();
                        }
                    },400);

                });
                AlertDialog alertDialog = builder.create();
                showDialog(alertDialog);
                TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                }
            } else if (position == nameRow) {
                PollEditTextCell cell = (PollEditTextCell) view;
                cell.getTextView().requestFocus();
                AndroidUtilities.showKeyboard(cell.getTextView());
            } else if (view instanceof UserCell) {
                UserCell cell = (UserCell) view;
                showRemoveAlert(position, cell.getName(), cell.getCurrentObject(), position < includeSectionRow);
            }
        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (view instanceof UserCell) {
                UserCell cell = (UserCell) view;
                showRemoveAlert(position, cell.getName(), cell.getCurrentObject(), position < includeSectionRow);
                return true;
            }
            return false;
        });

        checkDoneButton(false);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }



    @Override
    public boolean onBackPressed() {
        return checkDiscard();
    }

    private boolean nameChangedManually;

    private void fillFilterName() {
        if (!creatingNew || !TextUtils.isEmpty(newFilterName) && nameChangedManually) {
            return;
        }
        String newName = "";
        String newEmoticon = "";
        if (newName != null && newName.length() > MAX_NAME_LENGTH) {
            newName = "";
        }
        newFilterName = newName;
        newFilterEmoticon = newEmoticon;
        RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(nameRow);
        if (holder != null) {
            adapter.onViewAttachedToWindow(holder);
        }
    }

    private boolean checkDiscard() {
        if (doneItem.getAlpha() == 1.0f) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            if (creatingNew) {
                builder.setTitle("Create Filter?");
                builder.setMessage("You have not finished creating the filter yet. Create now?");
                builder.setPositiveButton(LocaleController.getString("FilterDiscardNewSave", R.string.FilterDiscardNewSave), (dialogInterface, i) -> processDone());
            } else {
                builder.setTitle(LocaleController.getString("FilterDiscardTitle", R.string.FilterDiscardTitle));
                builder.setMessage("You have edited this folder. Apply changes?");
                builder.setPositiveButton(LocaleController.getString("ApplyTheme", R.string.ApplyTheme), (dialogInterface, i) -> processDone());
            }
            builder.setNegativeButton(LocaleController.getString("PassportDiscard", R.string.PassportDiscard), (dialog, which) -> finishFragment());
            showDialog(builder.create());
            return false;
        }
        return true;
    }

    private void showRemoveAlert(int position, CharSequence name, Object object, boolean include) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        if (include) {
            builder.setTitle(LocaleController.getString("FilterRemoveInclusionTitle", R.string.FilterRemoveInclusionTitle));
             if (object instanceof TLRPC.Chat) {
                builder.setMessage(LocaleController.formatString("FilterRemoveInclusionChatText", R.string.FilterRemoveInclusionChatText, name));
            }
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString("StickersRemove", R.string.StickersRemove), (dialogInterface, i) -> {
            if (include) {
                dialogs.remove(position - includeStartRow);
            }
            fillFilterName();
            updateRows();
            checkDoneButton(true);
        });
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
        }
    }


    private  boolean hasUserChanged;
    private void processDone() {
        saveFilter(feedFilter, newFilterEmoticon, newFilterName, dialogs, creatingNew, false, hasUserChanged, true, this, () -> {
            getNotificationCenter().postNotificationName(NotificationCenter.feedFiltersUpdated);
            finishFragment();
        });
    }

    private static void processAddFilter(FeedFilter feedFilter, String newFilterEmoticon, String newFilterName, ArrayList<Long> newAlwaysShow, boolean creatingNew, boolean atBegin, boolean hasUserChanged, BaseFragment fragment, Runnable onFinish) {
        feedFilter.title = newFilterName;
        feedFilter.emoticon = newFilterEmoticon;
        feedFilter.feedDialogsId = newAlwaysShow;
        if (creatingNew) {
            FeedManager.getInstance(fragment.getCurrentAccount()).addFilter(feedFilter, atBegin);
        } else {
            FeedManager.getInstance(fragment.getCurrentAccount()).onFilterUpdate(feedFilter);
        }
        FeedManager.getInstance(fragment.getCurrentAccount()).saveFeedFilter(feedFilter, atBegin, true);
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public static void saveFilter(FeedFilter filter, String newFilterEmoticon, String newFilterName, ArrayList<Long> newAlwaysShow, boolean creatingNew, boolean atBegin, boolean hasUserChanged, boolean progress, BaseFragment fragment, Runnable onFinish) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        processAddFilter(filter, newFilterEmoticon, newFilterName, newAlwaysShow, creatingNew, atBegin, hasUserChanged, fragment, onFinish);

    }

    @Override
    public boolean canBeginSlide() {
        return checkDiscard();
    }

    private boolean hasChanges() {
        hasUserChanged = feedFilter.feedDialogsId.size() != dialogs.size();
        if (!hasUserChanged) {
            Collections.sort(feedFilter.feedDialogsId);
            Collections.sort(dialogs);
            if (!feedFilter.feedDialogsId.equals(dialogs)) {
                hasUserChanged = true;
            }
        }
        if (!TextUtils.equals(feedFilter.title, newFilterName)) {
            return true;
        }
        if (!TextUtils.equals(feedFilter.emoticon, newFilterEmoticon)) {
            return true;
        }

        return hasUserChanged;
    }

    private void checkDoneButton(boolean animated) {
        boolean enabled = !TextUtils.isEmpty(newFilterName) && newFilterName.length() <= MAX_NAME_LENGTH;
        if (enabled) {
            if (!creatingNew) {
                enabled = hasChanges();
            }
        }
        if (doneItem.isEnabled() == enabled) {
            return;
        }
        doneItem.setEnabled(enabled);
        if (animated) {
            doneItem.animate().alpha(enabled ? 1.0f : 0.0f).scaleX(enabled ? 1.0f : 0.0f).scaleY(enabled ? 1.0f : 0.0f).setDuration(180).start();
        } else {
            doneItem.setAlpha(enabled ? 1.0f : 0.0f);
            doneItem.setScaleX(enabled ? 1.0f : 0.0f);
            doneItem.setScaleY(enabled ? 1.0f : 0.0f);
        }
    }

    private void setTextLeft(View cell) {
        if (cell instanceof PollEditTextCell) {
            PollEditTextCell textCell = (PollEditTextCell) cell;
            int left = MAX_NAME_LENGTH - (newFilterName != null ? newFilterName.length() : 0);
            if (left <= MAX_NAME_LENGTH - MAX_NAME_LENGTH * 0.7f) {
                textCell.setText2(String.format("%d", left));
                SimpleTextView textView = textCell.getTextView2();
                String key = left < 0 ? Theme.key_windowBackgroundWhiteRedText5 : Theme.key_windowBackgroundWhiteGrayText3;
                textView.setTextColor(Theme.getColor(key));
                textView.setTag(key);
                textView.setAlpha(((PollEditTextCell) cell).getTextView().isFocused() || left < 0 ? 1.0f : 0.0f);
            } else {
                textCell.setText2("");
            }
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
            return type != 3 && type != 0 && type != 2 && type != 5;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1: {
                    UserCell cell = new UserCell(mContext, 6, 0, false);
                    cell.setSelfAsSavedMessages(true);
                    cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view = cell;
                    break;
                }
                case 2: {
                    PollEditTextCell cell = new PollEditTextCell(mContext, false, null, view1 -> {
                        IconSelectorAlert iconSelectorAlert = new IconSelectorAlert(mContext) {

                            @Override
                            protected void onItemClick(String emoticon) {
                                ImageView pollEditTextCell = (ImageView) view1;
                                pollEditTextCell.setImageResource(FolderIconHelper.getTabIcon(emoticon));
                                newFilterEmoticon = emoticon;
                                checkDoneButton(true);
                            }

                        };
                        iconSelectorAlert.show();
                    });
                    cell.setIcon(FolderIconHelper.getTabIcon(newFilterEmoticon));
                    cell.createErrorTextView();
                    cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    cell.addTextWatcher(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (cell.getTag() != null) {
                                return;
                            }
                            String newName = s.toString();
                            if (!TextUtils.equals(newName, newFilterName)) {
                                nameChangedManually = !TextUtils.isEmpty(newName);
                                newFilterName = newName;
                            }
                            RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(nameRow);
                            if (holder != null) {
                                setTextLeft(holder.itemView);
                            }
                            checkDoneButton(true);
                        }
                    });
                    EditTextBoldCursor editText = cell.getTextView();
                    cell.setShowNextButton(true);
                    editText.setOnFocusChangeListener((v, hasFocus) -> cell.getTextView2().setAlpha(hasFocus || newFilterName.length() > MAX_NAME_LENGTH ? 1.0f : 0.0f));
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    view = cell;
                    break;
                }
                case 3:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 4:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                    view = new HintInnerCell(mContext);
                    break;
                case 6:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            int viewType = holder.getItemViewType();
            if (viewType == 2) {
                setTextLeft(holder.itemView);
                PollEditTextCell textCell = (PollEditTextCell) holder.itemView;
                textCell.setTag(1);
                textCell.setTextAndHint(newFilterName != null ? newFilterName : "", "Filter Name", false);
                textCell.setTag(null);
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            if (holder.getItemViewType() == 2) {
                PollEditTextCell editTextCell = (PollEditTextCell) holder.itemView;
                EditTextBoldCursor editText = editTextCell.getTextView();
                if (editText.isFocused()) {
                    editText.clearFocus();
                    AndroidUtilities.hideKeyboard(editText);
                }
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == includeHeaderRow) {
                        headerCell.setText(LocaleController.getString("FilterInclude", R.string.FilterInclude));
                    }
                    break;
                }
                case 1: {
                    UserCell userCell = (UserCell) holder.itemView;
                    Long id;
                    boolean divider;
                    if (position >= includeStartRow && position < includeEndRow) {
                        id = dialogs.get(position - includeStartRow);
                        divider = includeShowMoreRow != -1 || position != includeEndRow - 1;
                    }  else {
                        return;
                    }
                    TLRPC.Chat chat = getMessagesController().getChat(-id);
                    if (chat != null) {
                        String status;
                        if (chat.participants_count != 0) {
                            status = LocaleController.formatPluralString("Members", chat.participants_count);
                        } else if (TextUtils.isEmpty(chat.username)) {
                            if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                status = LocaleController.getString("ChannelPrivate", R.string.ChannelPrivate);
                            } else {
                                status = LocaleController.getString("MegaPrivate", R.string.MegaPrivate);
                            }
                        } else {
                            if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                status = LocaleController.getString("ChannelPublic", R.string.ChannelPublic);
                            } else {
                                status = LocaleController.getString("MegaPublic", R.string.MegaPublic);
                            }
                        }
                        userCell.setData(chat, null, status, 0, divider);
                    }
                    break;
                }
                case 3: {
                    if (position == removeSectionRow) {
                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 4: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == removeRow) {
                        textCell.setColors(null, Theme.key_windowBackgroundWhiteRedText5);
                        textCell.setText(LocaleController.getString("FilterDelete", R.string.FilterDelete), false);
                    } else if (position == includeShowMoreRow) {
                        textCell.setColors(Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhiteBlueText4);
                        textCell.setTextAndIcon(LocaleController.formatPluralString("FilterShowMoreChats", dialogs.size() - 5), R.drawable.arrow_more, false);
                    } else if (position == includeAddRow) {
                        textCell.setColors(Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhiteBlueText4);
                        textCell.setTextAndIcon("Add Channels", R.drawable.msg_chats_add, position + 1 != includeSectionRow);
                    }
                    break;
                }
                case 6: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == includeSectionRow) {
                        cell.setText(LocaleController.getString("FilterIncludeInfo", R.string.FilterIncludeInfo));
                    }
                    holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == includeHeaderRow) {
                return 0;
            } else if (position >= includeStartRow && position < includeEndRow) {
                return 1;
            } else if (position == nameRow) {
                return 2;
            } else if (position == nameSectionRow || position == namePreSectionRow || position == removeSectionRow) {
                return 3;
            } else if (position == imageRow) {
                return 5;
            } else if (position == includeSectionRow) {
                return 6;
            } else {
                return 4;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        ThemeDescription.ThemeDescriptionDelegate themeDelegate = () -> {
            if (listView != null) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof UserCell) {
                        ((UserCell) child).update(0);
                    }
                }
            }
        };

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, TextCell.class, PollEditTextCell.class, UserCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteRedText5));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText4));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"ImageView"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{UserCell.class}, new String[]{"adminTextView"}, null, null, null, Theme.key_profile_creatorIcon));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusColor"}, null, null, themeDelegate, Theme.key_windowBackgroundWhiteGrayText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusOnlineColor"}, null, null, themeDelegate, Theme.key_windowBackgroundWhiteBlueText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, null, Theme.avatarDrawables, null, Theme.key_avatar_text));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundRed));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundOrange));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundViolet));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundGreen));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundCyan));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundBlue));
        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundPink));

        return themeDescriptions;
    }
}
