package com.aapo.userservice.service.impl;

import com.aapo.common.exception.BadRequestException;
import com.aapo.common.exception.ForbiddenException;
import com.aapo.userservice.config.JwtProperties;
import com.aapo.userservice.domain.dto.LoginFormDTO;
import com.aapo.userservice.domain.po.User;
import com.aapo.userservice.domain.vo.UserLoginVO;
import com.aapo.userservice.enums.UserStatus;
import com.aapo.userservice.mapper.UserMapper;
import com.aapo.userservice.service.IUserService;
import com.aapo.userservice.utils.JwtTool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final PasswordEncoder passwordEncoder;

    private final JwtTool jwtTool;

    private final JwtProperties jwtProperties;

    @Override
    public UserLoginVO login(LoginFormDTO loginFormDTO) {
        // 1.数据校验
        String username = loginFormDTO.getUsername();
        String password = loginFormDTO.getPassword();
        // 2.根据用户名或手机号查询
        User user = lambdaQuery().eq(User::getUsername, username).one();
        Assert.notNull(user, "用户名错误");
        // 3.校验是否禁用
        if (user.getStatus() == UserStatus.FROZEN) {
            throw new ForbiddenException("用户被冻结");
        }
        // 4.校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }
        // 5.生成TOKEN
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        // 6.封装VO返回
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setToken(token);
        return vo;
    }
}