package com.example.heimadianping;

import com.example.heimadianping.entity.Shop;
import com.example.heimadianping.service.impl.ShopServiceImpl;
import com.example.heimadianping.utils.CacheClient;
import com.example.heimadianping.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.heimadianping.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HeimadianpingApplicationTests {

	@Resource
	private CacheClient cacheClient;

	@Resource
	private ShopServiceImpl shopService;

	@Resource
	private RedisIdWorker redisIdWorker;

	private ExecutorService es = Executors.newFixedThreadPool(500);


	@Test
	void testIdWorker() throws InterruptedException{
		CountDownLatch latch = new CountDownLatch(300);

		Runnable task = () -> {
			for (int i = 0; i < 100; i++) {
				long id = redisIdWorker.nextId("order");
				System.out.println("id = " + id);
			}
			latch.countDown();
		};

		long begin = System.currentTimeMillis();
		for (int i = 0; i < 300; i++) {
			es.submit(task);
		}
		latch.await();
		long end = System.currentTimeMillis();
		System.out.println("time = " + (end - begin));



	}


	@Test
	void contextLoads() throws InterruptedException{
		Shop shop = shopService.getById(1L);
		cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L ,shop,10L, TimeUnit.SECONDS);
	}

}
