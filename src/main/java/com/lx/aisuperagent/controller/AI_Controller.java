package com.lx.aisuperagent.controller;

import com.lx.aisuperagent.app.LawApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AI_Controller {

    @Resource
    private LawApp lawApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel chatModel;

    @GetMapping("/law_app/chat/sync")
    public String doChatWithLawLovelyAppSync(String message, String chatId) {
        return lawApp.doChat(message, chatId);
    }

}
