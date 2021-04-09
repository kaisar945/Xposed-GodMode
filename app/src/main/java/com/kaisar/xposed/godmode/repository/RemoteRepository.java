package com.kaisar.xposed.godmode.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class RemoteRepository {

    public static List<Map<String, String>> fetchGroupInfo() throws IOException {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl("https://gitee.com/kaisarzu/Xposed-GodMode/raw/dev/")
                .build();
        GiteeService service = retrofit.create(GiteeService.class);
        Call<List<Map<String, String>>> fetchGroupInfo = service.fetchGroupInfo();
        Response<List<Map<String, String>>> response = fetchGroupInfo.execute();
        return response.body();
    }

    interface GiteeService {

        @GET("community.json")
        Call<List<Map<String, String>>> fetchGroupInfo();

    }
}
