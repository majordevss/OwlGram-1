package org.master.video.ui;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.FloatProperty;
import android.util.Property;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Adapters.FiltersView;
import org.telegram.ui.Cells.ContextLinkCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FadingTextViewLayout;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PipVideoOverlay;
import org.telegram.ui.Components.RadialProgress;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.Components.VideoEditTextureView;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.Components.VideoPlayerSeekBar;
import org.telegram.ui.FilteredSearchView;
import org.telegram.ui.PhotoViewer;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Filter;

public class VideoFeedFragment extends BaseFragment{

    private ViewPager2 viewPager2;
    private int currentPagerIndex;
    private ArrayList<MessageObject> messageObjects;

    public VideoFeedFragment(int index,ArrayList<MessageObject> messageObjects){
        this.currentPagerIndex = index;
        this.messageObjects = messageObjects;
    }

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    private Adapter adapter;

    public View createView(Context context) {
        actionBar.setAddToContainer(false);
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        viewPager2 = new ViewPager2(context);
        viewPager2.setOffscreenPageLimit(1);
        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
                RecyclerView.ViewHolder holder =  recyclerView.findViewHolderForLayoutPosition(currentPagerIndex);
               if(holder != null && holder.itemView instanceof TikTokItemLayout){
                  ((TikTokItemLayout)holder.itemView).releasePlayer(true);
              }
               currentPagerIndex = position;
                holder =  recyclerView.findViewHolderForLayoutPosition(currentPagerIndex);
                if(holder != null && holder.itemView instanceof TikTokItemLayout){
                    ((TikTokItemLayout)holder.itemView).startPlaying();
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        viewPager2.setAdapter(adapter = new Adapter(context));
        frameLayout.addView(viewPager2,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    private class Adapter extends RecyclerListView.SelectionAdapter{

        private Context context;

        public Adapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TikTokItemLayout tikTokItemLayout = new TikTokItemLayout(context,getParentActivity());
            tikTokItemLayout.setBackgroundColor(Color.BLACK);
            tikTokItemLayout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new RecyclerListView.Holder(tikTokItemLayout);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TikTokItemLayout itemLayout = (TikTokItemLayout)holder.itemView;
            itemLayout.setMessageObject(position,messageObjects.get(position));
        }

        @Override
        public int getItemCount() {
            return messageObjects.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }
    }



    private class TikTokItemLayout extends FrameLayout implements NotificationCenter.NotificationCenterDelegate{

        private RadialProgress radialProgress;
        private int buttonState;

        private int currentPosition;
        private String currentFileName;
        private MessageObject currentMessageObject;
        private VideoPlayer videoPlayer;
        private TextureView textureView;
        private boolean isStreaming;
        private boolean currentVideoFinishedLoading;
        private VideoPlayerSeekBar videoPlayerSeekbar;

        private long lastBufferedPositionCheck;
        private float seekToProgressPending;
        private long startedPlayTime;
        private boolean streamingAlertShown;

        private FirstFrameView firstFrameView;
        private AspectRatioFrameLayout aspectRatioFrameLayout;
        private TextureView videoTextureView;
        private Activity parentActivity;
        private boolean skipFirstBufferingProgress;
        private boolean keepScreenOnFlagSet;
        private boolean playerWasReady;
        private  boolean playerWasPlaying;
        private boolean isPlaying;

        private VideoPlayerControlFrameLayout videoPlayerControlFrameLayout;

        private Runnable updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoPlayer != null) {

                }
                if (firstFrameView != null) {
                    firstFrameView.updateAlpha();
                }
                if (isPlaying) {
                    AndroidUtilities.runOnUIThread(updateProgressRunnable, 17);
                }
            }
        };


        private Runnable setLoadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentMessageObject == null) {
                    return;
                }
                FileLoader.getInstance(currentMessageObject.currentAccount).setLoadingVideo(currentMessageObject.getDocument(), true, false);
            }
        };


        private FadingTextViewLayout nameTextView;
        private FadingTextViewLayout dateTextView;
        private FrameLayout bottomLayout;
        public TikTokItemLayout(@NonNull Context context, Activity activity) {
            super(context);
            setWillNotDraw(false);
            this.parentActivity = activity;

            bottomLayout = new FrameLayout(context) {
                @Override
                protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
                    super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
                }
            };
            bottomLayout.setBackgroundColor(0x7f000000);
            addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT));


            nameTextView = new FadingTextViewLayout(context) {
                @Override
                protected void onTextViewCreated(TextView textView) {
                    super.onTextViewCreated(textView);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    textView.setEllipsize(TextUtils.TruncateAt.END);
                    textView.setTextColor(0xffffffff);
                    textView.setGravity(Gravity.LEFT);
                }
            };

            bottomLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 5, 8, 0));


            dateTextView = new FadingTextViewLayout(getContext(), true) {

                private LocaleController.LocaleInfo lastLocaleInfo = null;
                private int staticCharsCount = 0;

                @Override
                protected void onTextViewCreated(TextView textView) {
                    super.onTextViewCreated(textView);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    textView.setEllipsize(TextUtils.TruncateAt.END);
                    textView.setTextColor(0xffffffff);
                    textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    textView.setGravity(Gravity.LEFT);
                }

                @Override
                protected int getStaticCharsCount() {
                    final LocaleController.LocaleInfo localeInfo = LocaleController.getInstance().getCurrentLocaleInfo();
                    if (lastLocaleInfo != localeInfo) {
                        lastLocaleInfo = localeInfo;
                        staticCharsCount = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().formatterYear.format(new Date()), LocaleController.getInstance().formatterDay.format(new Date())).length();
                    }
                    return staticCharsCount;
                }

                @Override
                public void setText(CharSequence text, boolean animated) {
                    if (animated) {
                        boolean dontAnimateUnchangedStaticChars = true;
                        if (LocaleController.isRTL) {
                            final int staticCharsCount = getStaticCharsCount();
                            if (staticCharsCount > 0) {
                                if (text.length() != staticCharsCount || getText() == null || getText().length() != staticCharsCount) {
                                    dontAnimateUnchangedStaticChars = false;
                                }
                            }
                        }
                        setText(text, true, dontAnimateUnchangedStaticChars);
                    } else {
                        setText(text, false, false);
                    }
                }
            };

            bottomLayout.addView(dateTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 25, 8, 0));

            radialProgress = new RadialProgress(this);

            videoPlayerControlFrameLayout = new VideoPlayerControlFrameLayout(context);
            addView(videoPlayerControlFrameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT));
            final VideoPlayerSeekBar.SeekBarDelegate seekBarDelegate = new VideoPlayerSeekBar.SeekBarDelegate() {
                @Override
                public void onSeekBarDrag(float progress) {
                    if (videoPlayer != null) {
                        long duration = videoPlayer.getDuration();
                        if (duration == C.TIME_UNSET) {
                            seekToProgressPending = progress;
                        } else {
                            videoPlayer.seekTo((int) (progress * duration));
                        }

                    }
                }

                @Override
                public void onSeekBarContinuousDrag(float progress) {

                }
            };

            videoPlayerSeekbarView = new View(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    videoPlayerSeekbar.draw(canvas, this);
                }
            };
            videoPlayerControlFrameLayout.addView(videoPlayerSeekbarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


            videoPlayerSeekbar = new VideoPlayerSeekBar(videoPlayerSeekbarView);
            videoPlayerSeekbar.setHorizontalPadding(AndroidUtilities.dp(2));
            videoPlayerSeekbar.setColors(0x33ffffff, 0x33ffffff, Color.WHITE, Color.WHITE, Color.WHITE, 0x59ffffff);
            videoPlayerSeekbar.setDelegate(seekBarDelegate);

            videoPlayerTime = new SimpleTextView(context);
            videoPlayerTime.setTextColor(0xffffffff);
            videoPlayerTime.setGravity(Gravity.RIGHT | Gravity.TOP);
            videoPlayerTime.setTextSize(14);
            videoPlayerTime.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            videoPlayerControlFrameLayout.addView(videoPlayerTime, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 0, 15, 12, 0));


            aspectRatioFrameLayout = new AspectRatioFrameLayout(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    videoTextureView.setPivotX(0);
                    firstFrameView.setPivotX(0);
                }
            };
            aspectRatioFrameLayout.setWillNotDraw(false);
            aspectRatioFrameLayout.setVisibility(View.INVISIBLE);
            addView(aspectRatioFrameLayout, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

            videoTextureView = new TextureView(context);
            videoTextureView.setPivotX(0);
            videoTextureView.setPivotY(0);
            videoTextureView.setOpaque(false);
            aspectRatioFrameLayout.addView(videoTextureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

            firstFrameView = new FirstFrameView(context);
            firstFrameView.setPivotX(0);
            firstFrameView.setPivotY(0);
            firstFrameView.setScaleType(ImageView.ScaleType.FIT_XY);
            aspectRatioFrameLayout.addView(firstFrameView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));


            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadFailed);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);

        }

        private int[] videoPlayerCurrentTime = new int[2];
        private int[] videoPlayerTotalTime = new int[2];
        private void updateVideoPlayerTime() {
            Arrays.fill(videoPlayerCurrentTime, 0);
            Arrays.fill(videoPlayerTotalTime, 0);
            if (videoPlayer != null) {
                long current = Math.max(0, videoPlayer.getCurrentPosition());
                long total = Math.max(0, videoPlayer.getDuration());
                current /= 1000;
                total /= 1000;
                videoPlayerCurrentTime[0] = (int) (current / 60);
                videoPlayerCurrentTime[1] = (int) (current % 60);
                videoPlayerTotalTime[0] = (int) (total / 60);
                videoPlayerTotalTime[1] = (int) (total % 60);
            }
            videoPlayerTime.setText(String.format(Locale.ROOT, "%02d:%02d / %02d:%02d", videoPlayerCurrentTime[0], videoPlayerCurrentTime[1], videoPlayerTotalTime[0], videoPlayerTotalTime[1]));
        }

        public void setMessageObject(int pos,MessageObject messageObject){
            if(currentMessageObject == messageObject){
                return;
            }
            currentPosition = pos;
            currentMessageObject = messageObject;
            currentFileName = messageObject.getFileName();
        }

        public void startPlaying(){
            if(currentPagerIndex == currentPosition){
                onActionClick(true);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            if (videoPlayerControlFrameLayout != null) {
                videoPlayerControlFrameLayout.parentWidth = widthSize;
                videoPlayerControlFrameLayout.parentHeight = heightSize;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        }


        public void removeObservers(){
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int x = getWidth() - AndroidUtilities.dp(24) / 2;
            int y  =  (getHeight() - AndroidUtilities.dp(24)) / 2;
            radialProgress.setProgressRect(x, y, x + AndroidUtilities.dp(24), y + AndroidUtilities.dp(24));
        }



        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.fileLoadFailed) {
                String location = (String) args[0];
                if(TextUtils.equals(currentFileName,location)){
                    radialProgress.setProgress(1,currentPosition == currentPagerIndex);
//                    checkProgress(a, false, true);

                }
            } else if (id == NotificationCenter.fileLoaded) {
                String location = (String) args[0];
                if(TextUtils.equals(currentFileName,location)){
                    radialProgress.setProgress(1,currentPosition == currentPagerIndex);
//                   checkProgress(a, false, animated);
                    if (videoPlayer == null && currentPosition == currentPagerIndex && (currentMessageObject != null && currentMessageObject.isVideo())) {
                        onActionClick(false);
                    }
                    if (videoPlayer != null) {
                        currentVideoFinishedLoading = true;
                    }
                }
            } else if (id == NotificationCenter.fileLoadProgressChanged) {
                String location = (String) args[0];
                if(TextUtils.equals(currentFileName,location)){
                    Long loadedSize = (Long) args[1];
                    Long totalSize = (Long) args[2];
                    float loadProgress = Math.min(1f, loadedSize / (float) totalSize);
                    radialProgress.setProgress(loadProgress,currentPosition == currentPagerIndex);
                    if(currentPosition == currentPagerIndex && videoPlayer != null && videoPlayerSeekbar != null){
                        float bufferedProgress;
                        if (currentVideoFinishedLoading) {
                            bufferedProgress = 1.0f;
                        }else{
                            long newTime = SystemClock.elapsedRealtime();
                            if (Math.abs(newTime - lastBufferedPositionCheck) >= 500) {
                                float progress;
                                if (seekToProgressPending == 0) {
                                    long duration = videoPlayer.getDuration();
                                    long position = videoPlayer.getCurrentPosition();
                                    if (duration >= 0 && duration != C.TIME_UNSET && position >= 0) {
                                        progress = position / (float) duration;
                                    } else {
                                        progress = 0.0f;
                                    }
                                } else {
                                    progress = seekToProgressPending;
                                }
                                bufferedProgress = isStreaming ? FileLoader.getInstance(currentAccount).getBufferedProgressFromPosition(progress, currentFileName) : 1.0f;
                                lastBufferedPositionCheck = newTime;
                            } else {
                                bufferedProgress = -1;
                            }
                        }
                        if (bufferedProgress != -1) {
                            videoPlayerSeekbar.setBufferedProgress(bufferedProgress);
//                                videoPlayerSeekbarView.invalidate();
                        }
                        checkBufferedProgress(loadProgress);

                    }
                }

            }
        }

        private void checkBufferedProgress(float progress) {
            if (!isStreaming  || streamingAlertShown || videoPlayer == null || currentMessageObject == null) {
                return;
            }
            TLRPC.Document document = currentMessageObject.getDocument();
            if (document == null) {
                return;
            }
            int innerDuration = currentMessageObject.getDuration();
            if (innerDuration < 20) {
                return;
            }
            if (progress < 0.9f && (document.size * progress >= 5 * 1024 * 1024 || progress >= 0.5f && document.size >= 2 * 1024 * 1024) && Math.abs(SystemClock.elapsedRealtime() - startedPlayTime) >= 2000) {
                long duration = videoPlayer.getDuration();
                if (duration == C.TIME_UNSET) {
                    Toast toast = Toast.makeText(getContext(), LocaleController.getString("VideoDoesNotSupportStreaming", R.string.VideoDoesNotSupportStreaming), Toast.LENGTH_LONG);
                    toast.show();
                }
                streamingAlertShown = true;
            }
        }

        private void onActionClick(boolean download) {
            if (currentMessageObject == null || currentFileName == null) {
                return;
            }
            Uri uri = null;
            File file = null;
            isStreaming = false;
            if (currentMessageObject.messageOwner.attachPath != null && currentMessageObject.messageOwner.attachPath.length() != 0) {
                file = new File(currentMessageObject.messageOwner.attachPath);
                if (!file.exists()) {
                    file = null;
                }
            }
            if (file == null) {
                file = FileLoader.getInstance(currentAccount).getPathToMessage(currentMessageObject.messageOwner);
                if (!file.exists()) {
                    file = null;
                    if (!DialogObject.isEncryptedDialog(currentMessageObject.getDialogId()) && currentMessageObject.isVideo() && currentMessageObject.canStreamVideo()) {
                        try {
                            int reference = FileLoader.getInstance(currentMessageObject.currentAccount).getFileReference(currentMessageObject);
                            FileLoader.getInstance(currentAccount).loadFile(currentMessageObject.getDocument(), currentMessageObject, 1, 0);
                            TLRPC.Document document = currentMessageObject.getDocument();
                            String params = "?account=" + currentMessageObject.currentAccount +
                                    "&id=" + document.id +
                                    "&hash=" + document.access_hash +
                                    "&dc=" + document.dc_id +
                                    "&size=" + document.size +
                                    "&mime=" + URLEncoder.encode(document.mime_type, "UTF-8") +
                                    "&rid=" + reference +
                                    "&name=" + URLEncoder.encode(FileLoader.getDocumentFileName(document), "UTF-8") +
                                    "&reference=" + Utilities.bytesToHex(document.file_reference != null ? document.file_reference : new byte[0]);
                            uri = Uri.parse("tg://" + currentMessageObject.getFileName() + params);
                            isStreaming = true;
//                            checkProgress(false, false);
                        } catch (Exception ignore) {

                        }
                    }
                }
            }
            if (file != null && uri == null) {
                uri = Uri.fromFile(file);
            }
            if (uri == null) {
                if (download) {
                    if (currentMessageObject !=  null) {
                        if (!FileLoader.getInstance(currentAccount).isLoadingFile(currentFileName)) {
                            FileLoader.getInstance(currentAccount).loadFile(currentMessageObject.getDocument(), currentMessageObject, 1, 0);
                        } else {
                            FileLoader.getInstance(currentAccount).cancelLoadFile(currentMessageObject.getDocument());
                        }
                    }
                }
            } else {
                preparePlayer(uri, true, false);
            }
        }
        private int videoWidth;
        private int videoHeight;
        private boolean videoSizeSet;

        private void preparePlayer(Uri uri, boolean playWhenReady, boolean preview) {
            streamingAlertShown = false;
            startedPlayTime = SystemClock.elapsedRealtime();
            currentVideoFinishedLoading = false;
            lastBufferedPositionCheck = 0;
            releasePlayer(false);
            playerWasPlaying = false;
            if(videoPlayer == null){
                videoPlayer = new VideoPlayer() {
                    @Override
                    public void play() {
                        super.play();
                    }

                    @Override
                    public void pause() {
                        super.pause();

                    }

                    @Override
                    public void seekTo(long positionMs) {
                        super.seekTo(positionMs);
                    }
                };
            }
            if (videoTextureView != null) {
                videoPlayer.setTextureView(videoTextureView);
            }
            if (firstFrameView != null) {
                firstFrameView.clear();
            }
            videoPlayer.setDelegate(new VideoPlayer.VideoPlayerDelegate() {
                @Override
                public void onStateChanged(boolean playWhenReady, int playbackState) {
                    updatePlayerState(playWhenReady, playbackState);
                }

                @Override
                public void onError(VideoPlayer player, Exception e) {
                    if (videoPlayer != player) {
                        return;
                    }
//                    FileLog.e(e);
//                    if (!menuItem.isSubItemVisible(gallery_menu_openin)) {
//                        return;
//                    }
//                    AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity, resourcesProvider);
//                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
//                    builder.setMessage(LocaleController.getString("CantPlayVideo", R.string.CantPlayVideo));
//                    builder.setPositiveButton(LocaleController.getString("Open", R.string.Open), (dialog, which) -> {
//                        try {
//                            AndroidUtilities.openForView(currentMessageObject, parentActivity, resourcesProvider);
//                            closePhoto(false, false);
//                        } catch (Exception e1) {
//                            FileLog.e(e1);
//                        }
//                    });
//                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
//                    showAlertDialog(builder);
                }

                @Override
                public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                    if (aspectRatioFrameLayout != null) {
                        if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                            int temp = width;
                            width = height;
                            height = temp;
                        }
                        videoWidth = (int) (width * pixelWidthHeightRatio);
                        videoHeight = (int) (height * pixelWidthHeightRatio);

                        aspectRatioFrameLayout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height, unappliedRotationDegrees);
                        videoSizeSet = true;
                    }
                }

                @Override
                public void onRenderedFirstFrame() {
                    if (firstFrameView != null && (videoPlayer == null || !videoPlayer.isLooping())) {
                        AndroidUtilities.runOnUIThread(() -> firstFrameView.updateAlpha(), 64);
                    }
                }

                @Override
                public void onRenderedFirstFrame(AnalyticsListener.EventTime eventTime) {
                    if (firstFrameView != null && (videoPlayer == null || !videoPlayer.isLooping())) {
                        AndroidUtilities.runOnUIThread(() -> firstFrameView.updateAlpha(), 64);
                    }
                }

                @Override
                public boolean onSurfaceDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }


                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                    if (firstFrameView != null) {
                        firstFrameView.checkFromPlayer(videoPlayer);
                    }
                }
            });

            videoPlayerSeekbar.setProgress(0);
            videoPlayerSeekbar.setBufferedProgress(0);
            videoPlayer.preparePlayer(uri, "other");
            videoPlayer.setPlayWhenReady(playWhenReady);
            if (currentMessageObject != null && currentMessageObject.forceSeekTo >= 0) {
                seekToProgressPending = currentMessageObject.forceSeekTo;
                currentMessageObject.forceSeekTo = -1;
            }
            videoPlayerSeekbar.updateTimestamps(currentMessageObject, videoPlayer == null ? 0L : videoPlayer.getDuration());
            updateVideoPlayerTime();
        }

        private void updatePlayerState(boolean playWhenReady, int playbackState) {
            if (videoPlayer == null) {
                return;
            }
            if (isStreaming) {
                if (playbackState == ExoPlayer.STATE_BUFFERING && skipFirstBufferingProgress) {
                    if (playWhenReady) {
                        skipFirstBufferingProgress = false;
                    }
                } else {
                    final boolean buffering = seekToProgressPending != 0 || playbackState == ExoPlayer.STATE_BUFFERING;

                }
            }
            if (aspectRatioFrameLayout != null) {
                aspectRatioFrameLayout.setKeepScreenOn(playWhenReady && (playbackState != ExoPlayer.STATE_ENDED && playbackState != ExoPlayer.STATE_IDLE));
            }
            if (playWhenReady && (playbackState != ExoPlayer.STATE_ENDED && playbackState != ExoPlayer.STATE_IDLE)) {
                try {
                    parentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    keepScreenOnFlagSet = true;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                try {
                    parentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    keepScreenOnFlagSet = false;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            if (playbackState == ExoPlayer.STATE_READY || playbackState == ExoPlayer.STATE_IDLE) {
                if (seekToProgressPending != 0) {
                    int seekTo = (int) (videoPlayer.getDuration() * seekToProgressPending);
                    videoPlayer.seekTo(seekTo);
                    seekToProgressPending = 0;
                    if (currentMessageObject != null && !FileLoader.getInstance(currentMessageObject.currentAccount).isLoadingVideoAny(currentMessageObject.getDocument())) {
                        skipFirstBufferingProgress = true;
                    }
                }
            }
            if (playbackState == ExoPlayer.STATE_READY) {
                if (aspectRatioFrameLayout.getVisibility() != View.VISIBLE) {
                    aspectRatioFrameLayout.setVisibility(View.VISIBLE);
                }

                if (currentMessageObject != null && currentMessageObject.isVideo()) {
                    AndroidUtilities.cancelRunOnUIThread(setLoadingRunnable);
                    FileLoader.getInstance(currentMessageObject.currentAccount).removeLoadingVideo(currentMessageObject.getDocument(), true, false);
                }
            } else if (playbackState == ExoPlayer.STATE_BUFFERING) {
                if (playWhenReady && currentMessageObject != null && currentMessageObject.isVideo()) {
                    if (playerWasReady) {
                        setLoadingRunnable.run();
                    } else {
                        AndroidUtilities.runOnUIThread(setLoadingRunnable, 1000);
                    }
                }
            }

            if (videoPlayer.isPlaying() && playbackState != ExoPlayer.STATE_ENDED) {
                if (!isPlaying) {
                    isPlaying = true;
                    playerWasPlaying = true;
                    AndroidUtilities.runOnUIThread(updateProgressRunnable);
                }
            } else if (isPlaying || playbackState == ExoPlayer.STATE_ENDED) {
                isPlaying = false;
                AndroidUtilities.cancelRunOnUIThread(updateProgressRunnable);
            }
            PipVideoOverlay.updatePlayButton();
            videoPlayerSeekbar.updateTimestamps(currentMessageObject, videoPlayer == null ? 0L : videoPlayer.getDuration());
//            updateVideoPlayerTime();
        }

        private void releasePlayer(boolean onClose) {
            if (videoPlayer != null) {
                AndroidUtilities.cancelRunOnUIThread(setLoadingRunnable);
                videoPlayer.releasePlayer(true);
                videoPlayer = null;
            } else {
                playerWasPlaying = false;
            }

            if (isPlaying) {
                isPlaying = false;
                AndroidUtilities.cancelRunOnUIThread(updateProgressRunnable);
            }
        }

        private class FirstFrameView extends ImageView {
            public FirstFrameView(Context context) {
                super(context);
                setAlpha(0f);
            }

            public void clear() {
                hasFrame = false;
                gotError = false;
                if (gettingFrame) {
                    gettingFrameIndex++;
                    gettingFrame = false;
                }
                setImageResource(android.R.color.transparent);
            }

            private int gettingFrameIndex = 0;
            private boolean gettingFrame = false;
            private boolean hasFrame = false;
            private boolean gotError = false;
            private VideoPlayer currentVideoPlayer;
            public void checkFromPlayer(VideoPlayer videoPlayer) {
                if (currentVideoPlayer != videoPlayer) {
                    gotError = false;
                    clear();
                }

                if (videoPlayer != null) {
                    long timeToEnd = videoPlayer.getDuration() - videoPlayer.getCurrentPosition();
                    if (!hasFrame && !gotError && !gettingFrame && timeToEnd < 1000 * 5 + fadeDuration) { // 5 seconds to get the first frame
                        final Uri uri = videoPlayer.getCurrentUri();
                        final int index = ++gettingFrameIndex;
                        Utilities.globalQueue.postRunnable(() -> {
                            try {
                                final AnimatedFileDrawable drawable = new AnimatedFileDrawable(new File(uri.getPath()), true, 0, null, null, null, 0, UserConfig.selectedAccount, false, AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y,null);
                                final Bitmap bitmap = drawable.getFrameAtTime(0);
                                drawable.recycle();
                                AndroidUtilities.runOnUIThread(() -> {
                                    if (index == gettingFrameIndex) {
                                        setImageBitmap(bitmap);
                                        hasFrame = true;
                                        gettingFrame = false;
                                    }
                                });
                            } catch (Throwable e) {
                                FileLog.e(e);
                                AndroidUtilities.runOnUIThread(() -> {
                                    gotError = true;
                                });
                            }
                        });
                        gettingFrame = true;
                    }
                }

                currentVideoPlayer = videoPlayer;
            }

            public boolean containsFrame() {
                return hasFrame;
            }

            public final static float fadeDuration = 250;
            private final TimeInterpolator fadeInterpolator = CubicBezierInterpolator.EASE_IN;

            private ValueAnimator fadeAnimator;
            private void updateAlpha() {
                if (videoPlayer == null || videoPlayer.getDuration() == C.TIME_UNSET) {
                    if (fadeAnimator != null) {
                        fadeAnimator.cancel();
                        fadeAnimator = null;
                    }
                    setAlpha(0f);
                    return;
                }
                long toDuration = Math.max(0, videoPlayer.getDuration() - videoPlayer.getCurrentPosition());
                float alpha = 1f - Math.max(Math.min(toDuration / fadeDuration, 1), 0);
                if (alpha <= 0) {
                    if (fadeAnimator != null) {
                        fadeAnimator.cancel();
                        fadeAnimator = null;
                    }
                    setAlpha(0f);
                } else if (videoPlayer.isPlaying()) {
                    if (fadeAnimator == null) {
                        fadeAnimator = ValueAnimator.ofFloat(alpha, 1f);
                        fadeAnimator.addUpdateListener(a -> {
                            setAlpha((float) a.getAnimatedValue());
                        });
                        fadeAnimator.setDuration(toDuration);
                        fadeAnimator.setInterpolator(fadeInterpolator);
                        fadeAnimator.start();
                        setAlpha(alpha);
                    }
                } else {
                    if (fadeAnimator != null) {
                        fadeAnimator.cancel();
                        fadeAnimator = null;
                    }
                    setAlpha(alpha);
                }
            }
        }

        private SimpleTextView videoPlayerTime;
        private View videoPlayerSeekbarView;

        private class VideoPlayerControlFrameLayout extends FrameLayout {

            private float progress = 1f;
            private boolean seekBarTransitionEnabled;
            private boolean translationYAnimationEnabled = true;
            private boolean ignoreLayout;
            private int parentWidth;
            private int parentHeight;

            public VideoPlayerControlFrameLayout(@NonNull Context context) {
                super(context);
                setWillNotDraw(false);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (progress < 1f) {
                    return false;
                }
                if (videoPlayerSeekbar.onTouch(event.getAction(), event.getX() - AndroidUtilities.dp(2), event.getY())) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    videoPlayerSeekbarView.invalidate();
                    return true;
                }
                return true;
            }

            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int extraWidth;
                ignoreLayout = true;
                LayoutParams layoutParams = (LayoutParams) videoPlayerTime.getLayoutParams();
                if (parentWidth > parentHeight) {

                    extraWidth = AndroidUtilities.dp(48);
                    layoutParams.rightMargin = AndroidUtilities.dp(47);
                } else {

                    extraWidth = 0;
                    layoutParams.rightMargin = AndroidUtilities.dp(12);
                }
                ignoreLayout = false;
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                long duration;
                if (videoPlayer != null) {
                    duration = videoPlayer.getDuration();
                    if (duration == C.TIME_UNSET) {
                        duration = 0;
                    }
                } else {
                    duration = 0;
                }
                duration /= 1000;
                int size = (int) Math.ceil(videoPlayerTime.getPaint().measureText(String.format(Locale.ROOT, "%02d:%02d / %02d:%02d", duration / 60, duration % 60, duration / 60, duration % 60)));
                videoPlayerSeekbar.setSize(getMeasuredWidth() - AndroidUtilities.dp(2 + 14) - size - extraWidth, getMeasuredHeight());
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                float progress = 0;
                if (videoPlayer != null) {
                    progress = videoPlayer.getCurrentPosition() / (float) videoPlayer.getDuration();
                }
                if (playerWasReady) {
                    videoPlayerSeekbar.setProgress(progress);
                }
            }

            public float getProgress() {
                return progress;
            }

            public void setProgress(float progress) {
                if (this.progress != progress) {
                    this.progress = progress;
                    onProgressChanged(progress);
                }
            }

            private void onProgressChanged(float progress) {
                videoPlayerTime.setAlpha(progress);
                if (seekBarTransitionEnabled) {
                    videoPlayerTime.setPivotX(videoPlayerTime.getWidth());
                    videoPlayerTime.setPivotY(videoPlayerTime.getHeight());
                    videoPlayerTime.setScaleX(1f - 0.1f * (1f - progress));
                    videoPlayerTime.setScaleY(1f - 0.1f * (1f - progress));
                    videoPlayerSeekbar.setTransitionProgress(1f - progress);
                } else {
                    if (translationYAnimationEnabled) {
                        setTranslationY(AndroidUtilities.dpf2(24) * (1f - progress));
                    }
                    videoPlayerSeekbarView.setAlpha(progress);
                }
            }

            public boolean isSeekBarTransitionEnabled() {
                return seekBarTransitionEnabled;
            }

            public void setSeekBarTransitionEnabled(boolean seekBarTransitionEnabled) {
                if (this.seekBarTransitionEnabled != seekBarTransitionEnabled) {
                    this.seekBarTransitionEnabled = seekBarTransitionEnabled;
                    if (seekBarTransitionEnabled) {
                        setTranslationY(0);
                        videoPlayerSeekbarView.setAlpha(1f);
                    } else {
                        videoPlayerTime.setScaleX(1f);
                        videoPlayerTime.setScaleY(1f);
                        videoPlayerSeekbar.setTransitionProgress(0f);
                    }
                    onProgressChanged(progress);
                }
            }

            public void setTranslationYAnimationEnabled(boolean translationYAnimationEnabled) {
                if (this.translationYAnimationEnabled != translationYAnimationEnabled) {
                    this.translationYAnimationEnabled = translationYAnimationEnabled;
                    if (!translationYAnimationEnabled) {
                        setTranslationY(0);
                    }
                    onProgressChanged(progress);
                }
            }
        }

    }


}
