package com.lx.aisuperagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LawAppTest {

    @Resource
    public LawApp lawApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
//        第一轮
        String message = "我是一个失败的丈夫, 我要离婚";
        String result = lawApp.doChat(message, chatId);
//        第二轮
        message = "我要最大程度减少我的财产损失";
        result = lawApp.doChat(message, chatId);
        Assertions.assertNotNull(result);
//        第三轮
        message = "我是谁?我要干什么,帮我回忆起来";
        result = lawApp.doChat(message, chatId);
        Assertions.assertNotNull(result);
    }
}