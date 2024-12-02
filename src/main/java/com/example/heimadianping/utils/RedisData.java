package com.example.heimadianping.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: shiyong
 * @CreateTime: 2024-12-02
 * @Description:
 * @Version: 1.0
 */

@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
