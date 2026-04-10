package com.lx.aisuperagent.Tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 单独的Java处理工具 不需要Spring AI的上下文支持
 */
class FileOptionToolTest {


    @Test
    void testReadFile() {
        FileOptionTool fileOptionTool = new FileOptionTool();
        String fileName = "Tools模块-思路笔记.md";
        String readFile = fileOptionTool.readFile(fileName);
        System.out.println(readFile);
        Assertions.assertNotNull(readFile);

    }

    @Test
    void testWriteFile() {
        FileOptionTool fileOptionTool = new FileOptionTool();
        String fileName = "tool测试工具.txt";
        String content = "这里是tool文件写入测试 2026年4月10日09:10:56";
        String result = fileOptionTool.writeFile(fileName,content);
        Assertions.assertNotNull(result);
    }
}
