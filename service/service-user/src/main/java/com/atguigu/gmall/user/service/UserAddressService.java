package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {
    /**
     * 根据userId获取用户地址
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(Long userId);

}
