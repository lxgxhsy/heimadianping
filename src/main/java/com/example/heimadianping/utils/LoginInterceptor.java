package com.example.heimadianping.utils;


import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @Author: sy
 * @CreateTime: 2024-12-02
 * @Description: 登录拦截器
 * @Version: 1.0
 */


public class LoginInterceptor implements HandlerInterceptor {



	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// 1.判断是否需要拦截（ThreadLocal中是否有用户）
		if (UserHolder.getUser() == null) {
			// 没有，需要拦截，设置状态码
			response.setStatus(401);
			// 拦截
			return false;
		}
		// 有用户，则放行
		return true;
	}

}
