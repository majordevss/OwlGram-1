package org.master.feature.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import org.master.feature.TelegramMessageUpdateManager;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.UserConfig;

@Database(entities = {
        TelegramMessageUpdateManager.UpdateObject.class,
        TelegramMessageUpdateManager.UpdateUser.class,
        Category.class,
        HiddenDialog.class,
        ContactChange.class}, version = 6, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase{

   public abstract CategoryDao categoryDao();
   public abstract HiddenDialogDao dialogDao();
    public abstract ContactChangeDao  contactChangeDao();
    public abstract AppDao appDao();



    public  static DispatchQueue databaseQueue = new DispatchQueue("databaseQueue");


    private static volatile AppDatabase[] Instance = new AppDatabase[UserConfig.MAX_ACCOUNT_COUNT];


    public static AppDatabase getInstance(int num) {
        AppDatabase localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AppDatabase.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = Room.databaseBuilder(ApplicationLoader.applicationContext,
                            AppDatabase.class, "app_databse_" + num)
                            .enableMultiInstanceInvalidation()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return localInstance;
    }


    public void logout(){
        databaseQueue.postRunnable(() -> {
            categoryDao().deleteAll();
            dialogDao().deleteAll();
            contactChangeDao().clearAll();
            appDao().clearAllUpdate();
            appDao().clearAllUserUpdate();

        });

    }


}

