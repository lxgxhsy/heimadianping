package com.example.heimadianping.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.Shop;
import com.example.heimadianping.mapper.ShopMapper;
import com.example.heimadianping.service.IShopService;
import com.example.heimadianping.utils.CacheClient;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.example.heimadianping.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private CacheClient cacheClient;

	@Override
	public Result queryById(Long id) {
		// 解决缓存穿透
		Shop shop = cacheClient
				.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
		System.out.println(shop);
		if (shop == null) {
			return Result.fail("店铺不存在！");
		}


		// 7.返回
		return Result.ok(shop);
	}

	@Override
	@Transactional
	public Result update(Shop shop) {
		Long id = shop.getId();
		if(id == null){
			return Result.fail("店铺id不能为空");
		}
		//更新数据库
		updateById(shop);
		//更新缓存
		stringRedisTemplate.delete(CACHE_SHOP_KEY + id);

		return Result.ok();
	}
}
