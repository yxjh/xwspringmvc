package com.xuwei.xwspring.beans.config;

import lombok.Data;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/17
 * @description: 配置的容器
 */
@Data
public class XwBeanDefinition {

    private String beanClassName;

    private boolean lazyInit=false;

    private String factoryBeanName;
}
