package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品:{}",dishDTO);
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);
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
        cleanCache("dish_*");
        dishService.deleteBatch(ids);
        return Result.success();
    }
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id)
    {
        log.info("查询回显id:+{}",id);
        DishVO dishVO = dishService.getById(id);
        
        return Result.success(dishVO);
    }
    @PutMapping
    public Result updateDish(@RequestBody DishDTO dishDTO)
    {
        log.info("修改菜品:{}",dishDTO);
        cleanCache("dish_*");
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }
    @GetMapping("/list")
    public Result<List<Dish>> getByCategoryId(Long categoryId)
    {
        log.info("通过分类id查询菜品，id:{}",categoryId);
        List<Dish> list = dishService.getByCategoryId(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        cleanCache("dish_*");
        dishService.startOrStop(status,id);
        return Result.success();
    }


    private void cleanCache(String pattern) {
        Set Keys = redisTemplate.keys(pattern);
        redisTemplate.delete(Keys);

    }

}
