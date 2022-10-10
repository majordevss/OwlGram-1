package org.master.network;

import android.util.SparseArray;

import com.google.android.gms.common.api.Api;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;
import org.master.plus.games.GameCategory;
import org.master.plus.games.GameModel;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiManager extends BaseController {


    public interface ResponseCallback{
        void onResponse(Object response, APIError apiError);

        default void onResult( APIError apiError,Object... response){

        }

    }
    public static final String SERVER= "";
    public static final String DEBUG_SERVER= "";

    public static String getServer(){
     if(BuildConfig.DEBUG){
         return DEBUG_SERVER;
     }
     return SERVER;
    }

    private final ConcurrentHashMap<Integer, ArrayList<Integer>> requestsByGuids  = new ConcurrentHashMap<>();
    private final SparseArray<Integer> guidsByRequests  = new SparseArray<>();
    private ArrayList<RequestObject> requestQueue = new ArrayList<>();


    private void removeRequestFromGuid(int requestToken) {
        Integer guid = guidsByRequests.get(requestToken);
        if (guid != null) {
            ArrayList<Integer> reqIds = requestsByGuids.get(guid);
            if (reqIds != null) {
                int index = Collections.binarySearch(reqIds, requestToken);
                if (index != -1) {
                    reqIds.remove(index);
                    if (reqIds.isEmpty()) {
                        requestsByGuids.remove(guid);
                    }

                }
            }
            guidsByRequests.remove(requestToken);
        }
    }

    protected int currentAccount;
    private static volatile ApiManager[] Instance = new ApiManager[UserConfig.MAX_ACCOUNT_COUNT];
    private final ConcurrentHashMap<Integer, Call> callConcurrentHashMap = new ConcurrentHashMap<>();
    private final DispatchQueue servicesQueue = new DispatchQueue("servicesQueue");
    private final AtomicInteger lastRequestToken = new AtomicInteger(1);


    public static ApiManager getInstance(int num) {
        ApiManager  localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (ApiManager.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ApiManager(num);
                }
            }
        }
        return localInstance;
    }


    public void bindRequestToGuid(int reqId,int guid){
        ArrayList<Integer> arrayList =  requestsByGuids.get(guid);
        if(arrayList == null){
            arrayList   = new ArrayList<>();
            requestsByGuids.put(guid,arrayList);
        }
        arrayList.add(reqId);
        guidsByRequests.put(reqId,guid);
    }

    public void cancelRequestsForGuid(int guid){
        ArrayList<Integer> arrayList  = requestsByGuids.get(guid);
        if(arrayList != null){
            for (int a = 0; a < arrayList.size(); a++) {
                cancelRequestInternal(arrayList.get(a),false);
                Integer reqId = guidsByRequests.get(arrayList.get(a));
                if (reqId != null) {
                    guidsByRequests.remove(reqId);
                }
            }
            arrayList.clear();
        }
    }


    private boolean cancelRequestInternal(int reqId, boolean removeFromClass){
        for(int a = 0; a  < requestQueue.size();a++){
            RequestObject request =  requestQueue.get(a);
            if(reqId != 0 && request.requestToken == reqId){
                FileLog.d(String.format("cancelled queued rest request %s - header = %s",reqId, request.request().headers()));
                request.cancel();
                requestQueue.remove(request);
                if (removeFromClass) {
                    removeRequestFromGuid(reqId);
                }
                return true;
            }
        }
        return true;
    }
    private  Retrofit retrofit;
    public ApiManager(int num) {
        super(num);
        Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().serializeNulls().create();
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().
                baseUrl(getServer())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(getOkHttpClient());
        retrofit = retrofitBuilder.build();

      
    }
    
    public RestAPI getRestAPI(){
        if(retrofit == null) {
            return createRetro().create(RestAPI.class);
        }
        return retrofit.create(RestAPI.class);
    }

    private Retrofit createRetro(){
        Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().serializeNulls().create();
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().
                baseUrl(getServer())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(getOkHttpClient());
        return retrofitBuilder.build();
    }

    private OkHttpClient getOkHttpClient(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpClient.Builder okHttpClientBuilder = builder
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
        if(BuildConfig.DEBUG){
            HttpLoggingInterceptor loggingInterceptor= new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(loggingInterceptor);
        }
        return okHttpClientBuilder.build();
    }
    public void cancelRequest(int reqId){
        servicesQueue.postRunnable(() -> cancelInternal(reqId));
    }

    private void cancelInternal(int reqId){
        Call call =  callConcurrentHashMap.get(reqId);
        if(call != null && !call.isCanceled() && reqId != 0){
            call.cancel();
            callConcurrentHashMap.remove(reqId);
        }
    }

    public int getGamesForCategory(String cat,int offset,int limit,ResponseCallback callback) {
        int reqId = lastRequestToken.getAndIncrement();
        servicesQueue.postRunnable(() -> {
            APIError apiError = null;
            ArrayList<GameModel> gameModels = new ArrayList<>();
            try {
                Call<ResponseBody> call = getRestAPI().getGamesByCategory(cat,offset,limit);
                callConcurrentHashMap.put(reqId, call);
                Response<ResponseBody> response = call.execute();
                callConcurrentHashMap.remove(reqId);
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    JSONObject rawObject = new JSONObject(response.body().string());
                    JSONArray resultArray = rawObject.getJSONArray("results");
                    for (int a = 0; a < resultArray.length(); a++){
                        JSONObject jsonObject = resultArray.getJSONObject(a);
                        GameModel gameModel = gson.fromJson(jsonObject.toString(), GameModel.class);
                        gameModels.add(gameModel);
                    }
                } else {
                    callConcurrentHashMap.remove(reqId);
                    apiError = new APIError();
                    apiError.message = response.errorBody().string();
                }
            } catch (Exception e) {
                callConcurrentHashMap.remove(reqId);
                apiError = new APIError();
                apiError.message = e.getMessage();
            }

            APIError finalAPIError = apiError;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onResponse(gameModels, finalAPIError);
                }
            });
        });

        return reqId;
    }

    public int getGameCategories(ResponseCallback callback) {
        int reqId = lastRequestToken.getAndIncrement();
        servicesQueue.postRunnable(() -> {
            APIError apiError = null;
            ArrayList<GameCategory> gameModels = new ArrayList<>();
            try {
                Call<ResponseBody> call = getRestAPI().getGameCategory();
                callConcurrentHashMap.put(reqId, call);
                Response<ResponseBody> response = call.execute();
                callConcurrentHashMap.remove(reqId);
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    JSONObject rawObject = new JSONObject(response.body().string());
                    JSONArray resultArray = rawObject.getJSONArray("results");
                    for (int a = 0; a < resultArray.length(); a++){
                        JSONObject jsonObject = resultArray.getJSONObject(a);
                        GameCategory gameModel = gson.fromJson(jsonObject.toString(), GameCategory.class);
                        gameModel.local_id = a;
                        gameModels.add(gameModel);
                    }
                } else {
                    callConcurrentHashMap.remove(reqId);
                    apiError = new APIError();
                    apiError.message = response.errorBody().string();
                }
            } catch (Exception e) {
                callConcurrentHashMap.remove(reqId);
                apiError = new APIError();
                apiError.message = e.getMessage();
            }

            APIError finalAPIError = apiError;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onResponse(gameModels, finalAPIError);
                }
            });
        });

        return reqId;
    }

}
