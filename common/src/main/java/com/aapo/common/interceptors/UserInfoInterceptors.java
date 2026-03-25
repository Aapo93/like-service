package com.aapo.common.interceptors;

import com.aapo.common.utils.UserContext;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserInfoInterceptors implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1、获取登陆用户信息
        String userInfo = request.getHeader("user-info");
        //2、判断是否获取了用户，如果有，存入ThreadLocal
        if (StringUtils.isNotBlank(userInfo)) {
            UserContext.setUser(Long.valueOf(userInfo));
        }
        //3、放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
    }
}