package com.xuwei.xwspring.context.support;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/17
 * @description: 模板模式，针对不同的Application扩展，ioc容器实现的顶层
 */
public abstract class XwAbstractApplicationContext {
    //只提供给子类重写
    public void refresh(){}
}
