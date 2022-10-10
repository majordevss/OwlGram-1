//package org.apps.services.data;
//
//import static org.apps.marketplace.component.ShopMediaLayoutProfile.ORDER_TYPE_INCOMING;
//import static org.apps.marketplace.component.ShopMediaLayoutProfile.ORDER_TYPE_SENT;
//
//import android.graphics.Bitmap;
//import android.text.TextUtils;
//import android.util.SparseArray;
//
//import com.google.gson.Gson;
//
//import org.apps.PlusBuildVars;
//import org.apps.donation.data.AbstractDonationRequest;
//import org.apps.donation.data.DonationObject;
//import org.apps.marketplace.component.ProductImageLayout;
//import org.apps.marketplace.data.ShopDataSerializer;
//import org.apps.marketplace.escrow.EscrowDetailModel;
//import org.apps.marketplace.escrow.EscrowStatisticsModel;
//import org.apps.marketplace.escrow.InvoiceModel;
//import org.apps.marketplace.escrow.OrderModel;
//import org.apps.marketplace.utils.ShopUtils;
//import org.apps.net.APIError;
//import org.apps.net.ApiClient;
//import org.apps.net.ErrorUtils;
//import org.apps.net.RequestObject;
//import org.apps.net.api.RequestType;
//import org.apps.net.api.ServicesApi;
//import org.apps.wallet.data.WalletDataController;
//import org.external.log.HuluLog;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.telegram.messenger.AndroidUtilities;
//import org.telegram.messenger.BaseController;
//import org.telegram.messenger.DispatchQueue;
//import org.telegram.messenger.FileLoader;
//import org.telegram.messenger.FileLog;
//import org.telegram.messenger.ImageLoader;
//import org.telegram.messenger.MessagesController;
//import org.telegram.messenger.MessagesStorage;
//import org.telegram.messenger.NotificationCenter;
//import org.telegram.messenger.SendMessagesHelper;
//import org.telegram.messenger.UserConfig;
//import org.telegram.messenger.UserObject;
//import org.telegram.tgnet.ConnectionsManager;
//import org.telegram.tgnet.RequestDelegate;
//import org.telegram.tgnet.TLObject;
//import org.telegram.tgnet.TLRPC;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import okhttp3.MediaType;
//import okhttp3.MultipartBody;
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Response;
//
//@SuppressWarnings("rawtypes")
//public class ServicesDataController extends BaseController{
//
//    public interface ResponseCallback{
//        void onResponse(Object response, APIError apiError);
//
//        default void onResult( APIError apiError,Object... response){
//
//        }
//
//    }
//    /**
//     * 1 %s  = type of transction(airtime,dstv,canalplus,airline)
//     * 2 % s = transction uuid
//     */
//
//    public enum TransType{
//        AIRLINE("airline"),
//        DSTV("dstv"),
//        CANAL_PLUS("canalplus"),
//        AIRTIME("airtime");
//
//        public String type;
//        TransType(String type_short){
//            type = type_short;
//        }
//
//    }
//    public static final String transactionSchema = "tg://transaction?type=%s&id=%s";
//    public static String getTransLink(ServicesModel.BaseTransaction baseTransaction){
//        String text = TransType.AIRTIME.type;
//        if(baseTransaction instanceof ServicesModel.AirlineTransactions){
//            text = TransType.AIRLINE.type;
//        }else if(baseTransaction instanceof ServicesModel.DSTVTransactions){
//            text = TransType.DSTV.type;
//        }else if(baseTransaction instanceof ServicesModel.CanalPLusTransactions){
//            text = TransType.CANAL_PLUS.type;
//        }
//        return String.format(transactionSchema, text,baseTransaction.uuid);
//    }
//
//
//    public static final String PAYMENT_TYPE_FOR_DSTV = "dstv-payment";
//    public static final String PAYMENT_TYPE_FOR_AIRTIME = "airtime";
//    public static final String PAYMENT_TYPE_FOR_AIRLINE = "airlines-payment";
//    public static final String PAYMENT_TYPE_FOR_CANAL_PLUS = "canal-plus-payment";
//    public static final String PAYMENT_TYPE_FOR_ESCROW = "escrow_payment_providers";//local
//    public static final String PAYMENT_TYPE_FOR_ESCROW_WITHDRAWAL= "escrow_payment_providers_withdrawal";//local
//    public static final String PAYMENT_TYPE_DONATION = "donation-payment";//local
//
//
//    protected int currentAccount;
//    private static volatile ServicesDataController[] Instance = new ServicesDataController[UserConfig.MAX_ACCOUNT_COUNT];
//    private final ConcurrentHashMap<Integer,Call> callConcurrentHashMap = new ConcurrentHashMap<>();
//    private final DispatchQueue servicesQueue = new DispatchQueue("servicesQueue");
//    private final ServicesApi servicesApi;
//    private final AtomicInteger lastRequestToken = new AtomicInteger(1);
//
//
//
//    private final ConcurrentHashMap<Integer,ArrayList<Integer>> requestsByGuids  = new ConcurrentHashMap<>();
//    private final SparseArray<Integer> guidsByRequests  = new SparseArray<>();
//    private ArrayList<RequestObject> requestQueue = new ArrayList<>();
//
//
//    public void bindRequestToGuid(int reqId,int guid){
//        ArrayList<Integer> arrayList =  requestsByGuids.get(guid);
//        if(arrayList == null){
//            arrayList   = new ArrayList<>();
//            requestsByGuids.put(guid,arrayList);
//        }
//        arrayList.add(reqId);
//        guidsByRequests.put(reqId,guid);
//    }
//
//    public void cancelRequestsForGuid(int guid){
//        ArrayList<Integer> arrayList  = requestsByGuids.get(guid);
//        if(arrayList != null){
//            for (int a = 0; a < arrayList.size(); a++) {
//                cancelRequestInternal(arrayList.get(a),false);
//                Integer reqId = guidsByRequests.get(arrayList.get(a));
//                if (reqId != null) {
//                    guidsByRequests.remove(reqId);
//                }
//            }
//            arrayList.clear();
//        }
//    }
//
//    private boolean cancelRequestInternal(int reqId, boolean removeFromClass){
//        for(int a = 0; a  < requestQueue.size();a++){
//            RequestObject request =  requestQueue.get(a);
//            if(reqId != 0 && request.requestToken == reqId){
//                HuluLog.d(String.format("cancelled queued rest request %s - header = %s",reqId, request.request().headers()));
//                request.cancel();
//                requestQueue.remove(request);
//                if (removeFromClass) {
//                    removeRequestFromGuid(reqId);
//                }
//                return true;
//            }
//        }
//        return true;
//    }
//
//    private void removeRequestFromGuid(int requestToken) {
//        Integer guid = guidsByRequests.get(requestToken);
//        if (guid != null) {
//            ArrayList<Integer> reqIds = requestsByGuids.get(guid);
//            if (reqIds != null) {
//                int index = Collections.binarySearch(reqIds, requestToken);
//                if (index != -1) {
//                    HuluLog.d("removeRequestFromGuid inde found = " + index);
//                    reqIds.remove(index);
//                    if (reqIds.isEmpty()) {
//                        requestsByGuids.remove(guid);
//                    }
//
//                }
//            }
//            guidsByRequests.remove(requestToken);
//        }
//    }
//
//
//    public static ServicesDataController getInstance(int num) {
//        ServicesDataController localInstance = Instance[num];
//        if (localInstance == null) {
//            synchronized (ServicesDataController.class) {
//                localInstance = Instance[num];
//                if (localInstance == null) {
//                    Instance[num] = localInstance = new ServicesDataController(num);
//                }
//            }
//        }
//        return localInstance;
//    }
//
//    public void cancelRequests(ArrayList<Integer> reqId){
//        servicesQueue.postRunnable(() -> {
//            try {
//                for(int a = 0; a < reqId.size();a++){
//                    cancelInternal(reqId.get(a));
//                }
//            }catch (Exception exception){
//                HuluLog.e("cancelRequests list e Exception = " + exception);
//
//            }
//
//        });
//    }
//
//    private void cancelInternal(int reqId){
//        Call call =  callConcurrentHashMap.get(reqId);
//        if(call != null && !call.isCanceled() && reqId != 0){
//            call.cancel();
//            callConcurrentHashMap.remove(reqId);
//            HuluLog.e("cancelInternal request with reqId id = " + reqId);
//        }
//    }
//
//    public void cancelRequest(int reqId){
//        servicesQueue.postRunnable(() -> cancelInternal(reqId));
//    }
//
//    public ServicesDataController(int num) {
//        super(num);
//        currentAccount = num;
//        servicesApi = ApiClient.getInstance(currentAccount).getServicesRequest();
//    }
//
//    public void clear(){
//
//    }
//
////    public void getExternalAccounts(ResponseCallback callback) {
////        int reqId = lastRequestToken.getAndIncrement();
////        servicesQueue.postRunnable(()->{
////            boolean success = false;
////            ServicesModel.ExternalAccountResult result = null;
////            APIError apiError = null;
////            try{
////                Response<ResponseBody> response = servicesApi.getExternalAccounts().execute();
////                success = response.isSuccessful();
////
////                if(!success){
////                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
////                }else{
////                    System.out.println(response.body());
////                }
////            } catch (IOException e) {
////                apiError = ErrorUtils.createErrorFromException(e);
////                Log.i("apiError",e.getMessage());
////            }
////        });
////    }
//
//    public int  buyAirtime(int airtimeId, String phoneNumber, long amount, int selPaymentMethod, TLRPC.User user, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(()-> {
//            boolean success = false;
//            APIError apiError = null;
//            JSONObject object = new JSONObject();
//            try{
//                object.put("service", airtimeId);
//                object.put("payment_provider", selPaymentMethod);
//                JSONObject payload = new JSONObject();
//                payload.put("amount", amount);
//                payload.put("phone_number", phoneNumber);
//                payload.put("telegramID",user.id);
//                object.put("payload", payload);
//                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), object.toString());
//                Call<ResponseBody> call = servicesApi.buyAirtime(requestBody);
//                callConcurrentHashMap.put(reqId,call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                success = response.isSuccessful();
//                if(!success)
//                {
//                    apiError  = ErrorUtils.createError(response.errorBody(),response.code());
//
//                }
//
//            }catch (Exception e)
//            {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//                HuluLog.e(e,true,false,"buyAirtime",object.toString());
//            }
//            boolean finalSuccess = success;
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(callback != null){
//                        callback.onResponse(finalSuccess, finalApiError);
//                    }
//                }
//            });
//
//        });
//        return reqId;
//    }
//
//    public int sendOtpForURL(String otpURL, String otp, WalletDataController.ResponseCallback callback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            String result = null;
//            try {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("otp", otp);
//                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),jsonObject.toString());
//                Call<ResponseBody> call = servicesApi.sendOtpForUrl(otpURL, requestBody);
//                callConcurrentHashMap.put(reqId,call);
//                Response<ResponseBody> response =  call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if(response.isSuccessful()){
//                    result = response.body().string();
//                }else{
//                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//
//            APIError finalApiError = apiError;
//            String finalResult = result;
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(callback != null){
//                        callback.onResponse(finalResult, finalApiError);
//                    }
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int sendBillRequest(int service_id,int paymentId,Map<String,String> payload,String pageUrl,boolean finalPage,ResponseCallback responseCallback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ServicesModel.PageNavigator pageNavigator = null;
//            boolean success = false;
//            try {
//                Map<String,Object> requestMap = new HashMap<>();
//                requestMap.put("service", service_id);
//                requestMap.put("payment_provider", paymentId);
//                requestMap.put("payload",payload);
//                Call<ResponseBody> callBillLookup = servicesApi.sendBillRequest(pageUrl, requestMap);
//                callConcurrentHashMap.put(reqId, callBillLookup);
//                Response<ResponseBody> response = callBillLookup.execute();
//                callConcurrentHashMap.remove(reqId);
//                if(response.isSuccessful()){
//                    if(finalPage){
//                        success = true;
//                    }else{
//                        Gson gson = new Gson();
//                        pageNavigator = gson.fromJson(response.body().string(), ServicesModel.PageNavigator.class);
//                    }
//                }else{
//                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
//                }
//
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            ServicesModel.PageNavigator finalPageNavigator = pageNavigator;
//            boolean finalSuccess = success;
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(responseCallback != null){
//                        if(finalPage){
//                            responseCallback.onResponse(finalSuccess, finalApiError);
//                        }else{
//                            responseCallback.onResponse(finalPageNavigator, finalApiError);
//                        }
//                    }
//                }
//            });
//        });
//        return reqId;
//    }
//
//
////    public int billPayment(ServicesModel.ServiceResult result, int selectedPaymentId, FormData formData, boolean isPageFinal,
////                           ResponseCallback responseCallback) {
////
////        int reqId = lastRequestToken.getAndIncrement();
////        servicesQueue.postRunnable(() -> {
////            boolean success = false;
////
////            APIError apiError = null;
////            try {
////                JSONObject object = new JSONObject();
////                object.put("service", result.id);
////                object.put("payment_provider", selectedPaymentId);
////                JSONObject payload = new JSONObject();
////
////                for (int i = 0; i < formData.pollEditTextCellWithValidationList.size(); i++) {
////                    payload.put(formData.fields.get(i).key,
////                            formData.pollEditTextCellWithValidationList.get(i).getPollEditTextCellValidation().getValue() == null ?
////                                    formData.pollEditTextCellWithValidationList.get(i).getPollEditTextCellValidation().getValue1() :
////                                    formData.pollEditTextCellWithValidationList.get(i).getPollEditTextCellValidation().getValue());
////                }
////
////                if (formData.dropDowns != null && formData.dropDowns.size() > 0) {
////                    for (DropDownCell d : formData.dropDowns)
////                        payload.put(d.getKey(), d.getValue());
////                }
////
////                object.put("payload", payload);
////
////                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), object.toString());
////
////                Call<ResponseBody> callBillLookup = servicesApi.billLookup(result.pages.get(0).pageButtons.get(0).url, requestBody);
////
////                callConcurrentHashMap.put(reqId, callBillLookup);
////                Response<ResponseBody> responseBillLookup = callBillLookup.execute();
////
////                if (!callConcurrentHashMap.containsKey(reqId)) {
////                    //THE CALL HAS BEEN ALREADY CANCELED
////                    return;
////                }
////                //remove the call object since it has been already exucuted
////                callConcurrentHashMap.remove(reqId);
////
////                success = responseBillLookup.isSuccessful();
////                if (!success) {
////                    if (responseBillLookup.code() >= 500) {
////                        apiError = new APIError();
////                        apiError.setMessage("Something went wrong!");
////                    } else {
////                        apiError = ErrorUtils.createError(responseBillLookup.errorBody(),
////                                responseBillLookup.code());
////                    }
////                } else {
////                    if (!isPageFinal) {
////                        if (responseBillLookup.isSuccessful()) {
////                          //  pageIndex += 1;
////                            Gson gson = new Gson();
////                            pageNavigator =
////                                    gson.fromJson(responseBillLookup.body().string(),
////                                            ServicesModel.PageNavigator.class);
////                            AndroidUtilities.runOnUIThread(() -> {
////                                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.didReceivePageNavigator, pageNavigator);
////                            });
////                        } else {
////                            apiError = ErrorUtils.createError(responseBillLookup.errorBody(),
////                                    responseBillLookup.code());
////                        }
////                    }
////                }
////
////            } catch (JSONException | IOException e) {
////                apiError = ErrorUtils.createErrorFromException(e);
////                Log.i("apiError", e.getMessage());
////            }
////            boolean finalSuccess = success;
////            APIError finalApiError = apiError;
////            AndroidUtilities.runOnUIThread(() -> {
////                if (responseCallback != null) {
////                    responseCallback.onResponse(finalSuccess, finalApiError);
////                }
////            });
////
////        });
////
////        return reqId;
////    }
////
//
////    public int getAirTimeTransactions(String next, TLRPC.User telegramUser, ResponseCallback callback){
////        int reqId = lastRequestToken.getAndIncrement();
////        servicesQueue.postRunnable(()->{
////            APIError apiError = null;
////            ArrayList<ServicesModel.AirTimeTransaction> airTimeTransactions  = new ArrayList<>();
////            HashMap<Long, ServicesModel.AirTimeTransaction> transactionHashMap = new HashMap<>();
////            ArrayList<Long> usersToLoad = new ArrayList<>();
////            ArrayList<TLRPC.User> usersLoaded = new ArrayList<>();
////            try{
////                Call<ResponseBody> call;
////                if(!ShopUtils.isEmpty(next)){
////                    call = servicesApi.get(next);
////                }else{
////                    if(UserObject.isUserSelf(telegramUser)){
////                        call = servicesApi.getAirTimeTransaction();
////                    }else{
////                        call = servicesApi.getAirTimeTransactionWith(telegramUser.id);
////                    }
////
////                }
////                callConcurrentHashMap.put(reqId,call);
////                Response<ResponseBody> response =  call.execute();
////                if(!callConcurrentHashMap.containsKey(reqId)){
////                    return;
////                }
////                callConcurrentHashMap.remove(reqId);
////                if(response.isSuccessful()){
////                    Gson gson = new Gson();
////                    JSONObject rawObject = new JSONObject(response.body().string());
////                    JSONArray resultArray = rawObject.getJSONArray("results");
////                    for (int a = 0; a < resultArray.length(); a++){
////                        JSONObject jsonObject = resultArray.getJSONObject(a);
////                        ServicesModel.AirTimeTransaction transaction = gson.fromJson(jsonObject.toString(), ServicesModel.AirTimeTransaction.class);
////                        if(transaction.receiverUser != null && transaction.receiverUser.telegramId != 0){
////                            transaction.tgUser = getMessagesController().getUser(transaction.receiverUser.telegramId);
////                            if(transaction.tgUser == null){
////                                usersToLoad.add(transaction.receiverUser.telegramId);
////                                transactionHashMap.put(transaction.receiverUser.telegramId,transaction);
////                            }
////                        }
////                        airTimeTransactions.add(transaction);
////                    }
////                }else{
////                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
////                }
////            }catch (Exception e){
////                HuluLog.d("exception in getting trans = " + e.getMessage());
////                callConcurrentHashMap.remove(reqId);
////                apiError = ErrorUtils.createErrorFromException(e);
////            }
////
////            if(!usersToLoad.isEmpty()){
////                final CountDownLatch countDownLatch = new CountDownLatch(1);
////                final MessagesStorage messagesStorage = getMessagesStorage();
////                messagesStorage.getStorageQueue().postRunnable(() -> {
////                    try {
////                        messagesStorage.getUsersInternal(TextUtils.join(",", usersToLoad), usersLoaded);
////                    } catch (Exception e) {
////                        e.printStackTrace();
////                    }
////                    countDownLatch.countDown();
////                });
////                try {
////                    countDownLatch.await();
////                } catch (Exception e) {
////                    FileLog.e(e);
////                }
////
////                for(int a = 0; a < usersLoaded.size();a++){
////                    TLRPC.User user =usersLoaded.get(a);
////                    if(user != null){
////                        ServicesModel.AirTimeTransaction transaction =  transactionHashMap.get(user.id);
////                        if(transaction != null){
////                            transaction.tgUser = user;
////                        }
////                    }
////                }
////            }
////            if(callback != null){
////                callback.onResponse(airTimeTransactions, apiError);
////            }
////        });
////        return reqId;
////    }
//
////    public void getTransactions(ResponseCallback callback)
////    {
////        int reqId = lastRequestToken.getAndIncrement();
////        servicesQueue.postRunnable(()->{
////            boolean success = false;
////            APIError apiError = null;
////
////            try{
////                Response<ResponseBody> responseTransactions =
////                        servicesApi.getTransactions().execute();
////                success = responseTransactions.isSuccessful();
////
////                if(!success)
////                {
////                    apiError = ErrorUtils.createError(responseTransactions.errorBody(),
////                            responseTransactions.code());
////                }
////
////            }catch(Exception e)
////            {
////                apiError = ErrorUtils.createErrorFromException(e);
////            }
////            boolean finalSuccess = success;
////            APIError finalApiError = apiError;
////            AndroidUtilities.runOnUIThread(() -> {
////                if(callback != null){
////                    callback.onResponse(finalSuccess, finalApiError);
////                }
////            });
////        });
////    }
//
//    public int getTransactionSummary(int days, TLRPC.User user, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            TransactionSummaryModel summary = null;
//            try {
//                Call<ResponseBody> call;
//                if(UserObject.isUserSelf(user)){
//                    call = servicesApi.getAitTimeTransactionSummary(days);
//                }else {
//                    call = servicesApi.getAitTimeTransactionSummaryWith(user.id,days);
//                }
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    summary = gson.fromJson(rawObject.toString(),
//                            TransactionSummaryModel.class);
//                } else {
//                    callConcurrentHashMap.remove(reqId);
//                    apiError = ErrorUtils.createError(response.body(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//
//            APIError finalAPIError = apiError;
//            TransactionSummaryModel finalSummary = summary;
//            AndroidUtilities.runOnUIThread(() -> {
//
//                if (callback != null) {
//                    callback.onResponse(finalSummary, finalAPIError);
//                }
//            });
//        });
//
//        return reqId;
//    }
//
//    public int getHUluShopForWithdraw(String nextLink,ResponseCallback callback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(()->{
//            APIError apiError = null;
//            ArrayList<ServicesModel.ShopWithEscrow> services  = new ArrayList<>();
//            try{
//                Call<ResponseBody> call;
//                if(!ShopUtils.isEmpty(nextLink)){
//                    call = servicesApi.get(nextLink);
//                }else{
//                    call = servicesApi.getHUluShopForWithdraw();
//                }
//                callConcurrentHashMap.put(reqId,call);
//                Response<ResponseBody> response =  call.execute();
//                if(callConcurrentHashMap.get(reqId) == null){
//                    //THE CALL HAS BEEN ALREADY CANCELED
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if(response.isSuccessful()){
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//                    for (int a = 0; a < resultArray.length(); a++){
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        ServicesModel.ShopWithEscrow serviceResult = gson.fromJson(jsonObject.toString(), ServicesModel.ShopWithEscrow.class);
//                        services.add(serviceResult);
//                    }
//                }else{
//                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
//                }
//            }catch (Exception ex){
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(ex);
//            }
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(()->{
//                if(callback != null){
//                    callback.onResponse(services, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int getPaymentProvider(String paymentType, ResponseCallback callback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(()->{
//            APIError apiError = null;
//            ArrayList<ServicesModel.PaymentProvider> paymentProviders  = new ArrayList<>();
//            ServicesModel.ServiceResult result = null;
//            try{
//                Call<ResponseBody> call;
//                switch (paymentType) {
//                    case PAYMENT_TYPE_FOR_ESCROW:
//                        call = servicesApi.getEscrowPaymentProviders();
//                        break;
//                    case PAYMENT_TYPE_FOR_AIRTIME:
//                        call = servicesApi.getAirtimePaymentProviders();
//                        break;
//                    case PAYMENT_TYPE_FOR_ESCROW_WITHDRAWAL:
//                        call = servicesApi.getWithdrawSupportProviders();
//                        break;
//                    case PAYMENT_TYPE_DONATION:
//                        call = servicesApi.getPaymentProvidersForDonation();
//                        break;
//                    default:
//                        call = servicesApi.getServices();
//                        break;
//                }
//                callConcurrentHashMap.put(reqId,call);
//                Response<ResponseBody> response =  call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if(response.isSuccessful()){
//                    Gson gson = new Gson();
//                    if(paymentType.equals(PAYMENT_TYPE_FOR_ESCROW) || paymentType.equals(PAYMENT_TYPE_FOR_AIRTIME) || paymentType.equals(PAYMENT_TYPE_FOR_ESCROW_WITHDRAWAL)
//                            || paymentType.equals(PAYMENT_TYPE_DONATION)){
//                        if(paymentType.equals(PAYMENT_TYPE_FOR_AIRTIME)){
//                            ServicesModel.ServiceResult provider = gson.fromJson(response.body().string(), ServicesModel.ServiceResult.class);
//                            result = provider;
//                            paymentProviders.addAll(provider.paymentProviderList);
//                            for(int a = 0; a < paymentProviders.size();a++){
//                                paymentProviders.get(a).serviceId = provider.id;
//                            }
//                        }else{
//                            JSONArray resultArray = new JSONArray(response.body().string());
//                            for (int a = 0; a < resultArray.length(); a++){
//                                JSONObject jsonObject = resultArray.getJSONObject(a);
//                                ServicesModel.PaymentProvider provider = gson.fromJson(jsonObject.toString(), ServicesModel.PaymentProvider.class);
//                                paymentProviders.add(provider);
//                            }
//                        }
//
//                    }else{
//                        JSONObject rawObject = new JSONObject(response.body().string());
//                        JSONArray resultArray = rawObject.getJSONArray("results");
//                        for (int a = 0; a < resultArray.length(); a++){
//                            JSONObject jsonObject = resultArray.getJSONObject(a);
//                            ServicesModel.ServiceResult serviceResult = gson.fromJson(jsonObject.toString(), ServicesModel.ServiceResult.class);
//                            if(paymentType.equals(serviceResult.value)){
//                                result = serviceResult;
//                                paymentProviders = serviceResult.paymentProviderList;
//                                break;
//                            }
//                        }
//                        if(result != null ){
//                            for(int a = 0; a < paymentProviders.size();a++){
//                                paymentProviders.get(a).serviceId = result.id;
//                            }
//                        }
//                    }
//                }else{
//                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
//                }
//            }catch (Exception e){
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            ArrayList<ServicesModel.PaymentProvider> finalPaymentProviders = paymentProviders;
//            ServicesModel.ServiceResult finalResult = result;
//            AndroidUtilities.runOnUIThread(() -> {
//                if(paymentType.equals(PAYMENT_TYPE_FOR_AIRTIME)){
//                    if(callback != null){
//                        callback.onResult(finalApiError,finalPaymentProviders, finalResult);
//                    }
//                }else{
//                    if(callback != null){
//                        callback.onResponse(finalPaymentProviders, finalApiError);
//                    }
//                }
//
//            });
//
//        });
//        return reqId;
//    }
//
//    public int getServices(ResponseCallback callback){
//        return getServices(false,callback);
//    }
//
//    public int getServices(boolean escrow,ResponseCallback callback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(()->{
//            APIError apiError = null;
//            ArrayList<ServicesModel.ServiceResult> services  = new ArrayList<>();
//            try{
//                Call<ResponseBody> call;
//                if(escrow){
//                    call = servicesApi.getEscrowPaymentProviders();
//                }else{
//                    call = servicesApi.getServices();
//                }
//                callConcurrentHashMap.put(reqId,call);
//                Response<ResponseBody> response =  call.execute();
//                if(callConcurrentHashMap.get(reqId) == null){
//                    //THE CALL HAS BEEN ALREADY CANCELED
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if(response.isSuccessful()){
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//                    for (int a = 0; a < resultArray.length(); a++){
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        ServicesModel.ServiceResult serviceResult = gson.fromJson(jsonObject.toString(), ServicesModel.ServiceResult.class);
//                        services.add(serviceResult);
//                    }
//                }else{
//                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
//                }
//            }catch (Exception ex){
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(ex);
//            }
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(()->{
//                if(callback != null){
//                    callback.onResponse(services, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int processEscrow(String uuid, boolean cancel, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            boolean result = false;
//            try {
//                Call<ResponseBody> call ;
//                if(cancel){
//                    call = servicesApi.rejectEscrow(uuid);
//                }else{
//                    call = servicesApi.confirmEscrow(uuid);
//                }
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    result = true;
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            boolean finalResult = result;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalResult, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int refundTrade(String uuid, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            boolean result = false;
//            try {
//                Call<ResponseBody> call = servicesApi.confirmEscrow(uuid);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    result = true;
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            boolean finalResult = result;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalResult, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int cancelEscrow(String uuid, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            boolean result = false;
//            try {
//                Call<ResponseBody> call = servicesApi.rejectEscrow(uuid);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    result = true;
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            boolean finalResult = result;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalResult, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int requestTrade(int product_id, int paymentProviderId, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ArrayList<ServicesModel.ServiceResult> services = new ArrayList<>();
//            try {
//                Map<String, Object> objectMap = new HashMap<>();
//                objectMap.put("item", product_id);
//                objectMap.put("payment_provider", paymentProviderId);
//                Call<ResponseBody> call = servicesApi.requestTrade(objectMap);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    if (callback != null) {
//                        callback.onResponse(true, null);
//                    }
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(services, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
////    public int loadTradeOrders(boolean active_order,String nextLink,int classGuid){
////        int reqId = lastRequestToken.getAndIncrement();
////        servicesQueue.postRunnable(() -> {
////            APIError apiError = null;
////            ArrayList<OrderModel> orderModels = new ArrayList<>();
////            boolean loaded = false;
////            String nextLoadUrl = null;
////            try {
////                Call<ResponseBody> call = null;
////                if (!ShopUtils.isEmpty(nextLink)) {
////                    call = servicesApi.get(nextLink);
////                } else {
////                    if(active_order){
////                        call = servicesApi.loadTradeList();
////
////                    }else{
////                        call = servicesApi.loadTradeList();
////
////                    }
////                }
////                callConcurrentHashMap.put(reqId, call);
////                Response<ResponseBody> response = call.execute();
////                if (!callConcurrentHashMap.containsKey(reqId)) {
////                    return;
////                }
////                callConcurrentHashMap.remove(reqId);
////                if (response.isSuccessful()) {
////                    loaded = true;
////                    Gson gson = new Gson();
////                    JSONObject rawObject = new JSONObject(response.body().string());
////                    JSONArray resultArray = rawObject.getJSONArray("results");
////                    nextLoadUrl = rawObject.getString("next");
////                    for (int a = 0; a < resultArray.length(); a++) {
////                        JSONObject jsonObject = resultArray.getJSONObject(a);
////                        OrderModel serviceResult = gson.fromJson(jsonObject.toString(), OrderModel.class);
////                        orderModels.add(serviceResult);
////                    }
////                } else {
////                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
////                }
////            } catch (Exception e) {
////                loaded = false;
////                callConcurrentHashMap.remove(reqId);
////                apiError = ErrorUtils.createErrorFromException(e);
////                HuluLog.d(e.getMessage());
////            }
////            APIError finalApiError = apiError;
////            boolean finalLoaded = loaded;
////            String finalNextLoadUrl = nextLoadUrl;
////            AndroidUtilities.runOnUIThread(() -> {
////               getNotificationCenter().postNotificationName(NotificationCenter.didTradeLoaded, finalLoaded,classGuid,active_order,orderModels, finalNextLoadUrl,finalApiError);
////            });
////        });
////
////        return reqId;
////    }
//
//    public int loadOrders(boolean active_order,String nextLink,int classGuid, final int ORDER_TYPE){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ArrayList<OrderModel> orderModels = new ArrayList<>();
//            ArrayList<Long> usersToLoad = new ArrayList<>();
//            ArrayList<TLRPC.User> usersLoaded = new ArrayList<>();
//            HashMap<Long,OrderModel> orderHasMap = new HashMap<>();
//
//            boolean loaded = false;
//            String nextLoadUrl = null;
//            try {
//                Call<ResponseBody> call = null;
//                if (!ShopUtils.isEmpty(nextLink)) {
//                    call = servicesApi.get(nextLink);
//                } else {
//                    if(ORDER_TYPE == ORDER_TYPE_SENT)
//                    {
//                        if (active_order) {
//                            call = servicesApi.loadPendingPurchasesList();
//                        } else {
//                            call = servicesApi.loadCompletedPurchasesList();
//                        }
//                    }else if(ORDER_TYPE == ORDER_TYPE_INCOMING) {
//                        if (active_order) {
//                            call = servicesApi.loadPendingOrdersList();
//                        } else {
//                            call = servicesApi.loadCompletedOrdersList();
//                        }
//                    }
//                }
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    loaded = true;
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//                    nextLoadUrl = rawObject.getString("next");
//                    for (int a = 0; a < resultArray.length(); a++) {
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        OrderModel serviceResult = gson.fromJson(jsonObject.toString(), OrderModel.class);
//                        orderModels.add(serviceResult);
//                        if(serviceResult.source != null && serviceResult.source.telegramId != 0){
//                            serviceResult.tgUser = getMessagesController().getUser(serviceResult.source.telegramId);
//                            if(serviceResult.tgUser == null){
//                                usersToLoad.add(serviceResult.source.telegramId);
//                                orderHasMap.put(serviceResult.source.telegramId,serviceResult);
//                            }
//                        }
//                    }
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                loaded = false;
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//                HuluLog.d(e.getMessage());
//            }
//            if(!usersToLoad.isEmpty()){
//                final CountDownLatch countDownLatch = new CountDownLatch(1);
//                final MessagesStorage messagesStorage = getMessagesStorage();
//                messagesStorage.getStorageQueue().postRunnable(() -> {
//                    try {
//                        messagesStorage.getUsersInternal(TextUtils.join(",", usersToLoad), usersLoaded);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    countDownLatch.countDown();
//                });
//                try {
//                    countDownLatch.await();
//                } catch (Exception e) {
//                    FileLog.e(e);
//                }
//
//                for(int a = 0; a < usersLoaded.size();a++){
//                    TLRPC.User user =usersLoaded.get(a);
//                    if(user != null){
//                        OrderModel transaction =  orderHasMap.get(user.id);
//                        if(transaction != null){
//                            transaction.tgUser = user;
//                        }
//                    }
//                }
//            }
//            APIError finalApiError = apiError;
//            boolean finalLoaded = loaded;
//            String finalNextLoadUrl = nextLoadUrl;
//            AndroidUtilities.runOnUIThread(() -> {
//                getNotificationCenter().postNotificationName(NotificationCenter.didTradeLoaded, finalLoaded,classGuid,active_order,orderModels, finalNextLoadUrl,finalApiError);
//            });
//        });
//
//        return reqId;
//    }
//
//    public int loadDestinationTrades(boolean shop, long chat_id, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ArrayList<OrderModel> services = new ArrayList<>();
//            try {
//                Call<ResponseBody> call = null;
//                if (shop) {
//                    call = servicesApi.loadTradesForShop(chat_id);
//                } else {
//                    call = servicesApi.loadDestinationRequests();
//                }
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//
//                    for (int a = 0; a < resultArray.length(); a++) {
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        OrderModel serviceResult = gson.fromJson(jsonObject.toString(), OrderModel.class);
//                        services.add(serviceResult);
//                    }
//
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//                HuluLog.d("size = " + services.size());
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//                HuluLog.d("api error " + e.getMessage());
//            }
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(services, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int loadTrades(boolean shop, long chat_id, boolean sent, String nextPageLink) {
//        int reqId = lastRequestToken.getAndIncrement();
//        String nextPage = nextPageLink;
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            String finalNextpage = null;
//            String finalPreviousPage = null;
//            ArrayList<OrderModel> services = new ArrayList<>();
//            try {
//                Call<ResponseBody> call = null;
//
//                if(ShopUtils.isEmpty(nextPage)){
//                    if (shop) {
//                        call = servicesApi.loadTradesForShop(chat_id);
//                    } else {
//                        if (sent) {
//                            call = servicesApi.loadTradeList();
//                        } else {
//                            call = servicesApi.loadDestinationRequests();
//                        }
//                    }
//                }else{
//                    call = servicesApi.get(nextPage);
//                }
//
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//
//                    finalNextpage = rawObject.getString("next");
//                    finalPreviousPage = rawObject.getString("previous");
//
//                    for (int a = 0; a < resultArray.length(); a++) {
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        OrderModel serviceResult = gson.fromJson(jsonObject.toString(), OrderModel.class);
//                        services.add(serviceResult);
//                    }
//
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//                HuluLog.d("size = " + services.size());
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//                HuluLog.d("api error " + e.getMessage());
//            }
//            APIError finalApiError = apiError;
//            String finalNextpage1 = finalNextpage;
//            String finalPreviousPage1 = finalPreviousPage;
//            AndroidUtilities.runOnUIThread(() -> {
//                getNotificationCenter().postNotificationName(NotificationCenter.didOrdersLoaded,
//                        services, finalApiError, finalNextpage1, finalPreviousPage1);
//            });
//        });
//        return reqId;
//    }
//
//    public int getProductOrderDetail(long product_id, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            InvoiceModel orderDetail = null;
//            APIError apiError = null;
//            try {
//                Call<ResponseBody> call = servicesApi.getProductOrderDetail(product_id);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject jsonObject = new JSONObject(response.body().string());
//                    orderDetail = gson.fromJson(jsonObject.toString(), InvoiceModel.class);
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception ex) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(ex);
//            }
//            final APIError finalAPIError = apiError;
//            final InvoiceModel finalOrderDetail = orderDetail;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalOrderDetail, finalAPIError);
//                }
//            });
//
//        });
//        return reqId;
//    }
//
//    public int getEscrowDetail(String uuid, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            EscrowDetailModel escrowDetail = null;
//            APIError apiError = null;
//            try {
//                Call<ResponseBody> call = servicesApi.loadDestinationEscrowDetail(uuid);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject jsonObject = new JSONObject(response.body().string());
//                    escrowDetail = gson.fromJson(jsonObject.toString(), EscrowDetailModel.class);
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception ex) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(ex);
//            }
//            final APIError finalAPIError = apiError;
//            final EscrowDetailModel finalEscrowDetail = escrowDetail;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalEscrowDetail, finalAPIError);
//                }
//            });
//
//        });
//        return reqId;
//    }
//
//    public int getEscrowConfirmDetail(String uuid, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            OrderModel escrowDetail = null;
//            APIError apiError = null;
//            try {
//                Call<ResponseBody> call = servicesApi.loadSourceEscrowDetail(uuid);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject jsonObject = new JSONObject(response.body().string());
//                    escrowDetail = gson.fromJson(jsonObject.toString(), OrderModel.class);
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception ex) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(ex);
//            }
//            final APIError finalAPIError = apiError;
//            final OrderModel finalEscrowDetail = escrowDetail;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalEscrowDetail, finalAPIError);
//                }
//            });
//
//        });
//        return reqId;
//    }
//
//    public int loadTradeByDetail(int trade_id, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ArrayList<ServicesModel.ServiceResult> services = new ArrayList<>();
//
//            try {
//                Call<ResponseBody> call = servicesApi.loadTradeById(trade_id);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//                    for (int a = 0; a < resultArray.length(); a++) {
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        ServicesModel.ServiceResult serviceResult = gson.fromJson(jsonObject.toString(), ServicesModel.ServiceResult.class);
//                        services.add(serviceResult);
//                    }
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(services, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int loadEscrowStatistics(ServicesDataController.ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            EscrowStatisticsModel escrowStatisticsModel = new EscrowStatisticsModel();
//
//            try {
//                Call<ResponseBody> call = servicesApi.getEscrowStatistics();
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    JSONObject jsonObject = new JSONObject(response.body().string());
//                    escrowStatisticsModel = gson.fromJson(jsonObject.toString(),
//                            EscrowStatisticsModel.class);
//
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            final EscrowStatisticsModel finalEscrowStatisticsModel = escrowStatisticsModel;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalEscrowStatisticsModel, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//
//    public int loadEscrowBalance(ServicesDataController.ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ServicesModel.EscrowBalance escrowBalance = null;
//            try {
//                Call<ResponseBody> call = servicesApi.getEscrowBalance();
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    JSONObject jsonObject = new JSONObject(response.body().string());
//                    Gson gson = new Gson();
//                    escrowBalance = gson.fromJson(jsonObject.toString(),ServicesModel.EscrowBalance.class );
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            ServicesModel.EscrowBalance finalEscrowBalance = escrowBalance;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalEscrowBalance, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int createEscrowOrderForProduct(int item, int paymentProvider, ResponseCallback callback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(()-> {
//            boolean success = false;
//            APIError apiError = null;
//            try{
//
//                Map<String,Object> objectMap = new HashMap<>();
//                objectMap.put("item_shop_product",item);
//                objectMap.put("payment_provider",paymentProvider);
//                Call<ResponseBody> callBuyAirTime = servicesApi.buyEscrow(objectMap);
//                callConcurrentHashMap.put(reqId,callBuyAirTime);
//                Response<ResponseBody> responseBuyEscrow = callBuyAirTime.execute();
//                callConcurrentHashMap.remove(reqId);
//                success = responseBuyEscrow.isSuccessful();
//                if(!success)
//                {
//                    apiError = ErrorUtils.createError(responseBuyEscrow.errorBody(), responseBuyEscrow.code());
//
//                }
//
//            }catch (Exception e)
//            {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            boolean finalSuccess = success;
//            APIError finalApiError = apiError;
//            AndroidUtilities.runOnUIThread(() -> {
//                if(callback != null){
//                    callback.onResponse(finalSuccess, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//
//    public ArrayList<TLRPC.User> getListOfTgUsers(ArrayList<Long> listOfUsers ){
//        ArrayList<Long> usersToLoad = new ArrayList<>();
//        ArrayList<TLRPC.User> usersLoaded = new ArrayList<>();
//
//        for(int a = 0; a < listOfUsers.size();a++){
//            TLRPC.User user = getMessagesController().getUser(listOfUsers.get(a));
//            if(user == null){
//                usersToLoad.add(listOfUsers.get(a));
//            }else{
//                usersLoaded.add(user);
//            }
//        }
//        if(!usersToLoad.isEmpty()){
//            final CountDownLatch countDownLatch = new CountDownLatch(1);
//            final MessagesStorage messagesStorage = getMessagesStorage();
//            messagesStorage.getStorageQueue().postRunnable(() -> {
//                try {
//                    messagesStorage.getUsersInternal(TextUtils.join(",", usersToLoad), usersLoaded);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                countDownLatch.countDown();
//            });
//            try {
//                countDownLatch.await();
//            } catch (Exception e) {
//                FileLog.e(e);
//            }
//        }
////        HashMap<Long, TLRPC.User> foundUsers = new HashMap<>();
////        if(listOfUsers.size() != usersLoaded.size()){
////            ArrayList<Long> usersNotFound = new ArrayList<>();
////
////            for(int a = 0; a < usersLoaded.size();a++){
////                foundUsers.put(usersLoaded.get(a).id,usersLoaded.get(a));
////            }
////            for(int a = 0;a < listOfUsers.size();a++){
////                TLRPC.User user = foundUsers.get(listOfUsers.get(a));
////                if(user == null){
////                    user = new TLRPC.TL_userForeign_old2();
////                    user.phone = "42777";
////                    user.id = 777000;
////                    user.verified = true;
////                    user.first_name = "";
////                    user.last_name = "Notifications";
////                    user.status = null;
////                    user.photo = new TLRPC.TL_userProfilePhotoEmpty();
////                    putUser(user, true);
////            }
////        }
//
//
//
//        return usersLoaded;
//    }
//
//    public int getTransactionDetail(String type,String uuid,ResponseCallback callback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            ServicesModel.BaseTransaction transaction = null;
//            try {
//                Class<? extends ServicesModel.BaseTransaction> baseClass;
//                Call<ResponseBody> call;
//                if(type.equals(TransType.DSTV.type)){
//                    call = servicesApi.getDSTVTransactionDetail(uuid);
//                    baseClass = ServicesModel.DSTVTransactions.class;
//                }else if(type.equals(TransType.CANAL_PLUS.type)){
//                    call = servicesApi.getCanalPlusTransactionDetail(uuid);
//                    baseClass = ServicesModel.CanalPLusTransactions.class;
//                }else if(type.equals(TransType.AIRLINE.type)){
//                    call = servicesApi.getAirLineTransactionDetail(uuid);
//                    baseClass = ServicesModel.AirlineTransactions.class;
//                }else{
//                    baseClass = ServicesModel.AirTimeTransaction.class;
//                    call = servicesApi.getAirTimeTransactionDetail(uuid);
//                }
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    transaction = gson.fromJson(response.body().string(), baseClass);
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            ServicesModel.BaseTransaction finalTransaction = transaction;
//            AndroidUtilities.runOnUIThread(() -> {
//                if (callback != null) {
//                    callback.onResponse(finalTransaction, finalApiError);
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int getTransactions(String type,String next, TLRPC.User telegramUser, ResponseCallback callback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(()->{
//            APIError apiError = null;
//            ArrayList<ServicesModel.BaseTransaction> airTimeTransactions  = new ArrayList<>();
//            HashMap<Long, ArrayList<ServicesModel.AirTimeTransaction>> transactionDictList = new HashMap<>();
//
//            ArrayList<Long> usersToLoad = new ArrayList<>();
//            ArrayList<TLRPC.User> usersLoaded = new ArrayList<>();
//            try{
//                Class<? extends ServicesModel.BaseTransaction> baseClass;
//                Call<ResponseBody> call;
//                if(type.equals(TransType.DSTV.type)){
//                    if(!ShopUtils.isEmpty(next)){
//                        call = servicesApi.get(next);
//                    }else{
//                        call = servicesApi.getDSTVTransaction();
//                    }
//                    baseClass = ServicesModel.DSTVTransactions.class;
//                }else if(type.equals(TransType.CANAL_PLUS.type)){
//                    if(!ShopUtils.isEmpty(next)){
//                        call = servicesApi.get(next);
//                    }else{
//                        call = servicesApi.getCanalPlusTransaction();
//                    }
//                    baseClass = ServicesModel.CanalPLusTransactions.class;
//                }else if(type.equals(TransType.AIRLINE.type)){
//                    if(!ShopUtils.isEmpty(next)){
//                        call = servicesApi.get(next);
//                    }else{
//                        call = servicesApi.getAirLIneTransaction();
//                    }
//                    baseClass = ServicesModel.AirlineTransactions.class;
//                }else{
//                    baseClass = ServicesModel.AirTimeTransaction.class;
//                    if(!ShopUtils.isEmpty(next)){
//                        call = servicesApi.get(next);
//                    }else{
//                        if(UserObject.isUserSelf(telegramUser)){
//                            call = servicesApi.getAirTimeTransaction();
//                        }else{
//                            call = servicesApi.getAirTimeTransactionWith(telegramUser.id);
//                        }
//                    }
//                }
//                callConcurrentHashMap.put(reqId,call);
//                Response<ResponseBody> response =  call.execute();
//                if(!callConcurrentHashMap.containsKey(reqId)){
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if(response.isSuccessful()){
//                    Gson gson = new Gson();
//                    JSONObject rawObject = new JSONObject(response.body().string());
//                    JSONArray resultArray = rawObject.getJSONArray("results");
//                    for (int a = 0; a < resultArray.length(); a++){
//                        JSONObject jsonObject = resultArray.getJSONObject(a);
//                        ServicesModel.BaseTransaction transaction = gson.fromJson(jsonObject.toString(), baseClass);
//                        if(transaction instanceof ServicesModel.AirTimeTransaction){
//                            ServicesModel.AirTimeTransaction timeTransaction = (ServicesModel.AirTimeTransaction) transaction;
//                            if(timeTransaction.receiverUser != null && timeTransaction.receiverUser.telegramId != 0){
//                                timeTransaction.tgUser = getMessagesController().getUser(timeTransaction.receiverUser.telegramId);
//                                if(timeTransaction.tgUser == null){
//                                    long tgUserId = timeTransaction.receiverUser.telegramId;
//                                    ArrayList<ServicesModel.AirTimeTransaction> transList = transactionDictList.get(tgUserId);
//                                    if(transList == null){
//                                        transList = new ArrayList<>();
//                                        transactionDictList.put(tgUserId,transList);
//                                        usersToLoad.add(tgUserId);
//                                    }
//                                    transList.add(timeTransaction);
//                                }
//                            }
//                        }
//                        airTimeTransactions.add(transaction);
//                    }
//                }else{
//                    apiError = ErrorUtils.createError(response.errorBody(),response.code());
//                }
//            }catch (Exception e){
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//
//            if(!usersToLoad.isEmpty()){
//                final CountDownLatch countDownLatch = new CountDownLatch(1);
//                final MessagesStorage messagesStorage = getMessagesStorage();
//                messagesStorage.getStorageQueue().postRunnable(() -> {
//                    try {
//                        messagesStorage.getUsersInternal(TextUtils.join(",", usersToLoad), usersLoaded);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    countDownLatch.countDown();
//                });
//                try {
//                    countDownLatch.await();
//                } catch (Exception e) {
//                    FileLog.e(e);
//                }
//                for(int a = 0; a < usersLoaded.size();a++){
//                    TLRPC.User user = usersLoaded.get(a);
//                    if(user != null){
//                        ArrayList<ServicesModel.AirTimeTransaction> transaction =  transactionDictList.get(user.id);
//                        if(transaction == null){
//                            continue;
//                        }
//                        for(int i = 0;i < transaction.size();i++){
//                            transaction.get(i).tgUser = user;
//                        }
//                    }
//                }
//            }
//            if(callback != null){
//                callback.onResponse(airTimeTransactions, apiError);
//            }
//        });
//        return reqId;
//    }
//
//    public int withdrawEscrowBalance(int payment_provider,long channelId,double amount,ResponseCallback responseCallback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            boolean success = false;
//            try {
//                Map<String,Object> map = new HashMap<>();
//                map.put("amount",amount);
//                map.put("channelID",channelId);
//                map.put("payment_provider",payment_provider);
//                Call<ResponseBody> call = servicesApi.withdrawEscrowBalance(map);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    success = true;
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            boolean finalSuccess = success;
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(responseCallback != null){
//                        responseCallback.onResponse(finalSuccess,finalApiError);
//                    }
//                }
//            });
//        });
//        return reqId;
//    }
//
//    public int resumeAirtimeTransaction(String uuid,ResponseCallback responseCallback) {
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            boolean success = false;
//            try {
//                Call<ResponseBody> call = servicesApi.resumePayment(uuid);
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                if (!callConcurrentHashMap.containsKey(reqId)) {
//                    return;
//                }
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    success = true;
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            boolean finalSuccess = success;
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(responseCallback != null){
//                        responseCallback.onResponse(finalSuccess,finalApiError);
//                    }
//                }
//            });
//        });
//        return reqId;
//    }
//
//
//    private boolean requesting;
//    private boolean resolving;
//    public void sendRequestToBot(int  prodId,long dialog_id,String uuid,boolean airtime,boolean donate) {
//        AndroidUtilities.runOnUIThread(() -> {
//            if (requesting) {
//                return;
//            }
//            requesting = true;
//            TLObject object = MessagesController.getInstance(currentAccount).getUserOrChat(PlusBuildVars.getFormatterBot());
//            if (object instanceof TLRPC.User) {
//                TLRPC.User user = (TLRPC.User) object;
//                long dialogId = UserConfig.getInstance(currentAccount).getClientUserId();
//                TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
//                if(airtime){
//                    req.query =  String.format("huluairtime:%s:%s",uuid,  UUID.randomUUID().toString());
//                }else if(donate){
//                    req.query =  String.format("hulufund:%s:%s",uuid,  UUID.randomUUID().toString());
//                }else{
//                    req.query = String.format("huluproduct:%s:%s",prodId,  UUID.randomUUID().toString());
//                }
//                req.bot = MessagesController.getInstance(currentAccount).getInputUser(user);
//                req.offset = "";
//                int lower_id = (int) dialogId;
//                if (lower_id != 0) {
//                    req.peer = MessagesController.getInstance(currentAccount).getInputPeer(lower_id);
//                } else {
//                    req.peer = new TLRPC.TL_inputPeerEmpty();
//                }
//                RequestDelegate requestDelegate = (response, error) -> {
//                    AndroidUtilities.runOnUIThread(() -> {
//                        requesting = false;
//                        if (error == null) {
//                            if (response instanceof TLRPC.TL_messages_botResults) {
//                                TLRPC.TL_messages_botResults res = (TLRPC.TL_messages_botResults) response;
//                                if (res.results.size() > 0) {
//                                    TLRPC.BotInlineResult result = res.results.get(0);
//                                    long uid = user.id;
//                                    HashMap<String, String> params = new HashMap<>();
//                                    params.put("id", result.id);
//                                    params.put("query_id", "" + res.query_id);
//                                    params.put("bot", "" + uid);
//                                    params.put("bot_name", user.username);
//                                    SendMessagesHelper.prepareSendingBotContextResult(getAccountInstance(), result, params, dialog_id, null, null, true, 0);
//                                }
//                            }
//                        }
//                    });
//                };
//                ConnectionsManager.getInstance(currentAccount).sendRequest(req, requestDelegate);
//            } else {
//                requesting = false;
//                if (resolving) {
//                    return;
//                }
//                resolving = true;
//                TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
//                req.username = PlusBuildVars.getFormatterBot();
//                ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
//                    resolving = false;
//                    if (error == null) {
//                        AndroidUtilities.runOnUIThread(() -> {
//                            TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
//                            MessagesController.getInstance(currentAccount).putUsers(res.users, false);
//                            MessagesController.getInstance(currentAccount).putChats(res.chats, false);
//                            MessagesStorage.getInstance(currentAccount).putUsersAndChats(res.users, res.chats, true, true);
//                        });
//                    }
//                });
//            }
//        });
//
//    }
//
//
//    public int  uploadPhoto( ArrayList<ProductImageLayout.ImageInput> imageInputs,ResponseCallback responseCallback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            ArrayList<Long> photoIds = new ArrayList<>();
//            for(int a = 0, N  = imageInputs.size();a <N;a++){
//                ProductImageLayout.ImageInput  imageInput = imageInputs.get(a);
//                if(imageInput == null){
//                    a--;
//                    N--;
//                    imageInputs.remove(a);
//                    continue;
//                }
//                String image_loc;
//                if(imageInput.bigSize != null){
//                    image_loc  = FileLoader.getPathToAttach(imageInput.bigSize, false).getAbsolutePath();
//                }else if(imageInput.smallSize != null){
//                    image_loc  = FileLoader.getPathToAttach(imageInput.smallSize, false).getAbsolutePath();
//                }else{
//                    a--;
//                    N--;
//                    imageInputs.remove(a);
//                    continue;
//                }
//                if(TextUtils.isEmpty(image_loc)){
//                    a--;
//                    N--;
//                    imageInputs.remove(a);
//                    continue;
//                }
//                File file = new File(image_loc);
//                if(!file.exists()){
//                    a--;
//                    N--;
//                    imageInputs.remove(a);
//                }
//                if(a == 0){
//                    File stripedFiled = null;
//                    File smallFile = null;
//                    Bitmap bitmap = ImageLoader.loadBitmap(file.getAbsolutePath(), null, 300, 300, true);
//
//                    if(bitmap != null){
//                        TLRPC.PhotoSize smallPhoto = ImageLoader.scaleAndSaveImage(bitmap,50,50, 80, false, 50, 50);
//                        String path  =  FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE) + "/" + smallPhoto.location.volume_id + "_" + smallPhoto.location.local_id + ".jpg";
//                        stripedFiled = new File(path);
//                    }
//                    if(bitmap != null){
//                        TLRPC.PhotoSize smallPhoto = ImageLoader.scaleAndSaveImage(bitmap,300,300, 80, false, 300, 300);
//                        String path =  FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE) + "/" + smallPhoto.location.volume_id + "_" + smallPhoto.location.local_id + ".jpg";
//                        smallFile = new File(path);
//                    }
//
//                    if(bitmap != null){
//                        bitmap.recycle();
//                    }
//                    MultipartBody.Part stripedBody = null;
//                    MultipartBody.Part smallBody = null;
//                    MultipartBody.Part body = null;
//                    if(stripedFiled != null && stripedFiled.exists()){
//                        stripedBody = MultipartBody.Part.createFormData("stripped", file.getName(), RequestBody.create( MediaType.parse("image/*"),stripedFiled));
//                    }
//                    if(stripedFiled != null && stripedFiled.exists()){
//                        smallBody = MultipartBody.Part.createFormData("small", file.getName(), RequestBody.create( MediaType.parse("image/*"),smallFile));
//                    }
//                    body = MultipartBody.Part.createFormData("photo", file.getName(), RequestBody.create( MediaType.parse("image/*"),file));
//                    try {
//                        String order = String.valueOf(imageInput.pos);
//                        String caption = "800_800";
//                        RequestBody capBody = RequestBody.create(MultipartBody.FORM, caption);
//                        RequestBody orderBody = RequestBody.create(MultipartBody.FORM, order);
//                        if(stripedBody != null && smallBody != null){
//                            Response<ShopDataSerializer.ImageUploadResult> response =  ApiClient.getInstance(currentAccount).createShopApi().uploadPhoto(body,stripedBody,smallBody,orderBody,capBody).execute();
//                            if(response.isSuccessful() && response.body() != null){
//                                photoIds.add(response.body().id);
//                            }
//                        }else{
//                            Response<ShopDataSerializer.ImageUploadResult> response =   ApiClient.getInstance(currentAccount).createShopApi().uploadPhoto(body,capBody).execute();
//                            if(response.isSuccessful() && response.body() != null){
//                                photoIds.add(response.body().id);
//                            }
//                        }
//                    } catch (Exception e) {
//                    }
//                }else{
//                    RequestBody uploadRequestBody = RequestBody.create( MediaType.parse("image/*"),file);
//                    MultipartBody.Part body = MultipartBody.Part.createFormData("photo", file.getName(), uploadRequestBody);
//
//                    String order = String.valueOf(imageInput.pos);
//                    RequestBody description = RequestBody.create(MultipartBody.FORM, order);
//                    try {
//                        Response<ShopDataSerializer.ImageUploadResult> response =   ApiClient.getInstance(currentAccount).createShopApi().uploadPhoto(body,description).execute();
//                        if(response.isSuccessful() && response.body() != null){
//                            photoIds.add(response.body().id);
//                        }
//                    } catch (Exception ignored) {
//                    }
//                }
//            }
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(responseCallback != null){
//                        responseCallback.onResponse(photoIds,null);
//                    }
//                }
//            });
//        });
//        return reqId;
//    }
//
//
//    public int  sendDonationRequest(AbstractDonationRequest request, ResponseCallback responseCallback){
//        int reqId = lastRequestToken.getAndIncrement();
//        servicesQueue.postRunnable(() -> {
//            APIError apiError = null;
//            Object responseObject = null;
//            try {
//                Call<ResponseBody> call = null;
//                if(request.getType() == RequestType.TYPE_GET){
//                    call = servicesApi.getRequest(request.getUrl());
//                }else if(request.getType() == RequestType.TYPE_POST){
//                    call = servicesApi.postRequest(request.getUrl(),request.getBody());
//                }else if(request.getType() == RequestType.TYPE_PUT){
//                    if(request.getBody() != null){
//                        call = servicesApi.putRequest(request.getUrl(),request.getBody());
//                    }else{
//                        call = servicesApi.putRequest(request.getUrl());
//                    }
//                }
//                callConcurrentHashMap.put(reqId, call);
//                Response<ResponseBody> response = call.execute();
//                callConcurrentHashMap.remove(reqId);
//                if (response.isSuccessful()) {
//                    Gson gson = new Gson();
//                    responseObject = gson.fromJson(response.body().string(),request.getResponseType());
//                } else {
//                    apiError = ErrorUtils.createError(response.errorBody(), response.code());
//                }
//            } catch (Exception e) {
//                callConcurrentHashMap.remove(reqId);
//                apiError = ErrorUtils.createErrorFromException(e);
//            }
//            APIError finalApiError = apiError;
//            Object finalResponseObject = responseObject;
//            AndroidUtilities.runOnUIThread(new Runnable() {
//                @Override
//                public void run() {
//                    if(responseCallback != null){
//                        responseCallback.onResponse(finalResponseObject,finalApiError);
//                    }
//                }
//            });
//        });
//        return reqId;
//    }
//
//
//
//}
//
