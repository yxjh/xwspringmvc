package com.xuwei.xwspring.beans.support;

import com.xuwei.xwspring.beans.config.XwBeanDefinition;
import com.xuwei.xwspring.context.support.XwAbstractApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/17
 * @description: 默认的处理类
 */
public class XwDefaultListableBeanFactory extends XwAbstractApplicationContext{
    private final Map<String, XwBeanDefinition>beanDefinitionMap=new ConcurrentHashMap<>(256);
}
