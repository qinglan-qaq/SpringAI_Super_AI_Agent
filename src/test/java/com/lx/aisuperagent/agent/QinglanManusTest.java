package com.lx.aisuperagent.agent;

import com.lx.aisuperagent.rag.config.LawAppVectorStoreConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest

class QinglanManusTest {

    @Resource
    private QinglanManus qinglanManus;

    @Test
    void run() {
        String userPrompt = """  
                我需要一些关于自然(nature)的照片
                然后给出相关的澳洲旅游地理位置推荐
                并使用中文输出到pdf文件
                """;
        String answer = qinglanManus.run(userPrompt);
        qinglanManus.runStream(userPrompt);
        Assertions.assertNotNull(answer);
    }
}


