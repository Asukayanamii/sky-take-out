package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    DishService dishService;

    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品:{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO)
    {
        log.info("分页查询菜品表");
        PageResult pageResult = dishService.getPage(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    @DeleteMapping
    public Result deleteBatch(@RequestParam List<Long> ids)
    {
        log.info("批量删除菜品+{}",ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }



}
