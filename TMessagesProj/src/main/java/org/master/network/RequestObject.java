package org.master.network;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public   class RequestObject implements  Call<ResponseBody>{

    public int requestToken;
    public int instanceNum;

    public RequestObject(int token,int account){
        requestToken = token;
        instanceNum = account;
    }

    @Override
    public Response<ResponseBody> execute() throws IOException {
        return null;
    }

    @Override
    public void enqueue(Callback<ResponseBody> callback) {

    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public Call<ResponseBody> clone() {
        return null;
    }

    @Override
    public Request request() {
        return null;
    }

    @Override
    public Timeout timeout() {
        return null;
    }
}
