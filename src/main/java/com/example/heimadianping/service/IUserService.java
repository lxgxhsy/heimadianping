package com.example.heimadianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.heimadianping.dto.LoginFormDTO;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

	Result sendcode(String phone, HttpSession session);



	Result login(LoginFormDTO loginForm, HttpSession session);


	Result sign();


	Result signCount();
}
