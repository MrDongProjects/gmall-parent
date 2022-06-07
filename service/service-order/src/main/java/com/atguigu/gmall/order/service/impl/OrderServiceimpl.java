package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.constant.MqConst;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@SuppressWarnings("all")
public class OrderServiceimpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String WARE_URL;


    /**
     * 提交订单
     * @param orderInfo
     * @return
     */
    @Override
    @Transactional
    public Long submitOrder(OrderInfo orderInfo) {
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        //TODO 插入没有用户ID 后加的
        orderInfo.setUserId(orderInfo.getId());
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        //定为一天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder tradeBody = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName()).append("  ");
        }
        if (tradeBody.toString().length() > 100) {
            orderInfo.setTradeBody(tradeBody.toString().substring(0, 100));
        } else {
            orderInfo.setTradeBody(tradeBody.toString());
        }
        orderInfoMapper.insert(orderInfo);
        //保存订单明细
        List<OrderDetail> orderDetails = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetails) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
        //发送延迟队列，如果定时未支付，取消订单
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        return orderInfo.getId();
    }

    /**
     * 生成订单流水号
     *
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    /**
     * 校验流水号
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeCodeNo.equals(redisTradeNo);
    }

    /**
     * 删除流水号
     * @param userId
     */
    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);

    }

    /**
     * 校验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    /**
     * 我的订单-分页查询订单列表
     * @param orderInfoPage
     * @param userId
     * @return
     */
    @Override
    public IPage<OrderInfo> getPage(Page<OrderInfo> orderInfoPage, String userId) {
        IPage<OrderInfo> page = orderInfoMapper.selectPageByUserId(orderInfoPage,userId);
        page.getRecords().stream().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
        });
        return page;
    }

    /**
     * 处理过期订单
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId,String flag) {
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)) {
            // 发送消息队列，关闭支付宝的交易记录。
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, MqConst.QUEUE_PAYMENT_CLOSE);
        }
    }

    /**
     * 根据订单Id 修改订单的状态
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        //Bug:写死userId为1
        orderInfo.setUserId(1L);
        orderInfoMapper.updateById(orderInfo);
    }

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(queryWrapper);
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    /**
     * 发送消息，通知仓库
     * @param orderId
     */
    @Override
    public void sendOrderStatu(Long orderId) {
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        String wareJson = initWareOrder(orderId);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    /**
     * 根据orderId获取json字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(Long orderId) {
        //根据id获取订单详情
        OrderInfo orderInfo = getOrderInfo(orderId);
        //将订单数据装换成map
        Map map=initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    /**
     * 将订单信息装换成map对象
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo){
        Map<String, Object> map = new HashMap<>();
        //订单的详情信息
        map.put("orderId",orderInfo.getId());
        map.put("Consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","ONLINE".equals(orderInfo.getPaymentWay())?"2":"1");
        //仓库id
        map.put("wareId",orderInfo.getWareId());
        //订单明细详情信息

        //获取订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //定义集合收集订单明细
        List<Map<String, Object>> listMap = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                //定义map介绍订单明细对象数据
                Map<String, Object> hashMap = new HashMap<>();
                hashMap.put("skuId",orderDetail.getSkuId());
                hashMap.put("skuNum",orderDetail.getSkuNum());
                hashMap.put("skuName",orderDetail.getSkuName());
                listMap.add(hashMap);
            }
        }
        map.put("details", listMap);
        return map;
    }

    /**
     * 根据orderInfo设置userId 解决订单无userId的Bug
     * @param parentOrderId
     */
//    @Override
//    public void setUserIdWithParent(OrderInfo orderInfo) {
//        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
//        QueryWrapper<OrderInfo> id = queryWrapper.eq("id", orderInfo.getId());
//        QueryWrapper<OrderInfo> parentOrderId = queryWrapper.eq("parent_order_id",  orderInfo.getParentOrderId());
//        if (id == parentOrderId){
//            orderInfo.setUserId();
//        }
//
//
//    }

    /**
     * 拆单业务
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    @Transactional
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        ArrayList<OrderInfo> orderInfoArrayList = new ArrayList<>();
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
//        Long userId = orderInfoOrigin.getUserId();
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(maps)){
            for (Map map : maps) {
                String wareId = (String) map.get("wareId");
                List<String> skuIds = (List<String>) map.get("skuIds");
                //子订单集合
                OrderInfo subOrderInfo = new OrderInfo();
                // 属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
                // 防止主键冲突
                subOrderInfo.setId(null);

                // 赋值仓库Id
                subOrderInfo.setWareId(wareId);
                // 计算子订单的金额: 必须有订单明细
                // 获取到子订单明细
                // 声明一个集合来存储子订单明细
                List<OrderDetail> orderDetails = new ArrayList<>();
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                // 表示从主订单明细中 获取到 子订单的明细
                if (orderDetailList != null && orderDetailList.size() > 0){
                    for (OrderDetail orderDetail : orderDetailList) {
                        // 获取子订单明细的商品Id
                        for (String skuId : skuIds) {
                            if (Long.parseLong(skuId) == orderDetail.getSkuId().longValue()){
                                // 将订单明细添加到集合
                                orderDetails.add(orderDetail);
                            }
                        }
                    }
                }
                subOrderInfo.setOrderDetailList(orderDetails);
                // 计算总金额
                subOrderInfo.sumTotalAmount();
                //设置父订单id
                subOrderInfo.setParentOrderId(orderId);
                subOrderInfo.setOperateTime(new Date());
                //获取当前ID
                Long userId = getNowUserId(orderId);
                //设置user_id
                subOrderInfo.setUserId(userId);
//                QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
//                queryWrapper.eq("user_id", userId);
//                orderInfoMapper.update(subOrderInfo, queryWrapper);
                // 保存子订单
                submitOrder(subOrderInfo);
                // 将子订单添加到集合中！
                orderInfoArrayList.add(subOrderInfo);
            }
        }
        // 修改原始订单的状态
        updateOrderStatus(orderId, ProcessStatus.SPLIT);
        return orderInfoArrayList;
    }

    /**
     * 获取当前ID
     * @param orderId
     * select userId from order_info where id = #{orderId}
     */
    @Override
    public Long getNowUserId(Long orderId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", orderId);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        Long userId = orderInfo.getUserId();
        return userId;

    }

}