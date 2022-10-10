package org.telegram.ui.Adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.DrawerLayoutContainer;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;

import it.owlgram.android.OwlConfig;
import it.owlgram.android.helpers.MenuOrderManager;
import it.owlgram.android.helpers.PasscodeHelper;


public class DrawerLayoutAdapter extends RecyclerListView.SelectionAdapter {

    private DrawerLayoutContainer mDrawerLayoutContainer;

    private Context mContext;
    private ArrayList<Item> items = new ArrayList<>(11);
    private ArrayList<Integer> accountNumbers = new ArrayList<>();
    private ArrayList<Item> createItems = new ArrayList<>();
    private ArrayList<Item> featuresItems = new ArrayList<>();
    private ArrayList<Item> miniApps = new ArrayList<>();
    private boolean accountsShown;

    private boolean createShown;
    private boolean featureShown;


    private DrawerProfileCell profileCell;
    private DrawerExpandActionCell drawerExpandActionCell;
    private FeatureExpandCell featureExpandCell;

    private RecyclerView.ItemAnimator itemAnimator;


    private boolean hasGps;


    public DrawerLayoutAdapter(Context context, RecyclerView.ItemAnimator animator,DrawerLayoutContainer drawerLayoutContainer) {
        mContext = context;
        mDrawerLayoutContainer = drawerLayoutContainer;
        itemAnimator = animator;
        accountsShown = UserConfig.getActivatedAccountsCount() > 1 && MessagesController.getGlobalMainSettings().getBoolean("accountsShown", true);
        createShown = MessagesController.getGlobalMainSettings().getBoolean("createShown", false);
        featureShown = MessagesController.getGlobalMainSettings().getBoolean("featureShown", false);

        Theme.createCommonDialogResources(context);
        resetItems();
        try {
            hasGps = ApplicationLoader.applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        } catch (Throwable e) {
            hasGps = false;
        }
    }

    private int getAccountRowsCount() {
        int count = accountNumbers.size() + 1;
        if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
            count++;
        }
        return count;
    }

    private int getCreateItemCount() {
        return createItems.size() + 1;
    }

    private int getFeatureItemCount() {
        return featuresItems.size() + 1;
    }

    private int getColorItemCount(){
        if(miniApps.isEmpty()){
            return 0;
        }
        return miniApps.size() + 1;
    }


    @Override
    public int getItemCount() {
        int count = items.size() + 2;
        count += getColorItemCount();
        if (accountsShown) {
            count += getAccountRowsCount();
        }
        if (createShown) {
            count += getCreateItemCount();
        }
        if (featureShown) {
            count += getFeatureItemCount();
        }
        return count;
    }

    public void setFeatureShown(boolean vale, boolean animated) {
        if (featureShown == vale || itemAnimator.isRunning()) {
            return;
        }
        featureShown = vale;
        if (featureExpandCell != null) {
            featureExpandCell.setFeatureShown(featureShown, animated);
        }

        MessagesController.getGlobalMainSettings().edit().putBoolean("featureShown", createShown).commit();
        if (animated) {
            int pos = 4;


            pos += getColorItemCount();

            if (accountsShown) {
                pos += getAccountRowsCount();
            }
            if (createShown) {
                pos += getCreateItemCount();
            }

            if (featureShown) {
                notifyItemRangeInserted(pos, getFeatureItemCount());
            } else {
                notifyItemRangeRemoved(pos, getFeatureItemCount());
            }
        } else {
            notifyDataSetChanged();
        }

    }

    public void setCreateShown(boolean vale, boolean animated) {
        if (createShown == vale || itemAnimator.isRunning()) {
            return;
        }
        createShown = vale;
        if (drawerExpandActionCell != null) {
            drawerExpandActionCell.setCreateShown(createShown, animated);
        }

        MessagesController.getGlobalMainSettings().edit().putBoolean("createShown", createShown).commit();
        if (animated) {
            int pos = 3;
            pos += getColorItemCount();

            if (accountsShown) {
                pos += getAccountRowsCount();
            }
            if (createShown) {
                notifyItemRangeInserted(pos, getCreateItemCount());
            } else {
                notifyItemRangeRemoved(pos, getCreateItemCount());
            }
        } else {
            notifyDataSetChanged();
        }

    }


    public void setAccountsShown(boolean value, boolean animated) {
        if (accountsShown == value || itemAnimator.isRunning()) {
            return;
        }
        accountsShown = value;
        if (profileCell != null) {
            profileCell.setAccountsShown(accountsShown, animated);
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean("accountsShown", accountsShown).commit();
        if (animated) {
            if (accountsShown) {
                notifyItemRangeInserted(2, getAccountRowsCount());
            } else {
                notifyItemRangeRemoved(2, getAccountRowsCount());
            }
        } else {
            notifyDataSetChanged();
        }
    }

    public boolean isAccountsShown() {
        return accountsShown;
    }

    public boolean isCreateShown() {
        return createShown;
    }

    public boolean isFeatureShown() {
        return featureShown;
    }

    private View.OnClickListener onPremiumDrawableClick;
    public void setOnPremiumDrawableClick(View.OnClickListener listener) {
        onPremiumDrawableClick = listener;

    }

    @Override
    public void notifyDataSetChanged() {
        resetItems();
        super.notifyDataSetChanged();
    }


    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int itemType = holder.getItemViewType();
        return itemType == 3 || itemType == 4 || itemType == 5 || itemType == 51 || itemType == 50 || itemType == 100;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = profileCell = new DrawerProfileCell(mContext, mDrawerLayoutContainer) {
                    @Override
                    protected void onPremiumClick() {
                        if (onPremiumDrawableClick != null) {
                            onPremiumDrawableClick.onClick(this);
                        }
                    }
                };
                break;
            case 2:
                view = new DividerCell(mContext);
                break;
            case 51:
            case 50:
            case 3:
                view = new DrawerActionCell(mContext);
                break;
            case 4:
                view = new DrawerUserCell(mContext);
                break;
            case 5:
                view = new DrawerAddCell(mContext);
                break;
            case 6:
                drawerExpandActionCell = new DrawerExpandActionCell(mContext);
                view = drawerExpandActionCell;
                break;
            case 7:
                featureExpandCell = new FeatureExpandCell(mContext);
                view = featureExpandCell;
                break;
            case 100:
                view = new IconColorCell(mContext);
                break;
            case 1:
            default:
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: {
                DrawerProfileCell profileCell = (DrawerProfileCell) holder.itemView;
                profileCell.setUser(MessagesController.getInstance(UserConfig.selectedAccount).getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()), accountsShown);
                break;
            }
            case 3: {
                DrawerActionCell drawerActionCell2 = (DrawerActionCell) holder.itemView;
                position -= 2;
                if (this.accountsShown) {
                    position -= getAccountRowsCount();
                }
                position -= getColorItemCount();

                if (createShown) {
                    position -= getCreateItemCount();
                }
                if (featureShown) {
                    position -= getFeatureItemCount();
                }
                Item item = items.get(position);
                item.bind(drawerActionCell2);
                if (item.id == 6 || item.id == 25) {
                    if (item.id == 6) {
                        //  drawerActionCell2.setCounter(PlusFilterController.getInstance(UserConfig.selectedAccount).countOnline(),true);
                        // drawerActionCell2.setDrawCounter(false);
                    } else {
//                        int count = HulugramUtils.getUnreadCountForHulugramChannel();
//                        drawerActionCell2.setCounter(count,false);
                        // drawerActionCell2.setDrawCounter(true);

//                        if(count > 0){
//                            drawerActionCell2.setCounter(count);
//                        }else{
//                            drawerActionCell2.hideCounter();
//                        }
                    }
                } else {
                    // drawerActionCell2.setDrawCounter(false);
                    // drawerActionCell2.hideCounter();
                }
                drawerActionCell2.setPadding(0, 0, 0, 0);
                break;
            }
            case 6:
                DrawerExpandActionCell drawerExpandActionCell = (DrawerExpandActionCell) holder.itemView;
                position -= 2;
                if (accountsShown) {
                    position -= getAccountRowsCount();
                }
                position -= getColorItemCount();

                Item item = items.get(position);
                drawerExpandActionCell.setTextAndIcon(item.text, item.icon);
                drawerExpandActionCell.setPadding(0, 0, 0, 0);
                break;
            case 7:
                FeatureExpandCell featureExpandCell = (FeatureExpandCell) holder.itemView;
                position -= 2;
                if (accountsShown) {
                    position -= getAccountRowsCount();
                }
                position -= getColorItemCount();

                if (createShown) {
                    position -= getCreateItemCount();
                }

                Item item1 = items.get(position);
                featureExpandCell.setTextAndIcon(item1.text, item1.icon);
                featureExpandCell.setPadding(0, 0, 0, 0);
                break;
            case 4: {
                DrawerUserCell drawerUserCell = (DrawerUserCell) holder.itemView;
                drawerUserCell.setAccount(accountNumbers.get(position - 2));
                break;
            }
            case 50:
                DrawerActionCell drawerActionCell = (DrawerActionCell) holder.itemView;
                int i3 = position - 2;
                if (accountsShown) {
                    i3 -= getAccountRowsCount();
                }
                i3 -= getColorItemCount();

                createItems.get(i3 - 1).bind(drawerActionCell);
                drawerActionCell.setPadding(0, 0, 0, 0);
                break;
            case 51:
                DrawerActionCell drawerActionCell2 = (DrawerActionCell) holder.itemView;
                int i4 = position - 2;
                if (accountsShown) {
                    i4 -= getAccountRowsCount();
                }
                i4 -= getColorItemCount();

                if (createShown) {
                    i4 -= getCreateItemCount();
                }
                featuresItems.get(i4 - 2).bind(drawerActionCell2);
                drawerActionCell2.setPadding(0, 0, 0, 0);
                break;
            case 100:
                IconColorCell iconColorCell = (IconColorCell)holder.itemView;
                position -= 2;
                if (this.accountsShown) {
                    position -= getAccountRowsCount();
                }
                item = miniApps.get(position);
                iconColorCell.setTextAndIcon(item.text,item.icon,item.color,item.id);
                break;
        }
    }


    @Override
    public int getItemViewType(int i) {
        if (i == 0) {
            return 0;
        } else if (i == 1) {
            return 1;
        }
        i -= 2;
        if (accountsShown) {
            if (i < accountNumbers.size()) {
                return 4;
            } else {
                if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
                    if (i == accountNumbers.size()) {
                        return 5;
                    } else if (i == accountNumbers.size() + 1) {
                        return 2;
                    }
                } else {
                    if (i == accountNumbers.size()) {
                        return 2;
                    }
                }
            }
            i -= getAccountRowsCount();
        }


        if(i < miniApps.size()){
            return 100;
        }else if(i == miniApps.size()){
            if(!miniApps.isEmpty()){
                return 2;
            }
        }
        i -= getColorItemCount();

        if (items.isEmpty()) {
            return 2;
        }
        if (i == 0) {
            return 6;
        }
        i--;
        if (createShown) {
            if (i < createItems.size()) {
                return 50;
            } else if (i == createItems.size()) {
                return 2;
            }
            i -= getCreateItemCount();
        }


        if (i == 0) {
            return 7;
        }
        i -= 1;



        if (featureShown) {
            if (i < featuresItems.size()) {
                return 51;
            } else if (i == featuresItems.size()) {
                return 2;

            }
            i -= getFeatureItemCount();
        }

        return items.get(i + 2) == null ? 2 : 3;

    }

    public void swapElements(int fromIndex, int toIndex) {
        int idx1 = fromIndex - 2;
        int idx2 = toIndex - 2;
        if (idx1 < 0 || idx2 < 0 || idx1 >= accountNumbers.size() || idx2 >= accountNumbers.size()) {
            return;
        }
        final UserConfig userConfig1 = UserConfig.getInstance(accountNumbers.get(idx1));
        final UserConfig userConfig2 = UserConfig.getInstance(accountNumbers.get(idx2));
        final int tempLoginTime = userConfig1.loginTime;
        userConfig1.loginTime = userConfig2.loginTime;
        userConfig2.loginTime = tempLoginTime;
        userConfig1.saveConfig(false);
        userConfig2.saveConfig(false);
        Collections.swap(accountNumbers, idx1, idx2);
        notifyItemMoved(fromIndex, toIndex);
    }

    private void resetItems() {
        accountNumbers.clear();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (PasscodeHelper.isProtectedAccount(UserConfig.getInstance(a).getClientUserId())) continue;
            if (UserConfig.getInstance(a).isClientActivated()) {
                accountNumbers.add(a);
            }
        }
        Collections.sort(accountNumbers, (o1, o2) -> {
            long l1 = UserConfig.getInstance(o1).loginTime;
            long l2 = UserConfig.getInstance(o2).loginTime;
            if (l1 > l2) {
                return 1;
            } else if (l1 < l2) {
                return -1;
            }
            return 0;
        });

        items.clear();
        createItems.clear();
        featuresItems.clear();
        miniApps.clear();
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        int eventType = Theme.getEventType();
        if (OwlConfig.eventType > 0) {
            eventType = OwlConfig.eventType - 1;
        }
        int newGroupIcon;
        int newSecretIcon;
        int newChannelIcon;
        int contactsIcon;
        int callsIcon;
        int savedIcon;
        int settingsIcon;
        int inviteIcon;
        int helpIcon;
        int peopleNearbyIcon;
        if (eventType == 0) {
            newGroupIcon = R.drawable.msg_groups_ny;
            newSecretIcon = R.drawable.msg_secret_ny;
            newChannelIcon = R.drawable.msg_channel_ny;
            contactsIcon = R.drawable.msg_contacts_ny;
            callsIcon = R.drawable.msg_calls_ny;
            savedIcon = R.drawable.msg_saved_ny;
            settingsIcon = R.drawable.msg_settings_ny;
            inviteIcon = R.drawable.msg_invite_ny;
            helpIcon = R.drawable.msg_help_ny;
            peopleNearbyIcon = R.drawable.msg_nearby_ny;
        } else if (eventType == 1) {
            newGroupIcon = R.drawable.msg_groups_14;
            newSecretIcon = R.drawable.msg_secret_14;
            newChannelIcon = R.drawable.msg_channel_14;
            contactsIcon = R.drawable.msg_contacts_14;
            callsIcon = R.drawable.msg_calls_14;
            savedIcon = R.drawable.msg_saved_14;
            settingsIcon = R.drawable.msg_settings_14;
            inviteIcon = R.drawable.msg_secret_ny;
            helpIcon = R.drawable.msg_help_14;
            peopleNearbyIcon = R.drawable.msg_secret_14;
        } else if (eventType == 2) {
            newGroupIcon = R.drawable.msg_groups_hw;
            newSecretIcon = R.drawable.msg_secret_hw;
            newChannelIcon = R.drawable.msg_channel_hw;
            contactsIcon = R.drawable.msg_contacts_hw;
            callsIcon = R.drawable.msg_calls_hw;
            savedIcon = R.drawable.msg_saved_hw;
            settingsIcon = R.drawable.msg_settings_hw;
            inviteIcon = R.drawable.msg_invite_hw;
            helpIcon = R.drawable.msg_help_hw;
            peopleNearbyIcon = R.drawable.msg_nearby_hw;
        } else if (eventType == 3) {
            newGroupIcon = R.drawable.menu_groups_cn;
            newSecretIcon = R.drawable.menu_secret_cn;
            newChannelIcon = R.drawable.menu_broadcast_cn;
            contactsIcon = R.drawable.menu_contacts_cn;
            callsIcon = R.drawable.menu_calls_cn;
            savedIcon = R.drawable.menu_bookmarks_cn;
            settingsIcon = R.drawable.menu_settings_cn;
            inviteIcon = R.drawable.menu_invite_cn;
            helpIcon = R.drawable.msg_help_hw;
            peopleNearbyIcon = R.drawable.menu_nearby_cn;
        } else {
            newGroupIcon = R.drawable.msg_groups;
            newSecretIcon = R.drawable.msg_secret;
            newChannelIcon = R.drawable.msg_channel;
            contactsIcon = R.drawable.msg_contacts;
            callsIcon = R.drawable.msg_calls;
            savedIcon = R.drawable.msg_saved;
            settingsIcon = R.drawable.msg_settings_old;
            inviteIcon = R.drawable.msg_invite;
            helpIcon = R.drawable.msg_help;
            peopleNearbyIcon = R.drawable.msg_nearby;
        }
        UserConfig me = UserConfig.getInstance(UserConfig.selectedAccount);

        miniApps.add(new Item(106, LocaleController.getString("ContactChanges", R.string.ContactChanges), R.drawable.ic_user_list,0xff00AE3F));
        miniApps.add(new Item(105, LocaleController.getString("AttachMusic", R.string.AttachMusic), R.drawable.ic_music,0xff2196f3));
        miniApps.add(new Item(107, LocaleController.getString("Proxy", R.string.Proxy), R.drawable.proxy_on,Theme.getColor(Theme.key_avatar_backgroundViolet)));
//        miniApps.add(new Item(108, LocaleController.getString("GhostSetting", R.string.GhostSetting), R.drawable.msg_secret_hw,Theme.getColor(Theme.key_avatar_backgroundArchived)));
        miniApps.add(new Item(201, LocaleController.getString("OwlSetting", R.string.OwlSetting),settingsIcon,Theme.getColor(Theme.key_avatar_backgroundArchived)));

        createItems.add(new Item(2, LocaleController.getString("NewGroup", R.string.NewGroup), newGroupIcon));
        createItems.add(new Item(3, LocaleController.getString("NewSecretChat", R.string.NewSecretChat), newSecretIcon));
        createItems.add(new Item(4, LocaleController.getString("NewChannel", R.string.NewChannel), newChannelIcon));

        featuresItems.add(new Item(101,LocaleController.getString("DownloadsTabs",R.string.DownloadsTabs), R.drawable.ic_download));
        featuresItems.add(new Item(102,LocaleController.getString("HiddenChat",R.string.HiddenChat), R.drawable.ic_incoginito));

        items.add(new Item(103, LocaleController.getString("Features", R.string.Features), R.drawable.msg_limit_folder));
        items.add(new Item(104, LocaleController.getString("Create", R.string.Create), R.drawable.msg_edit));

        int item_size = MenuOrderManager.sizeAvailable();
        for(int i = 0; i < item_size; i++) {
            MenuOrderManager.EditableMenuItem data = MenuOrderManager.getSingleAvailableMenuItem(i);
            if (data != null) {
                switch (data.id) {
                    case "edit_menu":
                        break;
                    case "new_group":
                        items.add(new DrawerLayoutAdapter.Item(2, data.text, newGroupIcon));
                        break;
                    case "new_channel":
                        items.add(new DrawerLayoutAdapter.Item(4, data.text, newChannelIcon));
                        break;
                    case "new_secret_chat":
                        items.add(new DrawerLayoutAdapter.Item(3, data.text, newSecretIcon));
                        break;
                    case "contacts":
                        items.add(new DrawerLayoutAdapter.Item(6, data.text, contactsIcon));
                        break;
                    case "calls":
                        items.add(new DrawerLayoutAdapter.Item(10, data.text, callsIcon));
                        break;
                    case "nearby_people":
                        if (hasGps) {
                            items.add(new DrawerLayoutAdapter.Item(12, data.text, peopleNearbyIcon));
                        }
                        break;
                    case "saved_message":
                        items.add(new DrawerLayoutAdapter.Item(11, data.text, savedIcon));
                        break;
                    case "settings":
                        items.add(new DrawerLayoutAdapter.Item(8, data.text, settingsIcon));
                        break;
                    case "owlgram_settings":
                        items.add(new DrawerLayoutAdapter.Item(201, data.text, settingsIcon));
                        break;
                    case "invite_friends":
                        items.add(new DrawerLayoutAdapter.Item(7, data.text, inviteIcon));
                        break;
                    case "telegram_features":
                        items.add(new DrawerLayoutAdapter.Item(13, data.text, helpIcon));
                        break;
                    case "archived_messages":
                        items.add(new DrawerLayoutAdapter.Item(202, data.text, R.drawable.msg_archive));
                        break;
                    case "datacenter_status":
                        items.add(new DrawerLayoutAdapter.Item(203, data.text, R.drawable.datacenter_status));
                        break;
                    case "qr_login":
                        items.add(new DrawerLayoutAdapter.Item(204, LocaleController.getString("AuthAnotherClient", R.string.AuthAnotherClient), R.drawable.msg_qrcode));
                        break;
                    case "set_status":
                        if (me != null && me.isPremium()) {
                            if (me.getEmojiStatus() != null) {
                                items.add(new DrawerLayoutAdapter.Item(15, LocaleController.getString("ChangeEmojiStatus", R.string.ChangeEmojiStatus), 0, R.raw.emoji_status_change_to_set,true));
                            } else {
                                items.add(new DrawerLayoutAdapter.Item(15, LocaleController.getString("SetEmojiStatus", R.string.SetEmojiStatus),0,  R.raw.emoji_status_change_to_set,true));
                            }
                        }
                        break;
                    case "divider":
                        boolean foundPreviousDivider = false;
                        if (i > 0) {
                            MenuOrderManager.EditableMenuItem previousData = MenuOrderManager.getSingleAvailableMenuItem(i - 1);
                            if (previousData != null && previousData.id.equals("divider")) {
                                foundPreviousDivider = true;
                            }
                        }
                        if ((items.size() != 0 || i == 0) && !foundPreviousDivider) {
                            items.add(null);
                        }
                        break;
                }
            }
        }
    }
    public int getId(int position) {
        position -= 2;
        if (accountsShown) {
            position -= getAccountRowsCount();
        }

        if (createShown) {
            position -= getCreateItemCount();
        }

        if (featureShown) {
            position -= getFeatureItemCount();
        }

        if (position < 0 || position >= items.size()) {
            return -1;
        }
        Item item = items.get(position);
        return item != null ? item.id : -1;
    }


    public int getFirstAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 2;
    }

    public int getLastAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 1 + accountNumbers.size();
    }

    private static class Item {
        public int icon;
        public int lottieIcon;
        public String text;
        public int id;
        public int color;

        public Item(int id, String text, int icon) {
            this.icon = icon;
            this.id = id;
            this.text = text;
        }

        public Item(int id, String text, int icon, int col) {
            this.icon = icon;
            this.id = id;
            this.text = text;
            this.color = col;


        }
        public Item(int id, String text, int icon, int lottieIcon,boolean lotti) {
            this.icon = icon;
            this.lottieIcon = lottieIcon;
            this.id = id;
            this.text = text;

        }

        public void bind(DrawerActionCell actionCell) {
            actionCell.setTextAndIcon(id, text, icon, lottieIcon);
        }
    }

    public static class DrawerExpandActionCell extends FrameLayout {

        private TextView textView;
        private RectF rect = new RectF();
        private ImageView arrowView;


        private boolean show;

        public DrawerExpandActionCell(Context context) {
            super(context);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            textView.setCompoundDrawablePadding(AndroidUtilities.dp(29));
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 19, 0, 16, 0));

            arrowView = new ImageView(context);
            arrowView.setScaleType(ImageView.ScaleType.CENTER);
            Drawable drawable = getResources().getDrawable(R.drawable.arrow_more).mutate();
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY));
            }
            arrowView.setImageDrawable(drawable);
            addView(arrowView, LayoutHelper.createFrame(59, 59, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
            setArrowState(false);
        }


        public void setShow(boolean expand) {
            show = expand;
        }

        private void setArrowState(boolean animated) {
            final float rotation = show ? -90.0f : 0.0f;
            if (animated) {
                arrowView.animate().rotation(rotation).setDuration(220).setInterpolator(CubicBezierInterpolator.EASE_OUT).start();
            } else {
                arrowView.animate().cancel();
                arrowView.setRotation(rotation);
            }
            arrowView.setContentDescription(show ? LocaleController.getString("AccDescrHideAccounts", R.string.AccDescrHideAccounts) : LocaleController.getString("AccDescrShowAccounts", R.string.AccDescrShowAccounts));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            arrowView.getDrawable().setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY));
        }

        public void setCreateShown(boolean value, boolean animated) {
            if (show == value) {
                return;
            }
            show = value;
            setArrowState(animated);
        }


        public void setTextAndIcon(String text, int resId) {
            try {
                textView.setText(text);
                Drawable drawable = getResources().getDrawable(resId).mutate();
                if (drawable != null) {
                    drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY));
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    public static class FeatureExpandCell extends FrameLayout {

        private TextView textView;
        private RectF rect = new RectF();
        private ImageView arrowView;


        private boolean show;

        public FeatureExpandCell(Context context) {
            super(context);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            textView.setCompoundDrawablePadding(AndroidUtilities.dp(29));
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 19, 0, 16, 0));

            arrowView = new ImageView(context);
            arrowView.setScaleType(ImageView.ScaleType.CENTER);
            Drawable drawable = getResources().getDrawable(R.drawable.arrow_more).mutate();
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY));
            }
            arrowView.setImageDrawable(drawable);
            addView(arrowView, LayoutHelper.createFrame(59, 59, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
            setArrowState(false);
        }


        public void setShow(boolean expand) {
            show = expand;
        }

        private void setArrowState(boolean animated) {
            final float rotation = show ? -90.0f : 0.0f;
            if (animated) {
                arrowView.animate().rotation(rotation).setDuration(220).setInterpolator(CubicBezierInterpolator.EASE_OUT).start();
            } else {
                arrowView.animate().cancel();
                arrowView.setRotation(rotation);
            }
            arrowView.setContentDescription(show ? LocaleController.getString("AccDescrHideAccounts", R.string.AccDescrHideAccounts) : LocaleController.getString("AccDescrShowAccounts", R.string.AccDescrShowAccounts));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            arrowView.getDrawable().setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY));
        }

        public void setFeatureShown(boolean value, boolean animated) {
            if (show == value) {
                return;
            }
            show = value;
            setArrowState(animated);
        }


        public void setTextAndIcon(String text, int resId) {
            try {
                textView.setText(text);
                Drawable drawable = getResources().getDrawable(resId).mutate();
                if (drawable != null) {
                    drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY));
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    public static class IconColorCell extends FrameLayout {

        private TextView textView;
        private RectF rect = new RectF();

        private int id;

        public int getIdValue() {
            return id;
        }

        private boolean show;

        public IconColorCell(Context context) {
            super(context);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            textView.setCompoundDrawablePadding(AndroidUtilities.dp(29));
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 19, 0, 16, 0));

        }


        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        }

        public void setTextAndIcon(String text, int resId,int color,int id) {
            try {
                this.id = id;
                textView.setText(text);
                Drawable drawable = getResources().getDrawable(resId).mutate();

                if (drawable != null) {
                    drawable.setColorFilter(new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.MULTIPLY));
                }
                Drawable back = Theme.createRoundRectDrawable(AndroidUtilities.dp(10),color);
                CombinedDrawable combinedDrawable = new CombinedDrawable(back,drawable);
                combinedDrawable.setIconSize(AndroidUtilities.dp(24),AndroidUtilities.dp(24));
                combinedDrawable.setCustomSize(AndroidUtilities.dp(34),AndroidUtilities.dp(34));
                textView.setCompoundDrawablesWithIntrinsicBounds(combinedDrawable, null, null, null);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }

    }

}
