package com.example.heimadianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.dto.ScrollResult;
import com.example.heimadianping.dto.UserDTO;
import com.example.heimadianping.entity.Blog;
import com.example.heimadianping.entity.Follow;
import com.example.heimadianping.entity.User;
import com.example.heimadianping.mapper.BlogMapper;
import com.example.heimadianping.service.IBlogService;
import com.example.heimadianping.service.IFollowService;
import com.example.heimadianping.service.IUserService;
import com.example.heimadianping.utils.SystemConstants;
import com.example.heimadianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.heimadianping.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.example.heimadianping.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

	@Resource
	private IUserService userService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public Result queryBlogById(Long id) {
		// 1.查询blog
		Blog blog = getById(id);
		if(blog == null){
			return Result.fail("笔记不存在");
		}
		// 2.查询blog有关的用户
		queryBlogUser(blog);
		//3.获取blog是否被点赞
		isBlogLiked(blog);

		return Result.ok(blog);
	}

	@Override
	public Result queryBlogLikes(Long id) {
		String key = BLOG_LIKED_KEY + id;
		//查询top5的点赞用户 zrange key 0 4
		Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
		//为空
		if(top5.isEmpty() || top5 == null){
			return Result.ok(Collections.emptyList());
		}
		//解析用户id	
		List<Long> ids = top5.stream().
				map(Long::valueOf).collect(Collectors.toList());
		String idStr = StrUtil.join(",",ids);
		// 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
		List<UserDTO> UserDTOS = userService.query()
				.in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
				.stream()
				.map(user -> BeanUtil.copyProperties(user, UserDTO.class))
				.collect(Collectors.toList());


		return Result.ok(UserDTOS);
	}
	
	@Override
	public Result likeBlog(Long id) {
		//获取登录用户
		Long userId = UserHolder.getUser().getId();
		//首先是否已经点赞过
		String key = BLOG_LIKED_KEY + id;
		Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
		if (score == null) {
			//如果未点赞 可以点赞
			boolean isSuccess = update().setSql("liked = liked +  1").eq("id", id).update();
			//保存用户到redis的set集合中 zadd key value score
			if (isSuccess) {
				stringRedisTemplate.opsForZSet()
						.add(key, userId.toString(), System.currentTimeMillis());
			}
		} else {
			//如果点赞 取消点赞
		boolean isSuccess = update().setSql("liked = liked  - 1").eq("id", id).update();
	     if(isSuccess){
	     	stringRedisTemplate.opsForZSet().remove(key, userId.toString());
	     }
		}
		return Result.ok();
	}

	private void isBlogLiked(Blog blog) {
		//获取登录用户
		UserDTO user = UserHolder.getUser();
		if(user == null){
//			log.error("用户未登录");
			return;
		}
		Long userId = user.getId();

		//判断登录用户是否是自己
		String key = BLOG_LIKED_KEY + blog.getId();
		Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
		blog.setIsLike(score != null);
	}

	private void queryBlogUser(Blog blog) {
		Long userId = blog.getUserId();
		User user = userService.getById(userId);
		blog.setName(user.getNickName());
		blog.setIcon(user.getIcon());
	}
}
