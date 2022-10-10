package org.master.feature;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CombinedDrawable;

public class AppTheme {

    public static final int COLOR_SEND = 0xff4caf50;
    public static final int COLOR_DEPOSIT = 0XFF673ab7;
    public static final int COLOR_WITHDRAW= 0XFFe91e63;
    public static final int COLOR_RECEIVE = 0XFF2196f3;

    public static final int PENDING_COLOR= 0xFFF29339;


    public static Drawable cloud;


    public static Drawable scanDrawable;
    public static Drawable marketplaceDrawable;
    public static Drawable rideDrawable;
    public static Drawable feedDrawable;
    public static Drawable peopleNearbyDrawable;
    public static Drawable walletDrawable;
    public static Drawable restaurantDrawable;
    public static Drawable jobsDrawable;
    public static Drawable newChatDrawable;

    private static boolean dialogDrawableLoaded;



    private static CombinedDrawable  createCombinedDrawable(int icon,int color){
        Drawable backgroundDrawable  = Theme.createCircleDrawable(AndroidUtilities.dp(42),color);
        Drawable iconDrawable  = ApplicationLoader.applicationContext.getResources().getDrawable(icon);
        CombinedDrawable combinedDrawable = new CombinedDrawable(backgroundDrawable,iconDrawable);
        combinedDrawable.setIconSize(AndroidUtilities.dp(24),AndroidUtilities.dp(24));
        return combinedDrawable;
    }

    private static CombinedDrawable  createCombinedDrawable(int icon,int backgroundColor,int iconColor){
        Drawable backgroundDrawable  = Theme.createCircleDrawable(AndroidUtilities.dp(42),backgroundColor);
        Drawable iconDrawable  = ApplicationLoader.applicationContext.getResources().getDrawable(icon);
        if(iconDrawable != null){
            iconDrawable.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
        }
        CombinedDrawable combinedDrawable = new CombinedDrawable(backgroundDrawable,iconDrawable);
        combinedDrawable.setIconSize(AndroidUtilities.dp(24),AndroidUtilities.dp(24));
        return combinedDrawable;
    }



    public static Drawable getDrawable(int res) {
        return ApplicationLoader.applicationContext.getResources().getDrawable(res);
    }


}
