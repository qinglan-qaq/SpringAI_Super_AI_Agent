package com.lx.aisuperagent.Tools;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


class PDFGenerationToolTest {


    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "pdf生成法务报告.pdf";
        String content = "乘舟侧畔千帆过,病树前头万木春...";
        String result = tool.GeneratePDF(fileName, content);
        assertNotNull(result);
    }
}


