package com.lx.aisuperagent.Tools;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
class GoogleWebSearchToolTest {
    @Value("${search-api.api-key}")
    private String searchApi;

    @Test
    void googleSearch() {
        GoogleWebSearchTool searchTool = new GoogleWebSearchTool(searchApi);
        String search = searchTool.googleSearch("梁博是谁?");
        log.info(search);
        assertNotNull(search);
    }
}