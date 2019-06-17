package com.xuwei.xwspring.context;

import com.xuwei.xwspring.beans.XwBeanFactory;
import com.xuwei.xwspring.beans.support.XwDefaultListableBeanFactory;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/17
 * @description: ApplicationContext类
 */
public class XwApplicationContext extends XwDefaultListableBeanFactory implements XwBeanFactory {

    @Override
    public Object getBean(String beanName) {
        return null;
    }

    @Override
    public void refresh() {

    }


}
