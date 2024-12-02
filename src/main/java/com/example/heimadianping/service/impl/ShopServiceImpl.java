package com.example.heimadianping.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.Shop;
import com.example.heimadianping.mapper.ShopMapper;
import com.example.heimadianping.service.IShopService;
//import com.example.heimadianping.utils.CacheClient;
import com.example.heimadianping.utils.RedisConstants;
import com.example.heimadianping.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
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

	@Override
	public Result queryById(Long id) {
		//从redis查询商铺缓存
       String key = CACHE_SHOP_KEY + id;
       String shopJson = stringRedisTemplate.opsForValue()
		       .get(key);
		//判断是否存在
          if(StrUtil.isNotBlank(shopJson)) {
	          //存在 返回
	         Shop shop = JSONUtil.toBean(shopJson, Shop.class);
	         return Result.ok(shop);
          }
		//不存在 查询数据库
       Shop shop = getById(id);
		//不存在 返回错误
           if(shop == null) {
           	return Result.fail("店铺不存在");
           }
		// 存在 写进redis
		stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopJson));


		return Result.ok(shop);
	}
}
