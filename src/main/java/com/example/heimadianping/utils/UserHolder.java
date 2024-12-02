package com.example.heimadianping.utils;

import com.example.heimadianping.dto.UserDTO;

/**
 * @Author: shiyong
 * @CreateTime: 2024-12-01
 * @Description: 获取用户上下文
 * @Version: 1.0
 */


public class UserHolder {
	private static ThreadLocal<UserDTO> tl = new ThreadLocal<>();

	public static void saveUser(UserDTO user) {
		tl.set(user);
	}

	public static UserDTO getUser(){
		return tl.get();
	}

	public static void removeUser(){
		tl.remove();
	}

}
