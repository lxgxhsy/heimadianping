package com.example.heimadianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.Follow;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {



	Result follow(Long followUserId, Boolean isFollow);


	Result isFollow(Long followUserId);


	Result followCommons(Long id);
}
