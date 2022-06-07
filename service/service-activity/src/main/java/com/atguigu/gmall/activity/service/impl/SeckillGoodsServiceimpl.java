package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@SuppressWarnings("all")
public class SeckillGoodsServiceimpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;


    /**
     * 秒杀列表查询
     * @return
     */
    @Override
    public List<SeckillGoods> findAll() {
        List<SeckillGoods> seckillGoodsList = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return seckillGoodsList;
    }

    /**
     * 根据ID获取实体
     * @param skuId
     * @return
     */
    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
        return seckillGoods;
    }

    /**
     * 创建订单
     * @param skuId
     * @param userId
     */
    @Override
    public void seckillOrder(Long skuId, String userId) {
        //产品状态位， 1：可以秒杀 0：秒杀结束
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)){
            //已售罄
            return;
        }
        //判断用户是否下单  setIfAbsent:如果为空就set值,并返回1 如果存在(不为空)不进行操作,并返回0
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //已经下单
        if (!isExist){
            return;
        }
        //获取队列中的商品，如果能够获取，则商品存在，可以下单
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(goodsId)){
            //商品售罄，更新状态位
            redisTemplate.convertAndSend("seckillpush", skuId+":0");
            //已售罄
            return;
        }
        //订单记录
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(getSeckillGoods(skuId));
        orderRecode.setNum(1);
        //生成订单单码
        orderRecode.setOrderStr(MD5.encrypt(userId+skuId));
        //订单数据存入Reids
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(), orderRecode);
        //更新库存
        updateStockCount(orderRecode.getSeckillGoods().getSkuId());
    }

    /**
     * 根据商品id与用户ID查看订单信息
     * @param skuId
     * @param userId
     * @return
     */
    @Override
    public Result checkOrder(Long skuId, String userId) {
        // 用户在缓存中存在，有机会秒杀到商品
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (isExist){
            //判断用户是否正在排队
            //判断用户是否下单
            Boolean isHasKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (isHasKey){
                //抢单成功
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                // 秒杀成功！
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        //判断是否下单
        boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        if (isExistOrder){
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)){
            //已售罄 抢单失败
            return Result.build(null, ResultCodeEnum.SECKILL_FAIL);
        }
        //正在排队中
        return Result.build(null, ResultCodeEnum.SECKILL_RUN);
    }


    /**
     * 表示更新mysql -- redis 的库存数据！
     * @param skuId
     */
    private void updateStockCount(Long skuId) {
        //  加锁！
        Lock lock = new ReentrantLock();
        //上锁
        lock.lock();
        try {
            //  获取到存储库存剩余数！
            //  key = seckill:stock:46
            String stockKey = RedisConst.SECKILL_STOCK_PREFIX + skuId;
            Long count = redisTemplate.boundListOps(stockKey).size();
            //减少库存数！方式一减少压力!
            if (count % 2 == 0){
                //  开始更新数据！
                SeckillGoods seckillGoods = getSeckillGoods(skuId);
                //  赋值剩余库存数！
                seckillGoods.setStockCount(count.intValue());
                //  更新的数据库！
                seckillGoodsMapper.updateById(seckillGoods);
                //  更新缓存！
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(), seckillGoods);
            }
        }finally {
            //解锁
            lock.unlock();
        }
    }
}
