package com.example.heimadianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.SeckillVoucher;
import com.example.heimadianping.entity.VoucherOrder;
import com.example.heimadianping.mapper.VoucherOrderMapper;
import com.example.heimadianping.service.ISeckillVoucherService;
import com.example.heimadianping.service.IVoucherOrderService;
import com.example.heimadianping.utils.RedisIdWorker;
import com.example.heimadianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

	@Resource
	private ISeckillVoucherService SeckillVoucherService;

	@Resource
	private RedisIdWorker redisIdWorker;

	@Resource
	private RedissonClient redissonClient;

	@Resource
	private StringRedisTemplate stringRedisTemplate;


	/**
	 * CAS compare and swap  乐观锁方法
	 * @param voucherId
	 * @return
	 */
	@Override
	public Result seckillVoucher(Long voucherId) {
		//查询优惠卷
		SeckillVoucher voucher = SeckillVoucherService.getById(voucherId);
		long orderId = redisIdWorker.nextId("order");
		//判断秒杀是否开始
		if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
			return Result.fail("秒杀还未开始");
		}
		//判断秒杀是否结束
		if(voucher.getEndTime().isBefore(LocalDateTime.now())){
			return Result.fail("秒杀已经结束");
		}
		//判断库存是否充足
		if(voucher.getStock() < 1){
			return Result.fail("库存不足");
		}

		Long userId = UserHolder.getUser().getId();
		synchronized (userId.toString().intern()) {
			IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
			return proxy.createVoucherOrder(voucherId);
		}
	}


	@Transactional
	public  Result createVoucherOrder(Long voucherId) {
		//一人一单
		//读写事务隔离  读写冲突
		Long userId = UserHolder.getUser().getId();
		long orderId = redisIdWorker.nextId("order");
			int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
			if (count > 0) {
				return Result.fail("用户已经购买了一次!");
			}

			//扣除库存
			boolean success = SeckillVoucherService.update()
					.setSql("stock = stock - 1")
					.eq("voucher_id", voucherId)
					.gt("stock", 0) // where id = ? and stock > 0
					.update();
			if (!success) {
				return Result.fail("库存不足");
			}


			//创建订单
			VoucherOrder voucherOrder = new VoucherOrder();
			voucherOrder.setId(orderId);
			voucherOrder.setUserId(userId);
			voucherOrder.setVoucherId(voucherId);
			save(voucherOrder);

		return Result.ok(orderId);

	}


}
