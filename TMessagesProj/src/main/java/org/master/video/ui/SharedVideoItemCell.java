package org.master.video.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.master.video.DialogVideoFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.MentionCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;

@SuppressLint("ViewConstructor")
public class SharedVideoItemCell extends FrameLayout {


    private SharedPhotoVideoCell3  sharedPhotoVideoCell;
    private int imageHeight;
    private TextView captionTextView;
    private BackupImageView chatImageView;
    private TextView chatNameTextView;
    private MentionCell mentionCell;


    public SharedVideoItemCell(@NonNull Context context, SharedPhotoVideoCell3.SharedResources sharedResources, int currentAccount) {
        super(context);


        imageHeight = DialogVideoFragment.getItemSize(2);

        sharedPhotoVideoCell = new SharedPhotoVideoCell3(context,sharedResources,currentAccount);
        sharedPhotoVideoCell.imageReceiver.setRoundRadius(AndroidUtilities.dp(8));
        addView(sharedPhotoVideoCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, imageHeight,Gravity.TOP,8,8,8,8));

        captionTextView = new TextView(context);
        captionTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        captionTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        captionTextView.setMaxLines(2);
        captionTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        captionTextView.setGravity(Gravity.LEFT);
        captionTextView.setEllipsize(TextUtils.TruncateAt.END);
        addView(captionTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 8, imageHeight + 12, 8, 0));

        mentionCell = new MentionCell(context,null);
        addView(mentionCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 8, imageHeight + 12 +12 + 32, 8, 0));

    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY));
//    }

    public ImageReceiver imageReceiver(){
        return sharedPhotoVideoCell.imageReceiver;
    }

    public void setImageAlpha(float alpha, boolean invalidate) {
        sharedPhotoVideoCell.setImageAlpha(alpha,invalidate);
    }



    public void setMessageObject(MessageObject messageObject, int parentColumnsCount){
        if(messageObject != null){
            if(messageObject.messageOwner.from_id != null){
                if(messageObject.isFromUser()){
                    TLRPC.User user = MessagesController.getInstance(UserConfig.selectedAccount).getUser(messageObject.messageOwner.peer_id.user_id);
                    mentionCell.setUser(user);
                }else {
                    TLRPC.Chat user = MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.user_id);
                    mentionCell.setChat(user);
                }
            }
            if(!TextUtils.isEmpty(messageObject.caption)){
                CharSequence caption = Emoji.replaceEmoji(messageObject.caption.toString().replace("\n", " ").replaceAll(" +", " ").trim(), Theme.chat_msgTextPaint.getFontMetricsInt(), AndroidUtilities.dp(20), false);
                captionTextView.setText(caption);
            }
        }
        sharedPhotoVideoCell.setMessageObject(messageObject,parentColumnsCount);

    }

    public int getMessageId() {
        return sharedPhotoVideoCell.getMessageId();
    }

    public MessageObject getMessageObject() {
        return sharedPhotoVideoCell.getMessageObject();
    }
    public void setImageScale(float scale, boolean invalidate) {
        sharedPhotoVideoCell.setImageScale(scale,invalidate);
    }

    public void setCrossfadeView(SharedVideoItemCell cell, float crossfadeProgress, int crossfadeToColumnsCount) {
        if(cell == null){
            sharedPhotoVideoCell.setCrossfadeView(null,crossfadeProgress,crossfadeToColumnsCount);
        }else{
            sharedPhotoVideoCell.setCrossfadeView(cell.sharedPhotoVideoCell,crossfadeProgress,crossfadeToColumnsCount);

        }
    }

    public void drawCrossafadeImage(Canvas canvas) {
       sharedPhotoVideoCell.drawCrossafadeImage(canvas);
    }

    public View getCrossfadeView() {
        return sharedPhotoVideoCell.getCrossfadeView();
    }


    public void setChecked(final boolean checked, boolean animated) {
      sharedPhotoVideoCell.setChecked(checked,animated);
    }

    public void startHighlight() {
        sharedPhotoVideoCell.startHighlight();
    }

    public void setHighlightProgress(float p) {
        sharedPhotoVideoCell.setHighlightProgress(p);

    }

    public void moveImageToFront() {
        sharedPhotoVideoCell.moveImageToFront();
    }

    public void setGradientView(FlickerLoadingView globalGradientView) {
        sharedPhotoVideoCell.setGradientView(globalGradientView);
    }

//    @Override
//    protected void dispatchDraw(Canvas canvas) {
//        super.dispatchDraw(canvas);
//        if (sharedPhotoVideoCell != null) {
//            canvas.save();
//            canvas.translate(sharedPhotoVideoCell.getX(), sharedPhotoVideoCell.getY());
//            sharedPhotoVideoCell.draw(canvas);
//            canvas.restore();
//        }
//
//    }
//
//    @Override
//    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
//        if(child ==sharedPhotoVideoCell){
//            return true;
//        }
//        return super.drawChild(canvas, child, drawingTime);
//    }
}
