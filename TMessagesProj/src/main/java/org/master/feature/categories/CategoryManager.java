package org.master.feature.categories;

import android.util.SparseArray;

import org.master.feature.database.AppDatabase;
import org.master.feature.database.Category;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CategoryManager extends BaseController {

    public static final int MAX_CATEGORY = 10;

    public  static int max_filter_id = 2;
    public  static int max_order;

    public  boolean categoryLoaded;
    public  ArrayList<Category> categories = new ArrayList<>();
    public  HashMap<Integer,Category> categoryHashMap = new HashMap<>();
    public  SparseArray<ArrayList<TLRPC.Dialog>> dialogsByFolder = new SparseArray<>();



    private static volatile CategoryManager[] Instance = new CategoryManager[UserConfig.MAX_ACCOUNT_COUNT];
    public static CategoryManager getInstance(int num) {
        CategoryManager localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (CategoryManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new CategoryManager(num);
                }
            }
        }
        return localInstance;
    }


    public CategoryManager(int num){
        super(num);
    }

    public   void loadCategories(){
        AppDatabase.databaseQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                List<Category> _categories = new ArrayList<>( AppDatabase.getInstance(currentAccount).categoryDao().getAll());
                max_filter_id = 2;
                max_order = 0;
                SparseArray<ArrayList<TLRPC.Dialog>> dialogsByFolderFinal = new SparseArray<>();
                MessagesController messagesController = MessagesController.getInstance(currentAccount);
                ArrayList<Category> _categoriesFinal = new ArrayList<>();
                HashMap<Integer,Category> _categoryHashMapFinal = new HashMap<>();
                for(int a= 0;a < _categories.size(); a++){
                    Category category = _categories.get(a);
                    if(category == null){
                        continue;
                    }
                    _categoriesFinal.add(category);
                    _categoryHashMapFinal.put(category.id,category);

                    ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
                    ArrayList<Long> longArrayList = category.dialogs;
                    for(int i = 0; i < longArrayList.size();i++){
                        TLRPC.Dialog dialog = messagesController.dialogs_dict.get(longArrayList.get(i));
                        if(dialog != null){
                            dialogs.add(dialog);
                        }
                    }
                    //update limits
                    if(category.id > max_filter_id){
                        max_filter_id = category.id;
                    }
                    if(category.order > max_order){
                        max_order = category.order;
                    }
                    //
                    dialogsByFolderFinal.put(category.id,dialogs);
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        dialogsByFolder = dialogsByFolderFinal;
                        categories = _categoriesFinal;
                        categoryHashMap = _categoryHashMapFinal;
                        categoryLoaded = true;
                    }
                });

            }
        });
    }


    private  void createCategory(Category category, MessagesStorage.BooleanCallback booleanCallback){
        AppDatabase.databaseQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                AppDatabase.getInstance(currentAccount).categoryDao().insert(category);
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if(booleanCallback != null){
                            booleanCallback.run(true);
                        }
                        loadCategories();
                    }
                });
            }
        });

    }



    public static void deleteCategories(Category category,Runnable deleteRunnble){
//        int index = -1;
//        for(int a = 0; a < categories.size();a++){
//            if(categories.get(a).id == category.json.id){
//                index = a;
//                break;
//            }
//        }
//        if(index != -1){
//            categories.remove(category.json);
//            categoryHashMap.remove(category.json.id);
//            saveTabList();
//
//            MessagesController messagesController =  MessagesController.getInstance(UserConfig.selectedAccount);
//            ArrayList<TLRPC.Dialog> dialogs =  messagesController.dialogsByFolder.get(category.json.id);
//            if(dialogs != null && dialogs.size() > 0){
//                for(int a = 0; a < dialogs.size(); a++){
//                    TLRPC.Dialog dialog = dialogs.get(a);
//                    messagesController.addDialogToFolder(dialog.id, 0, 0, 0);
//                    Log.i("remoteingdialog","dialog remvoed witht id = " + dialog.id);
//                }
//            }
//
//            if(deleteRunnble != null){
//                deleteRunnble.run();
//            }
//        }
    }


}
