package com.example.heimadianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.Blog;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {


	Result queryBlogById(Long id);


	Result queryBlogLikes(Long id);
	Result queryHotBlog(Integer current);

	Result likeBlog(Long id);

	Result saveBlog(Blog blog);

	Result queryBlogOfFollow(Long max, Integer offset);
}
