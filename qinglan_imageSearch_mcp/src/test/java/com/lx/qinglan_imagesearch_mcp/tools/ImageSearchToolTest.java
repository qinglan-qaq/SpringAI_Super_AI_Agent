package com.lx.qinglan_imagesearch_mcp.tools;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImages() {
        String searchImages = String.valueOf(imageSearchTool.searchMediumImages("nature"));
        log.info(searchImages);
        Assertions.assertNotNull(searchImages);
    }
}