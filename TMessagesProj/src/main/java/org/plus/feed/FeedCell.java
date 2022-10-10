package org.plus.feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.AvatarPreviewer;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.MediaActionDrawable;
import org.telegram.ui.Components.RadialProgress2;

@SuppressLint("ViewConstructor")
public  class FeedCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private static final float ANIM_DURATION = 150f;

    private final int radialProgressSize = AndroidUtilities.dp(64f);
    private final int[] coords = new int[2];
    private final Rect rect = new Rect();

    private final Interpolator interpolator = new AccelerateDecelerateInterpolator();
    private final ColorDrawable backgroundDrawable = new ColorDrawable(0x71000000);
    private final ImageReceiver imageReceiver = new ImageReceiver();
    private final RadialProgress2 radialProgress;
    private final Drawable arrowDrawable;

    private float progress;
    private boolean showing;
    private long lastUpdateTime;
//    private WindowInsets insets;
    private BottomSheet visibleSheet;
    private ValueAnimator moveAnimator;
    private float moveProgress; // [-1; 0]
    private float downY = -1;

    private String videoFileName;
    private InfoLoadTask<?, ?> infoLoadTask;
    private ValueAnimator progressHideAnimator;
    private ValueAnimator progressShowAnimator;
    private boolean showProgress;
    private boolean recycled;

    private TLRPC.User user;
    private TLRPC.UserFull userFull;
    private Data data;

    public TLRPC.User getUser() {
        return user;
    }

    private int nameLeft;
    private int nameTop;
    private StaticLayout nameLayout;


    private int bioLeft;
    private int bioTop;
    private StaticLayout bioLayout;


    public ImageReceiver getImageReceiver() {
        return imageReceiver;
    }

    public FeedCell(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);
        setFitsSystemWindows(true);
        imageReceiver.setAspectFit(true);
        imageReceiver.setInvalidateAll(true);
        imageReceiver.setRoundRadius(AndroidUtilities.dp(6));
        imageReceiver.setParentView(this);
        radialProgress = new RadialProgress2(this);
        radialProgress.setOverrideAlpha(0.0f);
        radialProgress.setIcon(MediaActionDrawable.ICON_EMPTY, false, false);
        radialProgress.setColors(0x42000000, 0x42000000, Color.WHITE, Color.WHITE);
        arrowDrawable = ContextCompat.getDrawable(context, R.drawable.preview_arrow);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widht = MeasureSpec.makeMeasureSpec(widthMeasureSpec,MeasureSpec.UNSPECIFIED);
        int height = MeasureSpec.makeMeasureSpec(heightMeasureSpec,MeasureSpec.AT_MOST);

        setMeasuredDimension(widht,widht);


    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageReceiver.onAttachedToWindow();
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (!showProgress || TextUtils.isEmpty(videoFileName)) {
            return;
        }
        if (id == NotificationCenter.fileLoaded) {
            final String fileName = (String) args[0];
            if (TextUtils.equals(fileName, videoFileName)) {
                radialProgress.setProgress(1f, true);
            }
        } else if (id == NotificationCenter.fileLoadProgressChanged) {
            String fileName = (String) args[0];
            if (TextUtils.equals(fileName, videoFileName)) {
                if (radialProgress != null) {
                    Long loadedSize = (Long) args[1];
                    Long totalSize = (Long) args[2];
                    float progress = Math.min(1f, loadedSize / (float) totalSize);
                    radialProgress.setProgress(progress, true);
                }
            }
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        invalidateSize();
    }

    public void invalidateSize() {
        final int width = getWidth();
        final int height = getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        backgroundDrawable.setBounds(0, 0, width, height);

        final int padding = AndroidUtilities.dp(8);

        int lPadding = padding, rPadding = padding, vPadding = padding;

//        if (Build.VERSION.SDK_INT >= 21) {
//            lPadding += insets.getStableInsetLeft();
//            rPadding += insets.getStableInsetRight();
//            vPadding += Math.max(insets.getStableInsetTop(), insets.getStableInsetBottom());
//        }

        final int arrowWidth = arrowDrawable.getIntrinsicWidth();
        final int arrowHeight = arrowDrawable.getIntrinsicHeight();
        final int arrowPadding = AndroidUtilities.dp(24);

        final int w = width - (lPadding + rPadding);
        final int h = height - vPadding * 2;

        final int size = Math.min(w, h);
        final int vOffset = arrowPadding + arrowHeight / 2;
        final int x = (w - size) / 2 + lPadding;
        final int y = (h - size) / 2 + vPadding + (w > h ? vOffset : 0);
        imageReceiver.setImageCoords(x, 0, size, size - (w > h ? vOffset : 0));

        final int cx = (int) imageReceiver.getCenterX();
        final int cy = (int) imageReceiver.getCenterY();
        radialProgress.setProgressRect(cx - radialProgressSize / 2, cy - radialProgressSize / 2, cx + radialProgressSize / 2, cy + radialProgressSize / 2);

//        final int arrowX = x + size / 2;
//        final int arrowY = y - arrowPadding;
//        arrowDrawable.setBounds(arrowX - arrowWidth / 2, arrowY - arrowHeight / 2, arrowX + arrowWidth / 2, arrowY + arrowHeight / 2);


//        if(user != null){
//            int nameWidth = width  - AndroidUtilities.dp(16);
//
//            String nameString = ContactsController.formatName(user.first_name,user.last_name);
//            nameTop =  (size - (w > h ? vOffset : 0) + AndroidUtilities.dp(8) );
//            nameLeft = x + AndroidUtilities.dp(8);
//            Theme.dialogs_searchNamePaint.setColor(Theme.getColor(Theme.key_dialogTextBlack));
//            CharSequence nameFinal = TextUtils.ellipsize(nameString, PlusTheme.productPricePaint,  AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
//            nameLayout = new StaticLayout(nameFinal, Theme.dialogs_searchNamePaint, nameWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//
//            HuluLog.d("user was not null so creating name layout ");
//            HuluLog.d("top = " + nameTop);            HuluLog.d("left = " + nameLeft);
//
//        }


    }

    @Override
    protected void onDraw(Canvas canvas) {

        long newTime = AnimationUtils.currentAnimationTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;

        if (showing && progress < 1f) {
            progress += dt / ANIM_DURATION;
            if (progress < 1f) {
                postInvalidateOnAnimation();
            } else {
                progress = 1f;
            }
        } else if (!showing && progress > 0f) {
            progress -= dt / ANIM_DURATION;
            if (progress > 0f) {
                postInvalidateOnAnimation();
            } else {
                progress = 0f;
                onHide();
            }
        }
        if (nameLayout != null) {
            canvas.save();
            canvas.translate(nameLeft , nameTop);
            nameLayout.draw(canvas);
            canvas.restore();
        }

        final float interpolatedProgress = interpolator.getInterpolation(progress);
        backgroundDrawable.setAlpha((int) (180 * interpolatedProgress));
        backgroundDrawable.draw(canvas);
        if (interpolatedProgress < 1.0f) {
            canvas.scale(AndroidUtilities.lerp(0.95f, 1.0f, interpolatedProgress), AndroidUtilities.lerp(0.95f, 1.0f, interpolatedProgress), imageReceiver.getCenterX(), imageReceiver.getCenterY());
        }


        imageReceiver.setAlpha(interpolatedProgress);
        imageReceiver.draw(canvas);


        if (showProgress) {
            final Drawable drawable = imageReceiver.getDrawable();
            if (drawable instanceof AnimatedFileDrawable && ((AnimatedFileDrawable) drawable).getDurationMs() > 0) {
                if (progressShowAnimator != null) {
                    progressShowAnimator.cancel();
                    if (radialProgress.getProgress() < 1f) {
                        radialProgress.setProgress(1f, true);
                    }
                    progressHideAnimator = ValueAnimator.ofFloat((Float) progressShowAnimator.getAnimatedValue(), 0);
                    progressHideAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            showProgress = false;
                            invalidate();
                        }
                    });
                    progressHideAnimator.addUpdateListener(a -> invalidate());
                    progressHideAnimator.setDuration(250);
                    progressHideAnimator.start();
                } else {
                    showProgress = false;
                }
            } else if (progressShowAnimator == null) {
                progressShowAnimator = ValueAnimator.ofFloat(0f, 1f);
                progressShowAnimator.addUpdateListener(a -> invalidate());
                progressShowAnimator.setStartDelay(250);
                progressShowAnimator.setDuration(250);
                progressShowAnimator.start();
            }
            if (progressHideAnimator != null) {
                radialProgress.setOverrideAlpha((Float) progressHideAnimator.getAnimatedValue());
                radialProgress.draw(canvas);
            } else if (progressShowAnimator != null) {
                radialProgress.setOverrideAlpha((Float) progressShowAnimator.getAnimatedValue());
                radialProgress.draw(canvas);
            }
        }




    }


    private TLRPC.FileLocation lastAvatar;
    public void update(int mask) {
        if (user== null) {
            return;
        }

        TLRPC.FileLocation photo = null;
        if (user.photo != null) {
            photo = user.photo.photo_big;
        }

        if (mask != 0) {
            boolean continueUpdate = false;
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0) {
                if (lastAvatar != null && photo == null || lastAvatar == null && photo != null || lastAvatar != null && (lastAvatar.volume_id != photo.volume_id || lastAvatar.local_id != photo.local_id)) {
                    continueUpdate = true;
                }
            }

            if (!continueUpdate) {
                return;
            }
        }
        lastAvatar = photo;
        data = Data.of(user,1);
        imageReceiver.setImage(data.videoLocation, data.videoFilter, data.imageLocation, data.imageFilter, data.thumbImageLocation, data.thumbImageFilter, null, 0, null, null, 1);
    }




    public void setData(Data data) {
        backgroundDrawable.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        showProgress = data.videoLocation != null;
        videoFileName = data.videoFileName;
        this.user =data.user;
        this.userFull =data.userFull;
        recycleInfoLoadTask();
        if (data.infoLoadTask != null) {
            infoLoadTask = data.infoLoadTask;
            infoLoadTask.load(result -> {
                if (!recycled) {
                    if (result instanceof TLRPC.UserFull) {
                        userFull = (TLRPC.UserFull) result;
//
//                        if(user != null && userFull.id == user.id){
//                            setData(Data.of((TLRPC.UserFull) result, data.menuItems));
//                        }
                    }
                }
            });
        }
        this.data = data;
        imageReceiver.setCurrentAccount(UserConfig.selectedAccount);
        imageReceiver.setImage(data.videoLocation, data.videoFilter, data.imageLocation, data.imageFilter, data.thumbImageLocation, data.thumbImageFilter, null, 0, null, data.parentObject, 1);
        setShowing(true);

    }

    private void setShowing(boolean showing) {
        if (this.showing != showing) {
            this.showing = showing;
            lastUpdateTime = AnimationUtils.currentAnimationTimeMillis();
            invalidate();
        }
    }

    public void recycle() {
        recycled = true;
        if (moveAnimator != null) {
            moveAnimator.cancel();
        }
        if (visibleSheet != null) {
            visibleSheet.cancel();
        }
        recycleInfoLoadTask();
    }

    private void recycleInfoLoadTask() {
        if (infoLoadTask != null) {
            infoLoadTask.cancel();
            infoLoadTask = null;
        }
    }

    protected void onHide() {

    }
    private static abstract class InfoLoadTask<A, B> {

        private final NotificationCenter.NotificationCenterDelegate observer = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (loading && id == notificationId) {
                    onReceiveNotification(args);
                }
            }
        };

        private final NotificationCenter notificationCenter;

        protected final A argument;
        protected final int classGuid;
        private final int notificationId;

        private Consumer<B> onResult;
        private boolean loading;

        public InfoLoadTask(A argument, int classGuid, int notificationId) {
            this.argument = argument;
            this.classGuid = classGuid;
            this.notificationId = notificationId;
            notificationCenter = NotificationCenter.getInstance(UserConfig.selectedAccount);
        }

        public final void load(Consumer<B> onResult) {
            if (!loading) {
                loading = true;
                this.onResult = onResult;
                notificationCenter.addObserver(observer, notificationId);
                load();
            }
        }

        public final void cancel() {
            if (loading) {
                loading = false;
                notificationCenter.removeObserver(observer, notificationId);
            }
        }

        protected final void onResult(B result) {
            if (loading) {
                cancel();
                onResult.accept(result);
            }
        }

        protected abstract void load();

        protected abstract void onReceiveNotification(Object... args);
    }

    private static class UserInfoLoadTask extends InfoLoadTask<TLRPC.User, TLRPC.UserFull> {

        public UserInfoLoadTask(TLRPC.User argument, int classGuid) {
            super(argument, classGuid, NotificationCenter.userInfoDidLoad);
        }

        @Override
        protected void load() {
            MessagesController.getInstance(UserConfig.selectedAccount).loadUserInfo(argument, false, classGuid);
        }

        @Override
        protected void onReceiveNotification(Object... args) {
            Long uid = (Long) args[0];
            if (uid == argument.id) {
                onResult((TLRPC.UserFull) args[1]);
            }
        }
    }



    public static class Data {

        public static Data of(TLRPC.User user, int classGuid) {
            final ImageLocation imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
            final ImageLocation thumbImageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL);
            final String thumbFilter = thumbImageLocation != null && thumbImageLocation.photoSize instanceof TLRPC.TL_photoStrippedSize ? "b" : null;
            return new Data(imageLocation, thumbImageLocation, null, null, thumbFilter, null, null, user, new UserInfoLoadTask(user, classGuid),user,null);
        }

        public static Data of(TLRPC.UserFull userFull) {
            final ImageLocation imageLocation = ImageLocation.getForUserOrChat(userFull.user, ImageLocation.TYPE_BIG);
            final ImageLocation thumbImageLocation = ImageLocation.getForUserOrChat(userFull.user, ImageLocation.TYPE_SMALL);
            final String thumbFilter = thumbImageLocation != null && thumbImageLocation.photoSize instanceof TLRPC.TL_photoStrippedSize ? "b" : null;
            final ImageLocation videoLocation;
            final String videoFileName;
            if (userFull.profile_photo != null && !userFull.profile_photo.video_sizes.isEmpty()) {
                final TLRPC.VideoSize videoSize = userFull.profile_photo.video_sizes.get(0);
                videoLocation = ImageLocation.getForPhoto(videoSize, userFull.profile_photo);
                videoFileName = FileLoader.getAttachFileName(videoSize);
            } else {
                videoLocation = null;
                videoFileName = null;
            }
            final String videoFilter = videoLocation != null && videoLocation.imageType == FileLoader.IMAGE_TYPE_ANIMATION ? ImageLoader.AUTOPLAY_FILTER : null;
            return new Data(imageLocation, thumbImageLocation, videoLocation, null, thumbFilter, videoFilter, videoFileName, userFull.user, null,userFull.user,userFull);
        }




        public final ImageLocation imageLocation;
        public final ImageLocation thumbImageLocation;
        public final ImageLocation videoLocation;
        public final String imageFilter;
        public final String thumbImageFilter;
        public final String videoFilter;
        public final String videoFileName;
        public final Object parentObject;
        public final InfoLoadTask<?, ?> infoLoadTask;
        private TLRPC.User user;
        private TLRPC.UserFull userFull;

        private Data(ImageLocation imageLocation, ImageLocation thumbImageLocation, ImageLocation videoLocation, String imageFilter, String thumbImageFilter, String videoFilter, String videoFileName, Object parentObject,InfoLoadTask<?, ?> infoLoadTask, TLRPC.User user, TLRPC.UserFull userFull) {
            this.imageLocation = imageLocation;
            this.thumbImageLocation = thumbImageLocation;
            this.videoLocation = videoLocation;
            this.imageFilter = imageFilter;
            this.thumbImageFilter = thumbImageFilter;
            this.videoFilter = videoFilter;
            this.videoFileName = videoFileName;
            this.parentObject = parentObject;
            this.infoLoadTask = infoLoadTask;
            this.user = user;
            this.userFull = userFull;
        }
    }


}

