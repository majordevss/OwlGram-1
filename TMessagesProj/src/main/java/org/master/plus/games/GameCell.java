package org.master.plus.games;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Random;

public class GameCell extends LinearLayout {

    public BackupImageView imageView;
    private TextView nameTextView;
    private LinearLayout tagLayout;
    private int itemsCount = 2;
    private TextView catTextView;
    private TextView firstTagView;
    private TextView secTagView;
    private Drawable placeHolder;

    public GameCell(@NonNull Context context, Drawable placeHolder) {
        super(context);
        setOrientation(VERTICAL);
        this.placeHolder = placeHolder;

        imageView = new BackupImageView(context);
        imageView.setSize(getItemSize(itemsCount),getItemSize(itemsCount));
        addView(imageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        tagLayout = new LinearLayout(context);
        tagLayout.setOrientation(HORIZONTAL);
        for (int a = 0; a < 2;a++){
            TextView textView = new TextView(context);
            if(a == 0){
                firstTagView = textView;
            }else{
                secTagView = textView;
            }
            textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12),Theme.getColor(Theme.keys_avatar_background[new Random().nextInt(Theme.keys_avatar_background.length)])));
            textView.setTextColor(Theme.getColor(Theme.key_avatar_text));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            textView.setPadding(AndroidUtilities.dp(2),AndroidUtilities.dp(4),AndroidUtilities.dp(2),AndroidUtilities.dp(4));
            textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            tagLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,Gravity.LEFT|Gravity.CENTER_VERTICAL));

        }


        addView(tagLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        nameTextView = new TextView(context);
        nameTextView.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8));
        nameTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 13, -0.7f, 0, 0));


        catTextView = new TextView(context);
        catTextView.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8));
        catTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
        catTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        catTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(catTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 13, -0.7f, 0, 0));

    }

    public GameModel getCurrentGameModel() {
        return currentGameModel;
    }


    private GameModel currentGameModel;
    public void setGame(GameModel gameModel){
        if(currentGameModel == gameModel || gameModel == null){
            return;
        }
        currentGameModel = gameModel;
        imageView.setImage(gameModel.photo,"100_100",placeHolder);
        nameTextView.setText(gameModel.name);
        if(gameModel.category != null){
            catTextView.setVisibility(VISIBLE);
            catTextView.setText(gameModel.category.title);
        }else{
            catTextView.setVisibility(GONE);
        }
        if(gameModel.tags != null && gameModel.tags.size() > 0){
            tagLayout.setVisibility(VISIBLE);
             firstTagView.setText(gameModel.tags.get(0));
             if(gameModel.tags.size() >= 2){
                 secTagView.setText(gameModel.tags.get(0));
             }
        }else{
            tagLayout.setVisibility(GONE);
        }
    }


    public static int getItemSize(int itemsCount) {
        final int itemWidth;
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - (itemsCount - 1) * AndroidUtilities.dp(2)) / itemsCount;
        }
        return itemWidth;
    }
}
