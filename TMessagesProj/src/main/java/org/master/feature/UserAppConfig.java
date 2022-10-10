package org.master.feature;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

public class UserAppConfig extends BaseController {

    public static int selectedAccount;

    private final Object sync = new Object();
    private boolean configLoaded;


    private static volatile UserAppConfig[] Instance = new UserAppConfig[UserConfig.MAX_ACCOUNT_COUNT];
    public static UserAppConfig getInstance(int num) {
        UserAppConfig localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (UserAppConfig.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new UserAppConfig(num);
                }
            }
        }
        return localInstance;
    }


    public UserAppConfig(int num) {
        super(num);
        selectedAccount = num;

    }

    public void saveConfig(boolean withFile) {
        NotificationCenter.getInstance(currentAccount).doOnIdle(() -> {
            synchronized (sync) {
                try {
                    SharedPreferences.Editor editor = getPreferences().edit();

                    editor.commit();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
    }

    public SharedPreferences getPreferences() {
        if (currentAccount == 0) {
            return ApplicationLoader.applicationContext.getSharedPreferences("userAppconfing", Context.MODE_PRIVATE);
        } else {
            return ApplicationLoader.applicationContext.getSharedPreferences("userAppconfig" + currentAccount, Context.MODE_PRIVATE);
        }
    }

    public void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }
            //load user tabs
           // CategoryManager.get(true);
            //

            configLoaded = true;
        }
    }

    public boolean isConfigLoaded() {
        return configLoaded;
    }

    public void clearConfig() {
        getPreferences().edit().clear().commit();


        saveConfig(true);
    }

}
