package com.aapo.api.client;

import com.aapo.api.client.fallback.DemoClientFallbackFactory;
import com.aapo.api.dto.DemoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "demo-service", fallbackFactory = DemoClientFallbackFactory.class)
public interface DemoClient {

    @GetMapping("/internal/demo")
    DemoDTO demo(@RequestParam("name") String name);

    @GetMapping("/internal/demo/delay")
    DemoDTO demoDelay(@RequestParam("name") String name);

    @GetMapping("/internal/demo/delay_fallback")
    DemoDTO demoDelayFallback(@RequestParam("name") String name);

    @PostMapping("/internal/demo/update_txt")
    void demoUpdateTxt(@RequestParam("id") Long id, @RequestParam("txt") String txt);
}