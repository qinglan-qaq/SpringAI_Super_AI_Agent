package com.lx.aisuperagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")

public class DemoController {

    @GetMapping
    public String healthCheck(){
        return "i`m live!";
    }
}
