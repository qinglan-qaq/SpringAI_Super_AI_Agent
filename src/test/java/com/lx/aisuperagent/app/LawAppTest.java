package com.lx.aisuperagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.UUID;

@SpringBootTest
class LawAppTest {

    private ChatModel chatModel;

    @Resource
    public LawApp lawApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
//        第一轮
        String message = "你是我结婚三年的老婆,今天是结婚纪念日";
        String result = lawApp.doChat(message, chatId);
//        第二轮
        message = "我下楼买点\"晚上好玩的\"(露出意味深长的表情)";
        result = lawApp.doChat(message, chatId);
        Assertions.assertNotNull(result);
//        第三轮
        message = "在床上时,我是谁?我在干什么,你又是谁?";
        result = lawApp.doChat(message, chatId);
        Assertions.assertNotNull(result);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
//        第一轮
        String message = "你好我是清澜, 我如何才能更好地满足对方的索取";
        LawApp.LawReport lawReport = lawApp.doChatWithReport(message,chatId);
        Assertions.assertNotNull(lawReport);
    }

    @Test
    void doChatWithRAG() {
        String chatId = UUID.randomUUID().toString();

        String message = "我想离婚,我如何获取赔偿,给出详细的方案,引用相关案件";
        ChatResponse lawReport = lawApp.doChatWithRAG(message,chatId);
        Assertions.assertNotNull(lawReport);
    }

    @Test
    void doChatWithCloudRAG() {
        String chatId = UUID.randomUUID().toString();
        String message = "我想离婚,我老婆在外边有人了有相关历史案例吗";
        ChatResponse lawReport = lawApp.doChatWithRAG(message,chatId);
        Assertions.assertNotNull(lawReport);

    }
}