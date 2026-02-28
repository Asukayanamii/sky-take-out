package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    DishMapper dishMapper;
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    DishFlavorMapper dishFlavorMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        log.info("新增菜品:{}",dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        Long id = dish.getId();
        log.info("菜品id:{}",id);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(id);
            }
            dishFlavorMapper.insertBatch(flavors);
        }



    }

    @Override
    public PageResult getPage(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.page(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //查询菜品是否启用
        for(Long id:ids)
        {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE)
            {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //查询菜品是否联系套餐
        List<Long> setmealIds = setmealMapper.getIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品
        dishMapper.deleteBatch(ids);

        //删除菜品口味数据
        dishFlavorMapper.deleteFlavorsBatch(ids);
    }

    @Override
    public DishVO getById(Long id) {
        //查询菜品数据
        Dish dish = dishMapper.getById(id);
        //查询菜品口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //装配VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        //返回VO
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        //更新菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //删除口味数据
        Long dishId = dishDTO.getId();
        List<Long> ids = new ArrayList<Long>();
        ids.add(dishId);
        dishFlavorMapper.deleteFlavorsBatch(ids);
        //插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> list = dishMapper.getByCategoryId(categoryId);
        return list;
    }
    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.getByCategoryId(dish.getCategoryId());

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

}
