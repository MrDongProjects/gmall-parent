package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataUnit;

import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class CartServiceimpl implements CartService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 添加购物车
     *
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        //获取缓存key
        String cartKey = getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo = null;
        //是否包含
        if (boundHashOperations.hasKey(skuId.toString())) {
            cartInfo = boundHashOperations.get(skuId.toString());
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
            cartInfo.setIsChecked(1);
            cartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
            cartInfo.setUpdateTime(new Date());
        } else {
            cartInfo = new CartInfo();
            //给cartinfo赋值
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
            cartInfo.setSkuPrice(skuInfo.getPrice());
        }
        boundHashOperations.put(skuId.toString(), cartInfo);

    }

    /**
     * 通过用户Id 查询购物车列表
     *
     * @param userId
     * @param userTempId
     * @return
     */
    @Override
    public List<CartInfo> cartList(String userId, String userTempId) {
 /*
            1.  判断是否登录，根据判断结果查询不同的购物车！
            2.  查询的结果需要排序！
            3.  有可能需要合并！
                    在登录的情况下
                    .  未登录 ---> 登录合并！
                        合并完成之后，需要删除未登录购物车数据！
                     case1: 有userId ,没有userTempId
                     case2: 没有userId ,有userTempId   return noLoginCartInfoList
                     case3: 有userId ,有userTempId
                        登录情况下合并购物车：
                            先判断未登录购物车集合有数据！
                                true: 有数据
                                    合并
                                false: 没有数据
                                    只需要登录购物车数据
                            删除未登录购物车！
         */
        //  声明一个集合来存储未登录数据
        List<CartInfo> noLoginCartInfoList = null;

        //  完成case2 业务逻辑
        //  属于未登录
        if (!StringUtils.isEmpty(userTempId)){
            String cartKey = this.getCartKey(userTempId);
            //  获取登录的购物车集合数据！
            //  noLoginCartInfoList = this.redisTemplate.boundHashOps(cartKey).values();
            noLoginCartInfoList  = this.redisTemplate.opsForHash().values(cartKey);
        }
        //  这个是集合的排序
        if (StringUtils.isEmpty(userId)){
            if (!CollectionUtils.isEmpty(noLoginCartInfoList)){
                noLoginCartInfoList.sort((o1,o2)->{
                    //  按照更新时间：
                    return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
                });
            }
            //  返回未登录数据！
            return noLoginCartInfoList;
        }

        //login
        List<CartInfo> LoginCartInfoList = null;
        //先获取到购物车的key
        String cartKey = getCartKey(userId);
        //合并
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //判断
        if (!CollectionUtils.isEmpty(noLoginCartInfoList)){
            //遍历循环未登录的购物车集合
            noLoginCartInfoList.forEach(cartInfo -> {
                //  在未登录购物车中的skuId 与登录的购物车skuId 相对  skuId = 17 18
                if (boundHashOperations.hasKey(cartInfo.getSkuId().toString())){
                    //  合并业务逻辑 : skuNum + skuNum 更新时间
                    CartInfo loginCartInfo = boundHashOperations.get(cartInfo.getSkuId().toString());
                    loginCartInfo.setSkuNum(loginCartInfo.getSkuNum()+cartInfo.getSkuNum());
                    loginCartInfo.setUpdateTime(new Date());
                    //最新价格
                    loginCartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
                    //选中状态合并
                    if (cartInfo.getIsChecked().intValue() == 1){
                        //修改缓存的数据：
                        boundHashOperations.put(cartInfo.getSkuId().toString(), loginCartInfo);
                    }else {
                        //直接添加缓存
                        cartInfo.setUserId(userId);
                        cartInfo.setCreateTime(new Date());
                        cartInfo.setUpdateTime(new Date());
                        boundHashOperations.put(cartInfo.getSkuId().toString(), cartInfo);
                    }

                }
            });
            //删除未登录的购物车数据
            redisTemplate.delete(getCartKey(userTempId));
        }
        //获取合并之后的数据
        LoginCartInfoList = redisTemplate.boundHashOps(cartKey).values();
        if (CollectionUtils.isEmpty(LoginCartInfoList)){
            return new ArrayList<>();
        }
        LoginCartInfoList.sort(((o1, o2) -> {
            return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
        }));
        return LoginCartInfoList;
    }


    /**
     * 更新选中状态
     * @param userId
     * @param isChecked
     * @param skuId
     */
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        String cartKey = getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo = boundHashOperations.get(skuId.toString());
        if (null != cartInfo){
            cartInfo.setIsChecked(isChecked);
            boundHashOperations.put(skuId.toString(), cartInfo);
        }
    }

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    @Override
    public void deleteCart(Long skuId, String userId) {
        String cartKey = getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())){
            boundHashOperations.delete(skuId.toString());
        }

    }

    /**
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        String cartKey = getCartKey(userId.toString());
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        //更新价格
        for (CartInfo cartInfo : cartInfoList) {
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
        }
        List<CartInfo> cartInfos  = cartInfoList.stream().filter(cartInfo -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        return cartInfos;
    }


    /**
     * 获取购物车的key
     *
     * @param userId
     * @return
     */
    private String getCartKey(String userId) {
        return RedisConst.SKUKEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
