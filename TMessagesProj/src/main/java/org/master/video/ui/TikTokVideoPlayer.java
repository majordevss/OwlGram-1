package org.master.video.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SecureDocument;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.OtherDocumentPlaceholderDrawable;
import org.telegram.ui.Components.PipVideoOverlay;
import org.telegram.ui.Components.RadialProgress;
import org.telegram.ui.Components.VideoEditTextureView;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.Components.VideoPlayerSeekBar;
import org.telegram.ui.PhotoViewer;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

public class TikTokVideoPlayer  {

    private WindowManager.LayoutParams windowLayoutParams;
    private FrameLayout containerView;
    private ViewPager2 viewPager2;
    private ActionBar actionBar;

    private int currentAccount;

    private int currentPagerIndex;

    @SuppressLint("StaticFieldLeak")
    private static volatile TikTokVideoPlayer Instance = null;

    public static TikTokVideoPlayer getInstance() {
        TikTokVideoPlayer localInstance = Instance;
        if (localInstance == null) {
            synchronized (TikTokVideoPlayer.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new TikTokVideoPlayer();
                }
            }
        }
        return localInstance;
    }

    public void openVideo(int index, ArrayList<MessageObject> messageObjects){

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


        public TikTokItemLayout(@NonNull Context context,Activity activity) {
            super(context);
            setWillNotDraw(false);
            this.parentActivity = activity;

            radialProgress = new RadialProgress(this);

            videoPlayerSeekbar = new VideoPlayerSeekBar(containerView);
            videoPlayerSeekbar.setHorizontalPadding(AndroidUtilities.dp(2));
            videoPlayerSeekbar.setColors(0x33ffffff, 0x33ffffff, Color.WHITE, Color.WHITE, Color.WHITE, 0x59ffffff);

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

        public void setMessageObject(int pos,MessageObject messageObject){
            if(currentMessageObject == messageObject){
                return;
            }
            currentPosition = pos;
            currentMessageObject = messageObject;
            currentFileName = messageObject.getFileName();

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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


    }







}
