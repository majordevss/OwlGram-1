package org.master.advertizment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.google.android.exoplayer2.util.Log;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;


import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

public class AddManager {

    private InterstitialAd admobInterstitialAd;

    public AtomicInteger counter =new AtomicInteger();
    private AdRequest adRequest;

    public void showAdd(Activity context){
        if(counter.getAndIncrement() % 5 != 0){
            return;
        }
        if(admobInterstitialAd != null){
            admobInterstitialAd.show(context);
//            loadAdmobInterstitialAdd(context);
        }else{
            if(unityInterstitialAdLoaded){
                DisplayInterstitialAd(context);
            }
        }
    }

    public void loadUnityAds(Context context){

    }


    public  void loadAdmobInterstitialAdd(Context context){
        if(adRequest == null){
            adRequest = new AdRequest.Builder().build();
        }
        String addUnit =  AddConstant.getAddUnit(AddConstant.AddUnitType.INTERSTITIAL);
        if(TextUtils.isEmpty(addUnit)){
            return;
        }
        InterstitialAd.load(context, addUnit, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        admobInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        admobInterstitialAd = null;
                    }
                });
    }

    public static class CustomAdd{

        public String desc;
        public String image;
        public String link;
        public String title;
    }

    public CustomAdd customAdd;

    public void loadCustomAdds(){
        String link = "https://fastchanneladdinglink.firebaseio.com/customadd/0.json";
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(link);
                    URLConnection urlConn = url.openConnection();
                    HttpsURLConnection httpsConn = (HttpsURLConnection) urlConn;
                    httpsConn.setRequestMethod("GET");
                    httpsConn.connect();
                    InputStream in;
                    String result = "";
                    if(httpsConn.getResponseCode() ==  200){
                        in = httpsConn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                in, "iso-8859-1"), 8);
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        in.close();
                        result = sb.toString();
                    }
                    JSONObject jsonObject = new JSONObject(result);
                    CustomAdd add = new CustomAdd();
                    add.desc = jsonObject.getString("desc");
                    add.title =jsonObject.getString("title");
                    add.link = jsonObject.getString("link");
                    add.image = jsonObject.getString("image");
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            customAdd = add;
                        }
                    });

                }catch (Exception e){
                    Log.i("rsult",e.getMessage());
                }
            }
        });
    }


    public static final String TAG = AddManager.class.getSimpleName();


    public AdListener getAdmobAddListener() {
        return admobAddListener;
    }

    //google
    private AdLoader adLoader;
    private boolean admobAddInit;
    private AdListener admobAddListener = new AdListener() {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            super.onAdFailedToLoad(loadAdError);
            Log.d("loadamdbnativead","onAdFailedToLoad: erro = " + loadAdError.toString());

        }

        @Override
        public void onAdOpened() {
            super.onAdOpened();
        }


        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            Log.d("loadamdbnativead","onAdLoaded: onAdLoaded = ");
            MessagesController.getInstance(UserConfig.selectedAccount).sortDialogs(null);
        }

        @Override
        public void onAdClicked() {
            super.onAdClicked();
        }

        @Override
        public void onAdImpression() {
            super.onAdImpression();
        }
    };

    private  static AddManager Instance;

    //app settings
    public static SharedPreferences preferences;
    public  static boolean addEnabled;
    public static boolean facebookAddEnabled;
    public static  boolean admobAddEnabled;
    public static  boolean customAddEnabled;

    //
    private FirebaseRemoteConfig mFirebaseRemoteConfig;


    public static AddManager getInstance() {
        AddManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (AddManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new AddManager();
                }
            }
        }
        return localInstance;
    }

    public AddManager(){
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("addSettings",Context.MODE_PRIVATE);
        addEnabled = preferences.getBoolean("addEnabled",true);
        facebookAddEnabled = preferences.getBoolean("facebookAddEnabled",true);
        admobAddEnabled = preferences.getBoolean("admobAddEnabled",true);
        customAddEnabled = preferences.getBoolean("customAddEnabled",true);


    }


    private void updatePref(){
        SharedPreferences.Editor editor =  preferences.edit();
        editor.putBoolean("addEnabled",addEnabled);
        editor.putBoolean("facebookAddEnabled",addEnabled);
        editor.putBoolean("admobAddEnabled",addEnabled);
        editor.putBoolean("customAddEnabled",addEnabled);
        editor.commit();
    }



    public void loadAddSettings(){
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d(TAG, "Config params updated: " + updated);
                            Toast.makeText(ApplicationLoader.applicationContext, "Fetch and activate succeeded",
                                    Toast.LENGTH_SHORT).show();
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    customAddEnabled = mFirebaseRemoteConfig.getBoolean("customAddEnabled");
                                    addEnabled = mFirebaseRemoteConfig.getBoolean("addEnabled");
                                    facebookAddEnabled = mFirebaseRemoteConfig.getBoolean("facebookAddEnabled");
                                    admobAddEnabled = mFirebaseRemoteConfig.getBoolean("admobAddEnabled");
                                    updatePref();
                                }
                            });

                        } else {
//                            Toast.makeText(ApplicationLoader.applicationContext, "Fetch failed",
//                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }


    private boolean unityInterstitialAdLoaded;

    public void loadUnityInterstialAdd(){
//        UnityAds.load("Interstitial_Android", new IUnityAdsLoadListener() {
//            @Override
//            public void onUnityAdsAdLoaded(String s) {
//                unityInterstitialAdLoaded = true;
//            }
//
//            @Override
//            public void onUnityAdsFailedToLoad(String s, UnityAds.UnityAdsLoadError unityAdsLoadError, String s1) {
//                unityInterstitialAdLoaded = false;
//
//            }
//        });
    }




    public void DisplayInterstitialAd (Activity activity) {
       if(!unityInterstitialAdLoaded){
           return;
       }
        unityInterstitialAdLoaded = false;
//        UnityAds.show(activity, "Interstitial_Android", new UnityAdsShowOptions(), new IUnityAdsShowListener() {
//            @Override
//            public void onUnityAdsShowFailure(String s, UnityAds.UnityAdsShowError unityAdsShowError, String s1) {
//
//            }
//
//            @Override
//            public void onUnityAdsShowStart(String s) {
//
//            }
//
//            @Override
//            public void onUnityAdsShowClick(String s) {
//
//            }
//
//            @Override
//            public void onUnityAdsShowComplete(String s, UnityAds.UnityAdsShowCompletionState unityAdsShowCompletionState) {
//
//            }
//        });

    }

    public void initAddNetworks(){
        MobileAds.initialize(ApplicationLoader.applicationContext, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                admobAddInit = true;

            }
        });
//        UnityAds.initialize(ApplicationLoader.applicationContext, "4821512", true, new IUnityAdsInitializationListener() {
//            @Override
//            public void onInitializationComplete() {
//            }
//            @Override
//            public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
//            }
//        });
        //facebook add network
//        AudienceNetworkAds.buildInitSettings(ApplicationLoader.applicationContext).withInitListener(new AudienceNetworkAds.InitListener() {
//            @Override
//            public void onInitialized(AudienceNetworkAds.InitResult initResult) {
//                facebookAddInit = initResult.isSuccess();
//                Log.i(TAG,  "onInitializationStatusFacebook"+ ":"+ initResult.getMessage());
//            }
//        }).initialize();
    }

    public ArrayList<NativeAd> nativeAds= new ArrayList<>();
    private int posation;
    public NativeAd getNativeadd(){
        if(posation >= nativeAds.size() || posation < 0){
            posation = 0;
        }
        return nativeAds.get(posation++);
    }




    public void loadAdmobNativeAdd(){
        if(adLoader == null){
            adLoader = new AdLoader.Builder(ApplicationLoader.applicationContext,AddConstant.getAddUnit(AddConstant.AddUnitType.NATIVE) )
                    .forNativeAd(nativeAd -> {
                        nativeAds.clear();
                        nativeAds.add(nativeAd);
                    })
                    .withAdListener(admobAddListener)
                    .build();
        }
        adLoader.loadAds(new AdRequest.Builder().build(), 1);
    }



    private void destroy(){

        if(nativeAds != null){
            nativeAds.clear();
        }
        admobAddListener = null;

    }
}
