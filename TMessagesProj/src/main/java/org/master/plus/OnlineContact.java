package org.master.plus;


import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class OnlineContact extends BaseFragment implements NotificationCenter.NotificationCenterDelegate{


    private ArrayList<TLRPC.User> onlineUsers = new ArrayList<>();
    private ArrayList<Long> onlineUserArrlistId = new ArrayList<>();

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
        actionBar.setTitle("Online Users");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){
            @Override
            public void onItemClick(int id) {
                if(id == -1){
                    finishFragment();
                }
            }
        });


//        ActionBarMenu menu = actionBar.createMenu();
//        ActionBarMenuItem otherItem =  menu.addItem(2,R.drawable.ic_ab_other);
//        otherItem.addSubItem(1,R.drawable.msg_delete,"Delete All");

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
                if(position < 0 || position >= onlineUsers.size()){
                    return;
                }
                TLRPC.User user = onlineUsers.get(position);
                Bundle args1 = new Bundle();
                args1.putLong("user_id", user.id);
                presentFragment(new ChatActivity(args1));
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyTextProgressView = new EmptyTextProgressView(context);
        emptyTextProgressView.setText("No one is online yet!");
        emptyTextProgressView.showTextView();
        emptyTextProgressView.setTopImage(R.drawable.stickers_empty);
        frameLayout.addView(emptyTextProgressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setEmptyView(emptyTextProgressView);

        loadOnlineUsers();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(listAdapter != null){
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }

    private boolean checkOnline(TLRPC.User user) {
        boolean isOnline = !user.self && (user.status != null && user.status.expires > ConnectionsManager.getInstance(currentAccount).getCurrentTime() || MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(user.id));
        return isOnline;
    }


    public void loadOnlineUsers(){
        ArrayList<TLRPC.Dialog> users = MessagesController.getInstance(currentAccount).dialogsUsersOnly;
        for (int a = 0; a < users.size(); a++){
          TLRPC.Dialog dialogUser = users.get(a);
          if(dialogUser == null){
              continue;
          }
          TLRPC.User user = getMessagesController().getUser(dialogUser.id);
          if(user == null){
              continue;
          }
          if(!checkOnline(user)){
              continue;
          }
          if(onlineUserArrlistId.contains(user.id)){
              continue;
          }
          onlineUsers.add(user);
          onlineUserArrlistId.add(user.id);
      }

       ArrayList<TLRPC.TL_contact> onlineContacts =  ContactsController.getInstance(currentAccount).contacts;
       for(int a = 0; a < onlineContacts.size();a++){
          TLRPC.TL_contact contact = onlineContacts.get(a);
          if(contact == null){
              continue;
          }
          if (onlineUserArrlistId.contains(contact.user_id)) {
              continue;
          }
          TLRPC.User user = getMessagesController().getUser(contact.user_id);
          if(user == null){
              continue;
          }
           if(!checkOnline(user)){
               continue;
           }
          onlineUsers.add(user);
          onlineUserArrlistId.add(user.id);
      }


        if(listAdapter != null){
          listAdapter.notifyDataSetChanged();
      }
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
            UserCell userCell = new UserCell(context,8,1,false);
            return new RecyclerListView.Holder(userCell);
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserCell userCell = (UserCell)holder.itemView;
            TLRPC.User user = onlineUsers.get(position);
            if(user != null){
                userCell.setData(user,user.first_name,"online",0);
            }
        }


        @Override
        public int getItemCount() {
            return onlineUsers != null ? onlineUsers.size():0;
        }
    }
}
