package org.master.feature.feed;

import android.content.Context;
import android.text.StaticLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.WebFile;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.RadialProgress2;

import java.util.ArrayList;

public class FeedCell extends FrameLayout implements DownloadController.FileDownloadProgressListener  {

    private final static int DOCUMENT_ATTACH_TYPE_NONE = 0;
    private final static int DOCUMENT_ATTACH_TYPE_DOCUMENT = 1;
    private final static int DOCUMENT_ATTACH_TYPE_GIF = 2;
    private final static int DOCUMENT_ATTACH_TYPE_AUDIO = 3;
    private final static int DOCUMENT_ATTACH_TYPE_VIDEO = 4;
    private final static int DOCUMENT_ATTACH_TYPE_MUSIC = 5;
    private final static int DOCUMENT_ATTACH_TYPE_STICKER = 6;
    private final static int DOCUMENT_ATTACH_TYPE_PHOTO = 7;
    private final static int DOCUMENT_ATTACH_TYPE_GEO = 8;


    private int TAG;
    private int buttonState;
    private RadialProgress2 radialProgress;

    private ImageReceiver imageView;
    private ImageReceiver profileImage;
    private int currentAccount = UserConfig.selectedAccount;

    private int chatNameY = AndroidUtilities.dp(7);
    private StaticLayout chatNameLayout;

    private int chatInfoY = AndroidUtilities.dp(7);
    private StaticLayout chatInfoLayout;

    private int titleY = AndroidUtilities.dp(7);
    private StaticLayout titleLayout;

    private int descriptionY = AndroidUtilities.dp(27);
    private StaticLayout descriptionLayout;




    public FeedCell(@NonNull Context context) {
        super(context);

        imageView = new ImageReceiver(this);
        imageView.setLayerNum(1);
        imageView.setUseSharedAnimationQueue(true);
        radialProgress = new RadialProgress2(this);
        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();
        setFocusable(true);

        profileImage = new ImageReceiver(this);


        setWillNotDraw(false);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        descriptionLayout = null;
        titleLayout = null;
        chatInfoLayout = null;
        chatNameLayout = null;

        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = viewWidth - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - AndroidUtilities.dp(8);

        TLRPC.PhotoSize currentPhotoObjectThumb = null;
        ArrayList<TLRPC.PhotoSize> photoThumbs = null;
        WebFile webFile = null;
        TLRPC.TL_webDocument webDocument = null;
        String urlLocation = null;




        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public void updateButtonState(boolean ifSame, boolean animated) {

    }
        @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        profileImage.onDetachedFromWindow();
        imageView.onDetachedFromWindow();
       radialProgress.onDetachedFromWindow();

        }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (imageView.onAttachedToWindow()) {
             updateButtonState(false, false);
        }
        profileImage.onAttachedToWindow();
        radialProgress.onAttachedToWindow();

    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {

    }

    @Override
    public void onSuccessDownload(String fileName) {

    }

    @Override
    public void onProgressDownload(String fileName, long downloadSize, long totalSize) {

    }

    @Override
    public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }
}
