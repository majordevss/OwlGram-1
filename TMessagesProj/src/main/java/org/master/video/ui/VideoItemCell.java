//package org.master.video.ui;
//
//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
//import android.animation.AnimatorSet;
//import android.animation.ObjectAnimator;
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.drawable.Drawable;
//import android.os.Build;
//import android.text.TextPaint;
//import android.text.TextUtils;
//import android.util.SparseArray;
//import android.view.Gravity;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.FrameLayout;
//
//import androidx.annotation.NonNull;
//import androidx.core.content.ContextCompat;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.ApplicationLoader;
//import org.telegram.messenger.DownloadController;
//import org.telegram.messenger.FileLoader;
//import org.telegram.messenger.ImageLocation;
//import org.telegram.messenger.ImageReceiver;
//import org.telegram.messenger.MessageObject;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.R;
//import org.telegram.messenger.UserConfig;
//import org.telegram.tgnet.TLRPC;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.Components.BackupImageView;
//import org.telegram.ui.Components.CheckBox2;
//import org.telegram.ui.Components.FlickerLoadingView;
//import org.telegram.ui.Components.LayoutHelper;
//import org.telegram.ui.Components.RadialProgress;
//import org.telegram.ui.Components.RadialProgress2;
//import org.telegram.ui.PhotoViewer;
//
//public class VideoItemCell extends FrameLayout implements DownloadController.FileDownloadProgressListener {
//
//    private int currentAccount = UserConfig.selectedAccount;
//    private int TAG;
//
//    public ImageReceiver imageReceiver = new ImageReceiver();
//
//
//    float imageAlpha = 1f;
//    FlickerLoadingView globalGradientView;
//
//    SharedPhotoVideoCell3 crossfadeView;
//    float crossfadeProgress;
//    float crossfadeToColumnsCount;
//    float highlightProgress;
//    public void setCrossfadeView(SharedPhotoVideoCell3 cell, float crossfadeProgress, int crossfadeToColumnsCount) {
//        crossfadeView = cell;
//        this.crossfadeProgress = crossfadeProgress;
//        this.crossfadeToColumnsCount = crossfadeToColumnsCount;
//    }
//
//    private FrameLayout container;
//    private View selector;
//    private CheckBox2 checkBox;
//    private AnimatorSet animator;
//    private Paint backgroundPaint = new Paint();
//
//    private MessageObject currentMessageObject;
//
//    private RadialProgress radialProgress;
//    SharedResources sharedResources;
//
//    public VideoItemCell(@NonNull Context context, SharedResources sharedResources,int currentAccount) {
//        super(context);
//        imageReceiver.setParentView(this);
//        this.sharedResources = sharedResources;
//        this.currentAccount = currentAccount;
//
//
//        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();
//        setWillNotDraw(false);
//
//
//        radialProgress = new RadialProgress(this);
//
//
//        container = new FrameLayout(context);
//        addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//
//
//        selector = new View(context);
//        selector.setBackgroundDrawable(Theme.getSelectorDrawable(false));
//        addView(selector, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//
//        checkBox = new CheckBox2(context, 21);
//        checkBox.setVisibility(INVISIBLE);
//        checkBox.setColor(null, Theme.key_sharedMedia_photoPlaceholder, Theme.key_checkboxCheck);
//        checkBox.setDrawUnchecked(false);
//        checkBox.setDrawBackgroundAsArc(1);
//        addView(checkBox, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.TOP, 0, 1, 1, 0));
//
//    }
//
//    public int getMessageId() {
//        return currentMessageObject != null ? currentMessageObject.getId() : 0;
//    }
//
//    public MessageObject getMessageObject() {
//        return currentMessageObject;
//    }
//
//;
//
//    public void setImageAlpha(float alpha, boolean invalidate) {
//        if (this.imageAlpha != alpha) {
//            this.imageAlpha = alpha;
//            if (invalidate) {
//                invalidate();
//            }
//        }
//    }
//
//
//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        super.onLayout(changed, left, top, right, bottom);
//    }
//    public void setGradientView(FlickerLoadingView globalGradientView) {
//        this.globalGradientView = globalGradientView;
//    }
//    public void setHighlightProgress(float p) {
//        if (highlightProgress != p) {
//            highlightProgress = p;
//            invalidate();
//        }
//    }
//
//    float imageScale = 1f;
//    public void setImageScale(float scale, boolean invalidate) {
//        if (this.imageScale != scale) {
//            this.imageScale = scale;
//            if (invalidate) {
//                invalidate();
//            }
//        }
//    }
//    public void setMessageObject(MessageObject messageObject) {
//        currentMessageObject = messageObject;
//        imageReceiver.setVisible(!PhotoViewer.isShowingImage(messageObject), false);
//        if (messageObject.isVideo()) {
//            showVideoLayout = true;
//             String  videoText = AndroidUtilities.formatShortDuration(messageObject.getDuration());
//
//            if (messageObject.mediaThumb != null) {
//                if (messageObject.strippedThumb != null) {
//                    imageReceiver.setImage(messageObject.mediaThumb, imageFilter, messageObject.strippedThumb, null, messageObject, 0);
//                } else {
//                    imageReceiver.setImage(messageObject.mediaThumb, imageFilter, messageObject.mediaSmallThumb, imageFilter + "_b", null, 0, null, messageObject, 0);
//                }
//            } else {
//                TLRPC.Document document = messageObject.getDocument();
//                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 50);
//                TLRPC.PhotoSize qualityThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, stride);
//                if (thumb == qualityThumb) {
//                    qualityThumb = null;
//                }
//                if (thumb != null) {
//                    if (messageObject.strippedThumb != null) {
//                        imageReceiver.setImage(ImageLocation.getForDocument(qualityThumb, document), imageFilter, messageObject.strippedThumb, null, messageObject, 0);
//                    } else {
//                        imageReceiver.setImage(ImageLocation.getForDocument(qualityThumb, document), imageFilter, ImageLocation.getForDocument(thumb, document), imageFilter + "_b", null, 0, null, messageObject, 0);
//                    }
//                } else {
//                    showImageStub = true;
//                }
//            }
//        }
//    }
//
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (Build.VERSION.SDK_INT >= 21) {
//            selector.drawableHotspotChanged(event.getX(), event.getY());
//        }
//        return super.onTouchEvent(event);
//    }
//
//    public void setChecked(final boolean checked, boolean animated) {
//        if (checkBox.getVisibility() != VISIBLE) {
//            checkBox.setVisibility(VISIBLE);
//        }
//        checkBox.setChecked(checked, animated);
//        if (animator != null) {
//            animator.cancel();
//            animator = null;
//        }
//        if (animated) {
//            animator = new AnimatorSet();
//            animator.playTogether(
//                    ObjectAnimator.ofFloat(container, View.SCALE_X, checked ? 0.81f : 1.0f),
//                    ObjectAnimator.ofFloat(container, View.SCALE_Y, checked ? 0.81f : 1.0f));
//            animator.setDuration(200);
//            animator.addListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    if (animator != null && animator.equals(animation)) {
//                        animator = null;
//                    }
//                }
//
//                @Override
//                public void onAnimationCancel(Animator animation) {
//                    if (animator != null && animator.equals(animation)) {
//                        animator = null;
//                    }
//                }
//            });
//            animator.start();
//        } else {
//            container.setScaleX(checked ? 0.85f : 1.0f);
//            container.setScaleY(checked ? 0.85f : 1.0f);
//        }
//    }
//
//    @Override
//    public void clearAnimation() {
//        super.clearAnimation();
//        if (animator != null) {
//            animator.cancel();
//            animator = null;
//        }
//    }
//
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        if (checkBox.isChecked() || !imageView.getImageReceiver().hasBitmapImage() || imageView.getImageReceiver().getCurrentAlpha() != 1.0f || PhotoViewer.isShowingImage(currentMessageObject)) {
//            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
//        }
//    }
//
//
//    @Override
//    public void onFailedDownload(String fileName, boolean canceled) {
//
//    }
//
//    @Override
//    public void onSuccessDownload(String fileName) {
//
//    }
//
//    @Override
//    public void onProgressDownload(String fileName, long downloadSize, long totalSize) {
//
//    }
//
//    @Override
//    public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {
//
//    }
//
//    @Override
//    public int getObserverTag() {
//        return 0;
//    }
//
//
//
//
//    public static int getItemSize(int itemsCount) {
//        final int itemWidth;
//        if (AndroidUtilities.isTablet()) {
//            itemWidth = (AndroidUtilities.dp(490) - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
//        } else {
//            itemWidth = (AndroidUtilities.displaySize.x - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
//        }
//        return itemWidth;
//    }
//
//    public static class SharedResources {
//        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
//        private Paint backgroundPaint = new Paint();
//        Drawable playDrawable;
//        Paint highlightPaint = new Paint();
//        SparseArray<String> imageFilters = new SparseArray<>();
//
//        public SharedResources(Context context, Theme.ResourcesProvider resourcesProvider) {
//            textPaint.setTextSize(AndroidUtilities.dp(12));
//            textPaint.setColor(Color.WHITE);
//            textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//            playDrawable = ContextCompat.getDrawable(context, R.drawable.play_mini_video);
//            playDrawable.setBounds(0, 0, playDrawable.getIntrinsicWidth(), playDrawable.getIntrinsicHeight());
//            backgroundPaint.setColor(Theme.getColor(Theme.key_sharedMedia_photoPlaceholder, resourcesProvider));
//        }
//
//        public String getFilterString(int width) {
//            String str = imageFilters.get(width);
//            if (str == null) {
//                str =  width + "_" + width + "_isc";
//                imageFilters.put(width, str);
//            }
//            return str;
//        }
//    }
//
//
//}
//
