package org.master.feature;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

public class SharedAppConfig {


    public static boolean filterTabDisabled;
    public static String tabsItem;

    private static boolean configLoaded;
    private static final Object sync = new Object();

    public static boolean useTabIcon ;
    public static boolean useLocalTabs;
    public static boolean tabCompactMode = false;
    public static boolean hideTabCounter = false;
    public static boolean autoSortingChat = true;


    public static String password;

    //
    public static boolean hideTypingStatus;
    public static boolean DoNotMarkMessageAsRead;
    public static boolean isGhostModeEnabled;


    //plus TODO
    public static boolean showFolderItem = true;
    public static boolean drawerFullProfile = false;
    public static boolean showGhostMode = true;

    //hiden chat passcode
    public static String passcodeHash = "";
    public static long passcodeRetryInMs;
    public static long lastUptimeMillis;
    public static int badPasscodeTries;
    public static byte[] passcodeSalt = new byte[0];
    public static boolean appLocked;
    public static int passcodeType;
    public static int autoLockIn = 60 * 60;
    public static boolean allowScreenCapture;
    public static int lastPauseTime;
    public static boolean isWaitingForPasscodeEnter;
    public static boolean useFingerprint = true;
    //

    public static int  launchCount;


    static {
        loadConfig();

    }

    public static SharedPreferences getPrefrence(){
       return  ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Context.MODE_PRIVATE);

    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded || ApplicationLoader.applicationContext == null) {
                return;
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Context.MODE_PRIVATE);
            useTabIcon = preferences.getBoolean("useTabIcon",true);

            //passcode
            passcodeHash = preferences.getString("passcodeHash1", "");
            appLocked = preferences.getBoolean("appLocked", false);
            passcodeType = preferences.getInt("passcodeType", 0);
            passcodeRetryInMs = preferences.getLong("passcodeRetryInMs", 0);
            lastUptimeMillis = preferences.getLong("lastUptimeMillis", 0);
            badPasscodeTries = preferences.getInt("badPasscodeTries", 0);
            autoLockIn = preferences.getInt("autoLockIn", 60 * 60);
            lastPauseTime = preferences.getInt("lastPauseTime", 0);
            useFingerprint = preferences.getBoolean("useFingerprint", true);
            allowScreenCapture = preferences.getBoolean("allowScreenCapture", false);
            if (passcodeHash.length() > 0 && lastPauseTime == 0) {
                lastPauseTime = (int) (SystemClock.elapsedRealtime() / 1000 - 60 * 10);
            }
            String passcodeSaltString = preferences.getString("passcodeSalt", "");
            if (passcodeSaltString.length() > 0) {
                passcodeSalt = Base64.decode(passcodeSaltString, Base64.DEFAULT);
            } else {
                passcodeSalt = new byte[0];
            }
            //

            tabsItem = preferences.getString("tabsItem", "[]");
            useLocalTabs = preferences.getBoolean("useLocalTabs", true);
            useTabIcon = preferences.getBoolean("useTabIcon", true);

            hideTypingStatus = preferences.getBoolean("hideTypingStatus", false);
            DoNotMarkMessageAsRead = preferences.getBoolean("DoNotMarkMessageAsRead", false);
            isGhostModeEnabled = preferences.getBoolean("isGhostModeEnabled", false);
            password = preferences.getString("password","");

            launchCount = preferences.getInt("launchCount",0);
            filterTabDisabled = preferences.getBoolean("filterTabDisabled", false);


            configLoaded = true;
        }
    }


    public static void incrementLaunchCount(){
        launchCount = launchCount + 1;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("launchCount", launchCount);
        editor.apply();
    }



    public static void setTabsItems(String data) {
        tabsItem = data;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("tabsItem", tabsItem);
        editor.apply();
    }

    public static void switchUserLocalTabs(){
        useLocalTabs = !useLocalTabs;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("useLocalTabs", useLocalTabs);
        editor.apply();
    }

    public static void switchUseTabIcons(){
        useTabIcon = !useTabIcon;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("useTabIcon", useTabIcon);
        editor.apply();
    }






    public static void saveConfig() {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("sharedAppConfig", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("useTabIcon",useTabIcon);
                //passcode related
                editor.putString("passcodeHash1", passcodeHash);
                editor.putString("passcodeSalt", passcodeSalt.length > 0 ? Base64.encodeToString(passcodeSalt, Base64.DEFAULT) : "");
                editor.putBoolean("appLocked", appLocked);
                editor.putInt("passcodeType", passcodeType);
                editor.putLong("passcodeRetryInMs", passcodeRetryInMs);
                editor.putLong("lastUptimeMillis", lastUptimeMillis);
                editor.putInt("badPasscodeTries", badPasscodeTries);
                editor.putInt("autoLockIn", autoLockIn);
                editor.putInt("lastPauseTime", lastPauseTime);
                //pascode end


                editor.putBoolean("hideTypingStatus", hideTypingStatus);
                editor.putBoolean("DoNotMarkMessageAsRead", DoNotMarkMessageAsRead);
                editor.putBoolean("isGhostModeEnabled", isGhostModeEnabled);

                editor.putString("password", password);


                editor.commit();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public static void increaseBadPasscodeTries() {
        badPasscodeTries++;
        if (badPasscodeTries >= 3) {
            switch (badPasscodeTries) {
                case 3:
                    passcodeRetryInMs = 5000;
                    break;
                case 4:
                    passcodeRetryInMs = 10000;
                    break;
                case 5:
                    passcodeRetryInMs = 15000;
                    break;
                case 6:
                    passcodeRetryInMs = 20000;
                    break;
                case 7:
                    passcodeRetryInMs = 25000;
                    break;
                default:
                    passcodeRetryInMs = 30000;
                    break;
            }
            lastUptimeMillis = SystemClock.elapsedRealtime();
        }
        saveConfig();
    }
    public static void clearConfig() {
        useTabIcon = true;
        appLocked = false;
        passcodeType = 0;
        passcodeRetryInMs = 0;
        lastUptimeMillis = 0;
        badPasscodeTries = 0;
        passcodeHash = "";
        passcodeSalt = new byte[0];
        autoLockIn = 60 * 60;
        lastPauseTime = 0;
        useFingerprint = true;
        isWaitingForPasscodeEnter = false;
        allowScreenCapture = false;
        saveConfig();
    }

    public static boolean checkPasscode(String passcode) {
        if (passcodeSalt.length == 0) {
            boolean result = Utilities.MD5(passcode).equals(passcodeHash);
            if (result) {
                try {
                    passcodeSalt = new byte[16];
                    Utilities.random.nextBytes(passcodeSalt);
                    byte[] passcodeBytes = passcode.getBytes("UTF-8");
                    byte[] bytes = new byte[32 + passcodeBytes.length];
                    System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
                    System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                    System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                    passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
                    saveConfig();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return result;
        } else {
            try {
                byte[] passcodeBytes = passcode.getBytes("UTF-8");
                byte[] bytes = new byte[32 + passcodeBytes.length];
                System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
                System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                String hash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
                return passcodeHash.equals(hash);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return false;
    }




}
