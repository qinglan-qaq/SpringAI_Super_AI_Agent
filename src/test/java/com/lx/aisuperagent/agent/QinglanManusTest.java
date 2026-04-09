package com.lx.aisuperagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest
class QinglanManusTest {

    @Resource
    private QinglanManus qinglanManus;

    @Test
    void run() {
        String userPrompt = """  
                我需要一些关于自然(nature)的照片
                然后给出相关的中国旅游地理位置推荐
                并输出为pdf文件
                """;
        String answer = qinglanManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}


