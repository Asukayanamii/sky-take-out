package com.sky.service;

import com.sky.result.Result;
import com.sky.vo.*;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO getTurnoverStatistic(LocalDate begin, LocalDate end);

    Result<UserReportVO> getUserStatistic(LocalDate begin, LocalDate end);

    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end);
}
