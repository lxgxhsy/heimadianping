package com.example.heimadianping.utils;

/**
 * @Author: sy
 * @CreateTime: 2024-12-04
 * @Description:
 */
public interface ILock {

	/**
	 *  尝试获取锁
	 * @param timeoutSec
	 * @return
	 */
	boolean tryLock(long timeoutSec);

	/**
	 * 获取锁
	 */
	void unlock();
}
