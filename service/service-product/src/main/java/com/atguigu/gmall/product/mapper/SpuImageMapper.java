package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuImage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SpuImageMapper extends BaseMapper<SpuImage> {
}
