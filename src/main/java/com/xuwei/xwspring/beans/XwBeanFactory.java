package com.xuwei.xwspring.beans;

/**
 * @author: xuWei
 * @create: 2019/06/17
 */
public interface XwBeanFactory {

    /**
     * 根据beanname从ioc容器中获取一个实例
     * @param beanName
     * @return
     */
    Object getBean(String beanName);
}
