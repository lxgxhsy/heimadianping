package com.example.heimadianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.entity.VoucherOrder;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

	Result seckillVoucher(Long voucherId);


	void createVoucherOrder(VoucherOrder voucherOrder);
}
