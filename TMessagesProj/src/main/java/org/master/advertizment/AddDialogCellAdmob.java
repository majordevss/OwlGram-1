package org.master.advertizment;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class AddDialogCellAdmob extends FrameLayout {

    private NativeAdView nativeAdView;
    private TextView attributionViewSmall;
    private TextView attributionViewLarge;
    private ImageView iconView;
    private TextView headlineView;
    private TextView bodyView;
    public AddDialogCellAdmob(@NonNull Context context) {
        super(context);

         nativeAdView = (NativeAdView) LayoutInflater.from(context).inflate(R.layout.native_add_layout, null);
         attributionViewSmall = nativeAdView.findViewById(R.id.tv_list_tile_native_ad_attribution_small);
         attributionViewLarge = nativeAdView.findViewById(R.id.tv_list_tile_native_ad_attribution_large);
         iconView = nativeAdView.findViewById(R.id.iv_list_tile_native_ad_icon);
         headlineView = nativeAdView.findViewById(R.id.tv_list_tile_native_ad_headline);
         bodyView = nativeAdView.findViewById(R.id.tv_list_tile_native_ad_body);
         addView(nativeAdView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL,0,0,0,0));

        updteColors();

    }

    public void setNativeAdd(NativeAd nativeAd){
        if(nativeAd == null){
            return;
        }
        NativeAd.Image icon = nativeAd.getIcon();
        if (icon != null) {
            attributionViewSmall.setVisibility(VISIBLE);
            attributionViewLarge.setVisibility(INVISIBLE);
            iconView.setImageDrawable(icon.getDrawable());
        } else {
            attributionViewSmall.setVisibility(INVISIBLE);
            attributionViewLarge.setVisibility(VISIBLE);
        }
        nativeAdView.setIconView(iconView);
        headlineView.setText(nativeAd.getHeadline());
        nativeAdView.setHeadlineView(headlineView);
        bodyView.setText(nativeAd.getBody());
        bodyView.setVisibility(nativeAd.getBody() != null ? VISIBLE : INVISIBLE);
        nativeAdView.setBodyView(bodyView);
        nativeAdView.setNativeAd(nativeAd);


    }

    private void updteColors(){
        headlineView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        attributionViewLarge.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        bodyView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(70),MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawLine(AndroidUtilities.dp(AndroidUtilities.leftBaseline), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);

    }
}
