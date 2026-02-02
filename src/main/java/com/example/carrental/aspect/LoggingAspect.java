package com.example.carrental.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.example.carrental.service..*(..))")
    public void serviceLayer() {
    }

    @Pointcut("execution(* com.example.carrental.controller..*(..))")
    public void controllerLayer() {
    }

    @Around("serviceLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("👉 Start method: {} with args: {}", methodName, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ End method: {}. Duration: {} ms. Result: {}", methodName, duration, result);

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Exception in method: {}. Duration: {} ms. Error: {}", methodName, duration, e.getMessage());
            throw e;
        }
    }

    @AfterThrowing(pointcut = "controllerLayer()", throwing = "e")
    public void logControllerException(JoinPoint joinPoint, Throwable e) {
        log.error("🔥 Controller Exception in {}. Message: {}", joinPoint.getSignature().toShortString(),
                e.getMessage());
    }
}
