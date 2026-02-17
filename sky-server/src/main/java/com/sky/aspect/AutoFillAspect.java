package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class AutoFillAspect {
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && " +
            "@annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行数据填充");
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = annotation.value();
        if (args == null || args.length == 0){
            return;
        }
        Object arg = args[0];
        log.info("对对象进行数据填充：{}", arg);
        //为创建时间、更新时间赋值
        LocalDateTime now = java.time.LocalDateTime.now();
        Long CurrentId = BaseContext.getCurrentId();
        if (operationType == OperationType.INSERT){
            try {
                Method setCreateTime = arg.getClass().getMethod( AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreatUser = arg.getClass().getMethod( AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = arg.getClass().getMethod( AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getMethod( AutoFillConstant.SET_UPDATE_USER, Long.class);

                setCreateTime.invoke(arg, now);
                setCreatUser.invoke(arg, CurrentId);
                setUpdateTime.invoke(arg, now);
                setUpdateUser.invoke(arg, CurrentId);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
        else if (operationType == OperationType.UPDATE){
            try {
                Method setUpdateTime = arg.getClass().getMethod( AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getMethod( AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(arg, now);
                setUpdateUser.invoke(arg, CurrentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }


    }
}
