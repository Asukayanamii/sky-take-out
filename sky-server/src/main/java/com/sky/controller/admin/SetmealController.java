package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping("/admin/setmeal")
    public Result save(@RequestBody SetmealDTO setmealDTO)
    {
        log.info("新增套餐");
        setmealService.save(setmealDTO);
        return Result.success();
    }

}
