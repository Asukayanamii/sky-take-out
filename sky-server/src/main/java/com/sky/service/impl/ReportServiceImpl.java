package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public TurnoverReportVO getTurnoverStatistic(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(StringUtils.join(dateList, ","));
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList)
        {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double amount = orderMapper.sumByMap(map);
            if (amount ==  null)
                amount = 0.0;
            turnoverList.add(amount);
        }
        turnoverReportVO.setTurnoverList(StringUtils.join(turnoverList, ","));
        return turnoverReportVO;
    }

    @Override
    public Result<UserReportVO> getUserStatistic(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        UserReportVO userReportVO = new UserReportVO();
        userReportVO.setDateList(StringUtils.join(dateList, ","));
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList)
        {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            map.put("begin", beginTime);
            Map map1 = new HashMap();
            map1.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);
            Integer newUser = userMapper.countByMap(map1);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        userReportVO.setNewUserList(StringUtils.join(newUserList, ","));
        userReportVO.setTotalUserList(StringUtils.join(totalUserList, ","));
        return Result.success(userReportVO);
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        OrderReportVO orderReportVO = new OrderReportVO();
        orderReportVO.setDateList(StringUtils.join(dateList, ","));
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList)
        {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer orderCount = orderMapper.countByMap(map);
            Map map1 = new HashMap();
            map1.put("begin", beginTime);
            map1.put("end", endTime);
            map1.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map1);
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }
        orderReportVO.setOrderCountList(StringUtils.join(orderCountList, ","));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList, ","));
        orderReportVO.setTotalOrderCount(orderCountList.stream().reduce(Integer::sum).get());
        orderReportVO.setValidOrderCount(validOrderCountList.stream().reduce(Integer::sum).get());
        Double orderCompletionRate = 0.0;
        if (orderReportVO.getTotalOrderCount() != 0) {
            orderCompletionRate = orderReportVO.getValidOrderCount().doubleValue() / orderReportVO.getTotalOrderCount().doubleValue();
        }
        orderReportVO.setOrderCompletionRate(orderCompletionRate);
        return orderReportVO;

    }
}
