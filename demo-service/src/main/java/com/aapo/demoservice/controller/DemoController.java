package com.aapo.demoservice.controller;

import com.aapo.demoservice.service.IDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final IDemoService demoService;
    @GetMapping
    public String index() {
        long count = demoService.count();
        System.out.println("count = " + count);
        return "hello world!";
    }
}