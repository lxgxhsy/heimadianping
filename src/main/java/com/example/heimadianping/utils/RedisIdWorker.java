package com.example.heimadianping.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author: shiyong
 * @CreateTime: 2024-12-03
 * @Description:
 * @Version: 1.0
 */

@Component
public class RedisIdWorker {
	/**
	 * 开始时间戳
	 */
	private final long BEGIN_TIMESTAMP = 1640995200L;

	private static final int COUNT_BITS = 32;

     private StringRedisTemplate stringRedisTemplate;





	public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public long nextId(String keyPrefix) {
		// 1.生成时间戳
		LocalDateTime now = LocalDateTime.now();
		//当前秒数
		long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
		//时间戳
		long timestamp = nowSecond - BEGIN_TIMESTAMP;

		// 2.生成序列号
		// 2.1.获取当前日期，精确到天
		String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
		// 2.2.自增长  date加上去就是为了区分 也可以达到统计效果
		long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

		// 3.拼接并返回
		return timestamp << COUNT_BITS | count;
	}
}
