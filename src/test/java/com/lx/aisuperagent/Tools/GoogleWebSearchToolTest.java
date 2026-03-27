package com.lx.aisuperagent.Tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.*;

class GoogleWebSearchToolTest {
    @Value("${search-api.api-key")
    private String searchApi;

    @Test
    void googleSearch() {
        GoogleWebSearchTool searchTool = new GoogleWebSearchTool(searchApi);
        String search = searchTool.googleSearch("谁是世界上最好的计算机高级语言?");
        assertNotNull(search);
    }
}