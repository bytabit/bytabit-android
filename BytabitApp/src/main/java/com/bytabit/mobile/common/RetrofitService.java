package com.bytabit.mobile.common;

import com.bytabit.mobile.config.AppConfig;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class RetrofitService {

    protected final Retrofit retrofit;

    public RetrofitService() {

        retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}
