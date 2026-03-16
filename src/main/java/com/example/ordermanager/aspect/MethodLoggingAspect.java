package com.example.ordermanager.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MethodLoggingAspect {

  private static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
      new DefaultParameterNameDiscoverer();

  private final LogValueFormatter logValueFormatter;

  public MethodLoggingAspect(LogValueFormatter logValueFormatter) {
    this.logValueFormatter = logValueFormatter;
  }

  @Around("execution(public * com.example.ordermanager..*(..))"
      + " && !within(com.example.ordermanager.aspect.MethodLoggingAspect)"
      + " && !within(com.example.ordermanager.aspect.LogValueFormatter)"
      + " && !within(com.example.ordermanager..entity..*)"
      + " && !within(com.example.ordermanager..dto..*)"
      + " && !within(com.example.ordermanager..exception..*)"
      + " && !within(com.example.ordermanager.OrderManagerApplication)")
  public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    Logger logger = LoggerFactory.getLogger(resolveLoggerType(joinPoint));
    String methodName = joinPoint.getSignature().toShortString();
    boolean debugEnabled = logger.isDebugEnabled();
    String arguments = null;

    long startTime = System.nanoTime();
    if (debugEnabled) {
      arguments =
          logValueFormatter.formatArguments(resolveParameterNames(joinPoint), joinPoint.getArgs());
      logger.debug("{} started with input={}", methodName, arguments);
    }

    try {
      Object result = joinPoint.proceed();
      long durationMs = toDurationMillis(startTime);
      if (debugEnabled) {
        String formattedResult =
            isVoidMethod(joinPoint) ? "void" : logValueFormatter.formatResult(result);
        logger.debug("{} finished with result={} durationMs={}", methodName, formattedResult,
            durationMs);
      }
      return result;
    } catch (Throwable throwable) {
      long durationMs = toDurationMillis(startTime);
      if (arguments == null) {
        arguments =
            logValueFormatter.formatArguments(resolveParameterNames(joinPoint), joinPoint.getArgs());
      }
      logger.error("{} failed after {} ms with input={} error={}", methodName, durationMs,
          arguments, throwable.getMessage(), throwable);
      throw throwable;
    }
  }

  private Class<?> resolveLoggerType(ProceedingJoinPoint joinPoint) {
    Object target = joinPoint.getTarget();
    if (target != null) {
      return AopUtils.getTargetClass(target);
    }
    return joinPoint.getSignature().getDeclaringType();
  }

  private String[] resolveParameterNames(ProceedingJoinPoint joinPoint) {
    if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
      String[] parameterNames =
          PARAMETER_NAME_DISCOVERER.getParameterNames(methodSignature.getMethod());
      if (parameterNames != null) {
        return parameterNames;
      }
    }
    if (joinPoint.getSignature() instanceof CodeSignature codeSignature) {
      return codeSignature.getParameterNames();
    }
    return new String[0];
  }

  private boolean isVoidMethod(ProceedingJoinPoint joinPoint) {
    return joinPoint.getSignature() instanceof MethodSignature methodSignature
        && Void.TYPE.equals(methodSignature.getReturnType());
  }

  private long toDurationMillis(long startTime) {
    return (System.nanoTime() - startTime) / 1_000_000;
  }
}

