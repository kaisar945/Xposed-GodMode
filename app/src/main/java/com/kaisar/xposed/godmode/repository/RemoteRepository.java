package com.kaisar.xposed.godmode.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class RemoteRepository {

    public static void fetchGroupInfo(Callback<Map<String, String>[]> cb) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl("https://gitee.com/kaisarzu/Xposed-GodMode/raw/dev/")
                .build();
        RemoteService service = retrofit.create(RemoteService.class);
        Call<Map<String, String>[]> fetchGroupInfo = service.fetchGroupInfo();
        fetchGroupInfo.enqueue(cb);
    }

    interface RemoteService {

        @GET("community.json")
        Call<Map<String, String>[]> fetchGroupInfo();

    }

}
