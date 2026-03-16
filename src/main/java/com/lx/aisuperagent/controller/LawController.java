package com.lx.aisuperagent.controller;

import com.lx.aisuperagent.app.LawApp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/law")
public class LawController {

    private final LawApp lawApp;

    public LawController(LawApp lawApp) {
        this.lawApp = lawApp;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("cid") String conversationId,
                       @RequestParam("msg") String message) {
        return lawApp.chat(conversationId, message);
    }
}

