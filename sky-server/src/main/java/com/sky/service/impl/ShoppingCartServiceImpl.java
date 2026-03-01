package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //检查购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //当前商品在购物车中已经存在
        if(list != null && list.size() > 0) {
            list.get(0).setNumber(list.get(0).getNumber() + 1);
            shoppingCartMapper.update(list.get(0));
        }
        else {
            if(shoppingCartDTO.getDishId() != null){
                Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            }
            else{
                SetmealVO setmealVO = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmealVO.getName());
                shoppingCart.setImage(setmealVO.getImage());
                shoppingCart.setAmount(setmealVO.getPrice());
            }
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);
            shoppingCartMapper.insert(shoppingCart);
        }

    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    @Override
    public void clean() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }
}
