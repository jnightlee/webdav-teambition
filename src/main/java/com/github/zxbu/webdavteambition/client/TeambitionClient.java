package com.github.zxbu.webdavteambition.client;

import com.github.zxbu.webdavteambition.config.TeambitionProperties;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import okhttp3.*;
import okio.BufferedSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeambitionClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeambitionClient.class);
    private OkHttpClient okHttpClient;
    private TeambitionProperties teambitionProperties;

    public TeambitionClient(OkHttpClient okHttpClient, TeambitionProperties teambitionProperties) {
        this.okHttpClient = okHttpClient;
        this.teambitionProperties = teambitionProperties;
    }

    public void init() {
        if (getOrgId() == null || getRootId() == null || getDriveId() == null || getSpaceId() == null) {
            String personalJson = get("https://www.teambition.com/api/organizations/personal", Collections.emptyMap());
            String orgId = (String) JsonUtil.getJsonNodeValue(personalJson, "_id");
            teambitionProperties.setOrgId(orgId);
            String memberId = (String) JsonUtil.getJsonNodeValue(personalJson, "_creatorId");

            String orgJson = get("/pan/api/orgs/" + orgId, Collections.singletonMap("orgId", orgId));
            String driveId = (String) JsonUtil.getJsonNodeValue(orgJson, "data.driveId");
            teambitionProperties.setDriveId(driveId);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("orgId", orgId);
            params.put("memberId", memberId);
            String spacesJson = get("/pan/api/spaces", params);
            String rootId = (String) JsonUtil.getJsonNodeValue(spacesJson, "[0].rootId");
            String spaceId = (String) JsonUtil.getJsonNodeValue(spacesJson, "[0].spaceId");
            teambitionProperties.setRootId(rootId);
            teambitionProperties.setSpaceId(spaceId);
        }
    }


    public String getOrgId() {
        return teambitionProperties.getOrgId();
    }

    public String getDriveId() {
        return teambitionProperties.getDriveId();
    }

    public String getSpaceId() {
        return teambitionProperties.getSpaceId();
    }

    public String getRootId() {
        return teambitionProperties.getRootId();
    }

    public InputStream download(String url) {
        Request request = new Request.Builder().url(url).build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            return response.body().byteStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void upload(String url, byte[] bytes, final int offset, final int byteCount) {
        Request request = new Request.Builder()
                .put(RequestBody.create(MediaType.parse(""), bytes, offset, byteCount))
                .url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()){
            LOGGER.info("post {}, code {}", url, response.code());
            if (!response.isSuccessful()) {
                LOGGER.error("请求失败，url={}, code={}, body={}", url, response.code(), response.body().string());
                throw new RuntimeException("请求失败：" + url);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String post(String url, Object body) {
        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(body)))
                .url(getTotalUrl(url)).build();
        try (Response response = okHttpClient.newCall(request).execute()){
            LOGGER.info("post {}, code {}", url, response.code());
            if (!response.isSuccessful()) {
                LOGGER.error("请求失败，url={}, code={}, body={}", url, response.code(), response.body().string());
                throw new RuntimeException("请求失败：" + url);
            }
            return toString(response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String put(String url, Object body) {
        Request request = new Request.Builder()
                .put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(body)))
                .url(getTotalUrl(url)).build();
        try (Response response = okHttpClient.newCall(request).execute()){
            LOGGER.info("put {}, code {}", url, response.code());
            if (!response.isSuccessful()) {
                LOGGER.error("请求失败，url={}, code={}, body={}", url, response.code(), response.body().string());
                throw new RuntimeException("请求失败：" + url);
            }
            return toString(response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String get(String url, Map<String, String> params)  {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(getTotalUrl(url)).newBuilder();
            params.forEach(urlBuilder::addQueryParameter);

            Request request = new Request.Builder().get().url(urlBuilder.build()).build();
            try (Response response = okHttpClient.newCall(request).execute()){
                LOGGER.info("get {}, code {}", urlBuilder.build(), response.code());
                if (!response.isSuccessful()) {
                    throw new RuntimeException("请求失败：" + urlBuilder.build().toString());
                }
                return toString(response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String toString(ResponseBody responseBody) throws IOException {
        if (responseBody == null) {
            return null;
        }
        return responseBody.string();
    }

    private String getTotalUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return teambitionProperties.getUrl() + url;
    }
}
