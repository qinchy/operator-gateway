package com.qinchy.operatorgatewayservice.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient getOkHttpClient() {
        LogInterceptor interceptor = new LogInterceptor();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .addNetworkInterceptor(interceptor)
                .build();
        return okHttpClient;
    }


}
