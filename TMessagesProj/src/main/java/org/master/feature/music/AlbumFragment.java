package org.master.feature.music;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.RecyclerListView;

public class AlbumFragment extends BaseFragment {

    private AlbumAdapter albumAdapter;
    private RecyclerListView listView;

    private boolean loading;

    @Override
    public View createView(Context context) {
        return super.createView(context);
    }


    @Override
    public void onResume() {
        super.onResume();
        if(albumAdapter != null){
            albumAdapter.notifyDataSetChanged();
        }
    }

    private void loadAlbums(){
        if(loading){
            return;
        }
        loading = true;

    }


    private class AlbumAdapter extends RecyclerListView.SelectionAdapter{

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }
    }

}
