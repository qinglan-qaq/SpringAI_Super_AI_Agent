package com.lx.aisuperagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


// 单元测试场景下不需要启动 MCP 客户端，避免 /path/to/server 占位命令报错
@SpringBootTest
class QinglanManusTest {

    @Resource
    private QinglanManus qinglanManus;

    @Test
    void run() {
        String userPrompt = """  
                我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点,
                并结合一些网络图片，制定一份详细的约会计划，  
                并以 PDF 格式输出""";
        String answer = qinglanManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}


