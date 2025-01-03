package com.example.heimadianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {


	Result queryById(Long id);


	Result update(Shop shop);


	Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
