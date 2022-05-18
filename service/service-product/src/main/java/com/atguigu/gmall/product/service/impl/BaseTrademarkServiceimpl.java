package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMappper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BaseTrademarkServiceimpl extends ServiceImpl<BaseTrademarkMappper,BaseTrademark> implements BaseTrademarkService{

    @Autowired
    private BaseTrademarkMappper baseTrademarkMappper;

    @Override
    public IPage<BaseTrademark> getTrademarkPage(Page<BaseTrademark> baseTrademarkPage) {
        QueryWrapper<BaseTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("id");
        return baseTrademarkMappper.selectPage(baseTrademarkPage,queryWrapper);
    }

}
