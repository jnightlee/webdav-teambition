package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.client.TeambitionClient;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
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
            private List<Cookie> cookies = Collections.emptyList();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                if (!StringUtils.hasLength(teambitionProperties.getCookies())) {
                    this.cookies = cookies;
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                String cookiesStr = teambitionProperties.getCookies();
                if (CollectionUtils.isEmpty(this.cookies) && StringUtils.hasLength(cookiesStr)) {
                    String[] cookieSplit = cookiesStr.split("; ");
                    List<Cookie> cookieList = new ArrayList<>(cookieSplit.length);
                    for (String cookie : cookieSplit) {
                        Cookie parse = Cookie.parse(url, cookie);
                        cookieList.add(parse);
                    }
                    this.cookies = cookieList;
                }
                return this.cookies;
            }
        }).build();
        TeambitionClient teambitionClient = new TeambitionClient(okHttpClient, teambitionProperties);
        teambitionClient.init();
        return teambitionClient;
    }



}
