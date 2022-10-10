package org.master.advertizment;

import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

@SuppressLint("ViewConstructor")
public class AddContextView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private BaseFragment fragment;
    private View applyingView;
    private AnimatorSet animatorSet;
    private FrameLayout frameLayout;
    private ImageView closeButton;

    private float topPadding;

    private View shadow;
    private View selector;

    private int animationIndex = -1;

    private boolean visible;

    private int currentType;

    //facebook
    private TextView addButton;

    //admob
    private BackupImageView notificationImageView;



    //common
    private AudioPlayerAlert.ClippingTextViewSwitcher titleTextView;
    private AudioPlayerAlert.ClippingTextViewSwitcher subtitleTextView;



    public interface  NotificationContextViewDelegate {
        void onAnimation(boolean start, boolean show);
    }

    private NotificationContextViewDelegate notificationContextViewDelegate;

    public void setNotificationContextViewDelegate(NotificationContextViewDelegate notificationContextViewDelegate) {
        this.notificationContextViewDelegate = notificationContextViewDelegate;
    }

    private final int account = UserConfig.selectedAccount;

    public AddContextView(@NonNull Context context,BaseFragment parentFragment, View paddingView) {
        super(context);
        applyingView = paddingView;
        fragment = parentFragment;
        visible = true;
        if (applyingView == null) {
            ((ViewGroup) fragment.getFragmentView()).setClipToPadding(false);
        }
        setTag(1);
        frameLayout =  new FrameLayout(context);
        addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 46, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 0));



        selector = new View(context);
        frameLayout.addView(selector, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        shadow = new View(context);
        shadow.setBackgroundResource(R.drawable.blockpanel_shadow);
        addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2, Gravity.LEFT | Gravity.TOP, 0, 46, 0, 0));

        notificationImageView = new BackupImageView(context);
        notificationImageView.setVisibility(GONE);
        frameLayout.addView(notificationImageView, LayoutHelper.createFrame(38, 38, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));


        titleTextView = new AudioPlayerAlert.ClippingTextViewSwitcher(context) {
            @Override
            protected TextView createTextView() {
                TextView textView = new TextView(context);
                textView.setMaxLines(1);
                textView.setLines(1);
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

                textView.setGravity(Gravity.TOP | Gravity.LEFT);
                textView.setTextColor(Theme.getColor(Theme.key_avatar_text));
                textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                return textView;
            }
        };
        frameLayout.addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 23, Gravity.LEFT | Gravity.TOP, 16 + 38  +  12, 4, 36, 0));

        subtitleTextView = new AudioPlayerAlert.ClippingTextViewSwitcher(context) {
            @Override
            protected TextView createTextView() {
                TextView textView = new TextView(context);
                textView.setMaxLines(1);
                textView.setLines(1);
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setGravity(Gravity.LEFT);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                textView.setTextColor(Theme.getColor(Theme.key_avatar_text));
                return textView;
            }
        };
        frameLayout.addView(subtitleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 23, Gravity.LEFT | Gravity.TOP, 16 + 38  +  12, 23 + 12, 36, 0));

        closeButton = new ImageView(context);
        closeButton.setVisibility(GONE);
        closeButton.setImageResource(R.drawable.miniplayer_close);
        closeButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_inappPlayerClose), PorterDuff.Mode.MULTIPLY));
        if (Build.VERSION.SDK_INT >= 21) {
            closeButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_inappPlayerClose) & 0x19ffffff, 1, AndroidUtilities.dp(14)));
        }
        closeButton.setScaleType(ImageView.ScaleType.CENTER);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        frameLayout.addView(closeButton, LayoutHelper.createFrame(36, 36, Gravity.RIGHT | Gravity.TOP, 0, 0, 2, 0));



        //facebook
        addButton = new TextView(context);
        addButton.setGravity(Gravity.CENTER);
        addButton.setVisibility(GONE);
        addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        addButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addButton.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        addButton.setPadding(AndroidUtilities.dp(17), 0, AndroidUtilities.dp(17), 0);
        frameLayout.addView(addButton, LayoutHelper.createFrame(36, 36, Gravity.RIGHT | Gravity.TOP, 0, 0, 2, 0));



//        setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
    }



    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if(id == NotificationCenter.didAddLoadded){
           int  type = (int)args[0];
            updateType(type);
            if(currentType == AddConstant.ADD_TYPE_ADMOB){
                createAdmobAdd(false);
            }else if(currentType == AddConstant.ADD_TYPE_FACEBOOK){
                Log.i("AddContext","facebook add Loade");
            }else if(currentType == AddConstant.ADD_TYPE_CUSTOM){
                createCusotomAdd(false);
            }
        }
    }


    private void createAdmobAdd(boolean create){
        View fragmentView = fragment.getFragmentView();
        if (!create && fragmentView != null) {
            if (fragmentView.getParent() == null || ((View) fragmentView.getParent()).getVisibility() != VISIBLE) {
                create = true;
            }
        }



    }




    private void createCusotomAdd(boolean create){
        View fragmentView = fragment.getFragmentView();
        if (!create && fragmentView != null) {
            if (fragmentView.getParent() == null || ((View) fragmentView.getParent()).getVisibility() != VISIBLE) {
                create = true;
            }
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, AndroidUtilities.dp2(getStyleHeight() + 2));
    }

    @Keep
    public float getTopPadding() {
        return topPadding;
    }

    @Keep
    public void setTopPadding(float value) {
        topPadding = value;
        if (fragment != null && getParent() != null) {
            View view = applyingView != null ? applyingView : fragment.getFragmentView();
            int additionalPadding = 0;
            if (view != null && getParent() != null) {
                view.setPadding(0, (int) (getVisibility() == View.VISIBLE ? topPadding : 0) + additionalPadding, 0, 0);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didAddLoadded);
        if (visible && topPadding == 0) {
            updatePaddings();
            setTopPadding(AndroidUtilities.dp2(getStyleHeight()));
        }
    }

    private void updateType(int type){
        if(currentType == type){
            return;
        }
        currentType = type;
        if(currentType == AddConstant.ADD_TYPE_FACEBOOK){
        }else if(currentType == AddConstant.ADD_TYPE_ADMOB){

        }else if(currentType == AddConstant.ADD_TYPE_CUSTOM){

        }
    }



    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
        visible = false;
        NotificationCenter.getInstance(account).onAnimationFinish(animationIndex);
        topPadding = 0;
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didAddLoadded);

    }

    public int getStyleHeight() {
        return 46;
    }
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        updatePaddings();
        setTopPadding(topPadding);
    }

    private void updatePaddings() {
        int margin = 0;
        if (getVisibility() == VISIBLE) {
            margin -= AndroidUtilities.dp(getStyleHeight());
        }
        ((LayoutParams) getLayoutParams()).topMargin = margin;
    }

}
