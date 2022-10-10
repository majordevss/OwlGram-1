//package org.master.advertizment;/*
// * This is the source code of Telegram for Android v. 5.x.x.
// * It is licensed under GNU GPL v. 2 or later.
// * You should have received a copy of the license in this archive (see LICENSE).
// *
// * Copyright Nikolai Kudashov, 2013-2018.
// */
//
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//
//import com.google.android.exoplayer2.util.Log;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.LocaleController;
//import org.telegram.messenger.UserConfig;
//import org.telegram.messenger.browser.Browser;
//import org.telegram.ui.ActionBar.Theme;
//import org.telegram.ui.Components.AvatarDrawable;
//import org.telegram.ui.Components.BackupImageView;
//import org.telegram.ui.Components.LayoutHelper;
//
//public class AdDialogCell extends FrameLayout {
//
//    private TextView nameTextView;
//    private BackupImageView avatarImageView;
//    private TextView statusTextView;
//    private TextView sponserTextView;
//
//    private AvatarDrawable avatarDrawable;
//
//
//    private int currentAccount = UserConfig.selectedAccount;
//
//    int padding = 8;
//    int additionalPadding = 0;
//
//    private FrameLayout frameLayout;
//    public AdDialogCell(Context context) {
//        super(context);
//
//        frameLayout = new FrameLayout(context);
//        addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 0));
//
//        avatarDrawable = new AvatarDrawable();
//        avatarDrawable.setInfo(5,"Sponsor","");
//        avatarImageView = new BackupImageView(context);
//        avatarImageView.setImage(null,null,avatarDrawable);
//        avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
//        frameLayout.addView(avatarImageView, LayoutHelper.createFrame(46, 46, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 7 + padding, 6, LocaleController.isRTL ? 7 + padding : 0, 0));
//
//
//        nameTextView = new TextView(context);
//        nameTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
//        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//        nameTextView.setText("Sponsor Message");
//        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
//
//        nameTextView.setGravity((Gravity.LEFT) | Gravity.TOP);
//        frameLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (Gravity.LEFT) | Gravity.TOP, (64 + padding), 10, 28 + additionalPadding + 50, 0));
//
//        statusTextView = new TextView(context);
//        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
//        statusTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
//        statusTextView.setText("No Sponsor Message");
//        statusTextView.setMaxLines(2);
//        statusTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
//        frameLayout.addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 + additionalPadding : (64 + padding), 32, LocaleController.isRTL ? (64 + padding) : 28 + additionalPadding, 0));
//
//
//        sponserTextView = new TextView(context);
//        sponserTextView.setText("Sponsor".toUpperCase());
//        sponserTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8);
//        sponserTextView.setPadding(AndroidUtilities.dp(6),AndroidUtilities.dp(2),AndroidUtilities.dp(6),AndroidUtilities.dp(2));
//        sponserTextView.setTextColor(Theme.getColor(Theme.key_chat_stickerNameText));
//        sponserTextView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12),Theme.getColor(Theme.key_chats_archiveBackground)));
//        frameLayout.addView(sponserTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (Gravity.RIGHT) | Gravity.TOP, 28 + additionalPadding, 8, (16 + 0), 0));
//
//    }
//
//    public void setCustomAdd(){
//        if(AddManager.getInstance().customAdd == null){
//            return;
//        }
//
////        AddManager.CustomAdd nativeAd = AddManager.getInstance().customAdd;
////        nameTextView.setText(nativeAd.title);
////        statusTextView.setText(nativeAd.desc);
////        String photo = nativeAd.image;
////        avatarImageView.setImage(photo,null,avatarDrawable);
////        setOnClickListener(new OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                Browser.openUrl(getContext(),nativeAd.link);
////            }
////        });
//    }
//
//
//    private void clearExistingAdds(){
//        if(frameLayout.getParent() != null){
//            ViewGroup viewGroup =(ViewGroup) frameLayout.getParent();
//            viewGroup.removeAllViews();
//        }
//        frameLayout.removeAllViews();
//
//    }
//
//
//
////    public void setGoogleAdd(){
////        if(AddManager.getInstance().admobNativeAdd == null){
////            return;
////        }
////
////        NativeAd nativeAd = AddManager.getInstance().admobNativeAdd;
////
////        nameTextView.setText(nativeAd.getHeadline());
////        statusTextView.setText(nativeAd.getBody());
////        String photo = null;
////        if(nativeAd.getIcon() != null && nativeAd.getIcon().getUri() != null){
////            photo = nativeAd.getIcon().getUri().toString();
////        }
////        avatarImageView.setImage(photo,null,avatarDrawable);
////        setOnClickListener(new OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                Log.i("testval","url = " + nativeAd.getStore());
////                Log.i("testval","extra" +  nativeAd.getExtras().toString());
////               AddManager.getInstance().getAdmobAddListener().onAdClicked();
////
////            }
////        });
////    }
//
//
//
//
//
//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//    }
//
//    @Override
//    protected void onAttachedToWindow() {
//        super.onAttachedToWindow();
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(70),MeasureSpec.EXACTLY));
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        canvas.drawLine(AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
//
//    }
//
//}
