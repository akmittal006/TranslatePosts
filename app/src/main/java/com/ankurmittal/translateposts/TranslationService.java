package com.ankurmittal.translateposts;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Created by AnkurMittal2 on 15-12-2016.
 */

public interface TranslationService {

    @Headers({
                "Accept : application/xml"
        })
    @GET("Translate")
    Call<String> translateTextArray(@Query("appid") String appid,@Query("from") String from,@Query("text") String texts,@Query("to") String to);


}
