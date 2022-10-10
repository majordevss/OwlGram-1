package org.master.feature.feed;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;

public class FeedActivity extends BaseFragment {

    private SimpleTextView nameTextView;
    ProfileActivity.AvatarImageView avatarImageView;
    private FeedMediaLayout feedMediaLayout;
    AudioPlayerAlert.ClippingTextViewSwitcher mediaCounterTextView;


    @Override
    public View createView(Context context) {
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        FrameLayout avatarContainer = new FrameLayout(context);
        FrameLayout fragmentView = new FrameLayout(context) {

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                LayoutParams lp = (LayoutParams) feedMediaLayout.getLayoutParams();
                lp.topMargin = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);

                lp = (LayoutParams) avatarContainer.getLayoutParams();
                lp.topMargin = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
                lp.height = ActionBar.getCurrentActionBarHeight();

                int textTop = (ActionBar.getCurrentActionBarHeight() / 2 - AndroidUtilities.dp(22)) / 2 + AndroidUtilities.dp(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 5);
                lp = (LayoutParams) nameTextView.getLayoutParams();
                lp.topMargin = textTop;

                textTop = ActionBar.getCurrentActionBarHeight() / 2 + (ActionBar.getCurrentActionBarHeight() / 2 - AndroidUtilities.dp(19)) / 2 - AndroidUtilities.dp(3);
                lp = (LayoutParams) mediaCounterTextView.getLayoutParams();
                lp.topMargin = textTop;

                lp = (LayoutParams) avatarImageView.getLayoutParams();
                lp.topMargin = (ActionBar.getCurrentActionBarHeight() - AndroidUtilities.dp(42)) / 2;
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }


        };
        this.fragmentView = fragmentView;

        mediaCounterTextView = new AudioPlayerAlert.ClippingTextViewSwitcher(context) {
            @Override
            protected TextView createTextView() {
                TextView textView = new TextView(context);
                textView.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setGravity(Gravity.LEFT);
                return textView;
            }
        };
        avatarContainer.addView(mediaCounterTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 56, 0));


        nameTextView = new SimpleTextView(context);

        nameTextView.setTextSize(18);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
        nameTextView.setScrollNonFitText(true);
        nameTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        avatarContainer.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 56, 0));

        avatarImageView = new ProfileActivity.AvatarImageView(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                if (getImageReceiver().hasNotThumb()) {
                    info.setText(LocaleController.getString("AccDescrProfilePicture", R.string.AccDescrProfilePicture));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, LocaleController.getString("Open", R.string.Open)));
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, LocaleController.getString("AccDescrOpenInPhotoViewer", R.string.AccDescrOpenInPhotoViewer)));
                    }
                } else {
                    info.setVisibleToUser(false);
                }
            }
        };
        avatarImageView.getImageReceiver().setAllowDecodeSingleFrame(true);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(21));
        avatarImageView.setPivotX(0);
        avatarImageView.setPivotY(0);
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setProfile(true);
        avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_FILTER_CHANNELS);
        avatarImageView.setImage("https://cdn-icons-png.flaticon.com/512/3698/3698144.png","50_50",avatarDrawable);

//        avatarImageView.setImageDrawable(avatarDrawable);
        avatarContainer.addView(avatarImageView, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));


       feedMediaLayout = new FeedMediaLayout(context,FeedActivity.this);
//
        fragmentView.addView(feedMediaLayout);
        fragmentView.addView(actionBar);
        fragmentView.addView(avatarContainer);

        nameTextView.setText("News");
        mediaCounterTextView.setText("123 Feed");

        AndroidUtilities.updateViewVisibilityAnimated(avatarContainer, true, 1, false);
        updateColors();

        return    fragmentView;
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        if (!feedMediaLayout.isSwipeBackEnabled()) {
            return false;
        }

        return feedMediaLayout.isCurrentTabFirst();
    }

    private void updateColors() {
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarActionModeDefaultSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate themeDelegate = () -> {
            updateColors();
        };
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_actionBarActionModeDefaultSelector));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.addAll(feedMediaLayout.getThemeDescriptions());
        return arrayList;
    }
}
