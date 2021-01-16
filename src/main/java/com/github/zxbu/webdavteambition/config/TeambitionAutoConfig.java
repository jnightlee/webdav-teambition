package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.client.TeambitionClient;
import com.github.zxbu.webdavteambition.store.TeambitionFileSystemStore;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableConfigurationProperties(TeambitionProperties.class)
public class TeambitionAutoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeambitionAutoConfig.class);

    @Autowired
    private TeambitionProperties teambitionProperties;

    @Bean
    public TeambitionClient teambitionClient(ApplicationContext applicationContext) throws Exception {

        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                request = request.newBuilder()
                        .removeHeader("User-Agent")
                        .addHeader("User-Agent", teambitionProperties.getAgent())
                        .build();
                return chain.proceed(request);
            }
        }).cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                if (StringUtils.hasLength(teambitionProperties.getCookies())) {
                    //  do nothing
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                String cookies = teambitionProperties.getCookies();
                String[] cookieSplit = cookies.split("; ");
                List<Cookie> cookieList = new ArrayList<>(cookieSplit.length);
                for (String cookie : cookieSplit) {
                    Cookie parse = Cookie.parse(url, cookie);
                    cookieList.add(parse);
                }
                return cookieList;
            }
        }).build();
        TeambitionClient teambitionClient = new TeambitionClient(okHttpClient, teambitionProperties);
        try (Response response = okHttpClient.newCall(new Request.Builder().get().url(teambitionProperties.getUrl() + "/pan/api").build()).execute()) {
            if (response.isSuccessful()) {
                LOGGER.info("TeambitionClient 启动成功");
            }
        }
        teambitionClient.init();
        return teambitionClient;
    }


}
