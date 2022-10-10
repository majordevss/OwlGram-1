package org.plus.database;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;


import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.SerializedData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public  class DataStorage extends BaseController {

    private DispatchQueue storageQueue;
    private RoomStorage database;

    public static final String TAG = DataStorage.class.getSimpleName();

    private AtomicLong lastTaskId = new AtomicLong(System.currentTimeMillis());
    private SparseArray<ArrayList<Runnable>> tasks = new SparseArray<>();

    private static volatile DataStorage[] Instance = new DataStorage[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }

    }


    public static DataStorage getInstance(int num) {
        DataStorage localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (lockObjects[num]) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new DataStorage(num);
                }
            }
        }
        return localInstance;
    }

    public DataStorage(int account){
        super(account);
        storageQueue = new DispatchQueue("RoomStorageQueue_" + account);
        storageQueue.postRunnable(this::openDatabase);
    }

    public void openDatabase(){
        database =  Room.databaseBuilder(ApplicationLoader.applicationContext, RoomStorage.class,
                        "app_database_" + currentAccount)
                        .fallbackToDestructiveMigration()
                        .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        loadPendingTasks();
                    }

                    @Override
                    public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                        super.onDestructiveMigration(db);
                    }

                })
                .build();
    }

    public DispatchQueue getStorageQueue() {
        return storageQueue;
    }

    public RoomStorage getDatabase() {
        return database;
    }

    public void clearDatabase(){
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                database.runInTransaction(() -> {
                    try {
                        SimpleSQLiteQuery query =new SimpleSQLiteQuery("DELETE FROM sqlite_sequence");
                        database.clearAllTables();
                        database.query(query);
                    }catch (Exception ignored){

                    }
                });


            }
        });

     }


    @UiThread
    public void bindTaskToGuid(Runnable task, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            tasks.put(guid, arrayList);
        }
        arrayList.add(task);
    }

    @UiThread
    public void cancelTasksForGuid(int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        for (int a = 0, N = arrayList.size(); a < N; a++) {
            storageQueue.cancelRunnable(arrayList.get(a));
        }
        tasks.remove(guid);
    }

    @UiThread
    public void completeTaskForGuid(Runnable runnable, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        arrayList.remove(runnable);
        if (arrayList.isEmpty()) {
            tasks.remove(guid);
        }
    }


    public long createPendingTask(SerializedData data) {
        if (data == null || data.length() < 1) {
            return 0;
        }
        long id = lastTaskId.getAndAdd(1);
        storageQueue.postRunnable(() -> {
            try {
                TableModels.PendingTask pendingTask = new TableModels.PendingTask();
                pendingTask.task_id = id;
                pendingTask.data = data.toByteArray();
                database.pendingTaskDao().insertOrUpdate(pendingTask);

            } catch (Exception e) {
            } finally {
                data.cleanup();
            }
        });
        return id;
    }

    public void removePendingTask(long id) {
        storageQueue.postRunnable(() -> {
            try {
                  database.pendingTaskDao().delete(id);

            } catch (Exception e) {

            }

        });
    }


    private void loadPendingTasks() {
        storageQueue.postRunnable(() -> {
            try {
                List<TableModels.PendingTask> pendingTasks = database.pendingTaskDao().getAll();
                if(pendingTasks != null){

                    for (int a = 0; a < pendingTasks.size();a++){
                        TableModels.PendingTask task = pendingTasks.get(a);
                        if(task != null){
                            long taskId = task.task_id;
                            byte[] byteArray = task.data;
                            if(byteArray != null){
                                SerializedData data = new SerializedData(byteArray);
                                int type = data.readInt32(false);
                                data.cleanup();
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        });
    }
 }

