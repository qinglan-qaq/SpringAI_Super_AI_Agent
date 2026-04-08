package com.lx.qinglan_imagesearch_mcp.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ImageSearchTool {

    //    API
    public static final String API_KEY = "TDjxtCU2CCJmWMEbEuFBSaRbABi8p800O7cU7gvBETRB1s36JSlRNLED";

    //    搜索接口
    public static final String API_URL = "https://api.pexels.com/v1/search";

    //  暴露为可用工具
    @Tool(description = "search Image from web")
    public String searchImage(@ToolParam(description = "Search Image Keywords") String query) {
        try {
            //      字符串拼接
            return String.join("?", searchMediumImages(query));

        } catch (Exception e) {
            return "Error search Image : " + e.getMessage();
        }
    }

    /**
     * 获取数据,
     * 利用类型转换提取src:medium:对应的url
     *
     * @param query
     * @return
     */
    public List<String> searchMediumImages(String query) {
        //设置API和问题参数, 注意格式
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + API_KEY);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        //      获取请求结果
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();


        //        解析JSON对象为字符串,在photo:src:medium中获取
        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .collect(Collectors.toList());
    }
}
