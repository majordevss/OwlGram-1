//package org.master.video;
//
//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
//import android.animation.AnimatorSet;
//import android.animation.ObjectAnimator;
//import android.animation.TimeInterpolator;
//import android.animation.ValueAnimator;
//import android.app.Activity;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.content.pm.ActivityInfo;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.RectF;
//import android.graphics.SurfaceTexture;
//import android.graphics.drawable.Drawable;
//import android.net.Uri;
//import android.os.Build;
//import android.os.SystemClock;
//import android.text.Layout;
//import android.text.Spannable;
//import android.text.SpannableString;
//import android.text.TextUtils;
//import android.text.style.ClickableSpan;
//import android.text.style.URLSpan;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.HapticFeedbackConstants;
//import android.view.MotionEvent;
//import android.view.TextureView;
//import android.view.View;
//import android.view.ViewConfiguration;
//import android.view.ViewGroup;
//import android.view.animation.DecelerateInterpolator;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.OverScroller;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.collection.ArrayMap;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.ViewCompat;
//import androidx.core.widget.NestedScrollView;
//import androidx.dynamicanimation.animation.DynamicAnimation;
//import androidx.dynamicanimation.animation.SpringAnimation;
//import androidx.dynamicanimation.animation.SpringForce;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.viewpager2.widget.ViewPager2;
//
//import com.google.android.exoplayer2.C;
//import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
//import com.google.android.exoplayer2.util.Log;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.ApplicationLoader;
//import org.telegram.messenger.DialogObject;
//import org.telegram.messenger.DownloadController;
//import org.telegram.messenger.FileLoader;
//import org.telegram.messenger.FileLog;
//import org.telegram.messenger.ImageLoader;
//import org.telegram.messenger.ImageLocation;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.MediaController;
//import org.telegram.messenger.MediaDataController;
//import org.telegram.messenger.MessageObject;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.NotificationCenter;
//import org.telegram.messenger.R;
//import org.telegram.messenger.SecureDocument;
//import org.telegram.messenger.SharedConfig;
//import org.telegram.messenger.UserConfig;
//import org.telegram.messenger.Utilities;
//import org.telegram.tgnet.TLObject;
//import org.telegram.tgnet.TLRPC;
//import org.telegram.ui.ActionBar.ActionBar;
//import org.telegram.ui.ActionBar.ActionBarMenu;
//import org.telegram.ui.ActionBar.BaseFragment;
//import org.telegram.ui.ActionBar.BottomSheet;
//import org.telegram.ui.ActionBar.SimpleTextView;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.Components.AlertsCreator;
//import org.telegram.ui.Components.AnimatedFileDrawable;
//import org.telegram.ui.Components.BulletinFactory;
//import org.telegram.ui.Components.CombinedDrawable;
//import org.telegram.ui.Components.CubicBezierInterpolator;
//import org.telegram.ui.Components.FadingTextViewLayout;
//import org.telegram.ui.Components.FloatSeekBarAccessibilityDelegate;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.LinkPath;
//import org.telegram.ui.Components.LinkSpanDrawable;
//import org.telegram.ui.Components.PipVideoOverlay;
//import org.telegram.ui.Components.PlayPauseDrawable;
//import org.telegram.ui.Components.RadialProgressView;
//import org.telegram.ui.Components.RecyclerListView;
//import org.telegram.ui.Components.StickersAlert;
//import org.telegram.ui.Components.TextViewSwitcher;
//import org.telegram.ui.Components.URLSpanReplacement;
//import org.telegram.ui.Components.VideoEditTextureView;
//import org.telegram.ui.Components.VideoPlayer;
//import org.telegram.ui.Components.VideoPlayerSeekBar;
//import org.telegram.ui.Components.VideoSeekPreviewImage;
//import org.telegram.ui.Components.ViewHelper;
//import org.telegram.ui.Components.spoilers.SpoilersTextView;
//import org.telegram.ui.DialogsActivity;
//import org.telegram.ui.LaunchActivity;
//import org.telegram.ui.PhotoViewer;
//
//import java.io.File;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.Locale;
//
//public class PlayerFragment extends BaseFragment {
//
//    private ArrayList<MessageObject> videoMessageObject;
//    private ViewPager2 viewPager2;
//    private VideoAdapter videoAdapter;
//    private int currentIndex;
//
//
//
//
//
//    public static final String TAG = PlayerFragment.class.getSimpleName();
//
//    public PlayerFragment(ArrayList<MessageObject> messageObjects,int index){
//        this.currentIndex = index;
//        this.videoMessageObject = messageObjects;
//    }
//
//
//    @Override
//    public boolean onFragmentCreate() {
//        return super.onFragmentCreate();
//    }
//
//
//    private boolean isStatusBarVisible() {
//        return Build.VERSION.SDK_INT >= 21 && !inBubbleMode;
//    }
//
//    @Override
//    protected ActionBar createActionBar(Context context) {
//        actionBar = new ActionBar(context) {
//            @Override
//            public void setAlpha(float alpha) {
//                super.setAlpha(alpha);
//            }
//        };
//        actionBar.setOverlayTitleAnimation(true);
//        actionBar.setTitleColor(0xffffffff);
//        actionBar.setSubtitleColor(0xffffffff);
//        actionBar.setBackgroundColor(0);
//        actionBar.setOccupyStatusBar(isStatusBarVisible());
//        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, false);
//        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
//
//        ActionBarMenu menu = actionBar.createMenu();
//        menu.addItem(1,R.drawable.ic_ab_search);
//
//        return actionBar;
//    }
//
//    @Override
//    public View createView(Context context) {
//        actionBar.setAddToContainer(false);
//        fragmentView = new FrameLayout(context);
//        FrameLayout frameLayout=(FrameLayout)fragmentView;
//
//        viewPager2 = new ViewPager2(context);
//        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
//        viewPager2.setOffscreenPageLimit(1);
//        viewPager2.setAdapter(videoAdapter = new VideoAdapter(context));
//        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//              currentIndex = position;
//                Log.d(TAG,"onPageSelected " + position );
//                playVideo();
//
//            }
//        });
//        frameLayout.addView(viewPager2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
//
//        frameLayout.addView(actionBar,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,Gravity.TOP));
//
//        return fragmentView;
//    }
//
//
//
//    public void playVideo(){
//      RecyclerView recyclerView = (RecyclerView)  viewPager2.getChildAt(0);
//     RecyclerView.ViewHolder viewHolder =  recyclerView.findViewHolderForLayoutPosition(currentIndex);
//     if(viewHolder != null && viewHolder.itemView instanceof VideoItemLayout){
//         VideoItemLayout videoItemLayout = (VideoItemLayout) viewHolder.itemView;
//         videoItemLayout.startPlaying();
//     }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if(videoAdapter != null){
//            videoAdapter.notifyDataSetChanged();
//        }
//    }
//
//    @Override
//    public void onFragmentDestroy() {
//        super.onFragmentDestroy();
//    }
//
//    private class VideoAdapter extends RecyclerListView.SelectionAdapter{
//
//        private Context context;
//
//        public VideoAdapter(Context context) {
//            this.context = context;
//        }
//
//        @NonNull
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            VideoItemLayout videoItemLayout= new VideoItemLayout(context);
//            videoItemLayout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//            return new RecyclerListView.Holder(videoItemLayout);
//        }
//
//
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//            VideoItemLayout itemLayout = (VideoItemLayout)holder.itemView;
//            itemLayout.setMessageObject(videoMessageObject.get(position));
//        }
//
//        @Override
//        public int getItemCount() {
//            return videoMessageObject.size();
//        }
//
//        @Override
//        public boolean isEnabled(RecyclerView.ViewHolder holder) {
//            return false;
//        }
//    }
//
//    private Uri currentPlayingVideoFile;
//    private static Drawable[] progressDrawables;
//
//    private  class VideoItemLayout extends FrameLayout implements NotificationCenter.NotificationCenterDelegate{
//
//        private void onLinkLongPress(URLSpan link, TextView widget, Runnable onDismiss) {
//            int timestamp = -1;
//            BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity(), false, null, 0xff1C2229);
//            if (link.getURL().startsWith("video?")) {
//                try {
//                    String timestampStr = link.getURL().substring(link.getURL().indexOf('?') + 1);
//                    timestamp = Integer.parseInt(timestampStr);
//                } catch (Throwable ignore) {
//
//                }
//            }
//            if (timestamp >= 0) {
//                builder.setTitle(AndroidUtilities.formatDuration(timestamp, false));
//            } else {
//                builder.setTitle(link.getURL());
//            }
//            final int finalTimestamp = timestamp;
//            builder.setItems(new CharSequence[]{LocaleController.getString("Open", R.string.Open), LocaleController.getString("Copy", R.string.Copy)}, (dialog, which) -> {
//                if (which == 0) {
//                    onLinkClick(link, widget);
//                } else if (which == 1) {
//                    String url1 = link.getURL();
//                    boolean tel = false;
//                    if (url1.startsWith("mailto:")) {
//                        url1 = url1.substring(7);
//                    } else if (url1.startsWith("tel:")) {
//                        url1 = url1.substring(4);
//                        tel = true;
//                    } else if (finalTimestamp >= 0) {
//                        if (currentMessageObject != null && !currentMessageObject.scheduled) {
//                            MessageObject messageObject1 = currentMessageObject;
//                            boolean isMedia = currentMessageObject.isVideo() || currentMessageObject.isRoundVideo() || currentMessageObject.isVoice() || currentMessageObject.isMusic();
//                            if (!isMedia && currentMessageObject.replyMessageObject != null) {
//                                messageObject1 = currentMessageObject.replyMessageObject;
//                            }
//                            long dialogId = messageObject1.getDialogId();
//                            int messageId = messageObject1.getId();
//
//                            if (messageObject1.messageOwner.fwd_from != null) {
//                                if (messageObject1.messageOwner.fwd_from.saved_from_peer != null) {
//                                    dialogId = MessageObject.getPeerId(messageObject1.messageOwner.fwd_from.saved_from_peer);
//                                    messageId = messageObject1.messageOwner.fwd_from.saved_from_msg_id;
//                                } else if (messageObject1.messageOwner.fwd_from.from_id != null) {
//                                    dialogId = MessageObject.getPeerId(messageObject1.messageOwner.fwd_from.from_id);
//                                    messageId = messageObject1.messageOwner.fwd_from.channel_post;
//                                }
//                            }
//
//                            if (DialogObject.isChatDialog(dialogId)) {
//                                TLRPC.Chat currentChat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
//                                if (currentChat != null && currentChat.username != null) {
//                                    url1 = "https://t.me/" + currentChat.username + "/" + messageId + "?t=" + finalTimestamp;
//                                }
//                            } else {
//                                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
//                                if (user != null && user.username != null) {
//                                    url1 = "https://t.me/" + user.username + "/" + messageId + "?t=" + finalTimestamp;
//                                }
//                            }
//                        }
//                    }
//                    AndroidUtilities.addToClipboard(url1);
//                    String bulletinMessage;
//                    if (tel) {
//                        bulletinMessage = LocaleController.getString("PhoneCopied", R.string.PhoneCopied);
//                    } else if (url1.startsWith("#")) {
//                        bulletinMessage = LocaleController.getString("HashtagCopied", R.string.HashtagCopied);
//                    } else if (url1.startsWith("@")) {
//                        bulletinMessage = LocaleController.getString("UsernameCopied", R.string.UsernameCopied);
//                    } else {
//                        bulletinMessage = LocaleController.getString("LinkCopied", R.string.LinkCopied);
//                    }
//                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//                        BulletinFactory.of((FrameLayout) fragmentView, null).createSimpleBulletin(R.raw.voip_invite, bulletinMessage).show();
//                    }
//                }
//            });
//            builder.setOnPreDismissListener(di -> onDismiss.run());
//            BottomSheet bottomSheet = builder.create();
//            bottomSheet.scrollNavBar = true;
//            bottomSheet.show();
//            try {
//                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
//            } catch (Exception ignore) {}
//            bottomSheet.setItemColor(0,0xffffffff, 0xffffffff);
//            bottomSheet.setItemColor(1,0xffffffff, 0xffffffff);
//            bottomSheet.setBackgroundColor(0xff1C2229);
//            bottomSheet.setTitleColor(0xff8A8A8A);
//            bottomSheet.setCalcMandatoryInsets(true);
//            AndroidUtilities.setNavigationBarColor(bottomSheet.getWindow(), 0xff1C2229, false);
//            AndroidUtilities.setLightNavigationBar(bottomSheet.getWindow(), false);
//            bottomSheet.scrollNavBar = true;
//        }
//        private boolean onLinkClick(ClickableSpan link, TextView widget) {
//            if (widget != null && link instanceof URLSpan) {
//                String url = ((URLSpan) link).getURL();
//                if (url.startsWith("video")) {
//                    if (videoPlayer != null && currentMessageObject != null) {
//                        int seconds = Utilities.parseInt(url);
//                        if (videoPlayer.getDuration() == C.TIME_UNSET) {
//                            seekToProgressPending = seconds / (float) currentMessageObject.getDuration();
//                        } else {
//                            videoPlayer.seekTo(seconds * 1000L);
//                            videoPlayerSeekbar.setProgress(seconds * 1000L / (float) videoPlayer.getDuration(), true);
//                            videoPlayerSeekbarView.invalidate();
//                        }
//                    }
//                } else if (url.startsWith("#")) {
//                    if (getParentActivity() instanceof LaunchActivity) {
//                        DialogsActivity fragment = new DialogsActivity(null);
//                        fragment.setSearchString(url);
//                        ((LaunchActivity) getParentActivity()).presentFragment(fragment, false, true);
//                        closePhoto();
//                    }
//                } else {
//                    link.onClick(widget);
//                }
//            } else {
//                link.onClick(widget);
//            }
//            return true;
//        }
//
//        private boolean closePhoto(){
//            //clso the fragemt
//
//            return true;
//        }
//
//        public void removeObserver(){
//            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
//            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
//            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
//
//        }
//        private int currnetPosation;
//        private MessageObject currentMessageObject;
//        private VideoPlayer videoPlayer;
//        private FirstFrameView firstFrameView;
//        private AspectRatioFrameLayout aspectRatioFrameLayout;
//        private TextureView videoTextureView;
//        private FrameLayout bottomLayout;
//        private FadingTextViewLayout nameTextView;
//        private FadingTextViewLayout dateTextView;
//        private CaptionTextViewSwitcher captionTextViewSwitcher;
//        private CaptionScrollView captionScrollView;
//        private FrameLayout captionContainer;
//        private RadialProgressView miniProgressView;
//
//
//        private AnimatorSet miniProgressAnimator;
//        private Runnable miniProgressShowRunnable = () -> toggleMiniProgressInternal(true);
//
//        private static final int PROGRESS_NONE = -1;
//        private static final int PROGRESS_EMPTY = 0;
//        private static final int PROGRESS_CANCEL = 1;
//        private static final int PROGRESS_LOAD = 2;
//        private static final int PROGRESS_PLAY = 3;
//        private static final int PROGRESS_PAUSE = 4;
//
//        private boolean dontAutoPlay;
//
//        private boolean firstAnimationDelay;
//        private boolean bottomTouchEnabled = true;
//
//        private VideoPlayerControlFrameLayout videoPlayerControlFrameLayout;
//        private boolean currentVideoFinishedLoading;
//
//        private String[] currentFileNames = new String[1];
//        @Override
//        public void didReceivedNotification(int id, int account, Object... args) {
//            if (id == NotificationCenter.fileLoadFailed) {
//                String location = (String) args[0];
//                for (int a = 0; a < 1; a++) {
//                    if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
//                        photoProgressViews[a].setProgress(1.0f, currentIndex == currnetPosation);
//                        checkProgress(a, false, true);
//                        break;
//                    }
//                }
//            } else if (id == NotificationCenter.fileLoaded) {
//                String location = (String) args[0];
//                for (int a = 0; a < 1; a++) {
//                    if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
//                        photoProgressViews[a].setProgress(1.0f, currentIndex == currnetPosation);
//                        checkProgress(a, false, currentIndex == currnetPosation);
//                        if (videoPlayer == null && a == 0 && (currentMessageObject != null && currentMessageObject.isVideo() || currentBotInlineResult != null && (currentBotInlineResult.type.equals("video") || MessageObject.isVideoDocument(currentBotInlineResult.document)) || pageBlocksAdapter != null && pageBlocksAdapter.isVideo(currentIndex))) {
//                            onActionClick(false);
//                        }
//                        if (a == 0 && videoPlayer != null) {
//                            currentVideoFinishedLoading = true;
//                        }
//                        break;
//                    }
//                }
//            } else if (id == NotificationCenter.fileLoadProgressChanged) {
//                String location = (String) args[0];
//                for (int a = 0; a < 3; a++) {
//                    if (currentFileNames[a] != null && currentFileNames[a].equals(location)) {
//                        Long loadedSize = (Long) args[1];
//                        Long totalSize = (Long) args[2];
//                        float loadProgress = Math.min(1f, loadedSize / (float) totalSize);
//                        photoProgressViews[a].setProgress(loadProgress, currentIndex == currnetPosation);
//                        if (a == 0 && videoPlayer != null && videoPlayerSeekbar != null) {
//                            float bufferedProgress;
//                            if (currentVideoFinishedLoading) {
//                                bufferedProgress = 1.0f;
//                            } else {
//                                long newTime = SystemClock.elapsedRealtime();
//                                if (Math.abs(newTime - lastBufferedPositionCheck) >= 500) {
//                                    float progress;
//                                    if (seekToProgressPending == 0) {
//                                        long duration = videoPlayer.getDuration();
//                                        long position = videoPlayer.getCurrentPosition();
//                                        if (duration >= 0 && duration != C.TIME_UNSET && position >= 0) {
//                                            progress = position / (float) duration;
//                                        } else {
//                                            progress = 0.0f;
//                                        }
//                                    } else {
//                                        progress = seekToProgressPending;
//                                    }
//                                    bufferedProgress = isStreaming ? FileLoader.getInstance(currentAccount).getBufferedProgressFromPosition(progress, currentFileNames[0]) : 1.0f;
//                                    lastBufferedPositionCheck = newTime;
//                                } else {
//                                    bufferedProgress = -1;
//                                }
//                            }
//                            if (bufferedProgress != -1) {
//                                videoPlayerSeekbar.setBufferedProgress(bufferedProgress);
//                                PipVideoOverlay.setBufferedProgress(bufferedProgress);
//                                videoPlayerSeekbarView.invalidate();
//                            }
//                            checkBufferedProgress(loadProgress);
//                        }
//                    }
//                }
//            }
//        }
//        private void checkBufferedProgress(float progress) {
//            if (!isStreaming || getParentActivity() == null || streamingAlertShown || videoPlayer == null || currentMessageObject == null) {
//                return;
//            }
//            TLRPC.Document document = currentMessageObject.getDocument();
//            if (document == null) {
//                return;
//            }
//            int innerDuration = currentMessageObject.getDuration();
//            if (innerDuration < 20) {
//                return;
//            }
//            if (progress < 0.9f && (document.size * progress >= 5 * 1024 * 1024 || progress >= 0.5f && document.size >= 2 * 1024 * 1024) && Math.abs(SystemClock.elapsedRealtime() - startedPlayTime) >= 2000) {
//                long duration = videoPlayer.getDuration();
//                if (duration == C.TIME_UNSET) {
//                    Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("VideoDoesNotSupportStreaming", R.string.VideoDoesNotSupportStreaming), Toast.LENGTH_LONG);
//                    toast.show();
//                }
//                streamingAlertShown = true;
//            }
//        }
//        private AnimatedFileDrawable currentAnimation;
//
//        private void checkProgress(int a, boolean scroll, boolean animated) {
//            android.util.Log.d("photoviwer","checkProgress a = " + a + "currentIndex = " + currentIndex);
//            int index = currentIndex;
//            if (currentFileNames[a] != null) {
//                File f2 = null;
//                boolean fileExist = false;
//                FileLoader.FileResolver f2Resolver = null;
//                boolean isVideo = false;
//                boolean canStream = false;
//                boolean canAutoPlay = false;
//                MessageObject messageObject = null;
//                if (a == 0 && currentIndex == 0 && currentAnimation != null) {
//                    fileExist = currentAnimation.hasBitmap();
//                }
//                if (currentMessageObject != null) {
//                    if (messageObject.isVideo()) {
//                        canStream = SharedConfig.streamMedia && messageObject.canStreamVideo() && !DialogObject.isEncryptedDialog(messageObject.getDialogId());
//                        isVideo = true;
//                    }
//                }
//                File f2Final = f2;
//                FileLoader.FileResolver finalF2Resolver = f2Resolver;
//                MessageObject messageObjectFinal = messageObject;
//                boolean canStreamFinal = canStream;
//                boolean canAutoPlayFinal = !(a == 0 && dontAutoPlay) && canAutoPlay;
//                boolean isVideoFinal = isVideo;
//
//                boolean finalFileExist = fileExist;
//                Utilities.globalQueue.postRunnable(() -> {
//                    boolean exists = finalFileExist;
//                    if (!exists && f1Final != null) {
//                        exists = f1Final.exists();
//                    }
//
//                    File f2Local = f2Final;
//                    File f3Local = null;
//                    if (f2Local == null && finalF2Resolver != null) {
//                        f2Local = finalF2Resolver.getFile();
//                    } else if (finalF2Resolver != null) {
//                        f3Local = finalF2Resolver.getFile();
//                    }
//
//                    if (!exists && f2Local != null) {
//                        exists = f2Local.exists();
//                    }
//
//                    if (!exists && f3Local != null) {
//                        exists = f3Local.exists();
//                    }
//                    if (!exists && a != 0 && messageObjectFinal != null && canStreamFinal) {
//                        if (DownloadController.getInstance(currentAccount).canDownloadMedia(messageObjectFinal.messageOwner) != 0) {
//                            if ((!messageObjectFinal.shouldEncryptPhotoOrVideo())){
//                                final TLRPC.Document document = messageObjectFinal.getDocument();
//                                if (document != null) {
//                                    FileLoader.getInstance(currentAccount).loadFile(document, messageObjectFinal, 0, 10);
//                                }
//                            }
//                        }
//                    }
//                    boolean existsFinal = exists;
//                    File finalF2Local = f2Local;
//                    AndroidUtilities.runOnUIThread(() -> {
//
//                        if (isVideoFinal) {
//                            if (!FileLoader.getInstance(currentAccount).isLoadingFile(currentFileNames[a])) {
//                                photoProgressViews[a].setBackgroundState(PROGRESS_LOAD, false, true);
//                            } else {
//                                photoProgressViews[a].setBackgroundState(PROGRESS_CANCEL, false, true);
//                            }
//                        } else {
//                            photoProgressViews[a].setBackgroundState(PROGRESS_EMPTY, animated, true);
//                        }
//                        Float progress = ImageLoader.getInstance().getFileProgress(currentFileNames[a]);
//                        if (progress == null) {
//                            progress = 0.0f;
//                        }
//                        photoProgressViews[a].setProgress(progress, false);
//                        if (a == 0) {
//                        }
//                    });
//                });
//            } else {
//                boolean isLocalVideo = false;
//                if (isLocalVideo) {
//                    photoProgressViews[a].setBackgroundState(PROGRESS_PLAY, animated, true);
//                } else {
//                    photoProgressViews[a].setBackgroundState(PROGRESS_NONE, animated, true);
//                }
//            }
//        }
//
//
//        private  class SavedVideoPosition {
//
//            public final float position;
//            public final long timestamp;
//
//            public SavedVideoPosition(float position, long timestamp) {
//                this.position = position;
//                this.timestamp = timestamp;
//            }
//        }
//        public  TLRPC.FileLocation getFileLocation(ImageLocation location) {
//            if (location == null) {
//                return null;
//            }
//            return location.location;
//        }
//        private View flashView;
//        private AnimatorSet flashAnimator;
//        private boolean manuallyPaused;
//        private Runnable videoPlayRunnable;
//        private boolean previousHasTransform;
//        private float previousCropPx;
//        private float previousCropPy;
//        private float previousCropPw;
//        private float previousCropPh;
//        private float previousCropScale;
//        private float previousCropRotation;
//        private boolean previousCropMirrored;
//        private int previousCropOrientation;
//        private VideoPlayer injectingVideoPlayer;
//        private SurfaceTexture injectingVideoPlayerSurface;
//        private boolean playerInjected;
//        private boolean skipFirstBufferingProgress;
//        private boolean playerWasReady;
//        private boolean playerWasPlaying;
//        private boolean playerAutoStarted;
//        private boolean playerLooping;
//        private float seekToProgressPending;
//        private String shouldSavePositionForCurrentVideo;
//        private String shouldSavePositionForCurrentVideoShortTerm;
//        private ArrayMap<String, SavedVideoPosition> savedVideoPositions = new ArrayMap<>();
//        private long lastSaveTime;
//        private float seekToProgressPending2;
//        private boolean streamingAlertShown;
//        private long startedPlayTime;
//        private boolean keepScreenOnFlagSet;
//        private Animator videoPlayerControlAnimator;
//        private boolean videoPlayerControlVisible = true;
//        private int[] videoPlayerCurrentTime = new int[2];
//        private int[] videoPlayerTotalTime = new int[2];
//        private SimpleTextView videoPlayerTime;
//        private ImageView exitFullscreenButton;
//        private VideoPlayerSeekBar videoPlayerSeekbar;
//        private View videoPlayerSeekbarView;
//        private VideoSeekPreviewImage videoPreviewFrame;
//        private AnimatorSet videoPreviewFrameAnimation;
//        private boolean needShowOnReady;
//        private int waitingForDraw;
//        private TextureView changedTextureView;
//        private ImageView textureImageView;
//        private ImageView[] fullscreenButton = new ImageView[3];
//        private boolean allowShowFullscreenButton;
//        private int[] pipPosition = new int[2];
//        private boolean pipAnimationInProgress;
//        private Bitmap currentBitmap;
//        private boolean changingTextureView;
//        private int waitingForFirstTextureUpload;
//        private boolean textureUploaded;
//        private boolean videoSizeSet;
//        private boolean isInline;
//        private boolean pipVideoOverlayAnimateFlag = true;
//        private boolean switchingInlineMode;
//        private boolean videoCrossfadeStarted;
//        private float videoCrossfadeAlpha;
//        private long videoCrossfadeAlphaLastTime;
//        private boolean isPlaying;
//        private boolean isStreaming;
//        private long lastBufferedPositionCheck;
//        private View playButtonAccessibilityOverlay;
//        private StickersAlert masksAlert;
//        private int lastImageId = -1;
//        private PhotoProgressView[] photoProgressViews = new PhotoProgressView[3];
//        private  Paint progressPaint;
//        private  DecelerateInterpolator decelerateInterpolator;
//
//        private class PhotoProgressView {
//
//            private long lastUpdateTime = 0;
//            private float radOffset = 0;
//            private float currentProgress = 0;
//            private float animationProgressStart = 0;
//            private long currentProgressTime = 0;
//            private float animatedProgressValue = 0;
//            private RectF progressRect = new RectF();
//            private int backgroundState = -1;
//            private View parent;
//            private int size = AndroidUtilities.dp(64);
//            private int previousBackgroundState = -2;
//            private float animatedAlphaValue = 1.0f;
//            private float[] animAlphas = new float[3];
//            private float[] alphas = new float[3];
//            private float scale = 1.0f;
//            private boolean visible;
//
//            private final CombinedDrawable playDrawable;
//            private final PlayPauseDrawable playPauseDrawable;
//
//            public PhotoProgressView(View parentView) {
//                if (decelerateInterpolator == null) {
//                    decelerateInterpolator = new DecelerateInterpolator(1.5f);
//                    progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                    progressPaint.setStyle(Paint.Style.STROKE);
//                    progressPaint.setStrokeCap(Paint.Cap.ROUND);
//                    progressPaint.setStrokeWidth(AndroidUtilities.dp(3));
//                    progressPaint.setColor(0xffffffff);
//                }
//                parent = parentView;
//                resetAlphas();
//
//                playPauseDrawable = new PlayPauseDrawable(28);
//                playPauseDrawable.setDuration(200);
//
//                Drawable circleDrawable = ContextCompat.getDrawable(getParentActivity(), R.drawable.circle_big);
//                playDrawable = new CombinedDrawable(circleDrawable.mutate(), playPauseDrawable);
//            }
//
//            private void updateAnimation(boolean withProgressAnimation) {
//                long newTime = System.currentTimeMillis();
//                long dt = newTime - lastUpdateTime;
//                if (dt > 18) {
//                    dt = 18;
//                }
//                lastUpdateTime = newTime;
//
//                boolean postInvalidate = false;
//
//                if (withProgressAnimation) {
//                    if (animatedProgressValue != 1 || currentProgress != 1) {
//                        radOffset += 360 * dt / 3000.0f;
//                        float progressDiff = currentProgress - animationProgressStart;
//                        if (Math.abs(progressDiff) > 0) {
//                            currentProgressTime += dt;
//                            if (currentProgressTime >= 300) {
//                                animatedProgressValue = currentProgress;
//                                animationProgressStart = currentProgress;
//                                currentProgressTime = 0;
//                            } else {
//                                animatedProgressValue = animationProgressStart + progressDiff * decelerateInterpolator.getInterpolation(currentProgressTime / 300.0f);
//                            }
//                        }
//                        postInvalidate = true;
//                    }
//
//                    if (animatedAlphaValue > 0 && previousBackgroundState != -2) {
//                        animatedAlphaValue -= dt / 200.0f;
//                        if (animatedAlphaValue <= 0) {
//                            animatedAlphaValue = 0.0f;
//                            previousBackgroundState = -2;
//                        }
//                        postInvalidate = true;
//                    }
//                }
//
//                for (int i = 0; i < alphas.length; i++) {
//                    if (alphas[i] > animAlphas[i]) {
//                        animAlphas[i] = Math.min(1f, animAlphas[i] + dt / 200f);
//                        postInvalidate = true;
//                    } else if (alphas[i] < animAlphas[i]) {
//                        animAlphas[i] = Math.max(0f, animAlphas[i] - dt / 200f);
//                        postInvalidate = true;
//                    }
//                }
//
//                if (postInvalidate) {
//                    parent.postInvalidateOnAnimation();
//                }
//            }
//
//            public void setProgress(float value, boolean animated) {
//                if (!animated) {
//                    animatedProgressValue = value;
//                    animationProgressStart = value;
//                } else {
//                    animationProgressStart = animatedProgressValue;
//                }
//                currentProgress = value;
//                currentProgressTime = 0;
//                parent.invalidate();
//            }
//
//            public void setBackgroundState(int state, boolean animated, boolean animateIcon) {
//                if (backgroundState == state) {
//                    return;
//                }
//                if (playPauseDrawable != null) {
//                    boolean animatePlayPause = animateIcon && (backgroundState == PROGRESS_PLAY || backgroundState == PROGRESS_PAUSE);
//                    if (state == PROGRESS_PLAY) {
//                        playPauseDrawable.setPause(false, animatePlayPause);
//                    } else if (state == PROGRESS_PAUSE) {
//                        playPauseDrawable.setPause(true, animatePlayPause);
//                    }
//                    playPauseDrawable.setParent(parent);
//                    playPauseDrawable.invalidateSelf();
//                }
//                lastUpdateTime = System.currentTimeMillis();
//                if (animated && backgroundState != state) {
//                    previousBackgroundState = backgroundState;
//                    animatedAlphaValue = 1.0f;
//                } else {
//                    previousBackgroundState = -2;
//                }
//                onBackgroundStateUpdated(backgroundState = state);
//                parent.invalidate();
//            }
//
//            protected void onBackgroundStateUpdated(int state) {
//            }
//
//            public void setAlpha(float value) {
//                setIndexedAlpha(0, value, false);
//            }
//
//            public void setScale(float value) {
//                scale = value;
//            }
//
//            public void setIndexedAlpha(int index, float alpha, boolean animated) {
//                if (alphas[index] != alpha) {
//                    alphas[index] = alpha;
//                    if (!animated) {
//                        animAlphas[index] = alpha;
//                    }
//                    checkVisibility();
//                    parent.invalidate();
//                }
//            }
//
//            public void resetAlphas() {
//                for (int i = 0; i < alphas.length; i++) {
//                    alphas[i] = animAlphas[i] = 1.0f;
//                }
//                checkVisibility();
//            }
//
//            private float calculateAlpha() {
//                float alpha = 1.0f;
//                for (int i = 0; i < animAlphas.length; i++) {
//                    if (i == 2) {
//                        alpha *= AndroidUtilities.accelerateInterpolator.getInterpolation(animAlphas[i]);
//                    } else {
//                        alpha *= animAlphas[i];
//                    }
//                }
//                return alpha;
//            }
//
//            private void checkVisibility() {
//                boolean newVisible = true;
//                for (int i = 0; i < alphas.length; i++) {
//                    if (alphas[i] != 1.0f) {
//                        newVisible = false;
//                        break;
//                    }
//                }
//                if (newVisible != visible) {
//                    visible = newVisible;
//                    onVisibilityChanged(visible);
//                }
//            }
//
//            protected void onVisibilityChanged(boolean visible) {
//            }
//
//            public boolean isVisible() {
//                return visible;
//            }
//
//            public int getX() {
//                return (getWidth() - (int) (size * scale)) / 2;
//            }
//
//            public int getY() {
//                int y = ((AndroidUtilities.displaySize.y + (isStatusBarVisible() ? AndroidUtilities.statusBarHeight : 0)) - (int) (size * scale)) / 2;
//                y += 0;
//                return y;
//            }
//
//            public void onDraw(Canvas canvas) {
//                int sizeScaled = (int) (size * scale);
//                int x = getX();
//                int y = getY();
//
//                final float alpha = calculateAlpha();
//
//                if (previousBackgroundState >= 0 && previousBackgroundState < progressDrawables.length + 2) {
//                    Drawable drawable;
//                    if (previousBackgroundState < progressDrawables.length) {
//                        drawable = progressDrawables[previousBackgroundState];
//                    } else {
//                        drawable = playDrawable;
//                    }
//                    if (drawable != null) {
//                        drawable.setAlpha((int) (255 * animatedAlphaValue * alpha));
//                        drawable.setBounds(x, y, x + sizeScaled, y + sizeScaled);
//                        drawable.draw(canvas);
//                    }
//                }
//
//                if (backgroundState >= 0 && backgroundState < progressDrawables.length + 2) {
//                    Drawable drawable;
//                    if (backgroundState < progressDrawables.length) {
//                        drawable = progressDrawables[backgroundState];
//                    } else {
//                        drawable = playDrawable;
//                    }
//                    if (drawable != null) {
//                        if (previousBackgroundState != -2) {
//                            drawable.setAlpha((int) (255 * (1.0f - animatedAlphaValue) * alpha));
//                        } else {
//                            drawable.setAlpha((int) (255 * alpha));
//                        }
//                        drawable.setBounds(x, y, x + sizeScaled, y + sizeScaled);
//                        drawable.draw(canvas);
//                    }
//                }
//
//                if (backgroundState == PROGRESS_EMPTY || backgroundState == PROGRESS_CANCEL || previousBackgroundState == PROGRESS_EMPTY || previousBackgroundState == PROGRESS_CANCEL) {
//                    int diff = AndroidUtilities.dp(4);
//                    if (previousBackgroundState != -2) {
//                        progressPaint.setAlpha((int) (255 * animatedAlphaValue * alpha));
//                    } else {
//                        progressPaint.setAlpha((int) (255 * alpha));
//                    }
//                    progressRect.set(x + diff, y + diff, x + sizeScaled - diff, y + sizeScaled - diff);
//                    canvas.drawArc(progressRect, -90 + radOffset, Math.max(4, 360 * animatedProgressValue), false, progressPaint);
//                    updateAnimation(true);
//                } else {
//                    updateAnimation(false);
//                }
//            }
//        }
//
//        public VideoItemLayout(@NonNull Context context) {
//            super(context);
//
//
//            if (progressDrawables == null) {
//                Drawable circleDrawable = ContextCompat.getDrawable(context, R.drawable.circle_big);
//                progressDrawables = new Drawable[] {
//                        circleDrawable, // PROGRESS_EMPTY
//                        ContextCompat.getDrawable(context, R.drawable.cancel_big), // PROGRESS_CANCEL
//                        ContextCompat.getDrawable(context, R.drawable.load_big), // PROGRESS_LOAD
//                };
//            }
//            aspectRatioFrameLayout = new AspectRatioFrameLayout(getContext());
//            aspectRatioFrameLayout.setWillNotDraw(false);
//            addView(aspectRatioFrameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
//
//            videoTextureView = new TextureView(context);
//            videoTextureView.setOpaque(false);
//            aspectRatioFrameLayout.addView(videoTextureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
//
//            firstFrameView = new FirstFrameView(context);
//            firstFrameView.setScaleType(ImageView.ScaleType.FIT_XY);
//            aspectRatioFrameLayout.addView(firstFrameView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
//
//            bottomLayout = new FrameLayout(context);
//            bottomLayout.setBackgroundColor(0x7f000000);
//            addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT));
//
//
//            captionTextViewSwitcher = new CaptionTextViewSwitcher(context);
//            captionTextViewSwitcher.setFactory(() -> createCaptionTextView());
//            captionTextViewSwitcher.setVisibility(View.INVISIBLE);
//            setCaptionHwLayerEnabled(true);
//
//            miniProgressView = new RadialProgressView(context, null);
//            miniProgressView.setUseSelfAlpha(true);
//            miniProgressView.setProgressColor(0xffffffff);
//            miniProgressView.setSize(AndroidUtilities.dp(54));
//            miniProgressView.setBackgroundResource(R.drawable.circle_big);
//            miniProgressView.setVisibility(View.INVISIBLE);
//            miniProgressView.setAlpha(0.0f);
//            addView(miniProgressView, LayoutHelper.createFrame(64, 64, Gravity.CENTER));
//
//            nameTextView = new FadingTextViewLayout(getContext()) {
//                @Override
//                protected void onTextViewCreated(TextView textView) {
//                    super.onTextViewCreated(textView);
//                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//                    textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//                    textView.setEllipsize(TextUtils.TruncateAt.END);
//                    textView.setTextColor(0xffffffff);
//                    textView.setGravity(Gravity.LEFT);
//                }
//            };
//            bottomLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 5, 8, 0));
//
//            dateTextView = new FadingTextViewLayout(getContext(), true) {
//
//                private LocaleController.LocaleInfo lastLocaleInfo = null;
//                private int staticCharsCount = 0;
//
//                @Override
//                protected void onTextViewCreated(TextView textView) {
//                    super.onTextViewCreated(textView);
//                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
//                    textView.setEllipsize(TextUtils.TruncateAt.END);
//                    textView.setTextColor(0xffffffff);
//                    textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//                    textView.setGravity(Gravity.LEFT);
//                }
//
//                @Override
//                protected int getStaticCharsCount() {
//                    final LocaleController.LocaleInfo localeInfo = LocaleController.getInstance().getCurrentLocaleInfo();
//                    if (lastLocaleInfo != localeInfo) {
//                        lastLocaleInfo = localeInfo;
//                        staticCharsCount = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().formatterYear.format(new Date()), LocaleController.getInstance().formatterDay.format(new Date())).length();
//                    }
//                    return staticCharsCount;
//                }
//
//                @Override
//                public void setText(CharSequence text, boolean animated) {
//                    if (animated) {
//                        boolean dontAnimateUnchangedStaticChars = true;
//                        if (LocaleController.isRTL) {
//                            final int staticCharsCount = getStaticCharsCount();
//                            if (staticCharsCount > 0) {
//                                if (text.length() != staticCharsCount || getText() == null || getText().length() != staticCharsCount) {
//                                    dontAnimateUnchangedStaticChars = false;
//                                }
//                            }
//                        }
//                        setText(text, true, dontAnimateUnchangedStaticChars);
//                    } else {
//                        setText(text, false, false);
//                    }
//                }
//            };
//            bottomLayout.addView(dateTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 25, 8, 0));
//
//            createVideoControlsInterface();
//
//            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadFailed);
//            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
//            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
//
//        }
//        private void createVideoControlsInterface() {
//            videoPlayerControlFrameLayout = new VideoPlayerControlFrameLayout(getContext());
//            addView(videoPlayerControlFrameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT));
//
//            final VideoPlayerSeekBar.SeekBarDelegate seekBarDelegate = new VideoPlayerSeekBar.SeekBarDelegate() {
//                @Override
//                public void onSeekBarDrag(float progress) {
//                    if (videoPlayer != null) {
//                        long duration = videoPlayer.getDuration();
//                        if (duration == C.TIME_UNSET) {
//                            seekToProgressPending = progress;
//                        } else {
//                            videoPlayer.seekTo((int) (progress * duration));
//                        }
//                        needShowOnReady = false;
//                    }
//                }
//
//                @Override
//                public void onSeekBarContinuousDrag(float progress) {
//                    if (videoPlayer != null && videoPreviewFrame != null) {
//                        videoPreviewFrame.setProgress(progress, videoPlayerSeekbar.getWidth());
//                    }
//
//                }
//            };
//
//            final FloatSeekBarAccessibilityDelegate accessibilityDelegate = new FloatSeekBarAccessibilityDelegate() {
//                @Override
//                public float getProgress() {
//                    return videoPlayerSeekbar.getProgress();
//                }
//
//                @Override
//                public void setProgress(float progress) {
//                    seekBarDelegate.onSeekBarDrag(progress);
//                    videoPlayerSeekbar.setProgress(progress);
//                    videoPlayerSeekbarView.invalidate();
//                }
//
//                @Override
//                public String getContentDescription(View host) {
//                    final String time = LocaleController.formatPluralString("Minutes", videoPlayerCurrentTime[0]) + ' ' + LocaleController.formatPluralString("Seconds", videoPlayerCurrentTime[1]);
//                    final String totalTime = LocaleController.formatPluralString("Minutes", videoPlayerTotalTime[0]) + ' ' + LocaleController.formatPluralString("Seconds", videoPlayerTotalTime[1]);
//                    return LocaleController.formatString("AccDescrPlayerDuration", R.string.AccDescrPlayerDuration, time, totalTime);
//                }
//            };
//            videoPlayerSeekbarView = new View(getContext()) {
//                @Override
//                protected void onDraw(Canvas canvas) {
//                    videoPlayerSeekbar.draw(canvas, this);
//                }
//            };
//            videoPlayerSeekbarView.setAccessibilityDelegate(accessibilityDelegate);
//            videoPlayerSeekbarView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
//            videoPlayerControlFrameLayout.addView(videoPlayerSeekbarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//
//            videoPlayerSeekbar = new VideoPlayerSeekBar(videoPlayerSeekbarView);
//            videoPlayerSeekbar.setHorizontalPadding(AndroidUtilities.dp(2));
//            videoPlayerSeekbar.setColors(0x33ffffff, 0x33ffffff, Color.WHITE, Color.WHITE, Color.WHITE, 0x59ffffff);
//            videoPlayerSeekbar.setDelegate(seekBarDelegate);
//
//            videoPreviewFrame = new VideoSeekPreviewImage(getContext(), () -> {
//
//            }) {
//                @Override
//                protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//                    super.onLayout(changed, left, top, right, bottom);
//                }
//
//                @Override
//                public void setVisibility(int visibility) {
//                    super.setVisibility(visibility);
//                    if (visibility == VISIBLE) {
//                    }
//                }
//            };
//            videoPreviewFrame.setAlpha(0.0f);
//            addView(videoPreviewFrame, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 0, 0, 0, 48 + 10));
//
//            videoPlayerTime = new SimpleTextView(getContext());
//            videoPlayerTime.setTextColor(0xffffffff);
//            videoPlayerTime.setGravity(Gravity.RIGHT | Gravity.TOP);
//            videoPlayerTime.setTextSize(14);
//            videoPlayerTime.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
//            videoPlayerControlFrameLayout.addView(videoPlayerTime, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 0, 15, 12, 0));
//
//            exitFullscreenButton = new ImageView(getContext());
//            exitFullscreenButton.setImageResource(R.drawable.msg_minvideo);
//            exitFullscreenButton.setContentDescription(LocaleController.getString("AccExitFullscreen", R.string.AccExitFullscreen));
//            exitFullscreenButton.setScaleType(ImageView.ScaleType.CENTER);
//            exitFullscreenButton.setBackground(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR));
//            exitFullscreenButton.setVisibility(View.INVISIBLE);
//            videoPlayerControlFrameLayout.addView(exitFullscreenButton, LayoutHelper.createFrame(48, 48, Gravity.TOP | Gravity.RIGHT));
//            exitFullscreenButton.setOnClickListener(v -> {
//                if (getParentActivity() == null) {
//                    return;
//                }
//                wasRotated = false;
//                fullscreenedByButton = 2;
//                if (prevOrientation == -10) {
//                    prevOrientation = getParentActivity().getRequestedOrientation();
//                }
//                getParentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            });
//        }
//        private int prevOrientation;
//        private int fullscreenedByButton;
//        private boolean wasRotated;
//
//        private boolean captionHwLayerEnabled;
//        private void setCaptionHwLayerEnabled(boolean enabled) {
//            if (captionHwLayerEnabled != enabled) {
//                captionHwLayerEnabled = enabled;
//                captionTextViewSwitcher.setLayerType(View.LAYER_TYPE_HARDWARE, null);
//                captionTextViewSwitcher.getCurrentView().setLayerType(View.LAYER_TYPE_HARDWARE, null);
//                captionTextViewSwitcher.getNextView().setLayerType(View.LAYER_TYPE_HARDWARE, null);
//            }
//        }
//        private void toggleMiniProgressInternal(final boolean show) {
//            if (show) {
//                miniProgressView.setVisibility(View.VISIBLE);
//            }
//            miniProgressAnimator = new AnimatorSet();
//            miniProgressAnimator.playTogether(ObjectAnimator.ofFloat(miniProgressView, View.ALPHA, show ? 1.0f : 0.0f));
//            miniProgressAnimator.setDuration(200);
//            miniProgressAnimator.addListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    if (animation.equals(miniProgressAnimator)) {
//                        if (!show) {
//                            miniProgressView.setVisibility(View.INVISIBLE);
//                        }
//                        miniProgressAnimator = null;
//                    }
//                }
//
//                @Override
//                public void onAnimationCancel(Animator animation) {
//                    if (animation.equals(miniProgressAnimator)) {
//                        miniProgressAnimator = null;
//                    }
//                }
//            });
//            miniProgressAnimator.start();
//        }
//
//        private void toggleMiniProgress(final boolean show, final boolean animated) {
//            AndroidUtilities.cancelRunOnUIThread(miniProgressShowRunnable);
//            if (animated) {
//                toggleMiniProgressInternal(show);
//                if (show) {
//                    if (miniProgressAnimator != null) {
//                        miniProgressAnimator.cancel();
//                        miniProgressAnimator = null;
//                    }
//                    if (firstAnimationDelay) {
//                        firstAnimationDelay = false;
//                        toggleMiniProgressInternal(true);
//                    } else {
//                        AndroidUtilities.runOnUIThread(miniProgressShowRunnable, 500);
//                    }
//                } else {
//                    if (miniProgressAnimator != null) {
//                        miniProgressAnimator.cancel();
//                        toggleMiniProgressInternal(false);
//                    }
//                }
//            } else {
//                if (miniProgressAnimator != null) {
//                    miniProgressAnimator.cancel();
//                    miniProgressAnimator = null;
//                }
//                miniProgressView.setAlpha(show ? 1.0f : 0.0f);
//                miniProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
//            }
//        }
//
//        private TextView createCaptionTextView() {
//            TextView textView = new SpoilersTextView(getContext()) {
//
//                private LinkSpanDrawable<ClickableSpan> pressedLink;
//                private LinkSpanDrawable.LinkCollector links = new LinkSpanDrawable.LinkCollector(this);
//
//                @Override
//                public boolean onTouchEvent(MotionEvent event) {
//
//                    boolean linkResult = false;
//                    if (event.getAction() == MotionEvent.ACTION_DOWN || pressedLink != null && event.getAction() == MotionEvent.ACTION_UP) {
//                        int x = (int) (event.getX() - getPaddingLeft());
//                        int y = (int) (event.getY() - getPaddingTop());
//                        final int line = getLayout().getLineForVertical(y);
//                        final int off = getLayout().getOffsetForHorizontal(line, x);
//                        final float left = getLayout().getLineLeft(line);
//
//                        ClickableSpan touchLink = null;
//                        if (left <= x && left + getLayout().getLineWidth(line) >= x && y >= 0 && y <= getLayout().getHeight()) {
//                            Spannable buffer = new SpannableString(getText());
//                            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
//                            if (link.length != 0) {
//                                touchLink = link[0];
//                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                                    linkResult = true;
//                                    links.clear();
//                                    pressedLink = new LinkSpanDrawable<>(link[0], null, event.getX(), event.getY());
//                                    pressedLink.setColor(0x6662a9e3);
//                                    links.addLink(pressedLink);
//                                    int start = buffer.getSpanStart(pressedLink.getSpan());
//                                    int end = buffer.getSpanEnd(pressedLink.getSpan());
//                                    LinkPath path = pressedLink.obtainNewPath();
//                                    path.setCurrentLayout(getLayout(), start, getPaddingTop());
//                                    getLayout().getSelectionPath(start, end, path);
//
//                                    final LinkSpanDrawable<ClickableSpan> savedPressedLink = pressedLink;
//                                    postDelayed(() -> {
//                                        if (savedPressedLink == pressedLink && pressedLink != null && pressedLink.getSpan() instanceof URLSpan) {
//                                            onLinkLongPress((URLSpan) pressedLink.getSpan(), this, () -> links.clear());
//                                            pressedLink = null;
//                                        }
//                                    }, ViewConfiguration.getLongPressTimeout());
//                                }
//                            }
//                        }
//                        if (event.getAction() == MotionEvent.ACTION_UP) {
//                            links.clear();
//                            if (pressedLink != null && pressedLink.getSpan() == touchLink) {
//                                onLinkClick(pressedLink.getSpan(), this);
//                            }
//                            pressedLink = null;
//                            linkResult = true;
//                        }
//                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
//                        links.clear();
//                        pressedLink = null;
//                        linkResult = true;
//                    }
//
//                    boolean b = linkResult || super.onTouchEvent(event);
//                    return bottomTouchEnabled && b;
//                }
//
//                @Override
//                public void setPressed(boolean pressed) {
//                    final boolean needsRefresh = pressed != isPressed();
//                    super.setPressed(pressed);
//                    if (needsRefresh) {
//                        invalidate();
//                    }
//                }
//
//                @Override
//                protected void onDraw(Canvas canvas) {
//                    canvas.save();
//                    canvas.translate(getPaddingLeft(), 0);
//                    if (links.draw(canvas)) {
//                        invalidate();
//                    }
//                    canvas.restore();
//                    super.onDraw(canvas);
//                }
//            };
//            ViewHelper.setPadding(textView, 16, 8, 16, 8);
//            textView.setLinkTextColor(0xff79c4fc);
//            textView.setTextColor(0xffffffff);
//            textView.setHighlightColor(0x33ffffff);
//            textView.setGravity(Gravity.CENTER_VERTICAL | LayoutHelper.getAbsoluteGravityStart());
//            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
//            return textView;
//        }
//
//        public void preparePlayer(Uri uri,boolean playWhenReady){
//            Log.d(TAG,"PREPARE PLAYER FOR URI" + uri);
//            currentPlayingVideoFile = uri;
//            releasePlayer(false);
//            boolean newPlayerCreated = false;
//            if(videoPlayer == null){
//                videoPlayer = new VideoPlayer();
//                newPlayerCreated = true;
//                if (videoTextureView != null) {
//                    videoPlayer.setTextureView(videoTextureView);
//                }
//                if (firstFrameView != null) {
//                    firstFrameView.clear();
//                }
//                videoPlayer.setDelegate(new VideoPlayer.VideoPlayerDelegate() {
//                    @Override
//                    public void onStateChanged(boolean playWhenReady, int playbackState) {
//                        updatePlayerState(playWhenReady,playbackState);
//                    }
//
//                    @Override
//                    public void onError(VideoPlayer player, Exception e) {
//                        Log.d(TAG,"onError(VideoPlayer player, Exception e)" + e.getMessage());
//                    }
//
//                    @Override
//                    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
//                        if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
//                            int temp = width;
//                            width = height;
//                            height = temp;
//                        }
//                        if(aspectRatioFrameLayout != null){
//                            aspectRatioFrameLayout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height, unappliedRotationDegrees);
//                        }
//                    }
//
//                    @Override
//                    public void onRenderedFirstFrame() {
//                        if (firstFrameView != null && (videoPlayer == null || !videoPlayer.isLooping())) {
//                            AndroidUtilities.runOnUIThread(() -> firstFrameView.updateAlpha(), 64);
//                        }
//                    }
//
//                    @Override
//                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//                        if (firstFrameView != null) {
//                            firstFrameView.checkFromPlayer(videoPlayer);
//                        }
//                    }
//
//                    @Override
//                    public boolean onSurfaceDestroyed(SurfaceTexture surfaceTexture) {
//                        return false;
//                    }
//                });
//            }
//            if (newPlayerCreated) {
//                seekToProgressPending = seekToProgressPending2;
//                videoPlayerSeekbar.setProgress(0);
//                videoPlayerSeekbar.setBufferedProgress(0);
//
//                if (currentMessageObject != null) {
//                    final int duration = currentMessageObject.getDuration();
//                    final String name = currentMessageObject.getFileName();
//                    if (!TextUtils.isEmpty(name)) {
//                        if (duration >= 10 * 60) {
//                            if (currentMessageObject.forceSeekTo < 0) {
//                                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("media_saved_pos", Activity.MODE_PRIVATE);
//                                float pos = preferences.getFloat(name, -1);
//                                if (pos > 0 && pos < 0.999f) {
//                                    currentMessageObject.forceSeekTo = pos;
//                                    videoPlayerSeekbar.setProgress(pos);
//                                }
//                            }
//                            shouldSavePositionForCurrentVideo = name;
//                        } else if (duration >= 10) {
//                            SavedVideoPosition videoPosition = null;
//                            for (int i = savedVideoPositions.size() - 1; i >= 0; i--) {
//                                final SavedVideoPosition item = savedVideoPositions.valueAt(i);
//                                if (item.timestamp < SystemClock.elapsedRealtime() - 5 * 1000) {
//                                    savedVideoPositions.removeAt(i);
//                                } else if (videoPosition == null && savedVideoPositions.keyAt(i).equals(name)) {
//                                    videoPosition = item;
//                                }
//                            }
//                            if (currentMessageObject.forceSeekTo < 0 && videoPosition != null) {
//                                float pos = videoPosition.position;
//                                if (pos > 0 && pos < 0.999f) {
//                                    currentMessageObject.forceSeekTo = pos;
//                                    videoPlayerSeekbar.setProgress(pos);
//                                }
//                            }
//                            shouldSavePositionForCurrentVideoShortTerm = name;
//                        }
//                    }
//                }
//
//                videoPlayer.preparePlayer(uri, "other");
//                videoPlayer.setPlayWhenReady(playWhenReady);
//            }
//
//        }
//
//
//        public void  updatePlayerState(boolean playWhenReady, int playbackState){
//
//        }
//
//
//        public void releasePlayer(boolean onClose){
//
//        }
//
//        public void startPlaying(){
//
//        }
//
//        public void setMessageObject(MessageObject messageObject,int pos) {
//            this.currentMessageObject = messageObject;
//            currnetPosation = pos;
//        }
//
//
//        private class FirstFrameView extends ImageView {
//
//            public FirstFrameView(Context context) {
//                super(context);
//                setAlpha(0f);
//            }
//
//            public void clear() {
//                hasFrame = false;
//                gotError = false;
//                if (gettingFrame) {
//                    gettingFrameIndex++;
//                    gettingFrame = false;
//                }
//                setImageResource(android.R.color.transparent);
//            }
//
//            private int gettingFrameIndex = 0;
//            private boolean gettingFrame = false;
//            private boolean hasFrame = false;
//            private boolean gotError = false;
//            private VideoPlayer currentVideoPlayer;
//
//
//            public void checkFromPlayer(VideoPlayer videoPlayer) {
//                if (currentVideoPlayer != videoPlayer) {
//                    gotError = false;
//                    clear();
//                }
//
//                if (videoPlayer != null) {
//                    long timeToEnd = videoPlayer.getDuration() - videoPlayer.getCurrentPosition();
//                    if (!hasFrame && !gotError && !gettingFrame && timeToEnd < 1000 * 5 + fadeDuration) { // 5 seconds to get the first frame
//                        final Uri uri = videoPlayer.getCurrentUri();
//                        final int index = ++gettingFrameIndex;
//                        Utilities.globalQueue.postRunnable(() -> {
//                            try {
//                                final AnimatedFileDrawable drawable = new AnimatedFileDrawable(new File(uri.getPath()), true, 0, null, null, null, 0, UserConfig.selectedAccount, false, AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
//                                final Bitmap bitmap = drawable.getFrameAtTime(0);
//                                drawable.recycle();
//                                AndroidUtilities.runOnUIThread(() -> {
//                                    if (index == gettingFrameIndex) {
//                                        setImageBitmap(bitmap);
//                                        hasFrame = true;
//                                        gettingFrame = false;
//                                    }
//                                });
//                            } catch (Throwable e) {
//                                FileLog.e(e);
//                                AndroidUtilities.runOnUIThread(() -> {
//                                    gotError = true;
//                                });
//                            }
//                        });
//                        gettingFrame = true;
//                    }
//                }
//
//                currentVideoPlayer = videoPlayer;
//            }
//
//            public boolean containsFrame() {
//                return hasFrame;
//            }
//
//            public final static float fadeDuration = 250;
//            private final TimeInterpolator fadeInterpolator = CubicBezierInterpolator.EASE_IN;
//
//            private ValueAnimator fadeAnimator;
//            private void updateAlpha() {
//                if (videoPlayer == null || videoPlayer.getDuration() == C.TIME_UNSET) {
//                    if (fadeAnimator != null) {
//                        fadeAnimator.cancel();
//                        fadeAnimator = null;
//                    }
//                    setAlpha(0f);
//                    return;
//                }
//                long toDuration = Math.max(0, videoPlayer.getDuration() - videoPlayer.getCurrentPosition());
//                float alpha = 1f - Math.max(Math.min(toDuration / fadeDuration, 1), 0);
//                if (alpha <= 0) {
//                    if (fadeAnimator != null) {
//                        fadeAnimator.cancel();
//                        fadeAnimator = null;
//                    }
//                    setAlpha(0f);
//                } else if (videoPlayer.isPlaying()) {
//                    if (fadeAnimator == null) {
//                        fadeAnimator = ValueAnimator.ofFloat(alpha, 1f);
//                        fadeAnimator.addUpdateListener(a -> {
//                            setAlpha((float) a.getAnimatedValue());
//                        });
//                        fadeAnimator.setDuration(toDuration);
//                        fadeAnimator.setInterpolator(fadeInterpolator);
//                        fadeAnimator.start();
//                        setAlpha(alpha);
//                    }
//                } else {
//                    if (fadeAnimator != null) {
//                        fadeAnimator.cancel();
//                        fadeAnimator = null;
//                    }
//                    setAlpha(alpha);
//                }
//            }
//        }
//
//        private class CaptionTextViewSwitcher extends TextViewSwitcher {
//
//            private boolean inScrollView = false;
//            private float alpha = 1.0f;
//
//            public CaptionTextViewSwitcher(Context context) {
//                super(context);
//            }
//
//            @Override
//            public void setVisibility(int visibility) {
//                setVisibility(visibility, true);
//            }
//
//            public void setVisibility(int visibility, boolean withScrollView) {
//                super.setVisibility(visibility);
//                if (inScrollView && withScrollView) {
//                    captionScrollView.setVisibility(visibility);
//                }
//            }
//
//            @Override
//            public void setAlpha(float alpha) {
//                this.alpha = alpha;
//                if (inScrollView) {
//                    captionScrollView.setAlpha(alpha);
//                } else {
//                    super.setAlpha(alpha);
//                }
//            }
//
//            @Override
//            public float getAlpha() {
//                if (inScrollView) {
//                    return alpha;
//                } else {
//                    return super.getAlpha();
//                }
//            }
//
//            @Override
//            public void setTranslationY(float translationY) {
//                super.setTranslationY(translationY);
//                if (inScrollView) {
//                    captionScrollView.invalidate(); // invalidate background drawing
//                }
//            }
//
//            @Override
//            protected void onAttachedToWindow() {
//                super.onAttachedToWindow();
//                if (captionContainer != null && getParent() == captionContainer) {
//                    inScrollView = true;
//                    captionScrollView.setVisibility(getVisibility());
//                    captionScrollView.setAlpha(alpha);
//                    super.setAlpha(1.0f);
//                }
//            }
//
//            @Override
//            protected void onDetachedFromWindow() {
//                super.onDetachedFromWindow();
//                if (inScrollView) {
//                    inScrollView = false;
//                    captionScrollView.setVisibility(View.GONE);
//                    super.setAlpha(alpha);
//                }
//            }
//        }
//        private class CaptionScrollView extends NestedScrollView {
//
//            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//
//            private final SpringAnimation springAnimation;
//
//            private boolean nestedScrollStarted;
//            private float overScrollY;
//            private float velocitySign;
//            private float velocityY;
//
//            private Method abortAnimatedScrollMethod;
//            private OverScroller scroller;
//
//            private boolean isLandscape;
//            private int textHash;
//            private int prevHeight;
//
//            private float backgroundAlpha = 1f;
//            private boolean dontChangeTopMargin;
//            private int pendingTopMargin = -1;
//
//            public CaptionScrollView(@NonNull Context context) {
//                super(context);
//                setClipChildren(false);
//                setOverScrollMode(View.OVER_SCROLL_NEVER);
//
//                paint.setColor(Color.BLACK);
//                setFadingEdgeLength(AndroidUtilities.dp(12));
//                setVerticalFadingEdgeEnabled(true);
//                setWillNotDraw(false);
//
//                springAnimation = new SpringAnimation(captionTextViewSwitcher, DynamicAnimation.TRANSLATION_Y, 0);
//                springAnimation.getSpring().setStiffness(100f);
//                springAnimation.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS);
//                springAnimation.addUpdateListener((animation, value, velocity) -> {
//                    overScrollY = value;
//                    velocityY = velocity;
//                });
//                springAnimation.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
//
//                try {
//                    abortAnimatedScrollMethod = NestedScrollView.class.getDeclaredMethod("abortAnimatedScroll");
//                    abortAnimatedScrollMethod.setAccessible(true);
//                } catch (Exception e) {
//                    abortAnimatedScrollMethod = null;
//                    FileLog.e(e);
//                }
//
//                try {
//                    final Field scrollerField = NestedScrollView.class.getDeclaredField("mScroller");
//                    scrollerField.setAccessible(true);
//                    scroller = (OverScroller) scrollerField.get(this);
//                } catch (Exception e) {
//                    scroller = null;
//                    FileLog.e(e);
//                }
//            }
//
//            @Override
//            public boolean onTouchEvent(MotionEvent ev) {
//                if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getY() < captionContainer.getTop() - getScrollY() + captionTextViewSwitcher.getTranslationY()) {
//                    return false;
//                }
//                return super.onTouchEvent(ev);
//            }
//
//            @Override
//            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                updateTopMargin(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
//                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            }
//
//            public void applyPendingTopMargin() {
//                dontChangeTopMargin = false;
//                if (pendingTopMargin >= 0) {
//                    ((MarginLayoutParams) captionContainer.getLayoutParams()).topMargin = pendingTopMargin;
//                    pendingTopMargin = -1;
//                    requestLayout();
//                }
//            }
//
//            public int getPendingMarginTopDiff() {
//                if (pendingTopMargin >= 0) {
//                    return pendingTopMargin - ((MarginLayoutParams) captionContainer.getLayoutParams()).topMargin;
//                } else {
//                    return 0;
//                }
//            }
//
//            public void updateTopMargin() {
//                updateTopMargin(getWidth(), getHeight());
//            }
//
//            private void updateTopMargin(int width, int height) {
//                final int marginTop = calculateNewContainerMarginTop(width, height);
//                if (marginTop >= 0) {
//                    if (dontChangeTopMargin) {
//                        pendingTopMargin = marginTop;
//                    } else {
//                        ((MarginLayoutParams) captionContainer.getLayoutParams()).topMargin = marginTop;
//                        pendingTopMargin = -1;
//                    }
//                }
//            }
//
//            public int calculateNewContainerMarginTop(int width, int height) {
//                if (width == 0 || height == 0) {
//                    return -1;
//                }
//
//                final TextView textView = captionTextViewSwitcher.getCurrentView();
//                final CharSequence text = textView.getText();
//
//                final int textHash = text.hashCode();
//                final boolean isLandscape = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y;
//
//                if (this.textHash == textHash && this.isLandscape == isLandscape && this.prevHeight == height) {
//                    return -1;
//                }
//
//                this.textHash = textHash;
//                this.isLandscape = isLandscape;
//                this.prevHeight = height;
//
//                textView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
//
//                final Layout layout = textView.getLayout();
//                final int lineCount = layout.getLineCount();
//
//                if (isLandscape && lineCount <= 2 || !isLandscape && lineCount <= 5) {
//                    return height - textView.getMeasuredHeight();
//                }
//
//                int i = Math.min(isLandscape ? 2 : 5, lineCount);
//
//                cycle:
//                while (i > 1) {
//                    for (int j = layout.getLineStart(i - 1); j < layout.getLineEnd(i - 1); j++) {
//                        if (Character.isLetterOrDigit(text.charAt(j))) {
//                            break cycle;
//                        }
//                    }
//                    i--;
//                }
//
//                final int lineHeight = textView.getPaint().getFontMetricsInt(null);
//                return height - lineHeight * i - AndroidUtilities.dp(8);
//            }
//
//            public void reset() {
//                scrollTo(0, 0);
//            }
//
//            public void stopScrolling() {
//                if (abortAnimatedScrollMethod != null) {
//                    try {
//                        abortAnimatedScrollMethod.invoke(this);
//                    } catch (Exception e) {
//                        FileLog.e(e);
//                    }
//                }
//            }
//
//            @Override
//            public void fling(int velocityY) {
//                super.fling(velocityY);
//                this.velocitySign = Math.signum(velocityY);
//                this.velocityY = 0f;
//            }
//
//            @Override
//            public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
//                consumed[1] = 0;
//
//                if (nestedScrollStarted && (overScrollY > 0 && dy > 0 || overScrollY < 0 && dy < 0)) {
//                    final float delta = overScrollY - dy;
//
//                    if (overScrollY > 0) {
//                        if (delta < 0) {
//                            overScrollY = 0;
//                            consumed[1] += dy + delta;
//                        } else {
//                            overScrollY = delta;
//                            consumed[1] += dy;
//                        }
//                    } else {
//                        if (delta > 0) {
//                            overScrollY = 0;
//                            consumed[1] += dy + delta;
//                        } else {
//                            overScrollY = delta;
//                            consumed[1] += dy;
//                        }
//                    }
//
//                    captionTextViewSwitcher.setTranslationY(overScrollY);
//                    return true;
//                }
//
//                return false;
//            }
//
//            @Override
//            public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type, @NonNull int[] consumed) {
//                if (dyUnconsumed != 0) {
//                    final int topMargin = (isStatusBarVisible() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
//                    final int dy = Math.round(dyUnconsumed * (1f - Math.abs((-overScrollY / (captionContainer.getTop() - topMargin)))));
//
//                    if (dy != 0) {
//                        if (!nestedScrollStarted) {
//                            if (!springAnimation.isRunning()) {
//                                int consumedY;
//                                float velocity = scroller != null ? scroller.getCurrVelocity() : Float.NaN;
//                                if (!Float.isNaN(velocity)) {
//                                    final float clampedVelocity = Math.min(AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? 3000 : 5000, velocity);
//                                    consumedY = (int) (dy * clampedVelocity / velocity);
//                                    velocity = clampedVelocity * -velocitySign;
//                                } else {
//                                    consumedY = dy;
//                                    velocity = 0;
//                                }
//                                if (consumedY != 0) {
//                                    overScrollY -= consumedY;
//                                    captionTextViewSwitcher.setTranslationY(overScrollY);
//                                }
//                                startSpringAnimationIfNotRunning(velocity);
//                            }
//                        } else {
//                            overScrollY -= dy;
//                            captionTextViewSwitcher.setTranslationY(overScrollY);
//                        }
//                    }
//                }
//            }
//
//            private void startSpringAnimationIfNotRunning(float velocityY) {
//                if (!springAnimation.isRunning()) {
//                    springAnimation.setStartVelocity(velocityY);
//                    springAnimation.start();
//                }
//            }
//
//            @Override
//            public boolean startNestedScroll(int axes, int type) {
//                if (type == ViewCompat.TYPE_TOUCH) {
//                    springAnimation.cancel();
//                    nestedScrollStarted = true;
//                    overScrollY = captionTextViewSwitcher.getTranslationY();
//                }
//                return true;
//            }
//
//            @Override
//            public void computeScroll() {
//                super.computeScroll();
//                if (!nestedScrollStarted && overScrollY != 0 && scroller != null && scroller.isFinished()) {
//                    startSpringAnimationIfNotRunning(0);
//                }
//            }
//
//            @Override
//            public void stopNestedScroll(int type) {
//                if (nestedScrollStarted && type == ViewCompat.TYPE_TOUCH) {
//                    nestedScrollStarted = false;
//                    if (overScrollY != 0 && scroller != null && scroller.isFinished()) {
//                        startSpringAnimationIfNotRunning(velocityY);
//                    }
//                }
//            }
//
//            @Override
//            protected float getTopFadingEdgeStrength() {
//                return 1f;
//            }
//
//            @Override
//            protected float getBottomFadingEdgeStrength() {
//                return 1f;
//            }
//
//            @Override
//            public void draw(Canvas canvas) {
//                final int width = getWidth();
//                final int height = getHeight();
//                final int scrollY = getScrollY();
//
//                final int saveCount = canvas.save();
//                canvas.clipRect(0, scrollY, width, height + scrollY);
//
//                paint.setAlpha((int) (backgroundAlpha * 127));
//                canvas.drawRect(0, captionContainer.getTop() + captionTextViewSwitcher.getTranslationY(), width, height + scrollY, paint);
//
//                super.draw(canvas);
//                canvas.restoreToCount(saveCount);
//            }
//
//            @Override
//            public void invalidate() {
//                super.invalidate();
//                final int scrollY = getScrollY();
//                final float translationY = captionTextViewSwitcher.getTranslationY();
//
//                boolean buttonVisible = scrollY == 0 && translationY == 0;
//                boolean enalrgeIconVisible = scrollY == 0 && translationY == 0;
//
//                if (!buttonVisible) {
//                    final int progressBottom = photoProgressViews[0].getY() + photoProgressViews[0].size;
//                    final int topMargin = (isStatusBarVisible() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
//                    final int captionTop = captionContainer.getTop() + (int) translationY - scrollY + topMargin - AndroidUtilities.dp(12);
//                    final int enlargeIconTop = (int) fullscreenButton[0].getY();
//                    enalrgeIconVisible = captionTop > enlargeIconTop + AndroidUtilities.dp(32);
//                    buttonVisible = captionTop > progressBottom;
//                }
//                if (allowShowFullscreenButton) {
//                    if (fullscreenButton[0].getTag() != null && ((Integer) fullscreenButton[0].getTag()) == 3 && enalrgeIconVisible) {
//                        fullscreenButton[0].setTag(2);
//                        fullscreenButton[0].animate().alpha(1).setDuration(150).setListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                fullscreenButton[0].setTag(null);
//                            }
//                        }).start();
//                    } else if (fullscreenButton[0].getTag() == null && !enalrgeIconVisible) {
//                        fullscreenButton[0].setTag(3);
//                        fullscreenButton[0].animate().alpha(0).setListener(null).setDuration(150).start();
//                    }
//
//                }
//                photoProgressViews[0].setIndexedAlpha(2, buttonVisible ? 1f : 0f, true);
//            }
//        }
//
//        private class VideoPlayerControlFrameLayout extends FrameLayout {
//
//            private float progress = 1f;
//            private boolean seekBarTransitionEnabled;
//            private boolean translationYAnimationEnabled = true;
//            private boolean ignoreLayout;
//            private int parentWidth;
//            private int parentHeight;
//
//            public VideoPlayerControlFrameLayout(@NonNull Context context) {
//                super(context);
//                setWillNotDraw(false);
//            }
//
//            @Override
//            public boolean onTouchEvent(MotionEvent event) {
//                if (progress < 1f) {
//                    return false;
//                }
//                if (videoPlayerSeekbar.onTouch(event.getAction(), event.getX() - AndroidUtilities.dp(2), event.getY())) {
//                    getParent().requestDisallowInterceptTouchEvent(true);
//                    videoPlayerSeekbarView.invalidate();
//                    return true;
//                }
//                return true;
//            }
//
//            @Override
//            public void requestLayout() {
//                if (ignoreLayout) {
//                    return;
//                }
//                super.requestLayout();
//            }
//
//            @Override
//            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                int extraWidth;
//                ignoreLayout = true;
//                LayoutParams layoutParams = (LayoutParams) videoPlayerTime.getLayoutParams();
//                if (parentWidth > parentHeight) {
//                    if (exitFullscreenButton.getVisibility() != VISIBLE) {
//                        exitFullscreenButton.setVisibility(VISIBLE);
//                    }
//                    extraWidth = AndroidUtilities.dp(48);
//                    layoutParams.rightMargin = AndroidUtilities.dp(47);
//                } else {
//                    if (exitFullscreenButton.getVisibility() != INVISIBLE) {
//                        exitFullscreenButton.setVisibility(INVISIBLE);
//                    }
//                    extraWidth = 0;
//                    layoutParams.rightMargin = AndroidUtilities.dp(12);
//                }
//                ignoreLayout = false;
//                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//                long duration;
//                if (videoPlayer != null) {
//                    duration = videoPlayer.getDuration();
//                    if (duration == C.TIME_UNSET) {
//                        duration = 0;
//                    }
//                } else {
//                    duration = 0;
//                }
//                duration /= 1000;
//                int size = (int) Math.ceil(videoPlayerTime.getPaint().measureText(String.format(Locale.ROOT, "%02d:%02d / %02d:%02d", duration / 60, duration % 60, duration / 60, duration % 60)));
//                videoPlayerSeekbar.setSize(getMeasuredWidth() - AndroidUtilities.dp(2 + 14) - size - extraWidth, getMeasuredHeight());
//            }
//
//            @Override
//            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//                super.onLayout(changed, left, top, right, bottom);
//                float progress = 0;
//                if (videoPlayer != null) {
//                    progress = videoPlayer.getCurrentPosition() / (float) videoPlayer.getDuration();
//                }
//                if (playerWasReady) {
//                    videoPlayerSeekbar.setProgress(progress);
//                }
//            }
//
//            public float getProgress() {
//                return progress;
//            }
//
//            public void setProgress(float progress) {
//                if (this.progress != progress) {
//                    this.progress = progress;
//                    onProgressChanged(progress);
//                }
//            }
//
//            private void onProgressChanged(float progress) {
//                videoPlayerTime.setAlpha(progress);
//                exitFullscreenButton.setAlpha(progress);
//                if (seekBarTransitionEnabled) {
//                    videoPlayerTime.setPivotX(videoPlayerTime.getWidth());
//                    videoPlayerTime.setPivotY(videoPlayerTime.getHeight());
//                    videoPlayerTime.setScaleX(1f - 0.1f * (1f - progress));
//                    videoPlayerTime.setScaleY(1f - 0.1f * (1f - progress));
//                    videoPlayerSeekbar.setTransitionProgress(1f - progress);
//                } else {
//                    if (translationYAnimationEnabled) {
//                        setTranslationY(AndroidUtilities.dpf2(24) * (1f - progress));
//                    }
//                    videoPlayerSeekbarView.setAlpha(progress);
//                }
//            }
//
//            public boolean isSeekBarTransitionEnabled() {
//                return seekBarTransitionEnabled;
//            }
//
//            public void setSeekBarTransitionEnabled(boolean seekBarTransitionEnabled) {
//                if (this.seekBarTransitionEnabled != seekBarTransitionEnabled) {
//                    this.seekBarTransitionEnabled = seekBarTransitionEnabled;
//                    if (seekBarTransitionEnabled) {
//                        setTranslationY(0);
//                        videoPlayerSeekbarView.setAlpha(1f);
//                    } else {
//                        videoPlayerTime.setScaleX(1f);
//                        videoPlayerTime.setScaleY(1f);
//                        videoPlayerSeekbar.setTransitionProgress(0f);
//                    }
//                    onProgressChanged(progress);
//                }
//            }
//
//            public void setTranslationYAnimationEnabled(boolean translationYAnimationEnabled) {
//                if (this.translationYAnimationEnabled != translationYAnimationEnabled) {
//                    this.translationYAnimationEnabled = translationYAnimationEnabled;
//                    if (!translationYAnimationEnabled) {
//                        setTranslationY(0);
//                    }
//                    onProgressChanged(progress);
//                }
//            }
//        }
//
//    }
//
//
//
//}
