package org.master.feature.channelCategory;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.gson.Gson;

import org.checkerframework.checker.units.qual.C;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

public class ChannelFragment extends BaseFragment {


    public static final String  category_link = "https://fastchanneladdinglink.firebaseio.com/channel_cat.json";

    private ArrayList<Category> categories = new ArrayList<>();

    private RecyclerListView listView;
    private ListAdapter adapter;
    private StaggeredGridLayoutManager staggeredGridLayoutManager;

    private FlickerLoadingView flickerLoadingView;
    private EmptyTextProgressView  emptyView;

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
            imageView.setAnimation(R.raw.utyan_call, 120, 120);
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
            emptyTextView1.setText(LocaleController.getString("NoRecentCalls", R.string.NoRecentCalls));
            emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            emptyTextView1.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            emptyTextView1.setGravity(Gravity.CENTER);
            addView(emptyTextView1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 40, 17, 0));

            emptyTextView2 = new TextView(context);
            String help = LocaleController.getString("NoRecentCallsInfo", R.string.NoRecentCallsInfo);
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

    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Channel Category");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        listView.setLayoutManager(staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.left = AndroidUtilities.dp(2);
                outRect.right = AndroidUtilities.dp(2);
                outRect.top = AndroidUtilities.dp(2);
                outRect.bottom = AndroidUtilities.dp(2);

            }
        });
        listView.setOnItemClickListener((view, position) -> {
            ChannelListBottomSheet channelListBottomSheet =new ChannelListBottomSheet(ChannelFragment.this,categories.get(position).channels);
            channelListBottomSheet.setDelegate(new ChannelListBottomSheet.ChannelsListBottomSheetDelegate() {
                @Override
                public void didSelectChannel(Channel filter) {
                   getMessagesController().openByUserName(filter.username,ChannelFragment.this,0);
                }
            });
            showDialog(channelListBottomSheet);
        });
        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setViewType(FlickerLoadingView.CALL_LOG_TYPE);
        flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flickerLoadingView.showDate(false);
        emptyView = new EmptyTextProgressView(context, flickerLoadingView);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setEmptyView(emptyView);
        emptyView.showProgress();
        loadData();

        return fragmentView;
    }



    private  boolean loading;
    private void loadData(){
        if(loading){
            return;
        }
        loading = true;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                String result = "";

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("channelData",Context.MODE_PRIVATE);
                result = preferences.getString("channelData","");
                long lastUpdateTime = preferences.getLong("lastUpdateTime",0);
                long dur = System.currentTimeMillis() - lastUpdateTime;
                if(result.isEmpty() || dur > 24 * 60 * 60 * 1000){
                    URL url = new URL(category_link);
                    URLConnection urlConn = url.openConnection();
                    HttpsURLConnection httpsConn = (HttpsURLConnection) urlConn;
                    httpsConn.setRequestMethod("GET");
                    httpsConn.connect();
                    InputStream in;
                    if(httpsConn.getResponseCode() ==  200){
                        in = httpsConn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                in, "iso-8859-1"), 8);
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        in.close();
                        result = sb.toString();
                        preferences.edit().putString("channelData",result).commit();
                        preferences.edit().putLong("lastUpdateTime",System.currentTimeMillis()).commit();
                    }
                }

                ArrayList<Category> categoryArrayList = new ArrayList<>();
                JSONObject jsonObject = new JSONObject(result);
                Gson gson = new Gson();

                Iterator<String> keysM = jsonObject.keys();

                while (keysM.hasNext()){
                   String s  = keysM.next();

                    JSONObject jsonObject1  =  jsonObject.getJSONObject(s);
                    Category category = new Category();
                    category.image = jsonObject1.getString("image");
                    category.title = jsonObject1.getString("title");
                    category.key = s;
                    JSONObject data = jsonObject1.getJSONObject("data");
                    ArrayList<Channel> channels = new ArrayList<>();

                   Iterator<String> keys = data.keys();
                    while (keys.hasNext()){
                        String  next = keys.next();
                        Channel channel = gson.fromJson(data.getJSONObject(next).toString(),Channel.class);
                        channels.add(channel);
                    }
                    category.channels = channels;
                    categoryArrayList.add(category);
                }

                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Collections.reverse(categoryArrayList);
                        categories = categoryArrayList;
                        showDetial();
                    }
                });
                loading = false;


            }catch (Exception e){
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        loading = false;
                        showDetial();
                    }
                });

            }


        });


    }


    private void showDetial(){
        emptyView.showTextView();

        if(adapter != null){
            adapter.notifyDataSetChanged();
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    public static int getItemSize(int itemsCount) {
         int itemWidth;
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
        }
        itemWidth = (int) (itemWidth/AndroidUtilities.density);
        return itemWidth;
    }
    private class ItemView extends FrameLayout{

        private BackupImageView imageView;
        TextView updateTitle;
        public ItemView(@NonNull Context context) {
            super(context);
            int colorBackground = Color.BLACK;

            imageView = new BackupImageView(context);
            addView(imageView, LayoutHelper.createFrame(getItemSize(2), getItemSize(2)));

            FrameLayout cardView = new FrameLayout(context);
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[] {colorBackground, AndroidUtilities.getTransparentColor(colorBackground, 0)});
            cardView.setBackground(gd);
            addView(cardView, LayoutHelper.createFrame(getItemSize(2), getItemSize(2)));


            updateTitle = new TextView(context);
            updateTitle.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(0), 0,AndroidUtilities.dp(16));
            updateTitle.setTextColor(Color.WHITE);
            updateTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 23);
            cardView.addView(updateTitle, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,Gravity.BOTTOM|Gravity.LEFT));


        }

        public void setCategoru(Category category){
            imageView.setImage(category.image, null, null);
            updateTitle.setText(category.title);


        }
    }



    private class ListAdapter extends RecyclerListView.SelectionAdapter{

        private Context context;

        public ListAdapter(Context context) {
           this.context  = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new ItemView(context));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemView itemView  = (ItemView) holder.itemView;
            itemView.setCategoru(categories.get(position));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }
    }

}
