package org.master.feature.feed;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ManageChatUserCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

public class TwitterFeedCell extends FrameLayout {

    private TextView contentTextView;
    private ManageChatUserCell manageChatUserCell;
    private BackupImageView imageView;

    public TwitterFeedCell(@NonNull Context context) {
        super(context);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        manageChatUserCell = new ManageChatUserCell(context,6,6,false);
        linearLayout.addView(manageChatUserCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT,0,0,9,0));

        contentTextView = new TextView(context);
        contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        contentTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        contentTextView.setMaxLines(5);
        contentTextView.setEllipsize(TextUtils.TruncateAt.END);
        contentTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        contentTextView.setGravity(Gravity.LEFT);
        linearLayout.addView(contentTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 46 + 12, 10, 0, 0));

        imageView = new BackupImageView(context);
        imageView.setRoundRadius(AndroidUtilities.dp(6));
        linearLayout.addView(imageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,210, Gravity.TOP | Gravity.LEFT, 46 + 12, 10, 0, 0));

        addView(linearLayout,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,Gravity.TOP|Gravity.LEFT,0,0,16,0));
    }

    private int currentAccount = UserConfig.selectedAccount;
    public void setMessage(MessageObject messageObject){
        TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(-messageObject.messageOwner.dialog_id);
        if(chat != null){
            manageChatUserCell.setData(chat,chat.title,LocaleController.formatDateChat(messageObject.messageOwner.date),true);

        }
        contentTextView.setText(messageObject.messageText);

        if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && messageObject.messageOwner.media.photo != null && !messageObject.photoThumbs.isEmpty()) {
            TLRPC.PhotoSize currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 50);
            TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 320, false, currentPhotoObjectThumb, false);
            if (messageObject.mediaExists || DownloadController.getInstance(currentAccount).canDownloadMedia(messageObject)) {
                if (currentPhotoObject == currentPhotoObjectThumb) {
                    currentPhotoObjectThumb = null;
                }
                if (messageObject.strippedThumb != null) {
                    imageView.getImageReceiver().setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "100_100", null, null, messageObject.strippedThumb, currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                } else {
                    imageView.getImageReceiver().setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "100_100", ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                }
            } else {
                if (messageObject.strippedThumb != null) {
                    imageView.setImage(null, null, null, null, messageObject.strippedThumb, null, null, 0, messageObject);
                } else {
                    imageView.setImage(null, null, ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.photo_placeholder_in), null, null, 0, messageObject);
                }
            }
        } else if (messageObject.isVideo()) {
//            videoInfoContainer.setVisibility(VISIBLE);
//            videoTextView.setText(AndroidUtilities.formatShortDuration(messageObject.getDuration()));
            TLRPC.Document document = messageObject.getDocument();
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 50);
            TLRPC.PhotoSize qualityThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
            if (thumb == qualityThumb) {
                qualityThumb = null;
            }
            if (thumb != null) {
                if (messageObject.strippedThumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(qualityThumb, document), "100_100", null, messageObject.strippedThumb, messageObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(qualityThumb, document), "100_100", ImageLocation.getForDocument(thumb, document), "b", ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.photo_placeholder_in), null, null, 0, messageObject);
                }
            } else {
                imageView.setImageResource(R.drawable.photo_placeholder_in);
            }
        }

    }


}
