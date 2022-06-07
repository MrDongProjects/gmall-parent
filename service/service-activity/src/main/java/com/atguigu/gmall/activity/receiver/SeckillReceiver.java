package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.constant.MqConst;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
@SuppressWarnings("all")
public class SeckillReceiver {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importToRedis(Message message, Channel channel){
        //  将当天的秒杀商品放入缓存！通过mapper 执行sql 语句！
        //  条件当天 ，剩余库存>0 , 审核状态 = 1
        QueryWrapper<SeckillGoods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status",1).gt("stock_count", 0);
        // select  DATE_FORMAT(start_time,'%Y-%m-%d') from seckill_goods; yyyy-mm-dd
        queryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        //  获取到当天秒杀的商品列表！
        List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectList(queryWrapper);
        //  将seckillGoodsList 这个集合数据放入缓存！
        for (SeckillGoods seckillGood : seckillGoods) {
            //  考虑使用哪种数据类型，以及缓存的key！使用hash！ hset key field value hget key field
            //  定义key = SECKILL_GOODS field = skuId value = seckillGoods
            //  判断当前缓存key 中是否有 秒杀商品的skuId
            Boolean aBoolean = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGood.getSkuId().toString());
            //  判断
            if (aBoolean){
                //  表示缓存中已经当前的商品了。
                continue;
            }else {
                //  没有就放入缓存！
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGood.getSkuId().toString(), seckillGood);
                //  将每个商品对应的库存剩余数，放入redis-list 集合中！
                for (Integer i = 0; i < seckillGood.getStockCount(); i++) {
                    //  将每个商品对应的库存剩余数，放入redis-list 集合中！
                    String key = RedisConst.SECKILL_STOCK_PREFIX + seckillGood.getSkuId();
                    redisTemplate.opsForList().leftPush(key, seckillGood.getSkuId().toString());
                }
                //  秒杀商品在初始化的时候：状态位初始化 1
                //  publish seckillpush 46:1  | 后续业务如果说商品被秒杀完了！ publish seckillpush 46:0
                redisTemplate.convertAndSend("seckillpush", seckillGood.getSkuId() + ":1");
            }
            //  手动确认消息

            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //监听用户与商品消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUser(UserRecode userRecode,Message message,Channel channel){
        //  判断接收过来的数据
        try {
            if (userRecode!=null){
                //  预下单处理！
                seckillGoodsService.seckillOrder(userRecode.getSkuId(),userRecode.getUserId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听删除消息！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void deleteRedisData(Message message,Channel channel){

        try {
            //  查询哪些商品是秒杀结束的！end_time , status = 1
            //  select * from seckill_goods where status = 1 and end_time < new Date();
            QueryWrapper<SeckillGoods> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1);
            queryWrapper.le("end_time", new Date());
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(queryWrapper);
            //  对应将秒杀结束缓存中的数据删除！
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                //  seckill:stock:46 删除库存对应key
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
            }
            //  删除预热等数据！ 主要针对于预热数据删除！ 我们项目只针对一个商品的秒杀！ 如果是多个秒杀商品，则不能这样直接删除预热秒杀商品的key！
            redisTemplate.delete(RedisConst.SECKILL_GOODS);
            //  预下单
            redisTemplate.delete(RedisConst.SECKILL_ORDERS);
            //  删除真正下单数据
            redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
            //  修改数据库秒杀对象的状态！
            SeckillGoods seckillGoods = new SeckillGoods();
            //  1:表示审核通过 ，2：表示秒杀结束
            seckillGoods.setStatus("2");
            seckillGoodsMapper.updateById(seckillGoods);
        } catch (Exception e) {
            e.printStackTrace();
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }


}
