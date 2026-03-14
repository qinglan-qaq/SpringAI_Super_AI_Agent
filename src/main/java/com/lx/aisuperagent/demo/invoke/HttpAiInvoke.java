package com.lx.aisuperagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 以Http形式请求
 */
public class HttpAiInvoke {
    public static void main(String[] args) {
        // 1. 定义 API 地址和 API Key
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        String apiKey = TestApiKey.apiKey; // 替换为实际的 API Key

        // 2. 构建请求体 (JSON 结构)
        JSONObject input = JSONUtil.createObj()
                .set("messages", JSONUtil.createArray()
                        .set(JSONUtil.createObj().set("role", "system").set("content", "You are a helpful assistant."))
                        .set(JSONUtil.createObj().set("role", "user").set("content", "泥嚎 我喜欢你~ 以温柔可爱傲娇的小妹妹语气回答我")));

        JSONObject parameters = JSONUtil.createObj()
                .set("result_format", "message");

        JSONObject body = JSONUtil.createObj()
                .set("model", "qwen-plus")
                .set("input", input)
                .set("parameters", parameters);

        // 3. 发送 POST 请求
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .execute();

        // 4. 打印结果
        if (response.isOk()) {
            System.out.println("请求成功：");
            System.out.println(JSONUtil.formatJsonStr(response.body()));
        } else {
            System.out.println("请求失败，状态码：" + response.getStatus());
            System.out.println("错误详情：" + response.body());
        }
    }
}