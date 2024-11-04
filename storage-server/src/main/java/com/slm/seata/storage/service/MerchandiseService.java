package com.slm.seata.storage.service;

import com.slm.seata.storage.entity.Merchandise;
import com.slm.seata.storage.model.MerchandiseCreate;

import java.math.BigDecimal;

/**
 * 商品服务
 */
public interface MerchandiseService {

    /**
     * 创建商品
     *
     * @param merchandiseCreate 商品创建信息
     * @return 完整商品信息
     */
    Merchandise create(MerchandiseCreate merchandiseCreate);

    /**
     * 获取商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    Merchandise get(Long id);

    /**
     * 扣减库存
     *
     * @param id 商品id
     * @param quantity 扣减数量
     */
    void deduct(Long id, BigDecimal quantity, Long buyerId);

}
