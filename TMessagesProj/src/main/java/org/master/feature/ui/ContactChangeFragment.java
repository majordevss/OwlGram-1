package org.master.feature.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.master.feature.database.AppDatabase;
import org.master.feature.database.ContactChange;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

public class ContactChangeFragment extends BaseFragment{


    private ArrayList<ContactChange> contactChanges = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    private ListAdapter listAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerListView listView;
    private EmptyTextProgressView emptyTextProgressView;


    @Override
    public View createView(Context context) {
        actionBar.setCastShadows(true);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Contact Change");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                if(id == -1){
                    finishFragment();
                }else if(id == 1){
                    AlertsCreator.createSimpleAlert(context,"Delete","Delete all changes")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    AppDatabase.databaseQueue.postRunnable(new Runnable() {
                                        @Override
                                        public void run() {
                                            AppDatabase.getInstance(currentAccount).contactChangeDao().clearAll();;
                                            AndroidUtilities.runOnUIThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context,"Cleared!",Toast.LENGTH_LONG).show();
                                                    loadContactChange();
                                                    if(dialogInterface !=null){
                                                        dialogInterface.dismiss();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();;
                                }
                            })
                            .show();
                }
            }
        });


        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem otherItem =  menu.addItem(2, R.drawable.ic_ab_other);
        otherItem.addSubItem(1, R.drawable.msg_delete,"Delete All");

        actionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(layoutManager != null){
                    layoutManager.scrollToPosition(0);
                }
            }
        });
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout)fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        listAdapter = new ListAdapter(context);
        listView = new RecyclerListView(context);
        layoutManager = new LinearLayoutManager(context,RecyclerView.VERTICAL,false);
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(position < 0 || position >= contactChanges.size()){
                    return;
                }
                int  user_id = (int)contactChanges.get(position).user_id;
                Bundle args1 = new Bundle();
                args1.putInt("user_id", user_id);
                presentFragment(new ChatActivity(args1));
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyTextProgressView = new EmptyTextProgressView(context);
        emptyTextProgressView.setText("No Contact change yet!");
        emptyTextProgressView.showTextView();
        emptyTextProgressView.setTopImage(R.drawable.stickers_empty);
        frameLayout.addView(emptyTextProgressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setEmptyView(emptyTextProgressView);

        loadContactChange();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(listAdapter != null){
            listAdapter.notifyDataSetChanged();
        }
    }



    public void loadContactChange(){

        AppDatabase.databaseQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
              List<ContactChange> contactChangeList =   AppDatabase.getInstance(currentAccount).contactChangeDao().getContactChagne();
              contactChanges = new ArrayList<>(contactChangeList);
              AndroidUtilities.runOnUIThread(new Runnable() {
                  @Override
                  public void run() {
                      if(listAdapter != null){
                          listAdapter.notifyDataSetChanged();
                      }
                  }
              });

            }
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter{

        private Context context;

        public ListAdapter(Context context){
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }


        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            UserCell userCell = new UserCell(context);
            return new RecyclerListView.Holder(userCell);
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserCell userCell = (UserCell)holder.itemView;
            ContactChange contactChange = contactChanges.get(position);
            userCell.setUser(contactChange);
        }


        @Override
        public int getItemCount() {
            return contactChanges != null ? contactChanges.size():0;
        }
    }

    private class UserCell extends FrameLayout {

        private ImageView imageView;
        private ProfileSearchCell profileSearchCell;

        public UserCell(Context context) {
            super(context);

            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            profileSearchCell = new ProfileSearchCell(context);
            profileSearchCell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(32) : 0, 0, LocaleController.isRTL ? 0 : AndroidUtilities.dp(32), 0);
            profileSearchCell.setSublabelOffset(AndroidUtilities.dp(LocaleController.isRTL ? 2 : -2), -AndroidUtilities.dp(4));
            addView(profileSearchCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            imageView = new ImageView(context);
            imageView.setAlpha(214);
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.MULTIPLY));
            imageView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setOnClickListener(v -> {

            });
            imageView.setContentDescription(LocaleController.getString("Call", R.string.Call));
            addView(imageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 8, 0, 8, 0));


        }


        public void setUser(ContactChange contactChange){
            TLRPC.User user = getMessagesController().getUser(contactChange.user_id);
            if(user == null){
                return;
            }
            String status = "";
            if(contactChange.mask == MessagesController.UPDATE_MASK_AVATAR){
                status = "Change Profile at " + LocaleController.formatDate(contactChange.time);
            }
            profileSearchCell.setData(user,  null,ContactsController.formatName(user.first_name,user.last_name),status,false,false);
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }
    }

}
