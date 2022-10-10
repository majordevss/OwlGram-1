package it.owlgram.android.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import it.owlgram.android.OwlConfig;
import it.owlgram.android.components.LabsHeader;
import it.owlgram.android.helpers.MonetIconsHelper;
import it.owlgram.android.helpers.PopupHelper;
import it.owlgram.android.translator.DeepLTranslator;

public class OwlgramExperimentalSettings extends BaseFragment {

    private int rowCount;
    private ListAdapter listAdapter;

    private int checkBoxExperimentalRow;
    private int headerImageRow;
    private int bottomHeaderRow;
    private int headerExperimental;
    private int betterAudioCallRow;
    private int maxRecentStickersRow;
    private int experimentalMessageAlert;
    private int monetIconRow;
    private int downloadSpeedBoostRow;
    private int uploadSpeedBoostRow;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRowsId(true);
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("Experimental", R.string.Experimental));
        actionBar.setAllowOverlayTitle(false);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);
        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        RecyclerListView listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        if (listView.getItemAnimator() != null) {
            ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        }
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == betterAudioCallRow) {
                OwlConfig.toggleBetterAudioQuality();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(OwlConfig.betterAudioQuality);
                }
            } else if (position == maxRecentStickersRow) {
                int[] counts = {20, 30, 40, 50, 80, 100, 120, 150, 180, 200};
                ArrayList<String> types = new ArrayList<>();
                for (int count : counts) {
                    if (count <= getMessagesController().maxRecentStickersCount) {
                        types.add(String.valueOf(count));
                    }
                }
                PopupHelper.show(types, LocaleController.getString("MaxRecentStickers", R.string.MaxRecentStickers), types.indexOf(String.valueOf(OwlConfig.maxRecentStickers)), context, i -> {
                    OwlConfig.setMaxRecentStickers(Integer.parseInt(types.get(i)));
                    listAdapter.notifyItemChanged(maxRecentStickersRow);
                });
            } else if (position == checkBoxExperimentalRow) {
                if (view instanceof TextCheckCell) {
                    TextCheckCell textCheckCell = (TextCheckCell) view;
                    if (MonetIconsHelper.isSelectedMonet()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("DisableExperimentalAlert", R.string.DisableExperimentalAlert));
                        builder.setPositiveButton(LocaleController.getString("AutoDeleteConfirm", R.string.AutoDeleteConfirm), (dialogInterface, i) -> {
                            MonetIconsHelper.switchToMonet();
                            toggleExperimentalMode(textCheckCell);
                            AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                            progressDialog.show();
                            AndroidUtilities.runOnUIThread(progressDialog::dismiss, 2000);
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        builder.show();
                    } else {
                        toggleExperimentalMode(textCheckCell);
                    }
                }
            } else if (position == monetIconRow) {
                MonetIconsHelper.switchToMonet();
                AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                progressDialog.show();
                AndroidUtilities.runOnUIThread(progressDialog::dismiss, 2000);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(MonetIconsHelper.isSelectedMonet());
                }
            } else if (position == downloadSpeedBoostRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("DownloadSpeedDefault", R.string.DownloadSpeedDefault));
                types.add(OwlConfig.DOWNLOAD_BOOST_DEFAULT);
                arrayList.add(LocaleController.getString("DownloadSpeedFast", R.string.DownloadSpeedFast));
                types.add(OwlConfig.DOWNLOAD_BOOST_FAST);
                arrayList.add(LocaleController.getString("DownloadSpeedExtreme", R.string.DownloadSpeedExtreme));
                types.add(OwlConfig.DOWNLOAD_BOOST_EXTREME);
                PopupHelper.show(arrayList, LocaleController.getString("DownloadSpeed", R.string.DownloadSpeed), types.indexOf(OwlConfig.downloadSpeedBoost), context, i -> {
                    OwlConfig.setDownloadSpeedBoost(types.get(i));
                    listAdapter.notifyItemChanged(downloadSpeedBoostRow);
                });
            } else if (position == uploadSpeedBoostRow) {
                OwlConfig.toggleUploadSpeedBoost();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(OwlConfig.uploadSpeedBoost);
                }
            }
        });
        return fragmentView;
    }

    private void toggleExperimentalMode(TextCheckCell textCheckCell) {
        OwlConfig.toggleDevOpt();
        boolean isEnabled = OwlConfig.isDevOptEnabled();
        textCheckCell.setChecked(isEnabled);
        textCheckCell.setText(isEnabled ? LocaleController.getString("OnModeCheckTitle", R.string.OnModeCheckTitle):LocaleController.getString("OffModeCheckTitle", R.string.OffModeCheckTitle));
        textCheckCell.setBackgroundColorAnimated(isEnabled, Theme.getColor(isEnabled ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
        if (isEnabled) {
            listAdapter.notifyItemRemoved(experimentalMessageAlert);
            updateRowsId(false);
            listAdapter.notifyItemRangeInserted(headerImageRow, rowCount - headerImageRow);
        } else {
            listAdapter.notifyItemRangeRemoved(headerImageRow, rowCount - headerImageRow);
            updateRowsId(false);
            listAdapter.notifyItemInserted(experimentalMessageAlert);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateRowsId(boolean notify) {
        rowCount = 0;
        headerImageRow = -1;
        bottomHeaderRow = -1;
        headerExperimental = -1;
        betterAudioCallRow = -1;
        maxRecentStickersRow = -1;
        monetIconRow = -1;
        downloadSpeedBoostRow = -1;
        uploadSpeedBoostRow = -1;
        experimentalMessageAlert = -1;

        checkBoxExperimentalRow = rowCount++;
        if(OwlConfig.isDevOptEnabled() ) {
            headerImageRow = rowCount++;
            bottomHeaderRow = rowCount++;
            headerExperimental = rowCount++;
            betterAudioCallRow = rowCount++;
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) {
                monetIconRow = rowCount++;
            }
            maxRecentStickersRow = rowCount++;
            downloadSpeedBoostRow = rowCount++;
            uploadSpeedBoostRow = rowCount++;
        } else {
            experimentalMessageAlert = rowCount++;
        }

        if (listAdapter != null && notify) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 2:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    if (position == betterAudioCallRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("MediaStreamVoip", R.string.MediaStreamVoip), OwlConfig.betterAudioQuality, true);
                    } else if (position == checkBoxExperimentalRow) {
                        boolean isEnabled = OwlConfig.isDevOptEnabled();
                        textCheckCell.setDrawCheckRipple(true);
                        textCheckCell.setTextAndCheck(isEnabled ? LocaleController.getString("OnModeCheckTitle", R.string.OnModeCheckTitle):LocaleController.getString("OffModeCheckTitle", R.string.OffModeCheckTitle), isEnabled, false);
                        textCheckCell.setBackgroundColor(Theme.getColor(isEnabled ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                        textCheckCell.setColors(Theme.key_windowBackgroundCheckText, Theme.key_switchTrackBlue, Theme.key_switchTrackBlueChecked, Theme.key_switchTrackBlueThumb, Theme.key_switchTrackBlueThumbChecked);
                        textCheckCell.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                        textCheckCell.setHeight(56);
                    } else if (position == monetIconRow) {
                        textCheckCell.setTextAndValueAndCheck(LocaleController.getString("MonetIcon", R.string.MonetIcon), LocaleController.getString("MonetIconDesc", R.string.MonetIconDesc), MonetIconsHelper.isSelectedMonet(), true, true);
                    }  else if (position == uploadSpeedBoostRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString("FasterUploadSpeed", R.string.FasterUploadSpeed), OwlConfig.uploadSpeedBoost, false);
                    }
                    break;
                case 4:
                    TextInfoPrivacyCell textInfoPrivacyCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == bottomHeaderRow) {
                        textInfoPrivacyCell.setText(LocaleController.getString("ExperimentalDesc", R.string.ExperimentalDesc));
                    } else if (position == experimentalMessageAlert) {
                        textInfoPrivacyCell.setText(LocaleController.getString("ExperimentalOff", R.string.ExperimentalOff));
                    }
                    break;
                case 5:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == headerExperimental) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    }
                    break;
                case 6:
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == maxRecentStickersRow) {
                        textCell.setTextAndValue(LocaleController.getString("MaxRecentStickers", R.string.MaxRecentStickers), String.valueOf(OwlConfig.maxRecentStickers), true);
                    } else if (position == downloadSpeedBoostRow) {
                        String value;
                        switch (OwlConfig.downloadSpeedBoost) {
                            case OwlConfig.DOWNLOAD_BOOST_FAST:
                                value = LocaleController.getString("DownloadSpeedFast", R.string.DownloadSpeedFast);
                                break;
                            case OwlConfig.DOWNLOAD_BOOST_EXTREME:
                                value = LocaleController.getString("DownloadSpeedExtreme", R.string.DownloadSpeedExtreme);
                                break;
                            default:
                                value = LocaleController.getString("DownloadSpeedDefault", R.string.DownloadSpeedDefault);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("DownloadSpeed", R.string.DownloadSpeed), value, true);
                    }
                    break;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return (type == 2 && holder.getAdapterPosition() != checkBoxExperimentalRow)  || type == 6;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 2:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new LabsHeader(mContext);
                    break;
                case 4:
                    TextInfoPrivacyCell textInfoPrivacyCell = new TextInfoPrivacyCell(mContext);
                    textInfoPrivacyCell.setBottomPadding(16);
                    view = textInfoPrivacyCell;
                    break;
                case 5:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 6:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == betterAudioCallRow || position == checkBoxExperimentalRow || position == monetIconRow ||
                    position == uploadSpeedBoostRow) {
                return 2;
            } else if (position == headerImageRow) {
                return 3;
            } else if (position == bottomHeaderRow || position == experimentalMessageAlert) {
                return 4;
            } else if (position == headerExperimental) {
                return 5;
            } else if (position == maxRecentStickersRow || position == downloadSpeedBoostRow) {
                return 6;
            }
            return 1;
        }
    }
}
