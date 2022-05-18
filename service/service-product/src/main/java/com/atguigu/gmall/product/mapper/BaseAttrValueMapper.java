package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Author Echo
 * @Create 2022-05-14-23:01
 * @Description
 */
@Mapper
@Repository
public interface BaseAttrValueMapper extends BaseMapper<BaseAttrValue> {
}
