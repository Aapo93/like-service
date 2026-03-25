package com.aapo.demoservice.controller.internal;

import cn.hutool.core.thread.ThreadUtil;
import com.aapo.api.dto.DemoDTO;
import com.aapo.demoservice.config.DemoProperties;
import com.aapo.demoservice.service.IDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("internalDemoController")
@RequestMapping("/internal/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoProperties demoProperties;

    private final IDemoService demoService;

    @GetMapping
    public DemoDTO index(String name) {
        DemoDTO demoDTO = new DemoDTO();
        demoDTO.setText(String.format("%s-%s,hello world!", name, demoProperties.getName()));
        return demoDTO;
    }

    /**
     * sentinel流控
     * 模拟业务延迟，单线程qps为2
     * @param name
     * @return
     */
    @GetMapping("/delay")
    public DemoDTO indexByDelay(String name) {
        //模拟业务延迟，单线程qps为2
        ThreadUtil.sleep(500);
        DemoDTO demoDTO = new DemoDTO();
        demoDTO.setText(String.format("%s-%s,模拟业务延迟", name, demoProperties.getName()));
        return demoDTO;
    }

    /**
     * sentinel流控
     * 模拟业务延迟，单线程qps为2
     * @param name
     * @return
     */
    @GetMapping("/delay_fallback")
    public DemoDTO indexByDelayFallback(String name) {
        //模拟业务延迟，单线程qps为2
        ThreadUtil.sleep(500);
        DemoDTO demoDTO = new DemoDTO();
        demoDTO.setText(String.format("%s-%s,模拟业务延迟", name, demoProperties.getName()));
        return demoDTO;
    }

    /**
     * seata分布式事务
     * @param id
     * @param txt
     */
    @PostMapping("/update_txt")
    public void updateTxt(Long id, String txt) {
        demoService.updateTxt(id, txt);
    }
}