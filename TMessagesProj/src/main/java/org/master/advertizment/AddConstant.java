package org.master.advertizment;

import com.google.android.exoplayer2.util.Log;
import com.onesignal.OneSignal;

import org.master.feature.database.AppDao;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;

public class AddConstant {

    public static final int ADD_TYPE_ADMOB = 1;
    public static final int ADD_TYPE_FACEBOOK = 2;
    public static final int ADD_TYPE_UNITY = 3;
    public static final int ADD_TYPE_CUSTOM = 4;

    public enum AddUnitType{
        NATIVE,
        INTERSTITIAL,
        BANNER,
        REWARDED
    }

    public enum AppAddUnit{
        MOBOGRAM_MESSENGER("com.org.mobogold.messenger.mobo","ca-app-pub-2480339536903760/8577604574", "ca-app-pub-2480339536903760/1410341242","ca-app-pub-2480339536903760~4461168117","591a257f-e970-4af9-b4e5-3ea4e271abde"),
        MOBOGRAM("messenger.mobo.plus.original","ca-app-pub-2388049881772306/9423215132","ca-app-pub-2388049881772306/4868607044","ca-app-pub-2388049881772306~8657175792","07cf1e72-7aa7-486b-a738-167e0661396c"),
        DEBUG_APP("","ca-app-pub-3940256099942544/1033173712","ca-app-pub-3940256099942544/2247696110","ca-app-pub-3940256099942544~3347511713","9fcdc34a-7813-4774-aa37-0d9611b106e1");

        AppAddUnit(String packageName,String interstitial,String nativeAdd,String appId,String oneSignalAppId){
            this.packageName = packageName;
            this.interstitial = interstitial;
            this.nativeAdd = nativeAdd;
            this.appId = appId;
            this.oneSignalAppId = oneSignalAppId;
        }

        public String packageName;
        public String interstitial;
        public String nativeAdd;
        public String appId;
        public String oneSignalAppId;
    }

    public static String getAddUnit(AddUnitType addUnitType){
        String packageName = ApplicationLoader.applicationContext.getPackageName();
        Log.d("packageName", packageName);
        if(packageName == null){
            return "";
        }
        switch (addUnitType) {
            case INTERSTITIAL:
                if (BuildConfig.DEBUG) {
                    return AppAddUnit.DEBUG_APP.interstitial;//test add unit
                }
                if (packageName.equals(AppAddUnit.MOBOGRAM_MESSENGER.packageName)) {
                    return AppAddUnit.MOBOGRAM_MESSENGER.interstitial;
                }else if (packageName.equals(AppAddUnit.MOBOGRAM.packageName)) {
                 return AppAddUnit.MOBOGRAM.interstitial;
            }
                break;
            case NATIVE:
                if (BuildConfig.DEBUG) {
                    return AppAddUnit.DEBUG_APP.nativeAdd;//test add unit
                }
                if (packageName.equals(AppAddUnit.MOBOGRAM_MESSENGER.packageName)) {
                    return AppAddUnit.MOBOGRAM_MESSENGER.nativeAdd;
                }else  if (packageName.equals(AppAddUnit.MOBOGRAM.packageName)) {
                    return AppAddUnit.MOBOGRAM.nativeAdd;
                }
                break;
        }
        return "";
    }

    public static String getOneSignalAppId(){
        String packageName = ApplicationLoader.applicationContext.getPackageName();
        if (packageName.equals(AppAddUnit.MOBOGRAM_MESSENGER.packageName)) {
            return AppAddUnit.MOBOGRAM_MESSENGER.oneSignalAppId;
        }else if (packageName.equals(AppAddUnit.MOBOGRAM.packageName)) {
            return AppAddUnit.MOBOGRAM.oneSignalAppId;
        }
        return AppAddUnit.DEBUG_APP.oneSignalAppId;
    }

    public static void intNotification(){
        //plus
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(ApplicationLoader.applicationContext);
        OneSignal.setAppId(AddConstant.getOneSignalAppId());
        //
    }



}
