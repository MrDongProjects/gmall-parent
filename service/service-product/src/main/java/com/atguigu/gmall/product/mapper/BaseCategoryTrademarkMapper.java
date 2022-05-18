package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BaseCategoryTrademarkMapper extends BaseMapper<BaseCategoryTrademark> {
}
