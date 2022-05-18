package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMappper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceimpl extends ServiceImpl<BaseCategoryTrademarkMapper, BaseCategoryTrademark> implements BaseCategoryTrademarkService {

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;
    @Autowired
    private BaseTrademarkMappper baseTrademarkMappper;

    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {
        //  根据分类Id 获取到品牌Id 集合数据
        //  select * from base_category_trademark where category3_id = ?;
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(queryWrapper);
        //  判断baseCategoryTrademarkList 这个集合
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //  需要获取到这个集合中的品牌Id 集合数据
            List<Long> tradeMarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //  正常查询数据的话... 需要根据品牌Id 来获取集合数据！
                return baseTrademarkMappper.selectBatchIds(tradeMarkIdList);
        }
        //  如果集合为空，则默认返回空
        return null;
    }

    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        //  哪些是关联的品牌Id
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //  找到关联的品牌Id 集合数据 {1,3}
            List<Long> tradeMarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());
            //  在所有的品牌Id 中将这些有关联的品牌Id 给过滤掉就可以！
            //  select * from base_trademark; 外面 baseTrademarkMapper.selectList(null) {1,2,3,5}
            List<BaseTrademark> baseTrademarkList = baseTrademarkMappper.selectList(null).stream().filter(baseTrademark -> {
                return !tradeMarkIdList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            return baseTrademarkList;
        }
        return baseTrademarkMappper.selectList(null);
    }

    @Override
    public void saveTrademark(CategoryTrademarkVo categoryTrademarkVo) {
        //获取管理的品牌id列表
        for (Long trademarkId : categoryTrademarkVo.getTrademarkIdList()) {
            //创建分类品牌对象
            BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
            baseCategoryTrademark.setTrademarkId(trademarkId);
            baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
            baseCategoryTrademarkMapper.insert(baseCategoryTrademark);
        }


    }

    @Override
    public void removeTrademark(Long category3Id, Long trademarkId) {
        QueryWrapper<BaseCategoryTrademark> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        queryWrapper.eq("trademark_id",trademarkId);
        baseCategoryTrademarkMapper.delete(queryWrapper);


    }
}
