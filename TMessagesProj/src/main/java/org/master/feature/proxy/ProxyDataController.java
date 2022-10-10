package org.master.feature.proxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class ProxyDataController {

    public interface CallBack{
        void onProxyLoaded(ArrayList<ProxyModel> proxyModels,String error);
    }

    public static final String PROXY_LINK = "https://fastchanneladdinglink.firebaseio.com/proxyInfo.json";
    private static volatile ProxyDataController Instance;

    public static ProxyDataController getInstance() {
        ProxyDataController localInstance = Instance;
        if (localInstance == null) {
            synchronized (ProxyDataController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ProxyDataController();
                }
            }
        }
        return localInstance;
    }

    private SharedPreferences preferences;
    private long lastUpdateTime;
    private boolean loadingProxy;
    private Gson gson;
    public ProxyDataController(){
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("proxyConfig", Context.MODE_PRIVATE);
        lastUpdateTime  = preferences.getLong("lastUpdateTime",0);
        gson = new Gson();
    }

    public void loadProxyData(CallBack callBack){
        if(loadingProxy){
            return;
        }
        loadingProxy = true;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                String result = preferences.getString("proxyModels","");
                String error = null;
                ArrayList<ProxyModel> models = new ArrayList<>();
                long dur = System.currentTimeMillis() - lastUpdateTime;
                if(dur > 1000 * 60 * 60 * 24 || TextUtils.isEmpty(result)){
                    URL url = new URL(PROXY_LINK);
                    URLConnection urlConn = url.openConnection();
                    HttpsURLConnection httpsConn = (HttpsURLConnection) urlConn;
                    httpsConn.setRequestMethod("GET");
                    httpsConn.connect();
                    InputStream in;
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
                    preferences.edit().putLong("lastUpdateTime",System.currentTimeMillis()).commit();
                }
                JSONArray jsonArray = new JSONArray(result);
                for(int a = 0; a < jsonArray.length();a++){
                    String channel = jsonArray.getString(a);
                    ProxyModel proxyModel  = gson.fromJson(channel,ProxyModel.class);
                    models.add(proxyModel);
                }
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingProxy = false;
                        if(callBack != null){
                            callBack.onProxyLoaded(models,error);
                        }
                    }
                });

            }catch (Exception e){
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingProxy = false;
                        if(callBack != null){
                            callBack.onProxyLoaded(null,e.getMessage());
                        }
                    }
                });
            }
        });
    }

}
