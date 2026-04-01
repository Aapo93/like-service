package com.aapo.userservice.service;

import com.aapo.userservice.domain.dto.LoginFormDTO;
import com.aapo.userservice.domain.po.User;
import com.aapo.userservice.domain.vo.UserLoginVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserService extends IService<User> {

    /**
     * 登录
     * @param loginFormDTO
     * @return
     */
    UserLoginVO login(LoginFormDTO loginFormDTO);
}