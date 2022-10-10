package org.master.video.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CheckBoxBase;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.MediaActionDrawable;
import org.telegram.ui.Components.RadialProgress;
import org.telegram.ui.Components.RadialProgress2;
import org.telegram.ui.PhotoViewer;

import java.io.File;

public class SharedPhotoVideoCell3 extends View  implements DownloadController.FileDownloadProgressListener{

    public ImageReceiver imageReceiver = new ImageReceiver();
    int currentAccount;
    MessageObject currentMessageObject;
    int currentParentColumnsCount;
    FlickerLoadingView globalGradientView;
    SharedPhotoVideoCell3 crossfadeView;
    float imageAlpha = 1f;
    float imageScale = 1f;
    boolean showVideoLayout;
    StaticLayout videoInfoLayot;
    String videoText;
    CheckBoxBase checkBoxBase;
    SharedResources sharedResources;
    private boolean attached;
    float crossfadeProgress;
    float crossfadeToColumnsCount;
    float highlightProgress;

    static long lastUpdateDownloadSettingsTime;
    static boolean lastAutoDownload;


    //plus
    private int TAG;
    private int buttonState;
    private int miniButtonState;
    private RadialProgress2 radialProgress;
    private boolean buttonPressed;
    private boolean miniButtonPressed;
    private int hasMiniProgress;
    private int buttonX;
    private int buttonY;




    public SharedPhotoVideoCell3(Context context, SharedResources sharedResources, int currentAccount) {
        super(context);
        this.sharedResources = sharedResources;
        this.currentAccount = currentAccount;

        setChecked(false, false);
        imageReceiver.setParentView(this);

        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();

        radialProgress = new RadialProgress2(this, null);
        radialProgress.setColors(Theme.key_chat_inLoader, Theme.key_chat_inLoaderSelected, Theme.key_chat_inMediaIcon, Theme.key_chat_inMediaIconSelected);
        radialProgress.setDrawBackground(false);
        radialProgress.setCircleRadius(AndroidUtilities.dp(15));
//        radialProgress.setProgressColor(getThemedColor(Theme.key_chat_mediaProgress));
        radialProgress.setProgressColor(getThemedColor(Theme.key_chat_outFileProgress));
//        radialProgress.setColors(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMessageObject == null) {
            return super.onTouchEvent(event);
        }
        boolean result = checkAudioMotionEvent(event);
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            miniButtonPressed = false;
            buttonPressed = false;
            result = false;
            radialProgress.setPressed(buttonPressed, false);
            radialProgress.setPressed(miniButtonPressed, true);
        }

        return result;
    }
    private boolean checkForButtonPress;

    private boolean checkAudioMotionEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        boolean result = false;
        int side = AndroidUtilities.dp(36);
        boolean area = false;
        if (miniButtonState >= 0) {
            int offset = AndroidUtilities.dp(27);
            area = x >= buttonX + offset && x <= buttonX + offset + side && y >= buttonY + offset && y <= buttonY + offset + side;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (area) {
                miniButtonPressed = true;
                radialProgress.setPressed(miniButtonPressed, true);
                invalidate();
                result = true;
            } else if (checkForButtonPress && radialProgress.getProgressRect().contains(x, y)) {
                buttonPressed = true;
                radialProgress.setPressed(buttonPressed, false);
                invalidate();
                result = true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (miniButtonPressed) {
                miniButtonPressed = false;
                playSoundEffect(SoundEffectConstants.CLICK);
                didPressedMiniButton(true);
                invalidate();
            } else if (buttonPressed) {
                buttonPressed = false;
                playSoundEffect(SoundEffectConstants.CLICK);
                didPressedButton();
                invalidate();
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            miniButtonPressed = false;
            buttonPressed = false;
            invalidate();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!area && miniButtonPressed) {
                miniButtonPressed = false;
                invalidate();
            }
        }
        radialProgress.setPressed(miniButtonPressed, true);
        return result;
    }
    private void didPressedMiniButton(boolean animated) {
        if (miniButtonState == 0) {
            miniButtonState = 1;
            radialProgress.setProgress(0, false);
            FileLoader.getInstance(currentAccount).loadFile(currentMessageObject.getDocument(), currentMessageObject, 1, 0);
            radialProgress.setMiniIcon(getMiniIconForCurrentState(), false, true);
            invalidate();
        } else if (miniButtonState == 1) {
            if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                MediaController.getInstance().cleanupPlayer(true, true);
            }
            miniButtonState = 0;
            FileLoader.getInstance(currentAccount).cancelLoadFile(currentMessageObject.getDocument());
            radialProgress.setMiniIcon(getMiniIconForCurrentState(), false, true);
            invalidate();
        }
    }

    public void didPressedButton() {
        if (buttonState == 0) {
            if (miniButtonState == 0) {
                currentMessageObject.putInDownloadsStore = true;
                FileLoader.getInstance(currentAccount).loadFile(currentMessageObject.getDocument(), currentMessageObject, 1, 0);
            }
        } else if (buttonState == 1) {
            boolean result = MediaController.getInstance().pauseMessage(currentMessageObject);
            if (result) {
                buttonState = 0;
                radialProgress.setIcon(getIconForCurrentState(), false, true);
                invalidate();
            }
        } else if (buttonState == 2) {
            radialProgress.setProgress(0, false);
            currentMessageObject.putInDownloadsStore = true;
            FileLoader.getInstance(currentAccount).loadFile(currentMessageObject.getDocument(), currentMessageObject, 1, 0);
            buttonState = 4;
            radialProgress.setIcon(getIconForCurrentState(), false, true);
            invalidate();
        } else if (buttonState == 4) {
            FileLoader.getInstance(currentAccount).cancelLoadFile(currentMessageObject.getDocument());
            buttonState = 2;
            radialProgress.setIcon(getIconForCurrentState(), false, true);
            invalidate();
        }
    }

    public void setMessageObject(MessageObject messageObject, int parentColumnsCount) {
        int oldParentColumsCount = currentParentColumnsCount;
        currentParentColumnsCount = parentColumnsCount;
        if (currentMessageObject == null && messageObject == null) {
            return;
        }
        if (currentMessageObject != null && messageObject != null && currentMessageObject.getId() == messageObject.getId() && oldParentColumsCount == parentColumnsCount) {
            return;
        }
        currentMessageObject = messageObject;
        if (messageObject == null) {
            imageReceiver.onDetachedFromWindow();
            videoText = null;
            videoInfoLayot = null;
            showVideoLayout = false;
            return;
        } else {
            if (attached) {
                imageReceiver.onAttachedToWindow();
            }
        }
        String restrictionReason = MessagesController.getRestrictionReason(messageObject.messageOwner.restriction_reason);
        String imageFilter;
        int stride;
        int width = (int) (AndroidUtilities.displaySize.x / parentColumnsCount / AndroidUtilities.density);
        imageFilter = sharedResources.getFilterString(width);
        boolean showImageStub = false;
        if (parentColumnsCount <= 2) {
            stride = AndroidUtilities.getPhotoSize();
        } else if (parentColumnsCount == 3) {
            stride = 320;
        } else if (parentColumnsCount == 5) {
            stride = 320;
        } else {
            stride = 320;
        }
        videoText = null;
        videoInfoLayot = null;
        showVideoLayout = false;
        if (!TextUtils.isEmpty(restrictionReason)) {
            showImageStub = true;
        } else if (messageObject.isVideo()) {
            showVideoLayout = true;
            if (parentColumnsCount != 9) {
                videoText = AndroidUtilities.formatShortDuration(messageObject.getDuration());
            }
            if (messageObject.mediaThumb != null) {
                if (messageObject.strippedThumb != null) {
                    imageReceiver.setImage(messageObject.mediaThumb, imageFilter, messageObject.strippedThumb, null, messageObject, 0);
                } else {
                    imageReceiver.setImage(messageObject.mediaThumb, imageFilter, messageObject.mediaSmallThumb, imageFilter + "_b", null, 0, null, messageObject, 0);
                }
            } else {
                TLRPC.Document document = messageObject.getDocument();
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 50);
                TLRPC.PhotoSize qualityThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, stride);
                if (thumb == qualityThumb) {
                    qualityThumb = null;
                }
                if (thumb != null) {
                    if (messageObject.strippedThumb != null) {
                        imageReceiver.setImage(ImageLocation.getForDocument(qualityThumb, document), imageFilter, messageObject.strippedThumb, null, messageObject, 0);
                    } else {
                        imageReceiver.setImage(ImageLocation.getForDocument(qualityThumb, document), imageFilter, ImageLocation.getForDocument(thumb, document), imageFilter + "_b", null, 0, null, messageObject, 0);
                    }
                } else {
                    showImageStub = true;
                }
            }

        } else if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && messageObject.messageOwner.media.photo != null && !messageObject.photoThumbs.isEmpty()) {
            if (messageObject.mediaExists || canAutoDownload(messageObject)) {
                if (messageObject.mediaThumb != null) {
                    if (messageObject.strippedThumb != null) {
                        imageReceiver.setImage(messageObject.mediaThumb, imageFilter, messageObject.strippedThumb, null, messageObject, 0);
                    } else {
                        imageReceiver.setImage(messageObject.mediaThumb, imageFilter, messageObject.mediaSmallThumb, imageFilter + "_b", null, 0, null, messageObject, 0);
                    }
                } else {
                    TLRPC.PhotoSize currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 50);
                    TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, stride, false, currentPhotoObjectThumb, false);
                    if (currentPhotoObject == currentPhotoObjectThumb) {
                        currentPhotoObjectThumb = null;
                    }
                    if (messageObject.strippedThumb != null) {
                        imageReceiver.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), imageFilter, null, null, messageObject.strippedThumb, currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                    } else {
                        imageReceiver.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), imageFilter, ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), imageFilter + "_b", currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                    }
                }
            } else {
                if (messageObject.strippedThumb != null) {
                    imageReceiver.setImage(null, null, null, null, messageObject.strippedThumb, 0, null, messageObject, 0);
                } else {
                    TLRPC.PhotoSize currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 50);
                    imageReceiver.setImage(null, null, ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", null, 0, null, messageObject, 0);
                }
            }
        } else {
            showImageStub = true;
        }

        if (showImageStub) {
            imageReceiver.setImageBitmap(ContextCompat.getDrawable(getContext(), R.drawable.photo_placeholder_in));
        }
        updateButtonState(false, false);
        invalidate();
    }


    private boolean canAutoDownload(MessageObject messageObject) {
        if (System.currentTimeMillis() - lastUpdateDownloadSettingsTime > 5000) {
            lastUpdateDownloadSettingsTime = System.currentTimeMillis();
            lastAutoDownload = DownloadController.getInstance(currentAccount).canDownloadMedia(messageObject);
        }
        return lastAutoDownload;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding;
        if (crossfadeProgress != 0 && (crossfadeToColumnsCount == 9 || currentParentColumnsCount == 9)) {
            if (crossfadeToColumnsCount == 9) {
                padding = AndroidUtilities.dp(0.5f) * crossfadeProgress + AndroidUtilities.dpf2(1) * (1f - crossfadeProgress);
            } else {
                padding = AndroidUtilities.dp(1f) * crossfadeProgress + AndroidUtilities.dpf2(0.5f) * (1f - crossfadeProgress);
            }
        } else {
            padding = currentParentColumnsCount == 9 ? AndroidUtilities.dpf2(0.5f) : AndroidUtilities.dpf2(1);
        }

        float imageWidth = (getMeasuredWidth() - padding * 2) * imageScale;
        float imageHeight = (getMeasuredHeight() - padding * 2) * imageScale;

        if (crossfadeProgress > 0.5f && crossfadeToColumnsCount != 9 && currentParentColumnsCount != 9) {
            imageWidth -= 2;
            imageHeight -= 2;
        }

        if (currentMessageObject == null || !imageReceiver.hasBitmapImage() || imageReceiver.getCurrentAlpha() != 1.0f || imageAlpha != 1f) {
            if (SharedPhotoVideoCell3.this.getParent() != null) {
                globalGradientView.setParentSize(((View) SharedPhotoVideoCell3.this.getParent()).getMeasuredWidth(), SharedPhotoVideoCell3.this.getMeasuredHeight(), -getX());
                globalGradientView.updateColors();
                globalGradientView.updateGradient();
                float localPadding = padding;
                if (crossfadeProgress > 0.5f && crossfadeToColumnsCount != 9 && currentParentColumnsCount != 9) {
                    localPadding += 1;
                }
                canvas.drawRect(localPadding, localPadding, localPadding + imageWidth, localPadding + imageHeight, globalGradientView.getPaint());
            }
            invalidate();
        }
        if (currentMessageObject == null) {
            return;
        }

        if (imageAlpha != 1f) {
            canvas.saveLayerAlpha(0,0, padding * 2 + imageWidth, padding * 2 + imageHeight, (int) (255 * imageAlpha), Canvas.ALL_SAVE_FLAG);
        } else {
            canvas.save();
        }

        if ((checkBoxBase != null && checkBoxBase.isChecked()) || PhotoViewer.isShowingImage(currentMessageObject)) {
            canvas.drawRect(padding, padding, imageWidth, imageHeight, sharedResources.backgroundPaint);
        }
        if (currentMessageObject != null) {
            if (checkBoxProgress > 0) {
                float offset = AndroidUtilities.dp(10) * checkBoxProgress;
                imageReceiver.setImageCoords(padding + offset, padding + offset, imageWidth - offset * 2, imageHeight - offset * 2);
            } else {
                float localPadding = padding;
                if (crossfadeProgress > 0.5f && crossfadeToColumnsCount != 9 && currentParentColumnsCount != 9) {
                    localPadding += 1;
                }
                imageReceiver.setImageCoords(localPadding, localPadding, imageWidth, imageHeight);
            }
            if (!PhotoViewer.isShowingImage(currentMessageObject)) {
                imageReceiver.draw(canvas);
                if (highlightProgress > 0) {
                    sharedResources.highlightPaint.setColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (0.5f * highlightProgress * 255)));
                    canvas.drawRect(imageReceiver.getDrawRegion(), sharedResources.highlightPaint);
                }
            }
        }
        if (showVideoLayout) {
            canvas.save();
            canvas.clipRect(padding, padding, padding + imageWidth, padding + imageHeight);
            if (currentParentColumnsCount != 9 && videoInfoLayot == null && videoText != null) {
                int textWidth = (int) Math.ceil(sharedResources.textPaint.measureText(videoText));
                videoInfoLayot = new StaticLayout(videoText, sharedResources.textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            int width;
            if (videoInfoLayot == null) {
                width = AndroidUtilities.dp(17);
            } else {
                width = AndroidUtilities.dp(14) + videoInfoLayot.getWidth() + AndroidUtilities.dp(4);
            }
            canvas.translate(AndroidUtilities.dp(5), AndroidUtilities.dp(1) + imageHeight - AndroidUtilities.dp(17) - AndroidUtilities.dp(4));
            AndroidUtilities.rectTmp.set(0, 0, width, AndroidUtilities.dp(17));
            canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(4), AndroidUtilities.dp(4), Theme.chat_timeBackgroundPaint);
            canvas.save();
            canvas.translate(videoInfoLayot == null ? AndroidUtilities.dp(5) : AndroidUtilities.dp(4), (AndroidUtilities.dp(17) - sharedResources.playDrawable.getIntrinsicHeight()) / 2f);
            sharedResources.playDrawable.setAlpha((int) (255 * imageAlpha));
            sharedResources.playDrawable.draw(canvas);
            canvas.restore();
            if (videoInfoLayot != null) {
                canvas.translate(AndroidUtilities.dp(14), (AndroidUtilities.dp(17) - videoInfoLayot.getHeight()) / 2f);
                videoInfoLayot.draw(canvas);
            }
            canvas.restore();

            //plus
            radialProgress.setProgressRect((int)(imageWidth - AndroidUtilities.dp(24)) ,AndroidUtilities.dp(8), (int)(imageWidth - AndroidUtilities.dp(8)), AndroidUtilities.dp(24));
//            radialProgress.draw(canvas);

//            radialProgress.setProgressColor(getThemedColor(buttonPressed ? Theme.key_chat_inAudioSelectedProgress : Theme.key_chat_inAudioProgress));
            radialProgress.draw(canvas);


//            int maxPhotoWidth = AndroidUtilities.dp(52);
//            int x = (int) (imageWidth - AndroidUtilities.dp(8) - maxPhotoWidth);
//            radialProgress.setProgressRect(buttonX = x + AndroidUtilities.dp(4), buttonY = AndroidUtilities.dp(6), x + AndroidUtilities.dp(48), AndroidUtilities.dp(50));

        }

        if (checkBoxBase != null && checkBoxBase.getProgress() != 0) {
            canvas.save();
            canvas.translate(imageWidth + AndroidUtilities.dp(2) - AndroidUtilities.dp(25), 0);
            checkBoxBase.draw(canvas);
            canvas.restore();
        }
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        if (checkBoxBase != null) {
            checkBoxBase.onAttachedToWindow();
        }
        if (currentMessageObject != null) {
            imageReceiver.onAttachedToWindow();
        }
        radialProgress.onAttachedToWindow();
        updateButtonState(false, false);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attached = false;
        if (checkBoxBase != null) {
            checkBoxBase.onDetachedFromWindow();
        }
        if (currentMessageObject != null) {
            imageReceiver.onDetachedFromWindow();
        }
        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
        radialProgress.onDetachedFromWindow();
    }

    public void setGradientView(FlickerLoadingView globalGradientView) {
        this.globalGradientView = globalGradientView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY));
    }

    public int getMessageId() {
        return currentMessageObject != null ? currentMessageObject.getId() : 0;
    }

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }

    public void setImageAlpha(float alpha, boolean invalidate) {
        if (this.imageAlpha != alpha) {
            this.imageAlpha = alpha;
            if (invalidate) {
                invalidate();
            }
        }
    }

    public void setImageScale(float scale, boolean invalidate) {
        if (this.imageScale != scale) {
            this.imageScale = scale;
            if (invalidate) {
                invalidate();
            }
        }
    }

    public void setCrossfadeView(SharedPhotoVideoCell3 cell, float crossfadeProgress, int crossfadeToColumnsCount) {
        crossfadeView = cell;
        this.crossfadeProgress = crossfadeProgress;
        this.crossfadeToColumnsCount = crossfadeToColumnsCount;
    }

    public void drawCrossafadeImage(Canvas canvas) {
        if (crossfadeView != null) {
            canvas.save();
            canvas.translate(getX(), getY());
            float scale = ((getMeasuredWidth() - AndroidUtilities.dp(2)) * imageScale) / (float) (crossfadeView.getMeasuredWidth() - AndroidUtilities.dp(2));
            crossfadeView.setImageScale(scale, false);
            crossfadeView.draw(canvas);
            canvas.restore();
        }
    }

    public View getCrossfadeView() {
        return crossfadeView;
    }

    ValueAnimator animator;
    float checkBoxProgress;

    public void setChecked(final boolean checked, boolean animated) {
        boolean currentIsChecked = checkBoxBase != null && checkBoxBase.isChecked();
        if (currentIsChecked == checked) {
            return;
        }
        if (checkBoxBase == null) {
            checkBoxBase = new CheckBoxBase(this,21, null);
            checkBoxBase.setColor(null, Theme.key_sharedMedia_photoPlaceholder, Theme.key_checkboxCheck);
            checkBoxBase.setDrawUnchecked(false);
            checkBoxBase.setBackgroundType(1);
            checkBoxBase.setBounds(0, 0, AndroidUtilities.dp(24), AndroidUtilities.dp(24));
            if (attached) {
                checkBoxBase.onAttachedToWindow();
            }
        }
        checkBoxBase.setChecked(checked, animated);
        if (animator != null) {
            ValueAnimator animatorFinal = animator;
            animator = null;
            animatorFinal.cancel();
        }
        if (animated) {
            animator = ValueAnimator.ofFloat(checkBoxProgress, checked ? 1f : 0);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    checkBoxProgress = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                }
            });
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animator != null && animator.equals(animation)) {
                        checkBoxProgress = checked ? 1f : 0;
                        animator = null;
                    }
                }
            });
            animator.start();
        } else {
            checkBoxProgress = checked ? 1f : 0;
        }
        invalidate();
    }

    public void startHighlight() {

    }

    public void setHighlightProgress(float p) {
        if (highlightProgress != p) {
            highlightProgress = p;
            invalidate();
        }
    }

    public void moveImageToFront() {
        imageReceiver.moveImageToFront();
    }

    private int getThemedColor(String key) {
        return Theme.getColor(key) ;
    }

    private int getIconForCurrentState() {
        if (buttonState == 1) {
            return MediaActionDrawable.ICON_PAUSE;
        } else if (buttonState == 2) {
            return MediaActionDrawable.ICON_DOWNLOAD;
        } else if (buttonState == 4) {
            return MediaActionDrawable.ICON_CANCEL;
        }
        return MediaActionDrawable.ICON_PLAY;
    }

    private int getMiniIconForCurrentState() {
        if (miniButtonState < 0) {
            return MediaActionDrawable.ICON_NONE;
        }
        if (miniButtonState == 0) {
            return MediaActionDrawable.ICON_DOWNLOAD;
        } else {
            return MediaActionDrawable.ICON_CANCEL;
        }
    }

    public void updateButtonState(boolean ifSame, boolean animated) {
        if(currentMessageObject == null){
            return;
        }
        String fileName = currentMessageObject.getFileName();
        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        boolean fileExists = currentMessageObject.attachPathExists || currentMessageObject.mediaExists;
        if (SharedConfig.streamMedia && currentMessageObject.isMusic() && (int) currentMessageObject.getDialogId() != 0) {
            hasMiniProgress = fileExists ? 1 : 2;
            fileExists = true;
        } else {
            hasMiniProgress = 0;
            miniButtonState = -1;
        }
        if (hasMiniProgress != 0) {
            radialProgress.setMiniProgressBackgroundColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outLoader : Theme.key_chat_inLoader));
            boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
            if (!playing || MediaController.getInstance().isMessagePaused()) {
                buttonState = 0;
            } else {
                buttonState = 1;
            }
            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
            if (hasMiniProgress == 1) {
                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                miniButtonState = -1;
                radialProgress.setMiniIcon(getMiniIconForCurrentState(), ifSame, animated);
            } else {
                DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                    miniButtonState = 0;
                    radialProgress.setMiniIcon(getMiniIconForCurrentState(), ifSame, animated);
                } else {
                    miniButtonState = 1;
                    radialProgress.setMiniIcon(getMiniIconForCurrentState(), ifSame, animated);
                    Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                    if (progress != null) {
                        radialProgress.setProgress(progress, animated);
                    } else {
                        radialProgress.setProgress(0, animated);
                    }
                }
            }
        } else if (fileExists) {
            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
            boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
            if (!playing || playing && MediaController.getInstance().isMessagePaused()) {
                buttonState = 0;
            } else {
                buttonState = 1;
            }
            radialProgress.setProgress(1, animated);
            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
            invalidate();
        } else {
            DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
            boolean isLoading = FileLoader.getInstance(currentAccount).isLoadingFile(fileName);
            if (!isLoading) {
                buttonState = 2;
                radialProgress.setProgress(0, animated);
            } else {
                buttonState = 4;
                Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                if (progress != null) {
                    radialProgress.setProgress(progress, animated);
                } else {
                    radialProgress.setProgress(0, animated);
                }
            }
            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
            invalidate();
        }
    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {
        updateButtonState(true, canceled);
    }

    @Override
    public void onSuccessDownload(String fileName) {
        radialProgress.setProgress(1, true);
        updateButtonState(false, true);
    }

    @Override
    public void onProgressDownload(String fileName, long downloadSize, long totalSize) {
        float progress = Math.min(1f, downloadSize / (float) totalSize);
        radialProgress.setProgress(progress, true);
        if (hasMiniProgress != 0) {
            if (miniButtonState != 1) {
                updateButtonState(false, true);
            }
        } else {
            if (buttonState != 4) {
                updateButtonState(false, true);
            }
        }
    }

    @Override
    public void onProgressUpload(String fileName, long uploadedSize, long totalSize, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }

//    public void updateButtonState(boolean animated) {
//        if(currentMessageObject == null){
//            return;
//        }
//        String fileName = currentMessageObject.getFileName();
//        boolean fileExists = currentMessageObject.attachPathExists || currentMessageObject.mediaExists;
//        if (TextUtils.isEmpty(fileName)) {
//            radialProgress.setBackground(null, false, false);
//            return;
//        }
//        if (fileExists) {
//            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
//            buttonState = -1;
//            radialProgress.setBackground(null, false, animated);
//            invalidate();
//        } else {
//            DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, this);
//            buttonState = 1;
//            Float progress = ImageLoader.getInstance().getFileProgress(fileName);
//            radialProgress.setBackground(Theme.chat_photoStatesDrawables[5][0], true, animated);
//            radialProgress.setProgress(progress != null ? progress : 0, animated);
//            invalidate();
//        }
//
//    }



    public static class SharedResources {
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private Paint backgroundPaint = new Paint();
        Drawable playDrawable;
        Paint highlightPaint = new Paint();
        SparseArray<String> imageFilters = new SparseArray<>();

        public SharedResources(Context context, Theme.ResourcesProvider resourcesProvider) {
            textPaint.setTextSize(AndroidUtilities.dp(12));
            textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            playDrawable = ContextCompat.getDrawable(context, R.drawable.play_mini_video);
            playDrawable.setBounds(0, 0, playDrawable.getIntrinsicWidth(), playDrawable.getIntrinsicHeight());
            backgroundPaint.setColor(Theme.getColor(Theme.key_sharedMedia_photoPlaceholder, resourcesProvider));
        }

        public String getFilterString(int width) {
            String str = imageFilters.get(width);
            if (str == null) {
                str =  width + "_" + width + "_isc";
                imageFilters.put(width, str);
            }
            return str;
        }
    }
}
