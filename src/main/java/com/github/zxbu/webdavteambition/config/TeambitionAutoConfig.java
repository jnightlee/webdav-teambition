package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
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
@EnableConfigurationProperties(AliYunDriveProperties.class)
public class TeambitionAutoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeambitionAutoConfig.class);

    @Autowired
    private AliYunDriveProperties aliYunDriveProperties;

    @Bean
    public AliYunDriverClient teambitionClient(ApplicationContext applicationContext) throws Exception {

        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                request = request.newBuilder()
                        .removeHeader("User-Agent")
                        .addHeader("User-Agent", aliYunDriveProperties.getAgent())
                        .addHeader("authorization", aliYunDriveProperties.getAuthorization())
                        .build();
                return chain.proceed(request);
            }
        }).build();
        AliYunDriverClient aliYunDriverClient = new AliYunDriverClient(okHttpClient, aliYunDriveProperties);
        aliYunDriverClient.init();
        return aliYunDriverClient;
    }



}
