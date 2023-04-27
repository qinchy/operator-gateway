package com.qinchy.operatorgatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * 定义OkHttp的请求和响应拦截器
 */
@Slf4j
public class LogInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        log.info("请求信息：{}", request.toString());

        Response response = chain.proceed(request);
        log.info("响应信息：{}", response);

        return response;
    }
}
