package com.kaisar.xposed.godmode.repository;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kaisar.xposed.godmode.BuildConfig;
import com.kaisar.xposed.godmode.bean.GroupInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class RemoteRepository {

    public static void fetchGroupInfo(Callback<List<GroupInfo>> cb) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        String branch = TextUtils.equals(BuildConfig.BUILD_TYPE, "release") ? "master" : "dev";
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(String.format("https://raw.githubusercontent.com/kaisar945/Xposed-GodMode/%s/", branch))
                .build();
        RemoteService service = retrofit.create(RemoteService.class);
        Call<List<GroupInfo>> call = service.fetchGroupInfoList();
        call.enqueue(cb);
    }

    interface RemoteService {

        @GET("community.json")
        Call<List<GroupInfo>> fetchGroupInfoList();

    }

}
