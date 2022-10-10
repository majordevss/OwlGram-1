package org.master.feature.feed;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Property;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PollVotesAlert;

import java.util.ArrayList;

public class FeedLayout extends FrameLayout {

    private UserCell userCell;
    private LinearLayout linearLayout;
    private TextView awardTextView;
    private TextView titleView;
    private TextView messageTextView;
    private ImageReceiver imageReceiver;


    public FeedLayout(@NonNull Context context) {
        super(context);

        setWillNotDraw(false);
        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);


        userCell = new UserCell(context);
        linearLayout.addView(userCell,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT));


        awardTextView = new TextView(context);
        awardTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        awardTextView.setLines(1);
        awardTextView.setMaxLines(1);
        awardTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        awardTextView.setSingleLine(true);
        awardTextView.setGravity(Gravity.LEFT);
        awardTextView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(awardTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 14 , 8, 12, 0));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setMaxLines(2);
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.LEFT);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 14, 13, 12, 0));

        messageTextView = new TextView(context);
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        messageTextView.setMaxLines(3);
        messageTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        messageTextView.setGravity(Gravity.LEFT);
        messageTextView.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 14 , 8, 12, 0));

        imageReceiver = new ImageReceiver();
        imageReceiver.setAspectFit(true);
        imageReceiver.setInvalidateAll(true);
        imageReceiver.setRoundRadius(AndroidUtilities.dp(6));


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setMessage(MessageObject messageObject){

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        imageReceiver.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        imageReceiver.onDetachedFromWindow();
    }

    public class UserCell extends FrameLayout {
        private Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RectF rect = new RectF();

        private BackupImageView avatarImageView;
        private SimpleTextView nameTextView;

        private AvatarDrawable avatarDrawable;
        private TLRPC.User currentUser;

        private String lastName;
        private int lastStatus;
        private TLRPC.FileLocation lastAvatar;

        private int currentAccount = UserConfig.selectedAccount;

        private boolean needDivider;
        private int placeholderNum;
        private boolean drawPlaceholder;
        private float placeholderAlpha = 1.0f;

        private ArrayList<Animator> animators;

        public UserCell(Context context) {
            super(context);

            setWillNotDraw(false);

            avatarDrawable = new AvatarDrawable();

            avatarImageView = new BackupImageView(context);
            avatarImageView.setRoundRadius(AndroidUtilities.dp(18));
            addView(avatarImageView, LayoutHelper.createFrame(36, 36, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 14, 6, LocaleController.isRTL ? 14 : 0, 0));

            nameTextView = new SimpleTextView(context);
            nameTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameTextView.setTextSize(16);
            nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
            addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 : 65, 14, LocaleController.isRTL ? 65 : 28, 0));
        }

        public void setData(TLRPC.User user, int num, boolean divider) {
            currentUser = user;
            needDivider = divider;
            drawPlaceholder = user == null;
            placeholderNum = num;
            if (user == null) {
                nameTextView.setText("");
                avatarImageView.setImageDrawable(null);
            } else {
                update(0);
            }
            if (animators != null) {
                animators.add(ObjectAnimator.ofFloat(avatarImageView, View.ALPHA, 0.0f, 1.0f));
                animators.add(ObjectAnimator.ofFloat(nameTextView, View.ALPHA, 0.0f, 1.0f));
                animators.add(ObjectAnimator.ofFloat(this, USER_CELL_PROPERTY, 1.0f, 0.0f));
            } else if (!drawPlaceholder) {
                placeholderAlpha = 0.0f;
            }
        }

        @Keep
        public void setPlaceholderAlpha(float value) {
            placeholderAlpha = value;
            invalidate();
        }

        @Keep
        public float getPlaceholderAlpha() {
            return placeholderAlpha;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }

        public void update(int mask) {
            TLRPC.FileLocation photo = null;
            String newName = null;
            if (currentUser != null && currentUser.photo != null) {
                photo = currentUser.photo.photo_small;
            }

            if (mask != 0) {
                boolean continueUpdate = false;
                if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0) {
                    if (lastAvatar != null && photo == null || lastAvatar == null && photo != null || lastAvatar != null && photo != null && (lastAvatar.volume_id != photo.volume_id || lastAvatar.local_id != photo.local_id)) {
                        continueUpdate = true;
                    }
                }
                if (currentUser != null && !continueUpdate && (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    int newStatus = 0;
                    if (currentUser.status != null) {
                        newStatus = currentUser.status.expires;
                    }
                    if (newStatus != lastStatus) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate && lastName != null && (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                    if (currentUser != null) {
                        newName = UserObject.getUserName(currentUser);
                    }
                    if (!newName.equals(lastName)) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate) {
                    return;
                }
            }

            avatarDrawable.setInfo(currentUser);
            if (currentUser.status != null) {
                lastStatus = currentUser.status.expires;
            } else {
                lastStatus = 0;
            }

            if (currentUser != null) {
                lastName = newName == null ? UserObject.getUserName(currentUser) : newName;
            } else {
                lastName = "";
            }
            nameTextView.setText(lastName);

            lastAvatar = photo;
            if (currentUser != null) {
                avatarImageView.setForUserOrChat(currentUser, avatarDrawable);
            } else {
                avatarImageView.setImageDrawable(avatarDrawable);
            }
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (drawPlaceholder || placeholderAlpha != 0) {
                placeholderPaint.setAlpha((int) (255 * placeholderAlpha));
                int cx = avatarImageView.getLeft() + avatarImageView.getMeasuredWidth() / 2;
                int cy = avatarImageView.getTop() + avatarImageView.getMeasuredHeight() / 2;
                canvas.drawCircle(cx, cy, avatarImageView.getMeasuredWidth() / 2, placeholderPaint);

                int w;

                if (placeholderNum % 2 == 0) {
                    cx = AndroidUtilities.dp(65);
                    w = AndroidUtilities.dp(48);
                } else {
                    cx = AndroidUtilities.dp(65);
                    w = AndroidUtilities.dp(60);
                }
                if (LocaleController.isRTL) {
                    cx = getMeasuredWidth() - cx - w;
                }
                rect.set(cx, cy - AndroidUtilities.dp(4), cx + w, cy + AndroidUtilities.dp(4));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), placeholderPaint);

                if (placeholderNum % 2 == 0) {
                    cx = AndroidUtilities.dp(119);
                    w = AndroidUtilities.dp(60);
                } else {
                    cx = AndroidUtilities.dp(131);
                    w = AndroidUtilities.dp(80);
                }
                if (LocaleController.isRTL) {
                    cx = getMeasuredWidth() - cx - w;
                }
                rect.set(cx, cy - AndroidUtilities.dp(4), cx + w, cy + AndroidUtilities.dp(4));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), placeholderPaint);
            }
            if (needDivider) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(64), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(64) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

    public static final Property<UserCell, Float> USER_CELL_PROPERTY = new AnimationProperties.FloatProperty<UserCell>("placeholderAlpha") {
        @Override
        public void setValue(UserCell object, float value) {
            object.setPlaceholderAlpha(value);
        }

        @Override
        public Float get(UserCell object) {
            return object.getPlaceholderAlpha();
        }
    };

}
