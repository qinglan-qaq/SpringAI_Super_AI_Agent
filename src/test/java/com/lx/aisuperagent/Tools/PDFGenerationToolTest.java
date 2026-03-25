package com.lx.aisuperagent.Tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class PDFGenerationToolTest {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Test
    void testApiKey() {
        System.out.println("API Key: " + apiKey);
    }


    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf生成法务报告.pdf";
        String content = "乘舟侧畔千帆过,病树前头万木春...";
        String result = tool.GeneratePDF(fileName, content);
        assertNotNull(result);
    }
}


