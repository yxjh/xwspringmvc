package com.xuwei.xwspring.context;

/**
 * @author: xuWei
 * @create: 2019/06/17
 */
public interface XwApplicationContextAware {

    /**
     * 通过解耦的方式获得ioc容器的顶层设计
     * 通过一个监听器去扫描所有的类，只要实现了该接口，就自动调用下面的fangfa
     * 从而将ioc容器注入到目标类中
     * @param xwApplicationContext
     */
    void setApplicationContext(XwApplicationContext xwApplicationContext);
}
