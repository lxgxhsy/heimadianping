package com.example.heimadianping.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.heimadianping.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: shiyong
 * @CreateTime: 2024-12-02
 * @Description:
 * @Version: 1.0
 */


public class RefreshTokenInterceptor implements HandlerInterceptor {
	private StringRedisTemplate stringRedisTemplate;

	public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		String token = request.getHeader("authorization");
		if(StrUtil.isBlank(token)){
			return true;
		}
		String key = RedisConstants.LOGIN_USER_KEY + token;
		Map<Object,Object> userMap = stringRedisTemplate.opsForHash()
				.entries(key);

		if(userMap.isEmpty()){
			return true;
		}
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);


		UserHolder.saveUser(userDTO);
		stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
		//移除用户
		UserHolder.removeUser();
	}
}
