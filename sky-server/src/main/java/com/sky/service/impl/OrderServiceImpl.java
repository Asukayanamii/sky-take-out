package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderQueryVO;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常(地址簿为空，购物车为空)
        AddressBook adb = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (adb == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartMapper.list(ShoppingCart.builder().id(userId).build());
        if (list == null || list.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(adb.getPhone());
        orders.setConsignee(adb.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(adb.getDetail());


        orderMapper.insert(orders);

        //向订单明细表插入n条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //封装响应结果并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult pageQuery4User(Integer page, Integer pageSize, Integer status) {
        List<OrderQueryVO> list = new ArrayList<>();
        //根据状态和用户id查询订单数据
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(page,pageSize);
        Page<Orders> pageInfo = orderMapper.pageQuery4User(status,userId);
        List<Orders> orders = pageInfo.getResult();
        for (Orders orders1 : orders){
            OrderQueryVO orderQueryVO = new OrderQueryVO();
            BeanUtils.copyProperties(orders1,orderQueryVO);
            list.add(orderQueryVO);
        }
        //根据订单查询详细订单数据
        for (OrderQueryVO orderQueryVO : list) {
            List<OrderDetail> orderDetailList = orderDetailMapper.list(orderQueryVO.getId());
            orderQueryVO.setOrderDetailList(orderDetailList);
        }
        //封装并返回分页数据
        return new PageResult(pageInfo.getTotal(),list);

    }

    @Override
    public OrderQueryVO orderDetailById(Long id) {
        OrderQueryVO orderQueryVO = new OrderQueryVO();
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //根据订单id查询订单详细
        List<OrderDetail> orderDetailList = orderDetailMapper.list(id);
        //封装VO
        orderQueryVO.setOrderDetailList(orderDetailList);
        BeanUtils.copyProperties(orders,orderQueryVO);
        return orderQueryVO;

    }

    @Override
    public void cancel(Long id) {
        //将订单状态设为已取消
        Orders orders = Orders.builder().status(Orders.CANCELLED).id(id).build();
        orderMapper.update(orders);
    }

    @Override
    @Transactional
    public void repetition(Long id) {
        //根据订单id查询订单数据
        Orders orders = orderMapper.getById(id);
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //根据订单id查询订单详细数据
        List<OrderDetail> orderDetailList = orderDetailMapper.list(id);
        //提交新的订单数据
        orders.setId(null);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setOrderTime(LocalDateTime.now());
        orders.setDeliveryStatus(0);
        orders.setEstimatedDeliveryTime(LocalDateTime.now().plus(60, ChronoUnit.MINUTES));
        orderMapper.insert(orders);
        //提交订单详细数据
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setId(null);
            orderDetail.setOrderId(orders.getId());
        }
        orderDetailMapper.insertBatch(orderDetailList);
    }

}
