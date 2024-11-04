package com.slm.seata.storage.service.impl;

import com.slm.seata.storage.entity.Merchandise;
import com.slm.seata.storage.entity.SaleDetail;
import com.slm.seata.storage.mapper.MerchandiseMapper;
import com.slm.seata.storage.mapper.SaleDetailMapper;
import com.slm.seata.storage.model.MerchandiseCreate;
import com.slm.seata.storage.service.MerchandiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MerchandiseServiceImpl implements MerchandiseService {

    private final MerchandiseMapper merchandiseMapper;
    private final SaleDetailMapper saleDetailMapper;

    @Override
    public Merchandise create(MerchandiseCreate merchandiseCreate) {
        Merchandise merchandise = new Merchandise();
        merchandise.setName(merchandiseCreate.getName());
        merchandise.setUnitPrice(merchandiseCreate.getUnitPrice());
        merchandise.setQuantity(merchandiseCreate.getQuantity());
        merchandise.setStatus(1);
        merchandiseMapper.save(merchandise);
        return merchandise;
    }

    @Override
    @Transactional(readOnly = true)
    public Merchandise get(Long id) {
        return merchandiseMapper.getById(id).orElseThrow(() -> new RuntimeException("商品不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long id, BigDecimal quantity, Long buyerId) {
        Merchandise merchandise = this.get(id);
        // 扣减库存
        Long m = merchandiseMapper.deduct(id, quantity);
        if (m == 0) { // 如果商品存在执行扣减SQL影响条数为0，则代表扣减大于库存
            throw new RuntimeException("商品[" + merchandise.getName() + "]库存不足");
        }
        // 保存销售明细
        this.saveSaleDetail(merchandise, quantity, buyerId);
    }

    private SaleDetail saveSaleDetail(Merchandise merchandise, BigDecimal quantity, Long buyerId) {
        SaleDetail saleDetail = new SaleDetail();
        saleDetail.setMerchandiseId(merchandise.getId());
        saleDetail.setMerchandiseName(merchandise.getName());
        saleDetail.setUnitPrice(merchandise.getUnitPrice());
        saleDetail.setQuantity(quantity);
        saleDetail.setTotalPrice(quantity.multiply(merchandise.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
        saleDetail.setAccountId(buyerId);
        saleDetail.setStatus(1);
        saleDetail.setCreatedTime(LocalDateTime.now());
        saleDetailMapper.save(saleDetail);
        return saleDetail;
    }

}
