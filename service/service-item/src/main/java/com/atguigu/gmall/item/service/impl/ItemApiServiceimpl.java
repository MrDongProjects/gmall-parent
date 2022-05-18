package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.item.service.ItemApiService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ItemApiServiceimpl implements ItemApiService {
    @Override
    public Map<String, Object> getItemApi(Long skuId) {
        Map<String, Object> result = new HashMap<>();
        return result;
    }
}
