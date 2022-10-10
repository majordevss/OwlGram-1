package org.master.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface RestAPI {

    @GET
    Call<ResponseBody> get(@Url String link);

    @GET("/games/cat/")
    Call<ResponseBody> getGameCategory();

    @GET("/games/{cat}/")
    Call<ResponseBody> getGamesByCategory(@Path("cat") String cat,@Query("offset")int offset,@Query("limit") int limit);
}
