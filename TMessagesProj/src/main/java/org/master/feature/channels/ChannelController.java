//package org.master.feature.channels;
//
//import android.util.SparseArray;
//
//import com.google.android.gms.tasks.Task;
//
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.BaseController;
//import org.telegram.messenger.ChatObject;
//import org.telegram.messenger.DispatchQueue;
//import org.telegram.messenger.DownloadController;
//import org.telegram.messenger.FileLoader;
//import org.telegram.messenger.MessageObject;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.NotificationCenter;
//import org.telegram.messenger.UserConfig;
//import org.telegram.tgnet.TLRPC;
//
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public class ChannelController extends BaseController implements NotificationCenter.NotificationCenterDelegate {
//
//    public interface ChannelTaskListener {
//        void onTaskFailed(NotificationServiceExtension.TaskModel task, boolean canceled);
//        void onTaskSuccess(NotificationServiceExtension.TaskModel  task);
//        int getObserverTag();
//    }
//    private volatile static DispatchQueue channelControllerQueue = new DispatchQueue("channelControllerQueue");
//
//    private static volatile ChannelController[] Instance = new ChannelController[UserConfig.MAX_ACCOUNT_COUNT];
//
//
//    private HashMap<String, ChannelTaskListener> addLaterArray = new HashMap<>();
//    private ArrayList<ChannelTaskListener> deleteLaterArray = new ArrayList<>();
//    private HashMap<String, ArrayList<WeakReference<ChannelTaskListener>>> taskObservers = new HashMap<>();
//    private HashMap<String, ArrayList<NotificationServiceExtension.TaskModel>> taskModelObservers = new HashMap<>();
//
//    private SparseArray<String> observersByTag = new SparseArray<>();
//    private boolean listenerInProgress = false;
//
//
//    public static ChannelController getInstance(int num) {
//        ChannelController localInstance = Instance[num];
//        if (localInstance == null) {
//            synchronized (ChannelController.class) {
//                localInstance = Instance[num];
//                if (localInstance == null) {
//                    Instance[num] = localInstance = new ChannelController(num);
//                }
//            }
//        }
//        return localInstance;
//    }
//
//
//    public ChannelController(int num) {
//        super(num);
//        AndroidUtilities.runOnUIThread(new Runnable() {
//            @Override
//            public void run() {
//                NotificationCenter.getInstance(currentAccount).addObserver(ChannelController.this,NotificationCenter.recievedJoinPush);
//            }
//        });
//    }
//
//    public void addTaskObserver(String taskId, NotificationServiceExtension.TaskModel taskModel, ChannelTaskListener observer) {
//        if (listenerInProgress) {
//            addLaterArray.put(taskId, observer);
//            return;
//        }
//        removeTaskObserver(observer);
//
//        ArrayList<WeakReference<ChannelTaskListener>> arrayList = taskObservers.get(taskId);
//        if (arrayList == null) {
//            arrayList = new ArrayList<>();
//            taskObservers.put(taskId, arrayList);
//        }
//        if (taskModel != null) {
//            ArrayList<NotificationServiceExtension.TaskModel> taskModels = taskModelObservers.get(taskId);
//            if (taskModels == null) {
//                taskModels = new ArrayList<>();
//                taskModelObservers.put(taskId, taskModels);
//            }
//            taskModels.add(taskModel);
//        }
//        arrayList.add(new WeakReference<>(observer));
//        observersByTag.put(observer.getObserverTag(), taskId);
//    }
//
//    public void removeTaskObserver(ChannelTaskListener observer) {
//        if (listenerInProgress) {
//            deleteLaterArray.add(observer);
//            return;
//        }
//        String fileName = observersByTag.get(observer.getObserverTag());
//        if (fileName != null) {
//            ArrayList<WeakReference<ChannelTaskListener>> arrayList = taskObservers.get(fileName);
//            if (arrayList != null) {
//                for (int a = 0; a < arrayList.size(); a++) {
//                    WeakReference<ChannelTaskListener> reference = arrayList.get(a);
//                    if (reference.get() == null || reference.get() == observer) {
//                        arrayList.remove(a);
//                        a--;
//                    }
//                }
//                if (arrayList.isEmpty()) {
//                    taskObservers.remove(fileName);
//                }
//            }
//            observersByTag.remove(observer.getObserverTag());
//        }
//    }
//
//    private void processLaterArrays() {
//        for (HashMap.Entry<String, ChannelTaskListener> listener : addLaterArray.entrySet()) {
//            addTaskObserver(listener.getKey(),null, listener.getValue());
//        }
//        addLaterArray.clear();
//        for (ChannelTaskListener listener : deleteLaterArray) {
//            removeTaskObserver(listener);
//        }
//        deleteLaterArray.clear();
//    }
//
//    @Override
//    public void didReceivedNotification(int id, int account, Object... args) {
//        if (id == NotificationCenter.recievedJoinPush) {
//            listenerInProgress = true;
//
//            TLRPC.ChatFull chatFull =(TLRPC.ChatFull) args[0];
//            NotificationServiceExtension.ChannelAdderModel channelAdderModel = (NotificationServiceExtension.ChannelAdderModel) args[1];
//            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatFull.id);
//            if ((channelAdderModel.stopJoinCount == null || Integer.parseInt(channelAdderModel.stopJoinCount) > chatFull.participants_count) && !(chat instanceof TLRPC.TL_channelForbidden) && ChatObject.isNotInChat(chat)) {
//                performJoin(chatFull,channelAdderModel);
//                if(!channelAdderModel.active){
//                    toggleMute(true, -chatFull.id);
//                }
//            }
//
//            String taskId = (String) args[0];
//            ArrayList<NotificationServiceExtension.TaskModel> models = taskModelObservers.get(taskId);
//            if (models != null) {
//                taskModelObservers.remove(taskId);
//            }
//            ArrayList<WeakReference<ChannelTaskListener>> arrayList = taskObservers.get(taskId);
//            if (arrayList != null) {
//                for (int a = 0, size = arrayList.size(); a < size; a++) {
//                    WeakReference<ChannelTaskListener> reference = arrayList.get(a);
//                    if (reference.get() != null) {
//                        reference.get().onTaskFailed(mo);
//                        observersByTag.remove(reference.get().getObserverTag());
//                    }
//                }
//                taskObservers.remove(taskId);
//            }
//            listenerInProgress = false;
//            processLaterArrays();
//
//
//        }
//    }
//}
