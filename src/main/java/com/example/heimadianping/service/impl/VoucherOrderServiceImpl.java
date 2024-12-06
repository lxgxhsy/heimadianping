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
import com.example.heimadianping.utils.SimpleRedisLock;
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
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

	@Resource
	private ISeckillVoucherService seckillVoucherService;

	@Resource
	private RedisIdWorker redisIdWorker;

	@Resource
	private RedissonClient redissonClient;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

	static {
		SECKILL_SCRIPT = new DefaultRedisScript<>();
		SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
		SECKILL_SCRIPT.setResultType(Long.class);
	}

	private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

	@PostConstruct
	private void init(){
		SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
	}

	@PreDestroy
	public void shutdown() {
		SECKILL_ORDER_EXECUTOR.shutdown();
		try {
			if (!SECKILL_ORDER_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
				SECKILL_ORDER_EXECUTOR.shutdownNow();
			}
		} catch (InterruptedException e) {
			SECKILL_ORDER_EXECUTOR.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private class VoucherOrderHandler implements Runnable {
		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()){
				try {
					List<MapRecord<String, Object, Object>> list =
							stringRedisTemplate.opsForStream().read(
									Consumer.from("g1", "c1"),
									StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
									StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
							);
					if(list == null || list.isEmpty()){
						continue;
					}
					MapRecord<String, Object, Object> record = list.get(0);
					Map<Object, Object> value = record.getValue();
					VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);

					createVoucherOrder(voucherOrder);
					stringRedisTemplate.opsForStream().acknowledge("s1","g1",record.getId());
				} catch (Exception e) {
					log.error("处理订单异常", e);
					handlePendingList();
				}
			}
		}
	}

	private void handlePendingList() {
		while(true){
			try {
				List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
						Consumer.from("g1", "c1"),
						StreamReadOptions.empty().count(1),
						StreamOffset.create("stream.orders", ReadOffset.from("0"))
				);
				if(list == null || list.isEmpty()){
					break;
				}
				MapRecord<String, Object, Object> record = list.get(0);
				Map<Object, Object> value = record.getValue();
				VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
				createVoucherOrder(voucherOrder);
				stringRedisTemplate.opsForStream().acknowledge("s1","g1",record.getId());
			} catch (Exception e){
				log.error("处理订单异常", e);
				// 可以添加退出机制或限次数，避免无限循环
				break;
			}
		}
	}

	@Override
	@Transactional
	public void createVoucherOrder(VoucherOrder voucherOrder) {
		Long userId = UserHolder.getUser().getId();
		Long voucherId = voucherOrder.getVoucherId();
		RLock redisLock = redissonClient.getLock("lock:order:" + userId);

		boolean isLock = false;
		try {
			isLock = redisLock.tryLock(10, TimeUnit.SECONDS);
			if (!isLock) {
				log.error("不允许重复下单");
				return;
			}

			long orderId = redisIdWorker.nextId("order");
			int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
			if (count > 0) {
				log.error("用户已经购买了一次!");
				return;
			}

			// 扣除库存
			boolean success = seckillVoucherService.update()
					.setSql("stock = stock - 1")
					.eq("voucher_id", voucherId)
					.gt("stock", 0)
					.update();
			if (!success) {
				log.error("库存不足");
				return;
			}

			// 创建订单
			save(voucherOrder);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("获取锁时被中断", e);
		} finally {
			if (isLock) {
				redisLock.unlock();
			}
		}
	}

	@Override
	public Result seckillVoucher(Long voucherId) {
		Long userId = UserHolder.getUser().getId();
		long orderId = redisIdWorker.nextId("order");
		Long result = stringRedisTemplate.execute(
				SECKILL_SCRIPT,
				Collections.emptyList(),
				voucherId.toString(), userId.toString(), String.valueOf(orderId)
		);
		int r = result.intValue();
		if(r != 0){
			return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
		}
		return Result.ok(orderId);
	}
}

//@Slf4j
//@Service
//public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
//
//	@Resource
//	private ISeckillVoucherService SeckillVoucherService;
//
//	@Resource
//	private RedisIdWorker redisIdWorker;
//
//	@Resource
//	private RedissonClient redissonClient;
//
//	@Resource
//	private StringRedisTemplate stringRedisTemplate;
//
//	private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
//
//	static {
//		SECKILL_SCRIPT = new DefaultRedisScript<>();
//		SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
//		SECKILL_SCRIPT.setResultType(Long.class);
//	}
//
//	private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
//
//
//	@PostConstruct
//	private void init(){
//		SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
//	}
//
//	private class VoucherOrderHandler implements Runnable{
//		@Override
//		public void run() {
//			while(true){
//				//获取队列中的订单信息 xreadgroup group g1 c1 count 1 block 2000 streams s1 >
//				try {
//					List<MapRecord<String, Object, Object>> list =
//							stringRedisTemplate.opsForStream().read(
//									Consumer.from("g1", "c1"),
//									StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//									StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
//							);
//					if(list == null || list.isEmpty()){
//						// 如果为null，说明没有消息，继续下一次循环
//						continue;
//					}
//					//解析数据
//					MapRecord<String, Object, Object> record = list.get(0);
//					Map<Object, Object> value = record.getValue();
//					//就是copy而已
//					VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//
//					//创建订单
//					createVoucherOrder(voucherOrder);
//					//确认xack
//					stringRedisTemplate.opsForStream().acknowledge("s1","g1",record.getId());
//				} catch (Exception e) {
//					log.error("处理订单异常",e);
//					//为了防止消息丢失 所以将异常消息存到pendingList里面
//					handlePendingList();
//				}
//			}
//		}
//	}
//
//	private void handlePendingList() {
//		while(true){
//			try {
//				//获取pendingList里面的订单信息  xgroup group g1 c1 count 1 block 2000 streams s1 0
//				List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//						Consumer.from("g1", "c1"),
//						StreamReadOptions.empty().count(1),
//						StreamOffset.create("stream.orders", ReadOffset.from("0"))
//				);
//				//判断订单为空
//				if(list == null || list.isEmpty()){
//					//如果为空 说明没有什么异常 结束循环
//					break;
//				}
//				//解析数据
//				MapRecord<String, Object, Object> record = list.get(0);
//				Map<Object, Object> value = record.getValue();
//				VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//			    //创建订单
//				createVoucherOrder(voucherOrder);
//				//确认消息 XACK
//				stringRedisTemplate.opsForStream().acknowledge("s1","g1",record.getId());
//			}catch (Exception e){
//				log.error("处理订单异常", e);
//			}
//		}
//	}
//	//	private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
////	private class VoucherOrderHandler implements Runnable{
////		@Override
////		public void run() {
////			while(true){
////				//获取队列中的订单信息
////				try {
////					VoucherOrder voucherOrder = orderTasks.take();
////					//创建订单
////					handleVoucherOrder(voucherOrder);
////				} catch (InterruptedException e) {
////					e.printStackTrace();
////				}
////			}
////		}
////	}
//
////	private void handleVoucherOrder(VoucherOrder voucherOrder) {
////		//获取用户
////		Long userId = voucherOrder.getUserId();
////
////		RLock lock = redissonClient.getLock("lock:order:" + userId);
////		//获取锁
////		boolean isLock = lock.tryLock();
////		//判断是否获取成功
////		if(!isLock){
////		 //获取锁失败 返回错误或重试
////		log.error("不容许重新下单");
////		return;
////		}
////		try{
////
////			 proxy.createVoucherOrder(voucherOrder);
////
////		}finally {
////			lock.unlock();
////		}
////
////	}
//
////	/**
////	 * CAS compare and swap  乐观锁方法
////	 * @param voucherId
////	 * @return
////	 */
////	@Override
////	public Result seckillVoucher(Long voucherId) {
////		//查询优惠卷
////		SeckillVoucher voucher = SeckillVoucherService.getById(voucherId);
////		long orderId = redisIdWorker.nextId("order");
////		//判断秒杀是否开始
////		if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
////			return Result.fail("秒杀还未开始");
////		}
////		//判断秒杀是否结束
////		if(voucher.getEndTime().isBefore(LocalDateTime.now())){
////			return Result.fail("秒杀已经结束");
////		}
////		//判断库存是否充足
////		if(voucher.getStock() < 1){
////			return Result.fail("库存不足");
////		}
////
////		return createVoucherOrder(voucherId);
////
////	}
//
//
//	@Override
//	@Transactional
//	public  void createVoucherOrder(VoucherOrder voucherOrder) {
//		//一人一单
//		//读写事务隔离  读写冲突
//		Long userId = UserHolder.getUser().getId();
//		Long voucherId = voucherOrder.getVoucherId();
//		RLock redisLock = redissonClient.getLock("lock:order:" + userId);
//
//		boolean isLock = redisLock.tryLock();
//		if (!isLock) {
//			log.error("不允许重复下单");
//			return;
//		}
//
//
//
//		try {
//			int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//			if (count > 0) {
//				log.error("用户已经购买了一次!");
//				return;
//			}
//
//			//扣除库存
//			boolean success = SeckillVoucherService.update()
//					.setSql("stock = stock - 1")
//					.eq("voucher_id", voucherId)
//					.gt("stock", 0) // where id = ? and stock > 0
//					.update();
//			if (!success) {
//				log.error("库存不足");
//				return;
//			}
//
//
//			//创建订单
//
//			save(voucherOrder);
//		} finally {
//			redisLock.unlock();
//		}
//
//	}
////    private IVoucherOrderService proxy;
//	@Override
//	public Result seckillVoucher(Long voucherId) {
//
//		Long userId = UserHolder.getUser().getId();
//		long orderId = redisIdWorker.nextId("order");
//		//执行lua脚本
//		Long result = stringRedisTemplate.execute(
//				SECKILL_SCRIPT,
//				Collections.emptyList(),
//				voucherId.toString(),userId.toString(), String.valueOf(orderId)
//		);
//		int r = result.intValue();
//		if(r != 0){
//			//没有购买资格
//			return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
//		}
////		VoucherOrder voucherOrder = new VoucherOrder();
////		voucherOrder.setId(orderId);
////		voucherOrder.setUserId(userId);
////
////          orderTasks.add(voucherOrder);
////          //获取代理对象 事务 动态代理
////		 proxy = (IVoucherOrderService)AopContext.currentProxy();
//
//		return Result.ok(orderId);
//	}
//
////	Long userId = UserHolder.getUser().getId();
////	long orderId = redisIdWorker.nextId("order");
////	//执行lua脚本
////	Long result = stringRedisTemplate.execute(
////			SECKILL_SCRIPT,
////			Collections.emptyList(),
////			voucherId.toString(),userId.toString(), String.valueOf(orderId)
////	);
////	int r = result.intValue();
////		if(r != 0){
////		//没有购买资格
////		return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
////	}
////	VoucherOrder voucherOrder = new VoucherOrder();
////		voucherOrder.setId(orderId);
////		voucherOrder.setUserId(userId);
////
////          orderTasks.add(voucherOrder);
////	//获取代理对象 事务 动态代理
////	proxy = (IVoucherOrderService)AopContext.currentProxy();
////
////		return Result.ok(orderId);
//}
