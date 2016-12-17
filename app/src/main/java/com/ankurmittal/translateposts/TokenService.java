package com.ankurmittal.translateposts;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by AnkurMittal2 on 15-12-2016.
 */

public interface  TokenService {


    @Headers({
            "Ocp-Apim-Subscription-Key: 80e4e888083549658be9a344ea1146c0",
            "Accept: application/jwt"
    })
    @POST("issueToken/")
    Call<String> getToken(@Body String body);


}
