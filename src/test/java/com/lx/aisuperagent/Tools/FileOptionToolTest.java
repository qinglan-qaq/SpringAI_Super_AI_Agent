package com.lx.aisuperagent.Tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

class FileOptionToolTest {


    @Test
    void testReadFile() {
        FileOptionTool fileOptionTool = new FileOptionTool();
        String fileName = "2026的运势.md";
        String readFile = fileOptionTool.readFile(fileName);
        System.out.println(readFile);
        Assertions.assertNotNull(readFile);

    }

    @Test
    void testWriteFile() {
        FileOptionTool fileOptionTool = new FileOptionTool();
        String fileName = "tool测试工具.txt";
        String content = "这里是tool文件写入测试";
        String result = fileOptionTool.writeFile(fileName,content);
        Assertions.assertNotNull(result);
    }
}
