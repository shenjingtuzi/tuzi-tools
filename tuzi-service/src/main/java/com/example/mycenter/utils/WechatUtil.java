package com.example.mycenter.utils;

import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WechatUtil {
    @Value("${spring.wechat.app-id}")
    private String appId;
    @Value("${spring.wechat.app-secret}")
    private String appSecret;

    // 调用微信接口获取openid/unionid
    public JSONObject getWxSession(String code) throws IOException {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code
        );
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONObject json = JSONObject.parseObject(responseBody);
            // 检查微信接口返回错误
            if (json.containsKey("errcode") && json.getInteger("errcode") != 0) {
                throw new RuntimeException("微信接口返回错误：" + json.getString("errmsg"));
            }
            return json;
        }
    }
}