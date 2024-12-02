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


}
