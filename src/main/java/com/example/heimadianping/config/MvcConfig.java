package com.example.heimadianping.config;

import com.example.heimadianping.utils.LoginInterceptor;
import com.example.heimadianping.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @Author: shiyong
 * @CreateTime: 2024-12-02
 * @Description:
 * @Version: 1.0
 */

@Configuration
public class MvcConfig implements WebMvcConfigurer {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginInterceptor())
				.excludePathPatterns(
						"/shop/**",
						"/voucher/**",
						"/shop-type/**",
						"/upload/**",
						"/blog/hot",
						"/user/code",
						"/user/login"
				).order(1);

		registry.addInterceptor(new RefreshTokenInterceptor((stringRedisTemplate))).order(0);

	}

}
