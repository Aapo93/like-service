package com.aapo.userservice.contoller;

import com.aapo.api.client.DemoClient;
import com.aapo.api.dto.DemoDTO;
import com.aapo.common.utils.UserContext;
import com.aapo.userservice.domain.dto.LoginFormDTO;
import com.aapo.userservice.domain.po.User;
import com.aapo.userservice.domain.vo.UserLoginVO;
import com.aapo.userservice.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final DemoClient demoClient;

    private final IUserService userService;

    @PostMapping("/login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO);
    }

    @GetMapping("/demo")
    public String getDemo() {
        String name = userService.lambdaQuery().eq(User::getId, UserContext.getUser()).one().getUsername();
        DemoDTO demo = demoClient.demo(name);
        return demo.getText();
    }

    @PostMapping("/demo")
    public String postDemo() {
        String name = userService.lambdaQuery().eq(User::getId, UserContext.getUser()).one().getUsername();
        DemoDTO demo = demoClient.demo(name);
        return demo.getText();
    }

    /**
     * 模拟延迟延迟服务
     *
     * @return
     */
    @GetMapping("/demo_by_delay")
    public String getDemoByDelay() {
        String name = userService.lambdaQuery().eq(User::getId, UserContext.getUser()).one().getUsername();
        DemoDTO demo = demoClient.demoDelay(name);
        return demo.getText();
    }

    /**
     * 模拟延迟延迟服务
     *
     * @return
     */
    @GetMapping("/demo_by_delay_fallback")
    public String getDemoByDelayFailback() {
        String name = userService.lambdaQuery().eq(User::getId, UserContext.getUser()).one().getUsername();
        DemoDTO demo = demoClient.demoDelayFallback(name);
        return demo.getText();
    }

    /**
     * 模拟分布式事务
     * @param phone
     */
    @PostMapping("/update_phone")
    public void updatePhone(String phone) {
        userService.updatePhone(phone);
    }
}