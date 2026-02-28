package com.sky.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private final static String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //1.调用微信接口服务，获取微信登录用户的openid
        String openid = getOpenid(userLoginDTO.getCode());
        //2.判断openid是否为空
        if (openid == null)
        {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //3.判断当前微信用户是否为新用户
        User user = userMapper.getByOpenid(openid);
        if (user == null)
        {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        return user;
    }

    //调用微信接口服务，获取微信登录用户的openid
    private String getOpenid(String code) {
        System.out.println("微信登录");
        Map<String, Object> param = new HashMap<>();
        param.put("appid", weChatProperties.getAppid());
        param.put("secret", weChatProperties.getSecret());
        param.put("js_code", code);
        param.put("grant_type", "authorization_code");

        String result= HttpUtil.get(WX_LOGIN_URL, param);
        JSONObject jsonObject = JSON.parseObject(result);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
