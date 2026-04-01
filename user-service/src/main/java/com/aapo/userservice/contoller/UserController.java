package com.aapo.userservice.contoller;

import com.aapo.userservice.domain.dto.LoginFormDTO;
import com.aapo.userservice.domain.vo.UserLoginVO;
import com.aapo.userservice.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping("/login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO);
    }
}