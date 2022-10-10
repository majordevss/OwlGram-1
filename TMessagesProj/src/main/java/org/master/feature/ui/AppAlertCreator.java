package org.master.feature.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.master.feature.SharedAppConfig;
import org.master.feature.ui.cells.InnerButtonCell;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

public class AppAlertCreator {

    public interface Callback{
       default void onBool(boolean bool){};

       default void onString(String str){}
    }

//    public static void createCategoryCreateDialog(BaseFragment fragment, Category category.json, ArrayList<Long> dialogs) {
//        if (fragment == null || fragment.getParentActivity() == null) {
//            return;
//        }
//        Context context = fragment.getParentActivity();
//        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
//        editText.setBackgroundDrawable(Theme.createEditTextDrawable(context, true));
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle(LocaleController.getString("NewCategoryTitle", R.string.NewCategoryTitle));
//        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
//        builder.setPositiveButton(LocaleController.getString("Create", R.string.Create), (dialog, which) -> {
//
//        });
//
//        LinearLayout linearLayout = new LinearLayout(context);
//        linearLayout.setOrientation(LinearLayout.VERTICAL);
//        builder.setView(linearLayout);
//
//        final TextView message = new TextView(context);
//        if (category.json != null) {
//            message.setText(AndroidUtilities.replaceTags(LocaleController.getString("EnterCategoryEdit", R.string.EnterCategoryEdit)));
//        } else {
//            message.setText(LocaleController.getString("EnterCategoryName", R.string.EnterCategoryName));
//        }
//        message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
//        message.setPadding(AndroidUtilities.dp(23), AndroidUtilities.dp(12), AndroidUtilities.dp(23), AndroidUtilities.dp(6));
//        message.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
//        linearLayout.addView(message, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
//
//        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
//        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
//        editText.setMaxLines(1);
//        editText.setLines(1);
//        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
//        editText.setGravity(Gravity.LEFT | Gravity.TOP);
//        editText.setSingleLine(true);
//        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
//        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//        editText.setCursorSize(AndroidUtilities.dp(20));
//        editText.setCursorWidth(1.5f);
//        editText.setPadding(0, AndroidUtilities.dp(4), 0, 0);
//        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));
//        editText.setOnEditorActionListener((textView, i, keyEvent) -> {
//            AndroidUtilities.hideKeyboard(textView);
//            return false;
//        });
//        editText.setSelection(editText.length());
//
//        final AlertDialog alertDialog = builder.create();
//        alertDialog.setOnShowListener(dialog -> AndroidUtilities.runOnUIThread(() -> {
//            editText.requestFocus();
//            AndroidUtilities.showKeyboard(editText);
//        }));
//        fragment.showDialog(alertDialog);
//        editText.requestFocus();
//        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
//            if (fragment.getParentActivity() == null) {
//                return;
//            }
//            if (editText.length() == 0) {
//                Vibrator vibrator = (Vibrator) ApplicationLoader.applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
//                if (vibrator != null) {
//                    vibrator.vibrate(200);
//                }
//                AndroidUtilities.shakeView(editText, 2, 0);
//                return;
//            }
//            alertDialog.dismiss();
//            Bundle arg = new Bundle();
//            arg.putInt("folderId",0);
//            arg.putBoolean("onlySelect",true);
//            arg.putInt("dialogsType",10);
//            DialogsActivity dialogsActivity = new DialogsActivity(arg);
//            dialogsActivity.setDelegate(new DialogsActivity.DialogsActivityDelegate() {
//                @Override
//                public void didSelectDialogs(DialogsActivity fragment, ArrayList<Long> dids, CharSequence message, boolean param) {
//                    fragment.finishFragment();
//                    int newId = ++CategoryManager.max_filter_id;
//                    Category localCategory = new Category();
//                    localCategory.title = editText.getText().toString();
//                    localCategory.locked = false;
//                    localCategory.hash = "";
//                    localCategory.order = CategoryManager.max_order + 1;
//                    localCategory.id  = newId;
//                    localCategory.dialogs = dids;
//                    AppDatabase.databaseQueue.postRunnable(new Runnable() {
//                        @Override
//                        public void run() {
//                            AppDatabase.getInstance(fragment.getCurrentAccount()).categoryDao().insert(localCategory);
//                            AndroidUtilities.runOnUIThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    CategoryManager.getInstance(fragment.getCurrentAccount()).loadCategories();
//                                }
//                            });
//                        }
//
//                    });
//
//
//                }
//            });
//
//            fragment.presentFragment(dialogsActivity);
//
//
//        });
//    }


    public static void showPinInputBottomSheetAlert(BaseFragment baseFragment,int type,Callback stringCallback){
        if(baseFragment == null || baseFragment.getParentActivity() == null){
            return;
        }
        //type 0 ente
        //type 1 create
        Context context = baseFragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Runnable dismissRunnable = builder.getDismissRunnable();
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        builder.setView(container);


        final String[] firstPasswod = new String[1];
        RLottieImageView blueImageView = new RLottieImageView(context);
        if(type == 0){
            blueImageView.setAnimation(R.raw.wallet_science,100,100);

        }else{
            blueImageView.setAnimation(R.raw.wallet_science,100,100);

        }

        blueImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!blueImageView.isPlaying()){
                    blueImageView.playAnimation();
                }
            }
        });
        container.addView(blueImageView, LayoutHelper.createLinear(100, 100, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 16, 0, 0));


        TextView titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        titleTextView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        titleTextView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        if(type == 0){
            titleTextView.setText("Enter Password");
        }else if(type == 1){
            titleTextView.setText("Create New Password!");
        }
        titleTextView.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8));
        container.addView(titleTextView,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        LinearLayout codeFieldContainer = new LinearLayout(context);
        codeFieldContainer.setOrientation(LinearLayout.HORIZONTAL);
        container.addView(codeFieldContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 36, Gravity.CENTER_HORIZONTAL,16,32,16,32));

        int length = 6;
        EditTextBoldCursor[]  codeField = new EditTextBoldCursor[length];
        for (int a = 0; a < length; a++) {
            final int num = a;
            codeField[a] = new EditTextBoldCursor(context);
            codeField[a].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            codeField[a].setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            codeField[a].setCursorSize(AndroidUtilities.dp(20));
            codeField[a].setCursorWidth(1.5f);

            Drawable pressedDrawable = context.getResources().getDrawable(R.drawable.search_dark_activated).mutate();
            pressedDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), PorterDuff.Mode.MULTIPLY));

            codeField[a].setBackgroundDrawable(pressedDrawable);
            codeField[a].setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            codeField[a].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
            codeField[a].setMaxLines(1);
            codeField[a].setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            codeField[a].setPadding(0, 0, 0, 0);
            codeField[a].setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            codeField[a].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            codeFieldContainer.addView(codeField[a], LayoutHelper.createLinear(34, 36, Gravity.CENTER_HORIZONTAL, 0, 0, a != length - 1 ? 7 : 0, 0));
            codeField[a].addTextChangedListener(new TextWatcher() {
                boolean ignoreOnTextChange = false;
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (ignoreOnTextChange) {
                        return;
                    }
                    int len = s.length();
                    if (len >= 1) {
                        if (len > 1) {
                            String text = s.toString();
                            ignoreOnTextChange = true;
                            for (int a = 0; a < Math.min(length - num, len); a++) {
                                if (a == 0) {
                                    s.replace(0, len, text.substring(a, a + 1));
                                } else {
                                    codeField[num + a].setText(text.substring(a, a + 1));
                                }
                            }
                            ignoreOnTextChange = false;
                        }

                        if (num != length - 1) {
                            codeField[num + 1].setSelection(codeField[num + 1].length());
                            codeField[num + 1].requestFocus();
                        }
                        if ((num == length - 1 || num == length - 2 && len >= 2) && getCode(codeField).length() == length) {
                            String code = getCode(codeField);
                            if (TextUtils.isEmpty(code)) {
                                onFieldError(codeFieldContainer,baseFragment.getParentActivity());
                                return;
                            }
                            if(!SharedAppConfig.password.equals(code) && type == 0){
                                titleTextView.setTextColor(Theme.getColor(Theme.key_dialogRedIcon));
                                titleTextView.setText("Wrong Password!");
                                onFieldError(codeFieldContainer,baseFragment.getParentActivity());
                                return;
                            }
                            AndroidUtilities.hideKeyboard(codeField[num]);
                            if(stringCallback != null && type == 0){

                                stringCallback.onBool(true);
                                dismissRunnable.run();
                            }
                        }
                    }
                }
            });
            codeField[a].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && codeField[num].length() == 0 && num > 0) {
                    codeField[num - 1].setSelection(codeField[num - 1].length());
                    codeField[num - 1].requestFocus();
                    codeField[num - 1].dispatchKeyEvent(event);
                    return true;
                }
                return false;
            });
            codeField[a].setOnEditorActionListener((textView, i, keyEvent) -> {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    if(stringCallback != null){
                        stringCallback.onString(getCode(codeField));
                        dismissRunnable.run();
                    }
                    return true;
                }
                return false;
            });
        }

//        FrameLayout bottomLayout;
//        bottomLayout = new FrameLayout(context);
//        bottomLayout.setVisibility(View.INVISIBLE);
//        bottomLayout.setEnabled(false);
//        bottomLayout.setBackgroundDrawable(Theme.createSelectorWithBackgroundDrawable(Theme.getColor(Theme.key_passport_authorizeBackground), Theme.getColor(Theme.key_passport_authorizeBackgroundSelected)));
//        container.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));
//        bottomLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String code = getCode(codeField);
//                if (TextUtils.isEmpty(code)) {
//                    onFieldError(codeFieldContainer,baseFragment.getParentActivity());
//                    return;
//                }
//                if(stringCallback != null){
//                    stringCallback.onString(getCode(codeField));
//                }
//            }
//        });
//        TextView acceptTextView = new TextView(context);
//        acceptTextView.setTextColor(Theme.getColor(Theme.key_passport_authorizeText));
//        acceptTextView.setText("Continue".toUpperCase());
//        acceptTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//        acceptTextView.setGravity(Gravity.CENTER);
//        acceptTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//        bottomLayout.addView(acceptTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        final int[] count = {0};
        if(type == 1){
            InnerButtonCell innerButtonCell = new InnerButtonCell(context);
            innerButtonCell.setText("Continue");
            innerButtonCell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(count[0] > 1){
                        return;
                    }
                    if(count[0] == 0){
                        firstPasswod[0] = getCode(codeField);
                        titleTextView.setText("Confirm password");
                        for(int a = 0; a < codeField.length;a++){
                            codeField[a].setText("");

                        }
                        codeField[0].requestFocus();

                    }
                    if(count[0] == 1){
                        if( firstPasswod[0].equals(getCode(codeField))){
                            SharedAppConfig.password = firstPasswod[0];
                            SharedAppConfig.saveConfig();
                            if(stringCallback != null){
                                stringCallback.onBool(true);
                            }
                        }
                    }
                    count[0] = count[0]++;
                }
            });
          container.addView(innerButtonCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));



        }
//
        AlertDialog bottomSheet = builder.create();

        if(!blueImageView.isPlaying()){
            blueImageView.playAnimation();
        }
        codeField[0].requestFocus();
        AndroidUtilities.showKeyboard(codeField[0]);
        baseFragment.showDialog(bottomSheet);
    }

    private static String getCode(EditTextBoldCursor[] codeField ) {
        if (codeField == null) {
            return "";
        }
        StringBuilder codeBuilder = new StringBuilder();
        for (int a = 0; a < codeField.length; a++) {
            codeBuilder.append(PhoneFormat.stripExceptNumbers(codeField[a].getText().toString()));
        }
        return codeBuilder.toString();
    }

    private static  void onFieldError(View view, Activity activity) {
        try {
            Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(200);
            }
        } catch (Throwable ignore) {

        }
        AndroidUtilities.shakeView(view, 2, 0);
    }


}
